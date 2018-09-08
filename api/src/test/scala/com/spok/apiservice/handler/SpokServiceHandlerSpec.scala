package com.spok.apiservice.handler

import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.spok.persistence.redis.RedisFactory
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpec }

import scala.concurrent.Future

class SpokServiceHandlerSpec extends WordSpec with Matchers with MockitoSugar with BeforeAndAfterAll with ScalatestRouteTest with SpokServiceHandler {

  val mockedRedisFactory = mock[RedisFactory]
  override val redisFactory = mockedRedisFactory

  "SpokServiceHandler" should {

    "be able to check response and take action" in {

      val userId = Some(getUUID())
      val spokId = getUUID()
      val phoneNumber = Some("1234")

      val s: Future[Set[String]] = Future(Set())
      when(mockedRedisFactory.fetchVisitiedUsers(spokId)) thenReturn (s)
      when(mockedRedisFactory.fetchSubscribers(spokId)) thenReturn (s)
      val spokResponse = """{"data":{"spokResponse":{"spokId":"""" + spokId + """","mentionUserId":["""" + userId + """"]}}}"""
      val respok = s"""{"data":{"respokResponse":{"spokId":"$spokId","counters":{"numberOfRespoked":1,"numberOfLanded":2,"numberOfComment":0,"travelled":4060852.576},"mentionUserId":["$userId"]}}}"""
      val unspok = s"""{"data":{"unspokResponse":{"spokId":"$spokId","counters":{"numberOfRespoked":0,"numberOfLanded":1,"numberOfComment":0,"travelled":0.0},"mentionUserId":[]}}}"""
      val addComment = """{"data":{"addCommentResponse":{"spok":{"spokId":"""" + spokId + """","nbRespoked":"1","nbLanded":"2","nbComments":"2","travelled":"1.0155378443E7","absoluteSpokId":"""" + spokId + """"},"user":{"id":"d663aa99-8905-4aa2-99c7-d85076cdbcca","nickName":"narayan","gender":"male","picture":""},"mentionUserId":["""" + userId + """"],"commentId":"387a0e2c-20ec-401c-b65c-71b8fb0cbf91"}}}"""
      val removeComment = """{"data":{"removeCommentResponse":{"spok":{"spokId":"""" + spokId + """","nbRespoked":"1","nbLanded":"2","nbComments":"2","travelled":"1.0155378443E7","absoluteSpokId":"""" + spokId + """"},"user":{"id":"d663aa99-8905-4aa2-99c7-d85076cdbcca","nickName":"narayan","gender":"male","picture":""},"mentionUserId":[],"commentId":"387a0e2c-20ec-401c-b65c-71b8fb0cbf91"}}}"""
      val removeSpokFromWall = s"""{"data":{"removeSpokResponse":{"spokId":"$spokId"}}}"""
      val updateComment = """{"data":{"updateCommentResponse":{"spokId":"""" + spokId + """","nbRespoked":"1","nbLanded":"2","nbComments":"2","travelled":"1.0155378443E7","absoluteSpokId":"""" + spokId + """"},"mentionUserId":["""" + userId + """"],"commentId":"387a0e2c-20ec-401c-b65c-71b8fb0cbf91"}}"""
      val nodata = """{"data":"nodata"}"""
      assert(checkResponseAndTakeAction(spokResponse, userId, phoneNumber))
      assert(checkResponseAndTakeAction("""{"data":{}}""", userId, phoneNumber))
      assert(checkResponseAndTakeAction(nodata, userId, phoneNumber))
      assert(checkResponseAndTakeAction("""{}""", userId, phoneNumber))
      assert(checkResponseAndTakeAction(respok, userId, phoneNumber))
      assert(checkResponseAndTakeAction(unspok, userId, phoneNumber))
      assert(checkResponseAndTakeAction(addComment, userId, phoneNumber))
      assert(checkResponseAndTakeAction(updateComment, userId, phoneNumber))
      assert(checkResponseAndTakeAction(removeComment, userId, phoneNumber))
      assert(checkResponseAndTakeAction(removeSpokFromWall, userId, phoneNumber))
    }
  }
}
