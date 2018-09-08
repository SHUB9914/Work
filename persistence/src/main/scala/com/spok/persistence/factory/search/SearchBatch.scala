package com.spok.persistence.factory.search

import com.spok.model.Search.BatchSpok
import com.spok.model.SpokModel.{ Answers, Content, LastSpoks, Questions }
import com.datastax.driver.dse.graph.Vertex
import com.spok.model.Account.PopularSpokerDetails
import com.spok.model.Search.PopularSpoker
import com.spok.persistence.dse.DseConnectionUri._
import com.spok.persistence.dse.DseGraphFactory
import com.spok.persistence.factory.spokgraph.DSESpokViewApi
import com.spok.persistence.factory.spoklog.SpokLogging
import com.spok.util.Constant._
import com.spok.util.{ JsonHelper, LoggerUtil }

import scala.collection.JavaConverters._
import scala.collection.parallel.immutable.ParSeq

trait SearchBatch extends SearchQuery with LoggerUtil with DSESpokViewApi with JsonHelper {

  val spokLogging: SpokLogging

  /**
   * This function will create batch view for  most trendy spok.
   *
   * @param startTime
   * @param endTime
   */
  def getTrendySpokInBatch(startTime: Long, endTime: Long): Boolean = {
    info("fetching trendy spoks for start time :: " + startTime + "  & end time :: " + endTime)
    try {
      val spokV = DseGraphFactory.dseOlapConn.executeGraph(getSpokQueryForBatch(startTime, endTime)).asScala.toList
      val listTrendySpok: ParSeq[LastSpoks] = spokV.par map { spokVertex =>
        val spokId = spokVertex.asVertex().getProperty(SPOK_ID).getValue.asString()
        val spok = getSpokDetails(spokId, spokVertex.asVertex(), "", "")
        val spokText = spok.text.replaceAll("'", "''")
        val spokContent = getSpokContent(spok.content)
        LastSpoks(spok.id, spok.spokType, spok.ttl, spok.launched, spokText, spok.author, spok.visibility, spok.counters, spokContent)
      }
      val trendySpoks: List[LastSpoks] = listTrendySpok.toList.sortBy {
        case LastSpoks(id, spokType, ttl, launched, text, author, visibility, counters,
          content, subscriptionStatus, flag) => (counters.nbSpoked, counters.distance, counters.nbScoped, counters.nbComments, launched.getTime)
      }
      insertSpok(trendySpoks, trendySpok)
      true
    } catch {
      case ex: Exception =>
        info(s"Exception while getting TrendySpok" + ex.getMessage)
        false
    }
  }

  private def insertSpok(spoks: List[LastSpoks], tableName: String) = {
    for (i <- 0 until spoks.length) {
      val dataJson = write(spoks(i))
      val spokJson = write(BatchSpok(spoks(i).id, timeStamp + i, dataJson))
      spokLogging.insertHistory(spokJson, tableName)
    }
  }

  private def getSpokContent(spokContent: Content): Content = {
    spokContent.copy(
      picturePreview = getContent(spokContent.picturePreview),
      pictureFull = getContent(spokContent.pictureFull),
      animatedGif = getContent(spokContent.animatedGif),
      videoPreview = getContent(spokContent.videoPreview),
      video = getContent(spokContent.video),
      soundPreview = getContent(spokContent.soundPreview),
      sound = getContent(spokContent.sound),
      url = getContent(spokContent.url),
      urlPreview = getContent(spokContent.urlPreview),
      urlType = getContent(spokContent.urlType),
      urlTitle = getContent(spokContent.urlTitle),
      urlText = getContent(spokContent.urlText),
      rawText = getContent(spokContent.rawText),
      htmlText = getContent(spokContent.htmlText),
      pollTitle = getContent(spokContent.pollTitle),
      pollDescription = getContent(spokContent.pollDescription),
      pollQuestions = getQuestions(spokContent.pollQuestions),
      riddleQuestion = getContent(spokContent.riddleQuestion),
      riddleAnswer = getContent(spokContent.riddleAnswer)
    )
  }

  private def getContent(contentOpt: Option[String]): Option[String] = {
    contentOpt match {
      case Some(content) => Some(content.replaceAll("'", "''"))
      case None => None
    }
  }

  private def getQuestions(questionsOpt: Option[List[Questions]]): Option[List[Questions]] = {
    questionsOpt match {
      case Some(questions) =>
        Some(questions.map { ques =>
          val answers: List[Answers] = ques.answers.map { ans =>
            ans.copy(text = ans.text.replaceAll("'", "''"))
          }
          ques.copy(question = ques.question.replaceAll("'", "''"), answers = answers)
        })
      case None => None
    }
  }

  /**
   * This function will create batch view for last spok.
   *
   * @param startTime
   * @param endTime
   * @return
   */
  def getLastSpokInBatch(startTime: Long, endTime: Long): Boolean = {
    info("fetching last spoks for start time :: " + startTime + "  & end time :: " + endTime)
    try {
      val spokV = DseGraphFactory.dseOlapConn.executeGraph(getSpokQueryForBatch(startTime, endTime)).asScala.toList
      val listLastSpok: ParSeq[LastSpoks] = spokV.par map { spokVertex =>
        val spokId = spokVertex.asVertex().getProperty(SPOK_ID).getValue.asString()
        val spok = getSpokDetails(spokId, spokVertex.asVertex(), "", "")
        val spokText = spok.text.replaceAll("'", "''")
        val spokContent = getSpokContent(spok.content)
        LastSpoks(spok.id, spok.spokType, spok.ttl, spok.launched, spokText, spok.author, spok.visibility, spok.counters, spokContent)
      }
      val lastSpoks: List[LastSpoks] = listLastSpok.toList.sortWith(_.launched.getTime < _.launched.getTime)
      insertSpok(lastSpoks, lastSpok)
      true
    } catch {
      case ex: Exception =>
        info(s"Exception while getting Last Spok :::: " + ex.getMessage)
        false
    }
  }

  /**
   *
   * @param startTime
   * @param endTime
   */
  def getPopularSpokersInBatch(startTime: Long, endTime: Long): Boolean = {
    try {
      val userVs: List[Vertex] = DseGraphFactory.dseConn.executeGraph(getPopularSpokerQuery(startTime, endTime)).asScala.toList.map(node => node.asVertex())

      val spokerDetails = userVs.par map { userV => viewMyProfile(userV.getProperty(USER_ID).getValue.asString(), userV) }
      val popularSpoker: List[PopularSpokerDetails] = spokerDetails.toList.sortBy {
        case PopularSpokerDetails(id, nickname, gender, picture, cover, launched, nbFollowers, nbFollowing, nbSpoks) => (nbFollowers, nbSpoks, launched)
      }
      insertSpoker(popularSpoker)
      info(s" popular spoker batch has been inserted :")
      true
    } catch {
      case ex: Exception =>
        info(s"Exception while inserting popular spok :" + ex.getMessage)
        false
    }
  }

  private def viewMyProfile(userId: String, userV: Vertex): PopularSpokerDetails = {
    val followerCount = DseGraphFactory.dseConn.executeGraph(getFollowerCount(userId)).one().asLong()
    val followingCount = DseGraphFactory.dseConn.executeGraph(getFollowingCount(userId)).one().asLong()
    val spokCount = DseGraphFactory.dseConn.executeGraph(getSpokCount(userId)).one().asLong()
    PopularSpokerDetails(
      userId,
      userV.getProperty(NICKNAME).getValue.asString(),
      userV.getProperty(GENDER).getValue.asString(),
      userV.getProperty(PICTURE).getValue.asString(),
      userV.getProperty(COVER).getValue.asString(),
      userV.getProperty(LAUNCHED).getValue.asLong(),
      followerCount, followingCount, spokCount
    )
  }

  private def insertSpoker(spokers: List[PopularSpokerDetails]) = {
    for (i <- 0 until spokers.length) {
      val dataJson = write(spokers(i))
      val spokerJson = write(PopularSpoker(spokers(i).id, timeStamp + i, dataJson))
      spokLogging.insertHistory(spokerJson, popularSpoker)
    }
  }

}

object SearchBatch extends SearchBatch {

  val spokLogging: SpokLogging = SpokLogging

}
