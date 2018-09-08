package com.spok.notification.service

import akka.actor.ActorRef
import com.rbmhtechnology.eventuate.EventsourcedView
import com.spok.model.{ NotificationDetail, NotificationsResponse }
import com.spok.persistence.factory.{ DSEUserNotificationFactoryApi }
import com.spok.util.Constant._

// Commands
case class GetNotification(notificationId: String)

case class ViewWallNotifications(userId: String, pos: String)

// Replies
case class ReadSuccess(notificationDetail: Option[NotificationDetail])

case class ViewWallNotificationsSuccess(notifications: NotificationsResponse)
case class ViewWallNotificationsFailure(cause: Throwable, errorCode: String)

class NotificationView(replicaId: String, override val eventLog: ActorRef)
    extends EventsourcedView {
  val dseGraphNotificationFactoryApi: DSEUserNotificationFactoryApi = DSEUserNotificationFactoryApi
  private var allnotifications: Map[String, NotificationDetail] = Map.empty
  override val id = s"s-nv-$replicaId"

  /**
   * Command handler.
   */
  override def onCommand: Receive = {

    case GetNotification(notificationId) =>
      sender() ! ReadSuccess(allnotifications.get(notificationId))

    case ViewWallNotifications(userId, pos) => {
      val wallNotificationsDetails = dseGraphNotificationFactoryApi.getnotifications(userId, pos)
      wallNotificationsDetails match {
        case Some(notifications) => sender() ! ViewWallNotificationsSuccess(notifications)
        case None => sender() ! ViewWallNotificationsFailure(new Exception(LOADING_NOTIFICATION_GENERIC_ERROR), MYA_102)
      }
    }
  }

  /**
   * Event handlers.
   */
  override val onEvent: Receive = {

    case NotificationCreated(newNotification) => {
      allnotifications = allnotifications + (newNotification.notificationId -> newNotification)
    }

  }

}

