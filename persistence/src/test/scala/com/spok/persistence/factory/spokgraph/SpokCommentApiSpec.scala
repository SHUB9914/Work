package com.spok.persistence.factory.spokgraph

import java.util.Date

import com.spok.model.Account.User
import com.spok.model.SpokModel.{ Spok, _ }
import com.spok.model.{ InnerLocation, NorthEast, SouthWest, ViewPort, _ }
import com.spok.persistence.dse.DseGraphFactory
import com.spok.persistence.factory.DSEGraphPersistenceFactoryApi
import com.spok.util.Constant._
import org.scalatest.{ BeforeAndAfterAll, FlatSpec, Matchers }

class SpokCommentApiSpec extends FlatSpec with Matchers with SpokCommentApi with BeforeAndAfterAll {

  val obj = DSEGraphPersistenceFactoryApi
  val spokObj = DSESpokApi
  val date: Date = new java.util.Date()

  val user1Id = getUUID()
  val user1 = User("Cyril", date, Location(List(LocationDetails(
    List(AddressComponents("Marsiele", "Marsiele", List("locality", "political"))),
    " Noida, Uttar Pradesh 201301, India",
    Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(43.2805546, 5.2647101), "APPROXIMATE",
      ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
  )), "OK"),
    "male", List("+919999999999", "+919983899777"), "+919638527401", user1Id, Some("testuser.jpg"))

  val user2Id = getUUID()
  val user2 = User("Kais", date, Location(List(LocationDetails(
    List(AddressComponents("Marsiele", "Marsiele", List("locality", "political"))),
    " Noida, Uttar Pradesh 201301, India",
    Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(43.2805546, 5.2647101), "APPROXIMATE",
      ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
  )), "OK"),
    "male", Nil, "+919999999999", user2Id, Some("testuser.jpg"))

  val spokId1 = getUUID()
  val spok: Spok = Spok("rawtext", None, Some("Public"), Some(1), Some("instanceText"), None, Some("Text"),
    Some(Url("Address", "Title", "text", "previewLink", Some("urlType"))), None, None,
    Geo(27.22, 27.55, 222), spokId1)

  override def beforeAll {
    obj.insertUser(user1)
    obj.insertUser(user2)
    spokObj.createSpok(user1Id, spok)
  }

  override def afterAll: Unit = {
    DseGraphFactory.dseConn.executeGraph("g.V().drop()")
  }

  behavior of "SpokCommentApiSpec "

  it should "be able to comment on spok" in {
    val commentId = getUUID().toString
    val (commentResponse, commentAddedtimestamp, spokError): (Option[SpokCommentResponse], Long, Option[Error]) = addComment(spokId1, commentId, user2Id,
      "Hi this is my first comment", Geo(55.55, 56.56, 66.66), Nil)
    assert(commentResponse.get.spok.spokId == spokId1)
    assert(commentResponse.get.commentId.get == commentId)

  }

  it should "be able to comment on spok with special characters" in {
    val commentId = getUUID().toString
    val (commentResponse, commentAddedtimestamp, spokError): (Option[SpokCommentResponse], Long, Option[Error]) = addComment(spokId1, commentId, user2Id,
      "Hi this is a comment with special chars $ and '", Geo(55.55, 56.56, 66.66), Nil)
    assert(commentResponse.get.spok.spokId == spokId1)
    assert(commentResponse.get.commentId.get == commentId)
    assert(spokError.isEmpty)

  }

  it should "be able to comment on spok with special character double quote" in {
    val commentId = getUUID().toString
    val (commentResponse, commentAddedtimestamp, spokError): (Option[SpokCommentResponse], Long, Option[Error]) = addComment(spokId1, commentId, user2Id,
      """Hi this is a "comment"" with special chars $ and '""", Geo(55.55, 56.56, 66.66), Nil)
    assert(commentResponse.get.spok.spokId == spokId1)
    assert(commentResponse.get.commentId.get == commentId)
    assert(spokError.isEmpty)

  }

  it should "not able to comment on spok" in {
    val commentResponse = addComment("invalid UserId", getUUID(), user1Id,
      "Hi this is my invalid comment", Geo(55.55, 56.56, 66.66), Nil)
    assert(commentResponse._1 == None)

  }

  it should "be able to update comment on spok" in {
    val commentId1 = getUUID()
    val (commentResponse, commentAddedtimestamp, spokError): (Option[SpokCommentResponse], Long, Option[Error]) = addComment(spokId1, commentId1, user2Id,
      "Hi this is my second comment", Geo(51.55, 51.56, 61.66), Nil)
    val commentText = Comment(commentId1, "Hi this is my secound updated comment", Geo(23.22, 23.23, 103), Nil)
    val (commentUpdateResponse, timestamp, error) = updateComment(user2Id, commentText)
    assert(commentUpdateResponse.get.spokId == spokId1)
    assert(commentUpdateResponse.get.commentId.get == commentId1)
  }

  it should "be able to update comment on spok if the update comment text contains special characters" in {
    val commentId1 = getUUID()
    val (commentResponse, commentAddedtimestamp, spokError): (Option[SpokCommentResponse], Long, Option[Error]) = addComment(spokId1, commentId1, user2Id,
      "Hi this is my second comment", Geo(51.55, 51.56, 61.66), Nil)
    val commentText = Comment(commentId1, "Hi this is an update with special chars ' and $.", Geo(23.22, 23.23, 103), Nil)
    val (commentUpdateResponse, timestamp, error) = updateComment(user2Id, commentText)
    assert(commentUpdateResponse.get.spokId == spokId1)
    assert(commentUpdateResponse.get.commentId.get == commentId1)
    assert(error.isEmpty)
  }

  it should "be able to update comment on spok if the update comment text contains special character double quote" in {
    val commentId1 = getUUID()
    val (commentResponse, commentAddedtimestamp, spokError): (Option[SpokCommentResponse], Long, Option[Error]) = addComment(spokId1, commentId1, user2Id,
      "Hi this is my second comment", Geo(51.55, 51.56, 61.66), Nil)
    val commentText = Comment(commentId1, """Hi this is an "update" with special chars ' and $.""", Geo(23.22, 23.23, 103), Nil)
    val (commentUpdateResponse, timestamp, error) = updateComment(user2Id, commentText)
    assert(commentUpdateResponse.get.spokId == spokId1)
    assert(commentUpdateResponse.get.commentId.get == commentId1)
    assert(error.isEmpty)
  }

  it should "not be able to update comment on spok if the comment is not found" in {
    val commentId1 = getUUID()
    val falseCommentId = getUUID()
    val (commentResponse, commentAddedtimestamp, spokError): (Option[SpokCommentResponse], Long, Option[Error]) = addComment(spokId1, commentId1, user2Id,
      "Hi this is my second comment", Geo(51.55, 51.56, 61.66), Nil)
    val commentText = Comment(falseCommentId, "Hi this is my secound updated comment", Geo(23.22, 23.23, 103), Nil)
    val (commentUpdateResponse, timestamp, error) = updateComment(user2Id, commentText)
    assert(commentUpdateResponse.isEmpty)
    assert(error.get.id equals SPK_008)
    assert(error.get.message equals s"Comment $falseCommentId not found")
  }

  it should "be able to remove comment from spok" in {
    val commentId2 = getUUID()
    val geo = Geo(27.22, 27.55, 22)
    addComment(spokId1, commentId2, user2Id,
      "Hi this is my second comment", geo, Nil)
    val (commentRemoveResponse, timestamp, None) = removeComment(commentId2, user2Id, geo)
    assert(commentRemoveResponse.get.spok.spokId == spokId1)
    assert(commentRemoveResponse.get.commentId == commentId2)
  }

}
