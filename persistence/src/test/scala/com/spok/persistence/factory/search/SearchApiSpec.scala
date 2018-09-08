package com.spok.persistence.factory.search

import java.util.Date

import com.spok.model.Account.{ FollowUnfollow, User }
import com.spok.model.SpokModel.{ Spok, _ }
import com.spok.model._
import com.spok.persistence.cassandra.CassandraProvider._
import com.spok.persistence.dse.DseConnectionUri._
import com.spok.persistence.dse.DseGraphFactory
import com.spok.persistence.factory.DSEGraphPersistenceFactoryApi
import com.spok.persistence.factory.spokLog.LaunchSearchSpokDetails
import com.spok.persistence.factory.spokgraph.DSESpokApi
import com.spok.util.Constant._
import org.scalatest.{ BeforeAndAfterAll, FlatSpec, Matchers }

class SearchApiSpec extends FlatSpec with Matchers with SearchApi with BeforeAndAfterAll {

  val obj = DSEGraphPersistenceFactoryApi
  val searchBatch = SearchBatch
  val dseSpokApi = DSESpokApi
  val date: Date = new java.util.Date()

  val user1Id = getUUID()
  val user1 = User("Cyril", date, Location(List(LocationDetails(
    List(AddressComponents("Marsiele", "Marsiele", List("locality", "political"))),
    " Noida, Uttar Pradesh 201301, India",
    Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(43.2805546, 5.2647101), "APPROXIMATE",
      ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
  )), "OK"),
    "male", List("+919582311059", "+919582611051", "+919999999999", "+919983899777"), "+919638527401", user1Id, Some("testuser.jpg"))

  val user2Id = getUUID()
  val user2 = User("Kais", date, Location(List(LocationDetails(
    List(AddressComponents("Paris", "Paris", List("locality", "political"))),
    " Noida, Uttar Pradesh 201301, India",
    Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(48.8589506, 2.2773452), "APPROXIMATE",
      ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
  )), "OK"),
    "male", List("+919582311067"), "+919582311059", user2Id, Some("testuser1.jpg"))

  val user3Id = getUUID()
  val user3 = User("Vikas", date, Location(List(LocationDetails(
    List(AddressComponents("Noida", "Noida", List("locality", "political"))),
    " Noida, Uttar Pradesh 201301, India",
    Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(27.22, 27.55), "APPROXIMATE",
      ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
  )), "OK"),
    "male", List(), "+919582611051", user3Id, Some("testuser1.jpg"))

  val user4Id = getUUID()
  val user4 = User("Vincent", date, Location(List(LocationDetails(
    List(AddressComponents("Tihati", "Tihati", List("locality", "political"))),
    " Noida, Uttar Pradesh 201301, India",
    Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(-17.6871718, -149.5132954), "APPROXIMATE",
      ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
  )), "OK"),
    "male", List("+919582311889"), "+919582311067", user4Id, Some("testuser1.jpg"))

  val user5Id = getUUID()
  val user5 = User("Ayush", date, Location(List(LocationDetails(
    List(AddressComponents("Bombay", "Bombay", List("locality", "political"))),
    " Noida, Uttar Pradesh 201301, India",
    Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(19.0821975, 72.7407731), "APPROXIMATE",
      ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
  )), "OK"),
    "male", List(), "+919582311889", user5Id, Some("testuser1.jpg"))

  val user6Id = getUUID()
  val user6 = User("testuser", date, Location(List(LocationDetails(
    List(AddressComponents("Bombay", "Bombay", List("locality", "political"))),
    " Noida, Uttar Pradesh 201301, India",
    Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(19.0821975, 72.7407731), "APPROXIMATE",
      ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
  )), "OK"),
    "male", List(), "+919582311888", user6Id, Some("testuser1.jpg"))

  val user7Id = getUUID()
  val user7 = User("testuser1", date, Location(List(LocationDetails(
    List(AddressComponents("Bombay", "Bombay", List("locality", "political"))),
    " Noida, Uttar Pradesh 201301, India",
    Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(19.0821975, 72.7407731), "APPROXIMATE",
      ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
  )), "OK"),
    "male", List(), "+919582311887", user7Id, Some("testuser1.jpg"))

  val location = Location(List(LocationDetails(
    List(AddressComponents("London", "London", List("locality", "political"))),
    "11 Downing Street, United Kingdom",
    Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(43.2805546, 5.2647101), "APPROXIMATE",
      ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
  )), "OK")

  val hashTags = List("fantastic")
  val currentTimeInMilis = new Date()
  val spokDetails = write(ViewSpok("spokId1", "rawText", 0, currentTimeInMilis, "#fantastic spok", Some(currentTimeInMilis),
    "curHeading", false, SpokModel.Spoker("userId1", "abc", "male", "nopicture"), SpokModel.Spoker("userId1", "abc", "male", "nopicture"),
    "visibility", SpokModel.Counters(1, 2, 3, 22.22), SpokModel.Content()))

  val json = write(LaunchSearchSpokDetails("userId1", "spokId1", spokDetails, hashTags.mkString(","),
    22.01, 33.02, 123456789, "picture"))

  val geo = Geo(45.00, 45.00, 45.00)

  override def beforeAll {
    cassandraConn.execute(s"INSERT INTO $LaunchSearchTable JSON '$json'")
    obj.insertUser(user1)
    obj.createUserSetting(user1.userId)
    obj.insertUser(user2)
    obj.createUserSetting(user2.userId)
    obj.insertUser(user3)
    obj.createUserSetting(user3.userId)
    obj.insertUser(user4)
    obj.createUserSetting(user4.userId)
    obj.insertUser(user5)
    obj.createUserSetting(user5.userId)
    obj.insertUser(user6)
    obj.createUserSetting(user6.userId)
    obj.insertUser(user7)
    obj.createUserSetting(user7.userId)
    obj.performFollow(FollowUnfollow("919582311059", user2Id, user1Id))
    obj.updateDefaultGroup(user2Id, user1Id, FOLLOWS)
    obj.performFollow(FollowUnfollow("919582611051", user3Id, user1Id))
    obj.updateDefaultGroup(user3Id, user1Id, FOLLOWS)
    obj.performFollow(FollowUnfollow("919582611051", user3Id, user4Id))
    obj.updateDefaultGroup(user3Id, user4Id, FOLLOWS)
    obj.performFollow(FollowUnfollow("919582611051", user4Id, user3Id))
    obj.updateDefaultGroup(user4Id, user3Id, FOLLOWS)
    obj.performFollow(FollowUnfollow("919582311067", user4Id, user2Id))
    obj.updateDefaultGroup(user4Id, user2Id, FOLLOWS)
    obj.performFollow(FollowUnfollow("919582311889", user5Id, user4Id))
    obj.updateDefaultGroup(user5Id, user4Id, FOLLOWS)
    obj.performFollow(FollowUnfollow("919582311889", user6Id, user5Id))
    obj.updateDefaultGroup(user6Id, user5Id, FOLLOWS)
    obj.performFollow(FollowUnfollow("919582311889", user7Id, user1Id))
    obj.updateDefaultGroup(user7Id, user1Id, FOLLOWS)
    obj.performFollow(FollowUnfollow("919582311889", user6Id, user1Id))
    obj.updateDefaultGroup(user6Id, user1Id, FOLLOWS)
    obj.performFollow(FollowUnfollow("919582311889", user7Id, user5Id))
    obj.updateDefaultGroup(user7Id, user5Id, FOLLOWS)
  }

  override def afterAll: Unit = {
    DseGraphFactory.dseConn.executeGraph("g.V().drop()")
  }

  behavior of "SearchApiSpec "

  it should "be able to get most relevant nicknames." in {
    val (result, flag) = getByNickname("ViK")
    assert(result.size == 1)
    assert(result.head.nickname == "Vikas")
  }

  it should "be able to get most relevant hashtag." in {
    cassandraConn.execute(s"INSERT INTO hashtags_test (hashtag) VALUES ('awesome')")
    cassandraConn.execute(s"INSERT INTO hashtags_test (hashtag) VALUES ('awesome1')")
    val (result, flag) = getByHashtag("awe")
    assert(result == List("awesome", "awesome1"))
    assert(flag == true)
  }

  it should "be able to get top most spoker in batch of 2" in {
    val endTime = timeStamp
    val timeDiffernece: Long = 10 * 3600 * 1000
    val startTime: Long = endTime - timeDiffernece
    searchBatch.getPopularSpokersInBatch(startTime, endTime)
    val res = getPopularSpokers("1")
    assert(res.get.previous == "1")
    assert(res.get.spokers.size >= 2)
  }

  it should "be able to get last 10 spoks" in {
    val spokId1 = getUUID()
    val spokId2 = getUUID()
    val spok1: Spok = Spok("rawtext", None, Some("public"), Some(1), Some("instanceText1"), None, Some("Text1"),
      None, None, None, Geo(27.22, 27.55, 222), spokId1)
    dseSpokApi.createSpok(user1Id, spok1)
    dseSpokApi.linkSpokerFollowers(user1Id, spok1.groupId, spok1.spokId, spok1.geo, spok1.visibility, spok1.headerText, spok1.launched, 0)
    val spok2: Spok = Spok("rawtext", None, Some("public"), Some(1), Some("instanceText2"), None, Some("Text2"),
      None, None, None, Geo(37.22, 37.55, 222), spokId2)
    dseSpokApi.createSpok(user1Id, spok2)
    dseSpokApi.linkSpokerFollowers(user1Id, spok2.groupId, spok2.spokId, spok2.geo, spok2.visibility, spok2.headerText, spok2.launched, 0)
    val endTime = timeStamp
    val timeDiffernece: Long = 10 * 3600 * 1000
    val startTime: Long = endTime - timeDiffernece
    searchBatch.getLastSpokInBatch(startTime, endTime)
    val spokObj: SpoksResponse = getLastSpoks(user2Id, "1").get
    assert(spokObj.previous.equals("1"))
    assert(spokObj.spoks.size > 1)
  }

  it should "be able to get 10 friend spoks" in {
    val spokId1 = getUUID()
    val spokId2 = getUUID()
    val spok1: Spok = Spok("rawtext", None, Some("public"), Some(1), Some("instanceText"), None, Some("Text"),
      Some(Url("Address", "Title", "text", "previewLink", Some("urlType"))), None, None,
      Geo(27.22, 27.55, 222), spokId1)
    dseSpokApi.createSpok(user3Id, spok1)
    dseSpokApi.linkSpokerFollowers(user3Id, spok1.groupId, spok1.spokId, spok1.geo, spok1.visibility, spok1.headerText, spok1.launched, 0)
    val spok2: Spok = Spok("rawtext", None, Some("public"), Some(1), Some("instanceText"), None, Some("Text"),
      Some(Url("Address", "Title", "text", "previewLink", Some("urlType"))), None, None,
      Geo(27.22, 27.55, 222), spokId2)
    dseSpokApi.createSpok(user3Id, spok2)
    dseSpokApi.linkSpokerFollowers(user3Id, spok2.groupId, spok2.spokId, spok2.geo, spok2.visibility, spok2.headerText, spok2.launched, 0)
    val spokObj = getFriendSpoks(user4Id, "1").get
    assert(spokObj.previous.equals("1"))
    assert(spokObj.spoks.size == 2)
  }

  it should "be able to get last 10 Trendy Spoks" in {
    val spokId1 = getUUID()
    val spokId2 = getUUID()
    val userId = getUUID()
    val spok1: Spok = Spok("rawtext", None, Some("public"), Some(1), Some("instanceText1"), None, Some("Text1"),
      None, None, None, Geo(27.22, 27.55, 222), spokId1)
    dseSpokApi.createSpok(user1Id, spok1)
    dseSpokApi.linkSpokerFollowers(user1Id, spok1.groupId, spok1.spokId, spok1.geo, spok1.visibility, spok1.headerText, spok1.launched, 0)
    val spok2: Spok = Spok("rawtext", None, Some("public"), Some(1), Some("instanceText2"), None, Some("Text2"),
      None, None, None, Geo(37.22, 37.55, 222), spokId2)
    dseSpokApi.createSpok(user1Id, spok2)
    dseSpokApi.linkSpokerFollowers(user1Id, spok2.groupId, spok2.spokId, spok2.geo, spok2.visibility, spok2.headerText, spok2.launched, 0)
    val endTime = timeStamp
    val timeDiffernece: Long = 10 * 3600 * 1000
    val startTime: Long = endTime - timeDiffernece
    searchBatch.getTrendySpokInBatch(startTime, endTime)
    val res: Option[SpoksResponse] = getTrendySpok("1", userId)
    assert(res.get.previous == "1")
    assert(res.get.spoks.size >= 2)
  }

  it should "cover all cases for launch search" in {
    val withoutUserIdQuery = fetchLaunchSearch(List(), List("famous"), "22.33", "33.44", "123456789", "123456780", List("picture"), 0, 10)
    val resWithoutUserId = """q=spokdetails:(''+TO+*)+AND+hashtag:(famous)+AND+geo_lat:[21.8800000+TO+22.7800000]+AND+geo_long:[32.9900000+TO+33.8900000]+AND+launchedtime:[123456789+TO+123456780]+AND+contenttype:(picture)&sort=score+desc+,+launchedtime+desc&df=hashtag&start=0&rows=10&fl=spokdetails"""
    val withoutHashTagIdQuery = fetchLaunchSearch(List("userid"), List(), "22.33", "33.44", "123456789", "123456780", List("picture"), 0, 10)
    val resWithoutHashTagId = """q=spokdetails:(''+TO+*)+AND+userid:(userid)+AND+geo_lat:[21.8800000+TO+22.7800000]+AND+geo_long:[32.9900000+TO+33.8900000]+AND+launchedtime:[123456789+TO+123456780]+AND+contenttype:(picture)&sort=score+desc+,+launchedtime+desc&df=hashtag&start=0&rows=10&fl=spokdetails"""
    val withoutLatitudeQuery = fetchLaunchSearch(List("userid"), List("famous"), "", "33.44", "123456789", "123456780", List("picture"), 0, 10)
    val resWithoutLatitudeQuery = """q=spokdetails:(''+TO+*)+AND+userid:(userid)+AND+hashtag:(famous)+AND+geo_long:[32.9900000+TO+33.8900000]+AND+launchedtime:[123456789+TO+123456780]+AND+contenttype:(picture)&sort=score+desc+,+launchedtime+desc&df=hashtag&start=0&rows=10&fl=spokdetails"""
    val withoutLongitudeQuery = fetchLaunchSearch(List("userid"), List("famous"), "22.33", "", "123456789", "123456780", List("picture"), 0, 10)
    val resWithoutLongitudeQuery = """q=spokdetails:(''+TO+*)+AND+userid:(userid)+AND+hashtag:(famous)+AND+geo_lat:[21.8800000+TO+22.7800000]+AND+launchedtime:[123456789+TO+123456780]+AND+contenttype:(picture)&sort=score+desc+,+launchedtime+desc&df=hashtag&start=0&rows=10&fl=spokdetails"""
    val withoutAllQuery = fetchLaunchSearch(List(), List(), "", "", "", "", List(), 0, 10)
    val resWithoutAllQuery = """q=spokdetails:(''+TO+*)+&sort=score+desc+,+launchedtime+desc&df=hashtag&start=0&rows=10&fl=spokdetails"""
    val withOnlyTimestampQuery = fetchLaunchSearch(List(), List(), "", "", "123456789", "132456789", List(), 0, 10)
    val resWithOnlyTimestampQuery = """q=spokdetails:(''+TO+*)+AND+launchedtime:[123456789+TO+132456789]+&sort=score+desc+,+launchedtime+desc&df=hashtag&start=0&rows=10&fl=spokdetails"""
    val withOnlyContentTypeQuery = fetchLaunchSearch(List(), List(), "", "", "", "", List("picture"), 0, 10)
    val resWithOnlyContentTypeQuery = """q=spokdetails:(''+TO+*)+AND+contenttype:(picture)&sort=score+desc+,+launchedtime+desc&df=hashtag&start=0&rows=10&fl=spokdetails"""
    assert(withoutUserIdQuery.toString.equals(resWithoutUserId))
    assert(withoutHashTagIdQuery.toString.equals(resWithoutHashTagId))
    assert(withoutLatitudeQuery.toString.equals(resWithoutLatitudeQuery))
    assert(withoutLongitudeQuery.toString.equals(resWithoutLongitudeQuery))
    assert(withoutAllQuery.toString.equals(resWithoutAllQuery))
    assert(withOnlyTimestampQuery.toString.equals(resWithOnlyTimestampQuery))
    assert(withOnlyContentTypeQuery.toString.equals(resWithOnlyContentTypeQuery))
  }

  it should "be able to get spok from full launch search" in {
    val userId = getUUID()
    val res = getlaunchSearch("1", List("userId1"), List("fantastic"), "22.01", "33.02",
      "123456789", "123456790", List("picture"), userId)
    assert(res.get.previous == "1")
    assert(res.get.next == "")
    assert(res.get.spoks.head.id == "spokId1")
  }
}
