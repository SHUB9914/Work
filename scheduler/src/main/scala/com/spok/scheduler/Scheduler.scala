package com.spok.scheduler

import com.spok.actors.LocalActorRefFactory
import com.spok.configuration.Configuration._
import com.spok.messages.Spok.{ LastSpok, TrendySpok, PopularSpoker }
import com.spok.util.LoggerUtil
import com.typesafe.akka.extension.quartz.QuartzSchedulerExtension

trait Scheduler extends LoggerUtil {

  val localActorRefFactory: LocalActorRefFactory

  def start(): Unit = {
    info("<<<<<<<<<<<<<<<Scheduler started for Cron Jobs>>>>>>>>>>>>>")
    schedule cronJobs
  }

  def schedule: Scheduler = {
    this
  }

  def cronJobs: Unit = {
    val schedulerSystem = localActorRefFactory.system

    //Trendy Spok
    val trendySpokReceiver = localActorRefFactory getReceiver TRENDY_SPOK_ACTOR
    QuartzSchedulerExtension(schedulerSystem).schedule(CRON_TRENDY_EXPRESSION, trendySpokReceiver, TrendySpok)

    //Last Spok
    val lastSpokReceiver = localActorRefFactory getReceiver LAST_SPOK_ACTOR
    QuartzSchedulerExtension(schedulerSystem).schedule(CRON_LAST_EXPRESSION, lastSpokReceiver, LastSpok)

    // Popular Spoker
    val popularSpokerReceiver = localActorRefFactory getReceiver POPULAR_SPOKER_ACTOR
    QuartzSchedulerExtension(schedulerSystem).schedule(CRON_POPULAR_EXPRESSION, popularSpokerReceiver, PopularSpoker)
  }

}

object Scheduler extends Scheduler {
  val localActorRefFactory: LocalActorRefFactory = LocalActorRefFactory
}
