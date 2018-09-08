package com.spok.apiservice.handler

import akka.actor.Props
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.spok.apiservice.service.{ ApiConnector, DisconnectService }
import com.spok.model.SpokModel._
import com.spok.persistence.redis.RedisFactory
import com.spok.util.Constant._
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpec }

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

class ApiServiceHandlerSpec extends WordSpec with Matchers with MockitoSugar with BeforeAndAfterAll with ScalatestRouteTest with ApiServiceHandler {

  val mockedRedisFactory = mock[RedisFactory]
  override val redisFactory = mockedRedisFactory

  val actorRef = system.actorOf(Props(new ApiConnector()))

  "ApiServiceHandler" should {

    "be able to test handle notification when user is follows" in {

      val userId = getUUID()
      val followerId = getUUID()

      actorRef ! DisconnectService(None)
      when(mockedRedisFactory.fetchVisitiedUsers(followerId)) thenReturn (Future(Set("followerId1")))
      when(mockedRedisFactory.fetchVisitiedUsers(userId)) thenReturn (Future(Set("userId1")))
      val (visitorFollowerNotificationDetail) = handleNotificationOnUnfollowOrFollow(followerId, userId, "follows")
      val visitorFollowerNotificationDetailResult = Await.result(visitorFollowerNotificationDetail, 10 seconds)

      assert(visitorFollowerNotificationDetailResult.emitterId === followerId)
      assert(visitorFollowerNotificationDetailResult.notificationType === FOLLOWS)
      assert(visitorFollowerNotificationDetailResult.relatedTo === userId)

    }

    "be able to test handle notification when user is unfollows" in {

      val userId = getUUID()
      val followerId = getUUID()

      when(mockedRedisFactory.fetchVisitiedUsers(followerId)) thenReturn (Future(Set("followerId2")))
      when(mockedRedisFactory.fetchVisitiedUsers(userId)) thenReturn (Future(Set("userId2")))
      val (visitorFollowerNotificationDetail) = handleNotificationOnUnfollowOrFollow(followerId, userId, "unfollows")
      val visitorFollowerNotificationDetailResult = Await.result(visitorFollowerNotificationDetail, 10 seconds)

      assert(visitorFollowerNotificationDetailResult.emitterId === followerId)
      assert(visitorFollowerNotificationDetailResult.notificationType === UNFOLLOWS)
      assert(visitorFollowerNotificationDetailResult.relatedTo === userId)

    }

    "be able to test handle notification when comment is removed" in {

      val userId = getUUID()
      val commentId = getUUID()
      val spokId = getUUID()
      val absoluteSpokId = getUUID()
      val commentInternalSpokResponse = CommentInternalSpokResponse(spokId, "1", "1", "4", "256545")
      val removeCommentResponse = RemoveCommentResponse(commentId, commentInternalSpokResponse)
      val txt = write(removeCommentResponse)
      when(mockedRedisFactory.fetchVisitiedUsers(absoluteSpokId)) thenReturn (Future(Set("userId2")))
      when(mockedRedisFactory.fetchSubscribers(absoluteSpokId)) thenReturn (Future(Set.empty[String]))
      val notificationDetail = handleCommentAndSpokOperations(userId, absoluteSpokId, REMOVE_COMMENT)
      val visitorNotificationDetailResult = Await.result(notificationDetail, 10 seconds)

      assert(visitorNotificationDetailResult.emitterId === userId)
      assert(visitorNotificationDetailResult.notificationType === REMOVE_COMMENT)
      assert(visitorNotificationDetailResult.relatedTo === absoluteSpokId)

    }

    "be able to test handle notification when comment is added" in {

      val userId = getUUID()
      val commentId = getUUID()
      val spokId = getUUID()
      val absoluteSpokId = getUUID()
      val commentInternalSpokResponse = CommentInternalSpokResponse(spokId, "1", "1", "4", "256545")
      val commenterUserResponse = CommenterUserResponse(userId, "john", "male", "picture")
      val addCommentResponse = SpokCommentResponse(commentInternalSpokResponse, commenterUserResponse, Some(Nil), Some(commentId))
      val txt = write(addCommentResponse)
      when(mockedRedisFactory.fetchVisitiedUsers(absoluteSpokId)) thenReturn (Future(Set("userId2")))
      when(mockedRedisFactory.fetchSubscribers(absoluteSpokId)) thenReturn (Future(Set.empty[String]))
      val notificationDetail = handleCommentAndSpokOperations(userId, absoluteSpokId, "Comment Added")
      val visitorNotificationDetailResult = Await.result(notificationDetail, 10 seconds)

      assert(visitorNotificationDetailResult.emitterId === userId)
      assert(visitorNotificationDetailResult.notificationType === COMMENT_ADDED)
      assert(visitorNotificationDetailResult.relatedTo === absoluteSpokId)
    }

    "be able to test handle notification when comment is updated" in {

      val userId = getUUID()
      val spokId = getUUID()
      val absoluteSpokId = getUUID()
      val updateCommentResponse = CommentUpdateResponse(spokId, "1", "1", "4", "256545")
      val txt = write(updateCommentResponse)
      when(mockedRedisFactory.fetchVisitiedUsers(absoluteSpokId)) thenReturn (Future(Set("userId2")))
      when(mockedRedisFactory.fetchSubscribers(absoluteSpokId)) thenReturn (Future(Set.empty[String]))
      val notificationDetail = handleCommentAndSpokOperations(userId, absoluteSpokId, COMMENT_UPDATED)
      val visitorNotificationDetailResult = Await.result(notificationDetail, 10 seconds)

      assert(visitorNotificationDetailResult.emitterId === userId)
      assert(visitorNotificationDetailResult.notificationType === COMMENT_UPDATED)
      assert(visitorNotificationDetailResult.relatedTo === absoluteSpokId)
    }

    "be able to test handle notification when user profile is updated" in {

      val userId = getUUID()
      when(mockedRedisFactory.fetchVisitiedUsers(userId)) thenReturn (Future(Set("userId2")))
      val notificationDetail = handleUpdateUserProfileNotification(userId)
      val visitorNotificationDetailResult = Await.result(notificationDetail, 10 seconds)
      assert(visitorNotificationDetailResult.emitterId === userId)
      assert(visitorNotificationDetailResult.notificationType === USER_PROFILE_UPDATE_SUCCESS)
      assert(visitorNotificationDetailResult.relatedTo === userId)
      assert(visitorNotificationDetailResult.userIds === List("userId2"))
    }

    "be able to test handle remove spok from wall Notification" in {

      val spokId = getUUID()
      val userId = getUUID()
      when(mockedRedisFactory.fetchVisitiedUsers(spokId)) thenReturn (Future(Set("userId2")))
      val notificationDetail = handleRemoveSpokFromWallNotification(spokId, userId)
      val visitorNotificationDetailResult = Await.result(notificationDetail, 10 seconds)
      assert(visitorNotificationDetailResult.emitterId === userId)
      assert(visitorNotificationDetailResult.notificationType === SPOK_REMOVE_SUCCESS)
      assert(visitorNotificationDetailResult.relatedTo === spokId)
      assert(visitorNotificationDetailResult.userIds === List("userId2"))
    }
  }

}
