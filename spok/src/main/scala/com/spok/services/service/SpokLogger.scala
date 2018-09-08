package com.spok.services.service

import com.datastax.driver.core.ResultSet
import com.google.common.reflect.TypeToken
import com.spok.model.SpokModel._
import com.spok.persistence.dse.DseConnectionUri._
import com.spok.persistence.dse.DseGraphFactory
import com.spok.persistence.factory.spokgraph.{ DSESpokApi, DSESpokQuery, DSESpokViewApi }
import com.spok.persistence.factory.spoklog.SpokLogging
import com.spok.util.Constant._
import com.spok.util.{ JsonHelper, LoggerUtil, RandomUtil }

import scala.collection.JavaConverters._
import com.spok.model.Account.{ userDisableDetails, User }

case class SpokId(spokId: String)

case class CommentDetails(commentId: String, commentText: String)

case class removeCommentDetails(commentId: String)

case class CommentAndSpokDetails(spokId: String, commentId: String, commentText: String)

case class removeCommentAndSpokDetails(spokId: String, commentId: String)

case class LaunchSearchSpokDetails(
  userId: String,
  spokId: String,
  spokDetails: String,
  hashtag: String,
  geo_lat: Double,
  geo_long: Double,
  launchedTime: Long,
  contentType: String
)

trait SpokLogger extends JsonHelper with SpokLogging with RandomUtil with LoggerUtil with DSESpokQuery {

  val dseSpokFactoryApi: DSESpokApi = DSESpokApi

  /**
   * Method to Log Spok Creation event in cassandra
   *
   * @param spokDetails spok details
   * @param userId      spokreId
   * @return
   */
  def insertSpokCreationEvent(spokDetails: Spok, userId: String): ResultSet = {
    val spokJson = SpokHistory(spokDetails.spokId, spokDetails.launched, "", spokDetails.geo.latitude, spokDetails.geo.longitude, spokDetails.geo.elevation,
      userId, RESPOKED_EVENT)
    insertHistory(write(spokJson), spok)

    val dataJsonSpoker = write(SpokId(spokDetails.spokId))
    val spokerJson = SpokerHistory(userId, spokDetails.launched, dataJsonSpoker, spokDetails.geo.latitude, spokDetails.geo.longitude, spokDetails.geo.elevation,
      RESPOKED_EVENT)
    logger.info("Storing spok creation event ::" + spokDetails)
    insertHistory(write(spokerJson), spoker)
  }

  /**
   * method to link spoker followers with spok
   *
   * @param userId spokerId
   * @param spok   spok details
   * @return
   */
  def linkFollowers(userId: String, spok: Spok, pendingQuestions: Int): List[ResultSet] = {
    val followerDetails: List[(String, Double, Double, Double, Long)] = dseSpokFactoryApi.linkSpokerFollowers(
      userId,
      spok.groupId, spok.spokId, spok.geo, spok.visibility, spok.headerText, spok.launched, pendingQuestions
    )
    logger.info("Storing linkFollowers event for spok ::", spok)
    logFollowersData(spok.spokId, spok.geo, followerDetails)
  }

  def logFollowersData(spokId: String, geo: Geo, followerDetails: List[(String, Double, Double, Double, Long)]): List[ResultSet] = {
    logger.info("Storing Followers data for spok ::", spokId)
    followerDetails.map { followerDetail =>
      followerDetail match {
        case (followerId, followerGeoLat, followerGeoLon, followerGeoEle, logTime) =>
          insertSpokPendingEvent(spokId, geo, followerId, followerGeoLat, followerGeoLon, followerGeoEle, logTime)
      }
    }
  }

  /**
   * Method to Log Spok Pending event in cassandra
   *
   * @param spokId         spok details
   * @param followerId     follower Id
   * @param followerGeoLat follower curent latitude
   * @param followerGeoLon follower curent longitude
   * @param followerGeoEle follower curent  elevation
   * @return
   */
  def insertSpokPendingEvent(spokId: String, geo: Geo, followerId: String, followerGeoLat: Double, followerGeoLon: Double, followerGeoEle: Double,
    logTime: Long): ResultSet = {
    val spokJson = SpokHistory(spokId, logTime, "", geo.latitude, geo.longitude, geo.elevation,
      followerId, PENDING)
    insertHistory(write(spokJson), spok)

    val dataJsonSpoker = write(SpokId(spokId))
    val spokerJson = SpokerHistory(followerId, logTime, dataJsonSpoker, followerGeoLat, followerGeoLon, followerGeoEle,
      PENDING)
    insertHistory(write(spokerJson), spoker)
  }

  def insertRespokCreationEvent(respok: Respok, userId: String, spokId: String): ResultSet = {
    val spokJson = SpokHistory(spokId, respok.launched, "", respok.geo.latitude, respok.geo.longitude, respok.geo.elevation,
      userId, RESPOKED_EVENT)
    insertHistory(write(spokJson), spok)

    val dataJsonSpoker = write(SpokId(spokId))
    val spokerJson = SpokerHistory(userId, respok.launched, dataJsonSpoker, respok.geo.latitude, respok.geo.longitude, respok.geo.elevation,
      RESPOKED_EVENT)
    logger.info("Storing respok creation event ::", respok)
    insertHistory(write(spokerJson), spoker)
  }

  def insertUnspokCreationEvent(unspok: Unspok, userId: String, spokId: String): ResultSet = {
    val spokJson = SpokHistory(spokId, unspok.launched, "", unspok.geo.latitude, unspok.geo.longitude, unspok.geo.elevation,
      userId, UNSPOKED_EVENT)
    insertHistory(write(spokJson), spok)

    val dataJsonSpoker = write(SpokId(spokId))
    val spokerJson = SpokerHistory(userId, unspok.launched, dataJsonSpoker, unspok.geo.latitude, unspok.geo.longitude, unspok.geo.elevation,
      UNSPOKED_EVENT)
    logger.info(s" $spokId Storing Unspok creation event ::", unspok)
    insertHistory(write(spokerJson), spoker)
  }

  /**
   * This method will store spok event in event store
   *
   * @
   *
   * @param spokId
   * @param launchedTime
   * @return
   */
  def insertSpokEvent(userId: String, spokId: String, launchedTime: Long, eventName: String, geo: Geo = Geo(0.0, 0.0, 0.0)): Boolean = {
    try {
      val spokJson = SpokHistory(spokId, launchedTime, "", geo.latitude, geo.longitude, geo.elevation, userId, eventName)
      insertHistory(write(spokJson), spok)

      val dataJsonSpoker = write(SpokId(spokId))
      val spokerJson = SpokerHistory(userId, launchedTime, dataJsonSpoker, geo.latitude, geo.longitude, geo.elevation, eventName)
      logger.info("Storing spok event ::" + spokId + " : " + eventName)
      insertHistory(write(spokerJson), spoker)
      true
    } catch {
      case ex: Exception => false
    }
  }

  /*
   * Insert log in cassandra for adding comment
   *
   * @param commenterId
   * @param commentId
   * @param spokId
   * @param createdTimestamp
   * @param commentText
   * @param geo
   * @return
   */
  def insertCommentEvent(commenterId: String, commentId: String, spokId: String, createdTimestamp: Long,
    commentText: String, geo: Geo, eventType: String): ResultSet = {
    val spokDataJson = write(CommentDetails(commenterId, commentText.replace("'", "''")))
    val spokJson = SpokHistory(spokId, createdTimestamp, spokDataJson,
      geo.latitude, geo.longitude, geo.elevation, commenterId, eventType)
    insertHistory(write(spokJson), spok)

    val spokerDataJson = write(CommentAndSpokDetails(spokId, commenterId, commentText.replace("'", "''")))
    val spokerJson = SpokerHistory(commenterId, createdTimestamp, spokerDataJson,
      geo.latitude, geo.longitude, geo.elevation, eventType)
    logger.info("Storing spok insert comment event ::", commenterId + ": SpokId " + spokId)
    insertHistory(write(spokerJson), spoker)
  }

  def insertRemoveCommentEvent(commenterId: String, commentId: String, spokId: String, createdTimestamp: Long, geo: Geo): ResultSet = {
    val spokDataJson = write(removeCommentDetails(commentId))
    val spokJson = SpokHistory(spokId, createdTimestamp, spokDataJson, geo.latitude, geo.longitude, geo.elevation, commenterId, COMMENT_REMOVED_EVENT)
    insertHistory(write(spokJson), spok)

    val spokerDataJson = write(removeCommentAndSpokDetails(spokId, commentId))
    val spokerJson = SpokerHistory(commenterId, createdTimestamp, spokerDataJson, geo.latitude, geo.longitude, geo.elevation, COMMENT_REMOVED_EVENT)
    logger.info("Storing spok remove comment event ::", commenterId + ": SpokId " + spokId)
    insertHistory(write(spokerJson), spoker)
  }

  def insertHashTags(text: String): Any = {
    if (!text.isEmpty) {
      val hashtags: List[String] = getHashTagsList(text)
      hashtags.map { hashtag =>
        insertHashTag(hashtag.replace("'", "''"), hashtagTable)
      }
    }
  }

  /**
   * Method to fetch hashtag from spok text string
   * @param text
   * @return
   */
  def getHashTagsList(text: String): List[String] = {
    text.split("#").map(_.split("\\s+")(0)).tail.toList
  }

  /**
   * spok details for launch search api inserted in cassandra
   * @param userId
   * @param spok
   * @return
   */
  def insertLaunchSearchDetailsOfSpok(userId: String, spok: Spok): ResultSet = {
    val hashTags = getHashTagsList(spok.text.getOrElse(""))
    val spokVertex = DseGraphFactory.dseConn.executeGraph(fetchSpokVertex(spok.spokId)).one().asVertex()
    val spokDetails = write(DSESpokViewApi.getSpokDetails(spok.spokId, spokVertex, "", userId))
    val jsonDetails = LaunchSearchSpokDetails(userId, spok.spokId, spokDetails, hashTags.mkString(","),
      spok.geo.latitude, spok.geo.longitude, spok.launched, spok.contentType)
    insertHistory(write(jsonDetails), LaunchSearchTable)
  }

  /**
   * This function will insert and update subscriber details.
   *
   * @param spokId
   * @param userId
   * @return
   */
  def upsertSubscriberDetails(spokId: String, userId: String): Boolean = {
    try {
      upsertSubscriber(spokId, userId, subscriberDetails)
      true
    } catch {
      case ex: Exception => false
    }
  }

  /**
   * This function will remove subscriber details.
   *
   * @param spokId
   * @param userId
   * @return
   */
  def removeSubscriberDetails(spokId: String, userId: String): Boolean = {
    try {
      removeSubscriber(spokId, userId, subscriberDetails)
      true
    } catch {
      case ex: Exception => false
    }
  }

  /**
   * This function will fetch list of subscriber details.
   *
   * @return
   */
  def fetchSubscriberDetails: List[SubscriberDetails] = {
    try {
      val listOfStrings = new TypeToken[String]() {}
      val res = fetchDataFromTable(subscriberDetails).all()
      val listofSubscriber = res.asScala.toList.map { row =>
        val spokId = row.getString("spokid")
        val userIds = row.getSet("userids", listOfStrings).asScala.toList
        SubscriberDetails(spokId, userIds)
      }
      listofSubscriber
    } catch {
      case ex: Exception => Nil
    }
  }

}

object SpokLogger extends SpokLogger
