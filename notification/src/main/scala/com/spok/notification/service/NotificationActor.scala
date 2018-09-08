package com.spok.notification.service

import akka.actor.ActorRef
import com.rbmhtechnology.eventuate.EventsourcedActor
import com.spok.model.{ NotificationDetail, SpokDataResponse }
import com.spok.persistence.factory.DSEUserNotificationFactoryApi

import scala.util.{ Failure, Success }
import com.spok.util.Constant._

// Commands
case class CreateNotification(notificationDetail: NotificationDetail)
case class RemoveNotification(notificationId: String, userId: String)

// Events
case class NotificationCreated(notificationDetail: NotificationDetail)
case class NotificationRemoved(notificationId: String, userId: String)

sealed trait NotificationAck

// Replies
case class NotificationCreateSuccess(id: String) extends NotificationAck
case class NotificationCreateFailure(id: String, cause: Throwable) extends NotificationAck
case class RemoveNotificationSuccess(notificationId: String) extends NotificationAck with SpokDataResponse
case class RemoveNotificationFailure(notificationId: String, cause: Throwable, errorCode: String) extends NotificationAck

class NotificationActor(override val id: String, override val aggregateId: Option[String], val eventLog: ActorRef) extends EventsourcedActor {

  private var currentState: Vector[NotificationDetail] = Vector.empty
  val dseGraphNotificationFactoryApi: DSEUserNotificationFactoryApi = DSEUserNotificationFactoryApi

  /**
   * Command handlers.
   */
  override val onCommand: Receive = {

    case CreateNotification(notificationDetail: NotificationDetail) => {
      persist(NotificationCreated(notificationDetail)) {
        case Success(evt) =>
          dseGraphNotificationFactoryApi.storeUsersNotifications(notificationDetail)
          sender() ! NotificationCreateSuccess(notificationDetail.notificationId)
        case Failure(err) =>
          sender() ! NotificationCreateFailure(notificationDetail.notificationId, err)
      }
    }

    case RemoveNotification(notificationId: String, userId: String) => {
      persist(NotificationRemoved(notificationId, userId)) {
        case Success(evt) => {
          val removeNotification = dseGraphNotificationFactoryApi.removeNotification(notificationId, userId)
          logger.info(s"remove notification response is $removeNotification")
          removeNotification match {
            case (true, msg) => sender() ! RemoveNotificationSuccess(notificationId)
            case (false, msg) => sender() ! RemoveNotificationFailure(notificationId, new Exception(msg), MYA_001)
          }
        }
        case Failure(err) =>
          sender() ! RemoveNotificationFailure(notificationId, new Exception(s"Unable removing notification $notificationId (generic error)."), MYA_103)
      }
    }

  }

  /**
   * Event handlers.
   */
  override val onEvent: Receive = {

    case NotificationCreated(newNotification) =>
      currentState = currentState :+ newNotification

  }

}

