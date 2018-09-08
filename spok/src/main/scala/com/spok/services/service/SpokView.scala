package com.spok.services.service

import akka.actor.ActorRef
import com.rbmhtechnology.eventuate.EventsourcedView
import com.spok.persistence.factory.DSEUserSpokFactoryApi
import com.spok.persistence.factory.spokgraph.{ DSESpokApi, DSESpokViewApi }
import com.spok.services.service.SpokViewCommands._
import com.spok.services.service.SpokViewReplies._
import com.spok.services.service.SpokViewValidationCommands._
import com.spok.services.service.SpokViewValidationReplies._
import com.spok.util.Constant._

// Replies

class SpokView(replicaId: String, override val eventLog: ActorRef)
    extends EventsourcedView {

  override val id = s"s-sv-$replicaId"

  val dseUserSpokFactoryApi: DSEUserSpokFactoryApi = DSEUserSpokFactoryApi
  val dseSpokFactoryApi: DSESpokApi = DSESpokApi
  val dseSpokViewApi: DSESpokViewApi = DSESpokViewApi

  /**
   * Command handler.
   */
  override def onCommand: Receive = {

    case IsValidSpokAndSendStatus(userId, spokId) => {
      val (status, isEnabled, edgeOp) = dseUserSpokFactoryApi.validateSpokAndSendStatus(userId, spokId)
      sender() ! IsValidSpokAck(status, isEnabled, edgeOp)
    }
    case IsValidPollQuestion(questionId) =>
      sender() ! IsValidPollQuestionAck(dseUserSpokFactoryApi.validatePollQuestionAndFetchSpokId(questionId))
    case IsValidSpokWithEnabledFlag(spokId) =>
      sender() ! IsValidSpokWithEnabledAck(dseUserSpokFactoryApi.isValidSpokWithEnabledFlag(spokId))
    case IsValidAbsoluteSpok(spokId) =>
      sender() ! IsValidAbsoluteSpokAck(dseUserSpokFactoryApi.validateAbsoluteSpokById(spokId))

    case ViewPollQuestionDetails(questionId, userId) =>
      val (viewPollQuestionResponse, error) = dseSpokFactoryApi.viewPollQuestion(questionId, userId)
      (viewPollQuestionResponse, error) match {
        case (Some(response), None) => sender() ! ViewPollQuestionSuccess(response)
        case (None, Some(err)) => sender() ! ViewPollQuestionFailure(err.id, err.message)
      }

    case GetSpokStats(spokId) =>
      sender() ! SpokStats(dseSpokViewApi.getSpokStats(spokId))
    case GetComments(spokId, pos) =>
      sender() ! GetCommentsRes(dseSpokViewApi.getComments(spokId, pos))
    case GetReSpokers(spokId, pos) =>
      sender() ! ReSpokersRes(dseSpokViewApi.getReSpokers(spokId, pos))
    case GetScopedUsers(spokId, pos) =>
      sender() ! ScopedUsersRes(dseSpokViewApi.getScopedUsers(spokId, pos))
    case GetSpokStack(userId, pos) =>
      sender() ! SpoksStack(dseSpokViewApi.getSpokStack(userId, pos))
    case ViewShortSpok(spokId, targetUserId, userId, spokVertex) =>
      sender() ! ViewShortSpokResponse(dseSpokViewApi.viewShortSpok(spokId, targetUserId, userId, spokVertex.get))

    case ViewSpokersWall(targetUserId, pos, userId) => {
      val userMinimalDetails = dseSpokViewApi.getSpokersWallDetails(targetUserId, pos, userId)
      userMinimalDetails match {
        case Some(usersWall) => sender() ! ViewSpokersWallSuccess(usersWall)
        case None => sender() ! ViewSpokersWallFailure(new Exception(s"Unable loading user $targetUserId wall (generic error)."), USR_102)
      }
    }
    case ViewFullSpokDetails(spokId, targetUserId, userId, spokVertex) =>
      sender() ! ViewFullSpokResponse(dseSpokViewApi.viewFullSpok(spokId, targetUserId, userId, spokVertex.get))
    case IsValidSpokById(spokId) =>
      val (status, spokVertex) = dseUserSpokFactoryApi.validateSpokById(spokId)
      sender() ! IsValidSpokByIdAck(status, spokVertex)

    case ViewPollStats(spokId, userId) =>
      val (pollStats, error) = dseSpokViewApi.viewPollStats(spokId, userId)
      (pollStats, error) match {
        case (Some(somePollStats), None) => sender() ! ViewPollStatsSuccess(somePollStats)
        case (None, Some(err)) => sender() ! ViewPollStatsFailure(err.id, err.message)
      }

    case GetMySpoks(userId, pos) =>
      sender() ! SpoksStack(dseSpokViewApi.getMySpoks(userId, pos))

    case IsUserSuspended(spokId) => {
      val status = dseUserSpokFactoryApi.spokerSuspendedOrNot(spokId)
      sender() ! IsUserSuspendedAsk(status)
    }

  }

  /**
   * Event handlers.
   */
  override val onEvent: Receive = {

    case _ =>

  }
}
