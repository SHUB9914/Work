package com.spok.persistence.factory

import com.datastax.driver.dse.graph.Vertex
import com.spok.model.Messaging.MessageResponse
import com.spok.model.{ Emitter, NotificationDetail, Notifications, NotificationsResponse }
import com.spok.persistence.dse.DseGraphFactory
import com.spok.persistence.factory.dsequery.DSENotificationQuery
import com.spok.util.Constant._
import com.spok.persistence.dse.DseConnectionUri._
import com.spok.util.JsonHelper

import scala.collection.JavaConverters._

case class NotificationInternalResponse(id: String, `type`: String, relatedTo: String, emitter: Emitter)

case class NotificationMessgingResponse(id: String, `type`: MessageResponse, relatedTo: String, emitter: Emitter)

case class NotificationResponse(userId: String, notification: NotificationInternalResponse)

case class NotificationResponseForMessaging(userId: String, notification: NotificationMessgingResponse)

trait DSEUserNotificationFactoryApi extends DSENotificationQuery with JsonHelper {

  def storeUsersNotifications(notificationObj: NotificationDetail): Option[Vertex] = {

    val notificationV: Vertex = DseGraphFactory.dseConn.executeGraph(get(notificationObj)).one().asVertex()

    notificationObj.userIds map { user_Id =>
      val userV = DseGraphFactory.dseConn.executeGraph(getUser(user_Id)).one().asVertex()
      DseGraphFactory.dseConn.executeGraph(executeSimpleGraphStatement(userV, notificationV, RECEIVE_A))
    }
    val emitterV = DseGraphFactory.dseConn.executeGraph(getUser(notificationObj.emitterId)).one().asVertex()
    DseGraphFactory.dseConn.executeGraph(executeSimpleGraphStatement(notificationV, emitterV, EMITTER_BY))
    if (notificationV.getProperty("notificationId").getValue.asString().equals(notificationObj.notificationId)) { Some(notificationV) }
    else { None }
  }

  def getnotificationRespone(userId: String, notificationDetail: NotificationDetail): String = {
    val userVertexQuery = DseGraphFactory.dseConn.executeGraph(getUser(notificationDetail.emitterId)).one().asVertex()
    val gender = userVertexQuery.getProperty("gender").getValue.asString()
    val nickName = userVertexQuery.getProperty("nickname").getValue.asString()
    val picture = userVertexQuery.getProperty("picture").getValue.asString()
    val notificationType = notificationDetail.notificationType
    val messageResponse = try {
      parse(notificationType).extractOpt[MessageResponse]
    } catch {
      case ex: Exception => None
    }
    val response = messageResponse match {
      case Some(data) => write(NotificationResponseForMessaging(userId, NotificationMessgingResponse(notificationDetail.notificationId, data,
        notificationDetail.relatedTo, Emitter(notificationDetail.emitterId, nickName, gender, picture))))

      case None => write(NotificationResponse(userId, NotificationInternalResponse(notificationDetail.notificationId, notificationDetail.notificationType,
        notificationDetail.relatedTo, Emitter(notificationDetail.emitterId, nickName, gender, picture))))
    }
    response
  }

  /**
   * Method to remove the edge between the notification and the user
   *
   * @param notificationId
   * @param userId
   * @return
   */
  def removeNotification(notificationId: String, userId: String): (Boolean, String) = {

    try {
      val notificationExist = DseGraphFactory.dseConn.executeGraph(notificationExistQuery(notificationId, userId)).one().asBoolean()
      if (notificationExist) {
        DseGraphFactory.dseConn.executeGraph(removeNotificationQuery(notificationId, userId))
        (true, "Removed")
      } else {
        (false, s"Notification $notificationId not found")
      }
    } catch {
      case ex: Exception => (false, s"Unable removing notification $notificationId (generic error).")
    }
  }

  /**
   * This function will get 10 Notifications of a Spoker.
   *
   * @param userId userId's id
   * @param pos    Pagination position identifier
   * @return List of Notifications if users are find else Nil
   */
  def getnotifications(userId: String, pos: String): Option[NotificationsResponse] = {
    try {
      val reSpokerPerPage: Int = pageSize
      val from = (pos.toInt - 1) * reSpokerPerPage
      val to = from + reSpokerPerPage
      val notificationsResponse: List[Notifications] = notificationsDetails(userId, from, to)
      val (previous, next): (String, String) = if (DseGraphFactory.dseConn.executeGraph(getNotificationsQuery(userId, to, to + 2)).asScala.toList.isEmpty) {
        ((pos.toInt - 1).toString, "")
      } else {
        ((pos.toInt - 1).toString, (pos.toInt + 1).toString)
      }
      Some(NotificationsResponse(previous, next, notificationsResponse))
    } catch {
      case ex: Exception => None
    }
  }

  private def notificationsDetails(userId: String, from: Int, to: Int): List[Notifications] = {
    val notificationsVertex = DseGraphFactory.dseConn.executeGraph(getNotificationsQuery(userId, from, to)).asScala.toList
    notificationsVertex map { notificationsVertex =>
      val notificationsV = notificationsVertex.asVertex()
      val notificationId = notificationsV.getProperty("notificationId").getValue.asString()
      val notificationType = notificationsV.getProperty("notificationType").getValue.asString()
      val relatedTo = notificationsV.getProperty("relatedTo").getValue.asString()
      val timestamp = notificationsV.getProperty("timestamp").getValue.asString()
      val emitterV = DseGraphFactory.dseConn.executeGraph(getEmitterQuery(notificationId)).one().asVertex()
      notificationsResponse(notificationId, notificationType, relatedTo, timestamp, emitterV)
    }
  }

  private def notificationsResponse(notificationId: String, notificationType: String, relatedTo: String, timestamp: String, commenterUserV: Vertex) = {

    Notifications(
      notificationId,
      notificationType,
      relatedTo,
      logTimeStamp,
      Emitter(
        commenterUserV.getProperty("userId").getValue.asString(),
        commenterUserV.getProperty("nickname").getValue.asString(),
        commenterUserV.getProperty("gender").getValue.asString(),
        commenterUserV.getProperty("picture").getValue.asString()
      )
    )
  }
}

object DSEUserNotificationFactoryApi extends DSEUserNotificationFactoryApi
