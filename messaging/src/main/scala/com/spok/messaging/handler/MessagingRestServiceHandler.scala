package com.spok.messaging.handler

import javax.management.remote.TargetedNotification

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.model.ws.TextMessage.Strict
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpResponse, StatusCodes }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import com.spok.messaging.service.MessagingAck
import com.spok.model.Messaging._
import com.spok.persistence.factory.DSEGraphPersistenceFactoryApi
import com.spok.persistence.factory.messaging.MessagingApi
import com.spok.persistence.redis.RedisFactory
import com.spok.util.Constant._
import com.spok.util._

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global
import com.spok.messaging.service.MessagingViewCommands._
import com.spok.messaging.service.MessagingViewSuccessReplies._
import com.spok.messaging.service.MessagingViewFailureReplies._
import com.spok.messaging.service.MessagingActorSuccessReplies._
import com.spok.messaging.service.MessagingActorFailureReplies._
import com.spok.messaging.service.MessagingManagerCommands._
import com.spok.model.NotificationDetail
import com.spok.model.SpokModel.{ StandardResponseForCaseClass, StandardResponseForString }

trait MessagingRestServiceHandler extends JsonHelper with RandomUtil with LoggerUtil with ResponseUtil with ValidationUtil {

  implicit val system: ActorSystem

  implicit val materializer: ActorMaterializer
  implicit val timeout = Timeout(40 seconds)

  import akka.pattern.ask

  val redisFactory: RedisFactory = RedisFactory
  val dseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = DSEGraphPersistenceFactoryApi
  val messagingApi: MessagingApi = MessagingApi

  def detectRequestAndPerform(command: ActorRef, query: ActorRef, userId: String, phoneNumber: String): Flow[ws.Message, ws.Message, _] = {
    logger.info(s"Message service is connected for ${phoneNumber}. Now ready to perform message action!!")
    Flow[ws.Message].mapAsync(Runtime.getRuntime.availableProcessors()) {
      case TextMessage.Strict(txt) => handleUserRequests(command, query, userId, phoneNumber, txt)
      case TextMessage.Streamed(stream) => {
        stream
          .limit(Int.MaxValue) // Max frames we are willing to wait for
          .completionTimeout(50 seconds) // Max time until last frame
          .runFold("")(_ + _) // Merges the frames
          .flatMap { txt =>
            logger.info("Getting streamed message " + txt)
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
    val actionOpt = (parse(txt) \ (ACTION)).extractOpt[String]
    logger.info(s"${phoneNumber}:  is performing <${actionOpt}>!")
    actionOpt match {
      case Some(REMOVE_TALK) => removeAllMessagesOfATalk(command, userId, txt)
      case Some(REMOVE_MESSAGE) => removeAMessage(command, userId, txt)
      case Some(READ_MESSAGE) => readMessageUpdate(command, userId, txt)
      case Some(TYPING) => userTyping(userId, txt)
      case Some(someAction) => Future(TextMessage(write(sendFormattedError(ACT_101, MISSING_ACTION))))
      case None => Future(TextMessage(write(sendFormattedError(ACT_101, MISSING_ACTION))))
    }
  }

  /**
   * This method is used to send message.
   *
   * @param command
   * @param userId
   * @param jsonData
   * @return
   */

  def toHandleMessage(command: ActorRef, userId: String, jsonData: MessageJsonData, friendUserId: String): Future[HttpResponse] = {
    val isSpokerSuspended = dseGraphPersistenceFactoryApi.isUserSuspendAlready(userId)
    isSpokerSuspended match {
      case None =>
        Future(HttpResponse(StatusCodes.BadRequest, entity = HttpEntity(
          ContentTypes.`application/json`,
          write(sendFormattedError(MSG_103, UNABLE_SEND_MESSAGE, Some(TALK)))
        )))

      case Some(true) =>
        Future(HttpResponse(StatusCodes.BadRequest, entity = HttpEntity(
          ContentTypes.`application/json`,
          write(sendFormattedError(SPK_014, ACTION_IMPOSSIBLE, Some(TALK)))
        )))
      case Some(false) =>
        redisFactory.isTalkExist(userId, friendUserId).flatMap { flag =>
          flag match {
            case true =>
              val message = Message(userId, friendUserId, cassandraTimeUUID.toString, jsonData.message)
              messageHandler(command, message)
            case false => handleInitialTalk(command, userId, jsonData, friendUserId)
          }
        }
    }
  }

  /**
   * This method will handle valid talk message.
   *
   * @param command
   * @param message
   * @return
   */
  private def messageHandler(command: ActorRef, message: Message, isTalkInitiated: Boolean = true): Future[HttpResponse] = {
    if (!message.text.isEmpty) {
      val futureResponse = ask(command, SendMessage(message.senderId, message)).mapTo[MessagingAck]
      futureResponse.map(response => handleResponse(message.senderId, message.receiverId, response, isTalkInitiated))
    } else {
      Future(HttpResponse(StatusCodes.BadRequest, entity = HttpEntity(
        ContentTypes.`application/json`,
        write(sendFormattedError(MSG_001, MESSAGE_CANNOT_EMPTY, Some(TALK)))
      )))
    }
  }

  /**
   * This method will handle to initiate talk.
   *
   * @param command
   * @param userId
   * @param jsonData
   * @return
   */
  private def handleInitialTalk(command: ActorRef, userId: String, jsonData: MessageJsonData, friendUserId: String): Future[HttpResponse] = {
    val isFriend = (dseGraphPersistenceFactoryApi.isFollowingExists(userId, friendUserId) &&
      dseGraphPersistenceFactoryApi.isFollowingExists(friendUserId, userId))
    if (isFriend) {
      val message = Message(userId, friendUserId, cassandraTimeUUID.toString, jsonData.message)
      messageHandler(command, message, false)
    } else {
      Future(HttpResponse(StatusCodes.BadRequest, entity = HttpEntity(
        ContentTypes.`application/json`,
        write(sendFormattedError(MSG_003, NOT_A_FRIEND, Some(TALK)))
      )))
    }
  }

  /**
   * This method is related to handle http response.
   *
   * @param senderId
   * @param receiverId
   * @param response
   * @param isTalkInitiated
   * @return
   */
  private def handleResponse(senderId: String, receiverId: String, response: MessagingAck, isTalkInitiated: Boolean = true): HttpResponse = {
    response match {
      case createMessageSuccess: CreateMessageSuccess =>
        if (!isTalkInitiated) redisFactory.storeTalk(senderId, receiverId)
        HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, write(generateCommonResponseForCaseClass(SUCCESS, Some(List()),
          Some(createMessageSuccess.messageResponse), Some(TALK)))))
      case createMessageFailure: CreateMessageFailure =>
        HttpResponse(StatusCodes.BadRequest, entity = HttpEntity(
          ContentTypes.`application/json`,
          write(sendFormattedError(createMessageFailure.errorCode, createMessageFailure.cause.getMessage, Some(TALK)))
        ))
    }
  }

  /**
   * This method will return all the talks of a Spoker according to pagination
   *
   * @param query
   * @param pos
   * @param userId who wants to see all the talks
   * @return
   */
  def viewTalks(query: ActorRef, pos: Option[String], userId: String): Future[String] = {
    val viewTalksResponse = ask(query, ViewTalks(userId, pos.getOrElse("1")))
    viewTalksResponse map {
      case viewTalksSuccess: ViewTalksSuccess => write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(viewTalksSuccess), Some(TALKS)))
      case viewTalksFailure: ViewTalksFailure => write(sendFormattedError(viewTalksFailure.errorId, viewTalksFailure.errorMessage, Some(TALKS)))
    }
  }

  /**
   * This method will return all messages of a single talk
   *
   * @param query
   * @param targetUserId of the talk for which all messages are to be viewed
   * @param messageId
   * @param userId
   * @return
   */
  def viewSingleTalk(query: ActorRef, targetUserId: String, messageId: Option[String], userId: String, order: String): Future[String] = {
    val viewTalksResponse = ask(query, ViewSingleTalk(targetUserId, userId, messageId, order))
    viewTalksResponse map {
      case viewSingleTalkSuccess: ViewSingleTalkSuccess => write(generateCommonResponseForCaseClass(SUCCESS, Some(List()),
        Some(viewSingleTalkSuccess), Some(TALK)))
      case viewSingleTalkFailure: ViewSingleTalkFailure => write(sendFormattedError(
        viewSingleTalkFailure.errorId,
        viewSingleTalkFailure.errorMessage, Some(TALK)
      ))
    }
  }

  /**
   * Methof to remove all messages of a talk (Remove A Talk)
   *
   * @param command
   * @param userId
   * @param json
   * @return
   */
  private def removeAllMessagesOfATalk(command: ActorRef, userId: String, json: String): Future[TextMessage] = {
    val targetUserIdOpt = (parse(json) \ TARGETUSERID).extractOpt[String]
    targetUserIdOpt match {
      case Some(targetUserId) => {
        val futureResponse = ask(command, TalkRemove(targetUserId, userId))
        futureResponse map {
          case removeTalkSuccess: RemoveTalkSuccess =>
            TextMessage(write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(removeTalkSuccess), Some(REMOVE_TALK))))
          case removeTalkFailure: RemoveTalkFailure => TextMessage(write(sendFormattedError(
            removeTalkFailure.errorId,
            removeTalkFailure.errorMessage, Some(REMOVE_TALK)
          )))
        }
      }
      case None => Future(TextMessage(write(sendJsonErrorWithEmptyData(Some(REMOVE_TALK)))))
    }
  }

  /**
   * method to update the read time for message
   *
   * @param command
   * @param userId
   * @param json
   * @return
   */
  private def readMessageUpdate(command: ActorRef, userId: String, json: String): Future[TextMessage] = {
    val parameterOpt = parse(json).extractOpt[ReadMessageUpdate]
    parameterOpt match {
      case Some(userMessageId) => {
        val futureResponse = ask(command, ReadMessageUpdateFlag(userId, userMessageId.userId, userMessageId.messageId))
        futureResponse map {
          case updateReadMsgSuccess: UpdateReadMessageFlagSuccess =>
            TextMessage(write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(updateReadMsgSuccess), Some(READ_MESSAGE))))
          case updateReadMsgFailure: UpdateReadMessageFlagFailure => TextMessage(write(sendFormattedError(
            updateReadMsgFailure.errorId,
            updateReadMsgFailure.errorMessage, Some(READ_MESSAGE)
          )))
        }
      }
      case None => Future(TextMessage(write(sendJsonErrorWithEmptyData(Some(READ_MESSAGE)))))
    }
  }

  /**
   * Function to send notification from typing user
   *
   * @param userId
   * @param json
   */
  def userTyping(userId: String, json: String): Future[TextMessage] = {
    val parameterOpt = parse(json).extractOpt[UserTyping]
    parameterOpt match {
      case Some(targetUserId) => {
        val isExistsTargetId = redisFactory.isTalkExist(userId, parameterOpt.get.targetedUserId)
        isExistsTargetId.map(isExists =>
          isExists match {
            case true => TextMessage(write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(UserTypingResponse(userId, parameterOpt.get.targetedUserId)), Some(TYPING))))
            case false => TextMessage(write(sendFormattedError(MSG_002, s"Talk " + parameterOpt.get.targetedUserId + " not found.", Some(TYPING))))
          })
      }
      case None => Future(TextMessage(write(sendJsonErrorWithEmptyData(Some(TYPING)))))
    }
  }

  /**
   * Method to remove only a single message of a talk
   *
   * @param command
   * @param userId
   * @param json
   * @return
   */
  private def removeAMessage(command: ActorRef, userId: String, json: String): Future[TextMessage] = {
    val removeMessageOpt = parse(json).extractOpt[RemoveMessage]
    removeMessageOpt match {
      case Some(removeMessage) => {
        val futureResponse = ask(command, RemoveSingleMessage(removeMessage.targetUserId, removeMessage.messageId, userId))
        futureResponse map {
          case removeSingleMessageSuccess: RemoveSingleMessageSuccess =>
            TextMessage(write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(removeSingleMessageSuccess), Some(REMOVE_MESSAGE))))
          case removeSingleMessageFailure: RemoveSingleMessageFailure => TextMessage(write(sendFormattedError(
            removeSingleMessageFailure.errorId,
            removeSingleMessageFailure.errorMessage, Some(REMOVE_MESSAGE)
          )))
        }
      }
      case None => Future(TextMessage(write(sendJsonErrorWithEmptyData(Some(REMOVE_MESSAGE)))))
    }
  }

  /**
   * Function to search message
   *
   * @param query
   * @param message
   * @return
   */
  def getByMessage(query: ActorRef, message: String, userId: String): Future[String] = {
    val getMessageResponse = ask(query, SearchMessage(userId, message))
    getMessageResponse map {
      case searchMessageSuccess: SearchMessageSuccess => write(generateCommonResponseForCaseClass(SUCCESS, Some(List()),
        Some(searchMessageSuccess), Some(SEARCHMSG)))
      case searchMessageFailure: SearchMessageFailure => write(sendFormattedError(
        searchMessageFailure.errorId,
        searchMessageFailure.errorMessage, Some(SEARCHMSG)
      ))
    }
  }

  /**
   * Function to search talkers
   *
   * @param query
   * @param talkers
   * @return
   */
  def getByTalkers(query: ActorRef, talkers: String): Future[String] = {
    val getTalkerResponse = ask(query, SearchTalker(talkers))
    getTalkerResponse map {
      case searchTalkerSuccess: SearchTalkerSuccess => write(generateCommonResponseForCaseClass(SUCCESS, Some(List()),
        Some(searchTalkerSuccess), Some(SEARCHTALKER)))
      case searchTalkerFailure: SearchTalkerFailure => write(sendFormattedError(
        searchTalkerFailure.errorId,
        searchTalkerFailure.errorMessage, Some(SEARCHTALKER)
      ))
    }
  }

}

