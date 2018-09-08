package com.spok.persistence.factory.dsequery

import java.util.Date

import com.datastax.driver.dse.graph.{ SimpleGraphStatement, Vertex }
import com.spok.model.NotificationDetail
import com.spok.util.Constant._

trait DSENotificationQuery extends DSEUserQuery {

  val date: Date = new java.util.Date()

  def get(notificationObj: NotificationDetail): String = {
    s"""graph.addVertex(label,"$DSE_NOTIFICATION","notificationId","${notificationObj.notificationId}",
  | "notificationType" , "${notificationObj.notificationType}" ,"timestamp" , "${date.getTime()}" ,
  | "relatedTo" ,"${notificationObj.relatedTo}"   )""".stripMargin

  }

  def executeSimpleGraphStatement(vertexFirst: Vertex, vertexSecond: Vertex, edgeLabel: String): SimpleGraphStatement = {
    val sGraphStmt = new SimpleGraphStatement(
      "def v1 = g.V(id1).next()\n" + "def v2 = g.V(id2).next()\n" + "v1.addEdge('" + edgeLabel + "', v2 , '" + DATETIME + "','" + date.toString() + "')"
    ).set("id1", vertexFirst.getId()).set("id2", vertexSecond.getId())
    sGraphStmt
  }

  def removeNotificationQuery(notificationId: String, userId: String): String = {

    s"""g.E().hasLabel('$RECEIVE_A').as('e').outV().has('$USER_ID','$userId')
       |.select('e').inV().has('$NOTIFICATION_ID','$notificationId').select('e').drop()""".stripMargin
  }

  def notificationExistQuery(notificationId: String, userId: String): String =
    s"""g.V().hasLabel('$DSE_NOTIFICATION')
       |.has('$NOTIFICATION_ID','$notificationId').inE('$RECEIVE_A').outV().has('$USER_ID','$userId').hasNext()""".stripMargin

  def getNotificationsQuery(userId: String, fromPosNo: Int, toPosNo: Int): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').outE('$RECEIVE_A').order().by('$DATETIME',decr).inV().range($fromPosNo,$toPosNo)"""
  }
  def getEmitterQuery(notificationId: String): String = {
    s"""g.V().hasLabel('$DSE_NOTIFICATION').has('$NOTIFICATION_ID','$notificationId').outE('$EMITTER_BY').inV()"""
  }
}
