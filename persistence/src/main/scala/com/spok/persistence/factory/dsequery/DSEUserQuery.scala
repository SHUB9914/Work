package com.spok.persistence.factory.dsequery

import com.datastax.driver.dse.graph.{ GraphResultSet, SimpleGraphStatement, Vertex }
import com.spok.model.Account.UserProfile
import com.spok.model.SpokModel.Geo
import com.spok.persistence.dse.DseGraphFactory
import com.spok.util.Constant._
import com.spok.util.RandomUtil

trait DSEUserQuery extends RandomUtil {
  def logTimeStamp: String = System.currentTimeMillis().toString

  def getUser(userId: String): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId')"""
  }

  def getAnswerVertexQuery(answerId: String): String = {
    s"""g.V().hasLabel('$ANSWER').has('$ID','$answerId')"""
  }

  def getUserVertexFromGroup(userId: String, groupId: String = "0"): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').outE('$CREATES_A_GROUP').inV().has('$GROUP_ID','$groupId').inE('$BELONGS_TO').outV().hasLabel('$USER')"""
  }

  def getMobileVertexFromPrivateGroup(userId: String, groupId: String): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').outE('$CREATES_A_GROUP').inV().has('$GROUP_ID','$groupId').inE('$BELONGS_TO').outV().hasLabel('$MOBILE_NO')"""
  }

  def checkIfUserHasAnsweredQuestion(answerId: String, userId: String): String = {
    s"""g.V().hasLabel('$ANSWER').has('$ID','$answerId').inE('$GIVES_AN_ANSWER').outV().has('$USER_ID','$userId').hasNext()"""
  }

  //has a poll changed to containsA
  def getAllQuestionsVertex(absoluteId: String): String = {
    s"""g.V().has('$SPOK_ID','$absoluteId').outE('$CONATINS_A').inV().outE('$HAS_A_QUESTION').inV()"""
  }

  def getRegisteredUser(userMobile_No: String): String = {
    s"""g.V().hasLabel('$USER').as('userV').outE('$HAS_A_CONTACT').inV().has('$PHONE_NO','$userMobile_No').select('userV').by('$USER_ID')"""
  }

  def executeGraphStatement(vertexFirst: Vertex, vertexSecond: Vertex, edgeLabel: String): SimpleGraphStatement = {
    new SimpleGraphStatement(
      "def v1 = g.V(id1).next()\n" + "def v2 = g.V(id2).next()\n" + "v1.addEdge('" + edgeLabel + "', v2 , '" + DATETIME + "','" + logTimeStamp + "')"
    ).set("id1", vertexFirst.getId()).set("id2", vertexSecond.getId())
  }

  def insertContactAndUsersInGroup(vertexFirst: Vertex, vertexSecond: Vertex, edgeLabel: String, edgeProperty: String): SimpleGraphStatement = {
    val sGraphStmt = new SimpleGraphStatement(
      "def v1 = g.V(id1).next()\n" + "def v2 = g.V(id2).next()\n" +
        "v1.addEdge('" + edgeLabel + "', v2 , '" + DATETIME + "','" + logTimeStamp + s"' , '$NICKNAME' , '" + edgeProperty + "')"
    ).set("id1", vertexFirst.getId()).set("id2", vertexSecond.getId())
    sGraphStmt
  }

  def createActivityGeo(geoLocations: Geo): String = {
    s"""graph.addVertex(label,"$DSE_GEO","latitude",${geoLocations.latitude},
        | "longitude", ${geoLocations.longitude} , "elevation", ${geoLocations.elevation})""".stripMargin
  }

  def getFollowersCurrentGeo(user_Id: String): String = {
    s"""g.V().hasLabel('$USER').has("$USER_ID",'$user_Id').outE('$ACTIVITY_GEO').inV().values()"""
  }

  def validateCommentByIdQuery(commentId: String, commenterId: String): String =
    s"""g.V().has('$COMMENT_ID','$commentId').has('commenterUserId','$commenterId')"""

  def updateCommentQuery(commentId: String, commentText: String, geo: Geo): String =
    s"""g.V().hasLabel('$DSE_COMMENT').has('$COMMENT_ID','$commentId').property('commentText','$commentText').
        |outE('$IS_PRESENT_AT').inV().property('elevation','${geo.elevation}').property('longitude','${geo.longitude}')
        |.property('latitude','${geo.latitude}')
     """.stripMargin

  def getSpokInstanceIdByCommentId(commentId: String): String = {
    s"""g.V().has('$COMMENT_ID','$commentId').inE('$HAS_A_COMMENT').outV().hasLabel('$SPOK_INSTANCE')"""
  }

  def hasUserGivenAnswer(answerId: String, userId: String): String =
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').outE('$GIVES_AN_ANSWER').inV().has('$ID','$answerId').hasNext()"""

  /**
   * This query will update user profile.
   *
   * @param userId      User's id
   * @param userProfile user profile update details
   * @return
   */
  def updateUserProfileQuery(userId: String, userProfile: UserProfile): String = {
    val Profile = userProfile.picture
    val cover = userProfile.cover
    (Profile, cover) match {
      case (None, None) =>
        s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').as('user').property('$NICKNAME','${userProfile.nickname}').
                           |property('$USER_BIRTHDATE','${userProfile.birthDate}').property('$GENDER','${userProfile.gender}').
                           |property('$GEOTEXT','${userProfile.geoText}').
                           |outE('$ACTIVITY_GEO').inV().as('geo').property('$ELEVATION','${userProfile.geo.elevation}').property('$LONGITUDE','${userProfile.geo.longitude}').
                           |property('$LATITUDE','${userProfile.geo.latitude}').select('user','geo')""".stripMargin

      case (None, _) => s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').as('user').property('$NICKNAME','${userProfile.nickname}').
                             |property('$USER_BIRTHDATE','${userProfile.birthDate}').property('$GENDER','${userProfile.gender}').
                             |property('$GEOTEXT','${userProfile.geoText}').property('$COVER','${userProfile.cover.getOrElse("")}').
                             |outE('$ACTIVITY_GEO').inV().as('geo').property('$ELEVATION','${userProfile.geo.elevation}').property('$LONGITUDE','${userProfile.geo.longitude}').
                             |property('$LATITUDE','${userProfile.geo.latitude}').select('user','geo')""".stripMargin

      case (_, None) => s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').as('user').property('$NICKNAME','${userProfile.nickname}').
                          |property('$USER_BIRTHDATE','${userProfile.birthDate}').property('$GENDER','${userProfile.gender}').
                          |property('$PICTURE','${userProfile.picture.getOrElse("")}').property('$GEOTEXT','${userProfile.geoText}').
                          |outE('$ACTIVITY_GEO').inV().as('geo').property('$ELEVATION','${userProfile.geo.elevation}').property('$LONGITUDE','${userProfile.geo.longitude}').
                          |property('$LATITUDE','${userProfile.geo.latitude}').select('user','geo')""".stripMargin

      case _ => s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').as('user').property('$NICKNAME','${userProfile.nickname}').
                    |property('$USER_BIRTHDATE','${userProfile.birthDate}').property('$GENDER','${userProfile.gender}').
                    |property('$PICTURE','${userProfile.picture.getOrElse("")}').property('$GEOTEXT','${userProfile.geoText}').property('$COVER','${userProfile.cover.getOrElse("")}').
                    |outE('$ACTIVITY_GEO').inV().as('geo').property('$ELEVATION','${userProfile.geo.elevation}').property('$LONGITUDE','${userProfile.geo.longitude}').
                    |property('$LATITUDE','${userProfile.geo.latitude}').select('user','geo')""".stripMargin

    }
  }

  /**
   * This query will count number of followers for a user based on user's id.
   *
   * @param userId user's id
   * @return follower count
   */
  def getFollowerCount(userId: String): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').inE('$FOLLOWS').count()"""
  }

  /**
   * This query will count number of following for a user based on user's id.
   *
   * @param userId user's id
   * @return following count
   */
  def getFollowingCount(userId: String): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').outE('$FOLLOWS').count()"""
  }

  /**
   * This query will count number of spok for a user based on user's id.
   *
   * @param userId user's id
   * @return spok count
   */
  def getSpokCount(userId: String): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').outE('$ISASSOCIATEDWITH').has('$STATUS','$RESPOKED').count()"""
  }

  /**
   * This query will fetch list of followers for a user based on user's id.
   *
   * @param userId user's id
   * @return follower count
   */
  def getFollowers(userId: String, fromPosNo: Int, toPosNo: Int): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').inE('$FOLLOWS').outV().order().by('$NICKNAME',incr).range($fromPosNo,$toPosNo)"""
  }

  /**
   * This query will fetch list of following for a user based on user's id.
   *
   * @param userId user's id
   * @return following count
   */
  def getFollowings(userId: String, fromPosNo: Int, toPosNo: Int): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').outE('$FOLLOWS').inV().order().by('$NICKNAME',incr).range($fromPosNo,$toPosNo)"""
  }

  def getGroups(userId: String, from: Int, to: Int): String =
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').outE('$CREATES_A_GROUP').inV().order().by('nbContacts',decr).range($from,$to)"""

  def getAllGroups(userId: String): String =
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').outE('$CREATES_A_GROUP').inV()"""

  def getLastTenUsers(groupId: String, userId: String): String =
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').outE('$CREATES_A_GROUP').inV().has('$GROUP_ID','$groupId')
        |.inE('$BELONGS_TO').order().by('$DATETIME',decr).outV().range(0,10)""".stripMargin

  def getContactName(phoneNumber: String, groupId: String, userId: String): String =
    s"""g.V().hasLabel('$MOBILE_NO').has('$PHONE_NO','$phoneNumber')
        |.outE('$BELONGS_TO').as('e').inV().has('$GROUP_ID','$groupId').select('e')""".stripMargin

  /**
   * This method will validate userId.
   *
   * @param userId
   * @return
   */
  def isValidUserIdQuery(userId: String): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').hasNext()"""
  }

  def getIsFollower(targetUserId: String, userId: String): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').inE('$FOLLOWS').outV().hasLabel('$USER').has('$USER_ID','$targetUserId').hasNext()"""

  }

  def getIsFollowing(targetUserId: String, userId: String): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$targetUserId').inE('$FOLLOWS').outV().hasLabel('$USER').has('$USER_ID','$userId').hasNext()"""
  }

  /**
   * This method will validate userNickname.
   *
   * @param nickName
   * @return
   */
  def isValidNickNameQuery(nickName: String): String = {
    s"""g.V().hasLabel('$USER').has('$NICKNAME','$nickName').hasNext()"""
  }

  def nickNameQuery(nickName: String): String = {
    s"""g.V().hasLabel('$USER').has('$NICKNAME','$nickName').values('$USER_ID')"""
  }

  /**
   * Query to get single group
   *
   * @param userId
   * @param groupId
   * @return
   */
  def getGroupWithId(userId: String, groupId: String): String =
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').out('$CREATES_A_GROUP').has('$GROUP_ID','$groupId')"""

  /**
   * Query to get followers and contacts of a group for a range in order of name
   *
   * @param groupId
   * @param userId
   * @param from
   * @param to
   * @return
   */
  def getSpecificGroupFollowersAndContacts(groupId: String, userId: String, from: Int, to: Int): String =
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').outE('$CREATES_A_GROUP').inV().has('$GROUP_ID','$groupId')
       |.inE('$BELONGS_TO').order().by('$NICKNAME',incr).outV().range($from,$to)""".stripMargin

  /**
   * Query to count followers in a group and contacts in a group
   *
   * @param groupId
   * @param userId
   * @return
   */
  def countMembersAndContact(groupId: String, userId: String): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').match(
        __.as("c").outE('$CREATES_A_GROUP').inV().has('$GROUP_ID','$groupId').in('$BELONGS_TO').hasLabel('$USER').count().as("totalUsers"),
        __.as("c").outE('$CREATES_A_GROUP').inV().has('$GROUP_ID','$groupId').in('$BELONGS_TO').hasLabel('$MOBILE_NO').count().as("totalContacts"))
        .select("$TOTALUSERSCOUNT","$TOTALCONTACTSCOUNT")"""
  }

  /**
   * Query to update the contact count, follower count and total count in group vertex
   *
   * @param groupId
   * @param userId
   * @param followersCount
   * @param contactsCount
   * @param totalCount
   * @return
   */
  def updateMemberAndCountInGroupQuery(groupId: String, userId: String, followersCount: Int, contactsCount: Int, totalCount: Int): String = {
    s"""g.V().hasLabel('$DSE_GROUP').has('$GROUP_ID','$groupId').property('nbFollowers','$followersCount')
       |.property('nbContacts','$contactsCount')
       |.property('nbTotal','$totalCount')
     """.stripMargin
  }

  /**
   * Query to fetch User nickname and PhoneNo
   *
   * @param userId
   * @return
   */
  def getUserNickNameAndPhoneNo(userId: String): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').match(
        __.as("c").values('$NICKNAME').as("userNickname"),
        __.as("c").outE('$HAS_A').inV().values('$PHONE_NO').as("userPhoneNo")).
        select("userNickname", "userPhoneNo")"""
  }

  /**
   * Query to fetch contacts mobile No from a Group
   *
   * @param groupId
   * @return
   */
  def fetchContactsFromGroup(groupId: String): String = {
    s""" g.V().hasLabel('$DSE_GROUP').has('$GROUP_ID','$groupId').inE('$BELONGS_TO').outV().hasLabel('$MOBILE_NO')"""
  }

  /**
   * Query to get user mobile no.
   * @param userId
   * @return
   */
  def getUserMobileNo(userId: String): String = {
    s"g.V().hasLabel('$USER').has('$USER_ID', '$userId').outE('$HAS_A').inV().values('$PHONE_NO')"
  }

  def deleteCreatesAGroup(userId: String): String = {
    s"g.V().hasLabel('$USER').has('$USER_ID', '$userId').outE('$CREATES_A_GROUP').inV().drop()"
  }
  def deleteActivityGeo(userId: String): String = {
    s"g.V().hasLabel('$USER').has('$USER_ID', '$userId').outE('$ACTIVITY_GEO').inV().drop()"
  }
  def deleteHasSettings(userId: String): String = {
    s"g.V().hasLabel('$USER').has('$USER_ID', '$userId').outE('$HAS_SETTINGS').inV().drop()"
  }
  def deleteHasA(userId: String): String = {
    s"g.V().hasLabel('$USER').has('$USER_ID', '$userId').outE('$HAS_A').inV().drop()"
  }
  def deleteUser(userId: String): String = {
    s"g.V().hasLabel('$USER').has('$USER_ID', '$userId').drop()"
  }

  def disableSpok(userId: String): String = {
    s"""g.V().hasLabel('$SPOK').has('$AUTHOR', '$userId').property('$ENABLED','false')"""

  }
  def getAllSpoksId(userId: String): String = {
    s"""g.V().hasLabel('$SPOK').has('$AUTHOR', '$userId').values('$SPOK_ID')"""
  }

  def getUserByMobileNumber(number: String): String = {
    val validMobileNumber = number.substring(1)
    s"""g.V().hasLabel('$MOBILE_NO').has('$PHONE_NO','$validMobileNumber').inE('$HAS_A').outV().values('$USER_ID')"""
  }
  def getUserGeo(userId: String): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').outE('$ACTIVITY_GEO').inV()"""
  }

  def getActivityGeo(userId: String): String =
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').outE('$ACTIVITY_GEO').as('e').inV().select('e')""".stripMargin

  def suspendUser(userId: String): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').property('$IS_SUSPENDED','$TRUE')""".stripMargin
  }
  def reactivateUser(userId: String): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').property('$IS_SUSPENDED','$FALSE')""".stripMargin
  }

}
