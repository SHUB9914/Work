package com.spok.messaging.service

import java.util.{ Date, UUID }

import akka.actor.{ ActorSystem, Props }
import akka.testkit.{ ImplicitSender, TestKit }
import com.spok.messaging.service.MessagingViewCommands._
import com.spok.messaging.service.MessagingViewFailureReplies.{ SearchMessageFailure, SearchTalkerFailure, ViewSingleTalkFailure, ViewTalksFailure }
import com.spok.messaging.service.MessagingViewSuccessReplies._
import com.spok.model.Messaging._
import com.spok.model.SpokModel.Error
import com.spok.persistence.cassandra.CassandraProvider
import com.spok.persistence.factory.messaging.MessagingApi
import com.spok.util.JsonHelper
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ BeforeAndAfterAll, MustMatchers, WordSpecLike }
class MessagingViewSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
    with WordSpecLike with MustMatchers with BeforeAndAfterAll with JsonHelper with MockitoSugar {

  def this() = this(ActorSystem("MessagingViewSystem"))

  val session = CassandraProvider.session

  val mockedDseMessagingApi: MessagingApi = mock[MessagingApi]

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
    session.get.close()
  }

  "A Messaging View" must {

    "Return success if all talks of a user are viewed successfully" in {
      val userId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new MessagingView {
        override val messagingApi = mockedDseMessagingApi
      }))
      val talkResponse = TalksResponse("0", "2", List(Talks(User("userId", "NickName", "gender", "Picture.jpg"), LastMessage("", new Date(), ""))))
      when(mockedDseMessagingApi.getTalkLists("1", userId)) thenReturn ((Some(talkResponse), None))
      actorRef ! ViewTalks(userId, "1")
      expectMsgType[ViewTalksSuccess]
    }

    "Return fail if user fails to viewe talk list" in {
      val userId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new MessagingView {
        override val messagingApi = mockedDseMessagingApi
      }))
      when(mockedDseMessagingApi.getTalkLists("1", userId)) thenReturn ((None, Some(Error("MSG-101", "Unable loading talks list (generic error).", None))))
      actorRef ! ViewTalks(userId, "1")
      expectMsgType[ViewTalksFailure]
    }

    "Return success if all message of a talk of a user are viewed successfully" in {
      val userId = UUID.randomUUID().toString
      val targetUserId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new MessagingView {
        override val messagingApi = mockedDseMessagingApi
      }))
      val response = TalkResponse(User("userId1", "me", "gender", "Picture.jpg"), User("userId2", "receiver", "gender", "Picture.jpg"), List(UserMessageDetail("messageId", "message", new Date(), "from")))
      when(mockedDseMessagingApi.getUserTalk(None, userId, targetUserId, "desc")) thenReturn ((Some(response), None))
      actorRef ! ViewSingleTalk(targetUserId, userId, None, "desc")
      expectMsgType[ViewSingleTalkSuccess]
    }

    "Return fail if user fails to viewe talk messages" in {
      val userId = UUID.randomUUID().toString
      val targetUserId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new MessagingView {
        override val messagingApi = mockedDseMessagingApi
      }))
      when(mockedDseMessagingApi.getUserTalk(None, userId, targetUserId, "desc")) thenReturn ((None, Some(Error("MSG-102", "Unable loading talk's messages (generic error).", None))))
      actorRef ! ViewSingleTalk(targetUserId, userId, None, "desc")
      expectMsgType[ViewSingleTalkFailure]
    }

    "Return success if user search message successfully" in {
      val userId = UUID.randomUUID().toString
      val messageId = UUID.randomUUID().toString
      val date = new Date(1000L)

      val actorRef = system.actorOf(Props(new MessagingView {
        override val messagingApi = mockedDseMessagingApi
      }))
      val response = List(SearchMessageResponse(messageId, "messageText", date, userId, "userNickname", "male", "userPic", userId))
      when(mockedDseMessagingApi.fullTextMessageSearch(userId, "Message")) thenReturn ((Some(response), None))
      actorRef ! SearchMessage(userId, "Message")
      expectMsgType[SearchMessageSuccess]
    }

    "Return fail if user fails to search messages" in {
      val userId = UUID.randomUUID().toString
      val targetUserId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new MessagingView {
        override val messagingApi = mockedDseMessagingApi
      }))
      when(mockedDseMessagingApi.fullTextMessageSearch(userId, "Message")) thenReturn ((None, Some(Error("MSG-106", "Unable searching message (generic error).", None))))
      actorRef ! SearchMessage(userId, "Message")
      expectMsgType[SearchMessageFailure]
    }

    "Return success if user search talker successfully" in {
      val userId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new MessagingView {
        override val messagingApi = mockedDseMessagingApi
      }))
      val response = List(User(userId, "userNickname", "male", "userPic"))
      when(mockedDseMessagingApi.searchTalker("piy")) thenReturn ((Some(response), None))
      actorRef ! SearchTalker("piy")
      expectMsgType[SearchTalkerSuccess]
    }

    "Return fail if user fails to search talker" in {
      val actorRef = system.actorOf(Props(new MessagingView {
        override val messagingApi = mockedDseMessagingApi
      }))
      when(mockedDseMessagingApi.searchTalker("piy")) thenReturn ((None, Some(Error("MSG-106", "Unable searching talkers (generic error).", None))))
      actorRef ! SearchTalker("piy")
      expectMsgType[SearchTalkerFailure]
    }

  }
}
