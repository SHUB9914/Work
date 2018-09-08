package com.spok.persistence.factory.spokgraph

import com.datastax.driver.dse.graph.Vertex
import com.spok.model.SpokModel._
import com.spok.persistence.dse.DseGraphFactory
import com.spok.persistence.factory.DSEUserSpokFactoryApi
import com.spok.util.Constant._
import com.spok.util.RandomUtil

import scala.collection.JavaConverters._

trait SpokCommentApi extends DSESpokCommentQuery with DSESpokQuery with DSEUserSpokFactoryApi with RandomUtil {

  /**
   * Add comment on Spok
   *
   * @param spokId
   * @param commentId
   * @param commenterUserId
   * @param commentText
   * @param commenterGeo
   * @param mentionUserId
   * @return SpokCommentResponse case class
   */
  def addComment(spokId: String, commentId: String, commenterUserId: String, commentText: String, commenterGeo: Geo,
    mentionUserId: List[String]): (Option[SpokCommentResponse], Long, Option[Error]) = {

    val (spokExist, spokVer) = validateSpokById(spokId)
    (spokExist, spokVer) match {
      case (SPOK_VALID, Some(spokVertex)) => {
        try {
          val commentTimeStamp = timeStamp
          val commentV: Vertex = DseGraphFactory.dseConn.executeGraph(addCommentQuery(commentId, commenterUserId,
            commentText, commenterGeo, commentTimeStamp)).one().asVertex()
          addEdgeWithTime(spokVertex, commentV, HAS_A_COMMENT, commentTimeStamp)
          // update stats
          val currentSpokStats = DseGraphFactory.dseConn.executeGraph(getSpokStatsVertex(spokId)).one().asVertex()
          //build comment response to be send
          val response = addCommentResponse(spokId, commenterUserId, mentionUserId, commentId, currentSpokStats)
          (Some(response), commentTimeStamp, None)
        } catch {
          case ex: Exception =>
            (None, timeStamp, Some(Error(SPK_119, s"Unable commenting spok $spokId (generic error).")))
        }
      }
      case (_, None) => (None, timeStamp, Some(Error(SPK_001, s"Spok $spokId not found")))
    }
  }

  /**
   * Creating comment response
   *
   * @param spokId
   * @param commenterUserId
   * @param mentionUserId
   * @param commentId
   * @param currentSpokStats
   * @return SpokCommentResponse case class
   */
  private def addCommentResponse(spokId: String, commenterUserId: String, mentionUserId: List[String],
    commentId: String, currentSpokStats: Vertex): SpokCommentResponse = {
    val commnenterDetailsList = DseGraphFactory.dseConn.executeGraph(
      getCommenterDetailsForCommentResponse(commenterUserId)
    ).asScala.toList

    val commenterUserResponse = CommenterUserResponse(commenterUserId, commnenterDetailsList(0).asString(),
      commnenterDetailsList(1).asString(), commnenterDetailsList(2).asString())

    val commentInternalSpokResponse = CommentInternalSpokResponse(
      spokId,
      currentSpokStats.getProperty(NB_RESPOKED).getValue.asString(),
      currentSpokStats.getProperty(NB_USERS).getValue.asString(),
      (currentSpokStats.getProperty(NB_COMMENTS).getValue.asInt() + 1).toString(),
      currentSpokStats.getProperty(TRAVELLED).getValue.asString()
    )
    val spokCommentResponse = SpokCommentResponse(commentInternalSpokResponse, commenterUserResponse, Some(mentionUserId), Some(commentId))
    spokCommentResponse
  }

  /**
   * Method to update the comment
   *
   * @param commenterId   the id of the user updating the comment who is also the user who created the comment
   * @param updateCommentDetails the details of the updated comment
   * @return CommentUpdateResponse if the comment is updated else None
   */
  def updateComment(commenterId: String, updateCommentDetails: Comment): (Option[CommentUpdateResponse], Long, Option[Error]) = {

    try {
      val isCommentUpdated = DseGraphFactory.dseConn.executeGraph(updateUserComment(commenterId, updateCommentDetails.commentId,
        updateCommentDetails.text, updateCommentDetails.geo, timeStamp)).iterator().hasNext
      if (isCommentUpdated) {
        val spokId = DseGraphFactory.dseConn.executeGraph(getSpokIdByCommentId(updateCommentDetails.commentId)).one().asString()
        val currentSpokStats = DseGraphFactory.dseConn.executeGraph(getSpokStatsVertex(spokId)).one().asVertex()
        val updateCommentResponse = CommentUpdateResponse(spokId, currentSpokStats.getProperty(NB_RESPOKED).getValue.asString(),
          currentSpokStats.getProperty(NB_USERS).getValue.asString(), currentSpokStats.getProperty(NB_COMMENTS).getValue.asString(),
          currentSpokStats.getProperty(TRAVELLED).getValue.asString(), Some(updateCommentDetails.mentionUserId), Some(updateCommentDetails.commentId))
        (Some(updateCommentResponse), timeStamp, None)
      } else {
        (None, timeStamp, Some(Error(SPK_008, s"Comment ${updateCommentDetails.commentId} not found")))
      }
    } catch {
      case ex: Exception => (None, timeStamp, Some(Error(SPK_120, s"Unable updating comment ${updateCommentDetails.commentId}(generic error).")))
    }
  }

  /**
   * Method to remove the comment
   *
   * @param commentId comment id to be removed
   * @return RemoveCommentResponse if the comment is updated else None
   */
  def removeComment(commentId: String, userId: String, geo: Geo): (Option[RemoveCommentResponse], Long, Option[Error]) = {
    val commentExist = validateCommentById(commentId, userId)
    val spokCreatorId = getSpokCreaterIdByCommentId(commentId)
    spokCreatorId match {
      case Some(creatorId) => handleValidRemoveCommentRequest(commentExist, creatorId, commentId, userId, geo)
      case None => (None, timeStamp, Some(Error(SPK_121, s"Unable removing comment $commentId(generic error).")))
    }
  }

  private def handleValidRemoveCommentRequest(commentExist: Boolean, creatorId: String, commentId: String, userId: String, geo: Geo) = {
    try {
      if (commentExist || creatorId.equals(userId)) {
        updateUserCurrentGeo(userId, geo)
        val spokId = DseGraphFactory.dseConn.executeGraph(getSpokIdByCommentId(commentId)).one().asString()
        //remove comment vertex
        DseGraphFactory.dseConn.executeGraph(removeUserComment(commentId))
        //update stats
        val currentSpokStats: Vertex = DseGraphFactory.dseConn.executeGraph(getSpokStatsVertex(spokId)).one().asVertex()
        val response = removeCommentResponse(commentId, spokId, currentSpokStats)
        (Some(response), timeStamp, None)
      } else {
        (None, timeStamp, Some(Error(SPK_008, s"Comment $commentId not found")))
      }
    } catch {
      case ex: Exception => (None, timeStamp, Some(Error(SPK_121, s"Unable removing comment $commentId(generic error).")))
    }
  }

  private def removeCommentResponse(commentId: String, spokId: String, currentSpokStats: Vertex): RemoveCommentResponse = {

    val nbRespoked = currentSpokStats.getProperty(NB_RESPOKED).getValue.asString()
    val nbLanded = currentSpokStats.getProperty(NB_USERS).getValue.asString()
    val nbComments = (currentSpokStats.getProperty(NB_COMMENTS).getValue.asInt() - 1).toString()
    val travelled = currentSpokStats.getProperty(TRAVELLED).getValue.asString()
    val commentInternalSpokResponse = CommentInternalSpokResponse(spokId, nbRespoked, nbLanded, nbComments, travelled)
    val removeCommentResponse = RemoveCommentResponse(commentId, commentInternalSpokResponse)
    removeCommentResponse
  }

  def getSpokCreaterIdByCommentId(commentId: String): Option[String] = {
    try {
      val spokId = DseGraphFactory.dseConn.executeGraph(getSpokIdByCommentId(commentId)).one().asString()
      Some(DseGraphFactory.dseConn.executeGraph(getUserIdBySpokId(spokId)).one().asString())
    } catch { case ex: Exception => None }
  }

  /**
   * This function will get user id of spok creator based on comment id.
   *
   * @param commentId comment id
   * @return spok creator User id
   */
  def getUserIdByCommentId(commentId: String): Option[String] = {
    try {
      val spokId = DseGraphFactory.dseConn.executeGraph(getSpokIdByCommentId(commentId)).one().asString()
      Some(DseGraphFactory.dseConn.executeGraph(getUserIdBySpokId(spokId)).one().asString())
    } catch { case ex: Exception => None }
  }

  private def updateUserCurrentGeo(userId: String, geo: Geo) = DseGraphFactory.dseConn.executeGraph(updateUserGeoLocation(userId, geo))

}

object SpokCommentApi extends SpokCommentApi
