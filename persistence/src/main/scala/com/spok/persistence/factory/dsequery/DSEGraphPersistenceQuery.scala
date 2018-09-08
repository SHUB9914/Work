package com.spok.persistence.factory.dsequery

import com.spok.model.SpokModel._
import com.spok.model.Account.{ User, UserSetting }
import com.spok.util.Constant._

trait DSEGraphPersistenceQuery {

  def getUserQuery(userId: String): String = {
    s"g.V().hasLabel('$USER').has('$USER_ID', '$userId')"
  }

  def isEdgeExistsFollows(firstVertexId: String, secondVertexId: String): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$firstVertexId').outE('$FOLLOWS').inV().has('$USER_ID','$secondVertexId').hasNext()"""
  }

  def removeFollowEdge(followerId: String, followingId: String): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$followerId').outE('$FOLLOWS')
        |.as('e').select('e').inV().has('$USER_ID','$followingId').select('e').drop()""".stripMargin
  }

  def fetchGroup(groupId: String): String = {
    s"""g.V().hasLabel("$DSE_GROUP").has("$GROUP_ID",'$groupId')"""
  }

  def fetchUserGroup(userId: String, groupId: String): String = {
    s"""g.V().hasLabel("$USER").has("$USER_ID",'$userId').outE('$CREATES_A_GROUP').inV().has('$GROUP_ID','$groupId')"""
  }

  def isFollower(followerUserId: String, followedUserID: String): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$followerUserId').outE('$FOLLOWS').inV().has('$USER_ID','$followedUserID').hasNext()"""
  }

  def isContactExists(userId: String, follwerContactNo: String): String = {
    s""" g.V().hasLabel('$USER').has('$USER_ID','$userId').outE('$HAS_A_CONTACT').inV().has('$PHONE_NO','$follwerContactNo').hasNext()"""
  }

  def fetchContactMobile(followerContactNo: String): String = {
    s"""g.V().hasLabel('$MOBILE_NO').has('$PHONE_NO','$followerContactNo')"""
  }

  def fetchFollowerContact(followerId: String): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$followerId').outE('$HAS_A').inV().values('phoneNo')"""
  }

  /**
   * Remove follower from all following groups
   *
   * @param followerId
   * @param followingId
   * @return
   */
  def removeFromGroup(followerId: String, followingId: String): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$followerId').outE('$BELONGS_TO').as('e').inV().inE('$CREATES_A_GROUP')
    |.outV().has('$USER_ID','$followingId').select('e').drop()""".stripMargin
  }

  def getAllGroup(followerUserId: String, followedUserID: String): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$followerUserId').outE('$BELONGS_TO').inV().as('id').inE('$CREATES_A_GROUP')
        | .outV().has('$USER_ID','$followedUserID').select('id').by('$GROUP_ID') """.stripMargin
  }

  def getPrivateGroup(userNo: String): String = {
    s"""g.V().hasLabel('$MOBILE_NO').has('$PHONE_NO','$userNo').outE('$BELONGS_TO').inV()"""
  }

  def getPendingInstanceLinked(userNo: String): String = {
    s"""g.V().hasLabel('$MOBILE_NO').has('$PHONE_NO','$userNo').outE('$ISASSOCIATEDWITH').inV()"""
  }

  def getSpokEdgeConnectedToContact(userNo: String, spokId: String): String = {
    s"""g.V().hasLabel('$MOBILE_NO').has('$PHONE_NO','$userNo').outE('$ISASSOCIATEDWITH').as('e').inV().has('$SPOK_ID','$spokId').select('e')"""
  }

  def removeEdgeFromGroup(userNo: String): String = {
    s"""g.V().hasLabel('$MOBILE_NO').has('$PHONE_NO','$userNo').outE('$BELONGS_TO').as('e').select('e').drop()"""
  }

  def removeEdgeFromPendingInstance(userNo: String, spokId: String): String = {
    s"""g.V().hasLabel('$MOBILE_NO').has('$PHONE_NO','$userNo').outE('$ISASSOCIATEDWITH').as('e').inV().has('$SPOK_ID','$spokId').select('e').drop()"""
  }

  /**
   *
   * @param userSetting case class containing follower,following and help parameters.
   * @return In vertex Label
   */
  def createSettingVertexQuery(userSetting: UserSetting, help: Boolean): String = {
    s"""graph.addVertex(label,"$SETTINGS_LABEL",'$FOLLOWER',"${userSetting.followers}",
        | '$FOLLOWINGS', "${userSetting.following}" ,'$HELP','$help')""".stripMargin
  }

  /**
   * Updates user Setting vertex followers and followings property
   *
   * @param userId
   * @param userSetting
   * @return
   */
  def updateSettings(userId: String, userSetting: UserSetting): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').outE('$HAS_SETTINGS').inV().property('$FOLLOWER','${userSetting.followers}').
        |property('$FOLLOWINGS','${userSetting.following}')""".stripMargin
  }

  /**
   * Query to get settings from user's setting vertex
   *
   * @param userId      current user ID
   * @param settingName Setting property value
   * @return
   */
  def getUserSettings(userId: String, settingName: String): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').outE('$HAS_SETTINGS').inV().values('$settingName')"""
  }

  def getUserAdminOrNot(userId: String): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').has('$LEVEL',within('$ADMIN','$SUPERADMIN')).hasNext()"""
  }

  def getUserSuspendOrNot(userId: String): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').has('$IS_SUSPENDED',within('$ADMIN','$SUPERADMIN')).hasNext()"""
  }

  def isUserSuperAdmin(userId: String): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').has('$LEVEL' ,'$SUPERADMIN').hasNext()"""
  }

  def getUserLevel(userId: String): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').values('$LEVEL')"""
  }

  def setLevel(targetUserId: String, level: String): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$targetUserId').property('$LEVEL','$level')"""
  }

  /**
   * This query will update user's phone
   *
   * @param userId      user's id
   * @param phoneNumber user updated number
   * @return true or false
   */
  def updateMobileNumber(userId: String, phoneNumber: String): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').outE('$HAS_A').inV().property('$PHONE_NO','$phoneNumber')""".stripMargin
  }

  /**
   * Query to update user help setting and fetch the updated setting
   *
   * @param userId      Current user ID
   * @param helpSetting help property name
   * @return query
   */
  def updateHelpSettings(userId: String, helpSetting: Boolean): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').outE('$HAS_SETTINGS').inV().property('$HELP','$helpSetting').values('$HELP')""".stripMargin
  }

  /**
   *
   * @param userAttributes user case class
   * @return query to insert user
   */
  def insertUserQuery(userAttributes: User): String = {
    s"""graph.addVertex(label,"$USER","nickname","${userAttributes.nickname}",
        | "birthDate", "${userAttributes.birthDate}" , "locationResult" , "${userAttributes.location.results}"  ,
        | "locationStatus" , "${userAttributes.location.status}"  , "gender" , "${userAttributes.gender}" ,
        | "userId" , "${userAttributes.userId}" , "picture" , "${userAttributes.picture.getOrElse("")}",
        | "cover" , "${userAttributes.cover.getOrElse("")}", "launched" , "${timeStamp}" ,"geoText" , "${userAttributes.geoText}" ,"level" ,"${userAttributes.level}" ,
        | "isSuspended" ,"${userAttributes.isSuspended}" )""".stripMargin
  }

  /**
   * get list of contacts of group
   *
   * @param groupId
   * @return
   */
  def getListOfContacts(groupId: String): String = {
    s"""g.V().hasLabel('$DSE_GROUP').has('$GROUP_ID','$groupId').inE('$BELONGS_TO').outV().values('$PHONE_NO')"""
  }

  /**
   * get list of userId
   *
   * @param groupId
   * @return
   */
  def getListOfUserId(groupId: String): String = {
    s"""g.V().hasLabel('$DSE_GROUP').has('$GROUP_ID','$groupId').inE('$BELONGS_TO').outV().values('$USER_ID')"""
  }

  /**
   * Remove all pending edges between follower and spok when user unfollows
   *
   * @param followerId
   * @param followingId
   * @return
   */
  def removeUnfollowerPendingEdges(followerId: String, followingId: String): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$followerId')
    .outE('$ISASSOCIATEDWITH').has('$FROM','$followingId').has('$STATUS','$PENDING')
    .as('e').select('e').drop()"""
  }

  /**
   * fetch spokId when user is in pending state with spok
   *
   * @param followerId
   * @param followingId
   * @return
   */
  def getSpokIdfromPendingEdges(followerId: String, followingId: String): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$followerId')
    .outE('$ISASSOCIATEDWITH').has('$FROM','$followingId').has('$STATUS','$PENDING').inV().values('$SPOK_ID')"""
  }

  /**
   * fetch next immediate following user
   *
   * @param followerId
   * @param spokId
   * @return
   */
  def getNextImmediateFollowingUser(followerId: String, spokId: String): String = {
    s"""g.V().hasLabel('$USER').has('userId','$followerId').outE('$FOLLOWS').order().by('$LAUNCHED',incr)
       |.range(0,1).inV().outE('$ISASSOCIATEDWITH').as('e').inV().has('$SPOK_ID','$spokId').select('e')""".stripMargin
  }

  /**
   * to check next immediate following user is linked with spok
   *
   * @param followerId
   * @param spokId
   * @return
   */
  def isNextImmediateFollowingUser(followerId: String, spokId: String): String = {
    s"""g.V().hasLabel('$USER').has('userId','$followerId').outE('$FOLLOWS').order().by('$LAUNCHED',incr)
        |.range(0,1).inV().outE('$ISASSOCIATEDWITH').as('e').inV().has('$SPOK_ID','$spokId').select('e').hasNext()""".stripMargin
  }

  /**
   * fetch spok vertex
   *
   * @param spokId
   * @return
   */
  def getSpokVertex(spokId: String): String = {
    s"""g.V().hasLabel('$DSE_SPOK').has('$SPOK_ID','$spokId')"""
  }

}

