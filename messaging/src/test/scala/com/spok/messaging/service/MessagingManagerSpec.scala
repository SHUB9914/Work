package com.spok.messaging.service

import java.util.UUID

import akka.actor.{ ActorSystem, PoisonPill, Props }
import akka.testkit.{ ImplicitSender, TestKit }
import com.spok.model.Messaging.{ Message, MessageDetail, MessageResponse, User }
import com.spok.persistence.cassandra.CassandraProvider
import com.spok.persistence.factory.messaging.MessagingApi
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ BeforeAndAfterAll, MustMatchers, WordSpecLike }

import scala.concurrent.duration._
import com.spok.messaging.service.MessagingActorSuccessReplies._
import com.spok.messaging.service.MessagingActorFailureReplies._
import com.spok.messaging.service.MessagingManagerCommands._
import com.spok.model.SpokModel.Error
import com.spok.util.Constant._

class MessagingManagerSpec(system: ActorSystem) extends TestKit(system) with ImplicitSender
    with WordSpecLike with MustMatchers with BeforeAndAfterAll with MockitoSugar {

  def this() = this(ActorSystem("MessagingManagerActorSystem"))
  val session = CassandraProvider.session

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
    session.get.close()
  }

  "A MessagingManager" must {

    "Able to create a message by MessagingManager" in {
      val id = UUID.randomUUID().toString
      val senderId = UUID.randomUUID().toString
      val receiverId = UUID.randomUUID().toString
      val messageId = UUID.randomUUID().toString
      val message = Message(senderId, receiverId, messageId, "message")
      val actorRef = system.actorOf(Props(new MessagingManager {
        override def createActor(userId: String): MessagingActor = {
          new MessagingActor(senderId) {
            override val dseMessagingApi: MessagingApi = mock[MessagingApi]
            when(dseMessagingApi.insertMessagesDetailsPerUser(message)) thenReturn (Some(MessageResponse(User(senderId, "Test1", "male", "abc.jpg"), User(receiverId, "test2", "female", "xyz.ping"), MessageDetail(messageId, "message", None))))
          }
        }
      }))
      actorRef ! SendMessage(senderId, message)
      expectMsgType[CreateMessageSuccess](5 seconds)
      actorRef ! PoisonPill
    }
  }

  "Able to remove a talk completely with all its messages" in {
    val targetUserId = UUID.randomUUID().toString
    val senderId = UUID.randomUUID().toString
    val actorRef = system.actorOf(Props(new MessagingManager {
      override def createActor(userId: String): MessagingActor = {
        new MessagingActor(senderId) {
          override val dseMessagingApi: MessagingApi = mock[MessagingApi]
          when(dseMessagingApi.removeTalkDetails(userId, targetUserId)) thenReturn ((Some("Talk deleted successfully"), None))
        }
      }
    }))
    actorRef ! TalkRemove(targetUserId, senderId)
    expectMsgType[RemoveTalkSuccess](5 seconds)
    actorRef ! PoisonPill
  }

  "Able to send failure if not able to remove all messages of a talk" in {
    val targetUserId = UUID.randomUUID().toString
    val senderId = UUID.randomUUID().toString
    val actorRef = system.actorOf(Props(new MessagingManager {
      override def createActor(userId: String): MessagingActor = {
        new MessagingActor(senderId) {
          override val dseMessagingApi: MessagingApi = mock[MessagingApi]
          when(dseMessagingApi.removeTalkDetails(userId, targetUserId)) thenReturn ((None, Some(Error(MSG_104, UNABLE_DELETE_TALK))))
        }
      }
    }))
    actorRef ! TalkRemove(targetUserId, senderId)
    expectMsgType[RemoveTalkFailure](5 seconds)
    actorRef ! PoisonPill
  }

  "Able to remove a message of a talk" in {
    val targetUserId = UUID.randomUUID().toString
    val senderId = UUID.randomUUID().toString
    val messageId = UUID.randomUUID().toString
    val actorRef = system.actorOf(Props(new MessagingManager {
      override def createActor(userId: String): MessagingActor = {
        new MessagingActor(senderId) {
          override val dseMessagingApi: MessagingApi = mock[MessagingApi]
          when(dseMessagingApi.removeMessageById(userId, targetUserId, messageId)) thenReturn ((Some("Message deleted successfully"), None))
        }
      }
    }))
    actorRef ! RemoveSingleMessage(targetUserId, messageId, senderId)
    expectMsgType[RemoveSingleMessageSuccess](5 seconds)
    actorRef ! PoisonPill
  }

  "Able to send failure if not able to remove a message of a talk" in {
    val targetUserId = UUID.randomUUID().toString
    val senderId = UUID.randomUUID().toString
    val messageId = UUID.randomUUID().toString
    val actorRef = system.actorOf(Props(new MessagingManager {
      override def createActor(userId: String): MessagingActor = {
        new MessagingActor(senderId) {
          override val dseMessagingApi: MessagingApi = mock[MessagingApi]
          when(dseMessagingApi.removeMessageById(userId, targetUserId, messageId)) thenReturn ((None, Some(Error(MSG_105, UNABLE_DELETE_MESSAGE))))
        }
      }
    }))
    actorRef ! RemoveSingleMessage(targetUserId, messageId, senderId)
    expectMsgType[RemoveSingleMessageFailure](5 seconds)
    actorRef ! PoisonPill
  }

  "Able to update read message flag" in {
    val targetUserId = UUID.randomUUID().toString
    val senderId = UUID.randomUUID().toString
    val messageId = UUID.randomUUID().toString
    val actorRef = system.actorOf(Props(new MessagingManager {
      override def createActor(userId: String): MessagingActor = {
        new MessagingActor(senderId) {
          override val dseMessagingApi: MessagingApi = mock[MessagingApi]
          when(dseMessagingApi.readMessageUpdate(senderId, targetUserId, messageId)) thenReturn ((Some("Saved Successfully"), None))
        }
      }
    }))
    actorRef ! ReadMessageUpdateFlag(senderId, targetUserId, messageId)
    expectMsgType[UpdateReadMessageFlagSuccess](5 seconds)
    actorRef ! PoisonPill
  }

  "Able to send failure if not update read message flag" in {
    val targetUserId = UUID.randomUUID().toString
    val senderId = UUID.randomUUID().toString
    val messageId = UUID.randomUUID().toString
    val actorRef = system.actorOf(Props(new MessagingManager {
      override def createActor(userId: String): MessagingActor = {
        new MessagingActor(senderId) {
          override val dseMessagingApi: MessagingApi = mock[MessagingApi]
          when(dseMessagingApi.readMessageUpdate(senderId, targetUserId, messageId)) thenReturn ((None, Some(Error(MSG_107, s"Unable to flag message $messageId as read (generic error)."))))
        }
      }
    }))
    actorRef ! ReadMessageUpdateFlag(senderId, targetUserId, messageId)
    expectMsgType[UpdateReadMessageFlagFailure](5 seconds)
    actorRef ! PoisonPill
  }

}
