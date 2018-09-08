package com.spok.persistence.factory

import java.text.SimpleDateFormat

import com.datastax.driver.dse.graph.{ Edge, GraphResultSet, SimpleGraphStatement, Vertex }
import com.spok.persistence.dse.DseGraphFactory
import com.spok.persistence.factory.dsequery.DSEUserQuery
import com.spok.persistence.factory.spokgraph.{ DSESpokCommentQuery, DSESpokQuery }
import com.spok.util.Constant._
import com.spok.util.{ JsonHelper, RandomUtil }

trait DSEUserSpokFactoryApi extends DSEUserQuery with RandomUtil with JsonHelper with DSESpokQuery with DSESpokCommentQuery {

  val DSEPersistenceObj = DSEGraphPersistenceFactoryApi
  val simpleFormatter = new SimpleDateFormat("EEE MMM dd hh:mm:ss z yyyy")
  val dseGraphNotificationFactoryApi: DSEUserNotificationFactoryApi = DSEUserNotificationFactoryApi

  /**
   * To check if user exists or not
   *
   * @param user_Id
   * @return boolean
   */
  def isExistsUser(user_Id: String): Boolean = {
    //done
    val user: Boolean = DseGraphFactory.dseConn.executeGraph(getUser(user_Id)).iterator().hasNext
    user
  }

  /**
   * Checks the group Validation
   *
   * @param userId  of the owner of the group
   * @param groupId of the group to be validated
   * @return true if the group is valid else false
   */
  def isValidGroup(userId: String, groupId: String): Boolean = {

    try {
      val edgeExistQuery = new SimpleGraphStatement(
        s"g.V().hasLabel('$USER').has('$USER_ID','$userId').outE('$CREATES_A_GROUP','$BELONGS_TO').inV().has('$GROUP_ID','$groupId').hasNext()"
      )
      DseGraphFactory.dseConn.executeGraph(edgeExistQuery).one.asBoolean()
    } catch {
      case ex: Exception => false
    }
  }

  def validateSpokAndSendStatus(userId: String, spokId: String): (String, Boolean, Option[Edge]) = {
    try {
      val edgeDetails = DseGraphFactory.dseConn
        .executeGraph(getEdgeBetweenUserAndSpok(userId, spokId)).one().asEdge()
      val status = edgeDetails.getProperty(STATUS).getValue.asString()
      val isEnabled = DseGraphFactory.dseConn.executeGraph(fetchSpokVertex(spokId)).one().asVertex().getProperty(ENABLED).getValue.asBoolean()
      (status, isEnabled, Some(edgeDetails))
    } catch {
      case ex: Exception => (SPOK_NOT_FOUND, false, None)
    }
  }

  /**
   * Check whether spok exits or not with spok flag enabled or disabled.
   *
   * @param spokId
   * @return
   */
  def isValidSpokWithEnabledFlag(spokId: String): Boolean = {
    try {
      val spokVertex = DseGraphFactory.dseConn.executeGraph(fetchSpokVertex(spokId)).one().asVertex()
      val isValidSpokId = spokVertex.getProperty(SPOK_ID).getValue.asString().equals(spokId)
      val isEnabled = spokVertex.getProperty(ENABLED).getValue.asBoolean()
      isValidSpokId && isEnabled
    } catch {
      case ex: Exception => false
    }
  }

  /**
   * Method to validate if the question exists and return the subsequent spok id related to it
   *
   * @param questionId
   * @return
   */
  def validatePollQuestionAndFetchSpokId(questionId: String): Option[String] = {

    val questionExist = isQuestionExist(questionId)
    if (questionExist) {
      try {
        val spokId = DseGraphFactory.dseConn.executeGraph(fetchSpokIdForPoll(questionId))
          .one().asVertex().getProperty(SPOK_ID).getValue.asString()
        Some(spokId)
      } catch {
        case ex: Exception => None
      }
    } else None
  }

  def isQuestionExist(questionId: String): Boolean =
    DseGraphFactory.dseConn.executeGraph(validatePollQuestionByIdQuery(questionId)).iterator().hasNext

  /**
   * Mehtod to check if the comment exists or not and if the user trying to update or remove comment
   * is the same user who created the comment or not
   *
   * @param commentId   the id of the comment to be validated
   * @param commenterId the id of the user trying to perform operation on comment
   * @return
   */
  def validateCommentById(commentId: String, commenterId: String): Boolean = {
    DseGraphFactory.dseConn.executeGraph(
      validateCommentByCommenterId(commentId, commenterId)
    ).iterator().hasNext()
  }

  /**
   * This method will validate spok by spok id.
   *
   * @param spokId
   * @return
   */
  def validateAbsoluteSpokById(spokId: String): String = {
    try {
      val spokVertex = DseGraphFactory.dseConn.executeGraph(fetchSpokVertex(spokId))
      spokVertex.iterator().hasNext match {
        case true => validateStatusOfSpokVertex(spokVertex)
        case false => SPOK_NOT_FOUND
      }
    } catch {
      case ex: Exception => SPOK_NOT_FOUND
    }
  }

  private def validateStatusOfSpokVertex(spokVertex: GraphResultSet) = {
    spokVertex.one().asVertex().getProperty(ENABLED).getValue.asBoolean() match {
      case true => SPOK_VALID
      case false => DISABLED_SPOK
    }
  }

  def validateSpokById(spokId: String): (String, Option[Vertex]) = {
    try {
      val spokVertex = DseGraphFactory.dseConn.executeGraph(fetchSpokVertex(spokId)).one().asVertex()
      spokVertex.getProperty(ENABLED).getValue.asBoolean() match {
        case true => (SPOK_VALID, Some(spokVertex))
        case false => (DISABLED_SPOK, None)
      }
    } catch {
      case ex: Exception => (SPOK_NOT_FOUND, None)
    }
  }

  def spokerSuspendedOrNot(spokerId: String): String = {

    val isSpokerDisable = DSEPersistenceObj.isUserSuspendAlready(spokerId)
    isSpokerDisable match {
      case Some(true) => SPOKER_SUSPENDED
      case Some(false) => SPOKER_NOT_SUSPENDED
      case _ => PROPERTY_NOT_FOUND
    }

  }

}

object DSEUserSpokFactoryApi extends DSEUserSpokFactoryApi

