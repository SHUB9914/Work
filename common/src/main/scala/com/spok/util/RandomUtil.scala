package com.spok.util

import java.util.Date

import com.datastax.driver.core.utils.UUIDs

/**
 * Generating the unique id
 */

trait RandomUtil {

  def getUUID(): String = java.util.UUID.randomUUID().toString

  def timeStamp: Long = System.currentTimeMillis()

  def millisToDate(millis: Long): Date = new Date(millis)
  def cassandraTimeUUID = UUIDs.timeBased

}

object RandomUtil extends RandomUtil
