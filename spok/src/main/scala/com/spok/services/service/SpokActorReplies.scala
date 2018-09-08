package com.spok.services.service

import com.datastax.driver.dse.graph.{ Edge, Vertex }
import com.spok.model.SpokDataResponse
import com.spok.model.SpokModel.{ RemoveCommentResponse, RemoveSpokResponse, _ }

/**
 * File contains all the success and failure replies that the spok actor sends
 */
sealed trait SpokAck

object SpokActorSuccessReplies {

  case class SpokCreateSuccess(spok: Spok) extends SpokAck

  case class RespokCreateSuccess(respokResponse: RespokInterimResponse, spokId: String) extends SpokAck

  case class UnspokPerformSuccess(unspokResponse: UnspokResponse, spokId: String) extends SpokAck

  case class AddCommentSuccess(addComment: Option[SpokCommentResponse]) extends SpokAck

  case class UpdateCommentSuccess(updateComment: Option[CommentUpdateResponse]) extends SpokAck

  case class RemoveCommentSuccess(removeComment: Option[RemoveCommentResponse]) extends SpokAck

  case class PollAnswerSavedSuccess(spokId: String) extends SpokAck

  case class DisableSpokSuccess(spokDisableResponse: String) extends SpokAck with SpokDataResponse

  case class UnsubscribeSpokFeedSuccess(message: String) extends SpokAck with SpokDataResponse

  case class SubscribeSpokFeedSuccess(message: String) extends SpokAck with SpokDataResponse

  case class RemoveWallSpokSuccess(removeWallSpokResponse: RemoveSpokResponse) extends SpokAck

  case class PollAllAnswersSavedSuccess(spokId: String) extends SpokAck

}

object SpokActorFailureReplies {

  case class SpokCreateFailure(errorId: String, cause: Throwable) extends SpokAck

  case class RespokCreateFailure(errorId: String, cause: Throwable) extends SpokAck

  case class UnspokPerformFailure(errorId: String, cause: Throwable) extends SpokAck

  case class AddCommentFailure(cause: Throwable, errorCode: String) extends SpokAck

  case class UpdateCommentFailure(cause: Throwable, errorCode: String) extends SpokAck

  case class RemoveCommentFailure(cause: Throwable, errorCode: String) extends SpokAck

  case class PollAnswerSavedFailure(cause: Throwable, errorCode: String) extends SpokAck

  case class DisableSpokFailure(cause: Throwable, errorCode: String) extends SpokAck

  case class RemoveWallSpokFailure(cause: Throwable, errorCode: String) extends SpokAck

  case class PollAllAnswersSavedFailure(errorId: String, errorMessage: String) extends SpokAck

}

object SpokViewValidationReplies {

  case class IsValidSpokAck(status: String, isEnabled: Boolean, edge: Option[Edge] = None)

  case class IsUserSuspendedAsk(status: String)

  case class IsValidPollQuestionAck(spokId: Option[String])

  case class IsValidAbsoluteSpokAck(status: String)

  case class IsValidSpokWithEnabledAck(status: Boolean)

  case class IsUnspokedAck(status: Boolean)

  case class IsValidSpokByIdAck(status: String, spokVertex: Option[Vertex])
}

object SpokViewReplies {

  case class ReadSuccess(spokDiffusion: Option[Spok])

  case class SpokStats(spokStats: SpokStatistics)

  case class SpoksStack(spoksStackResponse: Option[SpoksStackResponse])

  case class ScopedUsersRes(scopedUsersResponse: Option[ScopedUsersResponse])

  case class ReSpokersRes(reSpokerResponse: Option[ReSpokerResponse])

  case class ViewShortSpokResponse(viewShortSpok: Option[ViewSpok])

  case class ViewFullSpokResponse(viewFullSpok: Option[ViewFullSpok])

  case class ViewPollQuestionResponse(viewPollQuestion: Option[ViewPollQuestion], error: Option[Error])

  case class GetCommentsRes(commentsResponse: Option[CommentsResponse])

  case class ViewSpokersWallSuccess(usersWall: UsersWallResponse)

  case class ViewSpokersWallFailure(cause: Throwable, errorCode: String)

  case class ViewPollQuestionSuccess(viewPollQuestion: ViewPollQuestion)

  case class ViewPollQuestionFailure(errorId: String, errorMessage: String)

  case class ViewPollStatsSuccess(pollStats: PollStats)

  case class ViewPollStatsFailure(errorId: String, errorMessage: String)

}
