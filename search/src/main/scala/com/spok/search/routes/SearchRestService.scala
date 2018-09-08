package com.spok.search.routes

import akka.actor.ActorRef
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server._
import com.spok.search.handler.SearchRestServiceHandler
import com.spok.util.Constant._
import com.spok.util.HttpUtil
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Service to handle the search related queries.
 */
trait SearchRestService extends HttpUtil with SearchRestServiceHandler {

  val searchRestServiceHandler: SearchRestServiceHandler = SearchRestServiceHandler

  // ==============================
  //     REST ROUTES
  // ==============================

  /**
   * This method is use get most relevant 10 nicknames.
   *
   * @param query
   * @return
   */
  def searchByNickname(query: ActorRef): Route = get {
    path(SEARCH / AUTONICK) {
      parameters('nickname, 'userId, 'phone_number) { (nickname, userId, phoneNumber) =>
        val result: Future[HttpResponse] = searchRestServiceHandler.getByNickname(query, nickname).map(handleResponseWithEntity(_))
        logDuration(complete(result))
      }
    }
  }

  /**
   * This method is use get most relevant 10 hashtags.
   *
   * @param query
   * @return
   */
  def searchByHashtag(query: ActorRef): Route = get {
    path(SEARCH / AUTOHASH) {
      parameters('hashtag, 'userId, 'phone_number) { (hashtag, userId, phoneNumber) =>
        val result: Future[HttpResponse] = searchRestServiceHandler.getByHashtag(query, hashtag).map(handleResponseWithEntity(_))
        logDuration(complete(result))
      }
    }
  }

  def searchPopularSpoker(query: ActorRef): Route = get {
    path(SEARCH / POPULAR / Segment) {
      pos =>
        {
          logger.info("Got request to search most popular spok in Search Rest Service.....")
          parameters('userId, 'phone_number) { (userId, phoneNumber) =>
            val result: Future[HttpResponse] = searchRestServiceHandler.getPopularSpoker(query, pos).map(handleResponseWithEntity(_))
            logDuration(complete(result))
          }
        }
    }
  }

  /**
   * This method is use to get the last spoks.
   *
   * @param query
   * @return
   */
  def searchLastSpoks(query: ActorRef): Route = get {
    path(SEARCH / LAST / Segment) {
      pos =>
        {
          parameters('userId, 'phone_number) { (userId, phoneNumber) =>
            val result: Future[HttpResponse] = searchRestServiceHandler.getLastSpoks(query, userId, pos).map(handleResponseWithEntity(_))
            logDuration(complete(result))
          }
        }
    }
  }

  /**
   * This method is use to get friend's spoks.
   *
   * @param query
   * @return
   */
  def searchFriendSpoks(query: ActorRef): Route = get {
    path(SEARCH / FRIENDS / Segment) {
      pos =>
        {
          parameters('userId, 'phone_number) { (userId, phoneNumber) =>
            val result: Future[HttpResponse] = searchRestServiceHandler.getFriendSpoks(query, userId, pos).map(handleResponseWithEntity(_))
            logDuration(complete(result))
          }
        }
    }
  }

  /**
   * This method is use to get the trendy spoks.
   *
   * @param query
   * @return
   */
  def searchTrendySpoks(query: ActorRef): Route = get {
    path(SEARCH / TRENDY / Segment) {
      pos =>
        {
          parameters('userId, 'phone_number) { (userId, phoneNumber) =>
            val result: Future[HttpResponse] = searchRestServiceHandler.getTrendySpoks(query, pos, userId).map(handleResponseWithEntity(_))
            logDuration(complete(result))
          }
        }
    }
  }

  /**
   * This method is use to get launch search.
   *
   * @param query
   * @return
   */
  def launchSearch(query: ActorRef): Route = get {
    path(SEARCH / Segment) {
      pos =>
        {
          parameters('userId, 'phone_number, 'userids.*, 'hashtags.*, 'latitude, 'longitude, 'start, 'end, 'content_types.*) {
            (userId, phoneNumber, userIds, hashtags, latitude, longitude, start, end, contentTypes) =>
              val result: Future[HttpResponse] = searchRestServiceHandler.getlaunchSearch(query, pos,
                Some(userIds.mkString), Some(hashtags.mkString), latitude, longitude,
                start, end, Some(contentTypes.mkString), userId).map(handleResponseWithEntity(_))
              logDuration(complete(result))
          }
        }
    }
  }

  def routes(query: ActorRef): Route = searchByNickname(query) ~ searchByHashtag(query) ~ searchPopularSpoker(query) ~
    searchLastSpoks(query) ~ searchFriendSpoks(query) ~ searchTrendySpoks(query) ~ launchSearch(query)

}
