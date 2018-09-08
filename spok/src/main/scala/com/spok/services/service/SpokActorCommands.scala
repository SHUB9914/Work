package com.spok.services.service

import com.datastax.driver.dse.graph.{ Edge, Vertex }
import com.spok.model.SpokModel.{ UserPollAnswer, _ }

/**
 * File contains all the commands that are received by Spok Api.
 */
object SpokActorCommands {

  case class CreateSpok(spok: Spok, userId: String)

  case class RespokCreate(respok: Respok, spokId: String, userId: String, edgeOpt: Option[Edge])

  case class PerformUnspok(unspok: Unspok, spokId: String, userId: String, status: String)

  case class CommentAdd(addComment: Comment, spokId: String, userId: String)

  case class CommentUpdate(updateComment: Comment, userId: String)

  case class CommentRemove(commentId: String, userId: String, geo: Geo)

  case class SaveAnswer(questionId: String, userId: String, userPollAnswer: UserPollAnswer)

  case class DisableSpok(spokId: String, userId: String, launchedTime: Long, geo: Geo)

  case class RemoveWallSpok(spokId: String, userId: String, launchedTime: Long, geo: Geo)

  case class UpdateStatsAfterSpok(spokId: Spok, userId: String)

  case class UpdateStatsAfterRespok(respok: Respok, spokId: String, userId: String)

  case class UpdateStatsAfterUnspok(unspok: Unspok, spokId: String, userId: String)

  case class UpdateStatsAfterAddOrRemoveComment(spokId: String)

  case class SaveAllAnswersOfPoll(userId: String, allAnswers: AllAnswers)

}

