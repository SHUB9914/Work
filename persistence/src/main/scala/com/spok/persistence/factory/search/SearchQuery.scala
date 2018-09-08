package com.spok.persistence.factory.search

import com.spok.persistence.dse.DseConnectionUri._
import com.spok.util.Constant._
import org.apache.solr.client.solrj.SolrQuery

trait SearchQuery {

  /**
   * This query will return most relevant 10 nicknames.
   *
   * @param nickname
   * @param fromPosNo
   * @param toPosNo
   * @return
   */
  def getByNickname(nickname: String, fromPosNo: Int, toPosNo: Int): String = {
    s"""g.V().has('$USER','$NICKNAME', Search.tokenPrefix('$nickname')).valueMap('$NICKNAME','$USER_ID').range($fromPosNo, $toPosNo)"""
  }

  /**
   * This query will return most relevant 10 hashtags.
   *
   * @param hashtag
   * @return
   */
  def fetchByHashtag(hashtag: String): String = {
    s"""SELECT $HASHTAG FROM $hashtagTable WHERE solr_query='{"q":"hashtag: $hashtag*"}' limit $searchLimit;"""
  }

  def getPopularSpokerQuery(startTime: Long, endTime: Long): String = {
    s"""g.V().hasLabel('$USER').has('$LAUNCHED',between('$startTime','$endTime')).order().by(inE("$FOLLOWS").count(), decr).limit(500)"""
  }

  /**
   * This query will get last spoks.
   *
   * @param limit
   * @return
   */
  def getSpoksQuery(limit: Int): String = {
    s"""SELECT $DATA FROM $lastSpok WHERE solr_query='{"q":"*:*", "sort":"loggedtime desc"}' limit $limit;""".stripMargin
  }

  /**
   * This query will get last spoks of my friends.
   *
   * @param userId
   * @param fromPosNo
   * @param toPosNo
   * @return
   */
  def getFriendSpoksQuery(userId: String, fromPosNo: Int, toPosNo: Int): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID', '$userId').outE('$FOLLOWS').inV().as('spoks').outE('$FOLLOWS').inV().has('$USER_ID', '$userId').select('spoks')
        | .outE('$ISASSOCIATEDWITH').has('$STATUS', '$RESPOKED').inV().has('$ORIGINAL_VISIBILITY', '$PUBLIC').order().by('$LAUNCHED',decr)
        | .range($fromPosNo,$toPosNo)""".stripMargin
  }

  def getTrendySpokQuery(limit: Int): String = {
    s"""SELECT $DATA FROM $trendySpok WHERE solr_query='{"q":"*:*", "sort":"loggedtime desc"}' limit $limit;""".stripMargin
  }

  def getPopularSpokerQuery(limit: Int): String = {
    s"""SELECT $DATA FROM $popularSpoker WHERE solr_query='{"q":"*:*", "sort":"loggedtime desc"}' limit $limit;""".stripMargin
  }

  def getSpokQueryForBatch(startTime: Long, endTime: Long): String = {
    s"""g.V().hasLabel('$SPOK').has('$ORIGINAL_VISIBILITY', '$PUBLIC').has('$LAUNCHED',between('$startTime','$endTime'))""".stripMargin
  }

  def fetchLaunchSearch(userIds: List[String], hashtags: List[String], latitude: String, longitude: String,
    startTime: String, endTime: String, contentTypes: List[String], start: Int, rows: Int): SolrQuery = {
    val queryBuilder = new StringBuilder
    val partialQuery = s"""spokdetails:('' TO *) """
    if (userIds.nonEmpty) {
      queryBuilder.append(s"AND $SOLR_USERID:(${userIds.mkString(" OR ")}) ")
    }
    if (hashtags.nonEmpty) queryBuilder.append(s"AND $HASHTAG:(${hashtags.mkString(" OR ")}) ")
    if (!latitude.isEmpty) {
      val fiftyKmRadialLatitudeUpperBoundX = (BigDecimal(latitude) + 0.4500000).setScale(7, BigDecimal.RoundingMode.HALF_UP)
      val fiftyKmRadialLatitudeLowerBoundX = (BigDecimal(latitude) - 0.4500000).setScale(7, BigDecimal.RoundingMode.HALF_UP)
      queryBuilder.append(s"AND $GEO_LATITUDE:[$fiftyKmRadialLatitudeLowerBoundX TO $fiftyKmRadialLatitudeUpperBoundX] ")
    }
    if (!longitude.isEmpty) {
      val fiftyKmRadialLongitudeUpperBoundX = (BigDecimal(longitude) + 0.4500000).setScale(7, BigDecimal.RoundingMode.HALF_UP)
      val fiftyKmRadialLongitudeLowerBoundX = (BigDecimal(longitude) - 0.4500000).setScale(7, BigDecimal.RoundingMode.HALF_UP)
      queryBuilder.append(s"AND $GEO_LONGITUDE:[$fiftyKmRadialLongitudeLowerBoundX TO $fiftyKmRadialLongitudeUpperBoundX] ")
    }
    if (!startTime.isEmpty && (!endTime.isEmpty)) queryBuilder.append(s"AND $LAUNCHEDTIME:[$startTime TO $endTime] ")
    if (contentTypes.nonEmpty) {
      queryBuilder.append(s"AND $SOLR_CONTENT_TYPE:(${contentTypes.mkString(" OR ")})")
    }
    createSolrQuery(partialQuery + queryBuilder.toString(), start, rows)
  }

  def createSolrQuery(partialQuery: String, start: Int, rows: Int): SolrQuery = {
    val solrQuery = new SolrQuery
    solrQuery.set("q", partialQuery)
    solrQuery.set("sort", "score desc , launchedtime desc")
    solrQuery.set("df", s"$HASHTAG")
    solrQuery.set("start", s"$start")
    solrQuery.set("rows", s"$rows")
    solrQuery.set("fl", s"$SPOK_DETAILS")
    solrQuery
  }

}
