package com.spok.persistence.factory.dsequery

import com.spok.model.SpokModel.Geo
import com.spok.util.Constant._
import org.scalatest.{ BeforeAndAfterAll, FlatSpec, Matchers }

class DSEUserQuerySpec extends FlatSpec with Matchers with DSEUserQuery with BeforeAndAfterAll {

  val geo = Geo(43.2805546, 5.2647101, 45.0)

  behavior of "DSEUserQuerySpec "

  it should "be able to build query to validate user Id  " in {

    val result: String = getUser("userid_123456789_user")
    val output = s"""g.V().hasLabel('$USER').has('$USER_ID','userid_123456789_user')"""
    assert(result.equals(output))
  }

  it should "be able to build query to get registered users " in {

    val result: String = getRegisteredUser("919876543210")
    val output =
      s"""g.V().hasLabel('$USER').as('userV').outE('$HAS_A_CONTACT').inV().has('$PHONE_NO','919876543210').select('userV').by('$USER_ID')"""
    assert(result.equals(output))
  }

  it should "be able to build the query to update a comment" in {

    val commentId = getUUID()
    val commentText = "Updated Comment"
    val result = updateCommentQuery(commentId, commentText, geo)
    val output = s"""g.V().hasLabel('comment').has('commentId','$commentId').property('commentText','Updated Comment').
                     |outE('isPresentAt').inV().property('elevation','45.0').property('longitude','5.2647101')
                     |.property('latitude','43.2805546')
     """.stripMargin
    assert(result == output)
  }

  it should "be able to build the query to fetch spok instance given the comment id" in {

    val commentId = getUUID()
    val result = getSpokInstanceIdByCommentId(commentId)
    val output = s"""g.V().has('commentId','$commentId').inE('hasAComment').outV().hasLabel('SpokInstance')"""
    assert(result == output)
  }

  it should "be able to build the query to validate a comment by its id" in {

    val commentId = getUUID()
    val commenterId = getUUID()
    val result = validateCommentByIdQuery(commentId, commenterId)
    val output = s"g.V().has('commentId','$commentId').has('commenterUserId','$commenterId')"
    assert(result === output)
  }

  it should "be able to build the query to get all groups created by a user" in {

    val userId = getUUID()
    val result = getAllGroups(userId)
    val output = s"""g.V().hasLabel('users').has('userId','$userId').outE('createsAGroup').inV()"""
    assert(result === output)
  }

  it should "be able to build the query to get members of a group" in {

    val groupId = getUUID()
    val userId = getUUID()
    val result = countMembersAndContact(groupId, userId)
    val output =
      s"""g.V().hasLabel('users').has('userId','$userId').match(
        __.as("c").outE('createsAGroup').inV().has('groupId','$groupId').in('belongsTo').hasLabel('users').count().as("totalUsers"),
        __.as("c").outE('createsAGroup').inV().has('groupId','$groupId').in('belongsTo').hasLabel('mobileNo').count().as("totalContacts"))
        .select("totalUsers","totalContacts")"""
    assert(result === output)
  }

  it should "be able to build the query to get last ten users" in {

    val groupId = getUUID()
    val userId = getUUID()
    val result = getLastTenUsers(groupId, userId)
    val output =
      s"""g.V().hasLabel('$USER').has('userId','$userId').outE('createsAGroup').inV().has('groupId','$groupId')
         |.inE('belongsTo').order().by('datetime',decr).outV().range(0,10)""".stripMargin
    assert(result === output)
  }

  it should "be able to build the query to get the name of a contact" in {

    val groupId = getUUID()
    val userId = getUUID()
    val result = getContactName("919574755845", groupId, userId)
    val output =
      s"""g.V().hasLabel('mobileNo').has('phoneNo','919574755845')
          |.outE('belongsTo').as('e').inV().has('groupId','$groupId').select('e')""".stripMargin
    assert(result === output)
  }

  it should "be able to build the query to get the one user follows to anothe user" in {

    val targetUserId = getUUID()
    val userId = getUUID()
    val result = getIsFollower(targetUserId, userId)
    val output =
      s"""g.V().hasLabel('users').has('userId','$userId').inE('follows').outV().hasLabel('users').has('userId','$targetUserId').hasNext()""".stripMargin
    assert(result === output)
  }

  it should "be able to build the query to get the one user following to anothe user" in {

    val targetUserId = getUUID()
    val userId = getUUID()
    val result = getIsFollowing(targetUserId, userId)
    val output =
      s"""g.V().hasLabel('users').has('userId','$targetUserId').inE('follows').outV().hasLabel('users').has('userId','$userId').hasNext()""".stripMargin
    assert(result === output)
  }

  it should "be able to build the query to validate unique nickname" in {
    val nickName = "testUser"
    val result = isValidNickNameQuery(nickName)
    val output = s"""g.V().hasLabel('users').has('nickname','$nickName').hasNext()"""
    assert(result === output)
  }

}
