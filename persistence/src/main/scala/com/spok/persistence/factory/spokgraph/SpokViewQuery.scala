package com.spok.persistence.factory.spokgraph

import com.spok.util.Constant._

trait SpokViewQuery {

  /**
   * This mehtod will return stats for spok.
   *
   * @param spokId
   * @return
   */
  def getSpokStatsQuery(spokId: String): String = {
    s"""g.V().hasLabel('$DSE_SPOK').has('$SPOK_ID','$spokId').outE('$HAS_STATS').inV()"""
  }

  /**
   * This method is used to get the 10 comments of a spok
   *
   * @param spokId
   * @param fromPosNo
   * @param toPosNo
   * @return
   */
  def getCommentsQuery(spokId: String, fromPosNo: Int, toPosNo: Int): String = {
    s"""g.V().hasLabel('$DSE_SPOK').has('$SPOK_ID','$spokId').outE('$HAS_A_COMMENT').order()
        |.by('$LAUNCHED',decr).inV().range($fromPosNo,$toPosNo)""".stripMargin
  }

  /**
   * This method will get the total comments of a spok
   *
   * @param spokId
   * @return
   */
  def getTotalCommentsQuery(spokId: String): String = {
    s"""g.V().hasLabel('$DSE_SPOK').has('$SPOK_ID','$spokId').outE('$HAS_A_COMMENT').count()"""
  }

  /**
   * This method will get the time stamp of a comment over a spok
   *
   * @param spokId
   * @param commentId
   * @return
   */
  def getCommentsTimeStamp(spokId: String, commentId: String): String = {
    s"""g.V().has('$COMMENT','$COMMENT_ID','$commentId').values('$TIMESTAMP')"""
  }

  /**
   * This query is used to fetch 10 re-spokers of a spok.
   * @param spokId spok's id
   * @param fromPosNo from range
   * @param toPosNo to limit
   * @return
   */
  def getReSpokersQuery(spokId: String, fromPosNo: Int, toPosNo: Int): String = {
    s"""g.V().hasLabel('$DSE_SPOK').has('$SPOK_ID','$spokId').has('$ENABLED','$TRUE').inE('$ISASSOCIATEDWITH').has('$STATUS','$RESPOKED')
       |.order().by('$LAUNCHED',decr).outV().range($fromPosNo,$toPosNo)""".stripMargin
  }

  /**
   * This query is used to fetch 10 scoped user of a spok.
   *
   * @param spokId spok's id
   * @param fromPosNo from range
   * @param toPosNo to limit
   * @return
   */
  def getScopedUsersQuery(spokId: String, fromPosNo: Int, toPosNo: Int): String = {
    s"""g.V().hasLabel('$DSE_SPOK').has('$SPOK_ID','$spokId').has('$ENABLED','$TRUE').inE('$ISASSOCIATEDWITH').has('$STATUS','$PENDING')
           |.order().by('$LAUNCHED',decr).outV().hasLabel('$USER').range($fromPosNo,$toPosNo)""".stripMargin
  }

  /**
   * This mehtod will return list of spok.
   *
   * @param userId
   * @param fromPosNo
   * @param toPosNo
   * @return
   */
  def getSpokStackQuery(userId: String, fromPosNo: Int, toPosNo: Int): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').outE('$ISASSOCIATEDWITH').has('$STATUS','$PENDING')
       |.order().by('$LAUNCHED',decr).inV().has('$ENABLED','$TRUE').range($fromPosNo,$toPosNo)""".stripMargin
  }

  /**
   * This method will return
   *
   * @param userId
   * @param fromPosNo
   * @param toPosNo
   * @return
   */
  def getUsersWallQuery(userId: String, fromPosNo: Int, toPosNo: Int): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').outE('$ISASSOCIATEDWITH').has('$STATUS','$RESPOKED')
        |.has('$VISIBILITY','$PUBLIC').order().by('$LAUNCHED',decr).inV().has('$ENABLED','$TRUE')
        |.range($fromPosNo,$toPosNo)""".stripMargin
  }

  /**
   * This mehtod will return list of spok.
   *
   * @param userId
   * @param fromPosNo
   * @param toPosNo
   * @return
   */
  def getMySpokQuery(userId: String, fromPosNo: Int, toPosNo: Int): String = {
    s"""g.V().hasLabel('$DSE_SPOK').has('$ENABLED','$TRUE').has('$AUTHOR','$userId').order().by('$LAUNCHED',decr).range($fromPosNo,$toPosNo)""".stripMargin
  }

}
