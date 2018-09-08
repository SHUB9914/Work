package com.spok.services.service

import com.datastax.driver.dse.graph.Edge
import com.spok.model.SpokModel.{ UserPollAnswer, _ }

/**
 * File contains all the commands that are received by Spok Manager.
 */
object SpokManagerCommands {

  case class Create(spok: Spok, userId: String)

  case class CreateRespok(respok: Respok, spokId: String, userId: String, edgeOpt: Option[Edge])

  case class ExecuteUnspok(unspok: Unspok, spokId: String, userId: String, status: String)

  case class CreateComment(addComment: Comment, spokId: String, userId: String)

  case class UpdateComment(updateComment: Comment, userId: String)

  case class RemoveComment(commentId: String, userId: String, geo: Geo)

  case class SavePollAnswer(questionId: String, userId: String, userPollAnswer: UserPollAnswer)

  case class Disable(spokId: String, userId: String, launchedTime: Long, geo: Geo)

  case class RemoveSpok(spokId: String, userId: String, launchedTime: Long, geo: Geo)

  case class SaveAllPollAnswers(userId: String, allAnswers: AllAnswers)

  case object FillRedisWithSubscriberDetails
}

