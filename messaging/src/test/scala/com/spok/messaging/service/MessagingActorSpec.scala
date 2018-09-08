package com.spok.messaging.service

import java.util.UUID

import akka.actor.{ ActorSystem, PoisonPill, Props }
import akka.testkit.{ ImplicitSender, TestKit }
import com.spok.messaging.service.MessagingActorCommands._
import com.spok.messaging.service.MessagingActorFailureReplies._
import com.spok.messaging.service.MessagingActorSuccessReplies._
import com.spok.model.Messaging.{ Message, MessageDetail, MessageResponse, User }
import com.spok.model.SpokModel.Error
import com.spok.persistence.cassandra.CassandraProvider
import com.spok.persistence.factory.messaging.MessagingApi
import com.spok.util.Constant._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ BeforeAndAfterAll, MustMatchers, WordSpecLike }

import scala.concurrent.duration._

class MessagingActorSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
    with WordSpecLike with MustMatchers with BeforeAndAfterAll with MockitoSugar {

  def this() = this(ActorSystem("MessagingActorSystem"))
  val session = CassandraProvider.session

  val mockedDseMessagingApi: MessagingApi = mock[MessagingApi]

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
    session.get.close()
  }

  "A MessagingActor" must {

    "Able to send messsage to friend by MessagingActor" in {
      val senderId = UUID.randomUUID().toString
      val receiverId = UUID.randomUUID().toString
      val messageId = UUID.randomUUID().toString
      val message = Message(senderId, receiverId, messageId, "message")
      val actorRef = system.actorOf(Props(new MessagingActor(senderId) {
        override val dseMessagingApi = mockedDseMessagingApi
      }))
      when(mockedDseMessagingApi.insertMessagesDetailsPerUser(message)) thenReturn
        Some(MessageResponse(
          User(senderId, "Test1", "male", "abc.jpg"),
          User(receiverId, "test2", "female", "xyz.ping"), MessageDetail(messageId, "message", None)
        ))
      actorRef ! CreateMessage(message)
      expectMsgType[CreateMessageSuccess](10 seconds)
      actorRef ! PoisonPill
    }

    "Not able to send messsage to friend by MessagingActor" in {
      val senderId = UUID.randomUUID().toString
      val receiverId = UUID.randomUUID().toString
      val messageId = UUID.randomUUID().toString
      val message = Message(senderId, receiverId, messageId, "message")
      val actorRef = system.actorOf(Props(new MessagingActor(senderId) {
        override val dseMessagingApi = mockedDseMessagingApi
      }))
      when(mockedDseMessagingApi.insertMessagesDetailsPerUser(message)) thenReturn (None)
      actorRef ! CreateMessage(message)
      expectMsgType[CreateMessageFailure](10 seconds)
    }

    "Able to remove all messages of a talk" in {
      val targetUserId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new MessagingActor(userId) {
        override val dseMessagingApi = mockedDseMessagingApi
      }))
      when(mockedDseMessagingApi.removeTalkDetails(userId, targetUserId)) thenReturn ((Some("Talk deleted successfully"), None))
      actorRef ! RemoveTalk(targetUserId, userId)
      expectMsgType[RemoveTalkSuccess](10 seconds)
    }

    "Able to send failure if not able to remove all messages of a talk" in {
      val targetUserId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new MessagingActor(userId) {
        override val dseMessagingApi = mockedDseMessagingApi
      }))
      when(mockedDseMessagingApi.removeTalkDetails(userId, targetUserId)) thenReturn ((None, Some(Error(MSG_104, UNABLE_DELETE_TALK))))
      actorRef ! RemoveTalk(targetUserId, userId)
      expectMsgType[RemoveTalkFailure](10 seconds)
    }

    "Able to remove a message of a talk" in {
      val targetUserId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val messageId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new MessagingActor(userId) {
        override val dseMessagingApi = mockedDseMessagingApi
      }))
      when(mockedDseMessagingApi.removeMessageById(userId, targetUserId, messageId)) thenReturn ((Some("Message deleted successfully"), None))
      actorRef ! SingleMessageRemove(targetUserId, messageId, userId)
      expectMsgType[RemoveSingleMessageSuccess](10 seconds)
    }

    "Able to send failure if not able to remove a message of a talk" in {
      val targetUserId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val messageId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new MessagingActor(userId) {
        override val dseMessagingApi = mockedDseMessagingApi
      }))
      when(mockedDseMessagingApi.removeMessageById(userId, targetUserId, messageId)) thenReturn ((None, Some(Error(MSG_105, UNABLE_DELETE_MESSAGE))))
      actorRef ! SingleMessageRemove(targetUserId, messageId, userId)
      expectMsgType[RemoveSingleMessageFailure](10 seconds)
    }

    "Able to update a message read flag" in {
      val targetUserId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val messageId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new MessagingActor(userId) {
        override val dseMessagingApi = mockedDseMessagingApi
      }))
      when(mockedDseMessagingApi.readMessageUpdate(userId, targetUserId, messageId)) thenReturn ((Some("Saved Successfully"), None))
      actorRef ! UpdateReadMessageFlag(userId, targetUserId, messageId)
      expectMsgType[UpdateReadMessageFlagSuccess](10 seconds)
    }

    "Able to send failure if not able to update a message read flag" in {
      val targetUserId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val messageId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new MessagingActor(userId) {
        override val dseMessagingApi = mockedDseMessagingApi
      }))
      when(mockedDseMessagingApi.readMessageUpdate(userId, targetUserId, messageId)) thenReturn ((None, Some(Error(MSG_107, s"Unable to flag message $messageId as read (generic error)."))))
      actorRef ! UpdateReadMessageFlag(userId, targetUserId, messageId)
      expectMsgType[UpdateReadMessageFlagFailure](10 seconds)
    }

  }

}
