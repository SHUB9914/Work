package com.spok.apiservice.handler

import com.spok.model.NotificationDetail
import com.spok.persistence.redis.RedisFactory
import com.spok.util.ConfigUtil._
import com.spok.util.Constant._
import com.spok.util.{ JsonHelper, LoggerUtil, RandomUtil }
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.sys.process.Process

trait ApiServiceHandler extends RandomUtil with JsonHelper with LoggerUtil {
  val redisFactory: RedisFactory = RedisFactory

  val spokServiceName = ConfigFactory.load().getString("app.spok_service_name")
  val notificationServiceName = ConfigFactory.load().getString("app.notification_service_name")
  val accountServiceName = ConfigFactory.load().getString("app.account_service_name")
  val searchServiceName = ConfigFactory.load().getString("app.search_service_name")
  val messagingServiceName = ConfigFactory.load().getString("app.messaging_service_name")
  val isProd = ConfigFactory.load().getBoolean("app.prod_mode")
  /**
   * This fucntion will do DNS service lookup
   *
   * @param serviceName
   * @return IP address
   */
  def lookupService(serviceName: String): String = {
    if (isProd) {
      val command = "dig @" + interface + " -p 8600 " + serviceName + ".service.consul +short"
      logger.info("inside lookUpService: " + command)
      val ipAddresses: String = Process(command).!!
      LoggerUtil.info(s"In production IP address command result of $serviceName : $ipAddresses")
      val ipAddress = ipAddresses.split("\n")(0)
      LoggerUtil.info(s"In production IP address of $serviceName : $ipAddress")
      ipAddress.trim
    } else {
      LoggerUtil.info(s"In dev IP address of $serviceName : $interface")
      interface
    }
  }

  def handleNotificationOnUnfollowOrFollow(followerId: String, userId: String, notificationType: String): Future[NotificationDetail] = {

    val vistorIdsOfFollower: Future[Set[String]] = redisFactory.fetchVisitiedUsers(followerId).map(list => list ++ Set(followerId))

    val visitorIdsOfUser: Future[Set[String]] = redisFactory.fetchVisitiedUsers(userId).map(list => list ++ Set(userId))

    val finalUsers = mergeFutureSets(vistorIdsOfFollower, visitorIdsOfUser)

    val visitorNotificationDetail = finalUsers.map { visitorId =>
      NotificationDetail(visitorId.toList, getUUID(), notificationType, userId, followerId)
    }
    visitorNotificationDetail
  }

  def mergeFutureSets[X](fl1: Future[Set[X]], fl2: Future[Set[X]]): Future[Set[X]] = {
    for {
      f1Res <- fl1
      f2Res <- fl2
    } yield (f1Res ++ f2Res)
  }

  def handleUpdateUserProfileNotification(userId: String): Future[NotificationDetail] = {
    val visitorNotificationResponse = redisFactory.fetchVisitiedUsers(userId).map { visitorId =>
      NotificationDetail(visitorId.toList, getUUID(), USER_PROFILE_UPDATE_SUCCESS, userId, userId)
    }
    logger.info("visitorNotificationResponse of all visitors ::: ", visitorNotificationResponse)
    visitorNotificationResponse
  }

  def handleRemoveSpokFromWallNotification(spokId: String, userId: String): Future[NotificationDetail] = {

    val visitorNotificationResponse = redisFactory.fetchVisitiedUsers(spokId).map { visitorId =>
      NotificationDetail(visitorId.toList, getUUID(), SPOK_REMOVE_SUCCESS, spokId, userId)
    }
    visitorNotificationResponse
  }

  def handleCommentAndSpokOperations(userId: String, spokId: String, notificationType: String): Future[NotificationDetail] = {

    val finalUsers: Future[Set[String]] = mergeFutureSets(redisFactory.fetchSubscribers(spokId), redisFactory.fetchVisitiedUsers(spokId))

    finalUsers.map { userIds =>
      NotificationDetail(userIds.toList, getUUID(), notificationType, spokId, userId)
    }
  }

}
