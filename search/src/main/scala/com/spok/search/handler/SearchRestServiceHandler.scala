package com.spok.search.handler

import akka.actor.ActorRef
import akka.util.Timeout
import com.spok.model.SpokModel.{ HashTag, Nickname }
import com.spok.search.service._
import com.spok.util.Constant._
import com.spok.util._
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

trait SearchRestServiceHandler extends JsonHelper with RandomUtil with LoggerUtil with ResponseUtil with ValidationUtil {

  implicit val timeout = Timeout(40 seconds)

  import akka.pattern.ask

  /**
   * Handler to return most relevant 10 nicknames.
   *
   * @param query
   * @param nickname
   * @return
   */
  def getByNickname(query: ActorRef, nickname: String): Future[String] = {
    val nicknameResponse = ask(query, GetNicknames(nickname))
    nicknameResponse.map { res =>
      res match {
        case GetNicknamesSuccess(nicknames) =>
          write(generateCommonResponseForListCaseClass(SUCCESS, Some(List()), Some(nicknames), Some(SEARCH_NICKNAME)))
        case GetNicknamesFailure(err, errorCode) => write(sendFormattedError(errorCode, err.getMessage, Some(SEARCH_NICKNAME)))
      }
    }
  }

  /**
   * Handler to return most relevant 10 hashtags.
   *
   * @param query
   * @param hashtag
   * @return
   */
  def getByHashtag(query: ActorRef, hashtag: String): Future[String] = {
    val hashtagResponse = ask(query, GetHashtags(hashtag.toLowerCase))
    hashtagResponse.map { res =>
      res match {
        case GetHashtagsSuccess(hashtags) => write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(HashTag(hashtags)), Some(SEARCH_HASHTAG)))
        case GetHashtagsFailure(err, errorCode) => write(sendFormattedError(errorCode, err.getMessage, Some(SEARCH_HASHTAG)))
      }
    }
  }

  def getPopularSpoker(query: ActorRef, pos: String): Future[String] = {
    logger.info("Got request to search most popular spok in Search Rest Service Handler.....")
    val popularSpokersResponse = ask(query, GetPopularSpokers(pos))
    popularSpokersResponse.map { response =>
      response match {
        case GetPopularSpokersSuccess(response) => write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(response), Some(SEARCH_POPULAR_SPOKER)))
        case GetPopularSpokersFailure(err, errorCode) => write(sendFormattedError(errorCode, err.getMessage, Some(SEARCH_POPULAR_SPOKER)))
      }
    }
  }

  /**
   * Handler to return last 10 spoks.
   *
   * @param query
   * @param userId
   * @param pos
   * @return
   */
  def getLastSpoks(query: ActorRef, userId: String, pos: String): Future[String] = {
    val lastSpoksResponse = ask(query, GetLastSpoks(userId, pos))
    lastSpoksResponse.map { res =>
      res match {
        case GetLastSpoksSuccess(spoksResponse) => write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(spoksResponse), Some(LOAD_LAST_SPOKS)))
        case GetLastSpoksFailure(err, errorCode) => write(sendFormattedError(errorCode, err.getMessage, Some(LOAD_LAST_SPOKS)))
      }
    }
  }

  /**
   * Handler to return last 10 friend's spoks.
   *
   * @param query
   * @param userId
   * @param pos
   * @return
   */
  def getFriendSpoks(query: ActorRef, userId: String, pos: String): Future[String] = {
    val startTime = new DateTime()
    info("Get friend spok in search handler :::: " + startTime)
    val friendSpoksResponse = ask(query, GetFriendSpoks(userId, pos))
    val result = friendSpoksResponse.map { res =>
      res match {
        case GetFriendSpoksSuccess(spoksResponse) =>
          write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(spoksResponse), Some(LOAD_FRIEND_SPOKS)))
        case GetFriendSpoksFailure(err, errorCode) => write(sendFormattedError(errorCode, err.getMessage, Some(LOAD_FRIEND_SPOKS)))
      }
    }
    info("Total time to get friend spok in search handler :::: " + (startTime.getMillis - new DateTime().getMillis))
    result
  }

  /**
   * Handler to return last 10 trendy spoks.
   *
   * @param query
   * @param pos
   * @return
   */
  def getTrendySpoks(query: ActorRef, pos: String, userId: String): Future[String] = {
    val spoksResponse = ask(query, GetTrendySpoks(pos, userId))
    spoksResponse.map {
      case GetTrendySpoksSuccess(spoksResponse) =>
        write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(spoksResponse), Some(LOAD_TRENDY_SPOKS)))
      case GetTrendySpoksFailure(err, errorCode) => write(sendFormattedError(errorCode, err.getMessage, Some(LOAD_TRENDY_SPOKS)))
    }
  }

  /**
   * Handler to return launch search.
   *
   * @param query
   * @param userIds
   * @return
   */
  def getlaunchSearch(query: ActorRef, pos: String, userIds: Option[String], hashtags: Option[String], latitude: String,
    longitude: String, start: String, end: String, contentTypes: Option[String], userId: String): Future[String] = {
    val launchSearchResponse = ask(query, GetLaunchSearch(pos, userIds.getOrElse("").mkString.split(',').toList,
      hashtags.getOrElse("").mkString.split(',').toList, latitude, longitude,
      start, end, contentTypes.getOrElse("").mkString.split(',').toList, userId))
    launchSearchResponse.map { res =>
      res match {
        case GetLaunchSearchSuccess(launchSearchResponse) =>
          write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(launchSearchResponse), Some(LAUNCH_SEARCH)))
        case GetLaunchSearchFailure(err, errorCode) => write(sendFormattedError(errorCode, err.getMessage, Some(LAUNCH_SEARCH)))
      }
    }
  }

}

object SearchRestServiceHandler extends SearchRestServiceHandler

