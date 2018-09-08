package com.spok.persistence.redis

import java.util.UUID

import akka.util.Timeout
import org.scalatest.WordSpec

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global._

class RedisFactoryTest extends WordSpec {

  "One" should {
    "be able to pass Ping Pong test" in {
      implicit val timeout = Timeout(20 seconds)
      val result = RedisFactory.client.ping()
      assert(Await.result(result, 50 seconds))
    }

    "be able to store and fetch visited users" in {
      val key = UUID.randomUUID().toString
      implicit val timeout = Timeout(20 seconds)
      RedisFactory.client.del(key)(timeout)
      RedisFactory.storeVisitiedUsers(key, "hello")
      RedisFactory.storeVisitiedUsers(key, "world")
      val result = Await.result(RedisFactory.fetchVisitiedUsers(key), 50 second).asInstanceOf[Set[String]]
      assert(result == Set("world", "hello"))
      RedisFactory.client.del(key)(timeout)
    }

    "be able to store subscriber" in {
      val key = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      implicit val timeout = Timeout(20 seconds)
      RedisFactory.client.del(key)(timeout)
      val result: Long = Await.result(RedisFactory.storeSubscriber(key, userId), 50 second)
      assert(result == 1)
      RedisFactory.client.del(key)(timeout)
    }

    "be able to remove subscriber" in {
      val key = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      implicit val timeout = Timeout(20 seconds)
      RedisFactory.storeSubscriber(key, userId)
      val result: Long = Await.result(RedisFactory.removeSubscriber(key, userId), 50 second)
      assert(result == 1)
      RedisFactory.client.del(key)(timeout)
    }

    "be able to validate subscriber" in {
      val key = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val userId1 = UUID.randomUUID().toString
      implicit val timeout = Timeout(20 seconds)
      RedisFactory.storeSubscriber(key, userId)
      val result = Await.result(RedisFactory.isSubscriberExist(key, userId), 50 second)
      assert(result == true)
      val result1 = Await.result(RedisFactory.isSubscriberExist(key, userId1), 50 second)
      assert(result1 == false)
      RedisFactory.client.del(key)(timeout)
    }

    "be able to store connected users" in {
      val key = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      implicit val timeout = Timeout(20 seconds)
      RedisFactory.client.del(key)(timeout)
      val result: Boolean = Await.result(RedisFactory.storeConnectedUsers(key, userId), 50 second)
      assert(result)
      RedisFactory.client.del(key)(timeout)
    }

    "be able to fetch connected users" in {
      val key = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      implicit val timeout = Timeout(20 seconds)
      RedisFactory.client.del(key)(timeout)
      val result: Boolean = Await.result(RedisFactory.storeConnectedUsers(key, userId), 50 second)
      assert(result)
      val value: Option[String] = Await.result(RedisFactory.fetchConnectedUsers(key), 50 second)
      assert(value == Some(userId))
      RedisFactory.client.del(key)(timeout)
    }

    "be able to store talkId" in {
      val senderId = UUID.randomUUID().toString
      val receiverId = UUID.randomUUID().toString
      val key = senderId + receiverId
      implicit val timeout = Timeout(20 seconds)
      val result = Await.result(RedisFactory.storeTalk(senderId, receiverId), 50 second)
      assert(result == 1)
      RedisFactory.removeTalkId(senderId, receiverId)
    }

    "be able to validate talkId" in {
      val senderId = UUID.randomUUID().toString
      val receiverId = UUID.randomUUID().toString
      val key = senderId + receiverId
      implicit val timeout = Timeout(20 seconds)
      val result = Await.result(RedisFactory.storeTalk(senderId, receiverId), 50 second)
      assert(result == 1)
      val result1 = Await.result(RedisFactory.isTalkExist(senderId, receiverId), 50 second)
      assert(result1 == true)
      val result2 = Await.result(RedisFactory.removeTalkId(senderId, receiverId), 50 second)
      assert(result2 == 1)
      val result3 = Await.result(RedisFactory.isTalkExist(senderId, receiverId), 50 second)
      assert(result3 == false)
    }

    "be able to remove talkId" in {
      val senderId = UUID.randomUUID().toString
      val receiverId = UUID.randomUUID().toString
      val key = senderId + receiverId
      implicit val timeout = Timeout(20 seconds)
      RedisFactory.storeTalk(senderId, receiverId)
      val result = Await.result(RedisFactory.removeTalkId(senderId, receiverId), 50 second)
      val result1 = Await.result(RedisFactory.removeTalkId(senderId, receiverId), 50 second)
      assert(result == 1)
      assert(result1 == 0)
    }

    "be able to fetch connected users talkers" in {
      val key = "talk_d3f5abd8-0ee3-4126-875e-bdf3131e0087"
      val userId = UUID.randomUUID().toString
      implicit val timeout = Timeout(20 seconds)
      RedisFactory.client.del(key)(timeout)
      val result: Boolean = Await.result(RedisFactory.storeConnectedUsers(key, userId), 50 second)
      assert(result)
      val value: Option[String] = Await.result(RedisFactory.fetchConnectedUsers(key), 50 second)
      assert(value == Some(userId))
      RedisFactory.client.del(key)(timeout)
    }
  }

}
