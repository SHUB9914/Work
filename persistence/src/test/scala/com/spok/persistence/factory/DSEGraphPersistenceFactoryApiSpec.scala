package com.spok.persistence.factory

import java.util.{ Date, UUID }

import com.datastax.driver.dse.graph.{ GraphNode, Vertex }
import com.spok.model.Account._
import com.spok.model.SpokModel.{ Spok, _ }
import com.spok.model._
import com.spok.persistence.dse.DseGraphFactory
import com.spok.persistence.factory.spokgraph.DSESpokApi
import com.spok.util.Constant._
import org.joda.time.DateTime
import org.scalatest.{ BeforeAndAfterAll, FlatSpec, Matchers }

class DSEGraphPersistenceFactoryApiSpec extends FlatSpec with Matchers with DSEGraphPersistenceFactoryApi with BeforeAndAfterAll {

  val dseSpokApi = DSESpokApi
  val spokId1 = getUUID()
  val spok: Spok = Spok("String", None, Some("Public"), Some(1), Some("instanceText"), None, Some("Text"),
    Some(Url("Address", "Title", "text", "previewLink", Some("urlType"))), None, None,
    Geo(27.22, 27.55, 222), spokId1)

  val spokId2 = getUUID()
  val spokWithoutUrl: Spok = Spok("String", Some("456"), Some("Private"), Some(1), Some("instanceText"), None, Some("Text"),
    None, None, None, Geo(77.22, 77.22, 222), spokId2)

  val date: Date = new java.util.Date()

  val userAttributesId = getUUID()
  val userAttributes = User("piyush", date, Location(List(LocationDetails(
    List(AddressComponents("Noida", "Noida", List("locality", "political"))),
    " Noida, Uttar Pradesh 201301, India",
    Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(27.22, 27.55), "APPROXIMATE",
      ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
  )), "OK"),
    "male", List("+919582311059", "+919582611051"), "+919983899777", userAttributesId, Some("piyush.jpg"), None, "Ahmedabad, India")

  val userAttributesMobileTestId = getUUID()
  val userAttributesMobileTest = User("Prashant", date, Location(List(LocationDetails(
    List(AddressComponents("Noida", "Noida", List("locality", "political"))),
    " Noida, Uttar Pradesh 201301, India",
    Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(27.22, 27.55), "APPROXIMATE",
      ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
  )), "OK"),
    "male", List("+919582311059", "+919983899777"), "+919876543210", userAttributesMobileTestId, Some("Prashant.jpg"))

  val userAttributesTestId = getUUID()
  val userAttributesTest = User("sonu", date, Location(List(LocationDetails(
    List(AddressComponents("Noida", "Noida", List("locality", "political"))),
    " Noida, Uttar Pradesh 201301, India",
    Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(28.6363011, 77.5025632), "APPROXIMATE",
      ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
  )), "OK"),
    "male", List("+919983899777", "+919582611051", "+91851003849"), "+919582311059", userAttributesTestId, Some("sonu.jpg"))

  val userWithNoContactsId = getUUID()
  val userWithNoContacts = User("userNocontacts", date, Location(List(LocationDetails(
    List(AddressComponents("Noida", "Noida", List("locality", "political"))),
    " Noida, Uttar Pradesh 201301, India",
    Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(28.6363011, 77.5025632), "APPROXIMATE",
      ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
  )), "OK"),
    "male", List(), "+919582311059", userWithNoContactsId, None)

  val user1Id = getUUID()
  val user1 = User("Ram", date, Location(List(LocationDetails(
    List(AddressComponents("Marsiele", "Marsiele", List("locality", "political"))),
    " Noida, Uttar Pradesh 201301, India",
    Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(43.280, 5.26), "APPROXIMATE",
      ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
  )), "OK"),
    "male", List("+919582611066"), "+919638527400", user1Id, Some("testuser.jpg"))

  val user2Id = getUUID()
  val user2 = User("Shyam", date, Location(List(LocationDetails(
    List(AddressComponents("Paris", "Paris", List("locality", "political"))),
    " Noida, Uttar Pradesh 201301, India",
    Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(48.85, 2.277), "APPROXIMATE",
      ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
  )), "OK"),
    "male", List("+919582611066"), "+919582311077", user2Id, Some("testuser1.jpg"))

  val user3Id = getUUID()
  val user3 = User("Narayan", date, Location(List(LocationDetails(
    List(AddressComponents("Noida", "Noida", List("locality", "political"))),
    " Noida, Uttar Pradesh 201301, India",
    Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(-2.22, -27.55), "APPROXIMATE",
      ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
  )), "OK"),
    "male", List(), "+919582611066", user3Id, Some("testuser1.jpg"))

  val user4Id = getUUID()
  val user4 = User("Suresh", date, Location(List(LocationDetails(
    List(AddressComponents("Noida", "Noida", List("locality", "political"))),
    " Noida, Uttar Pradesh 201301, India",
    Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(21.22, 47.55), "APPROXIMATE",
      ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
  )), "OK"),
    "male", List(), "+919582611077", user4Id, Some("testuser1.jpg"))

  val user5Id = getUUID()
  val user5 = User("mahesh", date, Location(List(LocationDetails(
    List(AddressComponents("Noida", "Noida", List("locality", "political"))),
    " Noida, Uttar Pradesh 201301, India",
    Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(21.22, 47.55), "APPROXIMATE",
      ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
  )), "OK"),
    "male", List(), "+919582611033", user5Id, Some("testuser1.jpg"))

  val kaisId = getUUID()
  val vincentId = getUUID()
  val cyrilId = getUUID()
  val ayushId = getUUID()
  val vikasId = getUUID()

  override def beforeAll {

    val user1 = User("Cyril", date, Location(List(LocationDetails(
      List(AddressComponents("Marsiele", "Marsiele", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(43.2805546, 5.2647101), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+919582311059", "+919582611051", "+919999999999", "+919983899777", "+919983899345"), "+919638527401", cyrilId, Some("testuser.jpg"), None, "india")

    val user2 = User("Kais", date, Location(List(LocationDetails(
      List(AddressComponents("Paris", "Paris", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(48.8589506, 2.2773452), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+919582311067"), "+919582311059", kaisId, Some("testuser1.jpg"))

    val user3 = User("Vikas", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(27.22, 27.55), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List(), "+919582611051", vikasId, Some("testuser1.jpg"))

    val user4 = User("Vincent", date, Location(List(LocationDetails(
      List(AddressComponents("Tihati", "Tihati", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(-17.6871718, -149.5132954), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+919582311889"), "+919582311067", vincentId, Some("testuser1.jpg"))

    val user5 = User("Ayush", date, Location(List(LocationDetails(
      List(AddressComponents("Bombay", "Bombay", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(19.0821975, 72.7407731), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List(), "+919582311889", ayushId, Some("testuser1.jpg"))

    val location = Location(List(LocationDetails(
      List(AddressComponents("London", "London", List("locality", "political"))),
      "11 Downing Street, United Kingdom",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(43.2805546, 5.2647101), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK")

    insertUser(user1)
    createUserSetting(user1.userId)
    insertUser(user2)
    createUserSetting(user2.userId)
    insertUser(user3)
    createUserSetting(user3.userId)
    insertUser(user4)
    createUserSetting(user4.userId)
    insertUser(user5)
    createUserSetting(user5.userId)
    performFollow(FollowUnfollow("919582311059", vikasId, cyrilId))
    updateDefaultGroup(vikasId, cyrilId, FOLLOWS)
    performFollow(FollowUnfollow("919582611051", kaisId, cyrilId))
    updateDefaultGroup(kaisId, cyrilId, FOLLOWS)
    performFollow(FollowUnfollow("919582611051", vincentId, kaisId))
    updateDefaultGroup(vincentId, kaisId, FOLLOWS)
    performFollow(FollowUnfollow("919582311067", vincentId, vikasId))
    updateDefaultGroup(vincentId, vikasId, FOLLOWS)
    performFollow(FollowUnfollow("919582311889", ayushId, vincentId))
    updateDefaultGroup(ayushId, vincentId, FOLLOWS)
  }

  override def afterAll: Unit = {
    DseGraphFactory.dseConn.executeGraph("g.V().drop()")
  }

  behavior of "DSEGraphPersistenceFactoryApi "

   it should "be able to get details of logged user" in {
    val (result, error, userV) = fetchMyProfile(cyrilId)
    assert(error.isEmpty)
    assert(result.get.id == cyrilId)
  }

  it should "not be able to get details of logged user" in {
    val (result, error, userV) = fetchMyProfile("randomId")
    assert(error.get == Error(SYST_401, "Not Available"))
    assert(result.isEmpty)
  }

  it should "be able to get user details by admin" in {
    val UserV: Vertex = DseGraphFactory.dseConn.executeGraph(getUser(cyrilId)).one().asVertex()
    val (spoker, error) = fetchUserInfo(cyrilId, UserV)

    assert(error.isEmpty)
    assert(spoker.get.cover == "")
    assert(spoker.get.picture == "testuser.jpg")
    assert(spoker.get.last_position == Geo(43.2805546, 5.2647101, 0.0))
  }

  it should "not be able to get details by admin" in {
    val UserV: Vertex = DseGraphFactory.dseConn.executeGraph(getUser(cyrilId)).one().asVertex()
    val (spoker, error) = fetchUserInfo("randomId", UserV)

    assert(error.get == Error(SYST_401, "Not Available"))
    assert(spoker.isEmpty)
  }

  it should "be able to get user mobile no from Dse GraphDatabase " in {
    insertUser(userAttributes)
    val MobileV: Boolean = isExistsMobileNo("919983899777")
    assert(MobileV)
  }

  it should "not be able to get user mobile no from Dse GraphDatabase " in {
    val MobileV: Boolean = isExistsMobileNo("98765432100")
    assert(!MobileV)
  }

  it should "be able to check user mobile no by user id in Dse GraphDatabase " in {
    val user = userAttributes
    insertUser(user)
    val MobileV: Boolean = isMobileNoExists("919983899777", user.userId)
    assert(MobileV)
  }

  it should "be able to insert user having new number " in {
    val UserV: Boolean = insertUser(userAttributesMobileTest)
    assert(UserV)
  }

  it should "be able to insert user having no contact number " in {
    val UserV: Boolean = insertUser(userWithNoContacts)
    assert(UserV)
  }

  it should "be able to insert user with number already exists in graph as contactNo " in {
    val UserV: Boolean = insertUser(userAttributesTest)
    assert(UserV)
  }

  it should "be able to fetch userId that has the registering user's contact no " in {
    insertUser(userAttributesMobileTest)
    val userIds: List[GraphNode] = getRegisteredUsers("919983899777")
    assert(userIds.size > 0)
  }

  it should "not be able to fetch userId that has the registering user's contact no " in {
    val userIds: List[GraphNode] = getRegisteredUsers("4545")
    assert(userIds.isEmpty)
  }

  it should "be able to Check  an user is following or not" in {

    val followUser = performFollowOrUnfollow("919582311059", userAttributesId, userAttributesTestId)
    assert(followUser == Some(FOLLOWS))
  }

  it should "be able to follow an user " in {

    val follow = FollowUnfollow("919582311059", userAttributesId, userAttributesTestId)
    val followUser = performFollow(follow)
    assert(followUser == Some(FOLLOWS))
  }

  it should "be able to fetch friend status " in {
    assert(!isFollowingExists(userAttributesTestId, userAttributesId))
  }

  it should "not be able to follow an user " in {

    val follow = FollowUnfollow("919582311059", userAttributesId, "1212131311313")
    val followUser = performFollow(follow)
    assert(followUser == None)
  }

  it should "be able to Unfollow an user " in {

    val follow = FollowUnfollow("919582311059", userAttributesId, userAttributesTestId)
    val followUser = performUnfollow(follow)
    assert(followUser == Some(UNFOLLOWS))
  }

  it should "be able to create an user group" in {
    val groupId = getUUID()
    val group = Group(groupId, "title")
    val result = createGroup(cyrilId, group)
    assert(result == Some("Group created"))
  }

  it should "be able to create a group for a user even if the title has special characters" in {
    val groupId = getUUID()
    val group = Group(groupId, "$peci'@l")
    val result = createGroup(cyrilId, group)
    assert(result == Some("Group created"))
  }

  it should "be able to create a group for a user even if the title has special character double quote" in {
    val groupId = getUUID()
    val group = Group(groupId, """"$P@RT!@N$""")
    val result = createGroup(cyrilId, group)
    assert(result == Some("Group created"))
  }

  it should "not be able to create an user group " in {
    val id = getUUID()
    val groupId = getUUID()
    val group = Group(groupId, "title")
    val result = createGroup(id, group)
    assert(result.isEmpty)
  }

  it should "be able to get group which is in Dse" in {
    val groupId = getUUID()
    val group = Group(groupId, "title")
    createGroup(cyrilId, group)
    val result = isGroupExist(cyrilId, groupId)
    assert(result)
  }

  it should "not be able to get group which is not in Dse" in {
    val id = getUUID()
    val groupId = getUUID()
    val result = isGroupExist(id, groupId)
    assert(!result)
  }

  it should "able to remove a group" in {

    val groupId = getUUID()
    val group = Group(groupId, "title")
    createGroup(cyrilId, group)
    removeGroup(groupId, cyrilId)
    val result = isGroupExist(cyrilId, groupId)
    assert(!result)
  }

  it should "be able to update an user group" in {
    val groupId = getUUID()
    val group = Group(groupId, "title")
    createGroup(cyrilId, group)
    val updatedGroup = Group(groupId, "updated Title")
    val result = updateGroup(cyrilId, updatedGroup)
    assert(result)

  }

  it should "be able to update user group with special characters also" in {
    val groupId = getUUID()
    val group = Group(groupId, "Special")
    createGroup(cyrilId, group)
    val updatedGroup = Group(groupId, "$peci'@l")
    val result = updateGroup(cyrilId, updatedGroup)
    assert(result)

  }

  it should "be able to update user group with special character double quote" in {
    val groupId = getUUID()
    val group = Group(groupId, "Special")
    createGroup(cyrilId, group)
    val updatedGroup = Group(groupId, """"$peci'@l""")
    val result = updateGroup(cyrilId, updatedGroup)
    assert(result)

  }

  it should "not be able to delete group which is invalid" in {
    val groupId = getUUID()
    val group = Group(groupId, "title")
    createGroup(cyrilId, group)
    removeGroup("123", cyrilId)
    val result = isGroupExist(cyrilId, groupId)
    assert(result)
  }

  it should " be able to insert valid follower and contact in private group " in {
    val id = getUUID()
    val userAttributes = User("testuser", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(27.22, 27.55), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+919582311059", "+919582611051"), "+919983899777", id, Some("testuser.jpg"))

    val userAttributes1 = User("testuser1", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(27.22, 27.55), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List(), "+919582311059", "userid987654321", Some("testuser1.jpg"))

    assert(insertUser(userAttributes))

    assert(insertUser(userAttributes1))
    performFollow(FollowUnfollow("919983899777", "userid987654321", id))
    createGroup(id, Group("privategroupId", "title"))
    val (contactList, userIdsList) = insertFollowersInGroup(id, UserGroup("privategroupId", List("userid987654321"),
      List(Contact("contactuser", "+919582611051"), Contact("somebody", "+919999999999"), Contact("testuser1", "+919582311059"))))
    assert(contactList.head == "+919999999999")
  }

  it should "be able to get contact no that Exists In PrivateGroups and link them with group " in {

    val newUser = User("newuser", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(27.22, 27.55), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List(), "+919582611051", "userid55555", Some("testuser1.jpg"))

    val UserV: Boolean = insertUser(newUser)
    assert(UserV)
  }

  it should " be able to get added in default group while following user " in {
    val id1 = getUUID()
    val id2 = getUUID()
    val userAttributes = User("prashant", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(27.22, 27.55), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+919582311059", "+919582611051"), "+919638527401", id1, Some("testuser.jpg"))

    val userAttributes1 = User("dhiru", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(27.22, 27.55), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List(), "+917418529630", id2, Some("testuser1.jpg"))

    assert(insertUser(userAttributes))
    assert(insertUser(userAttributes1))
    val followUser = performFollow(FollowUnfollow("919983899777", id2, id1))
    assert(followUser == Some(FOLLOWS))
  }

  it should " be able to get removed from default group while Unfollowing user " in {
    val id1 = getUUID()
    val id2 = getUUID()
    val userAttributes = User("prashant", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(27.22, 27.55), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+919582311059", "+919582611051"), "+919638527401", id1, Some("testuser.jpg"))

    val userAttributes1 = User("dhiru", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(27.22, 27.55), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List(), "+917418529630", id2, Some("testuser1.jpg"))

    assert(insertUser(userAttributes))
    assert(insertUser(userAttributes1))
    val followUser = performFollow(FollowUnfollow("919983899777", id2, id1))
    val unfollowUser = performUnfollow(FollowUnfollow("919983899777", id2, id1))
    assert(unfollowUser == Some(UNFOLLOWS))
  }

  it should " be able to remove valid follower and contact in group " in {
    val userAttributes = User("testuser", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(27.22, 27.55), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+919582311059", "+919582611051"), "+919983899777", "userid123456789", Some("testuser.jpg"))

    val userAttributes1 = User("testuser1", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(27.22, 27.55), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List(), "+919582311059", "userid987654321", Some("testuser1.jpg"))

    insertUser(userAttributes)
    insertUser(userAttributes1)
    performFollow(FollowUnfollow("919983899777", "userid987654321", "userid123456789"))
    val groupId = getUUID()
    val group = Group(groupId, "title")
    createGroup("userid123456789", group)
    val insertFollower = insertFollowersInGroup("userid123456789", UserGroup(groupId, List("userid987654321"),
      List(Contact("", "+919582611051"), Contact("", "+919999999999"))))
    val result = removeFollowersFromGroup("userid123456789", List("userid987654321"), List("+919999999999"), groupId)
    assert(result)

  }

  it should "be able to update user profile" in {
    val userId = getUUID()
    val user = User("testuser", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(27.22, 27.55), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+919582311059", "+919582611051"), "+919983899777", userId, Some("testuser.jpg"))

    val geo = Geo(123, 123, 123)
    val userProfile = UserProfile("updatenickname", date, "updategender", Some("updatepicture"), Some("updatecover"), geo)
    insertUser(user)
    val result = updateUserProfile(userId, userProfile)
    assert(result.equals(true))
  }

  it should "not be able to update user profile" in {
    val userId = getUUID()
    val geo = Geo(123, 123, 123)
    val userProfile = UserProfile("updatenickname", date, "updategender", Some("updatepicture"), Some("updatecover"), geo)
    val result = updateUserProfile(userId, userProfile)
    assert(result.equals(false))
  }

  it should "be able to view user's minimal details details" in {
    val userId = getUUID()
    val user = User("testuser", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(27.22, 27.55), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+919582311059", "+919582611051"), "+919983899777", userId, Some("testuser.jpg"))

    insertUser(user)
    val result = getUserMinimalDetails(userId)
    val userMinimalDetailsResponse = Some(UserMinimalDetailsResponse(userId, "testuser", "male", "testuser.jpg"))
    assert(result.equals(userMinimalDetailsResponse))
  }

  it should "not be able to view user's minimal details details" in {
    val userId = getUUID()
    val result = getUserMinimalDetails(userId)
    assert(result.equals(None))
  }

  it should "be able to get user profile full details" in {
    val date: Date = new java.util.Date()
    val userId1 = UUID.randomUUID().toString
    val userId2 = UUID.randomUUID().toString
    val spokId = UUID.randomUUID().toString
    val user1 = User("piyush", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(27.22, 27.55), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+1111111111"), "+3333333333", userId1, Some("piyush.jpg"))

    val user2 = User("Prashant", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(27.22, 27.55), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+2222222222"), "+1111111111", userId2, Some("Prashant.jpg"))

    val launched = DateTime.now().toDate
    val spok: Spok = Spok("rawtext", None, Some("Public"), Some(1), Some("this is my first spok"), None, Some("first spok"),
      None, None, None, Geo(43.2805546, 5.2647101, 222), spokId, launched.getTime)

    val viewFullUserProfileDetails = UserProfileFullDetails(userId1, "piyush", "male", "piyush.jpg", "", 1, 1, 1, true, true)

    insertUser(user1)
    insertUser(user2)
    performFollow(FollowUnfollow("1111111111", userId2, userId1))
    performFollow(FollowUnfollow("3333333333", userId1, userId2))
    dseSpokApi.createSpok(userId1, spok)
    val result = viewFullUserProfile(userId1, userId2).get
    assert(result.equals(viewFullUserProfileDetails))
  }

  it should "not be able to view user's full details" in {
    val userId = getUUID()
    val targetUserId = getUUID()
    val result = viewFullUserProfile(targetUserId, userId)
    assert(result.equals(None))
  }

  it should "be able to get the list of the followers of an user" in {
    val date: Date = new java.util.Date()
    val userId1 = UUID.randomUUID().toString
    val userId2 = UUID.randomUUID().toString
    val userId3 = UUID.randomUUID().toString
    val spokId = UUID.randomUUID().toString
    val user1 = User("piyush", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(27.22, 27.55), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+1111111111"), "+3333333333", userId1, Some("piyush.jpg"))

    val user2 = User("Prashant", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(27.22, 27.55), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+2222222222"), "+1111111111", userId2, Some("Prashant.jpg"))

    val user3 = User("Narayan", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(27.22, 27.55), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+4444444444"), "+2222222222", userId3, Some("Narayan.jpg"))

    val listofFollowers = List(Follow(userId3, "Narayan", "male", "Narayan.jpg"), Follow(userId2, "Prashant", "male", "Prashant.jpg"))

    insertUser(user1)
    insertUser(user2)
    insertUser(user3)
    performFollow(FollowUnfollow("1111111111", userId2, userId1))
    performFollow(FollowUnfollow("2222222222", userId3, userId1))

    val result = fetchFollowers(userId1, "1").get
    assert(result.previous.equals("0"))
    assert(result.followers.equals(listofFollowers))
  }

  it should "be able to get the list of the followings of an user" in {
    val date: Date = new java.util.Date()
    val userId1 = UUID.randomUUID().toString
    val userId2 = UUID.randomUUID().toString
    val userId3 = UUID.randomUUID().toString
    val spokId = UUID.randomUUID().toString
    val user1 = User("piyush", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(27.22, 27.55), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+1111111111"), "+3333333333", userId1, Some("piyush.jpg"))

    val user2 = User("Prashant", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(27.22, 27.55), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+2222222222"), "+1111111111", userId2, Some("Prashant.jpg"))

    val user3 = User("Narayan", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(27.22, 27.55), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+4444444444"), "+2222222222", userId3, Some("Narayan.jpg"))

    val listoffollowings = List(Follow(userId3, "Narayan", "male", "Narayan.jpg"), Follow(userId1, "piyush", "male", "piyush.jpg"))
    insertUser(user1)
    insertUser(user2)
    insertUser(user3)
    performFollow(FollowUnfollow("1111111111", userId2, userId1))
    performFollow(FollowUnfollow("1111111111", userId2, userId3))

    val result = fetchFollowings(userId2, "1").get
    assert(result.previous.equals("0"))
    assert(result.followings.equals(listoffollowings))
  }

  it should "be able to get the details of the groups of a user" in {

    val perryUserId = UUID.randomUUID().toString + "123"
    val sheldonUserId = UUID.randomUUID().toString + "456"
    val groupId = UUID.randomUUID().toString + "789"

    val perry = User("Perry", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(27.22, 27.55), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+918563477358"), "+919736288923", perryUserId, Some("Perry.jpg"))

    val sheldon = User("Sheldon", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(27.22, 27.55), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+918563488534", "+918563488553"), "+875473477645", sheldonUserId, Some("Sheldon.jpg"))
    insertUser(perry)
    createUserSetting(perry.userId)
    insertUser(sheldon)
    createUserSetting(sheldon.userId)
    performFollow(FollowUnfollow("919736288923", perryUserId, sheldonUserId))
    updateDefaultGroup(perryUserId, sheldonUserId, FOLLOWS)
    val group = Group(groupId, "Spartians")
    createGroup(sheldonUserId, group)
    insertFollowersInGroup(sheldonUserId, UserGroup(groupId, List(perryUserId),
      List(Contact("penny", "+918563488534"), Contact("bradley", "+918563488553"))))
    val result: Option[GroupsResponse] = fetchGroupDetailsForAUser(sheldonUserId, "1")
    val response = result.get.groups
    assert(response.contains(UserGroupsDetails(groupId, "Spartians", List("bradley", "penny", "Perry"), 3, 1, 2)))
    assert(response.contains(UserGroupsDetails("0", "Followers", List("Perry"), 1, 1, 0)))
  }

  it should "be able to create setting Vertex while inserting user" in {

    val userId = UUID.randomUUID().toString
    val userDetails = User("sam", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(27.22, 27.55), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List(), "+919736288923", userId, Some("Perry.jpg"))

    insertUser(userDetails)
    val settingLabel: String = createSettingVertex(userId)
    assert(settingLabel.equals("settings"))
  }

  it should "be able to update user setting Vertex" in {

    val userId = UUID.randomUUID().toString
    val userDetails = User("ram", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(27.22, 27.55), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List(), "+919736288922", userId, Some("ram.jpg"))

    val userInserted = insertUser(userDetails)
    val settingLabel: String = createSettingVertex(userId)
    assert(updateUserSettings(userId, UserSetting(false, false)))

  }

  it should "be able to get user setting " in {

    val userId = UUID.randomUUID().toString
    val userDetails = User("shyam", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(27.22, 27.55), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List(), "+919454698752", userId, Some("shyam.jpg"))

    val userInserted = insertUser(userDetails)
    val settingLabel: String = createSettingVertex(userId)
    assert(fetchUserSettings(userId, "followings"))
    assert(fetchUserSettings(userId, "follower"))
  }

  it should "be able to update user help setting " in {

    val userId = UUID.randomUUID().toString
    val userDetails = User("abc", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(27.22, 27.55), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List(), "+919736284411", userId, Some("ram.jpg"))

    val userInserted = insertUser(userDetails)
    createUserSetting(userDetails.userId)
    assert(updateUserHelpSetting(userId))

  }

  it should "be able to update user phone number" in {
    val userId = UUID.randomUUID().toString
    val newNumber = "2222222222"
    val userDetails = User("ram", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(27.22, 27.55), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List(), "+911111111111", userId, Some("ram.jpg"))
    insertUser(userDetails)
    val result = updatePhoneNumber(userId, newNumber)
    assert(result.equals(true))
  }

  it should "not be able to update user phone number" in {
    val userId = UUID.randomUUID().toString
    val newNumber = "2222222222"
    val result = updatePhoneNumber(userId, newNumber)
    assert(result.equals(false))
  }

  it should "be able to validate user by userId" in {
    val userId = UUID.randomUUID().toString
    val newNumber = "2222222222"
    val userDetails = User("Rahul", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(27.22, 27.55), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List(), "+911111111111", userId, Some("Rahul.jpg"))
    insertUser(userDetails)
    val result = isValidUserId(userId)
    assert(result.equals(true))
  }

  it should "be able to get my details" in {
    val date: Date = new java.util.Date()
    val userId1 = UUID.randomUUID().toString
    val userId2 = UUID.randomUUID().toString
    val spokId = UUID.randomUUID().toString
    val user1 = User("piyush", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(27.22, 27.55), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+1111111111"), "+3333333333", userId1, Some("piyush.jpg"))

    val user2 = User("Prashant", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(27.22, 27.55), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+2222222222"), "+1111111111", userId2, Some("Prashant.jpg"))

    val launched = DateTime.now().toDate
    val spok: Spok = Spok("rawtext", None, Some("Public"), Some(1), Some("this is my first spok"), None, Some("first spok"),
      None, None, None, Geo(43.2805546, 5.2647101, 222), spokId, launched.getTime)

    val myDetails = MyDetails(userId1, "piyush", "male", "piyush.jpg", "", 1, 1, 1)

    insertUser(user1)
    insertUser(user2)
    performFollow(FollowUnfollow("1111111111", userId2, userId1))
    performFollow(FollowUnfollow("3333333333", userId1, userId2))
    dseSpokApi.createSpok(userId1, spok)
    val result = viewMyProfile(userId1).get
    assert(result.equals(myDetails))
  }

  it should "not be able to view my details" in {
    val userId = getUUID()
    val result = viewMyProfile(userId)
    assert(result.equals(None))
  }

  it should " be able to get list of already added follower and contact in private group " in {
    val userId = getUUID()
    val userId1 = getUUID()
    val groupId = getUUID()
    val userAttributes = User("testuser", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(27.22, 27.55), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+919582311059", "+919582611051"), "+919983899777", userId, Some("testuser.jpg"))

    val userAttributes1 = User("testuser1", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(27.22, 27.55), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List(), "+919582311059", userId1, Some("testuser1.jpg"))

    assert(insertUser(userAttributes))
    assert(insertUser(userAttributes1))
    performFollow(FollowUnfollow("919983899777", userId1, userId))
    createGroup(groupId, Group(groupId, "title"))
    val userGroupDetails = UserGroup(groupId, List(userId1),
      List(Contact("contactuser", "+919582611051"), Contact("somebody", "+919999999999"), Contact("testuser1", "+919582311059")))
    insertFollowersInGroup(userId, userGroupDetails)
    val (contectList, userIdlist) = validateUsersOrContactByGroupId(userId, userGroupDetails.groupId, userGroupDetails.userIds, List("919582611051", "919999999999", "919582311059"))
    assert(contectList.head == "919582611051")
    assert(userIdlist.head == userId1)
  }

  it should "not be able to provide the nickname of a user if there is no user against a userId" in {
    val userId = getUUID()
    val result = getNickname(userId)
    assert(result.isEmpty)
  }

  it should "be able to provide the nickname of a user with valid userId" in {
    val userId = getUUID()
    val userAttributes = User("Roger", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(27.22, 27.55), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+919582311059", "+919582611051"), "+919983899777", userId, Some("Roger.jpg"))
    insertUser(userAttributes)
    val result = getNickname(userId)
    assert(result.isDefined)
    assert(result.get === "Roger")
  }

  it should "be able to Unfollow an user and linked with its next immediate following user" in {

    val spokId = getUUID()
    val spok1: Spok = Spok("rawtext", Some("0"), Some("public"), Some(1), Some("this is my first spok from cyril"), Some("first spok"),
      None, None, None, None, Geo(43.2805546, 5.2647101, 222), spokId)
    dseSpokApi.createSpok(cyrilId, spok1)
    val launched = timeStamp
    val res = dseSpokApi.linkSpokerFollowers(cyrilId, Some("0"), spokId, Geo(43.2805546, 5.2647101, 222),
      Some("public"), Some("this is my first spok from cyril"), launched, 0)

    val geo = Geo(45.00, 45.00, 45.00)

    val edge = DseGraphFactory.dseConn.executeGraph(getEdgeBetweenUserAndSpok(cyrilId, spokId)).one().asEdge()

    dseSpokApi.createRespok(spokId, kaisId, Respok(Some("0"), Some("public"), Some("I kais am respoking"), geo, None), Some(edge))
    dseSpokApi.createRespok(spokId, vikasId, Respok(Some("0"), Some("public"), Some("I vikas am respoking"), geo, None), Some(edge))

    val unfollow = FollowUnfollow("919582311067", vincentId, kaisId)
    val followUser = performUnfollow(unfollow)
    assert(followUser == Some(UNFOLLOWS))
    performAfterUnfollowActions(unfollow)
    assert(DseGraphFactory.dseConn.executeGraph(
      "g.V().has('userId','" + vincentId + "').outE('isAssociatedWith').has('from','" + vikasId + "').hasNext()"
    ).one().asBoolean())
  }

  it should "be able to get unique nickname validation from Dse GraphDatabase " in {
    insertUser(userAttributes)
    val uniqueNickName: Boolean = isUniqueNickname("piyush")
    assert(uniqueNickName)
  }

  it should "not be able to get unique nickname validation from Dse GraphDatabase " in {
    insertUser(userAttributes)
    val uniqueNickName: Boolean = isUniqueNickname("abc12345674131")
    assert(!uniqueNickName)
  }

  it should "be able to get details of a specific group" in {
    val groupId = getUUID()
    val group = Group(groupId, "Specific")
    createGroup(cyrilId, group)
    val userGroup = UserGroup(groupId, List(vikasId), List(Contact("Jason", "+919983899345")))
    insertFollowersInGroup(cyrilId, userGroup)
    updateUserCountInGroup(groupId, cyrilId)
    val output = SingleGroupDetails(groupId, "Specific", "0", "", 2, 1, 1, List(
      ContactDetailsForSingleGroup("contact", "Jason", "919983899345"),
      FollowerDetailsForSingleGroup("spoker", vikasId, "Vikas", "male", "testuser1.jpg")
    ))
    val (result, error) = getSingleGroupDetails(cyrilId, groupId, "1")
    assert(error.isEmpty)
    assert(result.get == output)
  }

  it should "be able to send group not found error if the specific group is not found" in {
    val groupId = getUUID()
    val (result, error) = getSingleGroupDetails(cyrilId, groupId, "1")
    assert(error.get == Error("GRP-001", "Group not found"))
    assert(result.isEmpty)
  }

  it should "be able to get details of a specific group if the group has only users and no contacts" in {
    val groupId = getUUID()
    val group = Group(groupId, "UsersOnly")
    createGroup(cyrilId, group)
    val userGroup = UserGroup(groupId, List(vikasId), Nil)
    insertFollowersInGroup(cyrilId, userGroup)
    updateUserCountInGroup(groupId, cyrilId)
    val output = SingleGroupDetails(groupId, "UsersOnly", "0", "", 1, 0, 1, List(
      FollowerDetailsForSingleGroup("spoker", vikasId, "Vikas", "male", "testuser1.jpg")
    ))
    val (result, error) = getSingleGroupDetails(cyrilId, groupId, "1")
    assert(error.isEmpty)
    assert(result.get == output)
  }

  it should "be able to get details of a specific group if it has only contacts and no user" in {
    val groupId = getUUID()
    val group = Group(groupId, "ContactsOnly")
    createGroup(cyrilId, group)
    val userGroup = UserGroup(groupId, Nil, List(Contact("Jason", "+919983899345")))
    insertFollowersInGroup(cyrilId, userGroup)
    updateUserCountInGroup(groupId, cyrilId)
    val output = SingleGroupDetails(groupId, "ContactsOnly", "0", "", 1, 1, 0, List(
      ContactDetailsForSingleGroup("contact", "Jason", "919983899345")
    ))
    val (result, error) = getSingleGroupDetails(cyrilId, groupId, "1")
    assert(error.isEmpty)
    assert(result.get == output)
  }

  it should "be able to check User Admin or not" in {
    val result: Option[Boolean] = checkUserAdminOrNot(cyrilId)
    val status = result.getOrElse("generic")
    assert(status == false)
  }

  it should "be able to check User SuperAdmin or not" in {
    val result: Option[Boolean] = checkUserSuperAdminOrNot(cyrilId)
    val status = result.getOrElse("generic")
    assert(status == false)
  }

  it should "not be able to perform cleanup" in {
    val result = performCleanUp(kaisId)
    assert(result == false)
  }

  it should "be able to perform cleanup" in {
    DseGraphFactory.dseConn.executeGraph(s"graph.addVertex(label,'spok','spokId','123456789','author','$kaisId','enabled','true')")
    val result = performCleanUp(kaisId)
    assert(result)
  }

  it should "be able to check User is superAdmin or not" in {
    val result = checkUserSuperAdminOrNot(cyrilId)
    val status = result.get
    assert(!status)
  }

  it should "be able to check User is superAdmin or not when generic error comes" in {
    val shubhamId = getUUID()

    val result = checkUserSuperAdminOrNot(shubhamId)
    val status = result.getOrElse("generic")
    assert(status == status)
  }

  it should "be able to set user level" in {
    val level = "admin"
    val result = setUserLevel(cyrilId, level)
    val status = result.get
    assert(status)
  }

  it should "not be able to set user level when generic error comes" in {
    val shubhamId = getUUID()
    val level = "admin"
    val result = setUserLevel(shubhamId, level)
    val status = result.getOrElse("generic")
    assert(status == "generic")
  }

  it should "be able to fetch user level" in {
    val level = "admin"
    val result = fetchUserLevel(cyrilId)
    val status = result.getOrElse("generic")
    assert(status == level)
  }

  it should "not be able to fetch user level" in {
    val shubhamId = getUUID()
    val level = "admin"
    val result = fetchUserLevel(shubhamId)
    val status = result.getOrElse("generic")
    assert(status == "generic")
  }

  it should "be able to fetch isUserSuspendAlready" in {
    val result = isUserSuspendAlready(cyrilId)
    val status = result.getOrElse(true)
    assert(status == false)
  }

  it should "be able to suspend user account successfully" in {
    val result = suspendUserAccount(cyrilId)
    assert(result.getOrElse(false))
  }

  it should "not be able to suspend user account successfully when generic error comes" in {
    val result = suspendUserAccount("1234500000")
    assert(!result.getOrElse(false))
  }

  it should "be able to recativate user account successfully" in {
    val result = reactiveUserAccount(cyrilId)
    assert(result.getOrElse(false))
  }

  it should "not be able to recativate user account successfully when generic error comes" in {
    val result = reactiveUserAccount("1234500000")
    assert(!result.getOrElse(false))
  }

  it should "be able to get result success when generic error comes in isUserSuspendAlready" in {
    val shubhamId = getUUID()
    val result = isUserSuspendAlready(shubhamId)
    assert(!result.isDefined)
  }

  it should "be able to disable user account" in {
    val vertex = DseGraphFactory.dseConn.executeGraph(s"g.V().hasLabel('$USER').has('$USER_ID', '$cyrilId')").one().asVertex()
    val result = disableUserAccount(cyrilId, vertex)
    assert(result)
  }

  it should "be able to check User Account disable or not" in {
    val result: Option[Vertex] = fetchUserAccountDisableOrNot(cyrilId)
    val status = result.getOrElse("account has been disabled")
    assert(status == "account has been disabled")
  }

}
