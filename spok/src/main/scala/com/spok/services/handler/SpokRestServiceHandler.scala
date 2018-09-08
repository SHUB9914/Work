package com.spok.services.handler

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.model.{ HttpResponse, _ }
import akka.http.scaladsl.model.ws.{ Message, TextMessage }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Flow, Source }
import akka.util.Timeout
import com.datastax.driver.dse.graph.Edge
import com.spok.model.SpokModel._
import com.spok.persistence.factory.DSEUserSpokFactoryApi
import com.spok.persistence.redis.RedisFactory
import com.spok.services.service.{ SpokLogger, _ }
import com.spok.services.util.SpokValidationUtil
import com.spok.util.Constant._
import com.spok.util._
import java.io.File

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import com.spok.services.service.SpokViewValidationCommands._
import com.spok.services.service.SpokViewCommands._
import com.spok.services.service.SpokManagerCommands._
import com.spok.services.service.SpokActorSuccessReplies._
import com.spok.services.service.SpokActorFailureReplies._
import com.spok.services.service.SpokViewValidationReplies._
import com.spok.services.service.SpokViewReplies._

case class InterimSpok(
  contentType: String,
  groupId: Option[String],
  visibility: Option[String],
  ttl: Option[Int],
  headerText: Option[String],
  file: Option[String],
  text: Option[String],
  url: Option[Url],
  poll: Option[Poll],
  riddle: Option[Riddle],
  geo: Geo
)

trait SpokRestServiceHandler extends JsonHelper with HttpUtil with SpokValidationUtil with RandomUtil
    with LoggerUtil {
  implicit val system: ActorSystem

  import system.dispatcher

  implicit val materializer: ActorMaterializer
  implicit val timeout = Timeout(40 seconds)

  import akka.pattern.ask

  val redisFactory: RedisFactory = RedisFactory
  val dseUserSpokFactoryApi: DSEUserSpokFactoryApi = DSEUserSpokFactoryApi
  val spokLogger: SpokLogger = SpokLogger
  val fileUploadUtility: FileUploadUtility = FileUploadUtility

  /**
   * This method is for detecting request
   * and perform spok operation accordingily
   */
  def detectRequestAndPerform(command: ActorRef, query: ActorRef, userId: String, phoneNumber: String): Flow[Message, Message, _] = {
    logger.info(s"Spok service is connected for ${phoneNumber}. Now ready to perform spok action!!")
    Flow[Message].mapAsync(Runtime.getRuntime.availableProcessors()) {
      case TextMessage.Strict(txt) => handleUserRequests(command, query, userId, phoneNumber, txt)
      case TextMessage.Streamed(stream) => {
        stream
          .limit(Int.MaxValue) // Max frames we are willing to wait for
          .completionTimeout(50 seconds) // Max time until last frame
          .runFold("")(_ + _) // Merges the frames
          .flatMap { txt =>
            logger.info("Getting streamed message ")
            handleUserRequests(command, query, userId, phoneNumber, txt)
          }
      }
      case _ => {
        logger.info(s"${phoneNumber}: What the hell is this?")
        Future(TextMessage(write(sendFormattedError(ACT_101, INVALID_ACTION))))
      }
    }
  }

  private def handleUserRequests(command: ActorRef, query: ActorRef, userId: String, phoneNumber: String, txt: String): Future[TextMessage] = {
    val action = (parse(txt) \ (ACTION)).extractOpt[String]
    logger.info(s"${phoneNumber}:  is performing <${action}>!")
    action match {
      case Some(RESPOK) => {
        logger.info(s"Performing respok for $userId")
        respokHandler(command, query, userId, txt)
      }
      case Some(UNSPOK) => unspokHandler(command, query, userId, txt)
      case Some(ADD_COMMENT) => addCommentHandler(command, query, userId, txt)
      case Some(REMOVE_COMMENT) => removeCommentHandler(command, query, userId, txt)
      case Some(UPDATE_COMMENT) => updateCommentHandler(command, query, userId, txt)
      case Some(DISABLE) => eventuateDisableSpokHandler(command, query, userId, txt)
      case Some(REMOVE_SPOK) => eventuateRemoveSpokFromWallHandler(query, command, userId, txt)
      case Some(SUBSCRIBE) => eventuateSubscribeUnsubscribeHandler(query, userId, txt)
      case Some(ANSWER_POLL) => answerPollQuestionHandler(command, userId, txt)
      case Some(ANSWERS_POLL) => answerAllPollQuestionHandler(command, userId, txt)
      case Some(invalidAction) => Future(TextMessage(write(sendFormattedError(SPK_201, "Invalid Action", Some("Action")))))
      case None => Future(TextMessage(write(sendJsonErrorWithEmptyData(Some("Action")))))
    }
  }

  /**
   * @param command
   * @return spok creation success if spok is created else false
   */
  def storeSpokSettings(query: ActorRef, command: ActorRef, userId: String, json: String, file: Option[File]): Future[HttpResponse] = {
    val isSpokerSuspended: Future[IsUserSuspendedAsk] = ask(query, IsUserSuspended(userId)).mapTo[IsUserSuspendedAsk]
    val response = isSpokerSuspended.map { response =>
      response.status match {
        case SPOKER_SUSPENDED =>
          Future(HttpResponse(StatusCodes.BadRequest, entity = HttpEntity(
            ContentTypes.`application/json`,
            write(sendFormattedError(SPK_014, ACTION_IMPOSSIBLE, Some(CREATE_SPOK)))
          )))

        case SPOKER_NOT_SUSPENDED =>
          val spok = createSpok(json, file)
          spok match {
            case (Some(userSpok), message) => {
              val futureResponse = ask(command, Create(userSpok, userId)).mapTo[SpokAck]
              futureResponse.map(handleResponse(json, userSpok, _, userId))
            }
            case (None, message) => Future(message)
          }

        case _ => Future(HttpResponse(StatusCodes.BadRequest, entity = HttpEntity(
          ContentTypes.`application/json`,
          write(sendFormattedError(SPK_109, UNABLE_CREATING_SPOK_GENERIC_ERROR, Some(CREATE_SPOK)))
        )))

      }
    }
    response.flatMap(identity)
  }

  private def handleResponse(txt: String, userSpok: Spok, response: SpokAck, userId: String): HttpResponse = {
    response match {
      case spokDiffusion: SpokCreateSuccess =>
        val mentionUserIdList = getMentionUserList(txt)
        redisFactory.storeSubscriber(userSpok.spokId, userId)
        spokLogger.upsertSubscriberDetails(userSpok.spokId, userId)
        HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, write(generateCommonResponseForCaseClass(SUCCESS, Some(List()),
          Some(Response(spokResponse = Some(SpokResponse(userSpok.spokId, mentionUserIdList)))), Some(CREATE_SPOK)))))
      case spokCreateFailure: SpokCreateFailure =>
        HttpResponse(StatusCodes.BadRequest, entity = HttpEntity(
          ContentTypes.`application/json`,
          write(sendFormattedError(spokCreateFailure.errorId, spokCreateFailure.cause.getMessage, Some(CREATE_SPOK)))
        ))
    }
  }

  /**
   * @param command
   * @param query
   * @return respok success if all validations pass and respok is done otherwise the error message
   */
  private def respokHandler(command: ActorRef, query: ActorRef, userId: String, json: String): Future[TextMessage] = {

    val isSpokerSuspended: Future[IsUserSuspendedAsk] = ask(query, IsUserSuspended(userId)).mapTo[IsUserSuspendedAsk]
    val response = isSpokerSuspended.map { response =>
      response.status match {
        case SPOKER_SUSPENDED => Future(TextMessage(write(sendFormattedError(SPK_014, ACTION_IMPOSSIBLE, Some(RESPOK)))))
        case SPOKER_NOT_SUSPENDED =>

          val spokIdOpt = (parse(json) \ (SPOK_ID)).extractOpt[String]
          spokIdOpt match {
            case Some(spokInstanceId) => {
              val respok = extractRespokDetailsWithValidation(json)
              respok match {
                case (Some(respok), _) => eventuateRespokHandler(command, query, spokInstanceId, respok, userId)
                case (None, message) => Future(message)
              }
            }
            case None => Future(TextMessage(write(sendJsonErrorWithEmptyData(Some(RESPOK)))))
          }

        case _ => Future(TextMessage(write(sendFormattedError(SPK_117, "Unable re-spoking spok (generic error).", Some(RESPOK)))))
      }
    }
    response.flatMap(identity)
  }

  private def eventuateRespokHandler(command: ActorRef, query: ActorRef, spokId: String, respok: Respok, userId: String): Future[TextMessage] = {
    val futureResponse = ask(query, IsValidSpokAndSendStatus(userId, spokId)).mapTo[IsValidSpokAck]
    val respokResult = futureResponse.map {
      case IsValidSpokAck(RESPOKED, _, _) => Future(TextMessage(write(sendFormattedError(SPK_006, s"Spok $spokId already respoked", Some(RESPOK)))))
      case IsValidSpokAck(SPOK_NOT_FOUND, _, _) => Future(TextMessage(write(sendFormattedError(SPK_001, spokNotFound(spokId), Some(RESPOK)))))
      case IsValidSpokAck(REMOVED, _, _) => Future(TextMessage(write(sendFormattedError(SPK_001, s"Spok $spokId has been removed", Some(RESPOK)))))
      case IsValidSpokAck(_, false, _) => Future(TextMessage(write(sendFormattedError(SPK_016, DISABLED_SPOK, Some(RESPOK)))))
      case IsValidSpokAck(_, true, edgeOpt) => persistRespokEventHandler(command, spokId, respok, userId, edgeOpt)
    }
    respokResult.flatMap(identity)
  }

  private def persistRespokEventHandler(command: ActorRef, spokId: String, respok: Respok, userId: String, edgeOpt: Option[Edge]): Future[TextMessage] = {
    val respokResponseJson = ask(command, CreateRespok(respok, spokId, userId, edgeOpt)).mapTo[SpokAck]
    respokResponseJson.map {
      case RespokCreateSuccess(respokResult, spokId) => {
        logger.info(s"Respok done successfully for $spokId and forwarding response $respokResult")
        redisFactory.storeSubscriber(respokResult.spokId, userId)
        spokLogger.upsertSubscriberDetails(respokResult.spokId, userId)
        TextMessage(write(generateCommonResponseForCaseClass(SUCCESS, Some(List()),
          Some(Response(respokResponse = Some(RespokResponse(respokResult.spokId, respokResult.counters, respokResult.mentionUserId)))), Some(RESPOK))))
      }
      case RespokCreateFailure(errorId, err) => TextMessage(write(sendFormattedError(errorId, err.getMessage, Some(RESPOK))))
    }
  }

  /**
   * @param command
   * @param query
   * @return unspok success response if all the validations pass and the unspok happens else error message
   */
  private def unspokHandler(command: ActorRef, query: ActorRef, userId: String, json: String): Future[TextMessage] = {
    val spokIdOpt = (parse(json) \ SPOK_ID).extractOpt[String]
    spokIdOpt match {
      case Some(spokId) => {
        val unspok = extractUnspokDetailsWithValidation(json)
        unspok match {
          case (Some(unspokContent), _) => eventuateUnspokHandler(command, query, spokId, unspokContent, userId)
          case (None, message) => Future(TextMessage(message))
        }
      }
      case None => Future(TextMessage(write(sendJsonErrorWithEmptyData(Some(UNSPOK)))))
    }
  }

  private def eventuateUnspokHandler(command: ActorRef, query: ActorRef, spokId: String,
    unspok: Unspok, userId: String): Future[TextMessage] = {

    val futureResponse = ask(query, IsValidSpokAndSendStatus(userId, spokId)).mapTo[IsValidSpokAck]
    val unspokResult = futureResponse.map { response =>
      logger.info(s"$userId Getting status of spok ${response.status}")
      response match {
        case IsValidSpokAck(SPOK_NOT_FOUND, _, _) => Future(TextMessage(write(sendFormattedError(SPK_001, spokNotFound(spokId), Some(UNSPOK)))))
        case IsValidSpokAck(RESPOKED, _, _) => Future(TextMessage(write(sendFormattedError(SPK_006, s"Spok $spokId already respoked", Some(UNSPOK)))))
        case _ => eventuateUnspokHandlerForCases(command, userId, unspok, response, spokId)
      }
    }
    unspokResult.flatMap(identity)
  }

  private def eventuateUnspokHandlerForCases(command: ActorRef, userId: String, unspok: Unspok, response: IsValidSpokAck, spokId: String) = {
    response match {
      case IsValidSpokAck(REMOVED, _, _) => Future(TextMessage(write(sendFormattedError(SPK_001, s"Spok $spokId has been removed", Some(UNSPOK)))))
      case IsValidSpokAck(_, false, _) => Future(TextMessage(write(sendFormattedError(SPK_016, DISABLED_SPOK, Some(UNSPOK)))))
      case IsValidSpokAck(status, true, _) => persistUnspokEventHandler(command, spokId, unspok, userId, status)
    }
  }

  private def persistUnspokEventHandler(command: ActorRef, spokId: String, unspok: Unspok, userId: String, status: String): Future[TextMessage] = {
    logger.info(s"$userId is trying to unspok the spok $spokId")
    val unspokResponseJson = ask(command, ExecuteUnspok(unspok, spokId, userId, status)).mapTo[SpokAck]
    unspokResponseJson.map {
      case UnspokPerformSuccess(unspok, spokId) =>
        TextMessage(write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(Response(unspokResponse = Some(unspok))), Some(UNSPOK))))
      case UnspokPerformFailure(errorId, err) =>
        TextMessage(write(generateCommonResponseForError(FAILED, Some(List(Error(errorId, err.getMessage))), None, Some(UNSPOK))))
    }
  }

  def greeter: Flow[Message, Message, Any] =
    Flow[Message].mapConcat {
      case tm: TextMessage =>
        TextMessage(Source.single("Hello ping check ") ++ tm.textStream ++ Source.single("!")) :: Nil
    }

  /**
   *
   * @param spokJson
   * @return the URL spok if the Spok if of URL type otherwise will return a text Spok if Spok of text type
   */
  def createSpok(spokJson: String, file: Option[File]): (Option[Spok], HttpResponse) = {
    val interimSpokOpt = parse(spokJson).extractOpt[InterimSpok]
    interimSpokOpt match {
      case Some(interimSpok) => {
        val spok = Spok(interimSpok.contentType, interimSpok.groupId, interimSpok.visibility, interimSpok.ttl, interimSpok.headerText,
          interimSpok.file, interimSpok.text, interimSpok.url, interimSpok.poll, interimSpok.riddle, interimSpok.geo)
        val validSpok = isValidSpok(spok)
        validSpok match {
          case (true, message) =>
            val (mediaUrl, mediaUrlError) = if (file.isDefined) {
              fileUploadUtility.mediaUpload(file.get)
            } else {
              (None, None)
            }
            if (mediaUrlError.nonEmpty) {
              (None, HttpResponse(StatusCodes.BadRequest, entity = HttpEntity(
                ContentTypes.`application/json`,
                write(generateCommonResponseForError(FAILED, mediaUrlError, None, Some(CREATE_SPOK)))
              )))
            } else {
              val spokNew = if (mediaUrl.isDefined) {
                spok.copy(file = mediaUrl)
              } else {
                spok
              }
              (Some(spokNew), HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, VALID_SPOK)))
            }
          case (false, message) => (None, HttpResponse(StatusCodes.BadRequest, entity = HttpEntity(
            ContentTypes.`application/json`,
            write(generateCommonResponseForError(FAILED, message, None, Some(CREATE_SPOK)))
          )))
        }
      }
      case None => (None, HttpResponse(StatusCodes.BadRequest, entity = HttpEntity(
        ContentTypes.`application/json`,
        write(sendJsonErrorWithEmptyData(Some(CREATE_SPOK)))
      )))
    }
  }

  def getMentionUserList(spokJson: String): List[String] = {
    if ((parse(spokJson) \ ("mention")).extract[List[String]].length > 0) {
      (parse(spokJson) \ ("mention")).extract[List[String]]
    } else {
      Nil
    }
  }

  /**
   *
   * @param respokJson
   * @return either the respok object if the validations are successful or none and error message when validation fails
   */
  private def extractRespokDetailsWithValidation(respokJson: String): (Option[Respok], TextMessage) = {
    val respokContentOpt = parse(respokJson).extractOpt[InterimRespok]
    respokContentOpt match {
      case Some(respokContent) => {
        val validRespok = validateRespokDetails(respokContent)
        validRespok match {
          case (true, message) => {
            val respok = Respok(respokContent.groupId, respokContent.visibility, respokContent.text, respokContent.geo, respokContent.mention)
            (Some(respok), TextMessage(write(VALID_SPOK)))
          }
          case (false, message) => (None, TextMessage(write(generateCommonResponseForError(FAILED, message, None, Some(RESPOK)))))
        }
      }
      case None => (None, TextMessage(write(sendJsonErrorWithEmptyData(Some(RESPOK)))))
    }
  }

  /**
   *
   * @param unspokJson
   * @return either the unspok object if the validations are successful or none and error message when validation fails
   */
  private def extractUnspokDetailsWithValidation(unspokJson: String): (Option[Unspok], String) = {
    parse(unspokJson).extractOpt[InterimUnspok] match {
      case Some(unspokContent) => {
        val validUnspok = validateUnspokDetails(unspokContent)
        validUnspok match {
          case (true, message) => {
            val unspok = Unspok(unspokContent.geo)
            (Some(unspok), write(VALID_SPOK))
          }
          case (false, message) => (None, write(generateCommonResponseForError(FAILED, message, None, Some(UNSPOK))))
        }
      }
      case None => (None, write(sendJsonErrorWithEmptyData(Some(UNSPOK))))
    }
  }

  /**
   * This function will add a comment on a spok
   *
   * @param command spok command actorRef
   * @param userId  user id of an user
   * @return json of comment added success if comment is addesd succeffully else the erroe meassage
   */
  def addCommentHandler(command: ActorRef, query: ActorRef, userId: String, json: String): Future[TextMessage] = {
    val isSpokerSuspended: Future[IsUserSuspendedAsk] = ask(query, IsUserSuspended(userId)).mapTo[IsUserSuspendedAsk]
    val response = isSpokerSuspended.map { response =>
      response.status match {
        case SPOKER_SUSPENDED => Future(TextMessage(write(sendFormattedError(SPK_014, ACTION_IMPOSSIBLE, Some(ADD_COMMENT)))))
        case SPOKER_NOT_SUSPENDED =>
          val spokIdOpt = (parse(json) \ (SPOK_ID)).extractOpt[String]
          spokIdOpt match {
            case Some(spokInstanceId) => {
              val validComment = extractCommentWithValidation(json, getUUID())
              validComment match {
                case (Some(comment), _) => eventuateAddCommentHandler(command, spokInstanceId, userId, comment)
                case (None, message) => Future(TextMessage(write(generateCommonResponseForError(FAILED, message, None, Some(ADD_COMMENT)))))
              }
            }
            case None => Future(TextMessage(write(sendJsonErrorWithEmptyData(Some(ADD_COMMENT)))))
          }
        case _ => Future(TextMessage(write(sendFormattedError(SPK_119, UNABLE_COMMENTING_SPOK, Some(ADD_COMMENT)))))
      }
    }
    response.flatMap(identity)
  }

  private def eventuateAddCommentHandler(command: ActorRef, spokInstanceId: String, userId: String, comment: Comment): Future[TextMessage] = {
    val addComment = ask(command, CreateComment(comment, spokInstanceId, userId)).mapTo[SpokAck]
    addComment.map { response =>
      response match {
        case AddCommentSuccess(comment) => {
          if (comment.isDefined && comment.get.spok.spokId.nonEmpty) {
            redisFactory.storeSubscriber(comment.get.spok.spokId, userId)
            spokLogger.upsertSubscriberDetails(comment.get.spok.spokId, userId)
          }
          TextMessage(write(generateCommonResponseForCaseClass(SUCCESS, Some(List()),
            Some(Response(addCommentResponse = comment)), resource = Some(ADD_COMMENT))))
        }
        case addCommentFailure: AddCommentFailure => TextMessage(write(sendFormattedError(
          addCommentFailure.errorCode, addCommentFailure.cause.getMessage, Some(ADD_COMMENT)
        )))
      }
    }
  }

  private def extractCommentWithValidation(commentJson: String, commentId: String): (Option[Comment], Option[List[Error]]) = {
    try {
      val text = (parse(commentJson) \ (TEXT)).extract[String]
      val geo = (parse(commentJson) \ (GEO)).extract[Geo]
      val validComment = isValidComment(text, geo)
      val mentionedUserId = getMentionUserList(commentJson)
      validComment match {
        case Some(Nil) => (Some(Comment(commentId, text, geo, mentionedUserId)), None)
        case Some(errorList) => (None, Some(errorList))
      }
    } catch {
      case ex: Exception => (None, Some(List(Error(PRS_001, INVALID_JSON))))
    }
  }

  /**
   * Update Comment Handler
   *
   * @param command
   * @param userId Id of the user updating the comment (only the user who created the comment can update the comment)
   * @return
   */
  def updateCommentHandler(command: ActorRef, query: ActorRef, userId: String, json: String): Future[TextMessage] = {

    val isSpokerSuspended: Future[IsUserSuspendedAsk] = ask(query, IsUserSuspended(userId)).mapTo[IsUserSuspendedAsk]

    val response = isSpokerSuspended.map { response =>
      response.status match {
        case SPOKER_SUSPENDED => Future(TextMessage(write(sendFormattedError(SPK_014, ACTION_IMPOSSIBLE, Some(UPDATE_COMMENT)))))
        case SPOKER_NOT_SUSPENDED =>

          val commentIdOpt = (parse(json) \ (COMMENT_ID)).extractOpt[String]
          commentIdOpt match {
            case Some(commentId) => {
              val validComment = extractCommentWithValidation(json, commentId)
              validComment match {
                case (Some(comment), _) => eventuateUpdateCommentHandler(command, userId, comment)
                case (None, message) => Future(TextMessage(write(generateCommonResponseForError(FAILED, message, None, resource = Some(UPDATE_COMMENT)))))
              }
            }
            case None => Future(TextMessage(write(sendJsonErrorWithEmptyData(Some(UPDATE_COMMENT)))))
          }

        case _ => Future(TextMessage(write(sendFormattedError(SPK_120, "Unable updating comment (generic error)", Some(UPDATE_COMMENT)))))
      }
    }
    response.flatMap(identity)
  }

  private def eventuateUpdateCommentHandler(command: ActorRef, userId: String, comment: Comment): Future[TextMessage] = {
    val commentUpdate = ask(command, UpdateComment(comment, userId)).mapTo[SpokAck]
    commentUpdate.map { response =>
      response match {
        case UpdateCommentSuccess(updateComment) => TextMessage(write(generateCommonResponseForCaseClass(
          SUCCESS, Some(List()), Some(Response(updateCommentResponse = updateComment)), resource = Some(UPDATE_COMMENT)
        )))
        case updateCommentFailure: UpdateCommentFailure =>
          TextMessage(write(sendFormattedError(updateCommentFailure.errorCode, updateCommentFailure.cause.getMessage, Some(UPDATE_COMMENT))))
      }
    }
  }

  def removeCommentHandler(command: ActorRef, query: ActorRef, userId: String, json: String): Future[TextMessage] = {
    val isSpokerSuspended: Future[IsUserSuspendedAsk] = ask(query, IsUserSuspended(userId)).mapTo[IsUserSuspendedAsk]

    val response = isSpokerSuspended.map { response =>
      response.status match {
        case SPOKER_SUSPENDED => Future(TextMessage(write(sendFormattedError(SPK_014, ACTION_IMPOSSIBLE, Some(REMOVE_COMMENT)))))
        case SPOKER_NOT_SUSPENDED =>

          val (removeComment, message) = extractRemoveCommentDetailsWithValidation(json)
          removeComment match {
            case Some(validCommentRemove) => {
              val commentRemove = ask(command, RemoveComment(validCommentRemove.commentId, userId, validCommentRemove.geo)).mapTo[SpokAck]
              commentRemove map {
                case RemoveCommentSuccess(commentRemoved) => TextMessage(write(generateCommonResponseForCaseClass(
                  SUCCESS,
                  Some(List()), Some(Response(removeCommentResponse = commentRemoved)), resource = Some(REMOVE_COMMENT)
                )))
                case removeCommentFailure: RemoveCommentFailure => TextMessage(write(sendFormattedError(
                  removeCommentFailure.errorCode, removeCommentFailure.cause.getMessage, resource = Some(REMOVE_COMMENT)
                )))
              }
            }
            case None => Future(TextMessage(message))
          }

        case _ => Future(TextMessage(write(sendFormattedError(SPK_121, UNABLE_REMOVING_COMMENT, Some(REMOVE_COMMENT)))))
      }
    }

    response.flatMap(identity)
  }

  private def extractRemoveCommentDetailsWithValidation(removeCommentJson: String): (Option[InterimRemoveComment], String) = {
    parse(removeCommentJson).extractOpt[InterimRemoveComment] match {
      case Some(removeComment) => {
        val validRemoveComment = validateGeoLocationForAll(removeComment.geo)
        validRemoveComment match {
          case (true, message) => (Some(removeComment), write(VALID_SPOK))
          case (false, message) => (None, write(generateCommonResponseForError(FAILED, message, None, Some(REMOVE_COMMENT))))
        }
      }
      case None => (None, write(sendJsonErrorWithEmptyData(Some(REMOVE_COMMENT))))
    }
  }

  /**
   * Handler to store an answer for a question of a poll spok
   *
   * @param command
   * @param userId
   * @return
   */
  def answerPollQuestionHandler(command: ActorRef, userId: String, json: String): Future[TextMessage] = {
    val questionIdOpt = (parse(json) \ (QUESTION_ID)).extractOpt[String]
    questionIdOpt match {
      case Some(questionId) => {
        val pollAnswer = extractPollAnswerWithValidation(json, questionId)
        pollAnswer match {
          case (Some(answer), message) => eventuatePollAnswerHandler(command, questionId, userId, answer)
          case (None, errorList) => Future(TextMessage(write(generateCommonResponseForError(FAILED, errorList, None, Some(ANSWER_POLL)))))
        }
      }
      case None => Future(TextMessage(write(sendJsonErrorWithEmptyData(Some(ANSWER_POLL)))))
    }
  }

  private def extractPollAnswerWithValidation(pollAnswerJson: String, questionId: String): (Option[UserPollAnswer], Option[List[Error]]) = {

    val pollAnswer = parse(pollAnswerJson).extractOpt[UserPollAnswer]
    pollAnswer match {
      case Some(answer) => validateGeoLocationForAll(answer.geo) match {
        case (true, None) => (Some(answer), None)
        case (false, errorList) => (None, errorList)
      }
      case None => (None, Some(List(Error(PRS_001, INVALID_JSON))))
    }
  }

  private def eventuatePollAnswerHandler(command: ActorRef, questionId: String, userId: String,
    userPollAnswer: UserPollAnswer): Future[TextMessage] = {

    val savePollAnswer = ask(command, SavePollAnswer(questionId, userId, userPollAnswer)).mapTo[SpokAck]
    savePollAnswer.map {
      case PollAnswerSavedSuccess(spokId) => TextMessage(write(generateCommonResponseForCaseClass(SUCCESS, Some(List()),
        Some(Message("Response Saved")), Some(ANSWER_POLL))))
      case pollAnswerSavedFailure: PollAnswerSavedFailure => TextMessage(write(sendFormattedError(
        pollAnswerSavedFailure.errorCode, pollAnswerSavedFailure.cause.getMessage, Some(ANSWER_POLL)
      )))
    }
  }

  /**
   * Handler to view a poll question if the question id is valid or return the appropriate error message
   *
   * @param query
   * @param questionId of the question to be viewed
   * @param userId
   * @return
   */
  def viewPollQuestionHandler(query: ActorRef, questionId: String, userId: String): Future[String] = {

    val viewPollQuestionResponse = ask(query, ViewPollQuestionDetails(questionId, userId))
    val viewPollQuestionOpt = viewPollQuestionResponse.map {
      case viewPollQuestionSuccess: ViewPollQuestionSuccess =>
        write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(viewPollQuestionSuccess.viewPollQuestion), Some(VIEW_POLL_QUESTION)))
      case viewPollQuestionFailure: ViewPollQuestionFailure =>
        write(sendFormattedError(viewPollQuestionFailure.errorId, viewPollQuestionFailure.errorMessage, Some(VIEW_POLL_QUESTION)))
    }
    logger.info("Completed Request to view a poll question")
    viewPollQuestionOpt
  }

  /**
   * @param query
   * @return subscribe/unsubscribe success if all validations pass and subscribe/unsubscribe is done otherwise the error message
   */
  def eventuateSubscribeUnsubscribeHandler(query: ActorRef, userId: String, json: String): Future[TextMessage] = {

    val isSpokerSuspended: Future[IsUserSuspendedAsk] = ask(query, IsUserSuspended(userId)).mapTo[IsUserSuspendedAsk]

    val response = isSpokerSuspended.map { response =>
      response.status match {
        case SPOKER_SUSPENDED => Future(TextMessage(write(sendFormattedError(SPK_014, ACTION_IMPOSSIBLE, Some(SUBSCRIBE)))))
        case SPOKER_NOT_SUSPENDED =>

          val (subscribeDetails, message) = extractSubscribeDetailsWithValidation(json)
          val launchedTime = System.currentTimeMillis()
          subscribeDetails match {
            case Some(subscribe) => {
              val futureResponse = ask(query, IsValidSpokWithEnabledFlag(subscribe.spokId)).mapTo[IsValidSpokWithEnabledAck]
              futureResponse flatMap {
                response =>
                  response.status match {
                    case true => handleValidSubscribeUnsubscribeRequest(subscribe, userId, launchedTime)
                    case false => Future(TextMessage(write(sendFormattedError(SPK_001, spokNotFound(subscribe.spokId), Some(SUBSCRIBE)))))
                  }
              }
            }
            case None => Future(TextMessage(message))
          }

        case _ => Future(TextMessage(write(sendFormattedError(SPK_123, "Unable susbcribing to /unsubscribing from spok feed (generic error).", Some(SUBSCRIBE)))))
      }
    }
    response.flatMap(identity)
  }

  private def handleValidSubscribeUnsubscribeRequest(subscribe: InterimSubscribeUnsubscribe, userId: String, launchedTime: Long) = {
    redisFactory.isSubscriberExist(subscribe.spokId, userId).map {
      case true =>
        redisFactory.removeSubscriber(subscribe.spokId, userId)
        spokLogger.insertSpokEvent(userId, subscribe.spokId, launchedTime, UNSUBSCRIBED_EVENT, subscribe.geo)
        spokLogger.removeSubscriberDetails(subscribe.spokId, userId)
        TextMessage(write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(UnsubscribeSpokFeedSuccess(UNSUBSCRIBE_SPOK_FEED)), Some(SUBSCRIBE))))
      case false =>
        redisFactory.storeSubscriber(subscribe.spokId, userId)
        spokLogger.insertSpokEvent(userId, subscribe.spokId, launchedTime, SUBSCRIBED_EVENT, subscribe.geo)
        spokLogger.upsertSubscriberDetails(subscribe.spokId, userId)
        TextMessage(write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(SubscribeSpokFeedSuccess(SUBSCRIBE_SPOK_FEED)), Some(SUBSCRIBE))))
    }

  }

  private def extractSubscribeDetailsWithValidation(subscribeUnsubscribeJson: String): (Option[InterimSubscribeUnsubscribe], String) = {
    parse(subscribeUnsubscribeJson).extractOpt[InterimSubscribeUnsubscribe] match {
      case Some(disableSpok) => {
        val validRemoveComment = validateGeoLocationForAll(disableSpok.geo)
        validRemoveComment match {
          case (true, message) => (Some(disableSpok), write(SUBSCRIBE))
          case (false, message) => (None, write(generateCommonResponseForError(FAILED, message, None, Some(SUBSCRIBE))))
        }
      }
      case None => (None, write(sendJsonErrorWithEmptyData(Some(SUBSCRIBE))))
    }
  }

  /**
   * This method will handle spok disable query
   *
   * @param command
   * @param userId
   * @return
   */
  def eventuateDisableSpokHandler(command: ActorRef, query: ActorRef, userId: String, json: String): Future[TextMessage] = {

    val isSpokerSuspended: Future[IsUserSuspendedAsk] = ask(query, IsUserSuspended(userId)).mapTo[IsUserSuspendedAsk]

    val response = isSpokerSuspended.map { response =>
      response.status match {
        case SPOKER_SUSPENDED => Future(TextMessage(write(sendFormattedError(SPK_014, ACTION_IMPOSSIBLE, Some(DISABLE)))))
        case SPOKER_NOT_SUSPENDED =>

          val (disableSpokOpt, message) = extractDisableSpokDetailsWithValidation(json)
          disableSpokOpt match {
            case Some(disableSpok) => {
              val launchedTime = System.currentTimeMillis()
              val futureResponse = ask(command, Disable(disableSpok.spokId, userId, launchedTime, disableSpok.geo)).mapTo[SpokAck]
              futureResponse map {
                case DisableSpokSuccess(spokDisableResponse) =>
                  TextMessage(write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(DisableSpokSuccess(spokDisableResponse)), Some(DISABLE))))
                case disableSpokFailure: DisableSpokFailure => TextMessage(write(
                  sendFormattedError(disableSpokFailure.errorCode, disableSpokFailure.cause.getMessage, Some(DISABLE))
                ))
              }
            }
            case None => Future(TextMessage(message))
          }

        case _ => Future(TextMessage(write(sendFormattedError(SPK_115, UNABLE_DISABLING_SPOK, Some(DISABLE)))))
      }
    }
    response.flatMap(identity)
  }

  private def extractDisableSpokDetailsWithValidation(disableSpokJson: String): (Option[InterimDisableSpok], String) = {
    parse(disableSpokJson).extractOpt[InterimDisableSpok] match {
      case Some(disableSpok) => {
        val validRemoveComment = validateGeoLocationForAll(disableSpok.geo)
        validRemoveComment match {
          case (true, message) => (Some(disableSpok), write(DISABLE))
          case (false, message) => (None, write(generateCommonResponseForError(FAILED, message, None, Some(DISABLE))))
        }
      }
      case None => (None, write(sendJsonErrorWithEmptyData(Some(DISABLE))))
    }
  }

  def eventuateRemoveSpokFromWallHandler(query: ActorRef, command: ActorRef, userId: String, json: String): Future[TextMessage] = {

    val isSpokerSuspended: Future[IsUserSuspendedAsk] = ask(query, IsUserSuspended(userId)).mapTo[IsUserSuspendedAsk]

    val response = isSpokerSuspended.map { response =>
      response.status match {
        case SPOKER_SUSPENDED => Future(TextMessage(write(sendFormattedError(SPK_014, ACTION_IMPOSSIBLE, Some(REMOVE_SPOK)))))
        case SPOKER_NOT_SUSPENDED =>

          val (removeSpokOpt, message) = extractRemoveSpokDetailsWithValidation(json)
          removeSpokOpt match {
            case Some(removeSpok) => {
              val futureResponse = ask(query, IsValidAbsoluteSpok(removeSpok.spokId)).mapTo[IsValidAbsoluteSpokAck]
              futureResponse flatMap (response => checkIfSpokValidOrNot(response, command, removeSpok.spokId, userId, removeSpok.geo))
            }
            case None => Future(TextMessage(message))
          }

        case _ => Future(TextMessage(write(sendFormattedError(SPK_116, UNABLE_REMOVING_SPOK, Some(REMOVE_SPOK)))))
      }
    }
    response.flatMap(identity)
  }

  private def extractRemoveSpokDetailsWithValidation(removeSpokJson: String): (Option[InterimRemoveSpok], String) = {
    parse(removeSpokJson).extractOpt[InterimRemoveSpok] match {
      case Some(removeSpok) => {
        val validRemoveComment = validateGeoLocationForAll(removeSpok.geo)
        validRemoveComment match {
          case (true, message) => (Some(removeSpok), write(REMOVE_SPOK))
          case (false, message) => (None, write(generateCommonResponseForError(FAILED, message, None, Some(REMOVE_SPOK))))
        }
      }
      case None => (None, write(sendJsonErrorWithEmptyData(Some(REMOVE_SPOK))))
    }
  }

  private def checkIfSpokValidOrNot(response: IsValidAbsoluteSpokAck, command: ActorRef, spokId: String, userId: String, geo: Geo) = {
    response.status match {
      case SPOK_VALID => removeValidSpokFromWall(command, spokId, userId, geo)
      case DISABLED_SPOK => Future(TextMessage(write(sendFormattedError(SPK_016, DISABLED_SPOK, Some(REMOVE_SPOK)))))
      case SPOK_NOT_FOUND => Future(TextMessage(write(sendFormattedError(SPK_001, spokNotFound(spokId), Some(REMOVE_SPOK)))))
    }
  }

  private def removeValidSpokFromWall(command: ActorRef, spokId: String, userId: String, geo: Geo) = {
    val launchedTime = System.currentTimeMillis()
    val removeResponse = ask(command, RemoveSpok(spokId, userId, launchedTime, geo)).mapTo[SpokAck]
    removeResponse.map { result =>
      result match {
        case removeWallSpokSuccess: RemoveWallSpokSuccess => TextMessage(write(generateCommonResponseForCaseClass(
          SUCCESS, Some(List()), Some(Response(removeSpokResponse = Some(removeWallSpokSuccess.removeWallSpokResponse))), Some(REMOVE_SPOK)
        )))
        case removeWallSpokFailure: RemoveWallSpokFailure => TextMessage(write(sendFormattedError(
          removeWallSpokFailure.errorCode, removeWallSpokFailure.cause.getMessage, Some(REMOVE_SPOK)
        )))
      }
    }
  }

  private def spokNotFound(spokId: String) = s"Spok $spokId not found."

  /**
   * This method will return stats of spok.
   *
   * @param query
   * @param spokId
   * @return
   */
  def spokStatsHandler(query: ActorRef, spokId: String): Future[String] = {

    val futureResponse = ask(query, IsValidAbsoluteSpok(spokId)).mapTo[IsValidAbsoluteSpokAck]
    val spokInstanceStatsResult: Future[Future[String]] = futureResponse.map { response =>
      response.status match {
        case SPOK_VALID =>
          val spokInstanceStatsResponse = ask(query, GetSpokStats(spokId)).mapTo[SpokStats]
          spokInstanceStatsResponse.map { statsResponse =>
            write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(statsResponse.spokStats), Some(SPOK_STATS)))
          }
        case DISABLED_SPOK => Future(write(sendFormattedError(SPK_016, DISABLED_SPOK, Some(SPOK_STATS))))
        case SPOK_NOT_FOUND => Future(write(sendFormattedError(SPK_001, spokNotFound(spokId), Some(SPOK_STATS))))
      }
    }
    spokInstanceStatsResult.flatMap(identity)
  }

  /**
   * This method will get 10 comments of a spok
   *
   * @param query
   * @param spokId
   * @param pos
   * @return
   */

  def getCommentsHandler(query: ActorRef, spokId: String, pos: String): Future[String] = {

    val futureResponse = ask(query, IsValidAbsoluteSpok(spokId)).mapTo[IsValidAbsoluteSpokAck]
    val reSpokersResult: Future[Future[String]] = futureResponse.map { response =>
      response.status match {
        case SPOK_VALID =>
          val commentsResponse = ask(query, GetComments(spokId, pos)).mapTo[GetCommentsRes]
          commentsResponse.map { res =>
            handleGetCommentsValidRequest(spokId, res.commentsResponse)
          }
        case DISABLED_SPOK => Future(write(sendFormattedError(SPK_002, s"Spok $spokId is not available anymore.", Some(GET_COMMENTS))))
        case SPOK_NOT_FOUND => Future(write(sendFormattedError(SPK_001, spokNotFound(spokId), Some(GET_COMMENTS))))
      }
    }
    reSpokersResult.flatMap(identity)
  }

  private def handleGetCommentsValidRequest(spokId: String, commentsResponse: Option[CommentsResponse]) = {
    commentsResponse match {
      case Some(response) => write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(response), Some(GET_COMMENTS)))
      case None => write(sendFormattedError(SPK_122, s"Unable loading spok $spokId comments (generic error).", Some(GET_COMMENTS)))
    }
  }

  /**
   * This function is used to view 10 re-spokers of a spok.
   *
   * @param query
   * @param spokId
   * @param pos
   * @return
   */
  def getReSpokersHandler(query: ActorRef, spokId: String, pos: String): Future[String] = {

    val futureResponse = ask(query, IsValidAbsoluteSpok(spokId)).mapTo[IsValidAbsoluteSpokAck]
    val reSpokersResult: Future[Future[String]] = futureResponse.map { response =>
      response.status match {
        case SPOK_VALID =>
          val reSpokersResponse = ask(query, GetReSpokers(spokId, pos)).mapTo[ReSpokersRes]
          reSpokersResponse.map { res =>
            handleViewRespokersValidRequest(spokId, res.reSpokerResponse)
          }
        case DISABLED_SPOK => Future(write(sendFormattedError(SPK_002, s"Spok $spokId is not available anymore.", Some(LOAD_SPOKERS))))
        case SPOK_NOT_FOUND => Future(write(sendFormattedError(SPK_001, spokNotFound(spokId), Some(LOAD_SPOKERS))))
      }
    }
    reSpokersResult.flatMap(identity)
  }

  private def handleViewRespokersValidRequest(spokId: String, reSpokerResponse: Option[ReSpokerResponse]) = {
    reSpokerResponse match {
      case Some(respokerResponseData) => write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(respokerResponseData), Some(LOAD_SPOKERS)))
      case None => write(sendFormattedError(SPK_102, s"Unable loading spok $spokId re-spokers (generic error).", Some(LOAD_SPOKERS)))
    }
  }

  /**
   * This function is used to view 10 scoped user of a spok.
   *
   * @param query
   * @param spokId
   * @param pos
   * @return
   */
  def scopedUsersHandler(query: ActorRef, spokId: String, pos: String): Future[String] = {

    val futureResponse = ask(query, IsValidAbsoluteSpok(spokId)).mapTo[IsValidAbsoluteSpokAck]
    val scopedUsersResult: Future[Future[String]] = futureResponse.map { response =>
      response.status match {
        case SPOK_VALID =>
          val scopedUsersResponse = ask(query, GetScopedUsers(spokId, pos)).mapTo[ScopedUsersRes]
          scopedUsersResponse.map { res =>
            handleViewScopedUsersValidRequest(spokId, res.scopedUsersResponse)
          }
        case DISABLED_SPOK => Future(write(sendFormattedError(SPK_002, s"Spok $spokId is not available anymore.", Some(LOAD_SCOPED_USERS))))
        case SPOK_NOT_FOUND => Future(write(sendFormattedError(SPK_001, spokNotFound(spokId), Some(LOAD_SCOPED_USERS))))
      }
    }
    scopedUsersResult.flatMap(identity)
  }

  private def handleViewScopedUsersValidRequest(spokId: String, scopedUsersResponse: Option[ScopedUsersResponse]) = {
    scopedUsersResponse match {
      case Some(scopedUsersData) => write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(scopedUsersData), Some(LOAD_SCOPED_USERS)))
      case None => write(sendFormattedError(SPK_103, s"Unable loading spok $spokId scoped users (generic error).", Some(LOAD_SCOPED_USERS)))
    }
  }

  /**
   * This method is used for load spoks on stack.
   *
   * @param query
   * @param pos
   * @param userId
   * @return
   */
  def spokStackHandler(query: ActorRef, pos: String, userId: String): Future[String] = {

    val futureResponse = ask(query, GetSpokStack(userId, pos)).mapTo[SpoksStack]
    val spokInstanceStatsResult: Future[Future[String]] = futureResponse.map { response =>
      response.spoksStackResponse match {
        case Some(spokStackResponse) => Future(write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(spokStackResponse), Some(LOAD_STACK))))
        case None => Future(write(sendFormattedError(SPK_105, STACK_ERROR, Some(LOAD_STACK))))
      }
    }
    spokInstanceStatsResult.flatMap(identity)
  }

  /**
   * This method is use get quick view of one spok.
   *
   * @param query
   * @param userId
   * @return
   */
  def viewShortSpok(query: ActorRef, spokId: String, targetUserId: String, userId: String): Future[String] = {

    val futureResponse = ask(query, IsValidSpokById(spokId)).mapTo[IsValidSpokByIdAck]
    val viewShortSpokResult = futureResponse.map { response =>
      response.status match {
        case SPOK_VALID => {
          val viewShortSpokResponse = ask(query, ViewShortSpok(spokId, targetUserId, userId, response.spokVertex)).mapTo[ViewShortSpokResponse]
          viewShortSpokResponse.map { res =>
            handleViewShortSpokValidRequest(spokId, targetUserId, res.viewShortSpok)
          }
        }
        case DISABLED_SPOK => Future(write(sendFormattedError(SPK_002, s"Spok $spokId is not available anymore.", Some(VIEW_SHORT_SPOK))))
        case SPOK_NOT_FOUND => Future(write(sendFormattedError(SPK_001, spokNotFound(spokId), Some(VIEW_SHORT_SPOK))))
      }
    }
    viewShortSpokResult.flatMap(identity)
  }

  private def handleViewShortSpokValidRequest(spokId: String, userId: String, viewShortSpok: Option[ViewSpok]) = {
    viewShortSpok match {
      case Some(viewSpok: ViewSpok) => {
        redisFactory.storeVisitiedUsers(viewSpok.id, userId)
        redisFactory.storeVisitiedUsers(spokId, userId)
        write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(viewSpok), Some(VIEW_SHORT_SPOK)))
      }
      case None => write(sendFormattedError(SPK_101, s"Unable loading spok $spokId (generic error).", Some(VIEW_SHORT_SPOK)))
    }
  }

  /**
   * This method will return view of spoker's wall.
   *
   * @param query
   * @param targetUserId
   * @param pos
   * @return
   */
  def viewSpokersWallHandler(query: ActorRef, targetUserId: String, pos: String, userId: String): Future[String] = {

    val userWallResponse = ask(query, ViewSpokersWall(targetUserId, pos, userId))
    userWallResponse.map { res =>
      res match {
        case viewUserWallSuccess: ViewSpokersWallSuccess =>
          write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(viewUserWallSuccess.usersWall), Some(VIEW_SPOKERS_WALL)))
        case viewUserWallFailure: ViewSpokersWallFailure =>
          write(sendFormattedError(viewUserWallFailure.errorCode, viewUserWallFailure.cause.getMessage, Some(VIEW_SPOKERS_WALL)))
      }
    }
  }

  /**
   * This function is used to view full spok.
   *
   * @param query
   * @param spokId
   * @param userId
   * @return
   */
  def viewFullSpok(query: ActorRef, spokId: String, targetUserId: String, userId: String): Future[String] = {
    val futureResponse = ask(query, IsValidSpokById(spokId)).mapTo[IsValidSpokByIdAck]
    val viewFullSpokResult = futureResponse.map { response =>
      response.status match {
        case SPOK_VALID => {
          val viewFullSpokResponse = ask(query, ViewFullSpokDetails(spokId, targetUserId, userId, response.spokVertex)).mapTo[ViewFullSpokResponse]
          viewFullSpokResponse.map { res =>
            handleViewFullSpokValidRequest(spokId, targetUserId, res.viewFullSpok)
          }
        }
        case DISABLED_SPOK => Future(write(sendFormattedError(SPK_002, s"Spok $spokId is not available anymore.", Some(VIEW_FULL_SPOK))))
        case SPOK_NOT_FOUND => Future(write(sendFormattedError(SPK_001, spokNotFound(spokId), Some(VIEW_FULL_SPOK))))
      }
    }
    viewFullSpokResult.flatMap(identity)
  }

  private def handleViewFullSpokValidRequest(spokId: String, userId: String, viewFullSpok: Option[ViewFullSpok]) = {
    viewFullSpok match {
      case Some(someViewFullSpok) => {
        redisFactory.storeVisitiedUsers(someViewFullSpok.id, userId)
        redisFactory.storeVisitiedUsers(spokId, userId)
        write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(someViewFullSpok), Some(VIEW_FULL_SPOK)))
      }
      case None => write(sendFormattedError(SPK_101, s"Unable loading spok $spokId (generic error).", Some(VIEW_FULL_SPOK)))
    }
  }

  /**
   * Method to view a spok poll stats
   * @param query
   * @param spokId of the poll spok
   * @param userId of user who wants to view (will success for only creator or spoker who has completed the spok)
   * @return
   */
  def viewPollStatsHandler(query: ActorRef, spokId: String, userId: String): Future[String] = {

    val viewPollStatResponse = ask(query, ViewPollStats(spokId, userId))
    viewPollStatResponse.map {
      case viewPollStatsSuccess: ViewPollStatsSuccess =>
        write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(viewPollStatsSuccess.pollStats), Some(POLL_RESULTS)))
      case viewPollStatsFailure: ViewPollStatsFailure =>
        write(sendFormattedError(viewPollStatsFailure.errorId, viewPollStatsFailure.errorMessage, Some(POLL_RESULTS)))
    }
  }

  def viewMySpokHandler(query: ActorRef, userId: String, pos: String): Future[String] = {
    val futureResponse = ask(query, GetMySpoks(userId, pos)).mapTo[SpoksStack]
    val spokInstanceStatsResult: Future[Future[String]] = futureResponse.map { response =>
      response.spoksStackResponse match {
        case Some(spoksResponse) => Future(write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(spoksResponse), Some(MY_SPOKS))))
        case None => Future(write(sendFormattedError(SPK_110, MY_SPOKS_ERROR, Some(MY_SPOKS))))
      }
    }
    spokInstanceStatsResult.flatMap(identity)
  }

  /**
   * Method to enable user to answer all the poll questions at once
   * @param command
   * @param userId
   * @param json
   * @return success message or failure message that comes from the backend
   */
  def answerAllPollQuestionHandler(command: ActorRef, userId: String, json: String): Future[TextMessage] = {
    val allAnswers = parse(json).extractOpt[AllAnswers]
    allAnswers match {
      case Some(answers) => {
        val pollAnswer = validateGeoLocationForAll(answers.geo)
        pollAnswer match {
          case (true, None) => eventuateAllPollQuestionsHandler(command, userId, answers)
          case (false, errorList) => Future(TextMessage(write(generateCommonResponseForError(FAILED, errorList, None, Some(ANSWERS_POLL)))))
        }
      }
      case None => Future(TextMessage(write(sendJsonErrorWithEmptyData(Some(ANSWERS_POLL)))))
    }
  }

  private def eventuateAllPollQuestionsHandler(command: ActorRef, userId: String,
    allAnswers: AllAnswers): Future[TextMessage] = {

    val savePollAnswer = ask(command, SaveAllPollAnswers(userId, allAnswers)).mapTo[SpokAck]
    savePollAnswer.map {
      case pollAllAnswersSavedSuccess: PollAllAnswersSavedSuccess => {
        val completePollResponse = CompletePollResponse("Response Saved")
        TextMessage(write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(completePollResponse), Some(ANSWERS_POLL))))
      }
      case pollAllAnswersSavedFailure: PollAllAnswersSavedFailure => TextMessage(write(sendFormattedError(
        pollAllAnswersSavedFailure.errorId, pollAllAnswersSavedFailure.errorMessage, Some(ANSWERS_POLL)
      )))
    }
  }

}
