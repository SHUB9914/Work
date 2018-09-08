package com.spok.messaging.service

import akka.actor.Actor
import com.spok.persistence.factory.messaging.MessagingApi
import com.spok.util.{ JsonHelper, LoggerUtil }
import com.spok.messaging.service.MessagingViewCommands._
import com.spok.messaging.service.MessagingViewSuccessReplies.{ ViewTalksSuccess, _ }
import com.spok.messaging.service.MessagingViewFailureReplies._
import com.spok.model.SpokModel.Error

class MessagingView extends Actor with JsonHelper with LoggerUtil {

  val messagingApi: MessagingApi = MessagingApi

  /**
   * Command handler.
   */

  def receive: Receive = {

    case ViewTalks(userId, pos) => {
      val (getTalkListResponse, error) = messagingApi.getTalkLists(pos, userId)
      (getTalkListResponse, error) match {
        case (Some(response), None) => sender() ! ViewTalksSuccess(response)
        case (None, Some(someError)) => sender() ! ViewTalksFailure(someError.id, someError.message)
      }
    }

    case ViewSingleTalk(targetUserId, userId, messageId, order) => {
      val (getUserTalkResponse, error) = messagingApi.getUserTalk(messageId, userId, targetUserId, order)
      (getUserTalkResponse, error) match {
        case (Some(response), None) => sender() ! ViewSingleTalkSuccess(response)
        case (None, Some(someError)) => sender() ! ViewSingleTalkFailure(someError.id, someError.message)
      }
    }

    case IsSpokerSuspended(targerId) =>

    /**
     * Function to search message
     */
    case SearchMessage(userId, message) => {
      val (getSearchMessageResponse, error) = messagingApi.fullTextMessageSearch(userId, message)
      (getSearchMessageResponse, error) match {
        case (Some(response), None) => sender() ! SearchMessageSuccess(response)
        case (None, Some(someError)) => sender() ! SearchMessageFailure(someError.id, someError.message)
      }
    }

    /**
     * Function to search talkers
     */
    case SearchTalker(talkers) => {
      val (getSearchTalkerResponse, error) = messagingApi.searchTalker(talkers)
      (getSearchTalkerResponse, error) match {
        case (Some(response), None) => sender() ! SearchTalkerSuccess(response)
        case (None, Some(someError)) => sender() ! SearchTalkerFailure(someError.id, someError.message)
      }
    }
  }
}
