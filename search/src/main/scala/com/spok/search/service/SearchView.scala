package com.spok.search.service

import akka.actor.Actor
import com.spok.model.SpokModel.{ Nickname, PopularSpokerRes, SpoksResponse }
import com.spok.persistence.factory.search.SearchApi
import com.spok.util.Constant._
import com.spok.util.{ JsonHelper, LoggerUtil }

// Commands
case class GetNicknames(nickname: String)
case class GetHashtags(hashtag: String)
case class GetLastSpoks(userId: String, pos: String)
case class GetPopularSpokers(pos: String)
case class GetFriendSpoks(userId: String, pos: String)
case class GetTrendySpoks(pos: String, userId: String)
case class GetLaunchSearch(pos: String, userIds: List[String], hashtags: List[String], latitude: String, longitude: String,
  start: String, end: String, contentTypes: List[String], userId: String)

// Replies
case class GetNicknamesSuccess(nicknames: List[Nickname])
case class GetNicknamesFailure(cause: Throwable, errorCode: String)
case class GetHashtagsSuccess(hashtags: List[String])
case class GetHashtagsFailure(cause: Throwable, errorCode: String)
case class GetLastSpoksSuccess(spoksResponse: SpoksResponse)
case class GetLastSpoksFailure(cause: Throwable, errorCode: String)
case class GetPopularSpokersSuccess(popularSpokerRes: PopularSpokerRes)
case class GetPopularSpokersFailure(cause: Throwable, errorCode: String)
case class GetFriendSpoksSuccess(spoksResponse: SpoksResponse)
case class GetFriendSpoksFailure(cause: Throwable, errorCode: String)
case class GetTrendySpoksSuccess(spoksResponse: SpoksResponse)
case class GetTrendySpoksFailure(cause: Throwable, errorCode: String)
case class GetLaunchSearchSuccess(spoksResponse: SpoksResponse)
case class GetLaunchSearchFailure(cause: Throwable, errorCode: String)

class SearchView extends Actor with JsonHelper with LoggerUtil {

  val dseSearchApi: SearchApi = SearchApi

  /**
   * Command handler.
   */
  def receive: Receive = {

    case GetNicknames(nickname) => {
      val (nicknames, flag) = dseSearchApi.getByNickname(nickname)
      flag match {
        case true => sender ! GetNicknamesSuccess(nicknames)
        case false => sender ! GetNicknamesFailure(new Exception(UNABLE_SEARCHING_NICKNAME), SRH_106)
      }
    }

    case GetHashtags(hashtag) => {
      val (hashtags, flag) = dseSearchApi.getByHashtag(hashtag)
      flag match {
        case true => sender ! GetHashtagsSuccess(hashtags)
        case false => sender ! GetHashtagsFailure(new Exception(UNABLE_SEARCHING_HASHTAG), SRH_107)
      }
    }

    case GetPopularSpokers(pos) =>
      val spokerRes: Option[PopularSpokerRes] = dseSearchApi.getPopularSpokers(pos)
      spokerRes match {
        case Some(spokerRes) => sender() ! GetPopularSpokersSuccess(spokerRes)
        case None => sender() ! GetPopularSpokersFailure(new Exception(UNABLE_LOADING_SPOKER), SRH_108)

      }

    case GetLastSpoks(userId, pos) => {
      val spoksRes = dseSearchApi.getLastSpoks(userId, pos)
      spoksRes match {
        case Some(spoks) => sender ! GetLastSpoksSuccess(spoks)
        case None => sender ! GetLastSpoksFailure(new Exception(UNABLE_LOADING_LAST_SPOKS), SRH_104)
      }
    }

    case GetFriendSpoks(userId, pos) => {
      val spoksRes = dseSearchApi.getFriendSpoks(userId, pos)
      spoksRes match {
        case Some(spoks) => sender ! GetFriendSpoksSuccess(spoks)
        case None => sender ! GetFriendSpoksFailure(new Exception(UNABLE_LOADING_FRIEND_SPOKS), SRH_103)
      }
    }

    case GetTrendySpoks(pos, userId) => {
      val spoksRes = dseSearchApi.getTrendySpok(pos, userId)
      spoksRes match {
        case Some(spoks) => sender ! GetTrendySpoksSuccess(spoks)
        case None => sender ! GetTrendySpoksFailure(new Exception(UNABLE_LOADING_TRENDY_SPOKS), SRH_109)
      }
    }

    case GetLaunchSearch(pos, userIds, hashtags, latitude, longitude, start, end, contentTypes, userId) => {
      val spoksRes = dseSearchApi.getlaunchSearch(pos, userIds, hashtags, latitude, longitude, start, end, contentTypes, userId)
      spoksRes match {
        case Some(spoks) => sender ! GetLaunchSearchSuccess(spoks)
        case None => sender ! GetLaunchSearchFailure(new Exception(UNABLE_SEARCHING_SPOKS), SRH_105)
      }
    }

  }

}
