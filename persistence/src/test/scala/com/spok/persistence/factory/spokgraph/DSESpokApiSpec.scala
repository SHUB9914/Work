package com.spok.persistence.factory.spokgraph

import java.util.Date

import com.spok.model.Account._
import com.spok.model.SpokModel.{ Spok, _ }
import com.spok.model.{ InnerLocation, NorthEast, SouthWest, ViewPort, _ }
import com.spok.persistence.dse.DseGraphFactory
import com.spok.persistence.factory.{ DSEGraphPersistenceFactoryApi, DSEUserSpokFactoryApi }
import com.spok.util.Constant._
import org.scalatest.{ BeforeAndAfterAll, FlatSpec, Matchers }

import scala.collection.JavaConverters._
import scala.util.Try

class DSESpokApiSpec extends FlatSpec with Matchers with DSESpokApi with BeforeAndAfterAll {

  val obj = DSEGraphPersistenceFactoryApi
  val dSEUserSpokFactoryApi = DSEUserSpokFactoryApi
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

  val location = Location(List(LocationDetails(
    List(AddressComponents("London", "London", List("locality", "political"))),
    "11 Downing Street, United Kingdom",
    Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(43.2805546, 5.2647101), "APPROXIMATE",
      ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
  )), "OK")

  val geo = Geo(45.00, 45.00, 45.00)

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
    obj.updateDefaultGroup(user2Id, user1Id, FOLLOWS)
    obj.performFollow(FollowUnfollow("919582611051", user3Id, user1Id))
    obj.updateDefaultGroup(user3Id, user1Id, FOLLOWS)
    obj.performFollow(FollowUnfollow("919582611051", user4Id, user3Id))
    obj.updateDefaultGroup(user4Id, user3Id, FOLLOWS)
    obj.performFollow(FollowUnfollow("919582311067", user4Id, user2Id))
    obj.updateDefaultGroup(user4Id, user2Id, FOLLOWS)
    obj.performFollow(FollowUnfollow("919582311889", user5Id, user4Id))
    obj.updateDefaultGroup(user5Id, user4Id, FOLLOWS)
  }

  override def afterAll: Unit = {
    DseGraphFactory.dseConn.executeGraph("g.V().drop()")
  }

  behavior of "DSESpokApiSpec "

  it should "be able to send sms to contacts when user creates a spok" in {
    val spokId = getUUID().toString
    val contactVertex = DseGraphFactory.dseConn.executeGraph(s"g.V().hasLabel('users').has('userId','$user1Id').outE('hasAContact').inV().has('phoneNo','919983899777')").one.asVertex()
    val res: List[Try[String]] = sendSMSToContacts(List(contactVertex), spokId, s"$user1Id")
    assert(res.nonEmpty)
    assert(res.head.isSuccess)
  }

  it should "be able to send SMS when user respoks" in {
    val spokId = getUUID()
    val groupId = getUUID()
    obj.createGroup(user1Id, Group(groupId, "NewPrivateGroupByCyrils"))
    val result = obj.insertFollowersInGroup(user1Id, UserGroup(groupId, List(user3Id),
      List(Contact("", "+919983899777"))))
    val res = sendSMSToContactFromGroup(groupId, user1Id, spokId)
    assert(res.nonEmpty)
    assert(res.head.isSuccess)
  }

  it should "be able to insert rawtext type spok" in {
    val spokId1 = getUUID()
    val spok: Spok = Spok("rawtext", None, Some("public"), Some(1), Some("instanceText"), None, Some("Text"),
      Some(Url("Address", "Title", "text", "previewLink", Some("urlType"))), None, None,
      Geo(27.22, 27.55, 222), spokId1)
    val spokV = createSpok(user1Id, spok)
    assert(spokV)
  }

  it should "be able to insert rawtext type spok having special characters" in {
    val spokId1 = getUUID()
    val spok: Spok = Spok("rawtext", None, Some("public"), Some(1), Some("C'est le $ test"), None, Some("C'est le $$$ test"),
      Some(Url("Address", "Title", "text", "previewLink", Some("urlType"))), None, None,
      Geo(27.22, 27.55, 222), spokId1)
    val spokV = createSpok(user1Id, spok)
    assert(spokV)
  }

  it should "be able to insert rawtext type spok having special character double quote" in {
    val spokId1 = getUUID()
    val spok: Spok = Spok("rawtext", None, Some("public"), Some(1), Some("""C'est "l"e $ test"""), None, Some("C'est le $$$ test"),
      Some(Url("Address", "Title", "text", "previewLink", Some("urlType"))), None, None,
      Geo(27.22, 27.55, 222), spokId1)
    val spokV = createSpok(user1Id, spok)
    assert(spokV)
  }

  it should "be able to insert poll type spok" in {
    val poll = Poll("Survey", Some("Please fill it"), List(PollQuestions("Is Wine good for health", Some("poll"),
      None, 1, List(PollAnswers("Yes", None, None, 1), PollAnswers("No", None, None, 2)))))
    val pollSpok = Spok("poll", Some("0"), Some("public"), Some(0), Some("Fill the poll"), None, Some("Do it"), None, Some(poll),
      None, Geo(45.00, 45.00, 45.00), getUUID())
    val spokV = createSpok(user1Id, pollSpok)
    assert(spokV)
  }

  it should "be able to insert poll type spok with special characters in question and answers" in {
    val poll = Poll("$ur'vey", Some("Checking $pec!@l characters"), List(PollQuestions("??? Is Wine good for health $$$", Some("poll"),
      None, 1, List(PollAnswers("Ye's", None, None, 1), PollAnswers("Noi''", None, None, 2)))))
    val pollSpok = Spok("poll", Some("0"), Some("public"), Some(0), Some("Fill the poll$$"), None, Some("Do it"), None, Some(poll),
      None, Geo(45.00, 45.00, 45.00), getUUID())
    val spokV = createSpok(user1Id, pollSpok)
    assert(spokV)
  }

  it should "be able to insert url type spok" in {
    val spokId1 = getUUID()
    val spok: Spok = Spok("url", None, Some("public"), Some(1), Some("instanceText"), None, Some("Text"),
      Some(Url("Address", "Title", "text", "previewLink", Some("urlType"))), None, None,
      Geo(27.22, 27.55, 222), spokId1)
    val spokV = createSpok(user1Id, spok)
    assert(spokV)
  }

  it should "be able to insert url type spok with special characters" in {
    val spokId1 = getUUID()
    val spok: Spok = Spok("url", None, Some("public"), Some(1), Some("instanceText"), None, Some("Text"),
      Some(Url("Address", "Ti'tle$$$", "'$pec!@l'", "previewLink", Some("urlType"))), None, None,
      Geo(27.22, 27.55, 222), spokId1)
    val spokV = createSpok(user1Id, spok)
    assert(spokV)
  }

  it should "be able to insert media type spok" in {
    val spokId1 = getUUID()
    val spok: Spok = Spok("picture", None, Some("Public"), Some(1), Some("instanceText"), None, Some("Text"),
      Some(Url("Address", "Title", "text", "previewLink", Some("urlType"))), None, None,
      Geo(27.22, 27.55, 222), spokId1)

    val spokV = createSpok(user1Id, spok)
    assert(spokV)
  }

  it should "be able to insert riddle spok" in {
    val riddle = Riddle("riddle", RiddleQuestion("What is Pythagoras theorem", Some("riddle"), None),
      RiddleAnswer("b2+p2=h2", Some("riddle"), None))
    val riddleSpok = Spok("riddle", Some("0"), Some("Public"), Some(0), Some("Fill the poll"), None, Some("Do it"), None, None,
      Some(riddle), Geo(45.00, 45.00, 45.00), getUUID())
    val spokV = createSpok(user1Id, riddleSpok)
    assert(spokV)
  }

  it should "be able to insert riddle spok with special characters" in {
    val riddle = Riddle("riddle", RiddleQuestion("What is Pytha'gora$ theorem", Some("riddle"), None),
      RiddleAnswer("b2+p2=h2", Some("riddle"), None))
    val riddleSpok = Spok("riddle", Some("0"), Some("Public"), Some(0), Some("'$$Fill the poll$$'"), None, Some("Do it"), None, None,
      Some(riddle), Geo(45.00, 45.00, 45.00), getUUID())
    val spokV = createSpok(user1Id, riddleSpok)
    assert(spokV)
  }

  it should "be able to create spok with default group and link followers with it " in {
    val spokId = getUUID()
    val spok1: Spok = Spok("rawtext", Some("0"), Some("public"), Some(1), Some("this is my first spok"), None, Some("first spok"),
      None, None, None, Geo(43.28, 5.26, 22), spokId)
    assert(createSpok(user1Id, spok1))
    val launched = timeStamp
    val res = linkSpokerFollowers(user1Id, Some("0"), spokId, Geo(43.28, 5.26, 22), Some("public"), Some("this is my first spok"), launched, 0)
    assert(!res.isEmpty)
  }

  it should "be able to create spok with private group and link followers and contacts with it " in {
    val spokId = getUUID()
    val groupId = getUUID()
    obj.createGroup(user1Id, Group(groupId, "my_friends"))
    val result = obj.insertFollowersInGroup(user1Id, UserGroup(groupId, List(user3Id),
      List(Contact("", "+919983899777"))))
    val spok1: Spok = Spok("rawtext", Some(groupId), Some("private"), Some(1), Some("this is my first spok"), None, Some("first spok"),
      None, None, None,
      Geo(43.2805546, 5.2647101, 222), spokId)
    val launched = timeStamp
    assert(createSpok(user1Id, spok1))
    val res = linkSpokerFollowers(user1Id, Some(groupId), spokId, Geo(43.2805546, 5.2647101, 222),
      Some("private"), Some("this is my first spok"), launched, 0)
    assert(!res.isEmpty)
    assert(res.head._2 == 27.22)
    assert(res.head._3 == 27.55)
  }

  it should "be able to respok a spok when a user is connected in pending state with the spok " in {

    val spokId1 = getUUID()
    val geo = Geo(45.00, 45.00, 45.00)
    val spok: Spok = Spok("rawtext", None, Some("public"), Some(0), Some("This spok will be respoked"), None, Some("Text"),
      None, None, None, geo, spokId1)
    createSpok(user1Id, spok)
    linkSpokerFollowers(user1Id, Some("0"), spokId1, geo, Some("public"), Some("This spok will be respoked"), timeStamp, 0)
    val edge = DseGraphFactory.dseConn.executeGraph(getEdgeBetweenUserAndSpok(user1Id, spokId1)).one().asEdge()
    val (response, error) = createRespok(spokId1, user2Id, Respok(Some("0"), Some("public"), Some("I am respoking"), geo, None), Some(edge))
    assert(response.get.spokId equals spokId1)
    assert(error.isEmpty)
  }

  it should "be able to respok a spok when a user is connected in pending state with the spok and respok text has special characters " in {

    val spokId1 = getUUID()
    val geo = Geo(45.00, 45.00, 45.00)
    val spok: Spok = Spok("rawtext", None, Some("public"), Some(0), Some("This spok will be respoked"), None, Some("Text"),
      None, None, None, geo, spokId1)
    createSpok(user1Id, spok)
    linkSpokerFollowers(user1Id, Some("0"), spokId1, geo, Some("public"), Some("This spok will be respoked"), timeStamp, 0)
    val edge = DseGraphFactory.dseConn.executeGraph(getEdgeBetweenUserAndSpok(user1Id, spokId1)).one().asEdge()
    val (response, error) = createRespok(spokId1, user2Id, Respok(Some("0"), Some("public"), Some("I am respoking with $peci'@l chars"), geo, None), Some(edge))
    assert(response.get.spokId equals spokId1)
    assert(error.isEmpty)
  }

  it should "be able to respok a spok when a user is connected in pending state with the spok and respok text has special character double quote" in {

    val spokId1 = getUUID()
    val geo = Geo(45.00, 45.00, 45.00)
    val spok: Spok = Spok("rawtext", None, Some("public"), Some(0), Some("This spok will be respoked"), None, Some("Text"),
      None, None, None, geo, spokId1)
    createSpok(user1Id, spok)
    linkSpokerFollowers(user1Id, Some("0"), spokId1, geo, Some("public"), Some("This spok will be respoked"), timeStamp, 0)
    val edge = DseGraphFactory.dseConn.executeGraph(getEdgeBetweenUserAndSpok(user1Id, spokId1)).one().asEdge()
    val (response, error) = createRespok(spokId1, user2Id, Respok(Some("0"), Some("public"), Some("""I am "respoking" with $peci'@l chars"""), geo, None), Some(edge))
    assert(response.get.spokId equals spokId1)
    assert(error.isEmpty)
  }

  it should "not be able to respok a spok when a user is not connected with the spok " in {

    val spokId1 = getUUID()
    val geo = Geo(45.00, 45.00, 45.00)
    val spok: Spok = Spok("rawtext", None, Some("public"), Some(0), Some("This spok will not be respoked"), None, Some("Text"),
      None, None, None, geo, spokId1)
    createSpok(user1Id, spok)
    val (response, error) = createRespok(spokId1, user2Id, Respok(Some("0"), Some("public"), Some("I am trying respoking"), geo, None), None)
    assert(response.isEmpty)
    assert(error.get === Error(SPK_117, s"Unable re-spoking spok $spokId1 (generic error)."))
  }

  it should "not be able to respok a private spok as public" in {

    val spokId1 = getUUID()
    val groupId = getUUID()
    val geo = Geo(45.00, 45.00, 45.00)
    obj.createGroup(user1Id, Group(groupId, "my_spok"))
    obj.insertFollowersInGroup(user1Id, UserGroup(groupId, List(user2Id), List()))
    val spok: Spok = Spok("rawtext", Some(groupId), Some("private"), Some(0), Some("This spok will not be respoked for public as it is private"),
      None, Some("Text"), None, None, None, geo, spokId1)
    createSpok(user1Id, spok)
    linkSpokerFollowers(user1Id, Some(groupId), spokId1, geo, Some("private"),
      Some("This spok will not be respoked for public as it is private"), timeStamp, 0)
    val edge = DseGraphFactory.dseConn.executeGraph(getEdgeBetweenUserAndSpok(user1Id, spokId1)).one().asEdge()

    val (response, error) = createRespok(spokId1, user2Id, Respok(Some("0"), Some("public"), Some("I am trying respoking"), geo, None), Some(edge))
    assert(response.isEmpty)
    assert(error.get === Error(SPK_107, NOT_ALTER_VISIBILITY))
  }

  it should "be able to respok a private spok as private only" in {

    val spokId1 = getUUID()
    val groupId = getUUID()
    val geo = Geo(45.00, 45.00, 45.00)
    obj.createGroup(user1Id, Group(groupId, "my_spok_friends"))
    obj.insertFollowersInGroup(user1Id, UserGroup(groupId, List(user2Id), List()))
    val spok: Spok = Spok("rawtext", Some(groupId), Some("private"), Some(0), Some("This spok will be respoked for private as it is private"), None, Some("Text"),
      None, None, None, geo, spokId1)
    createSpok(user1Id, spok)
    linkSpokerFollowers(user1Id, Some(groupId), spokId1, geo, Some("private"),
      Some("This spok will be respoked for private as it is private"), timeStamp, 0)
    val edge = DseGraphFactory.dseConn.executeGraph(getEdgeBetweenUserAndSpok(user1Id, spokId1)).one().asEdge()

    val (response, error) = createRespok(spokId1, user2Id, Respok(Some(groupId), Some("private"), Some("I am trying respoking"), geo, None), Some(edge))
    assert(response.get.spokId equals spokId1)
    assert(error.isEmpty)
  }

  it should "not be able to respok a private spok as private in any other group other than the group to which the spok is related" in {

    val spokId1 = getUUID()
    val groupId = getUUID()
    val secondGroupId = getUUID()
    val geo = Geo(45.00, 45.00, 45.00)
    obj.createGroup(user1Id, Group(groupId, "my_spok_personal"))
    obj.createGroup(user2Id, Group(secondGroupId, "my_spok_other"))
    obj.insertFollowersInGroup(user1Id, UserGroup(groupId, List(user2Id), List()))
    obj.insertFollowersInGroup(user2Id, UserGroup(secondGroupId, List(user4Id), List()))
    val spok: Spok = Spok("rawtext", Some(groupId), Some("private"), Some(0), Some("This spok will not be respoked for private in any other group"),
      None, Some("Text"), None, None, None, geo, spokId1)
    createSpok(user1Id, spok)
    linkSpokerFollowers(user1Id, Some(groupId), spokId1, geo, Some("private"),
      Some("This spok will not be respoked for private in any other group"), timeStamp, 0)
    val edge = DseGraphFactory.dseConn.executeGraph(getEdgeBetweenUserAndSpok(user1Id, spokId1)).one().asEdge()

    val (response, error) = createRespok(spokId1, user2Id, Respok(Some(secondGroupId), Some("private"), Some("I am trying respoking"), geo, None), Some(edge))
    assert(response.isEmpty)
    assert(error.get === Error(SPK_128, RESPOK_IN_OTHER_GROUP_ERROR))
  }

  it should "not be able to respok a private spok if the user does not give a group id in respok data" in {

    val spokId1 = getUUID()
    val groupId = getUUID()
    val geo = Geo(45.00, 45.00, 45.00)
    obj.createGroup(user1Id, Group(groupId, "my_spok_spartans"))
    obj.insertFollowersInGroup(user1Id, UserGroup(groupId, List(user2Id), List()))
    val spok: Spok = Spok("rawtext", Some(groupId), Some("private"), Some(0), Some("This spok will not be respoked"), None, Some("Text"),
      None, None, None, geo, spokId1)
    createSpok(user1Id, spok)
    linkSpokerFollowers(user1Id, Some(groupId), spokId1, geo, Some("private"), Some("This spok will not be respoked"), timeStamp, 0)
    val edge = DseGraphFactory.dseConn.executeGraph(getEdgeBetweenUserAndSpok(user1Id, spokId1)).one().asEdge()

    val (response, error) = createRespok(spokId1, user2Id, Respok(None, Some("private"), Some("I am trying respoking"), geo, None), Some(edge))

    assert(response.isEmpty)
    assert(error.get === Error(SPK_130, RESPOK_IN_DEFAULT_GROUP_ERROR))
  }

  it should "not be able to respok a poll spok if the user has not answered any of the questions" in {

    val spokId = getUUID()
    val pollAttributes = Poll("Quiz", None, List(
      PollQuestions("How many planets do we have in our solar system ?", None, None, 1, List(PollAnswers("Eight", None, None, 1))),
      PollQuestions("How many suns do we have in our solar system ?", None, None, 2, List(PollAnswers("One", None, None, 1))),
      PollQuestions("How many moons does earth have ?", None, None, 3, List(PollAnswers("One", None, None, 1)))
    ))
    val pollSpok = Spok("poll", None, None, None, None, None, None, None, Some(pollAttributes), None, geo, spokId)
    createSpok(user1Id, pollSpok)
    val spokVertexQuery = s"""g.V().hasLabel('users').has('userId','$user1Id').outE('isAssociatedWith').inV().has('spokId','$spokId').outE('containsA').inV()"""
    val spokVertex = DseGraphFactory.dseConn.executeGraph(spokVertexQuery).one().asVertex()
    insertPollWithQuestions(spokVertex, pollAttributes, timeStamp)
    linkSpokerFollowers(user1Id, None, spokId, geo, None, None, timeStamp, 3)
    val edge = DseGraphFactory.dseConn.executeGraph(getEdgeBetweenUserAndSpok(user2Id, spokId)).one().asEdge()

    val (response, error) = createRespok(spokId, user2Id, Respok(None, None, None, geo, None), Some(edge))
    assert(error.get === Error(SPK_131, s"Poll's questions have to be all answered before respoking spok $spokId."))
    assert(response.isEmpty)
  }

  it should "not be able to respok a poll spok if the user has answered some of the questions but not all" in {

    val spokId = getUUID()
    val pollAttributes = Poll("Quiz", None, List(
      PollQuestions("How many planets do we have in our solar system ?", None, None, 1, List(PollAnswers("Eight", None, None, 1))),
      PollQuestions("How many suns do we have in our solar system ?", None, None, 2, List(PollAnswers("One", None, None, 1))),
      PollQuestions("How many moons does earth have ?", None, None, 3, List(PollAnswers("One", None, None, 1)))
    ))
    val pollSpok = Spok("poll", None, None, None, None, None, None, None, Some(pollAttributes), None, geo, spokId)
    createSpok(user1Id, pollSpok)
    val spokVertexQuery = s"""g.V().hasLabel('users').has('userId','$user1Id').outE('isAssociatedWith').inV().has('spokId','$spokId').outE('containsA').inV()"""
    val spokVertex = DseGraphFactory.dseConn.executeGraph(spokVertexQuery).one().asVertex()
    insertPollWithQuestions(spokVertex, pollAttributes, timeStamp)
    linkSpokerFollowers(user1Id, None, spokId, geo, None, None, timeStamp, 3)

    val questionOneQuery = s"""g.V().hasLabel('spok').has('spokId','$spokId').outE().inV().hasLabel('poll').outE('hasAQuestion').inV().has('rank','1')"""
    val questionOneId = DseGraphFactory.dseConn.executeGraph(questionOneQuery).one().asVertex().getProperty("id").getValue.asString()
    val answerOneQuery =
      s"""g.V().hasLabel('spok').has('spokId','$spokId').outE().inV().hasLabel('poll').outE('hasAQuestion').inV()
             |.has('id','$questionOneId').outE('hasAnAnswer').inV().has('rank','1')""".stripMargin
    val answerOneId = DseGraphFactory.dseConn.executeGraph(answerOneQuery).one().asVertex().getProperty("id").getValue.asString()

    val questionTwoQuery = s"""g.V().hasLabel('spok').has('spokId','$spokId').outE().inV().hasLabel('poll').outE('hasAQuestion').inV().has('rank','2')"""
    val questionTwoId = DseGraphFactory.dseConn.executeGraph(questionTwoQuery).one().asVertex().getProperty("id").getValue.asString()
    val answerTwoQuery = s"""g.V().hasLabel('spok').has('spokId','$spokId').outE().inV().hasLabel('poll').outE('hasAQuestion').inV().has('id','$questionTwoId').outE('hasAnAnswer').inV().has('rank','1')"""
    val answerTwoId = DseGraphFactory.dseConn.executeGraph(answerTwoQuery).one().asVertex().getProperty("id").getValue.asString()

    addAnswerToAPoll(questionOneId, spokId, user2Id, UserPollAnswer(answerOneId, geo))
    addAnswerToAPoll(questionTwoId, spokId, user2Id, UserPollAnswer(answerTwoId, geo))
    val edge = DseGraphFactory.dseConn.executeGraph(getEdgeBetweenUserAndSpok(user2Id, spokId)).one().asEdge()

    val (response, error) = createRespok(spokId, user2Id, Respok(None, None, None, geo, None), Some(edge))
    assert(error.get === Error(SPK_131, s"Poll's questions have to be all answered before respoking spok $spokId."))
    assert(response.isEmpty)
  }

  it should "be able to respok a poll spok if the user has answered all the questions only" in {

    val spokId = getUUID()
    val pollAttributes = Poll("Quiz", None, List(
      PollQuestions("How many planets do we have in our solar system ?", None, None, 1, List(PollAnswers("Eight", None, None, 1))),
      PollQuestions("How many suns do we have in our solar system ?", None, None, 2, List(PollAnswers("One", None, None, 1))),
      PollQuestions("How many moons does earth have ?", None, None, 3, List(PollAnswers("One", None, None, 1)))
    ))
    val pollSpok = Spok("poll", None, None, None, None, None, None, None, Some(pollAttributes), None, geo, spokId)
    createSpok(user1Id, pollSpok)
    val spokVertexQuery = s"""g.V().hasLabel('users').has('userId','$user1Id').outE('isAssociatedWith').inV().has('spokId','$spokId').outE('containsA').inV()"""
    val spokVertex = DseGraphFactory.dseConn.executeGraph(spokVertexQuery).one().asVertex()
    insertPollWithQuestions(spokVertex, pollAttributes, timeStamp)
    linkSpokerFollowers(user1Id, None, spokId, geo, None, None, timeStamp, 3)

    val questionOneQuery = s"""g.V().hasLabel('spok').has('spokId','$spokId').outE().inV().hasLabel('poll').outE('hasAQuestion').inV().has('rank','1')"""
    val questionOneId = DseGraphFactory.dseConn.executeGraph(questionOneQuery).one().asVertex().getProperty("id").getValue.asString()
    val answerOneQuery =
      s"""g.V().hasLabel('spok').has('spokId','$spokId').outE().inV().hasLabel('poll').outE('hasAQuestion').inV()
            |.has('id','$questionOneId').outE('hasAnAnswer').inV().has('rank','1')""".stripMargin
    val answerOneId = DseGraphFactory.dseConn.executeGraph(answerOneQuery).one().asVertex().getProperty("id").getValue.asString()

    val questionTwoQuery = s"""g.V().hasLabel('spok').has('spokId','$spokId').outE().inV().hasLabel('poll').outE('hasAQuestion').inV().has('rank','2')"""
    val questionTwoId = DseGraphFactory.dseConn.executeGraph(questionTwoQuery).one().asVertex().getProperty("id").
      getValue.asString()
    val answerTwoQuery = s"""g.V().hasLabel('spok').has('spokId','$spokId').outE().inV().hasLabel('poll').outE('hasAQuestion').inV()
                               |.has('id','$questionTwoId').outE('hasAnAnswer').inV().has('rank','1')""".stripMargin
    val answerTwoId = DseGraphFactory.dseConn.executeGraph(answerTwoQuery).one().asVertex().getProperty("id").getValue.asString()

    val questionThreeQuery = s"""g.V().hasLabel('spok').has('spokId','$spokId').outE().inV().hasLabel('poll').outE('hasAQuestion').inV().has('rank','3')"""
    val questionThreeId = DseGraphFactory.dseConn.executeGraph(questionThreeQuery).one().
      asVertex().getProperty("id").getValue.asString()
    val answerThreeQuery = s"""g.V().hasLabel('spok').has('spokId','$spokId').outE().inV().hasLabel('poll').outE('hasAQuestion').inV().has('id','$questionThreeId').outE('hasAnAnswer').inV().has('rank','1')"""
    val answerThreeId = DseGraphFactory.dseConn.executeGraph(answerThreeQuery).one().asVertex().getProperty("id").getValue.asString()

    addAnswerToAPoll(questionOneId, spokId, user2Id, UserPollAnswer(answerOneId, geo))
    updatePendingQuestionsInEdge(user2Id, spokId)
    addAnswerToAPoll(questionTwoId, spokId, user2Id, UserPollAnswer(answerTwoId, geo))
    updatePendingQuestionsInEdge(user2Id, spokId)
    addAnswerToAPoll(questionThreeId, spokId, user2Id, UserPollAnswer(answerThreeId, geo))
    updatePendingQuestionsInEdge(user2Id, spokId)
    val edge = DseGraphFactory.dseConn.executeGraph(getEdgeBetweenUserAndSpok(user2Id, spokId)).one().asEdge()

    val (response, error) = createRespok(spokId, user2Id, Respok(None, None, None, geo, None), Some(edge))
    assert(error.isEmpty)
    assert(response.get.spokId equals spokId)
  }

  it should "be able to view a poll question and the previous question and next question also" in {

    val userId = getUUID()
    val spokId = getUUID()
    val pollAttributes = Poll("Quiz", None, List(
      PollQuestions("How many planets do we have in our solar system ?", None, None, 1, List(PollAnswers("Eight", None, None, 1))),
      PollQuestions("How many suns do we have in our solar system ?", None, None, 2, List(PollAnswers("One", None, None, 1))),
      PollQuestions("How many moons does earth have ?", None, None, 3, List(PollAnswers("One", None, None, 1)))
    ))
    val pollSpok = Spok("poll", None, None, None, None, None, None, None, Some(pollAttributes), None, geo, spokId)
    createSpok(user1Id, pollSpok)
    val spokVertexQuery = s"""g.V().hasLabel('users').has('userId','$user1Id').outE('isAssociatedWith').inV().has('spokId','$spokId').outE('containsA').inV()"""
    val spokVertex = DseGraphFactory.dseConn.executeGraph(spokVertexQuery).one().asVertex()
    insertPollWithQuestions(spokVertex, pollAttributes, timeStamp)
    linkSpokerFollowers(user1Id, None, spokId, geo, None, None, timeStamp, 3)
    val query = """g.V().hasLabel('poll').outE('hasAQuestion').inV().has('rank','2')"""
    val questionId = DseGraphFactory.dseConn.executeGraph(query).one().asVertex().getProperty("id").getValue.asString()
    val (result, error) = viewPollQuestion(questionId, userId)
    assert(result.get.current.text === "How many suns do we have in our solar system ?")
    assert(result.get.previous.get.text === "How many planets do we have in our solar system ?")
    assert(result.get.next.get.text === "How many moons does earth have ?")
  }

  it should "be able to unspok a spok when a user is connected in pending state with the spok " in {

    val spokId1 = getUUID()
    val geo = Geo(45.00, 45.00, 45.00)
    val spok: Spok = Spok("rawtext", None, Some("public"), Some(0), Some("This spok will be unspoked"), None, Some("Text"),
      None, None, None, geo, spokId1)
    createSpok(user1Id, spok)
    linkSpokerFollowers(user1Id, Some("0"), spokId1, geo, Some("public"), Some("This spok will be unspoked"), timeStamp, 0)
    val response = createUnspok(spokId1, user2Id, Unspok(geo), PENDING)
    assert(response.get.spokId equals spokId1)
  }

  it should "be able to validate and  Disable a  spok " in {
    val spokId = getUUID()
    val geo = Geo(27.22, 27.55, 22)
    val spok: Spok = Spok("rawtext", None, Some("public"), Some(1), Some("instanceText"), None, Some("Text"),
      Some(Url("Address", "Title", "text", "previewLink", Some("urlType"))), None, None,
      geo, spokId)
    createSpok(user1Id, spok)
    val spokNotFoundResult = disableSpok("firstspokid", user1Id, geo)
    assert(spokNotFoundResult == SPOK_NOT_FOUND)
    val invalidUserResult = disableSpok(spokId, "firstUserID", geo)
    assert(invalidUserResult == INVALID_USER)
    val spokDisableResult = disableSpok(spokId, user1Id, geo)
    assert(spokDisableResult == SPOK_DISABLED)
    val spokDisabledResult = disableSpok(spokId, user1Id, geo)
    assert(spokDisabledResult == DISABLED_SPOK, geo)
  }

  it should "be able to validate and  Remove a  spok from wall " in {
    val spokId = getUUID()
    val geo = Geo(27.22, 27.55, 22)
    val spok: Spok = Spok("String", None, Some("public"), Some(1), Some("This spok will be removed"), None, Some("first spok"), None, None, None,
      geo, spokId)
    createSpok(user1Id, spok)
    val launched = timeStamp
    linkSpokerFollowers(user1Id, Some("0"), spokId, geo, Some("public"), Some("This spok will be removed"), timeStamp, 0)
    val spokNotRemoveResult = removeSpokFromWall(spokId, user2Id, launched, geo)
    assert(spokNotRemoveResult == RemoveSpokResponse(None, Some(SPOK_STATUS_NOT_RESPOKED)))
    val spokRemoveResult = removeSpokFromWall(spokId, user1Id, launched, geo)
    assert(spokRemoveResult == RemoveSpokResponse(Some(spokId), None))
  }

  it should " not be able to link follower with spok if already linked with it " in {
    val spokId = getUUID()
    val spok1: Spok = Spok("rawtext", Some("0"), Some("public"), Some(1), Some("this is my first spok from cyril"), Some("first spok"),
      None, None, None, None, Geo(43.2805546, 5.2647101, 222), spokId)
    assert(createSpok(user1Id, spok1))
    val launched = timeStamp
    val res = linkSpokerFollowers(user1Id, Some("0"), spokId, Geo(43.2805546, 5.2647101, 222),
      Some("public"), Some("this is my first spok from cyril"), launched, 0)
    val edge = DseGraphFactory.dseConn.executeGraph(getEdgeBetweenUserAndSpok(user2Id, spokId)).one().asEdge()
    val edge1 = DseGraphFactory.dseConn.executeGraph(getEdgeBetweenUserAndSpok(user3Id, spokId)).one().asEdge()

    createRespok(spokId, user2Id, Respok(Some("0"), Some("public"), Some("I kais am respoking"), geo, None), Some(edge))
    createRespok(spokId, user3Id, Respok(Some("0"), Some("public"), Some("I vikas am respoking"), geo, None), Some(edge1))
    val getPendingCountQuery = s"""g.V().hasLabel('$DSE_SPOK').has('$SPOK_ID','$spokId').inE('$ISASSOCIATEDWITH').has('$STATUS','$PENDING').hasNext()"""
    assert(DseGraphFactory.dseConn.executeGraph(getPendingCountQuery).one().asBoolean())
  }

  it should " be able get spok stats " in {
    val spokId1 = getUUID()
    val spok: Spok = Spok("rawtext", None, Some("Public"), Some(1), Some("instanceText"), None, Some("Text"),
      None, None, None, Geo(27.22, 27.55, 222), spokId1)
    createSpok(user1Id, spok)
    val (pendingCount, unspokedCount, respokedCount, landedCount, commentCount) = calculateSpokStatsCount(spokId1)
    assert(pendingCount == 0)
    assert(unspokedCount == 0)
    assert(respokedCount == 1)
    assert(landedCount == 1)
  }

  it should "be able to answer all poll questions at once" in {

    val spokId = getUUID()
    val pollAttributes = Poll("Quiz", None, List(
      PollQuestions("How many planets do we have in our solar system ?", None, None, 1, List(PollAnswers("Eight", None, None, 1)))
    ))
    val pollSpok = Spok("poll", None, None, None, None, None, None, None, Some(pollAttributes), None, geo, spokId)
    createSpok(user1Id, pollSpok)
    val spokVertexQuery = s"""g.V().hasLabel('users').has('userId','$user1Id').outE('isAssociatedWith').inV().has('spokId','$spokId').outE('containsA').inV()"""
    val spokVertex = DseGraphFactory.dseConn.executeGraph(spokVertexQuery).one().asVertex()
    insertPollWithQuestions(spokVertex, pollAttributes, timeStamp)
    linkSpokerFollowers(user1Id, None, spokId, geo, None, None, timeStamp, 1)

    val questionOneQuery = s"""g.V().hasLabel('spok').has('spokId','$spokId').outE().inV().hasLabel('poll').outE('hasAQuestion').inV().has('rank','1')"""
    val questionOneId = DseGraphFactory.dseConn.executeGraph(questionOneQuery).one().asVertex().getProperty("id").getValue.asString()
    val answerOneQuery =
      s"""g.V().hasLabel('spok').has('spokId','$spokId').outE().inV().hasLabel('poll').outE('hasAQuestion').inV()
          |.has('id','$questionOneId').outE('hasAnAnswer').inV().has('rank','1')""".stripMargin
    val answerOneId = DseGraphFactory.dseConn.executeGraph(answerOneQuery).one().asVertex().getProperty("id").getValue.asString()

    val allAnswers = AllAnswers(spokId, List(OneAnswer(questionOneId, answerOneId)), geo)
    val (response, alreadyAnswered) = addAllAnswersToAPoll(user2Id, allAnswers)
    assert(response.isEmpty)
    assert(alreadyAnswered.isEmpty)
  }

  it should "not be able to answer all poll questions at once if questions do not belong to this spok" in {

    val spokId = getUUID()
    val randomQuestionId = getUUID()
    val randomAnswerId = getUUID()
    val pollAttributes = Poll("Quiz", None, List(
      PollQuestions("How many planets do we have in our solar system ?", None, None, 1, List(PollAnswers("Eight", None, None, 1)))
    ))
    val pollSpok = Spok("poll", None, None, None, None, None, None, None, Some(pollAttributes), None, geo, spokId)
    createSpok(user1Id, pollSpok)
    val spokVertexQuery = s"""g.V().hasLabel('users').has('userId','$user1Id').outE('isAssociatedWith').inV().has('spokId','$spokId').outE('containsA').inV()"""
    val spokVertex = DseGraphFactory.dseConn.executeGraph(spokVertexQuery).one().asVertex()
    insertPollWithQuestions(spokVertex, pollAttributes, timeStamp)
    linkSpokerFollowers(user1Id, None, spokId, geo, None, None, timeStamp, 1)
    val allAnswers = AllAnswers(spokId, List(OneAnswer(randomQuestionId, randomAnswerId)), geo)
    val (error, alreadyAnswered) = addAllAnswersToAPoll(user2Id, allAnswers)
    assert(error.get == Error(SPK_137, s"Not all questions are related to the poll spok ${allAnswers.spokId}"))
    assert(alreadyAnswered == List(randomQuestionId))
  }

  it should "not be able to answer all poll questions again if the poll is already complete" in {

    val spokId = getUUID()
    val pollAttributes = Poll("Quiz", None, List(
      PollQuestions("How many planets do we have in our solar system ?", None, None, 1, List(PollAnswers("Eight", None, None, 1)))
    ))
    val pollSpok = Spok("poll", None, None, None, None, None, None, None, Some(pollAttributes), None, geo, spokId)
    createSpok(user1Id, pollSpok)
    val spokVertexQuery = s"""g.V().hasLabel('users').has('userId','$user1Id').outE('isAssociatedWith').inV().has('spokId','$spokId').outE('containsA').inV()"""
    val spokVertex = DseGraphFactory.dseConn.executeGraph(spokVertexQuery).one().asVertex()
    insertPollWithQuestions(spokVertex, pollAttributes, timeStamp)
    linkSpokerFollowers(user1Id, None, spokId, geo, None, None, timeStamp, 1)

    val questionOneQuery = s"""g.V().hasLabel('spok').has('spokId','$spokId').outE().inV().hasLabel('poll').outE('hasAQuestion').inV().has('rank','1')"""
    val questionOneId = DseGraphFactory.dseConn.executeGraph(questionOneQuery).one().asVertex().getProperty("id").getValue.asString()
    val answerOneQuery =
      s"""g.V().hasLabel('spok').has('spokId','$spokId').outE().inV().hasLabel('poll').outE('hasAQuestion').inV()
          |.has('id','$questionOneId').outE('hasAnAnswer').inV().has('rank','1')""".stripMargin
    val answerOneId = DseGraphFactory.dseConn.executeGraph(answerOneQuery).one().asVertex().getProperty("id").getValue.asString()

    val allAnswers = AllAnswers(spokId, List(OneAnswer(questionOneId, answerOneId)), geo)
    addAllAnswersToAPoll(user2Id, allAnswers)
    updateFinishCountOfPoll(user2Id, spokId)
    val (error, alreadyAnswered) = addAllAnswersToAPoll(user2Id, allAnswers)
    assert(error.get == Error(SPK_135, s"Spok ${allAnswers.spokId} already completed."))
    assert(alreadyAnswered.isEmpty)
  }

  it should "not be able to answer all poll questions at once if questions and answers are not complete for the poll" in {

    val spokId = getUUID()
    val pollAttributes = Poll("Quiz", None, List(
      PollQuestions("How many planets do we have in our solar system ?", None, None, 1, List(PollAnswers("Eight", None, None, 1))),
      PollQuestions("How many suns do we have in our solar system ?", None, None, 2, List(PollAnswers("One", None, None, 1)))
    ))
    val pollSpok = Spok("poll", None, None, None, None, None, None, None, Some(pollAttributes), None, geo, spokId)
    createSpok(user1Id, pollSpok)
    val spokVertexQuery = s"""g.V().hasLabel('users').has('userId','$user1Id').outE('isAssociatedWith').inV().has('spokId','$spokId').outE('containsA').inV()"""
    val spokVertex = DseGraphFactory.dseConn.executeGraph(spokVertexQuery).one().asVertex()
    insertPollWithQuestions(spokVertex, pollAttributes, timeStamp)
    linkSpokerFollowers(user1Id, None, spokId, geo, None, None, timeStamp, 2)

    val questionOneQuery = s"""g.V().hasLabel('spok').has('spokId','$spokId').outE().inV().hasLabel('poll').outE('hasAQuestion').inV().has('rank','1')"""
    val questionOneId = DseGraphFactory.dseConn.executeGraph(questionOneQuery).one().asVertex().getProperty("id").getValue.asString()
    val answerOneQuery =
      s"""g.V().hasLabel('spok').has('spokId','$spokId').outE().inV().hasLabel('poll').outE('hasAQuestion').inV()
          |.has('id','$questionOneId').outE('hasAnAnswer').inV().has('rank','1')""".stripMargin
    val answerOneId = DseGraphFactory.dseConn.executeGraph(answerOneQuery).one().asVertex().getProperty("id").getValue.asString()

    val allAnswers = AllAnswers(spokId, List(OneAnswer(questionOneId, answerOneId)), geo)
    val (error, alreadyAnswered) = addAllAnswersToAPoll(user2Id, allAnswers)

    assert(error.get == Error(SPK_136, s"Not all answers for ${allAnswers.spokId}"))
    assert(alreadyAnswered.isEmpty)
  }

  it should "be able to answer all poll questions at once but also return a list of those questions which were being re-answered" in {

    val spokId = getUUID()
    val pollAttributes = Poll("Quiz", None, List(
      PollQuestions("How many planets do we have in our solar system ?", None, None, 1, List(PollAnswers("Eight", None, None, 1))),
      PollQuestions("How many suns do we have in our solar system ?", None, None, 2, List(PollAnswers("One", None, None, 1)))
    ))
    val pollSpok = Spok("poll", None, None, None, None, None, None, None, Some(pollAttributes), None, geo, spokId)
    createSpok(user1Id, pollSpok)
    val spokVertexQuery = s"""g.V().hasLabel('users').has('userId','$user1Id').outE('isAssociatedWith').inV().has('spokId','$spokId').outE('containsA').inV()"""
    val spokVertex = DseGraphFactory.dseConn.executeGraph(spokVertexQuery).one().asVertex()
    insertPollWithQuestions(spokVertex, pollAttributes, timeStamp)
    linkSpokerFollowers(user1Id, None, spokId, geo, None, None, timeStamp, 3)

    val questionOneQuery = s"""g.V().hasLabel('spok').has('spokId','$spokId').outE().inV().hasLabel('poll').outE('hasAQuestion').inV().has('rank','1')"""
    val questionOneId = DseGraphFactory.dseConn.executeGraph(questionOneQuery).one().asVertex().getProperty("id").getValue.asString()
    val answerOneQuery =
      s"""g.V().hasLabel('spok').has('spokId','$spokId').outE().inV().hasLabel('poll').outE('hasAQuestion').inV()
          |.has('id','$questionOneId').outE('hasAnAnswer').inV().has('rank','1')""".stripMargin
    val answerOneId = DseGraphFactory.dseConn.executeGraph(answerOneQuery).one().asVertex().getProperty("id").getValue.asString()

    val questionTwoQuery = s"""g.V().hasLabel('spok').has('spokId','$spokId').outE().inV().hasLabel('poll').outE('hasAQuestion').inV().has('rank','2')"""
    val questionTwoId = DseGraphFactory.dseConn.executeGraph(questionTwoQuery).one().asVertex().getProperty("id").
      getValue.asString()
    val answerTwoQuery = s"""g.V().hasLabel('spok').has('spokId','$spokId').outE().inV().hasLabel('poll').outE('hasAQuestion').inV()
                             |.has('id','$questionTwoId').outE('hasAnAnswer').inV().has('rank','1')""".stripMargin
    val answerTwoId = DseGraphFactory.dseConn.executeGraph(answerTwoQuery).one().asVertex().getProperty("id").getValue.asString()

    addAnswerToAPoll(questionOneId, spokId, user2Id, UserPollAnswer(answerOneId, geo))
    updatePendingQuestionsInEdge(user2Id, spokId)

    val allAnswers = AllAnswers(spokId, List(OneAnswer(questionOneId, answerOneId), OneAnswer(questionTwoId, answerTwoId)), geo)
    val (response, allAnswered) = addAllAnswersToAPoll(user2Id, allAnswers)
    assert(response.isEmpty)
    assert(allAnswered == List(questionOneId))
  }

}
