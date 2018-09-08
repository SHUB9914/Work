package com.spok.persistence.factory

import java.util.Date

import com.spok.model.Account._
import com.spok.model.SpokModel.{ Spok, _ }
import com.spok.model._
import com.spok.persistence.dse.DseGraphFactory
import com.spok.persistence.factory.dsequery.DSEUserQuery
import com.spok.persistence.factory.spokgraph.DSESpokApi
import com.spok.util.Constant._
import org.scalatest.{ BeforeAndAfterAll, FlatSpec, Matchers }

class DSEUserSpokFactoryApiSpec extends FlatSpec with Matchers with DSEUserSpokFactoryApi with BeforeAndAfterAll with DSEGraphPersistenceFactoryApi with DSEUserQuery {

  val obj = DSEGraphPersistenceFactoryApi
  val dseSpokApi = DSESpokApi
  val date: Date = new java.util.Date()
  val spokId = getUUID()
  val spok: Spok = Spok("String", None, Some("Public"), Some(1), Some("this is my first spok"), None, Some("first spok"),
    Some(Url("Address", "Title", "text", "previewLink", Some("urlType"))), None, None,
    Geo(43.2805546, 5.2647101, 222), spokId)

  val user1Id = getUUID()
  val user1 = User("Cyril", date, Location(List(LocationDetails(
    List(AddressComponents("Marsiele", "Marsiele", List("locality", "political"))),
    " Noida, Uttar Pradesh 201301, India",
    Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(43.2805546, 5.2647101), "APPROXIMATE",
      ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
  )), "OK"),
    "male", List("+919582311059", "+919582611051", "+919999999999"), "+919638527401", user1Id, Some("testuser.jpg"))

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

  val location = Location(List(LocationDetails(
    List(AddressComponents("London", "London", List("locality", "political"))),
    "11 Downing Street, United Kingdom",
    Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(43.2805546, 5.2647101), "APPROXIMATE",
      ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
  )), "OK")

  val geo = Geo(43.2805546, 5.2647101, 45)

  val user6Id = getUUID()
  val user6 = s"""graph.addVertex(label,"$USER","nickname","prashant", "userId" ,'$user6Id')"""

  override def beforeAll {
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
    obj.performFollow(FollowUnfollow("919582311059", user2Id, user1Id))
    obj.performFollow(FollowUnfollow("919582611051", user3Id, user1Id))
    obj.performFollow(FollowUnfollow("919582311067", user4Id, user2Id))
    obj.performFollow(FollowUnfollow("919582311889", user5Id, user4Id))
    dseSpokApi.createSpok(user1Id, spok)
  }

  override def afterAll: Unit = {
    DseGraphFactory.dseConn.executeGraph("g.V().drop()")
  }

  behavior of "DSEUserSpokFactoryApiSpec "

  it should "be able validate user Id in DSE graph " in {
    DseGraphFactory.dseConn.executeGraph(user6)
    val result: Boolean = isExistsUser(user6Id)
    assert(result)
  }

  it should "be able to validate poll question and fetch its spok id" in {
    val spokId = getUUID()
    val pollAttributes = Poll("Quiz", None, List(PollQuestions(
      "How many planets do we have in our solar system ?",
      None, None, 1, List(PollAnswers("One", None, None, 1))
    )))
    val pollSpok = Spok("poll", None, None, None, None, None, None, None, Some(pollAttributes), None, geo, spokId)
    dseSpokApi.createSpok(user1Id, pollSpok)
    val spokVertexQuery = s"""g.V().hasLabel('users').has('userId','$user1Id').outE('isAssociatedWith').inV().has('spokId','$spokId').outE('containsA').inV()"""
    val spokVertex = DseGraphFactory.dseConn.executeGraph(spokVertexQuery).one().asVertex()
    dseSpokApi.insertPollWithQuestions(spokVertex, pollAttributes, timeStamp)
    val query =
      s"""g.V().hasLabel('users').has('userId','$user1Id').outE('isAssociatedWith').inV().has('spokId','$spokId').outE('containsA').inV().outE('hasAQuestion').inV()"""
    val questionId = DseGraphFactory.dseConn.executeGraph(query).one().asVertex().getProperty("id").getValue.asString()
    val result = validatePollQuestionAndFetchSpokId(questionId)
    assert(result === Some(spokId))
  }

  it should "be able validate whether poll question exists " in {
    val spokId = getUUID()
    val pollAttributes = Poll("Quiz", None, List(PollQuestions(
      "How many planets do we have in our solar system ?",
      None, None, 1, List(PollAnswers("One", None, None, 1))
    )))
    val pollSpok = Spok("poll", None, None, None, None, None, None, None, Some(pollAttributes), None, geo, spokId)
    dseSpokApi.createSpok(user1Id, pollSpok)
    val spokVertexQuery = s"""g.V().hasLabel('users').has('userId','$user1Id').outE('isAssociatedWith').inV().has('spokId','$spokId').outE('containsA').inV()"""
    val spokVertex = DseGraphFactory.dseConn.executeGraph(spokVertexQuery).one().asVertex()
    dseSpokApi.insertPollWithQuestions(spokVertex, pollAttributes, timeStamp)
    val query =
      s"""g.V().hasLabel('users').has('userId','$user1Id').outE('isAssociatedWith').inV().has('spokId','$spokId').outE('containsA').inV().outE('hasAQuestion').inV()"""
    val questionId = DseGraphFactory.dseConn.executeGraph(query).one().asVertex().getProperty("id").getValue.asString()
    val result = isQuestionExist(questionId)
    assert(result === true)
  }

  it should "be able to return status and enabled flag value" in {
    val spokId1 = getUUID()
    val spok1: Spok = Spok("picture", None, Some("Public"), Some(1), Some("instanceText"), None, Some("Text"),
      None, None, None, Geo(27.22, 27.55, 222), spokId1)
    dseSpokApi.createSpok(user1Id, spok1)
    val (status, isEnabled, edgeOpt) = validateSpokAndSendStatus(user1Id, spokId1)
    assert(isEnabled === true)
  }

  it should "be able to validate spok with enabled flag" in {
    val spokId1 = getUUID()
    val spok1: Spok = Spok("picture", None, Some("Public"), Some(1), Some("instanceText"), None, Some("Text"),
      None, None, None, Geo(27.22, 27.55, 222), spokId1)
    dseSpokApi.createSpok(user1Id, spok1)
    val result = isValidSpokWithEnabledFlag(spokId1)
    assert(result === true)
  }

  it should "be able to validate if grouo exists" in {
    val spokId1 = getUUID()
    val spok1: Spok = Spok("picture", None, Some("Public"), Some(1), Some("instanceText"), None, Some("Text"),
      None, None, None, Geo(27.22, 27.55, 222), spokId1)
    dseSpokApi.createSpok(user1Id, spok1)
    val result = isValidGroup(user1Id, "0")
    assert(result === true)
  }

  it should "be able to validate Spok by id" in {
    val res = validateAbsoluteSpokById(spokId)
    assert(res.equals(SPOK_VALID))
  }

  it should "not be able to validate Spok by id" in {
    val res = validateAbsoluteSpokById("123213")
    assert(res.equals(SPOK_NOT_FOUND))
  }

  it should "be able to validate Spok by id but will return disabled spok status" in {
    val spokId2 = getUUID()
    val spok: Spok = Spok("rawtext", None, Some("Public"), Some(1), Some("instanceText"), None, Some("Text"),
      Some(Url("Address", "Title", "text", "previewLink", Some("urlType"))), None, None,
      Geo(27.22, 27.55, 222), spokId2)

    dseSpokApi.createSpok(user1Id, spok)

    //dse query to make enabled field false
    DseGraphFactory.dseConn.executeGraph(s"g.V().hasLabel('spok').has('spokId','$spokId2').property('enabled','false')")
    val res = validateAbsoluteSpokById(spokId2)
    assert(res.equals(DISABLED_SPOK))
  }

  it should "be able to validate Spok by id while adding comment" in {
    val (res, spokv) = validateSpokById(spokId)
    assert(res.equals(SPOK_VALID))
  }

  it should "not be able to validate Spok by id while adding comment" in {
    val (res, spokv) = validateSpokById("213211")
    assert(res.equals(SPOK_NOT_FOUND))
  }

  it should "be able to hendle the generic error that comes in spokerSuspendedOrNot " in {
    val res = spokerSuspendedOrNot("213211")
    assert(res.equals(PROPERTY_NOT_FOUND))
  }

  it should "be able to find out spoker is not suspended successfully" in {
    val res = spokerSuspendedOrNot(user1Id)
    assert(res.equals(SPOKER_NOT_SUSPENDED))
  }

}
