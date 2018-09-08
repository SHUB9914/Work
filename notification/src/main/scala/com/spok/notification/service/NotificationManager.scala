package com.spok.notification.service

import akka.actor.{ ActorRef, Props }
import akka.util.Timeout
import com.rbmhtechnology.eventuate.EventsourcedView
import com.spok.model.NotificationDetail
import com.spok.persistence.factory.DSEUserNotificationFactoryApi
import com.spok.persistence.redis.RedisFactory
import com.spok.util.JsonHelper
import com.spok.util.Constant._

import scala.concurrent.duration._

// Commands
case class Create(notificationDetail: NotificationDetail)

case class SendNotification(notificationDetail: NotificationDetail)

case class ConnectUser(userId: String, subscriber: ActorRef)

case class DisConnectUser(userId: String)

case class NotificationRemove(notificationId: String, userId: String)

case class SendMessage(message: String, userId: String)

class NotificationManager(replicaId: String, override val eventLog: ActorRef)
    extends EventsourcedView with JsonHelper {

  import context.dispatcher

  private implicit val timeout = Timeout(10.seconds)

  //scalastyle:off
  private var notificationActors: Map[String, ActorRef] = Map.empty
  //scalastyle:on

  val dseGraphNotificationFactoryApi: DSEUserNotificationFactoryApi = DSEUserNotificationFactoryApi

  val redisFactory: RedisFactory = RedisFactory

  override val id = s"s-nm-$replicaId"

  /**
   * Command handler.
   */
  override def onCommand: Receive = {
    case Create(notificationDetail) => {
      logger.info(s"Command triggered to store notification ${notificationDetail}")
      notificationActor(notificationDetail.emitterId) forward CreateNotification(notificationDetail)
      sendNotification(notificationDetail)
    }

    case ConnectUser(userId, subscriber) => {
      redisFactory.storeConnectedUsers(NOTIFICATION_REDIS + userId, subscriber.path.toString)
      logger.info(s"Command triggered to connect notification receiver ${userId}")
    }

    case DisConnectUser(userId) => {
      logger.info(s"Command triggered to disconnect notification receiver ${userId}")
      redisFactory.remove(NOTIFICATION_REDIS + userId)
    }

    case SendNotification(notificationDetail) => {
      logger.info(s"Sending  notification via Manager ${notificationDetail}")
      sendNotification(notificationDetail)
      sender ! "Notification Sent!!!"
    }

    case NotificationRemove(notificationId, userId) => {
      logger.info(s"Command triggered to remove notification with notification id $notificationId and user id ${userId}")
      notificationActor(userId) forward RemoveNotification(notificationId, userId)
    }

    case SendMessage(message, userId) => {
      logger.info(s"Sending  Message via Manager ${userId}")
      redisFactory.fetchConnectedUsers(NOTIFICATION_REDIS + userId) filter (_.isDefined) map { sRef =>
        context.actorSelection(sRef.get) ! message
      }
    }

  }

  /**
   * Event handler.
   */
  override def onEvent: Receive = {
    case NotificationCreated(notification) => // Do Nothing
  }

  private def sendNotification(notification: NotificationDetail): Unit = {
    for (userId <- notification.userIds) {
      redisFactory.fetchConnectedUsers(NOTIFICATION_REDIS + userId) filter (_.isDefined) map { sRef =>
        for (receiver <- sRef) {
          val userResponse = dseGraphNotificationFactoryApi.getnotificationRespone(userId, notification)
          val notificationResponse = userResponse
          logger.info("Got receiver and Sending notification detail::" + userResponse)
          context.actorSelection(receiver) ! notificationResponse
        }
      }
    }
  }
  /**
   * Find or create and return the Notification actor by id.
   *
   * @param userId the notification id.
   * @return the Notification actor ActorRef.
   */
  private def notificationActor(userId: String): ActorRef = {
    notificationActors.get(userId) match {
      case Some(notificationActor) => notificationActor
      case None =>
        notificationActors = notificationActors + (userId -> context.actorOf(Props(
          createActor(userId)
        ), userId))
        notificationActors(userId)
    }
  }

  def createActor(notificationId: String): NotificationActor = {
    new NotificationActor(notificationId, Some(notificationId), eventLog)
  }

}
