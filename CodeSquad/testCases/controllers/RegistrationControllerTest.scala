package controllers

import akka.util.Timeout
import dbservice._
import org.junit.runner.RunWith
import org.specs2.mock.Mockito
import org.specs2.runner.JUnitRunner
import play.api.cache.CacheApi
import play.api.test.{ FakeRequest, PlaySpecification, WithApplication }
import util.{ CommonUtil, ConfigUtil }

import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class RegistrationControllerTest extends PlaySpecification with Mockito {

  val timeout: Timeout = Timeout(1 minute)
  val mockedCommonUtil = mock[CommonUtil]
  val mockedConfigUtil = mock[ConfigUtil]
  val mockedProjectDbService = mock[ProjectDbService]
  val mockedSchedulerDbProcess = mock[SchedulerDbProcess]
  val mockedRegistrationDbService = mock[RegistrationDbService]
  val mockedCollaboratorDbService = mock[CollaboratorDbService]
  val linksDbService = mock[LinksDbService]
  val commonUtil = mock[CommonUtil]
  val cache = mock[CacheApi]

  val registrationController = new RegistrationController(mockedRegistrationDbService, mockedCollaboratorDbService, linksDbService, commonUtil, mockedProjectDbService)

  "User" should {

    "be able to see the registration page" in new WithApplication {
      val result = registrationController.registerForm.apply(FakeRequest())
      status(result) must equalTo(200)
    }

  }
}

