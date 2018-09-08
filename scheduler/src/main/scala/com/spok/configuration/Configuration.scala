package com.spok.configuration

import com.typesafe.config.ConfigFactory

object Configuration {

  def load: (String) => String = {
    ConfigFactory.load().getString
  }

  lazy val TRENDY_SPOK_TIME_RANGE_IN_HOURS = load("spok.scheduler.range.hours.trendySpok")
  lazy val LAST_SPOK_TIME_RANGE_IN_HOURS = load("spok.scheduler.range.hours.lastSpok")
  lazy val POPULAR_SPOKER_TIME_RANGE_IN_HOURS = load("spok.scheduler.range.hours.popularSpoker")

  lazy val TRENDY_SPOK_ACTOR = load("spok.scheduler.actors.name.trendySpok")
  lazy val LAST_SPOK_ACTOR = load("spok.scheduler.actors.name.lastSpok")
  lazy val POPULAR_SPOKER_ACTOR = load("spok.scheduler.actors.name.popularSpoker")

  lazy val CRON_TRENDY_EXPRESSION = load("spok.scheduler.cron.trendy")
  lazy val CRON_LAST_EXPRESSION = load("spok.scheduler.cron.last")
  lazy val CRON_POPULAR_EXPRESSION = load("spok.scheduler.cron.popular")

  lazy val CONCURRENCY_LEVEL = load("spok.scheduler.concurrency")

}
