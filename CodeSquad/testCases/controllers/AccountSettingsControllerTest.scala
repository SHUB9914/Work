package controllers

import akka.util.Timeout
import dbservice._
import org.junit.runner.RunWith
import org.mockito.Mockito.when
import org.specs2.mock.Mockito
import org.specs2.runner.JUnitRunner
import play.api.test.{ FakeRequest, PlaySpecification, WithApplication }
import util.CommonUtil

import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class AccountSettingsControllerTest extends PlaySpecification with Mockito {

  val timeout: Timeout = Timeout(1 minute)
  val mockedAccountSettingsDbService = mock[AccountSettingsDbService]
  val mockedProjectDbService = mock[ProjectDbService]
  val passwordLinksDbService = mock[PasswordLinksDbService]
  val utilities = mock[CommonUtil]
  val accountSettingsController = new AccountSettingsController(mockedAccountSettingsDbService, mockedProjectDbService, passwordLinksDbService, utilities)
  val user = "codesquad"
  val email = "codesquadupdatedemail@gmail.com"
  val password = "password"
  "User" should {

    "be able to see the email change page" in new WithApplication {
      val userEmail = "codesquad@gmail.com"
      when(mockedAccountSettingsDbService.showEmail(user)) thenReturn userEmail
      val result = accountSettingsController.showEmail.apply(FakeRequest("GET", "/show-email"))
      status(result) must equalTo(OK)
    }

    "be able to change the current email" in new WithApplication {
      val userEmailUpdateResponse = 1
      when(mockedAccountSettingsDbService.changeEmail(user, email)) thenReturn userEmailUpdateResponse
      val result = accountSettingsController.changeEmail(email).apply(FakeRequest("GET", "/changeEmail"))
      status(result) must equalTo(303)
    }

    "be able to see the change password page" in new WithApplication {
      val result = accountSettingsController.updatePasswordPage.apply(FakeRequest())
      status(result) must equalTo(OK)
    }

    "be able to change the current password" in new WithApplication {
      val userPasswordUpdateResponse = 1
      when(mockedAccountSettingsDbService.updateUserPassword(user, password)) thenReturn userPasswordUpdateResponse
      val result = accountSettingsController.changeEmail(email).apply(FakeRequest("POST", "/update-user-password"))
      status(result) must equalTo(303)
    }
  }
}

