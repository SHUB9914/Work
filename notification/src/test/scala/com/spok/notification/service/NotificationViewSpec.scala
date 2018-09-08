package com.spok.notification.service

import java.util.UUID

import akka.actor.{ ActorSystem, PoisonPill, Props }
import akka.testkit.{ ImplicitSender, TestKit }
import com.rbmhtechnology.eventuate.ReplicationEndpoint
import com.rbmhtechnology.eventuate.ReplicationEndpoint._
import com.rbmhtechnology.eventuate.log.leveldb.LeveldbEventLog
import com.spok.model.{ Emitter, NotificationDetail, Notifications, NotificationsResponse }
import com.spok.persistence.cassandra.CassandraProvider
import com.spok.persistence.factory.DSEUserNotificationFactoryApi
import com.spok.util.Constant._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ BeforeAndAfterAll, MustMatchers, WordSpecLike }

import scala.concurrent.duration._
class NotificationViewSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
    with WordSpecLike with MustMatchers with BeforeAndAfterAll with MockitoSugar {

  val session = CassandraProvider.session

  def this() = this(ActorSystem("NotificationViewSystem"))

  val endpoint = ReplicationEndpoint(id => LeveldbEventLog.props(logId = "NotificationViewSpec"))(_system)
  val eventLog = endpoint.logs(DefaultLogName)

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
    session.get.close()
  }

  "A NotificationView" must {
    "Fetch a notification by its id" in {
      val id = UUID.randomUUID().toString
      val notification = NotificationDetail(List(""), "notification1", "comment1", "user id1", "emmiter id")
      val actorRef = system.actorOf(Props(new NotificationActor(id, Some(id), eventLog) {
        override val dseGraphNotificationFactoryApi: DSEUserNotificationFactoryApi = mock[DSEUserNotificationFactoryApi]
        when(dseGraphNotificationFactoryApi.storeUsersNotifications(notification)) thenReturn (None)
      }))

      actorRef ! CreateNotification(notification)
      expectMsgType[NotificationCreateSuccess](10 seconds)
      actorRef ! PoisonPill

      val actorRef2 = system.actorOf(Props(new NotificationView(endpoint.id, eventLog)))
      actorRef2 ! GetNotification("notification1")
      expectMsgPF() {
        case ReadSuccess(notificationOpt) => notificationOpt.map(_.notificationId).get mustBe "notification1"
      }
    }

    "be able to view wall notifications" in {
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val reSpokerId = UUID.randomUUID().toString
      val wallNotifications = NotificationsResponse("0", "2", List(Notifications("id", "notificationType", "relatedTo", "timestamp", Emitter("emitterId", "respoker", "male", "reSpoker.jpg"))))
      val actorRef = system.actorOf(Props(new NotificationView(id, eventLog) {
        override val dseGraphNotificationFactoryApi: DSEUserNotificationFactoryApi = mock[DSEUserNotificationFactoryApi]
        when(dseGraphNotificationFactoryApi.getnotifications(userId, "1")) thenReturn Some(wallNotifications)
      }))
      actorRef ! ViewWallNotifications(userId, "1")
      expectMsgPF() {
        case ViewWallNotificationsSuccess(value) =>
          value mustEqual wallNotifications
      }
    }
    "not able to view wall notifications" in {
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new NotificationView(id, eventLog) {
        override val dseGraphNotificationFactoryApi: DSEUserNotificationFactoryApi = mock[DSEUserNotificationFactoryApi]
        when(dseGraphNotificationFactoryApi.getnotifications(userId, "1")) thenReturn None
      }))
      actorRef ! ViewWallNotifications(userId, "1")
      expectMsgPF() {
        case ViewWallNotificationsFailure(value, MYA_102) =>
          value.getMessage mustEqual LOADING_NOTIFICATION_GENERIC_ERROR
      }
    }
  }

}
