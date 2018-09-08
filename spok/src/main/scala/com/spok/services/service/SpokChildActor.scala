package com.spok.services.service

import akka.actor.Actor
import com.spok.model.SpokModel._
import com.spok.persistence.dse.DseGraphFactory
import com.spok.persistence.factory.spokgraph.DSESpokApi
import com.spok.services.service.SpokActorCommands._
import com.spok.services.service.SpokPerformAfterCommands._
import com.spok.util.LoggerUtil

/**
 * SpokChildActor to perform all operation
 * after create spok, respok, unspok and comment
 */

class SpokChildActor extends Actor with LoggerUtil {

  val spokLogger: SpokLogger = SpokLogger
  val dseSpokFactoryApi: DSESpokApi = DSESpokApi

  def receive: PartialFunction[Any, Unit] = {

    case PerformAfterSpok(userId, spok, statActor) => {
      spok.poll match {
        case Some(somePoll) => {
          val spokTypeVertex = DseGraphFactory.dseConn.executeGraph(dseSpokFactoryApi.fetchPollVertex(spok.spokId)).one().asVertex()
          dseSpokFactoryApi.insertPollWithQuestions(spokTypeVertex, somePoll, timeStamp)
          spokLogger.linkFollowers(userId, spok, somePoll.questions.size)
        }
        case None => spokLogger.linkFollowers(userId, spok, 0)
      }
      spokLogger.insertSpokCreationEvent(spok, userId)
      spokLogger.insertHashTags(spok.headerText.getOrElse(""))
      spokLogger.insertHashTags(spok.text.getOrElse(""))
      spokLogger.insertLaunchSearchDetailsOfSpok(userId, spok)
      statActor ! UpdateStatsAfterSpok(spok, userId)
      logger.info("All action performed after creating spok " + spok.spokId)
    }

    case PerformAfterRespok(respok, userId, spokId, followers, statActor) => {
      dseSpokFactoryApi.updateUserCurrentGeo(userId, respok.geo)
      if (!respok.groupId.get.toString.equals("0")) {
        dseSpokFactoryApi.sendSMSToContactFromGroup(respok.groupId.get.toString, userId, spokId)
      }
      spokLogger.insertRespokCreationEvent(respok, userId, spokId)
      spokLogger.logFollowersData(spokId, respok.geo, followers)
      spokLogger.insertHashTags(respok.text.getOrElse(""))
      statActor ! UpdateStatsAfterRespok(respok, spokId, userId)
      logger.info("All action performed after respok " + spokId)
    }

    case _ => logger.warn(s"Unknown message")
  }
}
