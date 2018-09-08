package com.spok.services

import com.spok.configuration.Configuration._
import com.spok.persistence.factory.search.SearchBatch
import com.spok.util.{ LoggerUtil, RandomUtil }

trait SearchService extends LoggerUtil {

  val dseSearchBatch: SearchBatch
  val randomUtil: RandomUtil

  def triggerApiToGetTrendySpok: Unit = {
    val endTime = randomUtil.timeStamp
    val timeDiffernece: Long = TRENDY_SPOK_TIME_RANGE_IN_HOURS.toLong * 3600 * 1000
    val startTime: Long = endTime - timeDiffernece
    dseSearchBatch.getTrendySpokInBatch(startTime, endTime)
    info("<<<<<< Executed trendy spok batch process >>>>>>")
  }

  def triggerApiToGetLastSpok: Unit = {
    val endTime = randomUtil.timeStamp
    val timeDiffernece: Long = LAST_SPOK_TIME_RANGE_IN_HOURS.toLong * 3600 * 1000
    val startTime: Long = endTime - timeDiffernece
    dseSearchBatch.getLastSpokInBatch(startTime, endTime)
    info("<<<<<< Executed last spok batch process >>>>>>")
  }
  def triggerApiToGetPopularSpoker: Unit = {
    val endTime = randomUtil.timeStamp
    val timeDiffernece: Long = POPULAR_SPOKER_TIME_RANGE_IN_HOURS.toLong * 3600 * 1000
    val startTime: Long = endTime - timeDiffernece
    dseSearchBatch.getPopularSpokersInBatch(startTime, endTime)
    info("<<<<<< Triggred popular spoker batch process >>>>>>")
  }

}

object SearchService extends SearchService {
  val dseSearchBatch: SearchBatch = SearchBatch
  val randomUtil: RandomUtil = RandomUtil
}
