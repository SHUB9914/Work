package com.spok.notification.service

import java.util.UUID

import akka.actor.{ Actor, ActorSystem, Props }
import akka.testkit.{ ImplicitSender, TestActorRef, TestKit }
import com.rbmhtechnology.eventuate.ReplicationEndpoint
import com.rbmhtechnology.eventuate.ReplicationEndpoint._
import com.rbmhtechnology.eventuate.log.leveldb.LeveldbEventLog
import com.spok.model.NotificationDetail
import com.spok.persistence.cassandra.CassandraProvider
import com.spok.persistence.factory.DSEUserNotificationFactoryApi
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ BeforeAndAfterAll, MustMatchers, WordSpecLike }

import scala.concurrent.duration._

class NotificationActorSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
    with WordSpecLike with MustMatchers with BeforeAndAfterAll with MockitoSugar {

  val session = CassandraProvider.session

  def this() = this(ActorSystem("NotificationActorSystem"))

  val endpoint = ReplicationEndpoint(id => LeveldbEventLog.props(logId = "NotificationActorSpec"))(_system)
  val eventLog = endpoint.logs(DefaultLogName)

  val actorRef = TestActorRef(new Actor {
    def receive = {
      case "hello" => throw new IllegalArgumentException("boom")
    }
  })

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
    session.get.close()
  }

  "A NotificationActor" must {
    "Persists a notification when asked to store notification detail" in {
      val id = UUID.randomUUID().toString
      val notification = NotificationDetail(List("", ""), "notification1", "comment", "user id", "emmiter id")
      val actorRef = system.actorOf(Props(new NotificationActor(id, Some(id), eventLog) {

        override val dseGraphNotificationFactoryApi: DSEUserNotificationFactoryApi = mock[DSEUserNotificationFactoryApi]
        when(dseGraphNotificationFactoryApi.storeUsersNotifications(notification)) thenReturn (None)
      }))

      actorRef ! CreateNotification(notification)
      expectMsgType[NotificationCreateSuccess](10 seconds)
    }

    "should send success message when the notification is removed" in {

      val id = UUID.randomUUID().toString
      val notificationId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new NotificationActor(id, Some(id), eventLog) {
        override val dseGraphNotificationFactoryApi: DSEUserNotificationFactoryApi = mock[DSEUserNotificationFactoryApi]
        when(dseGraphNotificationFactoryApi.removeNotification(notificationId, userId)) thenReturn ((true, "Removed"))
      }))
      actorRef ! RemoveNotification(notificationId, userId)
      expectMsg(10 seconds, RemoveNotificationSuccess(notificationId))
    }

    "should send notification not found error message when the notification is not found" in {

      val id = UUID.randomUUID().toString
      val notificationId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new NotificationActor(id, Some(id), eventLog) {
        override val dseGraphNotificationFactoryApi: DSEUserNotificationFactoryApi = mock[DSEUserNotificationFactoryApi]
        when(dseGraphNotificationFactoryApi.removeNotification(notificationId, userId)) thenReturn ((false, s"Notification $notificationId not found"))
      }))
      actorRef ! RemoveNotification(notificationId, userId)
      expectMsgType[RemoveNotificationFailure](10 seconds)
    }

    "should send generic error message when the notification is not removed" in {

      val id = UUID.randomUUID().toString
      val notificationId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new NotificationActor(id, Some(id), eventLog) {
        override val dseGraphNotificationFactoryApi: DSEUserNotificationFactoryApi = mock[DSEUserNotificationFactoryApi]
        when(dseGraphNotificationFactoryApi.removeNotification(notificationId, userId)) thenReturn
          ((false, s"Unable removing notification $notificationId (generic error)."))
      }))
      actorRef ! RemoveNotification(notificationId, userId)
      expectMsgType[RemoveNotificationFailure](10 seconds)
    }

  }

}
