package com.spok.persistence.factory.spokgraph

import com.datastax.driver.dse.graph.{ GraphResultSet, SimpleGraphStatement, Vertex }
import com.spok.model.SpokModel._
import com.spok.persistence.dse.DseGraphFactory
import com.spok.util.Constant._
import com.spok.util.LoggerUtil

trait DSESpokQuery extends LoggerUtil {

  def insertSpok(spokVertexProperties: SpokVertex): String =
    s"""graph.addVertex(label,"$DSE_SPOK","$CONTENT_TYPE","${spokVertexProperties.contentType}",
        | "ttl" , "${spokVertexProperties.ttl}" ,"$HEADER_TEXT" , "${spokVertexProperties.headerText.replace("$", "\\$").replace(QUOTES, """\"""")}"  ,
        | "geo_latitude" ,  "${spokVertexProperties.geo.latitude}" , "geo_longitude" ,  "${spokVertexProperties.geo.longitude}",
        | "geo_elevation" ,  "${spokVertexProperties.geo.elevation}" , "$SPOK_ID" , "${spokVertexProperties.spokId}",
        | "$LAUNCHED" ,  "${spokVertexProperties.launched}", "$ENABLED" ,  "${spokVertexProperties.enabled}"
        | ,"$AUTHOR" ,"${spokVertexProperties.author}", "original_visibility" ,"${spokVertexProperties.orginalVisibility}"
        | ,"$ACTUAL_QUESTIONS","${spokVertexProperties.actualQuestions}")""".stripMargin

  def insertUrl(urlAttributes: Url): String =
    s"""graph.addVertex(label,"$DSE_URL","address","${urlAttributes.address}",
        | "title", "${urlAttributes.title.replace("$", "\\$").replace(QUOTES, QUOTES_WITH_ESCAPE)}" ,
        | "text" , "${urlAttributes.text.replace("$", "\\$").replace(QUOTES, QUOTES_WITH_ESCAPE)}" ,
        | "preview" ,"${urlAttributes.preview}" , "urlType" , "${urlAttributes.urlType.getOrElse("")}" )""".stripMargin

  def insertPoll(pollAttributes: Poll, id: String): String =
    s"""graph.addVertex(label,"$POLL","id","$id","$TITLE","${pollAttributes.title.replace("$", "\\$")}",
        |"desc","${pollAttributes.desc.getOrElse("").replace("$", "\\$")}","nbFinished","0")""".stripMargin

  def questionVertex(question: PollQuestions, id: String): String =
    s"""graph.addVertex(label,"$QUESTION","id","$id","$TEXT","${question.text.replace("$", "\\$").replace(QUOTES, QUOTES_WITH_ESCAPE)}",
        |"type","${question.contentType.getOrElse("")}","preview","${question.preview.getOrElse("")}",
        |"rank","${question.rank}")""".stripMargin

  def answerVertex(answer: PollAnswers, id: String): String =
    s"""graph.addVertex(label,"$ANSWER","id","$id","text","${answer.text.replace("$", "\\$").replace(QUOTES, QUOTES_WITH_ESCAPE)}",
        |"type","${answer.contentType.getOrElse("")}","preview","${answer.preview.getOrElse("")}",
        |"rank","${answer.rank}","$NB_ANSWERED","0")""".stripMargin

  def insertRiddle(riddleAttributes: Riddle): String =
    s"""graph.addVertex(label,"$RIDDLE","title","${riddleAttributes.title.replace("$", "\\$").replace(QUOTES, QUOTES_WITH_ESCAPE)}")""".stripMargin

  def riddleQuestionVertex(question: RiddleQuestion, id: String): String =
    s"""graph.addVertex(label,"$QUESTION","id","$id","text","${question.text.replace("$", "\\$").replace(QUOTES, QUOTES_WITH_ESCAPE)}",
        |"type","${question.`type`.getOrElse("")}",
        |"preview","${question.preview.getOrElse("")}")""".stripMargin

  def riddleAnswerVertex(answer: RiddleAnswer, id: String): String =
    s"""graph.addVertex(label,"$ANSWER","id","$id","text","${answer.text.replace("$", "\\$").replace(QUOTES, QUOTES_WITH_ESCAPE)}",
        |"type","${answer.`type`.getOrElse("")}",
        |"preview","${answer.preview.getOrElse("")}")""".stripMargin

  def insertText(text: String): String =
    s"""graph.addVertex(label,"$TEXT","text","${text.replace("$", "\\$").replace(QUOTES, QUOTES_WITH_ESCAPE)}")""".stripMargin

  def insertFileUrl(fileUrl: String): String = s"""graph.addVertex(label,"$FILE","file","${fileUrl}")""".stripMargin

  def createSpokStats(spokStatistics: Statistics): String = {
    s"""graph.addVertex(label,"$STATISTICS","travelled","${spokStatistics.travelled}",
        | "nb_users" , "${spokStatistics.numberOfLanded}"  ,
        | "nb_pending" , "${spokStatistics.numberOfPending}"  ,
        | "nb_respoked" , "${spokStatistics.numberOfRespoked}" ,"nb_unspoked" , "${spokStatistics.numberOfUnspoked}" ,
        | "nb_comments" , "${spokStatistics.numberOfComment}")""".stripMargin
  }

  def updateSpokStats(spokId: String, stats: Statistics): String = {
    s"""g.V().hasLabel('$DSE_SPOK').has('$SPOK_ID','$spokId').outE('$HAS_STATS').inV()
        |.property('nb_comments','${stats.numberOfComment}').property('nb_unspoked','${stats.numberOfUnspoked}')
        |.property('nb_pending','${stats.numberOfPending}').property('nb_respoked','${stats.numberOfRespoked}')
        |.property('travelled','${stats.travelled}').property('nb_users','${stats.numberOfLanded}')""".stripMargin
  }

  def addEdgeSpokerSpok(vertexFirst: Vertex, vertexSecond: Vertex, edgeLabel: String, edgeProperties: SpokEdge): GraphResultSet = {
    val statement = new SimpleGraphStatement(
      "def v1 = g.V(id1).next()\n" + "def v2 = g.V(id2).next()\n" + s"v1.addEdge('$edgeLabel', v2 ," +
        s" '$STATUS','${edgeProperties.status}', '$LAUNCHED' , '${edgeProperties.launched}'" +
        s" , '$LATITUDE' , '${edgeProperties.geo.latitude}' , '$LONGITUDE' , '${edgeProperties.geo.longitude}' ,  " +
        s"'$ELEVATION' , '${edgeProperties.geo.longitude}' , '$FROM' , '${edgeProperties.from}' " +
        s", '$GROUP_ID' , '${edgeProperties.groupId}'" + s", '$PENDING_QUESTIONS','${edgeProperties.pendingQuestions}'" +
        s""" , '$VISIBILITY' , '${edgeProperties.visibility}' , "$HEADER_TEXT" , "${edgeProperties.headerText.replace("$", "\\$").replace(QUOTES, QUOTES_WITH_ESCAPE)}")"""
    ).set("id1", vertexFirst.getId()).set("id2", vertexSecond.getId())
    DseGraphFactory.dseConn.executeGraph(statement)
  }

  def addEdgeWithTime(vertexFirst: Vertex, vertexSecond: Vertex, edgeLabel: String, launchedTime: Long): GraphResultSet = {
    val statement = new SimpleGraphStatement(
      "def v1 = g.V(id1).next()\n" + "def v2 = g.V(id2).next()\n" + s"v1.addEdge('$edgeLabel', v2, '$LAUNCHED', '${launchedTime}')"
    ).set("id1", vertexFirst.getId()).set("id2", vertexSecond.getId())
    DseGraphFactory.dseConn.executeGraph(statement)
  }

  def updateUserGeoLocation(userId: String, geo: Geo): String = {
    s""" g.V().hasLabel('$USER').has('$USER_ID','$userId').outE('$ACTIVITY_GEO').inV()
        |.property('$LATITUDE','${geo.latitude}')
        |.property('$LONGITUDE','${geo.longitude}').property('$ELEVATION','${geo.elevation}')""".stripMargin
  }

  def fetchSpokVertex(spokId: String): String = {
    s"""g.V().hasLabel('$DSE_SPOK').has('$SPOK_ID','$spokId')"""
  }

  //TODO Convert query to run on spark
  def fetchSpokStats(spokId: String): String = {
    s"""g.V().hasLabel("$SPOK").has('$SPOK_ID','$spokId').match(
        |          __.as("c").inE('$ISASSOCIATEDWITH').has('$STATUS','$PENDING').outV().hasLabel('$USER').count().as("$PENDING_USERS_COUNT"),
        |          __.as("c").inE('$ISASSOCIATEDWITH').has('$STATUS','$UNSPOKED').count().as("$UNSPOKED_USERS_COUNT"),
        |          __.as("c").inE('$ISASSOCIATEDWITH').has('$STATUS','$RESPOKED').count().as("$RESPOKED_USERS_COUNT")).
        |          select("$PENDING_USERS_COUNT", "$UNSPOKED_USERS_COUNT" ,"$RESPOKED_USERS_COUNT")""".stripMargin
  }

  //TODO Convert query to run on spark
  def fetchCommentCount(spokId: String): Long = {
    val query = s"""g.V().hasLabel('$DSE_SPOK').has('$SPOK_ID','$spokId').outE('$HAS_A_COMMENT').count()""".stripMargin
    logger.info(s" $spokId Comment count query::" + query)
    DseGraphFactory.dseConn.executeGraph(query).one().asLong()
  }

  def getEdgeBetweenUserAndSpok(userId: String, spokId: String): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').outE('$ISASSOCIATEDWITH').as('e').inV().has('$SPOK_ID','$spokId').select('e')"""
  }

  def updateEdgeBetweenUserAndSpok(userId: String, spokId: String, status: String, respok: Respok): String = {
    s"""g.V().hasLabel("$USER").has("$USER_ID","$userId").outE("$ISASSOCIATEDWITH").as("e").inV().has("$SPOK_ID","$spokId").select("e")
       |.property("$STATUS","$status").property("$HEADER_TEXT","${respok.text.getOrElse("").replace("$", "\\$").replace(QUOTES, QUOTES_WITH_ESCAPE)}")
       |.property("$LATITUDE","${respok.geo.latitude}").property("$VISIBILITY","${respok.visibility.getOrElse(PUBLIC).toLowerCase}")
       |.property("$FROM","$userId").property("$LONGITUDE","${respok.geo.longitude}")
       |.property("$ELEVATION","${respok.geo.elevation}").property("$LAUNCHED","${respok.launched}")
       |.property("$GROUP_ID","${respok.groupId.getOrElse("0")}")""".stripMargin
  }

  def fetchStatsVertex(spokId: String): String = {
    s"""g.V().hasLabel('$DSE_SPOK').has('spokId','$spokId').outE('$HAS_STATS').inV().hasLabel('$STATISTICS')"""
  }

  def fetchUserGeo(userId: String): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').outE('$ACTIVITY_GEO').inV()"""
  }

  def fetchUserById(userId: String): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId')"""
  }

  def getPollQuestionVertex(questionId: String): String =
    s"""g.V().hasLabel('$QUESTION').has('$ID','$questionId').as('quest').inE('$HAS_A_QUESTION').outV()
     |.inE('$CONATINS_A').outV().as('spokV').select('quest','spokV')""".stripMargin

  def getPollIdVertex(questionId: String): String =
    s"""g.V().hasLabel('$QUESTION').has('$ID','$questionId').inE('$HAS_A_QUESTION').outV()"""

  def getQuestionVertex(pollId: String, questionRank: Int): String =
    s"""g.V().hasLabel('$POLL_LABEL').has('$ID','$pollId').outE('$HAS_A_QUESTION')
        |.inV().has('$RANK','$questionRank')
     """.stripMargin

  def getCurrentQuestionsAllAnswers(questionId: String): String =
    s"""g.V().hasLabel('$QUESTION').has('$ID','$questionId').outE('$HAS_AN_ANSWER').inV()"""

  def updateUnspokStatusEdge(userId: String, spokId: String, unspok: Unspok): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').outE('$ISASSOCIATEDWITH').as('e').inV().has('$SPOK_ID','$spokId').select('e')
        |.property('$STATUS','$UNSPOKED').property('$LATITUDE','${unspok.geo.latitude}').property('$LONGITUDE','${unspok.geo.longitude}')
        |.property('$ELEVATION','${unspok.geo.elevation}').property('$LAUNCHED','${unspok.launched}')""".stripMargin
  }

  def fetchSpokIdForPoll(questionId: String): String =
    s"""g.V().hasLabel('$QUESTION').has('id','$questionId')
        |.inE('$HAS_A_QUESTION').outV().inE('$CONATINS_A').outV()""".stripMargin

  def validatePollQuestionByIdQuery(questionId: String): String = s"""g.V().hasLabel('$QUESTION').has('id','$questionId')"""

  /**
   * This method will validate if user is the author of spok.
   *
   * @param spokId
   * @param userId
   * @return
   */
  def validateUserBySpokId(spokId: String, userId: String): String = {
    s"""g.V().hasLabel('$SPOK').has('$SPOK_ID','$spokId').has('$AUTHOR','$userId').hasNext()"""
  }

  /**
   * This mehtod will disable a spok.
   *
   * @param spokId
   * @return
   */
  def disableSpokQuery(spokId: String): String = {
    s"""g.V().hasLabel('$DSE_SPOK').has('$SPOK_ID','$spokId').property('$ENABLED','false')"""
  }

  /**
   * This method will remove spok from wall.
   *
   * @param userId
   * @param spokId
   * @param launchTime
   * @return
   */
  def removeSpokFromWallQuery(userId: String, spokId: String, launchTime: Long): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').outE('$ISASSOCIATEDWITH').as('e').inV()
       |.has('$SPOK_ID','$spokId').select('e').property('$STATUS','$REMOVED').property('$LAUNCHED','$launchTime')""".stripMargin
  }

  /**
   * This query will return spok content details.
   *
   * @param spokId
   * @return
   */
  def fetchSpokContentVertex(spokId: String): String = {
    s"""g.V().hasLabel('$DSE_SPOK').has('$SPOK_ID','$spokId').outE('$CONATINS_A').inV()"""
  }

  /**
   * This query will return riddle question content details.
   *
   * @param spokId
   * @return
   */
  def fetchRiddleQuestionVertex(spokId: String): String = {
    s"""g.V().hasLabel('$DSE_SPOK').has('$SPOK_ID','$spokId').outE('$CONATINS_A').inV().outE('$HAS_A_QUESTION').inV()"""
  }

  /**
   * This query will return riddle answer content details.
   *
   * @param spokId
   * @return
   */
  def fetchRiddleAnswerVertex(spokId: String): String = {
    s"""g.V().hasLabel('$DSE_SPOK').has('$SPOK_ID','$spokId').outE('$CONATINS_A').inV().outE('$HAS_A_QUESTION').inV().outE('$HAS_AN_ANSWER').inV()"""
  }

  /**
   * This query will return poll details.
   *
   * @param spokId
   * @return
   */
  def fetchPollVertex(spokId: String): String = {
    s"""g.V().hasLabel('$DSE_SPOK').has('$SPOK_ID','$spokId').outE('$CONATINS_A').inV()"""
  }

  /**
   * This query will return poll question details.
   *
   * @param spokId
   * @return
   */
  def fetchPollQuestionVertex(spokId: String): String = {
    s"""g.V().hasLabel('$DSE_SPOK').has('$SPOK_ID','$spokId').outE('$CONATINS_A').inV().outE('$HAS_A_QUESTION').inV()"""
  }

  /**
   * To find if followers is already linked with spok in pending state
   *
   * @param userId
   * @param SpokId
   * @return true is linked else false
   */
  def isUserAlreadyLinkedWithSpok(userId: String, SpokId: String): String = {
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').
        |outE('$ISASSOCIATEDWITH','$STATUS','$PENDING').inV().has('$DSE_SPOK','$SPOK_ID','$SpokId')
        |.hasNext()""".stripMargin
  }

  /**
   * To find if contact is already likned with spok in pendng state
   *
   * @param contactNo
   * @param spokId
   * @return true if linked else false
   */
  def isContactAlreadyLinkedWithSpok(contactNo: String, spokId: String): String = {
    s"""g.V().hasLabel('$MOBILE_NO').has('$PHONE_NO','$contactNo')
        |.outE('$ISASSOCIATEDWITH','$STATUS','$PENDING').inV().has('$DSE_SPOK','$SPOK_ID','$spokId')
        |.hasNext()""".stripMargin
  }

  def isContactAUser(contactNo: String): String = {
    s"""g.V().hasLabel('$MOBILE_NO').has('$PHONE_NO','$contactNo').inE('$HAS_A').hasNext()"""
  }

  def updatePendingQuestionQuery(userId: String, spokId: String, pendingQuestions: Int): String =
    s"""g.V().hasLabel('$USER').has('$USER_ID','$userId').outE('$ISASSOCIATEDWITH').as("e").inV().has('$SPOK_ID','$spokId')
     |.select('e').property('$PENDING_QUESTIONS','${pendingQuestions}')""".stripMargin

  def updatePollCompletedCountQuery(pollId: String, nbFinished: Int): String =
    s"""g.V().hasLabel('$POLL').has('$ID','$pollId').property('$NB_FINISHED','$nbFinished')""".stripMargin

  def updateAnsweredCountQuery(answerId: String, nbAnswered: Int): String = {
    s"""g.V().hasLabel('$ANSWER').has('$ID','$answerId').property('$NB_ANSWERED','$nbAnswered')"""
  }

}
