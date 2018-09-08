package com.spok.notification.handler

import akka.actor.{ Actor, ActorRef, ActorSystem }
import akka.http.scaladsl.model.ws.{ Message, TextMessage }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Keep, Source }
import akka.stream.testkit.scaladsl.{ TestSink, TestSource }
import akka.testkit.TestActorRef
import com.spok.model.{ Emitter, NotificationDetail, Notifications, NotificationsResponse }
import com.spok.notification.service.{ DisConnectUser, _ }
import com.spok.util.Constant._
import com.spok.util.RandomUtil
import org.scalatest.WordSpec
import org.scalatest.mock.MockitoSugar

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class NotificationRestServiceHandlerSpec extends WordSpec with NotificationRestServiceHandler with MockitoSugar with RandomUtil {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  val failureMessage = TextMessage(Source.single("Notification creation failed with error Test Error"))

  "Notification Rest Service Handler" should {

    "be able to store notification detail event" in {

      val actorRef = TestActorRef(new Actor {
        def receive = {
          case Create(notificationDetail: NotificationDetail) ⇒ {
            sender ! NotificationCreateSuccess(notificationDetail.notificationId)
          }
        }
      })
      val notificationDetail = NotificationDetail(List("123456", "654321"), "123", "Text", "SpokText", "123321")
      val result = Await.result(storeNotificationEvent(actorRef, notificationDetail), 5 second).asInstanceOf[String]

      assert(result == notificationDetail.notificationId)

    }

    "be not able to store notification detail event" in {

      val actorRef = TestActorRef(new Actor {
        def receive = {
          case Create(notificationDetail: NotificationDetail) ⇒ {
            sender ! NotificationCreateFailure(notificationDetail.notificationId, (new Exception("Test Error")))
          }
        }
      })
      val notificationDetail = NotificationDetail(List("123456", "654321"), "123", "Text", "SpokText", "123321")
      val result = Await.result(storeNotificationEvent(actorRef, notificationDetail), 5 second).asInstanceOf[String]

      assert(result == "Notification creation failed with error Test Error")
    }

    "be not able to send notification detail event" in {

      val actorRef = TestActorRef(new Actor {
        def receive = {
          case SendNotification(notificationDetail) ⇒ {
            sender ! "Notification Sent!!!"
          }
        }
      })
      val notificationDetail = NotificationDetail(List("123456", "654321"), "123", "Text", "SpokText", "123321")
      val result = Await.result(sendNotificationEvent(actorRef, notificationDetail), 5 second).asInstanceOf[String]

      assert(result == "Notification Sent!!!")
    }

    "be not able to receive notification" in {
      val actorRef = TestActorRef(new Actor {
        private var connectedUsers: Map[String, ActorRef] = Map.empty
        def receive = {
          case ConnectUser(userId, subscriber) => {
            connectUserActor(userId, subscriber)
            sender ! NotificationCreateSuccess(userId)
          }

          case DisConnectUser(userId) => sender ! NotificationCreateSuccess(userId)

          case userId: String => connectedUsers.get(userId).get ! "Hello"
        }

        def connectUserActor(userId: String, subscriber: ActorRef): ActorRef = {
          logger.info(s"User ${userId} connected to receive notification!!!!")
          connectedUsers.get(userId) match {
            case Some(connectActor) => connectActor
            case None =>
              connectedUsers += (userId -> subscriber)
              connectedUsers(userId)
          }
        }
      })
      val userId = "5ad25ab8-e44f-4590-8e82-8bf0c974991e"
      val result = receiveNotificationFromUser(actorRef, actorRef, userId, userId)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      actorRef ! userId
      sub.ensureSubscription()
    }

    "be able to remove a notification" in {
      val notificationId = getUUID()
      val input = s"""{"action":"removeNotification","notificationId":"$notificationId"}"""
      val userId = getUUID()
      val command = TestActorRef(new Actor {
        def receive = {
          case NotificationRemove(notificationId, userId) ⇒ {
            sender ! RemoveNotificationSuccess(notificationId)
          }
        }
      })
      val result = detectRequestAndPerform(command, command, userId, "phoneNumber", input)
      Thread.sleep(5000)
      assert(result.isCompleted)
      result map { tm =>
        assert(tm === TextMessage(write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(RemoveNotificationSuccess(notificationId))))))
      }
      Thread.sleep(5000)
    }

    "be able to send notification not found error when notification not found" in {

      val notificationId = getUUID()
      val input = s"""{"action":"removeNotification","notificationId":"$notificationId"}"""
      val userId = getUUID()
      val command = TestActorRef(new Actor {
        def receive = {
          case NotificationRemove(notificationId, userId) ⇒ {
            sender ! RemoveNotificationFailure(notificationId, new Exception(s"Notification $notificationId not found"), MYA_001)
          }
        }
      })
      val result = detectRequestAndPerform(command, command, userId, "phoneNumber", input)
      val response = s"""{"resource":"ws://api.spok.me","status":"failed","errors":[{"id":"MYA-001","message":"Notification $notificationId not found"}]}"""
      Thread.sleep(5000)
      assert(result.isCompleted)
      result map { tm =>
        assert(tm === TextMessage(response))
      }
      Thread.sleep(5000)
    }

    "be able to send generic error when notification not removed" in {

      val notificationId = getUUID()
      val input = s"""{"action":"removeNotification","notificationId":"$notificationId"}"""
      val userId = getUUID()
      val command = TestActorRef(new Actor {
        def receive = {
          case NotificationRemove(notificationId, userId) ⇒ {
            sender ! RemoveNotificationFailure(notificationId, new Exception(s"Unable removing notification $notificationId (generic error)."), MYA_103)
          }
        }
      })
      val result = detectRequestAndPerform(command, command, userId, "phoneNumber", input)
      val response = s"""{"resource":"ws://api.spok.me","status":"failed","errors":[{"id":"MYA-103","message":"Unable removing notification $notificationId (generic error)."}]}"""
      Thread.sleep(5000)
      assert(result.isCompleted)
      result map { tm =>
        assert(tm === TextMessage(response))
      }
      Thread.sleep(5000)
    }

    "not able to remove a notification if user sends json with wrong action" in {
      val notificationId = getUUID()
      val input = s"""{"action":"removeNotification1","notificationId":"$notificationId"}"""
      val userId = getUUID()
      val command = TestActorRef(new Actor {
        def receive = {
          case NotificationRemove(notificationId, userId) ⇒ {
            sender ! RemoveNotificationFailure(notificationId, new Exception(s"Huhh!!! I didn't get your request."), MYA_001)
          }
        }
      })
      val result = detectRequestAndPerform(command, command, userId, "phoneNumber", input)
      val response = """{"resource":"ws://api.spok.me","status":"failed","errors":[{"id":"ACT_101","message":"Huhh!!! I didn't get your request."}]}"""
      Thread.sleep(5000)
      assert(result.isCompleted)
      result map { tm =>
        assert(tm === TextMessage(response))
      }
      Thread.sleep(5000)
    }

    "not able to remove a notification if user not sends action in json " in {
      val notificationId = getUUID()
      val input = s"""{"action1":"removeNotification","notificationId":"$notificationId"}"""
      val userId = getUUID()
      val command = TestActorRef(new Actor {
        def receive = {
          case NotificationRemove(notificationId, userId) ⇒ {
            sender ! RemoveNotificationFailure(notificationId, new Exception("Action is missing."), MYA_001)
          }
        }
      })
      val result = detectRequestAndPerform(command, command, userId, "phoneNumber", input)
      val response = """{"resource":"ws://api.spok.me","status":"failed","errors":[{"id":"ACT_101","message":"Action is missing."}]}"""
      Thread.sleep(5000)
      assert(result.isCompleted)
      result map { tm =>
        assert(tm === TextMessage(response))
      }
      Thread.sleep(5000)
    }

    "not able to remove a notification if user not sends notificationId in json " in {
      val notificationId = getUUID()
      val input = s"""{"action":"removeNotification","notificationId1":"$notificationId"}"""
      val userId = getUUID()
      val command = TestActorRef(new Actor {
        def receive = {
          case NotificationRemove(notificationId, userId) ⇒ {
            sender ! RemoveNotificationFailure(notificationId, new Exception("NotificationId Id not found!!!"), MYA_001)
          }
        }
      })
      val result = detectRequestAndPerform(command, command, userId, "phoneNumber", input)
      val response = """{"resource":"ws://api.spok.me","status":"failed","errors":[{"id":"MYA-001","message":"NotificationId Id not found!!!"}]}"""
      Thread.sleep(5000)
      assert(result.isCompleted)
      result map { tm =>
        assert(tm === TextMessage(response))
      }
      Thread.sleep(5000)
    }

    "view wall notifications successfully" in {
      val userId = getUUID()
      val wallNotifications = NotificationsResponse("0", "2", List(Notifications("id", "notificationType", "relatedTo", "timestamp", Emitter("emitterId", "respoker", "male", "reSpoker.jpg"))))
      val query = TestActorRef(new Actor {
        def receive = {
          case ViewWallNotifications(userId1, "1") ⇒ {
            sender() ! ViewWallNotificationsSuccess(wallNotifications)
          }
        }
      })
      val result = Await.result(getNotificationHandler(query, userId, Some("1")), 5 second)
      val response = """{"resource":"getNotifications","status":"success","errors":[],"data":{"previous":"0","next":"2","notifications":[{"id":"id","notificationType":"notificationType","relatedTo":"relatedTo","timestamp":"timestamp","emitter":{"emitterId":"emitterId","nickname":"respoker","gender":"male","picture":"reSpoker.jpg"}}]}}"""
      assert(result === response)
    }

    "view wall notifications successfully without segment" in {
      val userId = getUUID()
      val wallNotifications = NotificationsResponse("0", "2", List(Notifications("id", "notificationType", "relatedTo", "timestamp", Emitter("emitterId", "respoker", "male", "reSpoker.jpg"))))
      val query = TestActorRef(new Actor {
        def receive = {
          case ViewWallNotifications(userId1, "1") ⇒ {
            sender() ! ViewWallNotificationsSuccess(wallNotifications)
          }
        }
      })
      val result = Await.result(getNotificationHandler(query, userId, None), 5 second)
      val response = """{"resource":"getNotifications","status":"success","errors":[],"data":{"previous":"0","next":"2","notifications":[{"id":"id","notificationType":"notificationType","relatedTo":"relatedTo","timestamp":"timestamp","emitter":{"emitterId":"emitterId","nickname":"respoker","gender":"male","picture":"reSpoker.jpg"}}]}}"""
      assert(result === response)
    }

    "Generic error while viewing wall notifications" in {
      val userId = getUUID()
      val query = TestActorRef(new Actor {
        def receive = {
          case ViewWallNotifications(userId1, "1") ⇒ {
            sender() ! ViewWallNotificationsFailure(new Exception(LOADING_NOTIFICATION_GENERIC_ERROR), MYA_102)
          }
        }
      })
      val result = Await.result(getNotificationHandler(query, userId, Some("1")), 5 second)
      val output = s"""{"resource":"ws://api.spok.me","status":"failed","errors":[{"id":"MYA-102","message":"$LOADING_NOTIFICATION_GENERIC_ERROR"}],"data":{}}"""
      assert(result === output)
    }

    "Generic error while viewing wall notifications without segment" in {
      val userId = getUUID()
      val query = TestActorRef(new Actor {
        def receive = {
          case ViewWallNotifications(userId1, "1") ⇒ {
            sender() ! ViewWallNotificationsFailure(new Exception(LOADING_NOTIFICATION_GENERIC_ERROR), MYA_102)
          }
        }
      })
      val result = Await.result(getNotificationHandler(query, userId, None), 5 second)
      val output = s"""{"resource":"ws://api.spok.me","status":"failed","errors":[{"id":"MYA-102","message":"$LOADING_NOTIFICATION_GENERIC_ERROR"}],"data":{}}"""
      assert(result === output)
    }

  }
}
