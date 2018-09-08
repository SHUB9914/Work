package com.spok.services.service

import com.datastax.driver.dse.graph.Vertex

/**
 * File contains all the view commands that are received by Spok Api.
 */
object SpokViewCommands {

  case class GetSpokInstanceStats(spokId: String, userId: String)

  case class GetSpokStats(spokId: String)

  case class GetSpokStack(userId: String, pos: String)

  case class GetScopedUsers(spokId: String, pos: String)

  case class GetReSpokers(spokId: String, pos: String)

  case class ViewShortSpok(spokId: String, targetUserId: String, userId: String, spokVertex: Option[Vertex])

  case class ViewFullSpokDetails(spokId: String, targetUserId: String, userId: String, spokVertex: Option[Vertex])

  case class ViewPollQuestionDetails(questionId: String, userId: String)

  case class GetComments(spokId: String, pos: String)

  case class ViewSpokersWall(targetUserId: String, pos: String, userId: String)

  case class ViewPollStats(spokId: String, userId: String)

  case class GetMySpoks(userId: String, pos: String)

}
