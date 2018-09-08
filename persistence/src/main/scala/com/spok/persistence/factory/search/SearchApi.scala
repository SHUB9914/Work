package com.spok.persistence.factory.search

import com.spok.model.Account.MyDetails
import com.spok.model.SpokModel.{ Nickname, _ }
import com.spok.persistence.cassandra.CassandraProvider._
import com.spok.persistence.dse.DseConnectionUri._
import com.spok.persistence.dse.DseGraphFactory
import com.spok.persistence.factory.spokgraph.{ DSESpokQuery, DSESpokViewApi }
import com.spok.util.Constant._
import com.spok.util.{ JsonHelper, LoggerUtil }
import org.apache.solr.common.SolrDocument
import org.joda.time._

import scala.collection.JavaConverters._
import scala.collection.parallel.immutable.ParSeq

trait SearchApi extends SearchQuery with LoggerUtil with DSESpokViewApi with DSESpokQuery with JsonHelper {

  /**
   * This function will return most relevant 10 nicknames.
   *
   * @param nickname
   */
  def getByNickname(nickname: String): (List[Nickname], Boolean) = {
    try {
      val nicknameRes = DseGraphFactory.dseConn.executeGraph(getByNickname(nickname, 0, searchLimit)).asScala.toList.map(_.toString)
      val userDetails = (((nicknameRes.map(str => parse(str.replaceAll("\\[", "").replaceAll("\\]", "")).extractOpt[NicknameResponse]))))
      (userDetails.map(userNicknameId => Nickname(userNicknameId.get.nickname, userNicknameId.get.userId)), true)
    } catch {
      case ex: Exception =>
        info(s"Exception while getting list of most relevant 10 nicknames for string: $nickname")
        (Nil, false)
    }
  }

  /**
   * This function will return most relevant 10 hashtags.
   *
   * @param hashtag
   * @return
   */
  def getByHashtag(hashtag: String): (List[String], Boolean) = {
    try {
      val rows = cassandraConn.execute(fetchByHashtag(hashtag)).all().asScala.toList
      val hashtags = rows.map { row =>
        row.getString(HASHTAG)
      }
      (hashtags, true)
    } catch {
      case ex: Exception =>
        info(s"Exception while getting list of most relevant 10 hashtags for string: $hashtag")
        (Nil, false)
    }
  }

  /**
   * This function will return 10 most popular spoker in batch.
   *
   * @param pos pagination no
   * @return
   */
  def getPopularSpokers(pos: String): Option[PopularSpokerRes] = {
    info("fetching popular spokers for pos : " + pos)
    try {
      val validPos = if ((pos.toInt - 1) < 1) 1 else pos.toInt
      val spokPerPage: Int = searchLimit
      val limit = (validPos * searchLimit) + 1
      val dropSize = (validPos - 1) * searchLimit
      val rows = cassandraConn.execute(getPopularSpokerQuery(limit)).all().asScala.toList.drop(dropSize)
      val (previous, next) = getPaginationNumber(validPos, rows.size)
      val spokerList = if (rows.size > searchLimit) { rows.dropRight(1) } else { rows }
      val popularSpokers = spokerList.map { row =>
        parse(row.getString(DATA)).extract[MyDetails]
      }
      val spoksResponse = Some(PopularSpokerRes(previous, next, popularSpokers))
      info("successfuly returned popular spoker response ")
      spoksResponse

    } catch {
      case ex: Exception =>
        info(s"Exception while getting list of most popular 10 spokers for page no : $pos")
        None
    }
  }

  private def getPaginationNumber(pos: Int, listSize: Int): (String, String) = {
    val limit = searchLimit
    if (listSize > limit) {
      (pos.toString, (pos + 1).toString)
    } else if (pos - 1 < 1) {
      ((pos).toString, "")
    } else {
      ((pos - 1).toString, "")
    }
  }

  /**
   * This function will get last spoks.
   *
   * @param userId
   * @param pos
   * @return
   */
  def getLastSpoks(userId: String, pos: String): Option[SpoksResponse] = {
    info("fetching last spoks for pos : " + pos)
    try {
      val validPos = if ((pos.toInt - 1) < 1) 1 else pos.toInt
      val limit = (validPos * searchLimit) + 1
      val dropSize = (validPos - 1) * searchLimit
      val rows = cassandraConn.execute(getSpoksQuery(limit)).all().asScala.toList.drop(dropSize)
      val (previous, next) = getPaginationNumber(validPos, rows.size)
      val spokList = if (rows.size > searchLimit) { rows.dropRight(1) } else { rows }
      val lastSpoks = spokList.map { row =>
        val lastSpok = parse(row.getString(DATA)).extract[LastSpoks]
        val subscriber: Boolean = isSubscriberExist(lastSpok.id, userId)
        val respoker = isCreatorOrRespoked(lastSpok.id, userId)
        lastSpok.copy(subscriptionStatus = subscriber, flag = respoker)
      }
      val spoksResponse = Some(SpoksResponse(previous, next, lastSpoks))
      info("Successfuly returned last spoks response")
      spoksResponse
    } catch {
      case ex: Exception =>
        info(s"Exception while getting $searchLimit Last Spok :: " + ex.getMessage)
        None
    }

  }

  /**
   * This function will get last spoks of my friends.
   *
   * @param userId
   * @param pos
   * @return
   */
  def getFriendSpoks(userId: String, pos: String): Option[SpoksResponse] = {
    try {
      val validPos = if ((pos.toInt - 1) < 1) 1 else pos.toInt
      val startTime = new DateTime()
      info("Get friend spok in search api :::: " + startTime)
      val spokPerPage: Int = searchLimit
      val from = (validPos - 1) * spokPerPage
      val to = from + spokPerPage + 1
      val spokV = DseGraphFactory.dseConn.executeGraph(getFriendSpoksQuery(userId, from, to)).asScala.toList
      val spokList = if (spokV.size > searchLimit) {
        spokV.dropRight(1)
      } else {
        spokV
      }
      val friendSpokRes: ParSeq[LastSpoks] = spokList.par map { spokVertex =>
        val spokId = spokVertex.asVertex().getProperty(SPOK_ID).getValue.asString()
        val spok = getSpokDetails(spokId, spokVertex.asVertex(), "", "")
        val subscriber: Boolean = isSubscriberExist(spokId, userId)
        val respoker = isCreatorOrRespoked(spokId, userId)
        LastSpoks(spok.id, spok.spokType, spok.ttl, spok.launched, spok.text, spok.author, spok.visibility,
          spok.counters, spok.content, subscriber, respoker)
      }
      val (previous, next): (String, String) = getPaginationNumber(validPos, spokV.size)
      val result = Some(SpoksResponse(previous, next, friendSpokRes.toList))
      info("Total time to get friend spok in search api :::: " + (startTime.getMillis - new DateTime().getMillis))
      result
    } catch {
      case ex: Exception =>
        info(s"Exception while getting last $searchLimit spoks of friend for user id: $userId")
        None
    }
  }

  /**
   * This function will return 10 most trendy spok in batch.
   *
   * @param pos pagination no
   */
  def getTrendySpok(pos: String, userId: String): Option[SpoksResponse] = {
    info("fetching trendy spoks for pos : " + pos)
    try {
      val validPos = if ((pos.toInt - 1) < 1) 1 else pos.toInt
      val limit = (validPos * searchLimit) + 1
      val dropSize = (validPos - 1) * searchLimit
      val rows = cassandraConn.execute(getTrendySpokQuery(limit)).all().asScala.toList.drop(dropSize)
      val (previous, next) = getPaginationNumber(validPos, rows.size)
      val spokList = if (rows.size > searchLimit) {
        rows.dropRight(1)
      } else {
        rows
      }
      val trendySpoks = spokList.map { row =>
        val lastSpok = parse(row.getString(DATA)).extract[LastSpoks]
        val subscriber: Boolean = isSubscriberExist(lastSpok.id, userId)
        val respoker = isCreatorOrRespoked(lastSpok.id, userId)
        lastSpok.copy(subscriptionStatus = subscriber, flag = respoker)
      }
      val spoksResponse = Some(SpoksResponse(previous, next, trendySpoks))
      info("Successfuly returned trendy spoks response")
      spoksResponse
    } catch {
      case ex: Exception =>
        info(s"Exception while getting $searchLimit TrendySpok :: " + ex.getMessage)
        None
    }
  }

  /**
   * This function will return 10 launch search spoks
   *
   * @param pos pagination no
   */
  def getlaunchSearch(pos: String, userIds: List[String], hashtags: List[String], latitude: String, longitude: String,
    startTime: String, endTime: String, contentTypes: List[String], userId: String): Option[SpoksResponse] = {
    info("fetching launchSearch pos : " + pos)
    try {
      val validPos = if ((pos.toInt - 1) < 1) 1 else pos.toInt
      val searchHastags = hashtags.filter(!_.equals("")).map { hashtag => "*" + hashtag + "*" }
      val spokPerPage: Int = searchLimit
      val start = if (validPos == 1) { validPos - 1 } else { validPos * searchLimit - searchLimit }
      val limit = searchLimit + 1
      val solrQuery = fetchLaunchSearch(userIds.filter(!_.equals("")), searchHastags,
        latitude, longitude, startTime, endTime, contentTypes.filter(!_.equals("")), start, limit)
      info("executing Solr Query :" + solrQuery)
      val solrConnection = DseGraphFactory.dseSolrConn(solrKeyspace, solrTable)
      val docResponse: List[SolrDocument] = solrConnection.query(solrQuery).getResults.asScala.toList
      val (previous, next) = getPaginationNumber(validPos, docResponse.size)

      val spokerList = if (docResponse.size > searchLimit) { docResponse.dropRight(1) } else { docResponse }
      val spokDetails: List[String] = spokerList.map { s =>
        s.getFieldValues(s"$SPOK_DETAILS").toArray().mkString(",")
      }
      val spoks: Option[List[LastSpoks]] = parse("[" + spokDetails.mkString + "]").extractOpt[List[LastSpoks]]
      val finalSpok: Option[List[LastSpoks]] = spoks match {
        case None => None
        case Some(spok) => Some {
          spok map {
            oneSpok =>
              val subscriber: Boolean = isSubscriberExist(oneSpok.id, userId)
              val respoker = isCreatorOrRespoked(oneSpok.id, userId)
              oneSpok.copy(subscriptionStatus = subscriber, flag = respoker)
          }
        }
      }
      Some(SpoksResponse(previous.toString, next.toString, finalSpok.get))

    } catch {
      case ex: Exception =>
        info(s"Exception while getting $searchLimit launchSearch" + ex.getMessage)
        None
    }
  }

}

object SearchApi extends SearchApi
