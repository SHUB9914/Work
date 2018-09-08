package com.spok.apiservice.handler

import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.spok.model.Account.AccountResponse
import com.spok.persistence.redis.RedisFactory
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpec }

import scala.concurrent.Future

class AccountServiceHandlerSpec extends WordSpec with Matchers with MockitoSugar with BeforeAndAfterAll with ScalatestRouteTest with AccountServiceHandler {

  val mockedRedisFactory = mock[RedisFactory]
  override val redisFactory = mockedRedisFactory

  "AccountServiceHandler" should {

    "be able to check response and take action" in {

      getInfoFromToken("")
      val userId = "34d492ad-847e-4aab-8327-52f2302f70e7"
      val otherUserId = "1910f87b-f409-4bf1-a3cf-b2abc87b40dc"
      val phoneNumber = "1234"

      val s: Future[Set[String]] = Future(Set())
      when(mockedRedisFactory.fetchVisitiedUsers(otherUserId)) thenReturn (s)
      when(mockedRedisFactory.fetchVisitiedUsers(userId)) thenReturn (s)
      val data1 = """{\"followResponse\":{\"userMobileNumber\":\"919999382228\",\"followerId\":\"34d492ad-847e-4aab-8327-52f2302f70e7\",\"followingId\":\"1910f87b-f409-4bf1-a3cf-b2abc87b40dc\"}}"""
      val data2 = """{\"followResponse\":{\"userMobileNumber\":\"919999382228\",\"followerId\":\"34d492ad-847e-4aab-8327-52f2302f70e7\",\"followingId\":\"1910f87b-f409-4bf1-a3cf-b2abc87b40dc\"},\"isFriend\":true}"""
      val data3 = """{\"unFollowResponse\":{\"userMobileNumber\":\"919999382228\",\"followerId\":\"34d492ad-847e-4aab-8327-52f2302f70e7\",\"followingId\":\"1910f87b-f409-4bf1-a3cf-b2abc87b40dc\"}}"""
      val data4 = """{\"updateUserProfileResponse\":\"Profile is updated\"}"""
      val invalidData = """{"updateUserProfileResponse1":"Profile is updated"}"""
      val data5 = s"""{"token":"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiIyYjVmMDFhYy1kMTNiLTQ3MTEtODg1ZS1lMzM1YjlkMmRmNDAiLCJwaG9uZV9udW1iZXIiOiI5MTk5OTkzODIyMjMiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ3NTU3MDA3ODE5OH0.cLvCD276q0oxXOsziOINyK61OpMiPXCDmhRf_T9ClcE","userId":"$userId","userContactsIds":["""" + otherUserId + """"]}"""

      val data6 = write(AccountResponse(None, None, None, None))
      val invalidResponse = """{"resource":"ws://localhost:8080","status":"success","errors":[],"data":{}}"""
      val errorResponse = """{"resource":"ws://localhost:8080","status":"success","errors":[{"id":"123","message":"message"}],"data":{}}"""
      val response1 = """{"resource":"ws://localhost:8080","status":"success","errors":[],"data":"""" + data1 + """"}"""
      val response2 = """{"resource":"ws://localhost:8080","status":"success","errors":[],"data":"""" + data2 + """"}"""
      val response3 = """{"resource":"ws://localhost:8080","status":"success","errors":[],"data":"""" + data3 + """"}"""
      val response4 = """{"resource":"ws://localhost:8080","status":"success","errors":[],"data":"""" + data4 + """"}"""
      val response5 = """{"resource":"ws://localhost:8080","status":"success","errors":[],"data":""" + data5 + """}"""
      val response6 = """{"resource":"ws://localhost:8080","status":"success","errors":[],"data":""" + invalidData + """}"""
      val response7 = """{"resource":"ws://localhost:8080","status":"success","errors":[],"data":""" + data6 + """}"""
      assert(checkAccountResponseAndTakeAction("""{}""", Some(userId), Some(phoneNumber)))
      assert(checkAccountResponseAndTakeAction("""{}""", None, None))
      assert(checkAccountResponseAndTakeAction(invalidResponse, Some(userId), Some(phoneNumber)))
      assert(checkAccountResponseAndTakeAction(invalidData, Some(userId), Some(phoneNumber)))
      assert(checkAccountResponseAndTakeAction(errorResponse, None, None))
      assert(checkAccountResponseAndTakeAction(errorResponse, Some(userId), Some(phoneNumber)))
      assert(checkAccountResponseAndTakeAction(response1, Some(userId), Some(phoneNumber)))
      assert(checkAccountResponseAndTakeAction(response2, Some(userId), Some(phoneNumber)))
      assert(checkAccountResponseAndTakeAction(response3, Some(userId), Some(phoneNumber)))
      assert(checkAccountResponseAndTakeAction(response4, Some(userId), Some(phoneNumber)))
      assert(checkAccountResponseAndTakeAction(response6, Some(userId), Some(phoneNumber)))
      assert(checkAccountResponseAndTakeAction(response5, None, None))
      assert(checkAccountResponseAndTakeAction(response7, Some(userId), Some(phoneNumber)))
    }
  }

}
