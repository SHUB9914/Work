package com.spok.services.service

import akka.actor.{ ActorRef, Props }
import com.datastax.driver.dse.graph.{ Edge, Vertex }
import com.rbmhtechnology.eventuate.EventsourcedActor
import com.spok.model.SpokModel._
import com.spok.persistence.factory.spokgraph.{ DSESpokApi, SpokCommentApi }
import com.spok.persistence.factory.{ DSEGraphPersistenceFactoryApi, DSEUserSpokFactoryApi }
import com.spok.util.Constant._
import scala.collection.mutable.Map
import scala.util.{ Failure, Success }
import com.spok.services.service.SpokActorCommands._
import com.spok.services.service.SpokPerformAfterCommands._
import com.spok.services.service.SpokActorEvents._
import com.spok.services.service.SpokActorSuccessReplies._
import com.spok.services.service.SpokActorFailureReplies._

class SpokActor(override val id: String, override val aggregateId: Option[String], val eventLog: ActorRef) extends EventsourcedActor {

  import context.dispatcher

  val dseUserSpokFactoryApi: DSEUserSpokFactoryApi = DSEUserSpokFactoryApi
  val dseSpokFactoryApi: DSESpokApi = DSESpokApi
  val dseSpokCommentApi: SpokCommentApi = SpokCommentApi
  val spokLogger: SpokLogger = SpokLogger
  val dseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = DSEGraphPersistenceFactoryApi
  val spokChild = context.actorOf(Props[SpokChildActor])

  val statsActors: Map[String, ActorRef] = Map.empty
  /**
   * Command handlers.
   */
  override val onCommand: Receive = {

    case CreateSpok(spok: Spok, userId) => {
      persist(SpokCreated(spok, userId)) {
        case Success(evt) => {
          val groupExist = dseGraphPersistenceFactoryApi.isGroupExist(userId, spok.groupId.getOrElse(ZERO))
          if (groupExist) {
            val createSpok = dseSpokFactoryApi.createSpok(userId, spok)
            createSpok match {
              case true =>
                sender() ! SpokCreateSuccess(spok)
                spokChild ! PerformAfterSpok(userId, spok, statActor(spok.spokId))
              case false => sender() ! SpokCreateFailure(SPK_106, new Exception(UNABLE_CREATING_SPOK_GENERIC_ERROR))
            }
          } else sender() ! SpokCreateFailure(GRP_001, new Exception(s"Group ${spok.groupId} not found."))
        }
        case Failure(err) =>
          logger.error(err.getMessage)
          sender() ! SpokCreateFailure(SPK_106, new Exception(UNABLE_CREATING_SPOK_GENERIC_ERROR))
      }
    }

    case UpdateStatsAfterSpok(spok, userId) => {
      dseSpokFactoryApi.updateStats(spok.spokId, userId, spok.groupId, spok.geo)
    }

    case RespokCreate(respok, spokId, userId, edgeOpt) => {
      persist(RespokCreated(respok, spokId, userId)) {
        case Success(evt) => {
          val groupExist = dseUserSpokFactoryApi.isValidGroup(userId, respok.groupId.getOrElse(ZERO))
          logger.info(s"group in which the respok has to be done exists response : $groupExist")
          if (groupExist) {
            val (respokResponse, error) = dseSpokFactoryApi.createRespok(spokId, userId, respok, edgeOpt)
            logger.info(s"Respok response from DSE with message is  : $respokResponse and $error")
            (respokResponse, error) match {
              case (Some(response), None) => {
                sender() ! RespokCreateSuccess(response, spokId)
                spokChild ! PerformAfterRespok(respok, userId, spokId, response.followers, statActor(spokId))

              }
              case (None, Some(err)) => sender() ! RespokCreateFailure(err.id, new Exception(err.message))
            }
          } else sender() ! RespokCreateFailure(GRP_001, new Exception(s"Group ${respok.groupId} not found."))
        }
        case Failure(err) =>
          logger.error(err.getMessage)
          sender() ! RespokCreateFailure(SPK_117, new Exception(s"Unable re-spoking spok $spokId (generic error)."))
      }
    }

    case UpdateStatsAfterRespok(respok, spokId, userId) => {
      dseSpokFactoryApi.updateStats(spokId, userId, respok.groupId, respok.geo, true)
    }

    case PerformUnspok(unspok, spokId, userId, status) => {
      persist(UnspokPerformed(unspok, spokId, userId)) {
        case Success(evt) =>
          val unspokResponseOpt = dseSpokFactoryApi.createUnspok(spokId, userId, unspok, status)
          unspokResponseOpt match {
            case Some(unspokResponse) => {
              sender() ! UnspokPerformSuccess(unspokResponse, spokId)
              self ! PerformAfterUnspok(spokId, userId, unspok, status, statActor(spokId))
            }
            case None => sender() ! UnspokPerformFailure(SPK_118, new Exception(s"Unable un-spoking spok $spokId (generic error)."))
          }
        case Failure(err) =>
          sender() ! UnspokPerformFailure(SPK_118, new Exception(s"Unable un-spoking spok $spokId (generic error)."))
      }
    }

    case PerformAfterUnspok(spokId, userId, unspok, status, statActor) => {
      dseSpokFactoryApi.updateUserCurrentGeo(userId, unspok.geo)
      spokLogger.insertUnspokCreationEvent(unspok, userId, spokId)
      if (status == PENDING) {
        statActor ! UpdateStatsAfterUnspok(unspok, spokId, userId)
      }
    }

    case UpdateStatsAfterUnspok(unspok, spokId, userId) => {
      dseSpokFactoryApi.updateStatsAfterUnspok(spokId, userId, unspok)
    }

    case CommentAdd(addComment, spokId, commenterUserId) => {
      persist(CommentAdded(addComment, spokId, commenterUserId)) {
        case Success(evt) => {
          val (addCommentResponse, addedTimestamp, error) = dseSpokCommentApi.addComment(spokId, addComment.commentId,
            commenterUserId, addComment.text, addComment.geo, addComment.mentionUserId)
          (addCommentResponse, addedTimestamp, error) match {
            case (Some(response), someTimestamp, None) =>
              sender() ! AddCommentSuccess(Some(response))
              self ! PerformAfterComment(spokId, commenterUserId, addedTimestamp, addComment, statActor(spokId))
            case (None, someTimestamp, Some(someError)) =>
              sender() ! AddCommentFailure(new Exception(someError.message), someError.id)
          }
        }
        case Failure(err) =>
          sender() ! AddCommentFailure(new Exception(s"Unable commenting spok $spokId (generic error)."), SPK_119)
      }
    }

    case PerformAfterComment(spokId, commenterUserId, addedTimestamp, addComment, statActor) => {
      spokLogger.insertCommentEvent(commenterUserId, addComment.commentId,
        spokId, addedTimestamp, addComment.text, addComment.geo, COMMENT_ADDED_EVENT)
      spokLogger.insertHashTags(addComment.text)
      statActor ! UpdateStatsAfterAddOrRemoveComment(spokId)
    }

    case CommentUpdate(updateComment, commenterUserId) => {
      persist(CommentUpdated(updateComment, commenterUserId)) {
        case Success(evt) => {
          val (commentUpdatedRes, updatedTimeStamp, error) = dseSpokCommentApi.updateComment(commenterUserId, updateComment)
          (commentUpdatedRes, updatedTimeStamp, error) match {
            case (Some(response), someTimestamp, None) =>
              sender() ! UpdateCommentSuccess(Some(response))
              self ! PerformAfterUpdateComment(commenterUserId, updateComment, response, updatedTimeStamp)
            case (None, someTimestamp, Some(someError)) =>
              sender() ! UpdateCommentFailure(new Exception(someError.message), someError.id)
          }
        }
        case Failure(err) => sender() ! UpdateCommentFailure(new Exception(s"Unable updating comment ${updateComment.commentId}(generic error)."), SPK_120)
      }
    }

    case PerformAfterUpdateComment(commenterUserId, updateComment, response, updatedTimeStamp) => {
      spokLogger.insertCommentEvent(commenterUserId, updateComment.commentId, response.spokId, updatedTimeStamp,
        updateComment.text, updateComment.geo, COMMENT_UPDATED_EVENT)
      spokLogger.insertHashTags(updateComment.text)
    }

    case CommentRemove(commentId, commenterUserId, geo) => {
      persist(CommentRemoved(commentId, commenterUserId)) {
        case Success(evt) => {
          val (commentRemovedRes, updatedTimeStamp, error) = dseSpokCommentApi.removeComment(commentId, commenterUserId, geo)
          (commentRemovedRes, updatedTimeStamp, error) match {
            case (Some(response), someTimestamp, None) =>
              sender() ! RemoveCommentSuccess(Some(response))
              self ! PerformAfterRemoveComment(response.spok.spokId, commenterUserId, response.commentId, someTimestamp, geo, statActor(commentId))
            case (None, someTimestamp, Some(someError)) => sender() ! RemoveCommentFailure(new Exception(someError.message), someError.id)
          }
        }
        case Failure(err) =>
          sender() ! RemoveCommentFailure(new Exception(s"Unable removing comment $commentId(generic error)."), SPK_121)
      }
    }

    case PerformAfterRemoveComment(spokId, commenterUserId, commentId, updatedTimeStamp, geo, statActor) => {
      spokLogger.insertRemoveCommentEvent(commenterUserId, commentId, spokId, updatedTimeStamp, geo)
      statActor ! UpdateStatsAfterAddOrRemoveComment(spokId)
    }

    case UpdateStatsAfterAddOrRemoveComment(spokId) => dseSpokFactoryApi.updateStatsAfterAddComment(spokId)

    case SaveAnswer(questionId, userId, userPollAnswer) => {
      persist(PollAnswerSaved(questionId, userId, userPollAnswer)) {
        case Success(evt) => {
          val questionExist = dseUserSpokFactoryApi.validatePollQuestionAndFetchSpokId(questionId)
          questionExist match {
            case Some(spokId) => {
              val addAnswerResponse = dseSpokFactoryApi.addAnswerToAPoll(questionId, spokId, userId, userPollAnswer)
              addAnswerResponse match {
                case None => {
                  sender() ! PollAnswerSavedSuccess(spokId)
                  self ! PerformAfterAnswerPollQuestion(userId, spokId)
                }
                case Some(error) => sender() ! PollAnswerSavedFailure(new Exception(error.message), error.id)
              }
            }
            case None => sender() ! PollAnswerSavedFailure(new Exception(s"Question $questionId not found"), SPK_126)
          }
        }
        case Failure(err) =>
          sender() ! PollAnswerSavedFailure(new Exception(s"Unable saving answer to poll's question $questionId (generic error)."), SPK_124)
      }
    }

    case PerformAfterAnswerPollQuestion(userId, spokId) => {
      dseSpokFactoryApi.updatePendingQuestionsInEdge(userId, spokId)
    }

    case DisableSpok(spokId, userId, launchedTime, geo) => {
      persist(DisabledSpok(spokId, userId)) {
        case Success(evt) =>
          val spokDisableResponse = dseSpokFactoryApi.disableSpok(spokId, userId, geo)
          spokDisableResponse match {
            case SPOK_DISABLED => {
              sender() ! DisableSpokSuccess(spokDisableResponse)
              self ! PerformAfterSpokDisableEvents(userId, spokId, launchedTime, geo)
            }
            case UNABLE_DISABLING_SPOK =>
              sender() ! DisableSpokFailure(new Exception(s"Unable disabling spok $spokId (generic error)."), SPK_115)
            case INVALID_USER =>
              sender() ! DisableSpokFailure(new Exception(s"User $userId not found."), USR_001)
            case DISABLED_SPOK =>
              sender() ! DisableSpokFailure(new Exception(DISABLED_SPOK), SPK_016)
            case SPOK_NOT_FOUND =>
              sender() ! DisableSpokFailure(new Exception(s"Spok $spokId not found."), SPK_001)
          }
        case Failure(err) => sender() ! DisableSpokFailure(new Exception(s"Unable disabling spok $spokId (generic error)."), SPK_115)
      }
    }

    case PerformAfterSpokDisableEvents(userId, spokId, launchedTime, geo) => {
      dseSpokFactoryApi.updateUserCurrentGeo(userId, geo)
      spokLogger.insertSpokEvent(userId, spokId, launchedTime, DISABLED_EVENT, geo)
    }

    case RemoveWallSpok(spokId, userId, launchedTime, geo) => {
      persist(RemovedWallSpok(spokId, userId)) {
        case Success(evt) =>
          val spokDisableResponse = dseSpokFactoryApi.removeSpokFromWall(spokId, userId, launchedTime, geo)
          spokDisableResponse.message match {
            case Some(errorMessage) => {
              errorMessage match {
                case SPOK_STATUS_NOT_RESPOKED => sender() ! RemoveWallSpokFailure(new Exception(SPOK_STATUS_NOT_RESPOKED), SPK_126)
                case UNABLE_REMOVING_SPOK => sender() ! RemoveWallSpokFailure(new Exception(s"Unable removing spok $spokId (generic error)."), SPK_116)
              }
            }
            case None => {
              sender() ! RemoveWallSpokSuccess(spokDisableResponse)
              self ! PerformAfterRemoveWallSpok(userId, spokId, launchedTime, geo)
            }
          }
        case Failure(err) =>
          sender() ! RemoveWallSpokFailure(new Exception(s"Unable removing spok $spokId (generic error)."), SPK_116)
      }
    }

    case PerformAfterRemoveWallSpok(userId, spokId, launchedTime, geo) => {
      dseSpokFactoryApi.updateUserCurrentGeo(userId, geo)
      spokLogger.insertSpokEvent(userId, spokId, launchedTime, REMOVED_EVENT, geo)
    }

    case SaveAllAnswersOfPoll(userId, allAnswers) => {
      persist(AllPollAnswerSaved(userId, allAnswers)) {
        case Success(evt) => {
          val isValidSpok = dseSpokFactoryApi.validateAbsoluteSpokById(allAnswers.spokId)
          logger.info("Is a valid spok to answer all questions :: " + isValidSpok)
          isValidSpok match {
            case SPOK_VALID => {
              val (addAnswerResponse, alreadyAnswered) = dseSpokFactoryApi.addAllAnswersToAPoll(userId, allAnswers)
              addAnswerResponse match {
                case None => {
                  sender() ! PollAllAnswersSavedSuccess(allAnswers.spokId)
                  self ! PerformAfterAnsweringAllPollQuestion(userId, allAnswers.spokId)
                }
                case Some(error) => sender() ! PollAllAnswersSavedFailure(error.id, error.message)
              }
            }
            case _ => sender() ! PollAllAnswersSavedFailure(SPK_001, s"Spok ${allAnswers.spokId} not found.")
          }

        }
        case Failure(err) =>
          sender() ! PollAllAnswersSavedFailure(SPK_124, s"Unable saving answers to poll spok ${allAnswers.spokId} (generic error).")
      }
    }

    case PerformAfterAnsweringAllPollQuestion(userId, spokId) => {
      dseSpokFactoryApi.updateFinishCountOfPoll(userId, spokId)
    }

  }

  /**
   * Event handlers.
   */
  override val onEvent: Receive = {
    case _ =>
  }

  private def statActor(spokId: String): ActorRef = {
    statsActors.get(spokId) match {
      case Some(statActor) => statActor
      case None =>
        statsActors += (spokId -> context.actorOf(Props(createActor(spokId)), spokId))
        statsActors(spokId)
    }
  }

  def createActor(spokId: String): SpokActor = {
    new SpokActor(spokId, Some(spokId), eventLog)
  }
}

