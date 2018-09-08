package com.spok.persistence.factory.spokgraph

import java.util.Date

import com.spok.model.Account.{ FollowUnfollow, User, UserMinimalDetailsResponse }
import com.spok.model.SpokModel.{ Spok, _ }
import com.spok.model._
import com.spok.persistence.dse.DseGraphFactory
import com.spok.persistence.factory.{ DSEGraphPersistenceFactoryApi, DSEUserSpokFactoryApi }
import com.spok.util.Constant._
import com.spok.util.RandomUtil
import org.scalatest.{ BeforeAndAfterAll, FlatSpec, Matchers }

class SpokViewApiSpec extends FlatSpec with Matchers with DSESpokViewApi with BeforeAndAfterAll with RandomUtil {

  val obj = DSEGraphPersistenceFactoryApi
  val dSEUserSpokFactoryApi = DSEUserSpokFactoryApi
  val dseSpokCommentApi: SpokCommentApi = SpokCommentApi
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
    obj.performFollow(FollowUnfollow("919582311067", user4Id, user2Id))
    obj.updateDefaultGroup(user4Id, user2Id, FOLLOWS)
    obj.performFollow(FollowUnfollow("919582311889", user5Id, user4Id))
    obj.updateDefaultGroup(user5Id, user4Id, FOLLOWS)
  }

  override def afterAll: Unit = {
    DseGraphFactory.dseConn.executeGraph("g.V().drop()")
  }

  behavior of "DSESpokViewApiSpec "

  it should "be able to get spok stats " in {
    val spokId1 = getUUID()
    val spok: Spok = Spok("rawtext", None, Some("public"), Some(1), Some("instanceText"), None, Some("Text"),
      Some(Url("Address", "Title", "text", "previewLink", Some("urlType"))), None, None,
      Geo(27.22, 27.55, 222), spokId1)
    dseSpokApi.createSpok(user1Id, spok)
    val result = getSpokStats(spokId1)
    assert(result == SpokStatistics(0.0, 1, 1, 0, 0, 0))
  }

  it should "be able to get 10 comments of a spok " in {
    val spokId1 = getUUID()
    val commentId1 = getUUID()
    val spok: Spok = Spok("rawtext", None, Some("public"), Some(1), Some("instanceText"), None, Some("Text"),
      Some(Url("Address", "Title", "text", "previewLink", Some("urlType"))), None, None,
      Geo(27.22, 27.55, 222), spokId1)
    dseSpokApi.createSpok(user1Id, spok)
    val (response, commentTimeStamp, spokError) = dseSpokCommentApi.addComment(spokId1, commentId1, user5Id,
      "First Comment", Geo(27.22, 27.55, 222), Nil)
    val timeStampToLong = commentTimeStamp.toLong
    val timeStampToDate = new Date(timeStampToLong)
    val result = getComments(spokId1, "1")
    val commentResponse = CommentsResponse("0", "", 1, List(Comments(commentId1, timeStampToDate,
      "First Comment", UserMinimalDetailsResponse(user5Id, "Ayush", "male", "testuser1.jpg"))))
    assert(result.get == commentResponse)
  }

  it should "be able to get 10 respokers of a spok " in {
    val spokId1 = getUUID()
    val spok: Spok = Spok("rawtext", None, Some("public"), Some(1), Some("instanceText"), None, Some("Text"),
      Some(Url("Address", "Title", "text", "previewLink", Some("urlType"))), None, None,
      Geo(27.22, 27.55, 222), spokId1)
    dseSpokApi.createSpok(user1Id, spok)
    val result = getReSpokers(spokId1, "1")
    val response = ReSpokerResponse("0", "", List(ReSpoker(user1Id, user1.nickname, user1.gender, user1.picture.get)))
    assert(result.get == response)
  }

  it should "be able to get 10 scoped user of a spok " in {
    val spokId1 = getUUID()
    val spok: Spok = Spok("rawtext", None, Some("public"), Some(1), Some("instanceText"), None, Some("Text"),
      Some(Url("Address", "Title", "text", "previewLink", Some("urlType"))), None, None,
      Geo(27.22, 27.55, 222), spokId1)
    dseSpokApi.createSpok(user4Id, spok)
    dseSpokApi.linkSpokerFollowers(user4Id, spok.groupId, spok.spokId, spok.geo, spok.visibility, spok.headerText, spok.launched, 0)
    val result = getScopedUsers(spokId1, "1")
    val response = ScopedUsersResponse("0", "", List(ScopedUsers(user5Id, user5.nickname, user5.gender, user5.picture.get)))
    assert(result.get == response)
  }

  it should "be able to get user spok stack " in {
    val spokId1 = getUUID()
    val spokId2 = getUUID()
    val spok1: Spok = Spok("rawtext", None, Some("public"), Some(1), Some("instanceText"), None, Some("Text"),
      Some(Url("Address", "Title", "text", "previewLink", Some("urlType"))), None, None,
      Geo(27.22, 27.55, 222), spokId1)
    dseSpokApi.createSpok(user1Id, spok1)
    dseSpokApi.linkSpokerFollowers(user1Id, spok1.groupId, spok1.spokId, spok1.geo, spok1.visibility, spok1.headerText, spok1.launched, 0)
    val spok2: Spok = Spok("rawtext", None, Some("public"), Some(1), Some("instanceText"), None, Some("Text"),
      Some(Url("Address", "Title", "text", "previewLink", Some("urlType"))), None, None,
      Geo(27.22, 27.55, 222), spokId2)
    dseSpokApi.createSpok(user1Id, spok2)
    dseSpokApi.linkSpokerFollowers(user1Id, spok2.groupId, spok2.spokId, spok2.geo, spok2.visibility, spok2.headerText, spok2.launched, 0)
    val spokObj = getSpokStack(user2Id, "1").get
    assert(spokObj.previous.equals("0"))
    assert(spokObj.spoks.size > 1)
  }

  it should "be able to get view short spok for rawtext content type" in {
    val spokId1 = getUUID()
    val launched = new Date()
    val spok: Spok = Spok("rawtext", None, Some("public"), Some(1), Some("instanceText"), None, Some("Text"),
      None, None, None, Geo(27.22, 27.55, 222), spokId1, launched.getTime)
    dseSpokApi.createSpok(user1Id, spok)
    dseSpokApi.linkSpokerFollowers(user1Id, spok.groupId, spok.spokId, spok.geo, spok.visibility, spok.headerText, spok.launched, 0)
    dseSpokApi.updateStats(spokId1, user1Id, spok.groupId, spok.geo)
    val spokVertex = DseGraphFactory.dseConn.executeGraph("g.V().hasLabel('spok').has('spokId','" + spokId1 + "')").one().asVertex()
    val result = viewShortSpok(spokId1, user1Id, "", spokVertex)
    val response = ViewSpok(spokId1, "rawtext", 1, launched, "instanceText", Some(launched), "instanceText", false, Spoker(user1Id, "Cyril", "male", "testuser.jpg"),
      Spoker(user1Id, "Cyril", "male", "testuser.jpg"), "public", Counters(1, 3, 0, 3238962.9), Content(rawText = Some("Text")))
    assert(result.get == response)
  }

  it should "be able to get view short spok for url content type" in {
    val spokId1 = getUUID()
    val launched = new Date()
    val url = Url("Address", "Title", "text", "previewLink", Some("urlType"))
    val spok: Spok = Spok("url", None, Some("public"), Some(1), Some("instanceText"), None, None,
      Some(url), None, None, Geo(27.22, 27.55, 222), spokId1, launched.getTime)
    dseSpokApi.createSpok(user1Id, spok)
    dseSpokApi.linkSpokerFollowers(user1Id, spok.groupId, spok.spokId, spok.geo, spok.visibility, spok.headerText, spok.launched, 0)
    dseSpokApi.updateStats(spokId1, user1Id, spok.groupId, spok.geo)
    val spokVertex = DseGraphFactory.dseConn.executeGraph("g.V().hasLabel('spok').has('spokId','" + spokId1 + "')").one().asVertex()

    val result = viewShortSpok(spokId1, user1Id, "", spokVertex)
    val response = ViewSpok(spokId1, "url", 1, launched, "instanceText", Some(launched), "instanceText", false, Spoker(user1Id, "Cyril", "male", "testuser.jpg"),
      Spoker(user1Id, "Cyril", "male", "testuser.jpg"), "public", Counters(1, 3, 0, 3238962.9), Content(url = Some(url.address), urlPreview = Some(url.preview), urlType = url.urlType, urlTitle = Some(url.title), urlText = Some(url.text)))
    assert(result.get == response)
  }

  it should "be able to get view short spok for poll content type" in {
    val spokId1 = getUUID()
    val launched = new Date()
    val poll = Poll("Survey", Some("Please fill it"), List(PollQuestions("Is Wine good for health", Some("poll"),
      None, 1, List(PollAnswers("Yes", None, None, 1)))))
    val spok: Spok = Spok("poll", None, Some("public"), Some(1), Some("instanceText"), None, None,
      None, Some(poll), None, Geo(27.22, 27.55, 222), spokId1, launched.getTime)
    dseSpokApi.createSpok(user1Id, spok)
    dseSpokApi.linkSpokerFollowers(user1Id, spok.groupId, spok.spokId, spok.geo, spok.visibility, spok.headerText, spok.launched, 0)
    dseSpokApi.updateStats(spokId1, user1Id, spok.groupId, spok.geo)
    val spokVertex = DseGraphFactory.dseConn.executeGraph("g.V().hasLabel('spok').has('spokId','" + spokId1 + "')").one().asVertex()
    val result = viewShortSpok(spokId1, user1Id, "", spokVertex)

    val content = result.get.content
    val response = ViewSpok(spokId1, "poll", 1, launched, "instanceText", Some(launched), "instanceText", false, Spoker(user1Id, "Cyril", "male", "testuser.jpg"),
      Spoker(user1Id, "Cyril", "male", "testuser.jpg"), "public", result.get.counters, content)

    assert(result.get == response)
  }

  it should "be able to get view short spok for riddle content type" in {
    val spokId1 = getUUID()
    val launched = new Date()
    val riddle = Riddle("riddle", RiddleQuestion("What is Pythagoras theorem", Some("riddle"), None),
      RiddleAnswer("b2+p2=h2", Some("riddle"), None))
    val spok: Spok = Spok("riddle", None, Some("public"), Some(1), Some("instanceText"), None, None,
      None, None, Some(riddle), Geo(27.22, 27.55, 222), spokId1, launched.getTime)
    dseSpokApi.createSpok(user1Id, spok)
    dseSpokApi.linkSpokerFollowers(user1Id, spok.groupId, spok.spokId, spok.geo, spok.visibility, spok.headerText, spok.launched, 0)
    dseSpokApi.updateStats(spokId1, user1Id, spok.groupId, spok.geo)
    val spokVertex = DseGraphFactory.dseConn.executeGraph("g.V().hasLabel('spok').has('spokId','" + spokId1 + "')").one().asVertex()
    val result = viewShortSpok(spokId1, user1Id, "", spokVertex)
    val response = ViewSpok(spokId1, "riddle", 1, launched, "instanceText", Some(launched), "instanceText", false, Spoker(user1Id, "Cyril", "male", "testuser.jpg"),
      Spoker(user1Id, "Cyril", "male", "testuser.jpg"), "public", Counters(1, 3, 0, 3238962.9), Content(riddleTitle = Some(riddle.title), riddleQuestion = Some(riddle.question.text), riddleAnswer = Some(riddle.answer.text)))
    assert(result.get == response)
  }

  it should "be able to get view short spok for picture content type" in {
    val spokId1 = getUUID()
    val launched = new Date()
    val spok: Spok = Spok("picture", None, Some("public"), Some(1), Some("instanceText"), Some("picture url"), None,
      None, None, None, Geo(27.22, 27.55, 222), spokId1, launched.getTime)
    dseSpokApi.createSpok(user1Id, spok)
    dseSpokApi.linkSpokerFollowers(user1Id, spok.groupId, spok.spokId, spok.geo, spok.visibility, spok.headerText, spok.launched, 0)
    dseSpokApi.updateStats(spokId1, user1Id, spok.groupId, spok.geo)
    val spokVertex = DseGraphFactory.dseConn.executeGraph("g.V().hasLabel('spok').has('spokId','" + spokId1 + "')").one().asVertex()
    val result = viewShortSpok(spokId1, user1Id, "", spokVertex)
    val response = ViewSpok(spokId1, "picture", 1, launched, "instanceText", Some(launched), "instanceText", false, Spoker(user1Id, "Cyril", "male", "testuser.jpg"),
      Spoker(user1Id, "Cyril", "male", "testuser.jpg"), "public", Counters(1, 3, 0, 3238962.9), Content(picturePreview = spok.file, pictureFull = spok.file))
    assert(result.get == response)
  }

  it should "be able to get view short spok for animated gif content type" in {
    val spokId1 = getUUID()
    val launched = new Date()
    val spok: Spok = Spok("animatedgif", None, Some("public"), Some(1), Some("instanceText"), Some("animated gif url"), None,
      None, None, None, Geo(27.22, 27.55, 222), spokId1, launched.getTime)
    dseSpokApi.createSpok(user1Id, spok)
    dseSpokApi.linkSpokerFollowers(user1Id, spok.groupId, spok.spokId, spok.geo, spok.visibility, spok.headerText, spok.launched, 0)
    dseSpokApi.updateStats(spokId1, user1Id, spok.groupId, spok.geo)
    val spokVertex = DseGraphFactory.dseConn.executeGraph("g.V().hasLabel('spok').has('spokId','" + spokId1 + "')").one().asVertex()
    val result = viewShortSpok(spokId1, user1Id, "", spokVertex)
    val response = ViewSpok(spokId1, "animatedgif", 1, launched, "instanceText", Some(launched), "instanceText", false, Spoker(user1Id, "Cyril", "male", "testuser.jpg"),
      Spoker(user1Id, "Cyril", "male", "testuser.jpg"), "public", Counters(1, 3, 0, 3238962.9), Content(animatedGif = spok.file))
    assert(result.get == response)
  }

  it should "be able to get view short spok for video content type" in {
    val spokId1 = getUUID()
    val launched = new Date()
    val spok: Spok = Spok("video", None, Some("public"), Some(1), Some("instanceText"), Some("video url"), None,
      None, None, None, Geo(27.22, 27.55, 222), spokId1, launched.getTime)
    dseSpokApi.createSpok(user1Id, spok)
    dseSpokApi.linkSpokerFollowers(user1Id, spok.groupId, spok.spokId, spok.geo, spok.visibility, spok.headerText, spok.launched, 0)
    dseSpokApi.updateStats(spokId1, user1Id, spok.groupId, spok.geo)
    val spokVertex = DseGraphFactory.dseConn.executeGraph("g.V().hasLabel('spok').has('spokId','" + spokId1 + "')").one().asVertex()
    val result = viewShortSpok(spokId1, user1Id, "", spokVertex)
    val response = ViewSpok(spokId1, "video", 1, launched, "instanceText", Some(launched), "instanceText", false, Spoker(user1Id, "Cyril", "male", "testuser.jpg"),
      Spoker(user1Id, "Cyril", "male", "testuser.jpg"), "public", Counters(1, 3, 0, 3238962.9), Content(videoPreview = spok.file, video = spok.file))
    assert(result.get == response)
  }

  it should "be able to get view short spok for sound content type" in {
    val spokId1 = getUUID()
    val launched = new Date()
    val spok: Spok = Spok("sound", None, Some("public"), Some(1), Some("instanceText"), Some("sound url"), None,
      None, None, None, Geo(27.22, 27.55, 222), spokId1, launched.getTime)
    dseSpokApi.createSpok(user1Id, spok)
    dseSpokApi.linkSpokerFollowers(user1Id, spok.groupId, spok.spokId, spok.geo, spok.visibility, spok.headerText, spok.launched, 0)
    dseSpokApi.updateStats(spokId1, user1Id, spok.groupId, spok.geo)
    val spokVertex = DseGraphFactory.dseConn.executeGraph("g.V().hasLabel('spok').has('spokId','" + spokId1 + "')").one().asVertex()
    val result = viewShortSpok(spokId1, user1Id, "", spokVertex)
    val soundPreviewUrl = result.get.content.soundPreview
    val response = ViewSpok(spokId1, "sound", 1, launched, "instanceText", Some(launched), "instanceText", false, Spoker(user1Id, "Cyril", "male", "testuser.jpg"),
      Spoker(user1Id, "Cyril", "male", "testuser.jpg"), "public", Counters(1, 3, 0, 3238962.9), Content(soundPreview = soundPreviewUrl, sound = spok.file))
    assert(result.get == response)
  }

  it should "be able to get view short spok if target user id is empty" in {
    val spokId1 = getUUID()
    val launched = new Date()
    val spok: Spok = Spok("sound", None, Some("public"), Some(1), Some("instanceText"), Some("sound url"), None,
      None, None, None, Geo(27.22, 27.55, 222), spokId1, launched.getTime)
    dseSpokApi.createSpok(user1Id, spok)
    dseSpokApi.linkSpokerFollowers(user1Id, spok.groupId, spok.spokId, spok.geo, spok.visibility, spok.headerText, spok.launched, 0)
    dseSpokApi.updateStats(spokId1, user1Id, spok.groupId, spok.geo)
    val spokVertex = DseGraphFactory.dseConn.executeGraph("g.V().hasLabel('spok').has('spokId','" + spokId1 + "')").one().asVertex()
    val result = viewShortSpok(spokId1, "", "", spokVertex)
    val soundPreviewUrl = result.get.content.soundPreview
    val response = ViewSpok(spokId1, "sound", 1, launched, "instanceText", None, "", false, Spoker(user1Id, user1.nickname, user1.gender, user1.picture.getOrElse("")), Spoker("", "", "", ""),
      "", Counters(1, 3, 0, 3238962.9), Content(soundPreview = soundPreviewUrl, sound = spok.file))
    assert(result.get == response)
  }

  it should "be able to get spoker's wall " in {
    val spokId1 = getUUID()
    val spokId2 = getUUID()
    val userId = getUUID()
    val spok1: Spok = Spok("rawtext", None, Some("public"), Some(1), Some("instanceText"), None, Some("Text"),
      Some(Url("Address", "Title", "text", "previewLink", Some("urlType"))), None, None,
      Geo(27.22, 27.55, 222), spokId1)
    dseSpokApi.createSpok(user1Id, spok1)
    val spok2: Spok = Spok("rawtext", None, Some("public"), Some(1), Some("instanceText"), None, Some("Text"),
      Some(Url("Address", "Title", "text", "previewLink", Some("urlType"))), None, None,
      Geo(27.22, 27.55, 222), spokId2)
    dseSpokApi.createSpok(user1Id, spok2)

    val spokObj = getSpokersWallDetails(user1Id, "1", userId).get
    assert(spokObj.previous.equals("0"))
    assert(spokObj.spoks.size > 1)
  }

  it should "be able to get view full spok" in {
    val spokId1 = getUUID()
    val launched = new Date()
    val commentId1 = getUUID()
    val spok: Spok = Spok("rawtext", None, Some("public"), Some(1), Some("instanceText"), None, Some("Text"),
      None, None, None, Geo(27.22, 27.55, 222), spokId1, launched.getTime)

    //create spok
    dseSpokApi.createSpok(user1Id, spok)
    dseSpokApi.linkSpokerFollowers(user1Id, spok.groupId, spok.spokId, spok.geo, spok.visibility, spok.headerText, spok.launched, 0)
    dseSpokApi.updateStats(spokId1, user1Id, spok.groupId, spok.geo)
    //respok
    val edge = DseGraphFactory.dseConn.executeGraph(getEdgeBetweenUserAndSpok(user2Id, spokId1)).one().asEdge()
    dseSpokApi.createRespok(spokId1, user2Id, Respok(Some("0"), Some("public"), Some("I am trying respoking"), geo, None), Some(edge))
    dseSpokApi.updateStats(spokId1, user2Id, spok.groupId, spok.geo)
    //comment
    dseSpokCommentApi.addComment(spokId1, commentId1, user5Id, "First Comment", Geo(27.22, 27.55, 222), Nil)
    dseSpokApi.updateStatsAfterAddComment(spokId1)

    val reSpokers = reSpokersDetails(spokId1, 0, 10)
    val scopedUsers = scopedUsersDetails(spokId1, 0, 10)
    val comments = commentDetailsForFullSpok(spokId1, 0, 10)
    val spokVertex = DseGraphFactory.dseConn.executeGraph("g.V().hasLabel('spok').has('spokId','" + spokId1 + "')").one().asVertex();

    val result = viewFullSpok(spokId1, user1Id, "", spokVertex)
    val response = ViewFullSpok(spokId1, "rawtext", 1, launched, "instanceText", Some(launched), "instanceText", false,
      Spoker(user1Id, "Cyril", "male", "testuser.jpg"), Spoker(user1Id, "Cyril", "male", "testuser.jpg"), "public",
      result.get.counters, reSpokers, scopedUsers, comments, Content(rawText = Some("Text")))
    assert(result.get == response)
  }

  it should "be not able to get view full poll spok stats for the creator" in {
    val spokId = getUUID()
    val launched = new Date()
    val poll = Poll("Survey", Some("Please fill it"), List(PollQuestions("Is Wine good for health", Some("text"),
      None, 1, List(PollAnswers("Yes", None, None, 1)))))
    val spok: Spok = Spok("poll", None, Some("public"), Some(1), Some("Lets check poll"), None, None,
      None, Some(poll), None, Geo(27.22, 27.55, 222), spokId, launched.getTime)
    //create spok
    dseSpokApi.createSpok(user1Id, spok)
    dseSpokApi.linkSpokerFollowers(user1Id, spok.groupId, spok.spokId, spok.geo, spok.visibility, spok.headerText, spok.launched, 0)
    val pollVertex = DseGraphFactory.dseConn.executeGraph(s"g.V().hasLabel('spok').has('spokId','$spokId').outE('containsA').inV()").one().asVertex()
    dseSpokApi.insertPollWithQuestions(pollVertex, Poll(poll.title, poll.desc, poll.questions), timeStamp)
    val pollId = pollVertex.getProperty(ID).getValue.asString()
    val questionId = DseGraphFactory.dseConn.executeGraph(s"g.V().hasLabel('poll').has('id','$pollId').outE('hasAQuestion').inV()")
      .one().asVertex().getProperty(ID).getValue.asString()
    val answerId = DseGraphFactory.dseConn.executeGraph(s"g.V().hasLabel('question').has('id','$questionId').outE('hasAnAnswer').inV()")
      .one().asVertex().getProperty(ID).getValue.asString()
    val pollStats: PollStats = PollStats(pollId, Some("Lets check poll"), Some("Please fill it"), 0, List(
      PollQuestionsStats(questionId, "Is Wine good for health", List(PollAnswerStats(answerId, "Yes", 0)))
    ))
    val (result, error) = viewPollStats(spokId, user1Id)
    assert(!(result.isDefined))
  }

  it should "be able to get view full poll spok stats for the spoker who has completed the poll" in {
    val spokId = getUUID()
    val launched = new Date()
    val geo = Geo(27.22, 27.55, 22)
    val poll = Poll("Survey", Some("Please fill it"), List(PollQuestions("Is Wine good for health", Some("text"),
      None, 1, List(PollAnswers("Yes", None, None, 1)))))
    val spok: Spok = Spok("poll", None, Some("public"), Some(1), Some("Lets check poll"), None, None,
      None, Some(poll), None, geo, spokId, launched.getTime)
    //create spok
    dseSpokApi.createSpok(user1Id, spok)
    val pollVertex = DseGraphFactory.dseConn.executeGraph(
      s"""g.V().hasLabel('users').has('userId','$user1Id')
         |.outE('isAssociatedWith').inV().has('spokId','$spokId').outE('containsA').inV()""".stripMargin
    ).one().asVertex()
    dseSpokApi.insertPollWithQuestions(pollVertex, Poll(poll.title, poll.desc, poll.questions), timeStamp)
    dseSpokApi.linkSpokerFollowers(user1Id, spok.groupId, spok.spokId, spok.geo, spok.visibility, spok.headerText, spok.launched, 1)
    val pollId = pollVertex.getProperty(ID).getValue.asString()
    val questionId = DseGraphFactory.dseConn.executeGraph(s"g.V().hasLabel('poll').has('id','$pollId').outE('hasAQuestion').inV()")
      .one().asVertex().getProperty(ID).getValue.asString()
    val answerId = DseGraphFactory.dseConn.executeGraph(s"g.V().hasLabel('question').has('id','$questionId').outE('hasAnAnswer').inV()")
      .one().asVertex().getProperty(ID).getValue.asString()
    dseSpokApi.addAnswerToAPoll(questionId, spokId, user2Id, UserPollAnswer(answerId, geo))
    dseSpokApi.updatePendingQuestionsInEdge(user2Id, spokId)
    val pollStats: PollStats = PollStats(pollId, Some("Lets check poll"), Some("Please fill it"), 1, List(
      PollQuestionsStats(questionId, "Is Wine good for health", List(PollAnswerStats(answerId, "Yes", 1)))
    ))
    val (result, error) = viewPollStats(spokId, user2Id)
    assert(result.get == pollStats)
    assert(error.isEmpty)
  }

  it should "be not able to get view full poll spok stats for the spoker who has not completed the poll" in {
    val spokId = getUUID()
    val launched = new Date()
    val geo = Geo(27.22, 27.55, 22)
    val poll = Poll("Survey", Some("Please fill it"), List(PollQuestions("Is Wine good for health", Some("text"),
      None, 1, List(PollAnswers("Yes", None, None, 1)))))
    val spok: Spok = Spok("poll", None, Some("public"), Some(1), Some("Lets check poll"), None, None,
      None, Some(poll), None, geo, spokId, launched.getTime)
    //create spok
    dseSpokApi.createSpok(user1Id, spok)
    val pollVertex = DseGraphFactory.dseConn.executeGraph(
      s"""g.V().hasLabel('users').has('userId','$user1Id')
          |.outE('isAssociatedWith').inV().has('spokId','$spokId').outE('containsA').inV()""".stripMargin
    ).one().asVertex()
    dseSpokApi.insertPollWithQuestions(pollVertex, Poll(poll.title, poll.desc, poll.questions), timeStamp)
    dseSpokApi.linkSpokerFollowers(user1Id, spok.groupId, spok.spokId, spok.geo, spok.visibility, spok.headerText, spok.launched, 1)
    val (result, error) = viewPollStats(spokId, user2Id)
    assert(result.isEmpty)
    assert(error.get == Error(SPK_014, CANNOT_VIEW_SPOK_STATS))
  }

  it should "be not able to get view full poll spok stats for the spoker who is not connected to the spok" in {
    val spokId = getUUID()
    val launched = new Date()
    val geo = Geo(27.22, 27.55, 22)
    val poll = Poll("Survey", Some("Please fill it"), List(PollQuestions("Is Wine good for health", Some("text"),
      None, 1, List(PollAnswers("Yes", None, None, 1)))))
    val spok: Spok = Spok("poll", None, Some("public"), Some(1), Some("Lets check poll"), None, None,
      None, Some(poll), None, geo, spokId, launched.getTime)
    //create spok
    dseSpokApi.createSpok(user1Id, spok)
    val pollVertex = DseGraphFactory.dseConn.executeGraph(
      s"""g.V().hasLabel('users').has('userId','$user1Id')
          |.outE('isAssociatedWith').inV().has('spokId','$spokId').outE('containsA').inV()""".stripMargin
    ).one().asVertex()
    dseSpokApi.insertPollWithQuestions(pollVertex, Poll(poll.title, poll.desc, poll.questions), timeStamp)
    dseSpokApi.linkSpokerFollowers(user1Id, spok.groupId, spok.spokId, spok.geo, spok.visibility, spok.headerText, spok.launched, 1)
    val (result, error) = viewPollStats(spokId, user5Id)
    assert(result.isEmpty)
    assert(error.get == Error(SPK_101, s"Spok $spokId not found."))
  }

}
