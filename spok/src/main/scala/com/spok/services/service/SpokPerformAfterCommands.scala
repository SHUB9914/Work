package com.spok.services.service

import akka.actor.ActorRef
import com.spok.model.SpokModel.{ CommentUpdateResponse, _ }

object SpokPerformAfterCommands {

  case class PerformAfterUnspok(spokId: String, userId: String, unspok: Unspok, status: String, statActor: ActorRef)

  case class PerformAfterRespok(respok: Respok, userId: String, spokId: String,
    followerDetails: List[(String, Double, Double, Double, Long)], statActor: ActorRef)

  case class PerformAfterComment(spokId: String, commenterUserId: String, addedTimestamp: Long, addComment: Comment, statActor: ActorRef)

  case class PerformAfterRemoveComment(spokId: String, commenterUserId: String, commentId: String, updatedTimeStamp: Long, geo: Geo, statActor: ActorRef)

  case class PerformAfterAnswerPollQuestion(userId: String, spokId: String)

  case class PerformAfterSpokDisableEvents(userId: String, spokId: String, launchedTime: Long, geo: Geo)

  case class PerformAfterRemoveWallSpok(userId: String, spokId: String, launchedTime: Long, geo: Geo)

  case class PerformAfterSpok(userId: String, spok: Spok, statActor: ActorRef)

  case class PerformAfterUpdateComment(commenterUserId: String, updateComment: Comment, commentUpdatedRes: CommentUpdateResponse, updatedTimeStamp: Long)

  case class PerformAfterAnsweringAllPollQuestion(userId: String, spokId: String)
}
