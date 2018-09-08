package controllers

import akka.util.Timeout
import com.amazonaws.services.identitymanagement.model._
import dbservice._
import org.junit.runner.RunWith
import org.mockito.Mockito.when
import org.specs2.mock.Mockito
import org.specs2.runner.JUnitRunner
import play.api.cache.CacheApi
import play.api.test.{ PlaySpecification, WithApplication }
import util.IAMUtil

import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class IAMUtilTest extends PlaySpecification with Mockito {

  val timeout: Timeout = Timeout(1 minute)
  val mockedIAMUtil = mock[IAMUtil]
  val mockedSchedulerDbProcess = mock[SchedulerDbProcess]
  val cache = mock[CacheApi]
  val iamUtil = new IAMUtil
  val createAccessKeyResult = new CreateAccessKeyResult

  "User" should {
    "be able to create access key" in new WithApplication {
      pending
      val accessKeyForUser = Some(createAccessKeyResult)
      when(mockedIAMUtil.createAccessKeyForUser("codesquad")) thenReturn (accessKeyForUser)
      val result = iamUtil.createAccessKeyForUser("codesquad")
      assert(result === accessKeyForUser)
    }
  }

  "User" should {
    "be able to get keys" in new WithApplication {
      pending
      val getKeys = Some(("access-key", "secret-key"))
      when(mockedIAMUtil.getKeysFromResult(createAccessKeyResult)) thenReturn (getKeys)
      val result = iamUtil.getKeysFromResult(createAccessKeyResult)
      assert(result === getKeys)
    }
  }

}
