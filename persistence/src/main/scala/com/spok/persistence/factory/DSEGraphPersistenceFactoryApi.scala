package com.spok.persistence.factory

import java.sql.Timestamp
import java.util
import java.util.Date

import com.datastax.driver.core.ResultSet
import com.datastax.driver.dse.graph._
import com.spok.model.Account._
import com.spok.model.SpokModel._
import com.spok.model.{ Location, SpecificGroupResponse }
import com.spok.persistence.dse.DseConnectionUri._
import com.spok.persistence.dse.DseGraphFactory
import com.spok.persistence.factory.dsequery.{ DSEGraphPersistenceQuery, DSEUserQuery }
import com.spok.persistence.factory.spokgraph.DSESpokQuery
import com.spok.persistence.factory.spoklog.SpokLogging
import com.spok.persistence.redis.RedisFactory
import com.spok.util.Constant._
import com.spok.util.{ JWTTokenHelper, LoggerUtil }
import org.joda.time.DateTime

import scala.collection.JavaConverters._
import scala.collection.parallel.immutable.ParSeq
import scala.concurrent.Future

case class SpokId(spokId: String)

trait DSEGraphPersistenceFactoryApi extends DSEUserQuery with DSEGraphPersistenceQuery
    with LoggerUtil with DSESpokQuery with SpokLogging with RedisFactory {

  /**
   * Validates the User Mobile No in DSE Graph
   *
   * @param mobile_No
   * @return boolean
   */
  def isExistsMobileNo(mobile_No: String): Boolean = {
    val findMobileNo = s"""g.V().hasLabel("$USER").outE("$HAS_A").inV().has("$PHONE_NO",'$mobile_No')"""
    val MobileV: Boolean = DseGraphFactory.dseConn.executeGraph(findMobileNo).iterator().hasNext
    MobileV
  }

  /**
   * Validates the User with Mobile No in DSE Graph
   *
   * @param mobileNo
   * @param userId
   * @return boolean
   */
  def isMobileNoExists(mobileNo: String, userId: String): Boolean = {
    val findMobileNo = s"""g.V().hasLabel("$USER").has("$USER_ID",'$userId').outE("$HAS_A").inV().has("$PHONE_NO",'$mobileNo')"""
    DseGraphFactory.dseConn.executeGraph(findMobileNo).iterator().hasNext
  }

  /**
   * To insert the User
   *
   * @param userAttributes is User case class
   */
  def insertUser(userAttributes: User): Boolean = {
    val elevation: Double = 0.0
    val contactNumbers: List[String] = userAttributes.contacts
    val userQuery = insertUserQuery(userAttributes)
    val activityGeo = Geo(
      userAttributes.location.results.head.geometry.location.lat,
      userAttributes.location.results.head.geometry.location.lng, elevation
    )
    createUserVertexEdge(activityGeo, userQuery, contactNumbers, userAttributes.userNumber.substring(1))
  }

  def createUserSetting(userId: String): Unit = {
    try {
      val groupId = ZERO
      val groupDetails = Group(groupId, "Followers")
      createGroup(userId, groupDetails)
      createSettingVertex(userId)
      info(" Successfully created user settings")
    } catch {
      case ex: Exception =>
        error(" exception while creating  user settings  ", ex)
    }
  }

  def createSettingVertex(userId: String): String = {
    val userVertex = DseGraphFactory.dseConn.executeGraph(getUser(userId)).one().asVertex()
    val settingVertex = DseGraphFactory.dseConn.executeGraph(createSettingVertexQuery(UserSetting(), true)).one().asVertex()
    executeSimpleGraphStatement(userVertex, settingVertex, HAS_SETTINGS).one().asEdge().getInVLabel
  }

  def createUserVertexEdge(activityGeo: Geo, userQuery: String, contactNumbers: List[String], userNumber: String): Boolean = {
    val userStmt = new SimpleGraphStatement(userQuery)
    val userVertex: Vertex = DseGraphFactory.dseConn.executeGraph(userStmt).one().asVertex()
    val GeoVertex: Vertex = DseGraphFactory.dseConn.executeGraph(createActivityGeo(activityGeo)).one().asVertex()
    executeSimpleGraphStatement(userVertex, GeoVertex, ACTIVITY_GEO)

    if (isEdgeExist(USER, HAS_A_CONTACT, PHONE_NO, userNumber)) {
      val mobileV: Vertex = DseGraphFactory.dseConn.executeGraph(getVertexQuery(USER, HAS_A_CONTACT, PHONE_NO, userNumber)).one().asVertex()
      executeSimpleGraphStatement(userVertex, mobileV, HAS_A)
      isExistsInPrivateGroups(userVertex, userNumber)
      removePendingInstanceFromContactNo(userVertex, userNumber)
    } else {
      executeSimpleGraphStatement(userVertex, createNewMobileVertex(MOBILE_NO, userNumber), HAS_A)
    }
    if (contactNumbers.size > 0) {
      createContactVertexEdge(contactNumbers, userVertex).size.equals(contactNumbers.size)
    } else {
      true
    }
  }

  /**
   * This function will update user profile.
   *
   * @param userId      User's id
   * @param userProfile user profile update details
   * @return true if profile get updated else false
   */
  def updateUserProfile(userId: String, userProfile: UserProfile): Boolean = {
    try {
      val userWithGeo = DseGraphFactory.dseConn.executeGraph(updateUserProfileQuery(userId, userProfile)).asScala.toList
      val profile: Option[String] = userProfile.picture
      val cover = userProfile.cover
      userWithGeo.map { res =>
        val user = res.get("user").asVertex()
        val geo = res.get(GEO).asVertex()

        (profile, cover) match {
          case (None, None) => if (user.getProperty(NICKNAME).getValue.asString().equals(userProfile.nickname) &&
            user.getProperty("birthDate").getValue.asString().equals(userProfile.birthDate.toString) &&
            user.getProperty(GENDER).getValue.asString().equals(userProfile.gender) &&
            user.getProperty(GEOTEXT).getValue.asString().equals(userProfile.geoText) &&
            geo.getProperty(ELEVATION).getValue.asDouble().equals(userProfile.geo.elevation) &&
            geo.getProperty(LATITUDE).getValue.asDouble().equals(userProfile.geo.latitude) &&
            geo.getProperty(LONGITUDE).getValue.asDouble().equals(userProfile.geo.longitude)) {
            true
          } else {
            false
          }

          case (None, _) => if (user.getProperty(NICKNAME).getValue.asString().equals(userProfile.nickname) &&
            user.getProperty("birthDate").getValue.asString().equals(userProfile.birthDate.toString) &&
            user.getProperty(GENDER).getValue.asString().equals(userProfile.gender) &&
            user.getProperty(GEOTEXT).getValue.asString().equals(userProfile.geoText) &&
            user.getProperty(COVER).getValue.asString().equals(userProfile.cover.getOrElse("")) &&
            geo.getProperty(ELEVATION).getValue.asDouble().equals(userProfile.geo.elevation) &&
            geo.getProperty(LATITUDE).getValue.asDouble().equals(userProfile.geo.latitude) &&
            geo.getProperty(LONGITUDE).getValue.asDouble().equals(userProfile.geo.longitude)) {
            true
          } else {
            false
          }
          case (_, None) => if (user.getProperty(NICKNAME).getValue.asString().equals(userProfile.nickname) &&
            user.getProperty("birthDate").getValue.asString().equals(userProfile.birthDate.toString) &&
            user.getProperty(GENDER).getValue.asString().equals(userProfile.gender) &&
            user.getProperty(GEOTEXT).getValue.asString().equals(userProfile.geoText) &&
            user.getProperty(PICTURE).getValue.asString().equals(userProfile.picture.getOrElse("")) &&
            geo.getProperty(ELEVATION).getValue.asDouble().equals(userProfile.geo.elevation) &&
            geo.getProperty(LATITUDE).getValue.asDouble().equals(userProfile.geo.latitude) &&
            geo.getProperty(LONGITUDE).getValue.asDouble().equals(userProfile.geo.longitude)) {
            true
          } else {
            false
          }

          case _ => if (user.getProperty(NICKNAME).getValue.asString().equals(userProfile.nickname) &&
            user.getProperty("birthDate").getValue.asString().equals(userProfile.birthDate.toString) &&
            user.getProperty(GENDER).getValue.asString().equals(userProfile.gender) &&
            user.getProperty(GEOTEXT).getValue.asString().equals(userProfile.geoText) &&
            user.getProperty(PICTURE).getValue.asString().equals(userProfile.picture.getOrElse("")) &&
            user.getProperty(COVER).getValue.asString().equals(userProfile.cover.getOrElse("")) &&
            geo.getProperty(ELEVATION).getValue.asDouble().equals(userProfile.geo.elevation) &&
            geo.getProperty(LATITUDE).getValue.asDouble().equals(userProfile.geo.latitude) &&
            geo.getProperty(LONGITUDE).getValue.asDouble().equals(userProfile.geo.longitude)) {
            true
          } else {
            false
          }
        }

      }.head
    } catch {

      case ex: Exception => false
    }
  }

  /**
   * * To check if contanct exists in private group , If exist then remove egde from contact and link with registered user
   *
   * @param userVertex
   * @param userNumber
   */
  def isExistsInPrivateGroups(userVertex: Vertex, userNumber: String): List[Boolean] = {
    val listPrivateGroupV = DseGraphFactory.dseConn.executeGraph(getPrivateGroup(userNumber)).asScala.toList
    val isexists: List[Boolean] = if (listPrivateGroupV.nonEmpty) {
      val res: List[Boolean] = listPrivateGroupV map { groupId =>
        DseGraphFactory.dseConn.executeGraph(removeEdgeFromGroup(userNumber))
        DseGraphFactory.dseConn.executeGraph(executeGraphStatement(userVertex, groupId.asVertex(), BELONGS_TO)).one().isEdge
      }
      res
    } else {
      Nil
    }
    isexists
  }

  /**
   * To check if pending Instances are linked with contact No , If exists then remove edge and link with registered user
   *
   * @param userVertex
   * @param userNumber
   */
  def removePendingInstanceFromContactNo(userVertex: Vertex, userNumber: String): Unit = {
    val listOfSpoksContactAttachedTo = DseGraphFactory.dseConn.executeGraph(getPendingInstanceLinked(userNumber)).asScala.toList
    if (listOfSpoksContactAttachedTo.nonEmpty) {
      listOfSpoksContactAttachedTo map { spokAssociatedEdge =>
        val spokVertex = spokAssociatedEdge.asVertex()
        val spokId = spokVertex.getProperty(SPOK_ID).getValue.asString()
        val edge = DseGraphFactory.dseConn.executeGraph(getSpokEdgeConnectedToContact(userNumber, spokId)).one().asEdge()
        val elevation = edge.getProperty(ELEVATION).getValue.asDouble
        val from = edge.getProperty(FROM).getValue.asString()
        val groupId = edge.getProperty(GROUP_ID).getValue.asString()
        val headerText = edge.getProperty(HEADER_TEXT).getValue.asString()
        val latitude = edge.getProperty(LATITUDE).getValue.asDouble()
        val launched = edge.getProperty(LAUNCHED).getValue.asLong()
        val longitude = edge.getProperty(LONGITUDE).getValue.asDouble
        val pendingQuestion = edge.getProperty(PENDING_QUESTIONS).getValue.asInt()
        val status = edge.getProperty(STATUS).getValue.asString()
        val visibility = edge.getProperty(VISIBILITY).getValue.asString()
        val geo = Geo(latitude, longitude, elevation)
        val edgeProperties = SpokEdge(status, launched, geo, from, groupId, visibility, headerText, pendingQuestion)
        addEdgeSpokerSpok(userVertex, spokVertex, ISASSOCIATEDWITH, edgeProperties)
        DseGraphFactory.dseConn.executeGraph(removeEdgeFromPendingInstance(userNumber, spokId))
      }
    }
  }

  def createContactVertexEdge(contactAttributes: List[String], userVertex: Vertex): List[Boolean] = {
    contactAttributes map { contactNo =>
      val result = if (isEdgeExist(USER, HAS_A_CONTACT, PHONE_NO, contactNo.substring(1))) {
        val mobileContactVertex = DseGraphFactory.dseConn.executeGraph(getVertexQuery(USER, HAS_A_CONTACT, PHONE_NO, contactNo.substring(1))).one.asVertex()
        executeSimpleGraphStatement(userVertex, mobileContactVertex, HAS_A_CONTACT)

      } else if (isEdgeExist(USER, HAS_A, PHONE_NO, contactNo.substring(1))) {
        val mobileVertex = DseGraphFactory.dseConn.executeGraph(getVertexQuery(USER, HAS_A, PHONE_NO, contactNo.substring(1))).one.asVertex()
        executeSimpleGraphStatement(userVertex, mobileVertex, HAS_A_CONTACT)

      } else {
        executeSimpleGraphStatement(userVertex, createNewMobileVertex(MOBILE_NO, contactNo.substring(1)), HAS_A_CONTACT)
      }
      result.iterator().hasNext
    }
  }

  def getRegisteredUsers(userNumber: String): List[GraphNode] = {
    val registeredUsers: GraphResultSet = DseGraphFactory.dseConn.executeGraph(getRegisteredUser(userNumber))
    registeredUsers.all().asScala.toList
  }

  private def createNewMobileVertex(mobileLabel: String, contactNo: String): Vertex = {
    val mobileQuery = s"""graph.addVertex(label,"$mobileLabel","phoneNo","${contactNo}")""".stripMargin
    DseGraphFactory.dseConn.executeGraph(mobileQuery).one.asVertex()
  }

  private def isEdgeExist(vertexLabel: String, outEageLabel: String, inVertexProperty: String, inVertexPropertyValue: String): Boolean = {
    DseGraphFactory.dseConn.executeGraph(getVertexQuery(vertexLabel, outEageLabel, inVertexProperty, inVertexPropertyValue)).iterator().hasNext
  }

  private def getVertexQuery(vertexLabel: String, outEdgeLabel: String, inVertexProperty: String, inVertexPropertyValue: String): String = {
    s"""g.V().hasLabel("$vertexLabel").outE("$outEdgeLabel").inV().has("$inVertexProperty",'${inVertexPropertyValue}')"""
  }

  private def executeSimpleGraphStatement(vertexFirst: Vertex, vertexSecond: Vertex, edgeLabel: String): GraphResultSet = {
    val sGraphStmt = new SimpleGraphStatement(
      "def v1 = g.V(id1).next()\n" + "def v2 = g.V(id2).next()\n" + "v1.addEdge('" + edgeLabel + "', v2 ,'launched', '" + System.currentTimeMillis() + "')"
    ).set("id1", vertexFirst.getId()).set("id2", vertexSecond.getId())
    DseGraphFactory.dseConn.executeGraph(sGraphStmt)
  }

  def performFollowOrUnfollow(userNumber: String, followerId: String, followingId: String): Option[String] = {

    try {
      val edgeExist = isFollowingExists(followerId, followingId)
      logger.info(s"Is the user already a follower : $edgeExist")
      if (!edgeExist) {
        val follow = FollowUnfollow(userNumber, followerId, followingId)
        performFollow(follow)
      } else {
        val unfollow = FollowUnfollow(userNumber, followerId, followingId)
        performUnfollow(unfollow)
      }
    } catch {
      case ex: Exception => None
    }
  }

  /**
   * This function will determine whether user follows other user or not.
   *
   * @param userId
   * @param targetUserId
   * @return true if exists else false
   */
  def isFollowingExists(userId: String, targetUserId: String): Boolean = {
    DseGraphFactory.dseConn.executeGraph(isEdgeExistsFollows(userId, targetUserId)).one().asBoolean()
  }

  def performFollow(follow: FollowUnfollow): Option[String] = {

    try {
      val followerVertex = DseGraphFactory.dseConn.executeGraph(getUserQuery(follow.followerId)).one().asVertex()
      val followingVertex = DseGraphFactory.dseConn.executeGraph(getUserQuery(follow.followingId)).one().asVertex()
      val followEdge = executeSimpleGraphStatement(followerVertex, followingVertex, FOLLOWS).one().asEdge().getLabel
      logger.info(s"Label received after trying to follow : $followEdge")
      if (followEdge.equals(FOLLOWS)) {
        Some(FOLLOWS)
      } else {
        None
      }
    } catch {
      case ex: Exception => None
    }
  }

  /**
   *
   * @param unfollow
   * @return Successful user unfollowed message if the operation was successful else none.
   */
  def performUnfollow(unfollow: FollowUnfollow): Option[String] = {
    try {
      val unfollowEdge = DseGraphFactory.dseConn.executeGraph(removeFollowEdge(unfollow.followerId, unfollow.followingId)).all().isEmpty
      logger.info(s"Was it able to unfollow? : $unfollowEdge")
      if (unfollowEdge) {
        Some(UNFOLLOWS)
      } else {
        None
      }
    } catch {
      case ex: Exception => None
    }
  }

  /**
   * performs after unfollow operation like updating default user,removing pending instance and link with immediate follower
   *
   * @param unfollow
   * @return
   */
  def performAfterUnfollowActions(unfollow: FollowUnfollow): Any = {
    logger.info(s"updated groups after user unfollows : $unfollow")
    updateDefaultGroup(unfollow.followerId, unfollow.followingId, UNFOLLOWS)
    val listOfSpokIds: List[GraphNode] = removePendingEdges(unfollow.followerId, unfollow.followingId)
    logger.info(s"Linking follower with spokId again!!! : ${listOfSpokIds.size}")
    linkFollowerWithImmediateFollowingUser(unfollow.followerId, listOfSpokIds)
  }

  /**
   * Method to follower pending edges
   *
   * @param followerId
   * @param followingId
   * @return
   */
  private def removePendingEdges(followerId: String, followingId: String): List[GraphNode] = {
    val listSpokId = DseGraphFactory.dseConn.executeGraph(getSpokIdfromPendingEdges(followerId, followingId)).asScala.toList
    DseGraphFactory.dseConn.executeGraph(removeUnfollowerPendingEdges(followerId, followingId))
    listSpokId
  }

  /**
   * Links follower to its next immediate following user when follower unfollows
   *
   * @param followerId
   * @param listOfSpokIds
   * @return
   */
  private def linkFollowerWithImmediateFollowingUser(followerId: String, listOfSpokIds: List[GraphNode]) = {
    val followerVertex = DseGraphFactory.dseConn.executeGraph(getUserQuery(followerId)).one().asVertex()
    val followerGeo = DseGraphFactory.dseConn.executeGraph(getFollowersCurrentGeo(followerId)).asScala.toList
    if (listOfSpokIds.nonEmpty) {
      listOfSpokIds map { spokId =>
        val currentTimestamp = System.currentTimeMillis()
        val spokVertex = DseGraphFactory.dseConn.executeGraph(getSpokVertex(spokId.asString())).one().asVertex()
        val pendingQuestions = spokVertex.getProperty(ACTUAL_QUESTIONS).getValue.asInt()
        if (DseGraphFactory.dseConn.executeGraph(isNextImmediateFollowingUser(followerId, spokId.asString())).one().asBoolean()) {
          val followingUserEdge: Edge = DseGraphFactory.dseConn.executeGraph(
            getNextImmediateFollowingUser(followerId, spokId.asString())
          ).one().asEdge()
          if (followingUserEdge.getLabel.equals(ISASSOCIATEDWITH)) {
            val spokEdgeProperties = SpokEdge(PENDING, currentTimestamp, Geo(
              followerGeo(0).toString.toDouble, followerGeo(1).toString.toDouble, followerGeo(2).toString.toDouble
            ), followingUserEdge.getProperty(FROM).getValue.asString(),
              followingUserEdge.getProperty(GROUP_ID).getValue.asString(), followingUserEdge.getProperty(VISIBILITY).getValue.asString(),
              followingUserEdge.getProperty(HEADER_TEXT).getValue.asString(), pendingQuestions)
            addEdgeSpokerSpok(followerVertex, spokVertex, ISASSOCIATEDWITH, spokEdgeProperties)
            logger.info(s"Logging follower pending details in cassandra : $spokId")
            logFollowerPendingEdgeEvent(spokId.asString(), Geo(
              spokVertex.getProperty("geo_latitude").getValue.asString.toDouble, spokVertex.getProperty("geo_longitude").getValue.asString.toDouble,
              spokVertex.getProperty("geo_elevation").getValue.asString.toDouble
            ),
              followerId, Geo(followerGeo(0).toString.toDouble, followerGeo(1).toString.toDouble,
              followerGeo(2).toString.toDouble), currentTimestamp)
          }
        }
      }
    }
  }

  private def logFollowerPendingEdgeEvent(spokId: String, spokGeo: Geo,
    followerId: String, followerGeo: Geo, logTime: Long): ResultSet = {
    val spokJson = SpokHistory(spokId, logTime, "", spokGeo.latitude, spokGeo.longitude, spokGeo.elevation,
      followerId, PENDING)
    insertHistory(write(spokJson), spok)

    val dataJsonSpoker = write(SpokId(spokId))
    val spokerJson = SpokerHistory(followerId, logTime, dataJsonSpoker, followerGeo.latitude,
      followerGeo.longitude, followerGeo.elevation,
      PENDING)
    insertHistory(write(spokerJson), spoker)
  }

  /**
   * Update group when user follows and unfollows
   *
   * @param followerUserId
   * @param followingUserId
   * @param status
   */
  def updateDefaultGroup(followerUserId: String, followingUserId: String, status: String): Unit = {
    status match {
      case FOLLOWS =>
        val userV = DseGraphFactory.dseConn.executeGraph(getUser(followerUserId)).one().asVertex()
        val nickname = userV.getProperty(NICKNAME).getValue.asString()
        val groupV = DseGraphFactory.dseConn.executeGraph(fetchUserGroup(followingUserId, ZERO)).one().asVertex()
        DseGraphFactory.dseConn.executeGraph(insertContactAndUsersInGroup(userV, groupV, BELONGS_TO, nickname))
      case UNFOLLOWS =>
        DseGraphFactory.dseConn.executeGraph(removeFromGroup(followerUserId, followingUserId))
    }
  }

  /**
   * Checks the existence of the group
   *
   * @param userId  of the owner of the group
   * @param groupId of the group to be validated
   * @return true if the group exists and is owned by the person whose user id has been given or not
   */
  def isGroupExist(userId: String, groupId: String): Boolean = {

    try {
      val edgeExistQuery = new SimpleGraphStatement(
        s"g.V().hasLabel('$USER').has('$USER_ID','$userId').outE('$CREATES_A_GROUP').inV().has('$GROUP_ID','$groupId').hasNext()"
      )
      DseGraphFactory.dseConn.executeGraph(edgeExistQuery).one.asBoolean()
    } catch {
      case ex: Exception => false
    }
  }

  /**
   * To remove the group
   *
   * @param groupId to be removed
   * @return true if group is removed else false
   */
  def removeGroup(groupId: String, userId: String): Boolean = {

    val removeGroupQuery = new SimpleGraphStatement(s"g.V().hasLabel('$DSE_GROUP').has('$GROUP_ID','$groupId').drop()")
    DseGraphFactory.dseConn.executeGraph(removeGroupQuery).all().isEmpty
  }

  def createGroup(userId: String, group: Group): Option[String] = {
    try {
      val groupCreationQuery =
        s"""graph.addVertex(label,"$DSE_GROUP","$GROUP_ID","${group.id}",
           |"$TITLE","${group.title.replace("$", "\\$").replace(QUOTES, QUOTES_WITH_ESCAPE)}","nbContacts",0,"nbFollowers",0,"nbTotal",0)""".stripMargin
      val groupVertex: Vertex = DseGraphFactory.dseConn.executeGraph(groupCreationQuery).one().asVertex()
      val userVetex = DseGraphFactory.dseConn.executeGraph(getUser(userId)).one().asVertex()
      val groupEdge = DseGraphFactory.dseConn.executeGraph(executeGraphStatement(userVetex, groupVertex, CREATES_A_GROUP)).one().asEdge().getLabel
      if (groupEdge.equals(CREATES_A_GROUP)) {
        Some("Group created")
      } else {
        None
      }
    } catch {
      case ex: Exception => None
    }
  }

  /**
   *
   * @param user_Id of the owner of the group
   * @param group   id and the new title of the group
   * @return true if the group title is updated else false
   */
  def updateGroup(user_Id: String, group: Group): Boolean = {

    try {
      val groupUpdateQuery =
        s"""g.V().hasLabel("$DSE_GROUP").has("$GROUP_ID", "${group.id}")
           |.property("$TITLE","${group.title.replace("$", "\\$").replace(QUOTES, QUOTES_WITH_ESCAPE)}")""".stripMargin
      DseGraphFactory.dseConn.executeGraph(groupUpdateQuery).one().asVertex().getProperty(s"$TITLE").getValue.asString().equals(group.title)
    } catch {
      case ex: Exception => false
    }
  }

  /**
   * Get list of already added contect and usersIds in group
   *
   * @param createrUserId
   * @param groupId
   * @param userIdList
   * @param contactList
   * @return
   */

  def validateUsersOrContactByGroupId(createrUserId: String, groupId: String, userIdList: List[String],
    contactList: List[String]): (List[String], List[String]) = {
    val listOfContacts: List[GraphNode] = DseGraphFactory.dseConn.executeGraph(getListOfContacts(groupId)).asScala.toList
    val listOfUserId: List[GraphNode] = DseGraphFactory.dseConn.executeGraph(getListOfUserId(groupId)).asScala.toList
    val alReadyContacts = listOfContacts.map(res => res.asString()) intersect contactList
    val s: List[String] = listOfUserId.map(res => res.asString())
    val alReadyUserId = listOfUserId.map(res => res.asString()) intersect userIdList
    (alReadyContacts, alReadyUserId)
  }

  /**
   * Insert followers in  group
   *
   * @param createrUserId
   * @param userGroupDetails
   * @return
   */
  def insertFollowersInGroup(createrUserId: String, userGroupDetails: UserGroup): (List[String], List[String]) = {
    val groupVertex = DseGraphFactory.dseConn.executeGraph(fetchGroup(userGroupDetails.groupId)).one().asVertex()
    val insertedFollowersContacts: List[String] = if (userGroupDetails.userIds.nonEmpty) {
      val listOfFollowerContacts: List[String] = userGroupDetails.userIds map { userIds =>
        {
          if (isexistsContactOrFollower(userIds, createrUserId, FOLLOWS)) {
            val followerVextex = DseGraphFactory.dseConn.executeGraph(getUser(userIds)).one().asVertex()
            if (DseGraphFactory.dseConn.executeGraph(insertContactAndUsersInGroup(followerVextex, groupVertex,
              BELONGS_TO, followerVextex.getProperty(NICKNAME).getValue.asString)).one().isEdge) {
              DseGraphFactory.dseConn.executeGraph(fetchFollowerContact(userIds)).one().asString()
            } else ZERO
          } else {
            userIds
          }
        }
      }
      listOfFollowerContacts.filter(x => x != ZERO)
    } else Nil
    insertContactsInGroup(userGroupDetails, insertedFollowersContacts, createrUserId, groupVertex)
  }

  /**
   * Insert contact numbers in private group
   *
   * @param userGroupDetails
   * @param insertedFollowersContacts
   * @param createrUserId
   * @param groupVertex
   * @return List of successfully inserted number in boolean
   */
  def insertContactsInGroup(userGroupDetails: UserGroup, insertedFollowersContacts: List[String],
    createrUserId: String, groupVertex: Vertex): (List[String], List[String]) = {
    val insertedContacts: List[String] = if (userGroupDetails.contacts.nonEmpty) {
      val validListOfContact = userGroupDetails.contacts.filterNot(x => insertedFollowersContacts contains x.phone.substring(1))
      validListOfContact map { contact =>
        if (isexistsContactOrFollower(contact.phone.substring(1), createrUserId, HAS_A_CONTACT)) {
          val mobileVertex = DseGraphFactory.dseConn.executeGraph(fetchContactMobile(contact.phone.substring(1))).one().asVertex()
          if (DseGraphFactory.dseConn.executeGraph(insertContactAndUsersInGroup(mobileVertex, groupVertex, BELONGS_TO, contact.name)).one().isEdge) "true"
          else "false"
        } else contact.phone
      }
    } else Nil
    (insertedContacts.filter(x => x != "true"), userGroupDetails.userIds intersect insertedFollowersContacts)
  }

  /**
   * Remove list of users or contacts from a particular group
   *
   * @param createrUserId
   * @param userToBeRemovedList
   * @param contactsToBeRemovedList
   * @param groupid
   * @return true if all are removed else false
   */
  def removeFollowersFromGroup(createrUserId: String, userToBeRemovedList: List[String],
    contactsToBeRemovedList: List[String], groupid: String): Boolean = {

    val removeContact = removeUserIdAndContactsFromGroup(contactsToBeRemovedList, groupid, MOBILE_NO, PHONE_NO)
    val removeUser = removeUserIdAndContactsFromGroup(userToBeRemovedList, groupid, USER, USER_ID)
    removeContact && removeUser
  }

  private def removeUserIdAndContactsFromGroup(listTobeRemoved: List[String], groupId: String, labelName: String, propertyName: String): Boolean = {
    try {
      val query =
        s""" g.V().hasLabel('$DSE_GROUP').has('$GROUP_ID','$groupId').inE('$BELONGS_TO').as('e')
            .outV().hasLabel('$labelName').has('$propertyName',within(${listTobeRemoved.map(x => "'" + x + "'").mkString(",")})).select('e').drop()""".stripMargin
      DseGraphFactory.dseConn.executeGraph(query).all().isEmpty
    } catch {
      case ex: Exception => false
    }
  }

  /**
   * check if user is a follower or contact exists in users contact list
   *
   * @param userIdsContacts
   * @param createruserId
   * @param status
   * @return true or false
   */
  def isexistsContactOrFollower(userIdsContacts: String, createruserId: String, status: String): Boolean = {
    val isexists = status match {
      case FOLLOWS => DseGraphFactory.dseConn.executeGraph(isFollower(userIdsContacts, createruserId)).one().asBoolean()
      case HAS_A_CONTACT => DseGraphFactory.dseConn.executeGraph(isContactExists(createruserId, userIdsContacts)).one().asBoolean()
    }
    isexists
  }

  /**
   * Get users minimal details
   *
   * @param targetUserId user id
   * @return user's minimal details
   */
  def getUserMinimalDetails(targetUserId: String): Option[UserMinimalDetailsResponse] = {

    try {
      val UserV = DseGraphFactory.dseConn.executeGraph(getUser(targetUserId)).one().asVertex()
      Some(UserMinimalDetailsResponse(
        UserV.getProperty(USER_ID).getValue.asString(),
        UserV.getProperty(NICKNAME).getValue.asString(),
        UserV.getProperty(GENDER).getValue.asString(),
        UserV.getProperty(PICTURE).getValue.asString()
      ))
    } catch {
      case ex: Exception => None
    }
  }

  /**
   * This function will get an user's full profile.
   *
   * @param userId
   * @return
   */
  def viewFullUserProfile(targetUserId: String, userId: String): Option[UserProfileFullDetails] = {
    try {
      val followerCount = DseGraphFactory.dseConn.executeGraph(getFollowerCount(targetUserId)).one().asLong()
      val followingCount = DseGraphFactory.dseConn.executeGraph(getFollowingCount(targetUserId)).one().asLong()
      val spokCount = DseGraphFactory.dseConn.executeGraph(getSpokCount(targetUserId)).one().asLong()
      val UserV = DseGraphFactory.dseConn.executeGraph(getUser(targetUserId)).one().asVertex()
      val isFollower = DseGraphFactory.dseConn.executeGraph(getIsFollower(targetUserId, userId)).one().asBoolean()
      val isFollowing = DseGraphFactory.dseConn.executeGraph(getIsFollowing(targetUserId, userId)).one().asBoolean()
      Some(UserProfileFullDetails(
        targetUserId,
        UserV.getProperty(NICKNAME).getValue.asString(),
        UserV.getProperty(GENDER).getValue.asString(),
        UserV.getProperty(PICTURE).getValue.asString(),
        UserV.getProperty(COVER).getValue.asString(),
        followerCount, followingCount, spokCount, isFollower, isFollowing
      ))
    } catch {
      case ex: Exception => None
    }
  }

  /**
   * This function will get the list of the followers of an user.
   *
   * @param userId User's identifier.
   * @param pos    Pagination position identifier.
   * @return list of followers.
   */
  def fetchFollowers(userId: String, pos: String): Option[UserFollowers] = {
    try {
      val followersPerPage = pageSize
      val from = (pos.toInt - 1) * followersPerPage
      val to = from + followersPerPage
      val followers: List[Follow] = userFollowers(userId, from, to)
      val (previous, next): (String, String) = if (DseGraphFactory.dseConn.executeGraph(getFollowers(userId, to, to + 2)).asScala.toList.isEmpty) {
        ((pos.toInt - 1).toString, "")
      } else {
        ((pos.toInt - 1).toString, (pos.toInt + 1).toString)
      }
      Some(UserFollowers(previous, next, followers))
    } catch {
      case ex: Exception => None
    }
  }

  /**
   * This methods is used to disable user's account
   * @param targetUserId user id who's account is being disabled
   * @return true when account is disabled , false when account is not disabled
   */

  def disableUserAccount(targetUserId: String, userV: Vertex): Boolean = {
    val spoker_mobileNo = DseGraphFactory.dseConn.executeGraph(getUserMobileNo(targetUserId)).one().asString()
    val user = userDisableDetails(

      userV.getProperty(NICKNAME).getValue.asString(),
      userV.getProperty(USER_BIRTHDATE).getValue.asString(),
      userV.getProperty(LOCATIONRESULT).getValue.asString(),
      userV.getProperty(GENDER).getValue.asString(),
      spoker_mobileNo,
      userV.getProperty(USER_ID).getValue.asString(),
      userV.getProperty(PICTURE_TYPE).getValue.asString(),
      userV.getProperty(COVER).getValue.asString(),
      userV.getProperty(GEOTEXT).getValue.asString(),
      userV.getProperty(LEVEL).getValue.asString()
    )
    try {
      val result: ResultSet = insertIntoArchive(targetUserId, spoker_mobileNo, user)
      val deleteUserResponse = deleteUserVertax(targetUserId)
      true
    } catch {

      case ex: Exception => false
    }
  }

  private def insertIntoArchive(spokerid: String, mobileno: String, user: userDisableDetails): ResultSet = {
    val timestamp = System.currentTimeMillis()
    val dataJson: String = write(user)
    val archiveJson = write(ArchiveData(spokerid, timestamp, dataJson, mobileno))
    insertHistoryByBinding(archiveJson, archiveDetails)

  }

  private def deleteUserVertax(userId: String): GraphResultSet = {
    DseGraphFactory.dseConn.executeGraph(deleteCreatesAGroup(userId))
    DseGraphFactory.dseConn.executeGraph(deleteActivityGeo(userId))
    DseGraphFactory.dseConn.executeGraph(deleteHasSettings(userId))
    DseGraphFactory.dseConn.executeGraph(deleteHasA(userId))
    DseGraphFactory.dseConn.executeGraph(deleteUser(userId))
  }

  private def userFollowers(userId: String, from: Int, to: Int): List[Follow] = {
    val followersVertex = DseGraphFactory.dseConn.executeGraph(getFollowers(userId, from, to)).asScala.toList
    followersVertex map { followerVertex =>
      val followerV = followerVertex.asVertex()
      Follow(
        followerV.getProperty(USER_ID).getValue.asString(),
        followerV.getProperty(NICKNAME).getValue.asString(),
        followerV.getProperty(GENDER).getValue.asString(),
        followerV.getProperty(PICTURE).getValue.asString()
      )
    }
  }

  /**
   * This function will get the list of the followings of an user.
   *
   * @param userId User's identifier.
   * @param pos    Pagination position identifier.
   * @return list of followings.
   */
  def fetchFollowings(userId: String, pos: String): Option[UserFollowings] = {
    try {
      val followingsPerPage = pageSize
      val from = (pos.toInt - 1) * followingsPerPage
      val to = from + followingsPerPage
      val followings: List[Follow] = userFollowings(userId, from, to)
      val (previous, next): (String, String) = if (DseGraphFactory.dseConn.executeGraph(getFollowings(userId, to, to + 2)).asScala.toList.isEmpty) {
        ((pos.toInt - 1).toString, "")
      } else {
        ((pos.toInt - 1).toString, (pos.toInt + 1).toString)
      }
      Some(UserFollowings(previous, next, followings))
    } catch {
      case ex: Exception => None
    }
  }

  private def userFollowings(userId: String, from: Int, to: Int): List[Follow] = {
    val followingsVertex = DseGraphFactory.dseConn.executeGraph(getFollowings(userId, from, to)).asScala.toList
    followingsVertex map { followingVertex =>
      val followingV = followingVertex.asVertex()
      Follow(
        followingV.getProperty(USER_ID).getValue.asString(),
        followingV.getProperty(NICKNAME).getValue.asString(),
        followingV.getProperty(GENDER).getValue.asString(),
        followingV.getProperty(PICTURE).getValue.asString()
      )
    }
  }

  /**
   * Method to fetch the details of all the groups created by user
   *
   * @param userId
   * @return
   */
  def fetchGroupDetailsForAUser(userId: String, pos: String): Option[GroupsResponse] = {
    try {
      val validPos = if ((pos.toInt - 1) < 1) 1 else pos.toInt
      val groupLimit = groupSize
      val groupPerPage: Int = groupLimit
      val from = (validPos - 1) * groupPerPage
      val to = from + groupPerPage + 1
      val userGroupsList: List[GraphNode] = DseGraphFactory.dseConn.executeGraph(getGroups(userId, from, to)).asScala.toList
      val userGroups = if (userGroupsList.size > groupLimit) {
        userGroupsList.dropRight(1)
      } else {
        userGroupsList
      }
      val response = userGroups.par map { groupNode =>
        val groupVertex = groupNode.asVertex()
        val groupId = groupVertex.getProperty(GROUP_ID).getValue.asString()
        val groupTitle = groupVertex.getProperty(TITLE).getValue.asString()
        val query = countMembersAndContact(groupId, userId)
        val counts = DseGraphFactory.dseConn.executeGraph(query).asScala.toList
        val (numberOfMembers, numberOfContacts) = if (counts.nonEmpty) {
          val totalUsersCount = (parse(counts(0).toString) \ (s"$TOTALUSERSCOUNT")).extract[Int]
          val totalContactsCount = (parse(counts(0).toString) \ (s"$TOTALCONTACTSCOUNT")).extract[Int]
          (totalUsersCount, totalContactsCount)
        } else (0, 0)
        val totalNumberOfUsers = numberOfMembers + numberOfContacts
        val nicknamesOfLastTenUsers = fetchNamesOfUsersAndContacts(groupId, userId)
        UserGroupsDetails(groupId, groupTitle, nicknamesOfLastTenUsers, totalNumberOfUsers, numberOfMembers, numberOfContacts)
      }
      val (previous, next): (String, String) = getPaginationNumber(validPos, userGroupsList.size)
      val result = Some(GroupsResponse(previous, next, response.toList))
      result
    } catch {
      case ex: Exception => None
    }
  }
  private def getPaginationNumber(pos: Int, listSize: Int): (String, String) = {
    val limit = groupSize
    if (listSize > limit) {
      (pos.toString, (pos + 1).toString)
    } else if (pos - 1 < 1) {
      ((pos).toString, "")
    } else {
      ((pos - 1).toString, "")
    }
  }

  private def fetchNamesOfUsersAndContacts(groupId: String, userId: String) = {

    DseGraphFactory.dseConn.executeGraph(getLastTenUsers(groupId, userId)).asScala.toList.map {
      userNode =>
        val userVertex = userNode.asVertex()
        try {
          userVertex.getProperty(NICKNAME).getValue.asString()
        } catch {
          case ex: Exception => {
            val contactNumber = userVertex.getProperty(PHONE_NO).getValue.asString()
            DseGraphFactory.dseConn.executeGraph(getContactName(contactNumber, groupId, userId)).one().asEdge()
              .getProperty(NICKNAME).getValue.asString()
          }
        }
    }
  }

  /**
   * Updates user settings
   *
   * @param userId             current user ID
   * @param UpdatedUserSetting case class userSettings
   * @return true if updates successfully else false
   */
  def updateUserSettings(userId: String, UpdatedUserSetting: UserSetting): Boolean = {
    val updatedVertex = DseGraphFactory.dseConn.executeGraph(updateSettings(userId, UserSetting(
      UpdatedUserSetting.followers,
      UpdatedUserSetting.following
    ))).one().asVertex()
    if (updatedVertex.getProperty(FOLLOWER).getValue.asBoolean() == UpdatedUserSetting.followers &&
      updatedVertex.getProperty(FOLLOWINGS).getValue.asBoolean() == UpdatedUserSetting.following) {
      true
    } else {
      false
    }
  }

  /**
   * To fetch user settings from setting vertex
   *
   * @param userId      userId of current user
   * @param settingName Setting name like followers,followings or help
   * @return Boolean value
   */
  def fetchUserSettings(userId: String, settingName: String): Boolean = {
    DseGraphFactory.dseConn.executeGraph(getUserSettings(userId, settingName)).one().asBoolean()
  }

  def fetchUserAccountDisableOrNot(targetUserId: String): Option[Vertex] = {
    try {
      Some(DseGraphFactory.dseConn.executeGraph(getUser(targetUserId)).one().asVertex())
    } catch {
      case ex: Exception => None
    }
  }

  def isUserSuspendAlready(targetUserId: String): Option[Boolean] = {
    try {
      val userV = DseGraphFactory.dseConn.executeGraph(getUser(targetUserId)).one().asVertex()
      val isSuspended = userV.getProperty(IS_SUSPENDED).getValue.asBoolean()
      Some(isSuspended)
    } catch {
      case ex: Exception => None
    }
  }

  def suspendUserAccount(targetId: String): Option[Boolean] = {
    try {
      val userV = DseGraphFactory.dseConn.executeGraph(suspendUser(targetId)).one().asVertex()
      if (userV.getProperty(IS_SUSPENDED).getValue.asBoolean()) Some(true) else None
    } catch {
      case ex: Exception => None
    }
  }

  def reactiveUserAccount(targetId: String): Option[Boolean] = {
    try {
      val userV = DseGraphFactory.dseConn.executeGraph(reactivateUser(targetId)).one().asVertex()
      if (!userV.getProperty(IS_SUSPENDED).getValue.asBoolean()) Some(true) else None
    } catch {
      case ex: Exception => None
    }
  }

  def checkUserAdminOrNot(userId: String): Option[Boolean] = {
    try {
      val isAdmin = DseGraphFactory.dseConn.executeGraph(getUserAdminOrNot(userId)).one().asBoolean()
      Some(isAdmin)

    } catch {
      case ex: Exception => None
    }

  }

  def checkUserSuperAdminOrNot(userId: String): Option[Boolean] = {
    try {
      val isSuperAdmin = DseGraphFactory.dseConn.executeGraph(isUserSuperAdmin(userId)).one().asBoolean()
      Some(isSuperAdmin)

    } catch {
      case ex: Exception => None
    }
  }

  def setUserLevel(targetUserId: String, level: String): Option[Boolean] = {
    try {
      val userV: Vertex = DseGraphFactory.dseConn.executeGraph(setLevel(targetUserId, level)).one().asVertex()
      if (userV.getProperty(LEVEL).getValue.asString == level) Some(true) else Some(false)
    } catch {
      case ex: Exception => None
    }
  }

  def fetchUserLevel(userId: String): Option[String] = {
    try {
      val userLevel = DseGraphFactory.dseConn.executeGraph(getUserLevel(userId)).one().asString()
      Some(userLevel)

    } catch {
      case ex: Exception => None
    }
  }

  def performCleanUp(userId: String): Boolean = {
    try {
      DseGraphFactory.dseConn.executeGraph(disableSpok(userId))
      deletePopularSpokers(popularSpoker, userId)
      val spokes = "'" + DseGraphFactory.dseConn.executeGraph(getAllSpoksId(userId)).all().asScala.toList.map(spok => spok.asString()).mkString("','") + "'"
      deleteSpokes(trendySpok, spokes)
      deleteFromSearchTalkers(userId)
      removeUserIdTalkFromRedis(userId)
      true
    } catch {
      case ex: Exception =>
        logger.info("Exception occured " + ex.getMessage)
        false
    }
  }

  private def removeUserIdTalkFromRedis(userId: String) = {
    val key1 = "*" + userId
    val key2 = userId + "*"
    try {
      client.keys((TALK_ + key1)).flatMap {
        case List(key) => client.del(key)(timeout)
        case nill => client.keys((TALK_ + key2)).map {
          case List(key) => client.del(key)(timeout)
          case nill => // Do Nothing
        }
      }
    } catch {
      case ex: Exception =>
        logger.info("error while fetching data from redis" + ex.getMessage)
    }
  }
  def getUserIdByMobileNumber(number: String): String = {
    DseGraphFactory.dseConn.executeGraph(getUserByMobileNumber(number)).one().asString()
  }

  /**
   * Updates user Help settings
   *
   * @param userId userId of the Updating user
   * @return true if updates successfully else false
   */
  def updateUserHelpSetting(userId: String): Boolean = {

    try {
      val userSetting = DseGraphFactory.dseConn.executeGraph(getUserSettings(userId, HELP)).one().asBoolean()
      val settingUpdated = DseGraphFactory.dseConn.executeGraph(updateHelpSettings(userId, !userSetting)).one().asBoolean()
      userSetting != settingUpdated
    } catch {
      case ex: Exception => false
    }
  }
  /**
   * dseConn
   * This function will update user's phone number.
   *
   * @param userId    user's id
   * @param newNumber user new number
   * @return true if number updated successfully else false
   */
  def updatePhoneNumber(userId: String, newNumber: String): Boolean = {
    try {
      val updatedVertex = DseGraphFactory.dseConn.executeGraph(updateMobileNumber(userId, newNumber)).one().asVertex()
      if (updatedVertex.getProperty(PHONE_NO).getValue.asString().equals(newNumber)) {
        true
      } else {
        false
      }
    } catch {
      case ex: Exception => false
    }
  }

  /**
   * This method will validate userId .
   *
   * @param userId
   * @return
   */
  def isValidUserId(userId: String): Boolean = DseGraphFactory.dseConn.executeGraph(isValidUserIdQuery(userId)).one().asBoolean()

  /**
   * This function will get my details.
   *
   * @param userId
   * @return
   */

  def viewMyProfile(userId: String): Option[MyDetails] = {

    try {
      val followerCount = DseGraphFactory.dseConn.executeGraph(getFollowerCount(userId)).one().asLong()
      val followingCount = DseGraphFactory.dseConn.executeGraph(getFollowingCount(userId)).one().asLong()
      val spokCount = DseGraphFactory.dseConn.executeGraph(getSpokCount(userId)).one().asLong()
      val UserV = DseGraphFactory.dseConn.executeGraph(getUser(userId)).one().asVertex()
      Some(MyDetails(
        userId,
        UserV.getProperty(NICKNAME).getValue.asString(),
        UserV.getProperty(GENDER).getValue.asString(),
        UserV.getProperty(PICTURE).getValue.asString(),
        UserV.getProperty(COVER).getValue.asString(),
        followerCount, followingCount, spokCount
      ))
    } catch {
      case ex: Exception => None
    }
  }

  /**
   *
   * @param userId of the user
   * @return the nickname for the user whose userId has been sent or return None if no user against this userId
   */
  def getNickname(userId: String): Option[String] =
    try {
      Some(DseGraphFactory.dseConn.executeGraph(getUser(userId)).one().asVertex().getProperty(NICKNAME).getValue.asString())
    } catch {
      case ex: Exception => None
    }

  /**
   *
   * @param userId
   * @param groupId for which the details have to be fetched
   * @param pos
   * @return the details of the group in case of success or error in failure
   */
  def getSingleGroupDetails(userId: String, groupId: String, pos: String): (Option[SingleGroupDetails], Option[Error]) = {
    val specificGroup =
      try {
        Some(DseGraphFactory.dseConn.executeGraph(getGroupWithId(userId, groupId)).one().asVertex())
      } catch {
        case ex: Exception => None
      }
    logger.info("specificGroup exists ::: " + specificGroup.isDefined)
    specificGroup match {
      case Some(groupVertex) => fetchSpecificGroupDetails(userId, groupId, pos, groupVertex)
      case None => (None, Some(Error(GRP_001, GROUP_NOT_FOUND)))
    }
  }

  private def fetchSpecificGroupDetails(userId: String, groupId: String, pos: String, specificGroup: Vertex): (Option[SingleGroupDetails], Option[Error]) = {
    try {
      logger.info("starting to get details ::: " + groupId)
      val followingsPerPage = 20
      val from = (pos.toInt - 1) * followingsPerPage
      val to = from + followingsPerPage
      val groupTitle = specificGroup.getProperty(TITLE).getValue.asString()
      logger.info("groupTitle ::: " + groupTitle)
      val numberOfFollowers = specificGroup.getProperty("nbFollowers").getValue.asInt()
      val numberOfContacts = specificGroup.getProperty("nbContacts").getValue.asInt()
      val totalNumberOfUsers = specificGroup.getProperty("nbTotal").getValue.asInt()
      logger.info("numberOfFollowers ::: " + numberOfFollowers)
      logger.info("getSpecificGroupFollowersAndContacts ::: " + getSpecificGroupFollowersAndContacts(groupId, userId, to, to + 2))
      val (previous, next): (String, String) = if (totalNumberOfUsers <= (followingsPerPage * pos.toInt)) {
        ((pos.toInt - 1).toString, "")
      } else {
        ((pos.toInt - 1).toString, (pos.toInt + 1).toString)
      }
      logger.info("previous ::: " + previous)
      logger.info("next ::: " + next)

      val contactAndFollowersDetails = fetchFollowersAndContactsForSpecificGroup(groupId, userId, from, to)
      (Some(SingleGroupDetails(groupId, groupTitle, previous, next, totalNumberOfUsers, numberOfContacts,
        numberOfFollowers, contactAndFollowersDetails)), None)
    } catch {
      case ex: Exception => (None, Some(Error(GRP_107, s"Unable to load group $groupId")))
    }
  }

  private def fetchFollowersAndContactsForSpecificGroup(groupId: String, userId: String, from: Int, to: Int): List[SpecificGroupResponse] = {
    DseGraphFactory.dseConn.executeGraph(getSpecificGroupFollowersAndContacts(groupId, userId, from, to)).asScala.toList.par.map {
      userNode =>
        val userVertex = userNode.asVertex()
        val label = userVertex.getLabel
        logger.info("label ::: " + label)
        if (label.equals(USER)) {
          val id = userVertex.getProperty(USER_ID).getValue.asString()
          val nickname = userVertex.getProperty(NICKNAME).getValue.asString()
          val gender = userVertex.getProperty(GENDER).getValue.asString()
          val picture = userVertex.getProperty(PICTURE).getValue.asString()
          FollowerDetailsForSingleGroup("spoker", id, nickname, gender, picture)
        } else {
          val contactNumber = userVertex.getProperty(PHONE_NO).getValue.asString()
          val nickname = DseGraphFactory.dseConn.executeGraph(getContactName(contactNumber, groupId, userId)).one().asEdge()
            .getProperty(NICKNAME).getValue.asString()
          ContactDetailsForSingleGroup("contact", nickname, contactNumber)
        }
    }.toList
  }

  def updateUserCountInGroup(groupId: String, userId: String): GraphResultSet = {
    val query: String = countMembersAndContact(groupId, userId)
    val counts: List[GraphNode] = DseGraphFactory.dseConn.executeGraph(query).asScala.toList
    val (numberOfFollowers, numberOfContacts) = if (counts.nonEmpty) {
      val totalUsersCount = (parse(counts(0).toString) \ (s"$TOTALUSERSCOUNT")).extract[Int]
      val totalContactsCount = (parse(counts(0).toString) \ (s"$TOTALCONTACTSCOUNT")).extract[Int]
      (totalUsersCount, totalContactsCount)
    } else (0, 0)
    logger.info("number of followers ::: " + numberOfFollowers)
    logger.info("number of contacts ::: " + numberOfContacts)
    DseGraphFactory.dseConn.executeGraph(updateMemberAndCountInGroupQuery(groupId, userId,
      numberOfFollowers, numberOfContacts, numberOfFollowers + numberOfContacts))
  }

  def isUniqueNickname(nickName: String): Boolean = {
    DseGraphFactory.dseConn.executeGraph(isValidNickNameQuery(nickName)).one().asBoolean()
  }

  def isUniqueUserNickname(nickName: String, userId: String): Boolean = {
    val userIdWithNicknameExists = DseGraphFactory.dseConn.executeGraph(nickNameQuery(nickName)).iterator().hasNext
    if (userIdWithNicknameExists) {
      !userId.equals(DseGraphFactory.dseConn.executeGraph(nickNameQuery(nickName)).one().asString())
    } else {
      false
    }
  }

  def updateAllGroupsAfterUserUnfollowed(userId: String): ParSeq[GraphResultSet] = {
    DseGraphFactory.dseConn.executeGraph(getAllGroups(userId)).asScala.toList.par.map {
      userNode =>
        val groupId = userNode.asVertex().getProperty(GROUP_ID).getValue.asString()
        updateUserCountInGroup(groupId, userId)
    }
  }

  def fetchMyProfile(userId: String): (Option[LoggedUsersDetails], Option[Error], Option[Vertex]) = {
    try {
      val UserV: Vertex = DseGraphFactory.dseConn.executeGraph(getUser(userId)).one().asVertex()
      val userMobileNo = DseGraphFactory.dseConn.executeGraph(getUserMobileNo(userId)).one().asString()
      val (lat, long, elev) = try {
        val locationResult = UserV.getProperty("locationResult").getValue.asString()
        val subLocation = locationResult.substring(locationResult.indexOf("InnerLocation"), locationResult.indexOf("APPROXIMATE"))
        val Array(lat, lang) = subLocation.substring(subLocation.indexOf("(") + 1, subLocation.indexOf(")")).split(",")
        (lat.toDouble, lang.toDouble, 0.0)
      } catch {
        case ex: Exception => {
          val geoDetails = DseGraphFactory.dseConn.executeGraph(getFollowersCurrentGeo(userId)).asScala.toList
          (geoDetails(0).toString.toDouble, geoDetails(1).toString.toDouble, geoDetails(2).toString.toDouble)
        }
      }

      (Some(LoggedUsersDetails(
        userId,
        userMobileNo.substring(0, 2),
        userMobileNo.substring(2),
        UserV.getProperty(USER_BIRTHDATE).getValue.asString(),
        UserV.getProperty(NICKNAME).getValue.asString(),
        UserV.getProperty(GENDER).getValue.asString(),
        Geo(lat, long, elev),
        UserV.getProperty(GEOTEXT).getValue.asString()
      )), None, Some(UserV))
    } catch {
      case ex: Exception => {
        (None, Some(Error(SYST_401, "Not Available")), None)
      }
    }
  }
  def fetchUserInfo(userId: String, userV: Vertex): (Option[SpokerFewDetails], Option[Error]) = {
    try {
      val UserGeoV = DseGraphFactory.dseConn.executeGraph(getUserGeo(userId)).one().asVertex()
      val UserActivityGeo = DseGraphFactory.dseConn.executeGraph(getActivityGeo(userId)).one().asEdge()
      val phoneNumber = DseGraphFactory.dseConn.executeGraph(getUserMobileNo(userId)).one().asString()
      val userProfile = try {
        userV.getProperty("picture").getValue.asString()
      } catch { case ex: Exception => "" }

      val userCover = try {
        userV.getProperty("cover").getValue.asString()
      } catch { case ex: Exception => "" }

      val elevation = UserGeoV.getProperty("elevation").getValue.asDouble()
      val latitude = UserGeoV.getProperty("latitude").getValue.asDouble()
      val longitude = UserGeoV.getProperty("longitude").getValue.asDouble()
      val lastActivity = UserActivityGeo.getProperty("launched").getValue.asString()
      val timestamp = new Timestamp(lastActivity.toLong)
      val date = new Date(timestamp.getTime())
      val tocken = JWTTokenHelper.createJwtTokenWithRole(
        userId, phoneNumber, USER_ROLE
      )
      val geo = Geo(latitude, longitude, elevation)
      val spokerDetails = SpokerFewDetails(userCover, date.toString, geo, userProfile, tocken)
      (Some(spokerDetails), None)
    } catch {
      case ex: Exception => (None, Some(Error(SYST_401, "Not Available")))
    }
  }
}

object DSEGraphPersistenceFactoryApi extends DSEGraphPersistenceFactoryApi

