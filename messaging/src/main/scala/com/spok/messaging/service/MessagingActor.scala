package com.spok.messaging.service

import akka.actor.Actor
import com.spok.model.Messaging.MessageResponse
import com.spok.persistence.factory.messaging.MessagingApi
import com.spok.util.Constant._
import com.spok.messaging.service.MessagingActorCommands._
import com.spok.messaging.service.MessagingActorSuccessReplies._
import com.spok.messaging.service.MessagingActorFailureReplies._
import com.spok.model.SpokModel.Error

class MessagingActor(userId: String) extends Actor {

  val dseMessagingApi: MessagingApi = MessagingApi

  /**
   * Command handler.
   */
  def receive: Receive = {

    case CreateMessage(messageData) => {
      val messageResponse: Option[MessageResponse] = dseMessagingApi.insertMessagesDetailsPerUser(messageData)
      messageResponse match {
        case Some(messageRes) => sender ! CreateMessageSuccess(messageRes)
        case None => sender ! CreateMessageFailure(new Exception(UNABLE_SEND_MESSAGE), MSG_103)
      }
    }

    case RemoveTalk(talkId, someUserId) => {
      val (removeTalkResponse, error): (Option[String], Option[Error]) = dseMessagingApi.removeTalkDetails(someUserId, talkId)
      (removeTalkResponse, error) match {
        case (Some(response), None) => sender ! RemoveTalkSuccess(response)
        case (None, Some(someError)) => sender ! RemoveTalkFailure(someError.id, someError.message)
      }
    }

    case SingleMessageRemove(talkId, messageId, someUserId) => {
      val (removeMessageResponse, error): (Option[String], Option[Error]) = dseMessagingApi.removeMessageById(someUserId, talkId, messageId)
      (removeMessageResponse, error) match {
        case (Some(response), None) => sender ! RemoveSingleMessageSuccess(response)
        case (None, Some(someError)) => sender ! RemoveSingleMessageFailure(someError.id, someError.message)
      }
    }

    case UpdateReadMessageFlag(userId, targetUserId, messageId) => {
      val (readUpdateResponse, error): (Option[String], Option[Error]) = dseMessagingApi.readMessageUpdate(userId, targetUserId, messageId)
      (readUpdateResponse, error) match {
        case (Some(response), None) => sender ! UpdateReadMessageFlagSuccess(response)
        case (None, Some(someError)) => sender ! UpdateReadMessageFlagFailure(someError.id, someError.message)
      }
    }

  }
}

