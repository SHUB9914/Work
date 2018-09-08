package com.spok.persistence.factory.spokLog

import java.util.Date

import com.google.common.reflect.TypeToken
import com.spok.model.SpokModel
import com.spok.model.SpokModel.{ LastSpoks, SpokHistory, SpokerHistory, ViewSpok }
import com.spok.persistence.cassandra.CassandraMessageProvider._
import com.spok.persistence.cassandra.CassandraProvider._
import com.spok.persistence.dse.DseConnectionUri._
import com.spok.persistence.factory.spoklog.SpokLogging
import com.spok.util.RandomUtil
import org.scalatest.{ FlatSpec, Matchers }

import scala.collection.JavaConverters._

case class SpokerDetails(spokerId: String, nickname: String, gender: String, picture: String)

case class user(
  userid: String,
  spokdetails: String,
  geo_lat: Double,
  geo_long: Double,
  hashtag: String,
  launchedtime: Long
)

case class LaunchSearchSpokDetails(
  userId: String,
  spokId: String,
  spokDetails: String,
  hashtag: String,
  geo_lat: Double,
  geo_long: Double,
  LaunchedTime: Long,
  contentType: String
)

class SpokLoggingSpec extends FlatSpec with Matchers with SpokLogging with RandomUtil {

  val listOfStrings = new TypeToken[String]() {}

  behavior of "SpokLoggingSpec "

  it should "be able to insert spokhistory details in cassandra" in {

    val spokId = getUUID().toString
    val dataJson = """{\"commentid\":\"12334dsfdsfdsf\", \"commenttext\":\"comment\"}"""
    val currentTimeInMilis = new Date().getTime

    val json = write(SpokHistory(spokId, currentTimeInMilis, dataJson, 12.12, 12.12, 12.12, "12", "test event"))

    insertHistory(json, spok)
    val res = cassandraConn.execute(s"SELECT * from $spok where spokid = '$spokId' ")
    val row = res.one()
    assert(row.getString("spokid") == spokId)

  }

  it should "be able to insert spokhistoryByBinding details in cassandra" in {

    val spokId = getUUID().toString
    val dataJson = """{\"commentid\":\"12334dsfdsfdsf\", \"commenttext\":\"comment\"}"""
    val currentTimeInMilis = new Date().getTime

    val json = write(SpokHistory(spokId, currentTimeInMilis, dataJson, 12.12, 12.12, 12.12, "12", "test event's"))

    insertHistoryByBinding(json, spok)
    val res = cassandraConn.execute(s"SELECT * from $spok where spokid = '$spokId' ")
    val row = res.one()
    assert(row.getString("spokid") == spokId)

  }

  it should "be able to insert spokerhistory details in cassandra" in {

    val spokerId = getUUID().toString
    val dataJson = """{\"commentid\":\"12334dsfdsfdsf\", \"commenttext\":\"comment\"}"""
    val currentTimeInMilis = new Date().getTime

    val json = write(SpokerHistory(spokerId, currentTimeInMilis, dataJson, 12.12, 12.12, 12.12, "test event"))

    insertHistory(json, spoker)
    val res = cassandraConn.execute(s"SELECT * from $spoker where spokerid = '$spokerId' ")
    val row = res.one()
    assert(row.getString("spokerid") == spokerId)

  }

  it should "be able to insert hashtags in cassandra" in {

    insertHashTag("spokhashtag", hashtagTable)
    val spokerRes = cassandraConn.execute(s"SELECT * from $hashtagTable where hashtag = 'spokhashtag' ")
    assert(spokerRes.one().getString("hashtag") == "spokhashtag")
  }

  it should "be able to insert launchsearch details in cassandra" in {

    val userId = getUUID().toString
    val spokId = getUUID().toString
    val hashTags = List("fantastic")
    val currentTimeInMilis = new Date()
    val spokDetails = write(ViewSpok(spokId, "rawText", 0, currentTimeInMilis, "#fantastic spok", Some(currentTimeInMilis),
      "curHeading", true, SpokModel.Spoker(userId, "abc", "male", "nopicture"), SpokModel.Spoker(userId, "abc", "male", "nopicture"), "visibility", SpokModel.Counters(1, 2, 3, 22.22), SpokModel.Content()))

    val json = write(LaunchSearchSpokDetails(userId, spokId, spokDetails, hashTags.mkString(","),
      22.22, 33.33, 123456789, "rawtext"))
    insertHistory(json, LaunchSearchTable)
    val spokRes = cassandraConn.execute(s"SELECT spokdetails from $LaunchSearchTable where spokid = '$spokId' and userid='$userId' ")
    val spokRow: String = spokRes.one().getString("spokdetails")
    val lastSpoks = parse("[" + spokRow.mkString + "]").extractOpt[List[LastSpoks]].get
    assert(lastSpoks(0).id == spokId)
    assert(lastSpoks(0).author.id == userId)
  }

  it should "be able to insert messagingDetails in cassandra" in {
    val userId = getUUID().toString
    val spokerDetail = write(SpokerDetails(userId, "nickname", "gender", "picture"))
    insertMesssagingDetails(spokerDetail, spokerDetails)
    val spokerRes = cassandraMessageConn.execute(s"SELECT * from $spokerDetails where spokerId = '$userId' ")
    assert(spokerRes.one().getString("spokerId") == userId)
  }

  it should "be able to insert or update subscriber details" in {
    val spokId = getUUID().toString
    val userId = getUUID().toString
    upsertSubscriber(spokId, userId, subscriberDetails)
    val subscriberDetailsRes = cassandraMessageConn.execute(s"SELECT * from $subscriberDetails where spokId = '$spokId' ").one()
    assert(subscriberDetailsRes.getSet("userids", listOfStrings).asScala.toList == List(userId))
  }

  it should "be able to remove subscriber details" in {
    val spokId = getUUID().toString
    val userId = getUUID().toString
    upsertSubscriber(spokId, userId, subscriberDetails)
    val result = removeSubscriber(spokId, userId, subscriberDetails)
    assert(result.isExhausted)
  }

  it should "be able to fetch subscriber details" in {
    val spokId = getUUID().toString
    val userId = getUUID().toString
    upsertSubscriber(spokId, userId, subscriberDetails)
    val result = fetchDataFromTable(subscriberDetails)
    val subscriberDetailsRes = result.one()
    assert(subscriberDetailsRes.getSet("userids", listOfStrings).asScala.toList.nonEmpty)
  }

  it should "be able to get subscriber exists or not" in {
    val spokId = getUUID().toString
    val userId = getUUID().toString
    upsertSubscriber(spokId, userId, subscriberDetails)
    val result = isSubscriber(spokId, subscriberDetails)
    assert(result.one().getSet("userids", listOfStrings).asScala.toList.nonEmpty)
  }

}

