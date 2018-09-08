package com.spok.persistence.factory.spokgraph

import com.spok.model.SpokModel.Geo
import com.spok.util.Constant._

trait DSESpokCommentQuery {

  def addCommentQuery(commentId: String, commenterUserId: String, commentText: String, commenterGeo: Geo, commentTimestamp: Long): String = {
    s"""graph.addVertex(label,"$DSE_COMMENT","$COMMENT_ID",
        |"$commentId" ,"commenterUserId","$commenterUserId","commentText","${commentText.replace("$", "\\$").replace(QUOTES, QUOTES_WITH_ESCAPE)}" ,
        |"latitude", "${commenterGeo.latitude}" ,"longitude", "${commenterGeo.longitude}"
        |,"elevation", "${commenterGeo.elevation}","timestamp", "$commentTimestamp")""".stripMargin
  }

  def updateSpokCommentStats(spokId: String, numberOfComment: Int): String = {
    s"""g.V().hasLabel('$DSE_SPOK').has('$SPOK_ID','$spokId').outE('$HAS_STATS').inV()
        |.property('nb_comments','${numberOfComment}')""".stripMargin
  }

  def getSpokStatsVertex(spokId: String): String = {
    s"""g.V().hasLabel('$DSE_SPOK').has('$SPOK_ID','$spokId').outE('$HAS_STATS').inV()"""
  }

  def getCommenterDetailsForCommentResponse(userId: String): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').values('$GENDER','$NICKNAME','$PICTURE')"""
  }

  def updateUserComment(commenterId: String, commentId: String, commentText: String, geo: Geo, timeStamp: Long): String =
    s"""g.V().hasLabel("$DSE_COMMENT").has("$COMMENT_ID","$commentId").has("commenterUserId","$commenterId")
        |.property("commentText","${commentText.replace("$", "\\$").replace(QUOTES, QUOTES_WITH_ESCAPE)}")
        |.property("$ELEVATION","${geo.elevation}").property("$LONGITUDE","${geo.longitude}")
        |.property("$LATITUDE","${geo.latitude}").property("timestamp","$timeStamp")""".stripMargin

  def getSpokIdByCommentId(commentId: String): String = {
    s"""g.V().hasLabel('$DSE_COMMENT').has('$COMMENT_ID','$commentId').inE('$HAS_A_COMMENT').outV().hasLabel('$DSE_SPOK').values('$SPOK_ID')"""
  }

  def removeUserComment(commentId: String): String = s"""g.V().hasLabel('$DSE_COMMENT').has('$COMMENT_ID','$commentId').drop()"""

  def getUserIdBySpokId(spokId: String): String = {
    s"""g.V().hasLabel('$DSE_SPOK').has('$SPOK_ID','$spokId').values('$AUTHOR')"""
  }

  def validateCommentByCommenterId(commentId: String, commenterId: String): String =
    s"""g.V().hasLabel('$DSE_COMMENT').has('$COMMENT_ID','$commentId').has('commenterUserId','$commenterId')"""

}
