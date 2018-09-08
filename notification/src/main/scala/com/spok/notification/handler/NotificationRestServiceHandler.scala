package com.spok.notification.handler

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.model.ws.{ Message, TextMessage }
import akka.stream.{ ActorMaterializer, OverflowStrategy }
import akka.stream.scaladsl.{ Flow, Sink, Source }
import akka.util.Timeout
import com.spok.model.NotificationDetail
import com.spok.notification.service._
import com.spok.util.{ HttpUtil, JsonHelper, ResponseUtil }

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import com.spok.util.Constant._

trait NotificationRestServiceHandler extends JsonHelper with HttpUtil with ResponseUtil {
  implicit val system: ActorSystem
  import system.dispatcher
  implicit val materializer: ActorMaterializer
  implicit val timeout = Timeout(40 seconds)

  import akka.pattern.ask

  /**
   *
   * @param command
   * @return success message if notification creation is successful otherwise return the failure message
   */
  def storeNotificationEvent(command: ActorRef, notificationDetail: NotificationDetail): Future[String] = {
    info("Store Notification::" + notificationDetail)
    val futureResponse = ask(command, Create(notificationDetail)).mapTo[NotificationAck]
    futureResponse.map { response =>
      info("Store Notification:: response after storing notification " + response)
      response match {
        case notificationCreateSuccess: NotificationCreateSuccess => notificationDetail.notificationId
        case notificationCreateFailure: NotificationCreateFailure => "Notification creation failed with error " + notificationCreateFailure.cause.getMessage
      }
    }
  }

  def sendNotificationEvent(command: ActorRef, notificationDetail: NotificationDetail): Future[String] = {
    info("Send Notification::" + notificationDetail)
    val futureResponse = ask(command, SendNotification(notificationDetail)).mapTo[String]
    futureResponse
  }

  def receiveNotificationFromUser(command: ActorRef, query: ActorRef, userId: String, phoneNumber: String): Flow[Message, Message, _] = {
    info(s"Connecting user ${userId} to receive notification!!!!")
    Flow[Message]
      .collect {
        case TextMessage.Strict(txt) => {
          detectRequestAndPerform(command, query, userId, phoneNumber, txt) map {
            case TextMessage.Strict(txt) =>
              command ! SendMessage(txt, userId)
          }
          TextMessage(txt)
        }
      }
      .via(receiveNotification(command, userId)) // ... and route them through the receiveNotification ...
      .map {
        case msg: String ⇒ TextMessage.Strict(msg)
      }
  }

  private def receiveNotification(command: ActorRef, userId: String): Flow[Message, String, Any] = {
    val in = Sink.actorRef[Message](command, DisConnectUser(userId))
    val out =
      Source.actorRef[String](Int.MaxValue, OverflowStrategy.dropBuffer)
        .mapMaterializedValue(command ! ConnectUser(userId, _))

    Flow.fromSinkAndSource(in, out)

  }

  /**
   * This method is for detecting request
   * and perform spok operation accordingily
   */
  def detectRequestAndPerform(command: ActorRef, query: ActorRef, userId: String, phoneNumber: String, txt: String): Future[TextMessage] = {
    logger.info(s"Notification service is connected for ${phoneNumber}. Now ready to perform Notification action!!")
    val action = (parse(txt) \ ACTION).extractOpt[String]
    logger.info(s"$phoneNumber:  is performing <${action}>!")
    action match {
      case Some(REMOVE_NOTIFICATION) => removeNotificationHandler(command, query, txt, phoneNumber, userId)
      case Some(_) => Future(TextMessage(write(sendFormattedError(ACT_101, INVALID_ACTION))))
      case None => Future(TextMessage(write(sendFormattedError(ACT_101, MISSING_ACTION))))
    }
  }

  def removeNotificationHandler(command: ActorRef, query: ActorRef, json: String, phoneNumber: String, userId: String): Future[TextMessage] = {
    val notificationIdOpt = (parse(json) \ (NOTIFICATION_ID)).extractOpt[String]
    notificationIdOpt match {
      case Some(notificationId) => {
        val futureResponse = ask(command, NotificationRemove(notificationId, userId)).mapTo[NotificationAck]
        futureResponse.map { response =>
          info("Remove Notification:: response after removing notification " + response)
          response match {
            case removeNotificationSuccess: RemoveNotificationSuccess =>
              TextMessage(write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(removeNotificationSuccess), Some(REMOVE_NOTIFICATION))))
            case removeNotificationFailure: RemoveNotificationFailure =>
              TextMessage(write(sendFormattedError(removeNotificationFailure.errorCode, removeNotificationFailure.cause.getMessage, Some(REMOVE_NOTIFICATION))))
          }
        }
      }
      case None => Future(TextMessage(write(sendFormattedError(MYA_001, "NotificationId Id not found!!!"))))
    }
  }

  /**
   * This function is used to view a wall's notifications.
   *
   * @param query  notification view actorRef
   * @param userId userId's id
   * @param pos    Pagination position identifier
   * @return comments success if comments are find otherwise the error message
   */
  def getNotificationHandler(query: ActorRef, userId: String, pos: Option[String]): Future[String] = {
    val validPos: String = pos match {
      case Some(pos) => pos
      case None => "1"
    }

    val notificationsResponse = ask(query, ViewWallNotifications(userId, validPos))
    notificationsResponse.map { res =>
      res match {
        case viewWallNotificationsSuccess: ViewWallNotificationsSuccess => write(generateCommonResponseForCaseClass(
          SUCCESS, Some(List()), Some(viewWallNotificationsSuccess.notifications), Some(GET_NOTIFICATIONS)
        ))
        case viewWallNotificationsFailure: ViewWallNotificationsFailure => write(sendFormattedError(
          viewWallNotificationsFailure.errorCode, viewWallNotificationsFailure.cause.getMessage
        ))
      }
    }

  }

}
