package com.spok.persistence.factory.spokgraph

import com.spok.model.SpokModel._
import com.spok.persistence.dse.DseGraphFactory
import com.spok.util.Constant._

import scala.collection.JavaConverters._
import com.spok.persistence.dse.DseConnectionUri._
import com.spok.persistence.factory.dsequery.DSEUserQuery
import java.util.Date

import com.datastax.driver.dse.graph.{ Edge, GraphNode, Vertex }
import com.google.common.reflect.TypeToken
import com.spok.model.Account.UserMinimalDetailsResponse
import com.spok.model.SpokModel.{ Comments, CommentsResponse }
import com.spok.persistence.factory.spoklog.SpokLogging

import scala.collection.parallel.immutable.ParSeq

trait DSESpokViewApi extends SpokViewQuery with DSEUserQuery with DSESpokQuery {

  val rnd = new scala.util.Random

  /**
   * getSpokStats will return Spok's Stats
   *
   * @param spokId
   */

  def getSpokStats(spokId: String): SpokStatistics = {

    val spokStatsVertex = DseGraphFactory.dseConn.executeGraph(getSpokStatsQuery(spokId)).one().asVertex()
    val totalTravelledDis = spokStatsVertex.getProperty(TRAVELLED).getValue.asDouble()
    val numberOfUser = spokStatsVertex.getProperty(NB_USERS).getValue.asInt()
    val numberOfRespoked = spokStatsVertex.getProperty(NB_RESPOKED).getValue.asInt()
    val numberOfUnspoked = spokStatsVertex.getProperty(NB_UNSPOKED).getValue.asInt()
    val numberOfPending = spokStatsVertex.getProperty(NB_PENDING).getValue.asInt()
    val numberOfComment = spokStatsVertex.getProperty(NB_COMMENTS).getValue.asInt()
    SpokStatistics(totalTravelledDis, numberOfUser, numberOfRespoked, numberOfUnspoked, numberOfPending, numberOfComment)
  }

  /**
   * This function is used to view 10 comments of a spok.
   *
   * @param spokId
   * @param pos
   * @return
   */
  def getComments(spokId: String, pos: String): Option[CommentsResponse] = {
    try {
      val reSpokerPerPage: Int = pageSize
      val from = (pos.toInt - 1) * reSpokerPerPage
      val to = from + reSpokerPerPage + 1
      val comments: List[Comments] = commentDetails(spokId, from, to)
      val commentsRes = if (comments.size > pageSize) {
        comments.dropRight(1)
      } else {
        comments
      }

      val (previous, next): (String, String) = {
        if (comments.size > pageSize) {
          ((pos.toInt - 1).toString, (pos.toInt + 1).toString)
        } else {
          ((pos.toInt - 1).toString, "")
        }
      }
      val nbComment: Int = try {
        DseGraphFactory.dseConn.executeGraph(getTotalCommentsQuery(spokId)).one().asInt()
      } catch {
        case ex: Exception => 0
      }
      Some(CommentsResponse(previous, next, nbComment, commentsRes))
    } catch {
      case ex: Exception => None
    }
  }

  /**
   *
   * @param spokId of spok for which the comment details have to be gotten
   * @param from for pagination
   * @param to for pagination
   * @return
   */
  def commentDetails(spokId: String, from: Int, to: Int): List[Comments] = {
    val commentVertex = DseGraphFactory.dseConn.executeGraph(getCommentsQuery(spokId, from, to)).asScala.toList
    commentVertex map { commentVertex =>
      val reSpokerV = commentVertex.asVertex()
      val text = reSpokerV.getProperty(COMMENT_TEXT).getValue.asString()
      val commenterUserId = reSpokerV.getProperty(COMMENTER_ID).getValue.asString()
      val commentId = reSpokerV.getProperty(COMMENT_ID).getValue.asString()
      val commenterUserV = DseGraphFactory.dseConn.executeGraph(getUser(commenterUserId)).one().asVertex()
      val timeStamp = DseGraphFactory.dseConn.executeGraph(getCommentsTimeStamp(spokId, commentId)).one().asString()
      commentsResponse(text, timeStamp, commenterUserV, commentId)

    }
  }

  private def commentsResponse(text: String, timeStamp: String, commenterUserV: Vertex, commentId: String) = {
    val timeStampToLong = timeStamp.toLong
    val timeStampToDate = new Date(timeStampToLong)
    Comments(
      commentId,
      timeStampToDate,
      text,
      UserMinimalDetailsResponse(
        commenterUserV.getProperty(USER_ID).getValue.asString(),
        commenterUserV.getProperty(NICKNAME).getValue.asString(),
        commenterUserV.getProperty(GENDER).getValue.asString(),
        commenterUserV.getProperty(PICTURE).getValue.asString()
      )
    )
  }

  /**
   *
   * @param spokId of the spok for which the details have to be checked
   * @param from for pagination
   * @param to for pagination
   * @return
   */
  def commentDetailsForFullSpok(spokId: String, from: Int, to: Int): List[CommentsForFullSpok] = {
    val commentVertex = DseGraphFactory.dseConn.executeGraph(getCommentsQuery(spokId, from, to)).asScala.toList
    commentVertex map { commentVertex =>
      val reSpokerV = commentVertex.asVertex()
      val text = reSpokerV.getProperty(COMMENT_TEXT).getValue.asString()
      val commenterUserId = reSpokerV.getProperty(COMMENTER_ID).getValue.asString()
      val commentId = reSpokerV.getProperty(COMMENT_ID).getValue.asString()
      val commenterUserV = DseGraphFactory.dseConn.executeGraph(getUser(commenterUserId)).one().asVertex()
      val name = commenterUserV.getProperty(NICKNAME).getValue.asString()
      val picture = commenterUserV.getProperty(PICTURE).getValue.asString()
      val gender = commenterUserV.getProperty(GENDER).getValue.asString()
      val timeStamp = DseGraphFactory.dseConn.executeGraph(getCommentsTimeStamp(spokId, commentId)).one().asString()
      CommentsForFullSpok(text, new Date(timeStamp.toLong), commenterUserId, name, picture, gender)
    }
  }

  /**
   * This function is used to view 10 re-spokers of a spok.
   *
   * @param spokId
   * @param pos
   * @return
   */
  def getReSpokers(spokId: String, pos: String): Option[ReSpokerResponse] = {
    try {
      val reSpokerPerPage: Int = pageSize
      val from = (pos.toInt - 1) * reSpokerPerPage
      val to = from + reSpokerPerPage + 1
      val reSpokers: List[ReSpoker] = reSpokersDetails(spokId, from, to)

      val reSpokersRes = if (reSpokers.size > pageSize) {
        reSpokers.dropRight(1)
      } else {
        reSpokers
      }
      val (previous, next): (String, String) = {
        if (reSpokers.size > pageSize) {
          ((pos.toInt - 1).toString, (pos.toInt + 1).toString)
        } else {
          ((pos.toInt - 1).toString, "")
        }
      }
      Some(ReSpokerResponse(previous, next, reSpokersRes))
    } catch {
      case ex: Exception => None
    }
  }

  def reSpokersDetails(spokId: String, from: Int, to: Int): List[ReSpoker] = {
    val reSpokersVertex = DseGraphFactory.dseConn.executeGraph(getReSpokersQuery(spokId, from, to)).asScala.toList
    reSpokersVertex map { reSpokerVertex =>
      val reSpokerV = reSpokerVertex.asVertex()
      ReSpoker(
        reSpokerV.getProperty(USER_ID).getValue.asString(),
        reSpokerV.getProperty(NICKNAME).getValue.asString(),
        reSpokerV.getProperty(GENDER).getValue.asString(),
        reSpokerV.getProperty(PICTURE).getValue.asString()
      )
    }
  }

  /**
   * This function is used to view 10 scoped user of a spok.
   *
   * @param spokId
   * @param pos
   * @return
   */
  def getScopedUsers(spokId: String, pos: String): Option[ScopedUsersResponse] = {
    try {
      val scopedUserPerPage: Int = pageSize
      val from = (pos.toInt - 1) * scopedUserPerPage
      val to = from + scopedUserPerPage + 1
      val scopedUsers: List[ScopedUsers] = scopedUsersDetails(spokId, from, to)
      val scopedUsersRes = if (scopedUsers.size > pageSize) {
        scopedUsers.dropRight(1)
      } else {
        scopedUsers
      }
      val (previous, next): (String, String) = {
        if (scopedUsers.size > pageSize) {
          ((pos.toInt - 1).toString, (pos.toInt + 1).toString)
        } else {
          ((pos.toInt - 1).toString, "")
        }
      }

      Some(ScopedUsersResponse(previous, next, scopedUsersRes))
    } catch {
      case ex: Exception => None
    }
  }

  def scopedUsersDetails(spokId: String, from: Int, to: Int): List[ScopedUsers] = {
    val scopedUsersVertex = DseGraphFactory.dseConn.executeGraph(getScopedUsersQuery(spokId, from, to)).asScala.toList
    scopedUsersVertex map { scopedUserVertex =>
      val scopedUserV = scopedUserVertex.asVertex()
      ScopedUsers(
        scopedUserV.getProperty("userId").getValue.asString(),
        scopedUserV.getProperty("nickname").getValue.asString(),
        scopedUserV.getProperty("gender").getValue.asString(),
        scopedUserV.getProperty("picture").getValue.asString()
      )
    }
  }

  /**
   * This function is used to view short spok.
   *
   * @param spokId
   * @param userId
   */
  def viewShortSpok(spokId: String, targetUserId: String, userId: String, spokVertex: Vertex): Option[ViewSpok] = {
    try {
      Some(getSpokDetails(spokId, spokVertex, targetUserId, userId))
    } catch {
      case ex: Exception =>
        info("exception => " + ex.getMessage)
        None
    }
  }

  def getSpokDetails(spokId: String, spokVertex: Vertex, targetUserId: String, userId: String): ViewSpok = {
    val contentType = spokVertex.getProperty("contentType").getValue.asString()
    val ttl = spokVertex.getProperty("ttl").getValue.asInt()
    val launched = new Date(spokVertex.getProperty("launched").getValue.asLong())
    val originalHeading = spokVertex.getProperty(HEADER_TEXT).getValue.asString()
    //Fetch spok stats
    val statsCounter = getStatsCounter(spokId)
    //Fetch content type
    val content = getSpokContentDetails(spokId, contentType)
    //get author details
    val authorId = spokVertex.getProperty("author").getValue.asString()
    val authorDetails: Spoker = getUserDetails(authorId)
    val subscriber: Boolean = isSubscriberExist(spokId, userId)
    val respoked = isCreatorOrRespoked(spokId, userId)
    val (respokedTime, curHeading, visibility, fromDetails) = if (!targetUserId.isEmpty) {
      val edgeBetweenUserAndSpok: Edge = DseGraphFactory.dseConn.executeGraph(getEdgeBetweenUserAndSpok(targetUserId, spokId)).one().asEdge()
      val respokedTime = new Date(edgeBetweenUserAndSpok.getProperty("launched").getValue.asLong())
      val curHeading = edgeBetweenUserAndSpok.getProperty(HEADER_TEXT).getValue.asString()
      val visibility = edgeBetweenUserAndSpok.getProperty("visibility").getValue.asString()
      //get from/respoker details
      val fromId = edgeBetweenUserAndSpok.getProperty("from").getValue.asString()
      val fromDetails = getUserDetails(fromId)
      (Some(respokedTime), curHeading, visibility, fromDetails)
    } else {
      (None, "", "", Spoker("", "", "", ""))
    }
    ViewSpok(spokId, contentType, ttl, launched, originalHeading, respokedTime, curHeading, subscriber, authorDetails, fromDetails, visibility, statsCounter, content, respoked)
  }

  private def getUserDetails(authorId: String): Spoker = {
    val authorVertex = DseGraphFactory.dseConn.executeGraph(getUser(authorId)).one().asVertex()
    val userId = authorVertex.getProperty("userId").getValue.asString()
    val nickname = authorVertex.getProperty("nickname").getValue.asString()
    val gender = authorVertex.getProperty("gender").getValue.asString()
    val picture = authorVertex.getProperty("picture").getValue.asString()
    Spoker(userId, nickname, gender, picture)
  }

  private def getStatsCounter(spokId: String): Counters = {
    try {
      val spokStatsVertex = DseGraphFactory.dseConn.executeGraph(fetchStatsVertex(spokId)).one().asVertex()
      val nbSpoked = spokStatsVertex.getProperty("nb_respoked").getValue.asLong()
      val nbScoped = spokStatsVertex.getProperty("nb_users").getValue.asLong()
      val nbComments = spokStatsVertex.getProperty("nb_comments").getValue.asLong()
      val distance = spokStatsVertex.getProperty("travelled").getValue.asDouble()
      Counters(nbSpoked, nbScoped, nbComments, distance)
    } catch {
      case ex: Exception => Counters(0, 0, 0, 0)
    }

  }

  private def getSpokContentDetails(spokId: String, contentType: String): Content = {
    val spokContentVertex: Vertex = DseGraphFactory.dseConn.executeGraph(fetchSpokContentVertex(spokId)).one().asVertex()
    contentType match {
      case TEXT_TYPE => Content(rawText = Some(spokContentVertex.getProperty("text").getValue.asString()))
      case PICTURE_TYPE => getFileContent(spokId, spokContentVertex, contentType)
      case ANIMATED_GIF_TYPE => getFileContent(spokId, spokContentVertex, contentType)
      case HTML_TEXT => Content(htmlText = Some(spokContentVertex.getProperty("text").getValue.asString()))
      case _ => handleMoreSpokContents(spokId, spokContentVertex, contentType)
    }
  }

  private def handleMoreSpokContents(spokId: String, spokContentVertex: Vertex, contentType: String) = {
    contentType match {
      case VIDEO_TYPE => getFileContent(spokId, spokContentVertex, contentType)
      case SOUND_TYPE => getFileContent(spokId, spokContentVertex, contentType)
      case URL_TYPE => getUrlContent(spokId, spokContentVertex)
      case _ => handleRiddleAndPollContentTypes(spokId, spokContentVertex, contentType)
    }
  }

  private def handleRiddleAndPollContentTypes(spokId: String, spokContentVertex: Vertex, contentType: String) = {
    contentType match {
      case RIDDLE_TYPE => getRiddleContent(spokId, spokContentVertex)
      case POLL_TYPE => getPollContent(spokId, spokContentVertex)
    }
  }

  private def getFileContent(spokId: String, spokContentVertex: Vertex, contentType: String): Content = {
    val fileUrl = spokContentVertex.getProperty("file").getValue.asString()
    contentType match {
      case PICTURE_TYPE =>
        Content(picturePreview = Some(fileUrl), pictureFull = Some(fileUrl))
      case ANIMATED_GIF_TYPE =>
        Content(animatedGif = Some(fileUrl))
      case VIDEO_TYPE =>
        Content(videoPreview = Some(fileUrl), video = Some(fileUrl))
      case SOUND_TYPE =>
        Content(soundPreview = Some("https://spok-media.s3.amazonaws.com/image" + rnd.nextInt(6) + ".png"), sound = Some(fileUrl))
    }
  }

  private def getUrlContent(spokId: String, spokContentVertex: Vertex): Content = {
    val address = spokContentVertex.getProperty("address").getValue.asString()
    val preview = spokContentVertex.getProperty("preview").getValue.asString()
    val urlType = spokContentVertex.getProperty("urlType").getValue.asString()
    val title = spokContentVertex.getProperty("title").getValue.asString()
    val text = spokContentVertex.getProperty("text").getValue.asString()
    Content(url = Some(address), urlPreview = Some(preview), urlType = Some(urlType), urlTitle = Some(title), urlText = Some(text))
  }

  private def getRiddleContent(spokId: String, spokContentVertex: Vertex): Content = {
    val questionText = DseGraphFactory.dseConn.executeGraph(fetchRiddleQuestionVertex(spokId)).one().asVertex().getProperty("text").getValue.asString()
    val answerText = DseGraphFactory.dseConn.executeGraph(fetchRiddleAnswerVertex(spokId)).one().asVertex().getProperty("text").getValue.asString()
    val title = spokContentVertex.getProperty("title").getValue.asString()
    Content(riddleQuestion = Some(questionText), riddleAnswer = Some(answerText), riddleTitle = Some(title))
  }

  private def getPollContent(spokId: String, spokContentVertex: Vertex): Content = {
    val pollVertex = DseGraphFactory.dseConn.executeGraph(fetchPollVertex(spokId)).one().asVertex()
    val title = spokContentVertex.getProperty("title").getValue.asString()
    val description = spokContentVertex.getProperty("desc").getValue.asString()
    val questionsVertex = DseGraphFactory.dseConn.executeGraph(fetchPollQuestionVertex(spokId)).asScala.toList
    val questions = questionsVertex.map { quesGraphNode =>
      val quesVertex = quesGraphNode.asVertex()
      val questionId = quesVertex.getProperty("id").getValue.asString()
      val questionText = quesVertex.getProperty("text").getValue.asString()
      val questionRank = quesVertex.getProperty("rank").getValue.asString()
      val answersVertex = DseGraphFactory.dseConn.executeGraph(getCurrentQuestionsAllAnswers(questionId)).asScala.toList
      val answers = answersVertex.map { ansGraphNode =>
        val ansVertex = ansGraphNode.asVertex()
        val answerId = ansVertex.getProperty("id").getValue.asString()
        val answerText = ansVertex.getProperty("text").getValue.asString()
        val answerRank = ansVertex.getProperty("rank").getValue.asString()
        AnswersInterim(answerId, answerText, answerRank)
      }.sortBy {
        case AnswersInterim(id, text, rank) => (rank)
      }
      val answersOrdered = answers.map { ans =>
        Answers(ans.id, ans.text)
      }
      QuestionsInterim(questionId, questionText, answersOrdered, questionRank)
    }.sortBy {
      case QuestionsInterim(id, question, answers, rank) => (rank)
    }
    val questionsOrdered = questions.map { ques =>
      Questions(ques.id, ques.question, ques.answers)
    }
    Content(pollTitle = Some(title), pollDescription = Some(description), pollQuestions = Some(questionsOrdered))
  }

  /**
   * This method is used to load spok stack.
   *
   * @param userId
   * @param pos
   * @return
   */
  def getSpokStack(userId: String, pos: String): Option[SpoksStackResponse] = {
    try {
      val spokPerPage: Int = pageSize
      val from = (pos.toInt - 1) * spokPerPage
      val to = from + spokPerPage + 1
      val spokV = DseGraphFactory.dseConn.executeGraph(getSpokStackQuery(userId, from, to)).asScala.toList
      val (previous, next): (String, String) = {
        if (spokV.size > pageSize) {
          ((pos.toInt - 1).toString, (pos.toInt + 1).toString)
        } else {
          ((pos.toInt - 1).toString, "")
        }
      }
      val spokVList = if (spokV.size > pageSize) {
        spokV.dropRight(1)
      } else {
        spokV
      }
      val spokStackRes: List[ViewSpok] = spokVList map { spokVertex =>
        val spokId = spokVertex.asVertex().getProperty(SPOK_ID).getValue.asString()
        getSpokDetails(spokId, spokVertex.asVertex(), "", userId)
      }

      Some(SpoksStackResponse(previous, next, spokStackRes))
    } catch {
      case ex: Exception => None
    }
  }

  /**
   * This method will return details of spoker's wall.
   *
   * @param targetUserId
   * @param pos
   * @return
   */
  def getSpokersWallDetails(targetUserId: String, pos: String, userId: String): Option[UsersWallResponse] = {

    val spoksPage: Int = pageSize
    val from = (pos.toInt - 1) * spoksPage
    val to = from + spoksPage + 1
    val usersWallVertex = DseGraphFactory.dseConn.executeGraph(getUsersWallQuery(targetUserId, from, to)).asScala.toList
    val (previous, next): (String, String) = {
      if (usersWallVertex.size > pageSize) {
        ((pos.toInt - 1).toString, (pos.toInt + 1).toString)
      } else {
        ((pos.toInt - 1).toString, "")
      }
    }
    val userWallList = if (usersWallVertex.size > pageSize) {
      usersWallVertex.dropRight(1)
    } else {
      usersWallVertex
    }
    val usersWallRes: List[Spoks] = spokersWallDetails(targetUserId, userId, userWallList)

    Some(UsersWallResponse(previous, next, usersWallRes))
  }

  private def spokersWallDetails(targetUserId: String, userId: String, usersWallVertex: List[GraphNode]): List[Spoks] = {
    val spokList = usersWallVertex map { wallVertex =>
      try {
        val spokId = wallVertex.asVertex().getProperty(SPOK_ID).getValue.asString()
        val viewSpok = getSpokDetails(spokId, wallVertex.asVertex(), targetUserId, userId)
        Spoks(viewSpok.id, viewSpok.spokType, viewSpok.launched, viewSpok.text, viewSpok.respoked.getOrElse(new Date()),
          viewSpok.curtext, isSubscriberExist(spokId, userId), viewSpok.respoker, viewSpok.counters, viewSpok.content, viewSpok.flag)
      } catch {
        case ex: Exception =>
          info("spokid error :" + wallVertex.asVertex().getProperty(SPOK_ID).getValue.asString)
          Spoks("", "", new Date(), "", new Date(), "", false, Spoker("", "", "", ""), Counters(0, 0, 0, 0), Content())
      }
    }
    spokList.filter(_.id.nonEmpty)
  }

  /**
   * This function is used to view full spok.
   *
   * @param spokId
   * @param userId
   */
  def viewFullSpok(spokId: String, targetUserId: String, userId: String, spokVertex: Vertex): Option[ViewFullSpok] = {
    try {
      val ten = 10
      val spokDetails = getSpokDetails(spokId, spokVertex, targetUserId, userId)
      val lastTenReSpokers = reSpokersDetails(spokId, 0, ten)
      val lastTenScopedUsers = scopedUsersDetails(spokId, 0, ten)
      val lastTenComments = commentDetailsForFullSpok(spokId, 0, ten)
      val subscriber = isSubscriberExist(spokId, userId)
      Some(ViewFullSpok(spokDetails.id, spokDetails.spokType, spokDetails.ttl, spokDetails.launched, spokDetails.text, spokDetails.respoked,
        spokDetails.curtext, subscriber, spokDetails.author, spokDetails.respoker, spokDetails.visibility, spokDetails.counters, lastTenReSpokers,
        lastTenScopedUsers, lastTenComments, spokDetails.content, spokDetails.flag))
    } catch {
      case ex: Exception => None
    }
  }

  /**
   * Method to view poll stats. will only show result to the creator or those who have completed the poll
   *
   * @param spokId to which the poll is attached
   * @param userId
   * @return
   */
  def viewPollStats(spokId: String, userId: String): (Option[PollStats], Option[Error]) = {
    try {
      val userAllowedToView = isUserAllowedToViewStats(spokId, userId)
      userAllowedToView match {
        case Some(spokerSpokEdge) => {
          val pollV = DseGraphFactory.dseConn.executeGraph(fetchPollVertex(spokId)).one().asVertex()
          val pollId = pollV.getProperty(ID).getValue.asString()
          val pollText = spokerSpokEdge.getProperty(HEADER_TEXT).getValue.asString()
          val pollDesc = pollV.getProperty(DESC).getValue.asString()
          val nbFinished = pollV.getProperty(NB_FINISHED).getValue.asInt()
          val questions = getStatsOfPollQuestions(spokId)
          val result = PollStats(pollId, Some(pollText), Some(pollDesc), nbFinished, questions)
          (Some(result), None)
        }
        case None => (None, Some(Error(SPK_014, CANNOT_VIEW_SPOK_STATS)))
      }
    } catch {
      case ex: NullPointerException => (None, Some(Error(SPK_101, s"Spok $spokId not found.")))
      case ex: Exception => (None, Some(Error(SPK_125, s"$GENERIC_ERROR_SPOK_VIEWING $spokId (generic error).")))
    }
  }

  private def isUserAllowedToViewStats(spokId: String, userId: String): Option[Edge] = {
    val spokerSpokEdge = DseGraphFactory.dseConn.executeGraph(getEdgeBetweenUserAndSpok(userId, spokId)).one().asEdge()
    val pendingQuestions = spokerSpokEdge.getProperty(PENDING_QUESTIONS).getValue.asInt()
    if (pendingQuestions == 0) Some(spokerSpokEdge) else None
  }

  private def getStatsOfPollQuestions(spokId: String): List[PollQuestionsStats] = {
    val questionsVertex = DseGraphFactory.dseConn.executeGraph(fetchPollQuestionVertex(spokId)).asScala.toList
    val questions = questionsVertex.par.map { questionV =>
      val questV = questionV.asVertex()
      val questionId = questV.getProperty(ID).getValue.asString()
      val questionText = questV.getProperty(TEXT).getValue.asString()
      val questionRank = questV.getProperty(RANK).getValue.asString()
      val answers = DseGraphFactory.dseConn.executeGraph(getCurrentQuestionsAllAnswers(questionId)).asScala.toList.par.map {
        answerVertex =>
          val answerV = answerVertex.asVertex()
          val answerId = answerV.getProperty(ID).getValue.asString()
          val answerText = answerV.getProperty(TEXT).getValue.asString()
          val answerRank = answerV.getProperty(RANK).getValue.asString()
          val nbAnswered = answerV.getProperty(NB_ANSWERED).getValue.asInt()
          PollAnswersStatsInterim(answerId, answerText, nbAnswered, answerRank.toInt)
      }.toList.sortBy {
        case PollAnswersStatsInterim(answerId, answerText, nbAnswered, rank) => rank
      }
      val answersOrdered = answers.par.map { ans =>
        PollAnswerStats(ans.answerId, ans.answerText, ans.nbAnswered)
      }.toList
      PollQuestionsStatsInterim(questionId, questionText, answersOrdered, questionRank.toInt)
    }.toList.sortBy {
      case PollQuestionsStatsInterim(questionId, questionText, answersOrdered, questionRank) => questionRank
    }
    questions.par.map { quest =>
      PollQuestionsStats(quest.id, quest.text, quest.answers)
    }.toList
  }

  /**
   * This function is used to check a user is a subscriber of a spok or not
   *
   * @param spokId spok id
   * @param userId user id
   * @return
   */

  def isSubscriberExist(spokId: String, userId: String): Boolean = {
    try {
      val listOfStrings = new TypeToken[String]() {}
      val row = SpokLogging.isSubscriber(spokId, subscriberDetails).one()
      row.getSet("userids", listOfStrings).asScala.toList contains userId
    } catch {
      case ex: Exception => false
    }
  }

  def isCreatorOrRespoked(spokId: String, userId: String): Option[String] = {
    try {
      val edgeBetweenUserAndSpok = DseGraphFactory.dseConn.executeGraph(getEdgeBetweenUserAndSpok(userId, spokId)).one().asEdge()
      val status = edgeBetweenUserAndSpok.getProperty(STATUS).getValue.asString()
      if (status == RESPOKED) {
        val spokV = DseGraphFactory.dseConn.executeGraph(fetchSpokVertex(spokId)).one().asVertex()
        val authorId = spokV.getProperty(AUTHOR).getValue.asString()
        if (authorId equals userId) Some(CREATOR)
        else Some(RESPOKER)
      } else None
    } catch {
      case ex: Exception => None
    }
  }

  /**
   * This method is used to load my spoks.
   *
   * @param userId
   * @param pos
   * @return
   */
  def getMySpoks(userId: String, pos: String): Option[SpoksStackResponse] = {
    try {
      val spokPerPage: Int = pageSize
      val from = (pos.toInt - 1) * spokPerPage
      val to = from + spokPerPage + 1
      val spokV = DseGraphFactory.dseConn.executeGraph(getMySpokQuery(userId, from, to)).asScala.toList
      val (previous, next): (String, String) = {
        if (spokV.size > pageSize) {
          ((pos.toInt - 1).toString, (pos.toInt + 1).toString)
        } else {
          ((pos.toInt - 1).toString, "")
        }
      }
      val spokVList = if (spokV.size > pageSize) {
        spokV.dropRight(1)
      } else {
        spokV
      }

      val spokStackRes: ParSeq[ViewSpok] = spokVList.par map { spokVertex =>
        val spokId = spokVertex.asVertex().getProperty(SPOK_ID).getValue.asString()
        getSpokDetails(spokId, spokVertex.asVertex(), "", userId)
      }
      Some(SpoksStackResponse(previous, next, spokStackRes.toList))
    } catch {
      case ex: Exception => None
    }
  }

}

object DSESpokViewApi extends DSESpokViewApi
