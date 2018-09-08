package com.spok.notification.routes

import akka.actor.ActorRef
import akka.http.scaladsl.server._
import com.spok.model.NotificationDetail
import com.spok.notification.handler.NotificationRestServiceHandler
import org.json4s.{ DefaultFormats, Formats }
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * All REST routes of Notification are defined here. The Notification Service would be responding to these routes.
 */
trait NotificationRestService extends NotificationRestServiceHandler {

  implicit def json4sFormats: Formats = DefaultFormats
  val secretKey = "secret"

  // ==============================
  //     REST ROUTES
  // ==============================

  /**
   * After connecting websocket connection
   *
   * @example sample message { "notificationId": "123456", "notificationType": "comment", "userId": "66", "relatedTo": "test" }
   * @return Add notification web socket route
   */
  def notificationRoute(command: ActorRef, query: ActorRef): Route = post {
    path("notification" / "add") {
      entity(as[NotificationDetail]) { notificationDetail =>
        logDuration(onSuccess(storeNotificationEvent(command, notificationDetail))(complete(_)))
      }
    }
  }

  /**
   * Send notification to connected users
   *
   * @param command
   * @return
   */

  def sendNotification(command: ActorRef): Route = post {
    path("notification" / "send") {
      entity(as[NotificationDetail]) { notificationDetail =>
        logDuration(onSuccess(sendNotificationEvent(command, notificationDetail))(complete(_)))
      }
    }
  }

  def receiveNotification(command: ActorRef, query: ActorRef): Route = pathSingleSlash {
    parameters('userId, 'phone_number) { (userId, phoneNumber) =>
      logDuration(handleWebSocketMessages(receiveNotificationFromUser(command, query, userId, phoneNumber)))
    }
  }

  /**
   * This method is used to view a wall's notifications.
   *
   * @param query
   * @return
   */

  def getNotifications(query: ActorRef): Route = get {
    pathPrefix("my" / "allnotifications") {
      pathEnd {
        parameters('userId, 'phone_number) { (userId, phoneNumber) =>
          val result = getNotificationHandler(query, userId, None).map(handleResponseWithEntity(_))
          logDuration(onSuccess(result)(complete(_)))
        }
      }
    } ~
      path("my" / "allnotifications" / Segment) {
        (pos) =>
          {
            parameters('userId, 'phone_number) { (userId, phoneNumber) =>
              val result = getNotificationHandler(query, userId, Some(pos)).map(handleResponseWithEntity(_))
              logDuration(onSuccess(result)(complete(_)))
            }
          }
      }
  }

  def routes(command: ActorRef, query: ActorRef): Route = notificationRoute(command, query) ~ receiveNotification(command, query) ~
    sendNotification(command) ~ getNotifications(query)

}
