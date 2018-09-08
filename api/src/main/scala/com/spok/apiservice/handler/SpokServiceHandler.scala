package com.spok.apiservice.handler

import akka.http.scaladsl.model.ws.Message
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{ Source, SourceQueue, SourceQueueWithComplete }
import com.spok.model.NotificationDetail
import com.spok.model.SpokModel._
import com.spok.util.Constant._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Future, Promise }

trait SpokServiceHandler extends NotificationServiceHandler {

  /**
   * Spok source queue to send message in queue
   *
   * @return
   */
  def sourceQueue: (Source[Message, SourceQueueWithComplete[Message]], Future[SourceQueue[Message]]) = {
    val p = Promise[SourceQueue[Message]]
    val s = Source.queue[Message](Int.MaxValue, OverflowStrategy.backpressure).mapMaterializedValue(m => {
      p.trySuccess(m)
      m
    })
    (s, p.future)
  }
  /**
   * Check Response from microservice
   * and take appropriate action
   *
   * @param json
   * @param userIdOpt
   * @param phoneNumberOpt
   * @return
   */

  def checkResponseAndTakeAction(json: String, userIdOpt: Option[String], phoneNumberOpt: Option[String]): Boolean = {
    logger.info(s"${phoneNumberOpt}: Got Response and Now taking further action !!!!!" + json)
    val responseOpt = (parse(json) \ "data").extractOpt[Response]
    for (
      response <- responseOpt;
      userId <- userIdOpt;
      phoneNumber <- phoneNumberOpt
    ) {
      response match {
        case Response(Some(spokResponse), _, _, _, _, _, _) =>
          sendUserMentionNotification(spokResponse.mentionUserId, spokResponse.spokId, (userId, phoneNumber))
        case Response(_, Some(respokResponse), _, _, _, _, _) => respokResponseHandler(userId, phoneNumber, respokResponse)
        case Response(_, _, Some(unspokResponse), _, _, _, _) => unspokResponseHandler((userId, phoneNumber), unspokResponse)
        case Response(_, _, _, Some(addCommentResponse), _, _, _) => commentResponseHandler(
          (userId, phoneNumber),
          addCommentResponse.spok.spokId, addCommentResponse.mentionUserId, addCommentResponse.commentId, COMMENT_ADDED
        )
        case Response(_, _, _, _, Some(removeCommentResponse), _, _) => removeCommentResponseHandler(
          (userId, phoneNumber),
          Some(removeCommentResponse.spok.spokId)
        )
        case Response(_, _, _, _, _, Some(updateCommentResponse), _) => commentResponseHandler(
          (userId, phoneNumber),
          updateCommentResponse.spokId, updateCommentResponse.mentionUserId, updateCommentResponse.commentId, COMMENT_UPDATED
        )
        case Response(_, _, _, _, _, _, Some(removeSpokResponse)) => removeSpokFromWallHandler((userId, phoneNumber), removeSpokResponse.spokId)
        case _ => // Do nothing
      }
    }
    true
  }

  private def respokResponseHandler(userId: String, phoneNumber: String, respokResponse: RespokResponse) = {
    val spokId = respokResponse.spokId
    val mentionUserId = respokResponse.mentionUserId
    val futureUserIds: Future[Set[String]] = redisFactory.fetchSubscribers(spokId)

    val futureVisiterUserIds: Future[Set[String]] = redisFactory.fetchVisitiedUsers(spokId)

    val finalUsers = mergeFutureSets(futureVisiterUserIds, futureUserIds)

    finalUsers.map { userIds =>
      val notificationDetail = NotificationDetail(userIds.toList, getUUID, RESPOKED, spokId, userId)
      sendNotification(notificationDetail, "send")
    }
    if (mentionUserId != Nil) {
      sendUserMentionNotification(mentionUserId, spokId, (userId, phoneNumber))
    }

  }

  private def unspokResponseHandler(userInfo: (String, String), unspokResponse: UnspokResponse) = {
    val (userId, _) = userInfo
    val spokId = unspokResponse.spokId
    val unspokNotification: Future[NotificationDetail] = handleCommentAndSpokOperations(userId, spokId, UNSPOKED)
    unspokNotification map (notification => sendNotification(notification, "send"))
  }

  private def commentResponseHandler(userInfo: (String, String), spokId: String,
    mentionUserIdOpt: Option[List[String]], commentId: Option[String], notificationType: String) = {
    if (spokId.nonEmpty) {
      val (userId, _) = userInfo
      val addCommentNotification = handleCommentAndSpokOperations(userId, spokId,
        notificationType)
      addCommentNotification map (notification => sendNotification(notification, "send"))
      if (mentionUserIdOpt.isDefined) {
        val mentionUserId = mentionUserIdOpt.get
        if (mentionUserId != Nil && commentId.isDefined) {
          sendUserMentionNotification(mentionUserId, commentId.get, userInfo)
        }
      }
    }
  }

  private def removeSpokFromWallHandler(userInfo: (String, String), spokInstanceId: Option[String]) = {
    val (userId, _) = userInfo
    if (spokInstanceId.isDefined) {
      val updateUserProfileNotification = handleRemoveSpokFromWallNotification(spokInstanceId.get, userId)
      updateUserProfileNotification map (notification => sendNotification(notification, "send"))
    }
  }
  /**
   *
   * @param userInfo
   * @return
   */
  private def removeCommentResponseHandler(userInfo: (String, String), spokId: Option[String]) = {
    if (spokId.isDefined) {
      val (userId, _) = userInfo
      val removeCommentNotification = handleCommentAndSpokOperations(userId, spokId.get, REMOVE_COMMENT)
      removeCommentNotification map (notification => sendNotification(notification, "send"))
    }
  }
}
