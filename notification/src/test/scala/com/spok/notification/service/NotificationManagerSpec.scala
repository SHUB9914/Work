package com.spok.notification.service

import java.util.UUID

import akka.actor.{ Actor, ActorRef, ActorSystem, Props }
import akka.testkit.{ ImplicitSender, TestActorRef, TestKit }
import com.rbmhtechnology.eventuate.ReplicationEndpoint
import com.rbmhtechnology.eventuate.ReplicationEndpoint._
import com.rbmhtechnology.eventuate.log.leveldb.LeveldbEventLog
import com.spok.model.NotificationDetail
import com.spok.persistence.cassandra.CassandraProvider
import com.spok.persistence.factory.DSEUserNotificationFactoryApi
import com.spok.util.Constant._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ BeforeAndAfterAll, MustMatchers, WordSpecLike }

import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Await

class NotificationManagerSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
    with WordSpecLike with MustMatchers with BeforeAndAfterAll with MockitoSugar {

  val session = CassandraProvider.session

  def this() = this(ActorSystem("NotificationManagerSystem"))

  val endpoint = ReplicationEndpoint(id => LeveldbEventLog.props(logId = "NotificationManagerSpec"))(_system)
  val eventLog = endpoint.logs(DefaultLogName)

  val testActorRef = TestActorRef(new Actor {
    def receive = {
      case "hello" => throw new IllegalArgumentException("boom")
    }
  })
  implicit val timeout = Timeout(40 seconds)

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
    session.get.close()
  }

  "A NotificationManager" must {

    "should remove notification" in {

      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val notificationId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new NotificationManager(id, eventLog) {
        private var connectedUsers: Map[String, ActorRef] = Map(userId -> testActorRef)

        override def createActor(actorId: String): NotificationActor = {
          new NotificationActor(actorId, Some(actorId), eventLog) {
            override val dseGraphNotificationFactoryApi: DSEUserNotificationFactoryApi = mock[DSEUserNotificationFactoryApi]
            when(dseGraphNotificationFactoryApi.removeNotification(notificationId, userId)) thenReturn ((true, "Removed"))
          }
        }
      }))
      actorRef ! NotificationRemove(notificationId, userId)
      expectMsgType[RemoveNotificationSuccess](10 second)
    }

    "should give error message when notification is not found" in {

      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val notificationId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new NotificationManager(id, eventLog) {
        private var connectedUsers: Map[String, ActorRef] = Map(userId -> testActorRef)

        override def createActor(actorId: String): NotificationActor = {
          new NotificationActor(actorId, Some(actorId), eventLog) {
            override val dseGraphNotificationFactoryApi: DSEUserNotificationFactoryApi = mock[DSEUserNotificationFactoryApi]
            when(dseGraphNotificationFactoryApi.removeNotification(notificationId, userId)) thenReturn ((false, s"Notification $notificationId not found"))
          }
        }
      }))
      actorRef ! NotificationRemove(notificationId, userId)
      expectMsgType[RemoveNotificationFailure](10 seconds)
    }

    "should give generic error message when notification is not removed" in {

      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val notificationId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new NotificationManager(id, eventLog) {
        private var connectedUsers: Map[String, ActorRef] = Map(userId -> testActorRef)

        override def createActor(actorId: String): NotificationActor = {
          new NotificationActor(actorId, Some(actorId), eventLog) {
            override val dseGraphNotificationFactoryApi: DSEUserNotificationFactoryApi = mock[DSEUserNotificationFactoryApi]
            when(dseGraphNotificationFactoryApi.removeNotification(notificationId, userId)) thenReturn
              ((false, s"Unable removing notification $notificationId (generic error)."))
          }
        }
      }))
      actorRef ! NotificationRemove(notificationId, userId)
      expectMsgType[RemoveNotificationFailure](10 seconds)
    }

    "Create a notification by Notification manager" in {
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val notificationId = UUID.randomUUID().toString
      val notification = NotificationDetail(List(userId + "123"), notificationId, REGISTERED, userId, userId)

      val actorRef = system.actorOf(Props(new NotificationManager(id, eventLog) {
        val result = s"""{"emitter":{"id": "$id","nickName": "nickName","gender": "gender","picture": "picture"}}"""
        private var connectedUsers: Map[String, ActorRef] = Map(userId -> testActorRef)
        override val dseGraphNotificationFactoryApi: DSEUserNotificationFactoryApi = mock[DSEUserNotificationFactoryApi]
        when(dseGraphNotificationFactoryApi.getnotificationRespone(userId, notification)) thenReturn (result)

        override def createActor(notificationId: String): NotificationActor = {
          new NotificationActor(notificationId, Some(notificationId), eventLog) {
            override val dseGraphNotificationFactoryApi: DSEUserNotificationFactoryApi = mock[DSEUserNotificationFactoryApi]
            when(dseGraphNotificationFactoryApi.storeUsersNotifications(notification)) thenReturn (None)
          }
        }

      }))
      actorRef ! ConnectUser(userId, testActorRef)
      Thread.sleep(5000)
      actorRef ! Create(notification)
      expectMsgType[NotificationCreateSuccess](10 seconds)
    }

    "Create a mention notification by Notification manager" in {
      implicit val timeout = Timeout(40 seconds)
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val notificationId = UUID.randomUUID().toString
      val notification = NotificationDetail(List(userId), notificationId, MENTION, userId, userId)
      val result = s"""{"emitter":{"id": "$id","nickName": "nickName","gender": "gender","picture": "picture"}}"""
      val actorRef = system.actorOf(Props(new NotificationManager(id, eventLog) {
        val result = s"""{"emitter":{"id": "$id","nickName": "nickName","gender": "gender","picture": "picture"}}"""
        override val dseGraphNotificationFactoryApi: DSEUserNotificationFactoryApi = mock[DSEUserNotificationFactoryApi]
        when(dseGraphNotificationFactoryApi.getnotificationRespone(userId, notification)) thenReturn (result)

      }))
      actorRef ! ConnectUser(userId, testActorRef)
      Thread.sleep(5000)
      val futureResponse = ask(actorRef, SendNotification(notification))
      val res = Await.result(futureResponse, 10 second)

      assert(res == "Notification Sent!!!")
    }

    "Send notification by Notification Manager when a user registers" in {
      implicit val timeout = Timeout(40 seconds)
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val notificationId = UUID.randomUUID().toString
      val notification = NotificationDetail(List(userId), notificationId, REGISTERED, userId, userId)
      val actorRef = system.actorOf(Props(new NotificationManager(id, eventLog) {
        val result = s"""{"emitter":{"id": "$id","nickName": "nickName","gender": "gender","picture": "picture"}}"""
        private var connectedUsers: Map[String, ActorRef] = Map(userId -> testActorRef)
        override val dseGraphNotificationFactoryApi: DSEUserNotificationFactoryApi = mock[DSEUserNotificationFactoryApi]
        when(dseGraphNotificationFactoryApi.getnotificationRespone(userId, notification)) thenReturn (result)
      }))
      actorRef ! ConnectUser(userId, testActorRef)
      Thread.sleep(5000)
      val futureResponse = ask(actorRef, SendNotification(notification))
      val result = Await.result(futureResponse, 10 second)

      assert(result == "Notification Sent!!!")
    }

    "Send notification by Notification Manager when a spok in unspoked" in {
      implicit val timeout = Timeout(40 seconds)
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val notificationId = UUID.randomUUID().toString
      val notification = NotificationDetail(List(userId + "456"), notificationId, UNSPOKED, userId, userId)
      val actorRef = system.actorOf(Props(new NotificationManager(id, eventLog) {
        val result = s"""{"emitter":{"id": "$id","nickName": "nickName","gender": "gender","picture": "picture"}}"""
        private var connectedUsers: Map[String, ActorRef] = Map(userId + "456" -> testActorRef)
        override val dseGraphNotificationFactoryApi: DSEUserNotificationFactoryApi = mock[DSEUserNotificationFactoryApi]
        when(dseGraphNotificationFactoryApi.getnotificationRespone(userId, notification)) thenReturn (result)
      }))
      actorRef ! ConnectUser(userId + "456", testActorRef)
      Thread.sleep(5000)
      val futureResponse = ask(actorRef, SendNotification(notification))
      val result = Await.result(futureResponse, 10 second)

      assert(result == "Notification Sent!!!")
    }

    "Send notification by Notification Manager when a comment is removed" in {
      implicit val timeout = Timeout(40 seconds)
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val notificationId = UUID.randomUUID().toString
      val notification = NotificationDetail(List(userId), notificationId, REMOVE_COMMENT, userId, userId)
      val actorRef = system.actorOf(Props(new NotificationManager(id, eventLog) {
        val result = s"""{"emitter":{"id": "$id","nickName": "nickName","gender": "gender","picture": "picture"}}"""
        private var connectedUsers: Map[String, ActorRef] = Map(userId -> testActorRef)
        override val dseGraphNotificationFactoryApi: DSEUserNotificationFactoryApi = mock[DSEUserNotificationFactoryApi]
        when(dseGraphNotificationFactoryApi.getnotificationRespone(userId, notification)) thenReturn (result)
      }))
      actorRef ! ConnectUser(userId, testActorRef)
      Thread.sleep(5000)
      val futureResponse = ask(actorRef, SendNotification(notification))
      val result = Await.result(futureResponse, 10 second)

      assert(result == "Notification Sent!!!")
    }

    "Send notification by Notification Manager when user profile is updated" in {
      implicit val timeout = Timeout(40 seconds)
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val notificationId = UUID.randomUUID().toString
      val notification = NotificationDetail(List(userId), notificationId, USER_PROFILE_UPDATE_SUCCESS, userId, userId)
      val actorRef = system.actorOf(Props(new NotificationManager(id, eventLog) {
        val result = s"""{"emitter":{"id": "$id","nickName": "nickName","gender": "gender","picture": "picture"}}"""
        private var connectedUsers: Map[String, ActorRef] = Map(userId -> testActorRef)
        override val dseGraphNotificationFactoryApi: DSEUserNotificationFactoryApi = mock[DSEUserNotificationFactoryApi]
        when(dseGraphNotificationFactoryApi.getnotificationRespone(userId, notification)) thenReturn (result)
      }))
      actorRef ! ConnectUser(userId, testActorRef)
      Thread.sleep(5000)
      val futureResponse = ask(actorRef, SendNotification(notification))
      val result = Await.result(futureResponse, 10 second)

      assert(result == "Notification Sent!!!")
    }

    "Send notification by Notification Manager when a comment is added" in {

      implicit val timeout = Timeout(40 seconds)
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val notificationId = UUID.randomUUID().toString
      val notification = NotificationDetail(List(userId), notificationId, COMMENT_ADDED, spokId, userId)
      val actorRef = system.actorOf(Props(new NotificationManager(id, eventLog) {
        val result = s"""{"emitter":{"id": "$id","nickName": "nickName","gender": "gender","picture": "picture"}}"""
        private var connectedUsers: Map[String, ActorRef] = Map(userId -> testActorRef)
        override val dseGraphNotificationFactoryApi: DSEUserNotificationFactoryApi = mock[DSEUserNotificationFactoryApi]
        when(dseGraphNotificationFactoryApi.getnotificationRespone(userId, notification)) thenReturn (result)
      }))
      actorRef ! ConnectUser(userId, testActorRef)
      Thread.sleep(5000)
      val futureResponse = ask(actorRef, SendNotification(notification))
      val result = Await.result(futureResponse, 10 second)
      assert(result == "Notification Sent!!!")
    }

    "Send notification by Notification Manager when a comment is updated" in {

      implicit val timeout = Timeout(40 seconds)
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val notificationId = UUID.randomUUID().toString
      val notification = NotificationDetail(List(userId), notificationId, COMMENT_UPDATED, spokId, userId)
      val actorRef = system.actorOf(Props(new NotificationManager(id, eventLog) {
        val result = s"""{"emitter":{"id": "$id","nickName": "nickName","gender": "gender","picture": "picture"}}"""
        private var connectedUsers: Map[String, ActorRef] = Map(userId -> testActorRef)
        override val dseGraphNotificationFactoryApi: DSEUserNotificationFactoryApi = mock[DSEUserNotificationFactoryApi]
        when(dseGraphNotificationFactoryApi.getnotificationRespone(userId, notification)) thenReturn (result)
      }))
      actorRef ! ConnectUser(userId, testActorRef)
      Thread.sleep(5000)
      val futureResponse = ask(actorRef, SendNotification(notification))
      val result = Await.result(futureResponse, 10 second)
      assert(result == "Notification Sent!!!")
    }

    "Send notification by Notification Manager when a user is followed" in {

      implicit val timeout = Timeout(40 seconds)
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val notificationId = UUID.randomUUID().toString
      val notification = NotificationDetail(List(userId), notificationId, FOLLOWS, spokId, userId)
      val actorRef = system.actorOf(Props(new NotificationManager(id, eventLog) {
        val result = s"""{"emitter":{"id": "$id","nickName": "nickName","gender": "gender","picture": "picture"}}"""
        private var connectedUsers: Map[String, ActorRef] = Map(userId -> testActorRef)
        override val dseGraphNotificationFactoryApi: DSEUserNotificationFactoryApi = mock[DSEUserNotificationFactoryApi]
        when(dseGraphNotificationFactoryApi.getnotificationRespone(userId, notification)) thenReturn (result)
      }))
      actorRef ! ConnectUser(userId, testActorRef)
      Thread.sleep(5000)
      val futureResponse = ask(actorRef, SendNotification(notification))
      val result = Await.result(futureResponse, 10 second)
      assert(result == "Notification Sent!!!")
    }

    "Send notification by Notification Manager when a user is unfollowed" in {

      implicit val timeout = Timeout(40 seconds)
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val notificationId = UUID.randomUUID().toString
      val notification = NotificationDetail(List(userId), notificationId, UNFOLLOWS, spokId, userId)
      val actorRef = system.actorOf(Props(new NotificationManager(id, eventLog) {
        val result = s"""{"emitter":{"id": "$id","nickName": "nickName","gender": "gender","picture": "picture"}}"""
        private var connectedUsers: Map[String, ActorRef] = Map(userId -> testActorRef)
        override val dseGraphNotificationFactoryApi: DSEUserNotificationFactoryApi = mock[DSEUserNotificationFactoryApi]
        when(dseGraphNotificationFactoryApi.getnotificationRespone(userId, notification)) thenReturn (result)
      }))
      actorRef ! ConnectUser(userId, testActorRef)
      Thread.sleep(5000)
      val futureResponse = ask(actorRef, SendNotification(notification))
      val result = Await.result(futureResponse, 10 second)
      assert(result == "Notification Sent!!!")
    }

    "Send notification by Notification Manager when a default situation occurs" in {
      implicit val timeout = Timeout(40 seconds)
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val notificationId = UUID.randomUUID().toString
      val notification = NotificationDetail(List(userId), notificationId, "Default", userId, userId)
      val actorRef = system.actorOf(Props(new NotificationManager(id, eventLog) {
        val result = s"""{"emitter":{"id": "$id","nickName": "nickName","gender": "gender","picture": "picture"}}"""
        private var connectedUsers: Map[String, ActorRef] = Map(userId -> testActorRef)
        override val dseGraphNotificationFactoryApi: DSEUserNotificationFactoryApi = mock[DSEUserNotificationFactoryApi]
        when(dseGraphNotificationFactoryApi.getnotificationRespone(userId, notification)) thenReturn (result)
      }))
      actorRef ! ConnectUser(userId, testActorRef)
      Thread.sleep(5000)
      val futureResponse = ask(actorRef, SendNotification(notification))
      val result = Await.result(futureResponse, 10 second)

      assert(result == "Notification Sent!!!")
    }

    "Connect receiver by Notification manager" in {
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val notificationId = UUID.randomUUID().toString
      val notification = NotificationDetail(List(userId + "123"), notificationId, MENTION, userId, userId)
      val actorRef = system.actorOf(Props(new NotificationManager(id, eventLog)))
      actorRef ! ConnectUser(userId + "123", testActorRef)
      expectNoMsg()
    }

    "Dis Connect receiver by Notification manager" in {

      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val notificationId = UUID.randomUUID().toString
      val notification = NotificationDetail(List(userId + "456"), notificationId, MENTION, userId, userId)
      val actorRef = system.actorOf(Props(new NotificationManager(id, eventLog)))
      actorRef ! DisConnectUser(userId + "456")
      expectNoMsg()
    }

    "Send notification by Notification Manager when a spok is respoked" in {
      implicit val timeout = Timeout(40 seconds)
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val notificationId = UUID.randomUUID().toString
      val notification = NotificationDetail(List(userId + "456"), notificationId, RESPOKED, userId, userId)
      val actorRef = system.actorOf(Props(new NotificationManager(id, eventLog) {
        val result = s"""{"emitter":{"id": "$id","nickName": "nickName","gender": "gender","picture": "picture"}}"""
        private var connectedUsers: Map[String, ActorRef] = Map(userId + "456" -> testActorRef)
        override val dseGraphNotificationFactoryApi: DSEUserNotificationFactoryApi = mock[DSEUserNotificationFactoryApi]
        when(dseGraphNotificationFactoryApi.getnotificationRespone(userId, notification)) thenReturn (result)
      }))
      actorRef ! ConnectUser(userId + "456", testActorRef)
      Thread.sleep(5000)
      val futureResponse = ask(actorRef, SendNotification(notification))
      val result = Await.result(futureResponse, 10 second)

      assert(result == "Notification Sent!!!")
    }
  }
}
