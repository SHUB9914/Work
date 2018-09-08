package com.spok.services.service

import java.util.Date

import com.datastax.driver.core.Row
import com.spok.model.Account.User
import com.spok.model.{ InnerLocation, NorthEast, SouthWest, ViewPort, _ }
import com.spok.model.SpokModel.{ Geo, LastSpoks, Spok, Url }
import com.spok.persistence.cassandra.CassandraProvider._
import com.spok.persistence.dse.DseConnectionUri._
import com.spok.persistence.factory.DSEGraphPersistenceFactoryApi
import com.spok.persistence.factory.spokgraph.DSESpokApi
import com.spok.persistence.factory.spoklog.SpokLogging
import com.spok.util.Constant._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ FlatSpec, Matchers }

class SpokLoggerSpec extends FlatSpec with Matchers with SpokLogger with MockitoSugar {

  val mockedDSESpokFactoryApi: DSESpokApi = mock[DSESpokApi]
  val mockedDSESpokLogging: SpokLogging = mock[SpokLogging]
  override val dseSpokFactoryApi = mockedDSESpokFactoryApi

  behavior of "SpokLogSpec "

  it should "be able to insert spoker and spok history details in cassandra when user creates a spok" in {

    val spokerId = getUUID().toString
    val spokId = getUUID().toString
    val url = Url("address", "url_title", "url_text", "url_preview", Some("url_type"))
    val geo = Geo(132233.67, 123244.56, 3133113.3)
    val spokDetails = Spok("content_type", Some("0"), Some("Public"), Some(0), Some("instance_text"), None, Some("text"), Some(url), None,
      None, geo, spokId)
    insertSpokCreationEvent(spokDetails, spokerId)

    val spokerRes = cassandraConn.execute(s"SELECT * from $spoker where spokerid = '$spokerId' ")
    val spokerRow: Row = spokerRes.one()
    val spokRes = cassandraConn.execute(s"SELECT * from $spok where spokid = '$spokId' ")
    val spokRow: Row = spokRes.one()

    assert(spokerRow.getString("spokerid") == spokerId)
    assert(spokerRow.getString("eventname") == RESPOKED_EVENT)
    assert(spokRow.getString("spokid") == spokId)
    assert(spokRow.getString("eventname") == RESPOKED_EVENT)
  }

  it should "be able to insert spoker and spok history details in cassandra when linking spoker's follower with spok" in {

    val spokerId = getUUID().toString
    val spokId = getUUID().toString
    val followerId = getUUID().toString
    val launched = System.currentTimeMillis()
    val url = Url("address", "url_title", "url_text", "url_preview", Some("url_type"))
    val geo = Geo(132233.67, 123244.56, 3133113.3)
    val spokDetails = Spok("content_type", Some("0"), Some("public"), Some(0), Some("instance_text"), None, Some("text"), Some(url), None,
      None, geo, spokId)
    val followerDetails = List((followerId, 12.12, 12.12, 12.12, launched))
    when(mockedDSESpokFactoryApi.linkSpokerFollowers(spokerId, Some("0"), spokId, geo, Some("public"), Some("instance_text"), launched, 0)) thenReturn followerDetails
    linkFollowers(spokerId, spokDetails, 0)

    val spokerRes = cassandraConn.execute(s"SELECT * from $spoker where spokerid = '$followerId' ")
    val spokerRow: Row = spokerRes.one()
    val spokRes = cassandraConn.execute(s"SELECT * from $spok where spokid = '$spokId' ")
    val spokRow: Row = spokRes.one()

    assert(spokerRow.getString("spokerid") == followerId)
    assert(spokerRow.getString("eventname") == PENDING)
    assert(spokRow.getString("spokid") == spokId)
    assert(spokRow.getString("spokerid") == followerId)
    assert(spokRow.getString("eventname") == PENDING)
  }

  it should "be able to add disable log in spok and spoker history tables in cassandra" in {
    val userId = getUUID()
    val spokId = getUUID()
    insertSpokEvent(userId, spokId, System.currentTimeMillis(), DISABLED_EVENT)

    val spokerRes = cassandraConn.execute(s"SELECT * from $spoker where spokerid = '$userId' ")
    val spokerRow: Row = spokerRes.one()
    val spokRes = cassandraConn.execute(s"SELECT * from $spok where spokid = '$spokId' ")
    val spokRow: Row = spokRes.one()

    assert(spokerRow.getString("spokerid") == userId)
    assert(spokerRow.getString("eventname") == DISABLED_EVENT)
    assert(spokRow.getString("spokid") == spokId)
  }

  it should "be able to add comment log in spok and spoker history tables in cassandra" in {
    val commenterId = getUUID()
    val commentId = getUUID()
    val spokId = getUUID()
    val geo = Geo(13.67, 12.56, 31.3)
    insertCommentEvent(commenterId, commentId, spokId, System.currentTimeMillis(), "this is my comment", geo, COMMENT_ADDED_EVENT)

    val spokerRes = cassandraConn.execute(s"SELECT * from $spoker where spokerid = '$commenterId' ")
    val spokerRow: Row = spokerRes.one()
    val spokRes = cassandraConn.execute(s"SELECT * from $spok where spokid = '$spokId' ")
    val spokRow: Row = spokRes.one()

    assert(spokerRow.getString("spokerid") == commenterId)
    assert(spokerRow.getString("eventname") == COMMENT_ADDED_EVENT)
    assert(spokRow.getString("spokid") == spokId)
  }

  it should "be able to add comment log in spok and spoker history tables in cassandra with special characters" in {
    val commenterId = getUUID()
    val commentId = getUUID()
    val spokId = getUUID()
    val geo = Geo(13.67, 12.56, 31.3)
    insertCommentEvent(commenterId, commentId, spokId, System.currentTimeMillis(), "this is my comment with $ and ', should be added", geo, COMMENT_ADDED_EVENT)

    val spokerRes = cassandraConn.execute(s"SELECT * from $spoker where spokerid = '$commenterId' ")
    val spokerRow: Row = spokerRes.one()
    val spokRes = cassandraConn.execute(s"SELECT * from $spok where spokid = '$spokId' ")
    val spokRow: Row = spokRes.one()

    assert(spokerRow.getString("spokerid") == commenterId)
    assert(spokerRow.getString("eventname") == COMMENT_ADDED_EVENT)
    assert(spokRow.getString("spokid") == spokId)
  }

  it should "be able to remove comment log in spok and spoker history tables in cassandra" in {
    val commenterId = getUUID()
    val commentId = getUUID()
    val spokId = getUUID()
    val geo = Geo(13.67, 12.56, 31.3)
    insertRemoveCommentEvent(commenterId, commentId, spokId, System.currentTimeMillis(), geo)
    val spokerRes = cassandraConn.execute(s"SELECT * from $spoker where spokerid = '$commenterId' ")
    val spokerRow: Row = spokerRes.one()
    val spokRes = cassandraConn.execute(s"SELECT * from $spok where spokid = '$spokId' ")
    val spokRow: Row = spokRes.one()

    assert(spokerRow.getString("spokerid") == commenterId)
    assert(spokerRow.getString("eventname") == COMMENT_REMOVED_EVENT)
    assert(spokRow.getString("spokid") == spokId)
  }

  it should "be able to extract hashtags from text and save in cassandra" in {
    val text = "Hi I am cyril feeling #fantaboluos#awesome at stadium"
    val hashtag = insertHashTags(text)
    val spokerRes = cassandraConn.execute(s"SELECT * from $hashtagTable where hashtag = 'fantaboluos' ")
    assert(spokerRes.one().getString("hashtag") == "fantaboluos")
  }

  it should "be able to insert launchsearch details in cassandra" in {

    val spokId1 = getUUID()
    val spok: Spok = Spok("rawtext", None, Some("public"), Some(1), Some("instanceTextnew"), None, Some("#famous #fabolousText"),
      None, None, None, Geo(11.22, 59.55, 457), spokId1)

    val user1Id = getUUID()
    val currentTimeInMilis = new Date()
    val user1 = User("Cyril", currentTimeInMilis, Location(List(LocationDetails(
      List(AddressComponents("Marsiele", "Marsiele", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(43.2805546, 5.2647101), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+919582311059", "+919582611051", "+919999999999"), "+919638527401", user1Id, Some("testuser.jpg"))

    DSEGraphPersistenceFactoryApi.insertUser(user1)
    DSEGraphPersistenceFactoryApi.createUserSetting(user1.userId)
    val dseSpokApi = DSESpokApi
    dseSpokApi.createSpok(user1Id, spok)
    val response = insertLaunchSearchDetailsOfSpok(user1Id, spok)
    val spokRes = cassandraConn.execute(s"SELECT spokdetails from $LaunchSearchTable where spokid = '$spokId1' and userid='$user1Id' ")
    val spokRow: String = spokRes.one().getString("spokdetails")
    val lastSpoks = parse("[" + spokRow.mkString + "]").extractOpt[List[LastSpoks]].get
    assert(lastSpoks(0).id == spokId1)
    assert(lastSpoks(0).author.id == user1Id)
  }

  it should "be able to insert or update subscriber details" in {
    val spokId = getUUID().toString
    val userId = getUUID().toString
    val result = upsertSubscriberDetails(spokId, userId)
    assert(result)
  }

  it should "be able to remove subscriber details" in {
    val spokId = getUUID().toString
    val userId = getUUID().toString
    upsertSubscriberDetails(spokId, userId)
    val result = removeSubscriberDetails(spokId, userId)
    assert(result)
  }

  it should "be able to fetch subscriber details" in {
    val spokId = getUUID().toString
    val userId = getUUID().toString
    upsertSubscriberDetails(spokId, userId)
    val result = fetchSubscriberDetails
    assert(result.nonEmpty)
  }

}
