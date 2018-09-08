package com.spok.persistence.factory.search

import java.util.Date

import com.spok.model.Account.{ FollowUnfollow, User }
import com.spok.model.SpokModel.{ Spok, _ }
import com.spok.model.{ InnerLocation, NorthEast, SouthWest, ViewPort, _ }
import com.spok.persistence.dse.DseGraphFactory
import com.spok.persistence.factory.DSEGraphPersistenceFactoryApi
import com.spok.persistence.factory.spokgraph.DSESpokApi
import com.spok.util.RandomUtil
import org.scalatest.{ BeforeAndAfterAll, FlatSpec, Matchers }

class SearchBatchSpec extends FlatSpec with Matchers with RandomUtil with BeforeAndAfterAll {

  val obj = DSEGraphPersistenceFactoryApi
  val dseSpokApi = DSESpokApi
  val searchBatch = SearchBatch
  val date: Date = new java.util.Date()

  val user1Id = getUUID()
  val user2Id = getUUID()
  val user1 = User("Cyril", date, Location(List(LocationDetails(
    List(AddressComponents("Marsiele", "Marsiele", List("locality", "political"))),
    " Noida, Uttar Pradesh 201301, India",
    Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(43.2805546, 5.2647101), "APPROXIMATE",
      ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
  )), "OK"),
    "male", List("+919582311059", "+919582611051", "+919999999999", "+919983899777"), "+919638527401", user1Id, Some("testuser.jpg"))
  val user2 = User("kais", date, Location(List(LocationDetails(
    List(AddressComponents("Marsiele", "Marsiele", List("locality", "political"))),
    " Noida, Uttar Pradesh 201301, India",
    Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(43.2805546, 5.2647101), "APPROXIMATE",
      ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
  )), "OK"),
    "male", List("+919582611051", "+919999999999", "+919983899777"), "+919582311059", user2Id, Some("testuser.jpg"))

  override def beforeAll {
    obj.insertUser(user1)
    obj.createUserSetting(user1.userId)
    obj.insertUser(user2)
    obj.createUserSetting(user2.userId)
    obj.performFollow(FollowUnfollow("919582311059", user2Id, user1Id))
  }

  override def afterAll: Unit = {
    DseGraphFactory.dseConn.executeGraph("g.V().drop()")
  }

  behavior of "SearchBatchSpec"

  it should "be able to create batch view for trendy spoks" in {
    val spokId1 = getUUID()
    val geo = Geo(45.00, 45.00, 45.00)
    val pollAttributes = Poll("Quiz", None, List(
      PollQuestions("How many planets do we have in our solar system ?", None, None, 1, List(PollAnswers("Eight", None, None, 1))),
      PollQuestions("How many suns do we have in our solar system ?", None, None, 2, List(PollAnswers("One", None, None, 1))),
      PollQuestions("How many moons does earth have ?", None, None, 3, List(PollAnswers("One", None, None, 1)))
    ))
    val pollSpok = Spok("poll", None, None, None, None, None, None, None, Some(pollAttributes), None, geo, spokId1)
    dseSpokApi.createSpok(user1Id, pollSpok)
    val spokVertexQuery = s"""g.V().hasLabel('users').has('userId','$user1Id').outE('isAssociatedWith').inV().has('spokId','$spokId1').outE('containsA').inV()"""
    val spokVertex = DseGraphFactory.dseConn.executeGraph(spokVertexQuery).one().asVertex()
    dseSpokApi.insertPollWithQuestions(spokVertex, pollAttributes, timeStamp)
    dseSpokApi.linkSpokerFollowers(user1Id, None, spokId1, geo, None, None, timeStamp, 3)

    val spokId2 = getUUID()
    val spok2: Spok = Spok("rawtext", None, Some("public"), Some(1), Some("instanceText2"), None, Some("Text2"),
      None, None, None, Geo(37.22, 37.55, 222), spokId2)
    dseSpokApi.createSpok(user1Id, spok2)
    dseSpokApi.linkSpokerFollowers(user1Id, spok2.groupId, spok2.spokId, spok2.geo, spok2.visibility, spok2.headerText, spok2.launched, 0)
    val endTime = timeStamp
    val timeDiffernece: Long = 10 * 3600 * 1000
    val startTime: Long = endTime - timeDiffernece
    val res = searchBatch.getTrendySpokInBatch(startTime, endTime)
    assert(res == true)
  }

  it should "be able to create batch view for popular spoker" in {
    val endTime = timeStamp
    val timeDiffernece: Long = 10 * 3600 * 1000
    val startTime: Long = endTime - timeDiffernece
    val res = searchBatch.getPopularSpokersInBatch(startTime, endTime)
    assert(res == true)
  }

  it should "be able to create batch view for last spoks" in {
    val spokId1 = getUUID()
    val geo = Geo(45.00, 45.00, 45.00)
    val pollAttributes = Poll("Quiz", None, List(
      PollQuestions("How many planets do we have in our solar system ?", None, None, 1, List(PollAnswers("Eight", None, None, 1))),
      PollQuestions("How many suns do we have in our solar system ?", None, None, 2, List(PollAnswers("One", None, None, 1))),
      PollQuestions("How many moons does earth have ?", None, None, 3, List(PollAnswers("One", None, None, 1)))
    ))
    val pollSpok = Spok("poll", None, None, None, None, None, None, None, Some(pollAttributes), None, geo, spokId1)
    dseSpokApi.createSpok(user1Id, pollSpok)
    val spokVertexQuery = s"""g.V().hasLabel('users').has('userId','$user1Id').outE('isAssociatedWith').inV().has('spokId','$spokId1').outE('containsA').inV()"""
    val spokVertex = DseGraphFactory.dseConn.executeGraph(spokVertexQuery).one().asVertex()
    dseSpokApi.insertPollWithQuestions(spokVertex, pollAttributes, timeStamp)
    dseSpokApi.linkSpokerFollowers(user1Id, None, spokId1, geo, None, None, timeStamp, 3)

    val spokId2 = getUUID()
    val spok2: Spok = Spok("rawtext", None, Some("public"), Some(1), Some("instanceText2"), None, Some("Text2"),
      None, None, None, Geo(37.22, 37.55, 222), spokId2)
    dseSpokApi.createSpok(user1Id, spok2)
    dseSpokApi.linkSpokerFollowers(user1Id, spok2.groupId, spok2.spokId, spok2.geo, spok2.visibility, spok2.headerText, spok2.launched, 0)
    val endTime = timeStamp
    val timeDiffernece: Long = 10 * 3600 * 1000
    val startTime: Long = endTime - timeDiffernece
    val res = searchBatch.getLastSpokInBatch(startTime, endTime)
    assert(res == true)
  }

}
