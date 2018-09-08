package com.spok.messaging.handler

import java.util.{ Date, UUID }

import akka.actor.{ Actor, ActorSystem }
import akka.http.scaladsl.model.{ StatusCodes, _ }
import akka.http.scaladsl.model.ws.TextMessage
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.{ TestSink, TestSource }
import akka.testkit.TestActorRef
import com.spok.messaging.service.MessagingActorFailureReplies._
import com.spok.messaging.service.MessagingActorSuccessReplies._
import com.spok.messaging.service.MessagingManagerCommands._
import com.spok.messaging.service.MessagingViewCommands._
import com.spok.messaging.service.MessagingViewFailureReplies._
import com.spok.messaging.service.MessagingViewSuccessReplies._
import com.spok.model.Messaging._
import com.spok.model.SpokModel.Error
import com.spok.persistence.factory.DSEGraphPersistenceFactoryApi
import com.spok.persistence.factory.messaging.MessagingApi
import com.spok.persistence.redis.RedisFactory
import com.spok.util.Constant._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ Matchers, WordSpec }

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ Await, Future }

class MessagingRestServiceHandlerSpec extends WordSpec with MessagingRestServiceHandler with MockitoSugar with Matchers {
  implicit val system = ActorSystem("messaging")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher
  override val dseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
  override val redisFactory: RedisFactory = mock[RedisFactory]
  override val messagingApi: MessagingApi = mock[MessagingApi]

  "Messaging service" should {

    "send success message if user sends a message successfully with talk id" in {
      val senderId = UUID.randomUUID().toString
      val receiverId = UUID.randomUUID().toString
      val talkId = UUID.randomUUID().toString
      val messageId = UUID.randomUUID().toString

      val command = TestActorRef(new Actor {
        def receive = {
          case SendMessage(phoneNumber: String, message: Message) ⇒ {
            sender ! CreateMessageSuccess(MessageResponse(User(senderId, "Test1", "male", "abc.jpg"), User(receiverId, "test2", "female", "xyz.ping"), MessageDetail(messageId, "message", None)))
          }
        }
      })
      when(dseGraphPersistenceFactoryApi.isUserSuspendAlready(senderId)) thenReturn Some(false)
      when(redisFactory.isTalkExist(senderId, receiverId)) thenReturn Future(true)
      val result = Await.result(toHandleMessage(command, senderId, MessageJsonData("message"), receiverId), 5 second)
      assert(result.status equals (StatusCodes.OK))
    }

    "send error message when suspende use send a message to another user" in {
      val senderId = UUID.randomUUID().toString
      val receiverId = UUID.randomUUID().toString
      val talkId = UUID.randomUUID().toString
      val messageId = UUID.randomUUID().toString

      val command = TestActorRef(new Actor {
        def receive = {
          case SendMessage(phoneNumber: String, message: Message) ⇒ {
            sender ! CreateMessageSuccess(MessageResponse(User(senderId, "Test1", "male", "abc.jpg"), User(receiverId, "test2", "female", "xyz.ping"), MessageDetail(messageId, "message", None)))
          }
        }
      })
      when(dseGraphPersistenceFactoryApi.isUserSuspendAlready(senderId)) thenReturn Some(true)
      val result = Await.result(toHandleMessage(command, senderId, MessageJsonData("message"), receiverId), 5 second)
      assert(result.status equals (StatusCodes.BadRequest))
    }

    "send error message when suspend property of user not found" in {
      val senderId = UUID.randomUUID().toString
      val receiverId = UUID.randomUUID().toString
      val talkId = UUID.randomUUID().toString
      val messageId = UUID.randomUUID().toString

      val command = TestActorRef(new Actor {
        def receive = {
          case SendMessage(phoneNumber: String, message: Message) ⇒ {
            sender ! CreateMessageSuccess(MessageResponse(User(senderId, "Test1", "male", "abc.jpg"), User(receiverId, "test2", "female", "xyz.ping"), MessageDetail(messageId, "message", None)))
          }
        }
      })
      when(dseGraphPersistenceFactoryApi.isUserSuspendAlready(senderId)) thenReturn None
      val result = Await.result(toHandleMessage(command, senderId, MessageJsonData("message"), receiverId), 5 second)
      assert(result.status equals (StatusCodes.BadRequest))
    }

    "send fail message if user sends a message with empty message " in {
      val senderId = UUID.randomUUID().toString
      val receiverId = UUID.randomUUID().toString

      val command = TestActorRef(new Actor {
        def receive = {
          case SendMessage(phoneNumber: String, message: Message) ⇒ {
            sender ! CreateMessageFailure(new Exception(s"Talk not found."), MSG_002)
          }
        }
      })
      when(redisFactory.isTalkExist(senderId, receiverId)) thenReturn Future(true)
      when(dseGraphPersistenceFactoryApi.isUserSuspendAlready(senderId)) thenReturn Some(false)
      val result = Await.result(toHandleMessage(command, senderId, MessageJsonData(""), receiverId), 5 second)
      assert(result.status equals (StatusCodes.BadRequest))
    }

    "not be able to send a message when user is not friend" in {
      val senderId = UUID.randomUUID().toString
      val receiverId = UUID.randomUUID().toString
      val messageId = UUID.randomUUID().toString

      val command = TestActorRef(new Actor {
        def receive = {
          case SendMessage(phoneNumber: String, message: Message) ⇒ {
            sender ! CreateMessageFailure(new Exception(s"Talk not found."), MSG_002)
          }
        }
      })
      val message = MessageJsonData("message")
      when(dseGraphPersistenceFactoryApi.isUserSuspendAlready(senderId)) thenReturn Some(false)
      when(redisFactory.isTalkExist(senderId, receiverId)) thenReturn Future(false)
      when(dseGraphPersistenceFactoryApi.isFollowingExists(senderId, receiverId)) thenReturn false
      val result = Await.result(toHandleMessage(command, senderId, message, receiverId), 5 second)
      assert(result.status equals (StatusCodes.BadRequest))
    }

    "be able to send a message when user is a friend while initiate talk" in {
      val senderId = UUID.randomUUID().toString
      val receiverId = UUID.randomUUID().toString
      val talkId = UUID.randomUUID().toString
      val messageId = UUID.randomUUID().toString

      val command = TestActorRef(new Actor {
        def receive = {
          case SendMessage(phoneNumber: String, message: Message) ⇒ {
            sender ! CreateMessageSuccess(MessageResponse(User(senderId, "Test1", "male", "abc.jpg"), User(receiverId, "test2", "female", "xyz.ping"), MessageDetail(messageId, "message", None)))
          }
        }
      })
      val message = MessageJsonData("message")
      when(dseGraphPersistenceFactoryApi.isUserSuspendAlready(senderId)) thenReturn Some(false)
      when(redisFactory.isTalkExist(senderId, receiverId)) thenReturn Future(false)
      when(dseGraphPersistenceFactoryApi.isFollowingExists(senderId, receiverId)) thenReturn true
      when(dseGraphPersistenceFactoryApi.isFollowingExists(receiverId, senderId)) thenReturn true
      val result = Await.result(toHandleMessage(command, senderId, message, receiverId), 5 second)
      assert(result.status equals (StatusCodes.OK))
    }

    "send success response when user is able to view all talks successfully" in {
      val senderId = UUID.randomUUID().toString
      val talkId = UUID.randomUUID().toString
      val date = new Date(1000L)

      val query = TestActorRef(new Actor {
        def receive = {
          case ViewTalks(userId, "1") ⇒ {
            sender ! ViewTalksSuccess(TalksResponse("0", "2", List(Talks(
              User(senderId, "cyril", "male", "picture"), LastMessage("1234567", date, "How are you?")
            ))))
          }
        }
      })
      val response: (Option[TalksResponse], Option[Error]) = (Some(TalksResponse("0", "2", List(Talks(
        User(senderId, "cyril", "male", "picture"), LastMessage("1234567", date, "How are you?")
      )))), None)
      when(messagingApi.getTalkLists("1", senderId)) thenReturn response
      val result = Await.result(viewTalks(query, None, senderId), 5 second)
      val expectedResponse = s"""{"resource":"talks","status":"success","errors":[],"data":{"response":{"previous":"0","next":"2","talks":[{"user":{"id":"$senderId","nickname":"cyril","gender":"male","picture":"picture"},"last":{"senderId":"1234567","timestamp":"1970-01-01T00:00:01Z","text":"How are you?"}}]}}}"""
      result shouldEqual expectedResponse
    }

    "send failure response when user is not able to view all talks successfully" in {
      val senderId = UUID.randomUUID().toString
      val query = TestActorRef(new Actor {
        def receive = {
          case ViewTalks(userId, "1") ⇒ {
            sender ! ViewTalksFailure(MSG_101, UNABLE_LOAD_TALK_LIST)
          }
        }
      })
      val response: (Option[TalksResponse], Option[Error]) = (None, Some(Error(MSG_101, UNABLE_LOAD_TALK_LIST)))
      when(messagingApi.getTalkLists("1", senderId)) thenReturn response
      val result = Await.result(viewTalks(query, None, senderId), 5 second)
      val expectedResponse = s"""{"resource":"talks","status":"failed","errors":[{"id":"MSG-101","message":"Unable loading talks list (generic error)."}],"data":{}}"""
      result shouldEqual expectedResponse
    }

    "send success response when user is able to view a single talk successfully" in {
      val senderId = UUID.randomUUID().toString
      val receiverId = UUID.randomUUID().toString
      val talkId = UUID.randomUUID().toString
      val messageId = UUID.randomUUID().toString
      val date = new Date(1000L)

      val query = TestActorRef(new Actor {
        def receive = {
          case ViewSingleTalk(receiverId, userId, Some(messageId), "desc") ⇒ {
            sender ! ViewSingleTalkSuccess(TalkResponse(
              User(senderId, "cyril", "male", "picture"),
              User(receiverId, "kais", "male", "picture"),
              List(UserMessageDetail(messageId, "How are you ?", date, senderId))
            ))
          }
        }
      })
      val response: (Option[TalkResponse], Option[Error]) = (Some(TalkResponse(
        User(senderId, "cyril", "male", "picture"),
        User(receiverId, "kais", "male", "picture"),
        List(UserMessageDetail(messageId, "How are you ?", date, senderId))
      )), None)
      when(messagingApi.getUserTalk(Some(messageId), senderId, receiverId, "desc")) thenReturn response
      val result = Await.result(viewSingleTalk(query, receiverId, Some(messageId), senderId, "desc"), 5 second)
      val expectedResponse = s"""{"resource":"talk","status":"success","errors":[],"data":{"response":{"me":{"id":"$senderId","nickname":"cyril","gender":"male","picture":"picture"},"user":{"id":"$receiverId","nickname":"kais","gender":"male","picture":"picture"},"messages":[{"id":"$messageId","text":"How are you ?","timestamp":"1970-01-01T00:00:01Z","from":"$senderId"}]}}}"""
      result shouldEqual expectedResponse
    }

    "send failure response when user is not able to view a single talk successfully" in {
      val senderId = UUID.randomUUID().toString
      val receiverId = UUID.randomUUID().toString
      val messageId = UUID.randomUUID().toString
      val query = TestActorRef(new Actor {
        def receive = {
          case ViewSingleTalk(receiverId, userId, Some(messageId), "desc") ⇒ {
            sender ! ViewSingleTalkFailure(MSG_102, UNABLE_LOAD_TALK)
          }
        }
      })
      val response: (Option[TalkResponse], Option[Error]) = (None, Some(Error(MSG_102, UNABLE_LOAD_TALK)))
      when(messagingApi.getUserTalk(Some(messageId), senderId, receiverId, "desc")) thenReturn response
      val result = Await.result(viewSingleTalk(query, receiverId, Some(messageId), senderId, "desc"), 5 second)
      val expectedResponse = s"""{"resource":"talk","status":"failed","errors":[{"id":"MSG-102","message":"Unable loading talk's messages (generic error)."}],"data":{}}"""
      result shouldEqual expectedResponse
    }

    "send success message, when a talk is removed successfully" in {

      val targetUserId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val input =
        s"""
          |{
          |"action":"removeTalk",
          |"targetUserId":"$targetUserId"
          |}
        """.stripMargin
      val actorRef = TestActorRef(new Actor {
        def receive = {
          case TalkRemove(talkId, userId) ⇒ {
            sender ! RemoveTalkSuccess("Talk removed successfully.")
          }
        }
      })
      val expectedOutput = """{"resource":"removeTalk","status":"success","errors":[],"data":{"removeResponse":"Talk removed successfully."}}"""
      val result = detectRequestAndPerform(actorRef, actorRef, userId, userId)
      val (pub, sub) = TestSource.probe[ws.Message]
        .via(result)
        .toMat(TestSink.probe[ws.Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(input))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send failure message, when a talk is not removed successfully" in {

      val targetUserId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val input =
        s"""
           |{
           |"action":"removeTalk",
           |"targetUserId":"$targetUserId"
           |}
        """.stripMargin
      val actorRef = TestActorRef(new Actor {
        def receive = {
          case TalkRemove(talkId, userId) ⇒ {
            sender ! RemoveTalkFailure(MSG_104, UNABLE_DELETE_TALK)
          }
        }
      })
      val expectedOutput = """{"resource":"removeTalk","status":"failed","errors":[{"id":"MSG-104","message":"Unable removing talk (generic error)."}],"data":{}}"""
      val result = detectRequestAndPerform(actorRef, actorRef, userId, userId)
      val (pub, sub) = TestSource.probe[ws.Message]
        .via(result)
        .toMat(TestSink.probe[ws.Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(input))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send success message, when a message is removed successfully" in {

      val targetUserId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val messageId = UUID.randomUUID().toString
      val input =
        s"""
           |{
           |"action":"removeMessage",
           |"targetUserId":"$targetUserId",
           |"messageId":"$messageId"
           |}
        """.stripMargin
      val actorRef = TestActorRef(new Actor {
        def receive = {
          case RemoveSingleMessage(talkId, messageId, userId) ⇒ {
            sender ! RemoveSingleMessageSuccess("Message removed successfully.")
          }
        }
      })
      val expectedOutput = """{"resource":"removeMessage","status":"success","errors":[],"data":{"removeResponse":"Message removed successfully."}}"""
      val result = detectRequestAndPerform(actorRef, actorRef, userId, userId)
      val (pub, sub) = TestSource.probe[ws.Message]
        .via(result)
        .toMat(TestSink.probe[ws.Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(input))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send failed message, when a message is not removed successfully" in {

      val targetUserId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val messageId = UUID.randomUUID().toString
      val input =
        s"""
           |{
           |"action":"removeMessage",
           |"targetUserId":"$targetUserId",
           |"messageId":"$messageId"
           |}
        """.stripMargin
      val actorRef = TestActorRef(new Actor {
        def receive = {
          case RemoveSingleMessage(talkId, messageId, userId) ⇒ {
            sender ! RemoveSingleMessageFailure(MSG_105, UNABLE_DELETE_MESSAGE)
          }
        }
      })
      val expectedOutput = """{"resource":"removeMessage","status":"failed","errors":[{"id":"MSG-105","message":"Unable removing message (generic error)."}],"data":{}}"""
      val result = detectRequestAndPerform(actorRef, actorRef, userId, userId)
      val (pub, sub) = TestSource.probe[ws.Message]
        .via(result)
        .toMat(TestSink.probe[ws.Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(input))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send failed message, when a remove message is not sent with proper json" in {

      val talkId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val messageId = UUID.randomUUID().toString
      val input =
        s"""
           |{
           |"action":"removeMessage",
           |"talkId":"$talkId"
           |}
        """.stripMargin
      val actorRef = TestActorRef(new Actor {
        def receive = {
          case RemoveSingleMessage(talkId, messageId, userId) ⇒ {
            sender ! JSONERROR
          }
        }
      })
      val expectedOutput = """{"resource":"removeMessage","status":"failed","errors":[{"id":"PRS-001","message":"Invalid JSON"}],"data":{}}"""
      val result = detectRequestAndPerform(actorRef, actorRef, userId, userId)
      val (pub, sub) = TestSource.probe[ws.Message]
        .via(result)
        .toMat(TestSink.probe[ws.Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(input))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send failed message, when a remove talk is not sent with proper json" in {

      val talkId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val messageId = UUID.randomUUID().toString
      val input =
        s"""
           |{
           |"action":"removeTalk"
           |}
        """.stripMargin
      val actorRef = TestActorRef(new Actor {
        def receive = {
          case TalkRemove(talkId, userId) ⇒ {
            sender ! JSONERROR
          }
        }
      })
      val expectedOutput = """{"resource":"removeTalk","status":"failed","errors":[{"id":"PRS-001","message":"Invalid JSON"}],"data":{}}"""
      val result = detectRequestAndPerform(actorRef, actorRef, userId, userId)
      val (pub, sub) = TestSource.probe[ws.Message]
        .via(result)
        .toMat(TestSink.probe[ws.Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(input))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send failed message, when a no proper action is sent in json" in {

      val talkId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val messageId = UUID.randomUUID().toString
      val input =
        s"""
           |{
           |"action":"remove"
           |}
        """.stripMargin
      val actorRef = TestActorRef(new Actor {
        def receive = {
          case TalkRemove(talkId, userId) ⇒ {
            sender ! JSONERROR
          }
        }
      })
      val expectedOutput = """{"resource":"ws://localhost:8080","status":"failed","errors":[{"id":"ACT-101","message":"Action is missing."}],"data":{}}"""
      val result = detectRequestAndPerform(actorRef, actorRef, userId, userId)
      val (pub, sub) = TestSource.probe[ws.Message]
        .via(result)
        .toMat(TestSink.probe[ws.Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(input))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send failed message, when a no action is sent at all in json" in {

      val talkId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val messageId = UUID.randomUUID().toString
      val input =
        s"""
           |{
           |"talkId":"$talkId"
           |}
        """.stripMargin
      val actorRef = TestActorRef(new Actor {
        def receive = {
          case TalkRemove(talkId, userId) ⇒ {
            sender ! JSONERROR
          }
        }
      })
      val expectedOutput = """{"resource":"ws://localhost:8080","status":"failed","errors":[{"id":"ACT-101","message":"Action is missing."}],"data":{}}"""
      val result = detectRequestAndPerform(actorRef, actorRef, userId, userId)
      val (pub, sub) = TestSource.probe[ws.Message]
        .via(result)
        .toMat(TestSink.probe[ws.Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(input))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send success response when user is able to search message successfully" in {
      val senderId = UUID.randomUUID().toString
      val messageId = UUID.randomUUID().toString
      val date = new Date(1000L)
      val query = TestActorRef(new Actor {
        def receive = {
          case SearchMessage(userId, messgae) ⇒ {
            sender ! SearchMessageSuccess(List(SearchMessageResponse(messageId, "messageText", date, senderId, "userNickname", "male", "userPic", senderId)))
          }
        }
      })
      val response: (Option[List[SearchMessageResponse]], Option[Error]) = (Some(List(SearchMessageResponse(messageId, "messageText", date, senderId, "userNickname", "male", "userPic", senderId))), None)
      when(messagingApi.fullTextMessageSearch(senderId, "messgae")) thenReturn response
      val result = Await.result(getByMessage(query, "message", senderId), 5 second)
      val expectedResponse = s"""{"resource":"searchmsg","status":"success","errors":[],"data":{"response":[{"id":"$messageId","text":"messageText","launchedTime":"1970-01-01T00:00:01Z","userId":"$senderId","userNickname":"userNickname","userGender":"male","userMainPic":"userPic","senderId":"$senderId"}]}}"""
      result shouldEqual expectedResponse
    }

    "send failure response when user is not able to search message successfully" in {
      val senderId = UUID.randomUUID().toString
      val messageId = UUID.randomUUID().toString
      val query = TestActorRef(new Actor {
        def receive = {
          case SearchMessage(userId, messgae) ⇒ {
            sender ! SearchMessageFailure(MSG_106, UNABLE_SEARCHING_MESSAGES)
          }
        }
      })
      val response: (Option[List[SearchMessageResponse]], Option[Error]) = (None, Some(Error(MSG_106, UNABLE_SEARCHING_MESSAGES)))
      when(messagingApi.fullTextMessageSearch(senderId, "messgae")) thenReturn response
      val result = Await.result(getByMessage(query, "message", senderId), 5 second)
      val expectedResponse = s"""{"resource":"searchmsg","status":"failed","errors":[{"id":"MSG-106","message":"Unable searching message (generic error)."}],"data":{}}"""
      result shouldEqual expectedResponse
    }

    "send success response when user is able to search talker successfully" in {
      val senderId = UUID.randomUUID().toString
      val messageId = UUID.randomUUID().toString
      val date = new Date(1000L)
      val query = TestActorRef(new Actor {
        def receive = {
          case SearchTalker(talker) ⇒ {
            sender ! SearchTalkerSuccess(List(User(senderId, "userNickname", "male", "userPic")))
          }
        }
      })
      val response: (Option[List[User]], Option[Error]) = (Some(List(User(senderId, "userNickname", "male", "userPic"))), None)
      when(messagingApi.searchTalker("piy")) thenReturn response
      val result = Await.result(getByTalkers(query, "piy"), 5 second)
      val expectedResponse = s"""{"resource":"searchtalker","status":"success","errors":[],"data":{"response":[{"id":"$senderId","nickname":"userNickname","gender":"male","picture":"userPic"}]}}"""
      result shouldEqual expectedResponse
    }

    "send failure response when user is not able to search talker successfully" in {
      val senderId = UUID.randomUUID().toString
      val messageId = UUID.randomUUID().toString
      val query = TestActorRef(new Actor {
        def receive = {
          case SearchTalker(talker) ⇒ {
            sender ! SearchTalkerFailure(MSG_106, UNABLE_SEARCHING_TALKERS)
          }
        }
      })
      val response: (Option[List[User]], Option[Error]) = (None, Some(Error(MSG_106, UNABLE_SEARCHING_TALKERS)))
      when(messagingApi.searchTalker("piy")) thenReturn response
      val result = Await.result(getByTalkers(query, "piy"), 5 second)
      val expectedResponse = s"""{"resource":"searchtalker","status":"failed","errors":[{"id":"MSG-106","message":"Unable searching talkers (generic error)."}],"data":{}}"""
      result shouldEqual expectedResponse
    }

    "send success message, when a read message flag is updated successfully" in {

      val targetUserId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val messageId = UUID.randomUUID().toString
      val input =
        s"""
           |{
           |"action":"readmsg",
           |"userId":"$targetUserId",
           |"messageId":"$messageId"
           |}
        """.stripMargin
      val actorRef = TestActorRef(new Actor {
        def receive = {
          case ReadMessageUpdateFlag(userId, targetUserId, messageId) ⇒ {
            sender ! UpdateReadMessageFlagSuccess("saved successfully.")
          }
        }
      })
      val expectedOutput = """{"resource":"readmsg","status":"success","errors":[],"data":{"updateReadMessageFlagResponse":"saved successfully."}}"""
      val result = detectRequestAndPerform(actorRef, actorRef, userId, userId)
      val (pub, sub) = TestSource.probe[ws.Message]
        .via(result)
        .toMat(TestSink.probe[ws.Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(input))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send failed message, when a read message flag is not updated successfully" in {

      val targetUserId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val messageId = UUID.randomUUID().toString
      val input =
        s"""
           |{
           |"action":"readmsg",
           |"userId":"$targetUserId",
           |"messageId":"$messageId"
           |}
        """.stripMargin
      val actorRef = TestActorRef(new Actor {
        def receive = {
          case ReadMessageUpdateFlag(userId, targetUserId, messageId) ⇒ {
            sender ! UpdateReadMessageFlagFailure(MSG_107, s"Unable to flag message $messageId as read (generic error).")
          }
        }
      })
      val expectedOutput = s"""{"resource":"readmsg","status":"failed","errors":[{"id":"MSG-107","message":"Unable to flag message $messageId as read (generic error)."}],"data":{}}"""
      val result = detectRequestAndPerform(actorRef, actorRef, userId, userId)
      val (pub, sub) = TestSource.probe[ws.Message]
        .via(result)
        .toMat(TestSink.probe[ws.Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(input))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send failed message, when a read message flag is not sent with proper json" in {

      val targetUserId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val messageId = UUID.randomUUID().toString
      val input =
        s"""
           |{
           |"action":"readmsg",
           |"userId":"$targetUserId"
           |}
        """.stripMargin
      val actorRef = TestActorRef(new Actor {
        def receive = {
          case ReadMessageUpdateFlag(userId, targetUserId, messageId) ⇒ {
            sender ! JSONERROR
          }
        }
      })
      val expectedOutput = """{"resource":"readmsg","status":"failed","errors":[{"id":"PRS-001","message":"Invalid JSON"}],"data":{}}"""
      val result = detectRequestAndPerform(actorRef, actorRef, userId, userId)
      val (pub, sub) = TestSource.probe[ws.Message]
        .via(result)
        .toMat(TestSink.probe[ws.Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(input))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send failed when a typing notificaiton is not send due to in-correct userid" in {

      val targetUserId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val messageId = UUID.randomUUID().toString
      val input =
        s"""{"targetedUserId":"$targetUserId"}"""
      val expectedOutput = s"""TextMessage.Strict({"resource":"typing","status":"failed","errors":[{"id":"MSG-002","message":"Talk $targetUserId not found."}],"data":{}})"""
      when(redisFactory.isTalkExist(userId, targetUserId)) thenReturn Future(false)
      val res: TextMessage = Await.result(userTyping(userId, input), 5 second)
      assert(res.toString == expectedOutput)
    }

    "send failed when a typing notificaiton is not send  with proper json" in {

      val targetUserId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val messageId = UUID.randomUUID().toString
      val input =
        s"""{"invalidJson":"$targetUserId"}"""
      val expectedOutput = s"""TextMessage.Strict({"resource":"typing","status":"failed","errors":[{"id":"PRS-001","message":"Invalid JSON"}],"data":{}})"""
      val res: TextMessage = Await.result(userTyping(userId, input), 5 second)
      assert(res.toString == expectedOutput)
    }

    "send success message, when a typing user notification is hit" in {

      val targetUserId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val input =
        s"""
           |{
           |"action":"typing",
           |"targetedUserId":"$targetUserId"
           |}
        """.stripMargin
      val actorRef = TestActorRef(new Actor {
        def receive = {
          case _ ⇒ {
            sender ! "Success"
          }
        }
      })
      val expectedOutput = s"""{"resource":"typing","status":"success","errors":[],"data":{"userId":"$userId","targetUserId":"$targetUserId"}}"""
      when(redisFactory.isTalkExist(userId, targetUserId)) thenReturn Future(true)

      val result = detectRequestAndPerform(actorRef, actorRef, userId, userId)
      val (pub, sub) = TestSource.probe[ws.Message]
        .via(result)
        .toMat(TestSink.probe[ws.Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(input))
      sub.expectNext(TextMessage(expectedOutput))
    }

  }
}