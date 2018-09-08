package com.spok.persistence.redis

import akka.actor.ActorSystem
import akka.util.Timeout
import com.redis.{ RedisClient, RedisClientSettings }
import com.redis.RedisClientSettings.ConstantReconnectionSettings
import com.redis.serialization.KeyValuePair
import com.spok.util.LoggerUtil
import com.typesafe.config.ConfigFactory

import scala.concurrent.Future
import scala.concurrent.duration._
import com.spok.util.Constant._

/**
 * Set up Redis client
 */

trait RedisFactory extends LoggerUtil {

  // Akka setup
  implicit val system = ActorSystem("redis-client")
  implicit val executionContext = system.dispatcher
  implicit val timeout = Timeout(5 seconds)

  val config = ConfigFactory.load("references.conf")
  val host = config.getString("redis.host")
  val port = config.getInt("redis.port")
  val CONSTANT_DELAY_MS = 100
  val MAX_ATTEMPT = 20

  logger.info(s"Redis connected on $host:$port")

  // Redis client setup
  val client = RedisClient(host, port, settings = RedisClientSettings(reconnectionSettings = ConstantReconnectionSettings(CONSTANT_DELAY_MS, MAX_ATTEMPT)))

  /**
   * This method will store users, interacting
   * with a spok/user
   *
   * @param key
   * @param value
   * @return
   */
  def storeVisitiedUsers(key: String, value: String): Future[Long] = {
    logger info (s"Adding $key in Redis")
    client.sadd(key, value)
  }

  def storeConnectedUsers(key: String, value: String): Future[Boolean] = client.set(key, value)

  def fetchConnectedUsers(key: String): Future[Option[String]] = client.get(key)

  /**
   * This method will fetch users, interacting
   * with a spok/user
   *
   * @param key
   * @return
   */
  def fetchVisitiedUsers(key: String): Future[Set[String]] = {
    client.smembers[String](key)
  }

  /**
   * This method will fetch users, interacting
   * with a spok/user
   *
   * @param key
   * @return
   */
  def remove(key: String): Future[Long] = {
    client.del(key)(timeout)
  }

  /**
   * This method will store spok's feed subscriber
   *
   * @param key
   * @param value
   * @return
   */
  def storeSubscriber(key: String, value: String): Future[Long] = {
    logger info (s"Adding subscriber $key in Redis")
    client.sadd(SUBSCRIBER + key, value)
  }

  /**
   * This method will remove spok's feed subscriber
   *
   * @param key
   * @param value
   * @return
   */
  def removeSubscriber(key: String, value: String): Future[Long] = {
    logger info (s"Remove subscriber $key from Redis")
    client.srem(SUBSCRIBER + key, value)
  }

  /**
   * this method validates spok's feed subscriber
   *
   * @param key
   * @param value
   * @return
   */
  def isSubscriberExist(key: String, value: String): Future[Boolean] = {
    fetchSubscribers(key).map {
      listOfSubscriber => listOfSubscriber.exists(x => x.equals(value))
    }
  }

  /**
   * This methods returns subscribers of a spok
   *
   * @param key
   * @return
   */
  def fetchSubscribers(key: String): Future[Set[String]] = {
    client.smembers[String](SUBSCRIBER + key)
  }

  /**
   * This method will store talk and it's sender and receiver.
   *
   * @param senderId
   * @param receiverId
   * @return
   */
  def storeTalk(senderId: String, receiverId: String): Future[Long] = {
    val key = senderId + receiverId
    logger info (s"Adding talk $key in Redis")
    client.sadd(TALK_ + key, "true")
  }

  /**
   * This method will validate a given talk.
   *
   * @param senderId
   * @param receiverId
   * @return
   */
  def isTalkExist(senderId: String, receiverId: String): Future[Boolean] = {
    val key1 = senderId + receiverId
    val key2 = receiverId + senderId
    try {
      client.exists(TALK_ + key1).flatMap { flag =>
        flag match {
          case true => Future(true)
          case false => client.exists(TALK_ + key2)
        }
      }
    } catch {
      case ex: Exception =>
        logger.info("error while fetching data from redis" + ex.getMessage)
        Future(false)
    }
  }

  /**
   * This method will remove talk from redis.
   *
   * @param senderId
   * @param receiverId
   * @return
   */
  def removeTalkId(senderId: String, receiverId: String): Future[Long] = {
    val key1 = senderId + receiverId
    val key2 = receiverId + senderId
    client.del(TALK_ + key1)(timeout).flatMap { flag =>
      flag match {
        case 1 => Future(1)
        case 0 => client.del(TALK_ + key2)
      }
    }
  }

}

object RedisFactory extends RedisFactory
