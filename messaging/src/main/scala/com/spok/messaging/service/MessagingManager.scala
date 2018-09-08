package com.spok.messaging.service

import akka.actor.{ Actor, ActorRef, Props }
import akka.util.Timeout
import com.spok.messaging.service.MessagingActorCommands._
import com.spok.messaging.service.MessagingManagerCommands._
import scala.collection.mutable.Map
import scala.concurrent.duration.DurationInt

class MessagingManager extends Actor {

  private implicit val timeout = Timeout(10.seconds)

  val messageActors: Map[String, ActorRef] = Map.empty

  override def receive: Receive = {
    case SendMessage(userId, message) =>
      messagingActor(userId) forward CreateMessage(message)

    case TalkRemove(targetUserId, userId) =>
      messagingActor(userId) forward RemoveTalk(targetUserId, userId)

    case RemoveSingleMessage(targetUserId, messageId, userId) =>
      messagingActor(userId) forward SingleMessageRemove(targetUserId, messageId, userId)

    case ReadMessageUpdateFlag(userId, targetUserId, messageId) =>
      messagingActor(userId) forward UpdateReadMessageFlag(userId, targetUserId, messageId)
  }

  private def messagingActor(userId: String): ActorRef = {
    messageActors.get(userId) match {
      case Some(messageActor) => messageActor
      case None =>
        messageActors += (userId -> context.actorOf(Props(createActor(userId)), userId))
        messageActors(userId)
    }
  }

  def createActor(userId: String): MessagingActor = {
    new MessagingActor(userId)
  }
}
