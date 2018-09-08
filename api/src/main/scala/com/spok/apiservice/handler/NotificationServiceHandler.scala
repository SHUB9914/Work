package com.spok.apiservice.handler

import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpMethods, HttpRequest }
import akka.http.scaladsl.{ Http, HttpExt }
import com.spok.apiservice.service.ApiStarter._
import com.spok.apiservice.utility.ApiHttpUtil
import com.spok.model.NotificationDetail
import com.spok.util.ConfigUtil._
import com.spok.util.Constant._
import com.spok.util.LoggerUtil

import scala.concurrent.Future

trait NotificationServiceHandler extends ApiServiceHandler with ApiHttpUtil {

  val httpExt: HttpExt = Http()

  def sendUserMentionNotification(mentionUserId: List[String], relatedTo: String, userInfo: (String, String)): Any = {
    val (userId, _) = userInfo
    LoggerUtil.info("Notification: sending notification to mention userId list...")
    if (mentionUserId.nonEmpty) {
      val notificationDetail = NotificationDetail(mentionUserId, getUUID(), MENTION, relatedTo, userId)
      sendNotification(notificationDetail, ADD)
    }
  }

  def sendNotification(notificationDetail: NotificationDetail, routeType: String): Any = {
    LoggerUtil.info("Notification Service Handler: Send notification detail to notification api :" + notificationDetail)
    httpExt.singleRequest(HttpRequest(
      method = HttpMethods.POST,
      uri = "http://" + lookupService(notificationServiceName) + ":" + notificationPort + DELIMITER + NOTIFICATION + DELIMITER + routeType,
      entity = HttpEntity(ContentTypes.`application/json`, write(notificationDetail))
    ))
  }

  def sendNotificationToFollowersAndVisitedUsers(
    notificationDetails: Future[NotificationDetail]
  ): Future[Any] = {
    notificationDetails.map { notificationDetail =>
      sendNotification(notificationDetail, "send")
    }
  }
}
