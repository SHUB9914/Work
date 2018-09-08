package com.spok.util

import com.typesafe.config.ConfigFactory

/**
 * Provides the basic configuration to all the services
 */
object ConfigUtil {

  val config = ConfigFactory.load()

  val interface = config.getString("app.interface")
  val hostPoint = config.getString("app.host")
  val port = config.getInt("app.port")
  val spokPort = config.getInt("app.spokPort")
  val notificationPort = config.getInt("app.notificationPort")
  val accountPort = config.getInt("app.accountPort")
  val searchPort = config.getInt("app.searchPort")
  val messagingPort = config.getInt("app.messagingPort")
  val protocol = config.getString("app.protocol")
  val awsAccessKey = config.getString("app.aws.accessKey")
  val awsSecretKey = config.getString("app.aws.secretKey")
  val awsBucket = config.getString("app.aws.bucket")
  val concurrencyLevel = config.getInt("app.concurrency")

}
