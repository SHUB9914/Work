package com.spok.persistence.factory.spokgraph

import com.datastax.driver.dse.graph.{ Edge, GraphNode, GraphResultSet, Vertex }
import com.spok.model.InnerLocation
import com.spok.model.SpokModel._
import com.spok.persistence.dse.DseGraphFactory
import com.spok.persistence.factory.DSEUserSpokFactoryApi
import com.spok.persistence.factory.dsequery.DSEUserQuery
import com.spok.util.Constant._
import com.spok.util.{ CalculateDistance, LoggerUtil, TwilioSMSSender }

import scala.collection.JavaConverters._
import scala.collection.parallel.immutable.ParSeq
import scala.util.Try
import scala.collection.mutable._

trait DSESpokApi extends DSESpokQuery with DSEUserQuery with DSEUserSpokFactoryApi with LoggerUtil with TwilioSMSSender {

  /**
   * This function will create spok for an user
   *
   * @param userId user'id
   * @param spok   spok details
   * @return true if spok created successfully else false
   */
  def createSpok(userId: String, spok: Spok): (Boolean) = {
    try {
      updateUserCurrentGeo(userId, Geo(spok.geo.latitude, spok.geo.longitude, spok.geo.elevation))
      val actualQuestions = if (spok.poll.isDefined) spok.poll.get.questions.size else 0
      val spokProperties = SpokVertex(spok.spokId, spok.ttl.getOrElse(0), spok.contentType, true,
        spok.headerText.getOrElse(""), spok.launched, spok.geo, userId, spok.visibility.getOrElse(PUBLIC).toLowerCase, actualQuestions)
      val spokEdgeProperties = SpokEdge(RESPOKED, spok.launched, spok.geo, userId, spok.groupId.getOrElse(ZERO),
        spok.visibility.getOrElse(PUBLIC).toLowerCase, spok.headerText.getOrElse(""), actualQuestions)
      val userVertex = DseGraphFactory.dseConn.executeGraph(getUser(userId)).one().asVertex()
      //create spok vertex and join with user vertex
      val spokVertex: Vertex = DseGraphFactory.dseConn.executeGraph(insertSpok(spokProperties)).one().asVertex()
      addEdgeSpokerSpok(userVertex, spokVertex, ISASSOCIATEDWITH, spokEdgeProperties)
      //create spok content type vertex and related edges
      insertSpokTypeVertex(spok, spokVertex)
      //create stats vertex and join with spok vertex
      val statsVertex = DseGraphFactory.dseConn.executeGraph(createSpokStats(Statistics(0, 0, 1, 1, 0, 0))).one().asVertex()
      addEdgeWithTime(spokVertex, statsVertex, HAS_STATS, spok.launched)
      true
    } catch {
      case ex: Exception => false
    }
  }

  /**
   * To update User Current Geo coordinates
   *
   * @param userId SpokerID
   * @param geo    Geo Case Class
   * @return graphresultset
   */
  def updateUserCurrentGeo(userId: String, geo: Geo): Unit = {
    DseGraphFactory.dseConn.executeGraph(updateUserGeoLocation(userId, geo))
    logger.info(s" $userId User geo has been updated " + userId)
  }

  /**
   * to insert the content type of spok
   *
   * @param spok       spok details
   * @param spokVertex spokVertex
   * @return
   */
  private def insertSpokTypeVertex(spok: Spok, spokVertex: Vertex): Vertex = {
    val updatedSpok: Spok = getContentType(spok)
    val spokContainsVertex = updatedSpok.contentType match {
      case TEXT_TYPE =>
        DseGraphFactory.dseConn.executeGraph(insertText(spok.text.getOrElse(""))).one().asVertex()
      case HTML_TEXT =>
        DseGraphFactory.dseConn.executeGraph(insertText(spok.text.getOrElse(""))).one().asVertex()
      case FILE_TYPE =>
        DseGraphFactory.dseConn.executeGraph(insertFileUrl(spok.file.getOrElse(""))).one().asVertex()
      case URL_TYPE =>
        DseGraphFactory.dseConn.executeGraph(insertUrl(spok.url.get)).one().asVertex()
      case _ => insertSpokOfPollOrRiddleType(spok, updatedSpok.contentType)
    }
    addEdgeWithTime(spokVertex, spokContainsVertex, CONATINS_A, spok.launched)
    spokContainsVertex
  }

  private def insertSpokOfPollOrRiddleType(spok: Spok, contentType: String) = {
    (contentType, spok.riddle, spok.poll) match {
      case (RIDDLE_TYPE, Some(riddle), None) =>
        val riddelVertex = DseGraphFactory.dseConn.executeGraph(insertRiddle(riddle)).one().asVertex()
        insertRiddleWithQuestion(riddelVertex, riddle, spok.launched)
        riddelVertex
      case (POLL_TYPE, None, Some(poll)) =>
        val pollVertex = DseGraphFactory.dseConn.executeGraph(insertPoll(poll, getUUID())).one().asVertex()
        pollVertex
    }
  }

  /**
   * To get the Contect type of spok
   *
   * @param spok spok details
   * @return spok case class
   */
  private def getContentType(spok: Spok): Spok = {
    if (spok.contentType.equals(PICTURE) || spok.contentType.equals(ANIMATED_GIF) || spok.contentType.equals(VIDEO)
      || spok.contentType.equals(SOUND)) {
      spok.copy(contentType = FILE_TYPE)
    } else {
      spok
    }
  }

  /**
   * To insert Poll type vertex with its related questions
   *
   * @param pollVertex     poll vertex
   * @param pollAttributes poll attributes
   * @param launchedTime   Launched timestamp
   * @return
   */
  def insertPollWithQuestions(pollVertex: Vertex, pollAttributes: Poll, launchedTime: Long): List[List[GraphResultSet]] = {
    pollAttributes.questions map { question =>
      logger.info("Inserting the question.... ")
      val quesVertex = DseGraphFactory.dseConn.executeGraph(questionVertex(question, getUUID())).one().asVertex()
      addEdgeWithTime(pollVertex, quesVertex, HAS_A_QUESTION, launchedTime)
      question.answers map { answer =>
        val ansVertex = DseGraphFactory.dseConn.executeGraph(answerVertex(answer, getUUID())).one().asVertex()
        addEdgeWithTime(quesVertex, ansVertex, HAS_AN_ANSWER, launchedTime)
      }
    }
  }

  /**
   * To insert riddle type vertex with its related questions
   *
   * @param riddleVertex
   * @param riddleAttributes
   * @param launchedTime
   * @return
   */
  private def insertRiddleWithQuestion(riddleVertex: Vertex, riddleAttributes: Riddle, launchedTime: Long) = {
    val questionVertex = DseGraphFactory.dseConn.executeGraph(riddleQuestionVertex(riddleAttributes.question, getUUID())).one().asVertex()
    addEdgeWithTime(riddleVertex, questionVertex, HAS_A_QUESTION, launchedTime)
    val answerVertex = DseGraphFactory.dseConn.executeGraph(riddleAnswerVertex(riddleAttributes.answer, getUUID())).one().asVertex()
    addEdgeWithTime(questionVertex, answerVertex, HAS_AN_ANSWER, launchedTime)
  }

  /**
   * To Link Spok Follower with the Spok
   *
   * @param userId spoker Id
   * @return List of followerID and their Geo
   */
  def linkSpokerFollowers(userId: String, groupId: Option[String], spokId: String, geo: Geo,
    visibility: Option[String], instanceText: Option[String], launched: Long, pendingQuestions: Int,
    status: Boolean = false): List[(String, Double, Double, Double, Long)] = {
    try {
      val (followersVertex, contactsVertex) = getFollowersVertex(userId, groupId)
      val spokVertex = DseGraphFactory.dseConn.executeGraph(fetchSpokVertex(spokId)).one().asVertex()
      val followersDetail = followersVertex.par map {
        followerVertex =>
          val followerId = followerVertex.getProperty(USER_ID).getValue.asString()
          val currentLogTime = timeStamp
          if (!DseGraphFactory.dseConn.executeGraph(isUserAlreadyLinkedWithSpok(followerId, spokId)).one().asBoolean()) {
            associateFollower(followerVertex, userId, groupId, visibility, instanceText, spokVertex, currentLogTime, pendingQuestions)
            val followerGeo = getFollowerGeo(followerVertex)
            (followerId, followerGeo(0).toDouble, followerGeo(1).toDouble, followerGeo(2).toDouble, currentLogTime)
          } else {
            (getUUID(), 0.0, 0.0, 0.0, currentLogTime)
          }
      }
      associateContacts(contactsVertex, userId, launched, geo, groupId, visibility, instanceText, spokVertex, pendingQuestions)
      if (contactsVertex.nonEmpty) sendSMSToContacts(contactsVertex, spokId, userId)
      followersDetail.toList
    } catch {
      case ex: Exception => Nil
    }
  }

  /**
   * Function to Send SMS to Non-Spokers at time of Spok Creation
   *
   * @param contactVertexs
   * @param spokId
   * @param userId
   * @return
   */
  def sendSMSToContacts(contactVertexs: List[Vertex], spokId: String, userId: String): List[Try[String]] = {
    try {
      val userDetails = DseGraphFactory.dseConn.executeGraph(getUserNickNameAndPhoneNo(userId)).asScala.toList
      contactVertexs map {
        contactV =>
          logger.info("Contacts Fetched Now Sending SMS TO " + contactV.getProperty(PHONE_NO).getValue.asString())
          sendSMS("+" + contactV.getProperty(PHONE_NO).getValue.asString(), getMessageFormat(spokId, (parse(userDetails.head.toString) \ "userPhoneNo")
            .extract[String], (parse(userDetails.head.toString) \ "userNickname").extract[String]))
      }
    } catch {
      case ex: Exception =>
        logger.info("Not able to send SMS to contact No for spok id " + spokId)
        List()
    }
  }

  /**
   * Function to format SMS Body
   *
   * @param spokId
   * @param senderNickname
   * @param spokerPhoneNo
   * @return
   */
  private def getMessageFormat(spokId: String, senderNickname: String, spokerPhoneNo: String): String = {
    s""" Hey! Check this spok https://spok.me/spok/$spokId ! $senderNickname $spokerPhoneNo """
  }

  /**
   * Function to get all contacts from group while respoking spok and send SMS.
   *
   * @param groupId
   * @param userId
   * @param spokId
   */
  def sendSMSToContactFromGroup(groupId: String, userId: String, spokId: String): List[Try[String]] = {
    val contactVertexs = DseGraphFactory.dseConn.executeGraph(fetchContactsFromGroup(groupId)).asScala.toList.map(x => x.asVertex())
    sendSMSToContacts(contactVertexs, spokId, userId)
  }

  /**
   * To fetch all followers and contact of spoker in a group
   *
   * @param userId
   * @param groupId
   * @return List of followers and contacts vertex
   */
  private def getFollowersVertex(userId: String, groupId: Option[String]): (List[Vertex], List[Vertex]) = {
    val userVertex = DseGraphFactory.dseConn.executeGraph(getUserVertexFromGroup(userId, groupId.getOrElse(ZERO))).asScala.toList.map(x => x.asVertex())
    val moblieVertex = DseGraphFactory.dseConn.executeGraph(getMobileVertexFromPrivateGroup(userId, groupId.getOrElse(ZERO))).
      asScala.toList.map(x => x.asVertex())
    (userVertex, moblieVertex)
  }

  /**
   * Link Followers with Pending state with spok
   *
   * @param followerVertex
   * @param userId
   * @param spokVertex
   * @return
   */
  private def associateFollower(followerVertex: Vertex, userId: String, groupId: Option[String], visibility: Option[String],
    instanceText: Option[String], spokVertex: Vertex, currentLogTime: Long, pendingQuestions: Int): GraphResultSet = {
    val followerGeo: List[String] = getFollowerGeo(followerVertex)
    val spokEdgeProperties = SpokEdge(PENDING, currentLogTime, Geo(followerGeo(0).toDouble, followerGeo(1).toDouble,
      followerGeo(2).toDouble), userId, groupId.getOrElse("0"), visibility.getOrElse(PUBLIC).toLowerCase,
      instanceText.getOrElse(""), pendingQuestions)
    addEdgeSpokerSpok(followerVertex, spokVertex, ISASSOCIATEDWITH, spokEdgeProperties)
  }

  /**
   * Link Contact from group to spok
   *
   * @param contactsVertex
   * @param userId
   * @param spokVertex
   * @return
   */
  private def associateContacts(contactsVertex: List[Vertex], userId: String, launched: Long, geo: Geo, groupId: Option[String],
    visibility: Option[String], instanceText: Option[String], spokVertex: Vertex, pendingQuestions: Int) = {
    val spokEdgeProperties = SpokEdge(PENDING, launched, geo, userId, groupId.getOrElse("0"),
      visibility.getOrElse(PUBLIC), instanceText.getOrElse(""), pendingQuestions)
    contactsVertex.par map { contactVertex =>
      val contactLinked = DseGraphFactory.dseConn.executeGraph(
        isContactAlreadyLinkedWithSpok(contactVertex.getProperty(PHONE_NO).toString, spokVertex.getProperty(SPOK_ID).toString)
      ).one().asBoolean()
      val contactUser = DseGraphFactory.dseConn.executeGraph(isContactAUser(contactVertex.getProperty(PHONE_NO).getValue.asString())).one().asBoolean()

      if (contactLinked.equals(false) && contactUser.equals(false)) addEdgeSpokerSpok(contactVertex, spokVertex, ISASSOCIATEDWITH, spokEdgeProperties)
    }
  }

  /**
   * Get all follower Geo
   *
   * @param followerVertex
   * @return
   */
  private def getFollowerGeo(followerVertex: Vertex): List[String] = {
    val followerId = followerVertex.getProperty(USER_ID).getValue.asString()
    (DseGraphFactory.dseConn.executeGraph(getFollowersCurrentGeo(followerId)).asScala.toList) map (_.toString.replace("\"", ""))
  }

  /**
   * To calculate spok stats at time of spok creation
   *
   * @param spokId
   * @param followersVertex
   * @param spokerGeo
   * @return
   */
  private def calculateSpokStats(spokId: String, followersVertex: List[Vertex], spokerGeo: InnerLocation, previousGeo: InnerLocation, status: Boolean) = {
    try {
      val (pendingCount, unspokedCount, respokedCount, landedCount, commentCount) = calculateSpokStatsCount(spokId)
      val listOfFollowersGeo: List[InnerLocation] = (followersVertex.par map { followerVertex =>
        val followerGeo = getFollowerGeo(followerVertex)
        InnerLocation(followerGeo(0).toDouble, followerGeo(1).toDouble)
      }).toList
      val totalDistanceTravelled = if (status) {
        calculateRespokDistance(listOfFollowersGeo, spokerGeo, previousGeo, spokId)
      } else {
        calculateTravelledDistance(listOfFollowersGeo, spokerGeo)
      }
      Statistics(pendingCount, unspokedCount, respokedCount, landedCount, commentCount, totalDistanceTravelled)
    } catch {
      case ex: Exception => Statistics(0, 0, 0, 0, 0, 0.0)
    }
  }

  def calculateSpokStatsCount(spokId: String): (Long, Long, Long, Long, Long) = {
    //TODO: Process this through OLAP only
    val spokStats = DseGraphFactory.dseConn.executeGraph(fetchSpokStats(spokId))
    val spokStatsList = spokStats.asScala.toList
    if (spokStatsList.nonEmpty) {
      val pendingCount = (parse(spokStatsList(0).toString) \ (s"$PENDING_USERS_COUNT")).extract[Long]
      val respokedCount = (parse(spokStatsList(0).toString) \ (s"$RESPOKED_USERS_COUNT")).extract[Long]
      val unspokedCount = (parse(spokStatsList(0).toString) \ (s"$UNSPOKED_USERS_COUNT")).extract[Long]

      val landedCount = respokedCount + unspokedCount + pendingCount
      val commentCount = fetchCommentCount(spokId)
      (pendingCount, unspokedCount, respokedCount, landedCount, commentCount)
    } else { (0, 0, 0, 0, fetchCommentCount(spokId)) }
  }

  private def getTravelledDistance(spokId: String): Double =
    DseGraphFactory.dseConn.executeGraph(fetchStatsVertex(spokId)).one().asVertex().getProperty("travelled").getValue.asDouble()

  /**
   * Methos to calcualte travelled distance using haversineDistance formulae
   *
   * @param listOfFollowersGeo
   * @param spokerGeo
   * @return
   */
  def calculateTravelledDistance(listOfFollowersGeo: List[InnerLocation], spokerGeo: InnerLocation): Double = {
    def sum(listOfFollowersGeo: List[InnerLocation]): Double = {
      listOfFollowersGeo match {
        case x :: tail => CalculateDistance.haversineDistance((x.lat, x.lng), (spokerGeo.lat, spokerGeo.lng)) + sum(tail)
        case Nil => 0.0
      }
    }
    sum(listOfFollowersGeo)
  }

  def calculateRespokDistance(listOfFollowersGeo: List[InnerLocation], spokerGeo: InnerLocation, previousGeo: InnerLocation, spokId: String): Double = {
    val travelled = getTravelledDistance(spokId)
    calculateTravelledDistance(listOfFollowersGeo, spokerGeo) + CalculateDistance.haversineDistance(
      (previousGeo.lat, previousGeo.lng), (spokerGeo.lat, spokerGeo.lng)
    ) + travelled
  }

  /**
   * To Respok the Spok
   *
   * @param spokId
   * @param userId
   * @param respok
   * @return Respok Response
   */
  def createRespok(spokId: String, userId: String, respok: Respok, edgeOpt: Option[Edge]): (Option[RespokInterimResponse], Option[Error]) = {

    try {
      val edgeBetweenUserAndSpok = edgeOpt match {
        case Some(edge) => edge
      }
      val spokVertex = DseGraphFactory.dseConn.executeGraph(fetchSpokVertex(spokId)).one().asVertex()
      logger.info("got spok vertex " + spokVertex.getId)
      val previousVisibility = edgeBetweenUserAndSpok.getProperty(VISIBILITY).getValue.asString()
      val spokContentType = spokVertex.getProperty(CONTENT_TYPE).getValue.asString()
      val actualGroupId = edgeBetweenUserAndSpok.getProperty(GROUP_ID).getValue.asString()
      previousVisibility match {
        case PRIVATE => createPrivateVisibilityRespok(respok, spokContentType, actualGroupId, spokId,
          edgeBetweenUserAndSpok, userId, previousVisibility, spokVertex)
        case PUBLIC => {
          if (spokContentType.equals(POLL)) respokPollSpok(spokId, edgeBetweenUserAndSpok, respok, userId, previousVisibility, respok.launched, spokVertex)
          else insertRespok(spokId, edgeBetweenUserAndSpok, respok, userId, previousVisibility, respok.launched, 0)
        }
      }
    } catch {
      case ex: Exception => (None, Some(Error(SPK_117, s"Unable re-spoking spok $spokId (generic error).")))
    }
  }

  private def createPrivateVisibilityRespok(respok: Respok, spokContentType: String, actualGroupId: String,
    spokId: String, edgeBetweenUserAndSpok: Edge, userId: String, previousVisibility: String, spokVertex: Vertex) = {
    respok.visibility.getOrElse(PUBLIC).toLowerCase match {
      case PUBLIC => (None, Some(Error(SPK_107, NOT_ALTER_VISIBILITY)))
      case PRIVATE => handleRespokForPrivateGroup(respok, spokContentType, actualGroupId, spokId,
        edgeBetweenUserAndSpok, userId, previousVisibility, spokVertex)
    }
  }

  private def handleRespokForPrivateGroup(respok: Respok, spokContentType: String, actualGroupId: String, spokId: String,
    edgeBetweenUserAndSpok: Edge, userId: String, previousVisibility: String, spokVertex: Vertex) = {
    respok.groupId match {
      case Some(id) => if (id equals actualGroupId) {
        if (spokContentType.equals(POLL)) respokPollSpok(spokId, edgeBetweenUserAndSpok, respok, userId, previousVisibility, respok.launched, spokVertex)
        else insertRespok(spokId, edgeBetweenUserAndSpok, respok, userId, previousVisibility, respok.launched, 0)
      } else {
        (None, Some(Error(SPK_128, RESPOK_IN_OTHER_GROUP_ERROR)))
      }
      case None => (None, Some(Error(SPK_130, RESPOK_IN_DEFAULT_GROUP_ERROR)))
    }
  }

  /**
   * Method to handle respoking of a poll spok.
   *
   * @param spokId
   * @param edgeBetweenUserAndSpok
   * @param respok
   * @param userId
   * @param previousVisibility
   * @param reSpokedTime
   * @return
   */
  private def respokPollSpok(spokId: String, edgeBetweenUserAndSpok: Edge, respok: Respok, userId: String,
    previousVisibility: String, reSpokedTime: Long, spokVertex: Vertex) = {

    val actualQuestions = spokVertex.getProperty(ACTUAL_QUESTIONS).getValue.asInt()
    val questionsPending = edgeBetweenUserAndSpok.getProperty(PENDING_QUESTIONS).getValue.asInt()
    if (questionsPending == 0) {
      insertRespok(spokId, edgeBetweenUserAndSpok, respok, userId, previousVisibility, reSpokedTime, actualQuestions)
    } else (None, Some(Error(SPK_131, s"Poll's questions have to be all answered before respoking spok $spokId.")))
  }

  /**
   * Method to respok a spok if everything is valid
   *
   * @param spokId
   * @param edgeBetweenUserAndSpok
   * @param respok
   * @param userId
   * @param previousVisibility
   * @param reSpokedTime
   * @return
   */
  private def insertRespok(spokId: String, edgeBetweenUserAndSpok: Edge, respok: Respok,
    userId: String, previousVisibility: String, reSpokedTime: Long, actualQuestions: Int) = {
    logger.info("Now inserting respok  " + spokId)
    updateStatusOfEdge(userId, spokId, RESPOKED, respok)
    logger.info("updated StatusOfEdge  " + spokId)
    logger.info("now linking followers and updating stats ")

    //Try and optimize this further
    val followersOfRespoker: List[(String, Double, Double, Double, Long)] = linkSpokerFollowers(userId, respok.groupId,
      spokId, respok.geo, respok.visibility, respok.text, reSpokedTime, actualQuestions, true)

    val statsVertex = DseGraphFactory.dseConn.executeGraph(fetchStatsVertex(spokId)).one().asVertex()
    val numberOfRespoked = statsVertex.getProperty(NB_RESPOKED).getValue.asLong()
    val numberOfComments = statsVertex.getProperty(NB_COMMENTS).getValue.asLong()
    val travelled = statsVertex.getProperty(TRAVELLED).getValue.asDouble()
    val numberOflanded = statsVertex.getProperty(NB_USERS).getValue.asLong()
    logger.info("now linking followers and updating stats completed  " + spokId)
    (Some(RespokInterimResponse(spokId, RespokStatistics(numberOfRespoked, numberOflanded, numberOfComments, travelled),
      respok.mention.getOrElse(List()), followersOfRespoker)), None)
  }

  private def updateStatusOfEdge(userId: String, spokId: String, status: String, respok: Respok) =
    DseGraphFactory.dseConn.executeGraph(updateEdgeBetweenUserAndSpok(userId, spokId, status, respok))

  /**
   * Method to add an edge btween the user and the answer user is giving
   *
   * @param questionId to which the answer belongs
   * @param spokId
   * @param userId
   * @param userPollAnswer
   * @return
   */
  def addAnswerToAPoll(questionId: String, spokId: String, userId: String, userPollAnswer: UserPollAnswer): Option[Error] = {

    try {
      val currentQuestionsAnswerList = DseGraphFactory.dseConn.executeGraph(
        getCurrentQuestionsAllAnswers(questionId)
      ).asScala.toList
      val isQuestionAnswered = currentQuestionsAnswerList map { answerNode =>
        val answerVertex = answerNode.asVertex()
        val answerId = answerVertex.getProperty(ID).getValue.asString()
        DseGraphFactory.dseConn.executeGraph(hasUserGivenAnswer(answerId, userId)).one().asBoolean()
      }
      if (isQuestionAnswered contains true) Some(Error(SPK_134, s"Question $questionId already answered."))
      else {
        updateUserCurrentGeo(userId, userPollAnswer.geo)
        val userVetex = DseGraphFactory.dseConn.executeGraph(getUser(userId)).one().asVertex()
        val answerVertex = DseGraphFactory.dseConn.executeGraph(getAnswerVertexQuery(userPollAnswer.answerId))
          .one().asVertex()
        DseGraphFactory.dseConn.executeGraph(executeGraphStatement(userVetex, answerVertex, GIVES_AN_ANSWER))
        val nbAnswered = answerVertex.getProperty(NB_ANSWERED).getValue.asInt() + 1
        DseGraphFactory.dseConn.executeGraph(updateAnsweredCountQuery(userPollAnswer.answerId, nbAnswered))
        None
      }
    } catch {
      case ex: Exception => Some(Error(SPK_010, s"Invalid answer to question $questionId."))
    }
  }

  /**
   * Method to view the current question with the details of previous and next questions also
   *
   * @param questionId of the question to be viewed
   * @param userId
   * @return full details of the question
   */
  def viewPollQuestion(questionId: String, userId: String): (Option[ViewPollQuestion], Option[Error]) = {
    val currentQuestionId = questionId
    val currentQuestionVertex = getQuestionVertex(currentQuestionId)
    currentQuestionVertex match {
      case Nil => (None, Some(Error(SPK_126, s"Question $questionId Not Found")))
      case vertexList => {
        vertexList.map { vertex =>
          val spokVertex = vertex.get("spokV").asVertex()
          val questionVertex = vertex.get("quest").asVertex()
          val spokId = spokVertex.getProperty(SPOK_ID).getValue.asString()
          try {
            val pollId = DseGraphFactory.dseConn.executeGraph(getPollIdVertex(currentQuestionId))
              .one().asVertex().getProperty(ID).getValue.asString()
            val currentQuestionText = questionVertex.getProperty(TEXT).getValue.asString()
            val currentQuestionRank = questionVertex.getProperty(RANK).getValue.asInt()
            val answersOfCurrentQuestions = getPollAnswers(currentQuestionId)
            val currentQuestionDetails = ViewPollQuestionInternalResponse(currentQuestionId, currentQuestionText)
            val previousQuestion = findPreviousAndNextQuestionDetails(pollId, List(currentQuestionRank - 1, currentQuestionRank + 1))
            (Some(ViewPollQuestion(previousQuestion.head, currentQuestionDetails, previousQuestion.last, answersOfCurrentQuestions)), None)
          } catch {
            case ex: Exception =>
              (None, Some(Error(SPK_112, s"Unable viewing question $questionId of the spok poll $spokId (generic error).")))
          }
        }.head
      }
    }
  }

  def getQuestionVertex(currentQuestionId: String): List[GraphNode] = {
    try {
      DseGraphFactory.dseConn.executeGraph(getPollQuestionVertex(currentQuestionId)).asScala.toList
    } catch {
      case ex: Exception => Nil
    }
  }

  private def findPreviousAndNextQuestionDetails(pollId: String, previousQuestionRank: List[Int]): List[Option[ViewPollQuestionInternalResponse]] = {
    previousQuestionRank.par.map { rank =>
      {
        try {
          val questionVertex = DseGraphFactory.dseConn.executeGraph(getQuestionVertex(
            pollId, rank
          )).one().asVertex()
          val questionId = questionVertex.getProperty(ID).getValue.asString()
          val questionText = questionVertex.getProperty(TEXT).getValue.asString()
          Some(ViewPollQuestionInternalResponse(questionId, questionText))
        } catch {
          case ex: Exception => None
        }
      }
    }.toList
  }

  private def getPollAnswers(currentQuestionId: String) = {

    val currentQuestionsAnswerList = DseGraphFactory.dseConn.executeGraph(
      getCurrentQuestionsAllAnswers(currentQuestionId)
    ).asScala.toList
    currentQuestionsAnswerList.par.map { answerNode =>
      val answerVertex = answerNode.asVertex()
      val answerId = answerVertex.getProperty(ID).getValue.asString()
      val answerRank = answerVertex.getProperty(RANK).getValue.asInt()
      val answerText = answerVertex.getProperty(TEXT).getValue.asString()
      ViewPollAnswerResponse(answerId, answerRank, answerText)
    }.toList
  }

  /**
   * To Unspok a spok
   *
   * @param spokId
   * @param userId
   * @param unspok
   * @return unspok Response
   */
  def createUnspok(spokId: String, userId: String, unspok: Unspok, status: String): Option[UnspokResponse] = {
    try {
      if (status == PENDING) updateUnspokStatus(userId, spokId, unspok)
      val statsVertex = DseGraphFactory.dseConn.executeGraph(fetchStatsVertex(spokId)).one().asVertex()
      Some(UnspokResponse(spokId, RespokStatistics(
        statsVertex.getProperty(NB_RESPOKED).getValue.asLong(),
        statsVertex.getProperty(NB_USERS).getValue.asLong(),
        statsVertex.getProperty(NB_COMMENTS).getValue.asLong(),
        statsVertex.getProperty(TRAVELLED).getValue.asDouble()
      )))
    } catch {
      case ex: Exception => None
    }
  }

  def updateStatsAfterUnspok(spokId: String, userId: String, unspok: Unspok): Unit = {
    val (pendingCount, unspokedCount, respokedCount, landedCount, commentCount) = calculateSpokStatsCount(spokId)
    logger.info(s" $spokId Got latest stats ::" + pendingCount + unspokedCount + respokedCount + landedCount + commentCount)
    val travelledDistance = getTravelledDistance(spokId)
    val stats = Statistics(pendingCount, unspokedCount, respokedCount, landedCount, commentCount, travelledDistance)
    DseGraphFactory.dseConn.executeGraph(updateSpokStats(spokId, stats))
    logger.info(s" $spokId Stats has been updated after unspok " + spokId)
  }

  private def updateUnspokStatus(userId: String, spokId: String, unspok: Unspok) =
    DseGraphFactory.dseConn.executeGraph(updateUnspokStatusEdge(userId, spokId, unspok))

  /**
   * This method will disable a spok
   *
   * @param spokId
   * @param userId
   * @return
   */
  def disableSpok(spokId: String, userId: String, geo: Geo): String = {

    validateAbsoluteSpokById(spokId) match {
      case SPOK_VALID => handleDisableSpokRequest(spokId, userId)
      case DISABLED_SPOK => DISABLED_SPOK
      case SPOK_NOT_FOUND => SPOK_NOT_FOUND
    }
  }

  private def handleDisableSpokRequest(spokId: String, userId: String) = {
    DseGraphFactory.dseConn.executeGraph(validateUserBySpokId(spokId, userId)).one().asBoolean() match {
      case true => handleValidDisableSpokRequest(spokId)
      case false => INVALID_USER
    }
  }

  private def handleValidDisableSpokRequest(spokId: String) = {
    try {
      DseGraphFactory.dseConn.executeGraph(disableSpokQuery(spokId))
      SPOK_DISABLED
    } catch {
      case ex: Exception => UNABLE_DISABLING_SPOK
    }
  }

  /**
   * This method will remove spok from wall
   *
   * @param spokId
   * @param userId
   * @return
   */
  def removeSpokFromWall(spokId: String, userId: String, launchedTime: Long, geo: Geo): RemoveSpokResponse = {
    try {
      logger.info(s" removeSpokFromWall with spokId " + spokId)
      val edgeBetweenUserAndSpok = DseGraphFactory.dseConn.executeGraph(getEdgeBetweenUserAndSpok(userId, spokId)).one().asEdge()
      val edgeStatus = edgeBetweenUserAndSpok.getProperty(STATUS).getValue.asString()
      logger.info(s" edgeStatus of spokId " + edgeStatus)
      edgeStatus match {
        case RESPOKED =>
          DseGraphFactory.dseConn.executeGraph(removeSpokFromWallQuery(userId, spokId, launchedTime))
          RemoveSpokResponse(Some(spokId))
        case _ => RemoveSpokResponse(message = Some(SPOK_STATUS_NOT_RESPOKED))
      }
    } catch {
      case ex: Exception => RemoveSpokResponse(message = Some(UNABLE_REMOVING_SPOK))
    }
  }

  /**
   * This function update spok stats after adding comments
   *
   * @param spokId
   */
  def updateStatsAfterAddComment(spokId: String): Unit = {
    DseGraphFactory.dseConn.executeGraph(updateSpokCommentStats(spokId, fetchCommentCount(spokId).toInt))
    logger.info(s" $spokId Stats has been updated after add comment " + spokId)
  }

  def updatePendingQuestionsInEdge(userId: String, spokId: String): GraphResultSet = {
    val pendingQuestions =
      DseGraphFactory.dseConn.executeGraph(getEdgeBetweenUserAndSpok(userId, spokId))
        .one.asEdge().getProperty(PENDING_QUESTIONS).getValue.asInt()
    if (pendingQuestions == 1) {
      DseGraphFactory.dseConn.executeGraph(updatePendingQuestionQuery(userId, spokId, pendingQuestions - 1))
      val pollVertex = DseGraphFactory.dseConn.executeGraph(fetchPollVertex(spokId)).one.asVertex()
      val nbFinished = pollVertex.getProperty(NB_FINISHED).getValue.asInt()
      val pollId = pollVertex.getProperty(ID).getValue.asString()
      DseGraphFactory.dseConn.executeGraph(updatePollCompletedCountQuery(pollId, nbFinished + 1))
    } else {
      DseGraphFactory.dseConn.executeGraph(updatePendingQuestionQuery(userId, spokId, pendingQuestions - 1))
    }
  }

  /**
   * Method to update the spok stats
   *
   * @param spokId of the spok whose stats are to be updated
   * @param userId
   * @param groupId in which the spok exists
   * @param geo
   * @param status whether the spok has been created to respoked
   */
  def updateStats(spokId: String, userId: String, groupId: Option[String], geo: Geo, status: Boolean = false): Unit = {
    if (status) {
      val userGeoVertex = DseGraphFactory.dseConn.executeGraph(fetchUserGeo(userId)).one().asVertex()
      val previousGeo = InnerLocation(
        userGeoVertex.getProperty(LATITUDE).getValue.asDouble(),
        userGeoVertex.getProperty(LONGITUDE).getValue.asDouble()
      )
      calculateAndUpdateStats(spokId, userId, groupId, geo, previousGeo, true)
    } else {
      logger.info("Calculating stats for spok creation")
      calculateAndUpdateStats(spokId, userId, groupId, geo)
    }
  }

  def fetchUserV(userId: String): Vertex = {
    DseGraphFactory.dseConn.executeGraph(fetchUserById(userId)).one().asVertex()
  }

  def fetchUserGeoV(userId: String): Vertex = {
    DseGraphFactory.dseConn.executeGraph(fetchUserGeo(userId)).one().asVertex()
  }

  private def calculateAndUpdateStats(spokId: String, userId: String, groupId: Option[String], geo: Geo, previousGeo: InnerLocation = InnerLocation(0.0, 0.0), status: Boolean = false) = {
    val (followersVertex, contactsVertex) = getFollowersVertex(userId, groupId)
    val stats = calculateSpokStats(spokId, followersVertex, InnerLocation(geo.latitude, geo.longitude), previousGeo, status)
    DseGraphFactory.dseConn.executeGraphAsync(updateSpokStats(spokId, stats))
    logger.info("Updated the spok stats completely")
  }

  /**
   * This method adds all answers to a poll. It also checks if poll was already completed or not, do questions belong to this spok only,
   * are all questions being answered or not.
   * @param userId
   * @param allAnswers
   * @return error if there is any error else may return a list that contains the id's of questions which were already answered.
   */
  def addAllAnswersToAPoll(userId: String, allAnswers: AllAnswers): (Option[Error], List[String]) = {
    try {
      val spokerSpokEdge = DseGraphFactory.dseConn.executeGraph(getEdgeBetweenUserAndSpok(userId, allAnswers.spokId)).one().asEdge()
      val pendingQuestions = spokerSpokEdge.getProperty(PENDING_QUESTIONS).getValue.asInt()
      if (pendingQuestions == 0) (Some(Error(SPK_135, s"Spok ${allAnswers.spokId} already completed.")), Nil)
      else if (pendingQuestions != allAnswers.oneAnswer.length) (Some(Error(SPK_136, s"Not all answers for ${allAnswers.spokId}")), Nil)
      else {
        val questionList = allAnswers.oneAnswer.map(elem => elem.questionId).sortBy(x => x)
        val questionIdsOfPoll = DseGraphFactory.dseConn.executeGraph(fetchPollQuestionVertex(allAnswers.spokId)).asScala.toList.map {
          questionV => questionV.asVertex().getProperty(ID).getValue.asString()
        }.sortBy(x => x)
        if (questionList equals questionIdsOfPoll) addAnswersToQuestions(userId, allAnswers)
        else (Some(Error(SPK_137, s"Not all questions are related to the poll spok ${allAnswers.spokId}")), questionList diff questionIdsOfPoll)
      }
    } catch {
      case ex: Exception => (Some(Error(SPK_010, s"Generic error in answering spok's ${allAnswers.spokId} all questions.")), Nil)
    }
  }

  private def addAnswersToQuestions(userId: String, allAnswers: AllAnswers) = {
    updateUserCurrentGeo(userId, allAnswers.geo)
    val alreadyAnswered: ListBuffer[String] = ListBuffer()
    val questionAnswers = allAnswers.oneAnswer
    val userVetex = DseGraphFactory.dseConn.executeGraph(getUser(userId)).one().asVertex()
    questionAnswers map {
      case OneAnswer(questionId, answerId) => {
        val answerVertex = DseGraphFactory.dseConn.executeGraph(getAnswerVertexQuery(answerId))
          .one().asVertex()
        val currentQuestionsAnswerList = DseGraphFactory.dseConn.executeGraph(
          getCurrentQuestionsAllAnswers(questionId)
        ).asScala.toList
        val isQuestionAnswered = currentQuestionsAnswerList map { answerNode =>
          val answerVertex = answerNode.asVertex()
          val answerId = answerVertex.getProperty(ID).getValue.asString()
          DseGraphFactory.dseConn.executeGraph(hasUserGivenAnswer(answerId, userId)).one().asBoolean()
        }
        if (isQuestionAnswered contains true) {
          alreadyAnswered += questionId
        } else {
          DseGraphFactory.dseConn.executeGraph(executeGraphStatement(userVetex, answerVertex, GIVES_AN_ANSWER))
          val nbAnswered = answerVertex.getProperty(NB_ANSWERED).getValue.asInt() + 1
          DseGraphFactory.dseConn.executeGraph(updateAnsweredCountQuery(answerId, nbAnswered))
        }
      }
    }
    (None, alreadyAnswered.toList)
  }

  /**
   * Method to update the finish count of poll when all poll questions are answered at once
   * @param userId
   * @param spokId
   * @return
   */
  def updateFinishCountOfPoll(userId: String, spokId: String): GraphResultSet = {
    DseGraphFactory.dseConn.executeGraph(updatePendingQuestionQuery(userId, spokId, 0))
    val pollVertex = DseGraphFactory.dseConn.executeGraph(fetchPollVertex(spokId)).one.asVertex()
    val nbFinished = pollVertex.getProperty(NB_FINISHED).getValue.asInt()
    val pollId = pollVertex.getProperty(ID).getValue.asString()
    DseGraphFactory.dseConn.executeGraph(updatePollCompletedCountQuery(pollId, nbFinished + 1))
  }

}

object DSESpokApi extends DSESpokApi
