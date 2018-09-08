package controllers

import akka.util.Timeout
import dbservice.{ CollaboratorDbService, LinksDbService, ProjectDbService }
import model.Collaborator.LinkDetail
import org.junit.runner.RunWith
import org.mockito.Mockito.when
import org.specs2.mock.Mockito
import org.specs2.runner.JUnitRunner
import play.api.test.{ FakeRequest, PlaySpecification, WithApplication }
import util.LoginUtil
import scala.concurrent.duration._
import helper.TestData._

@RunWith(classOf[JUnitRunner])
class LoginControllerTest extends PlaySpecification with Mockito {

  val timeout: Timeout = Timeout(1 minute)
  val loginUtility = mock[LoginUtil]
  val mockedProjectDbService = mock[ProjectDbService]
  val collaboratorDbService = mock[CollaboratorDbService]
  val linksDbService = mock[LinksDbService]
  val loginController = new LoginController(loginUtility, mockedProjectDbService, collaboratorDbService, linksDbService)

  "When activation link is not provided, Login Controller" should {

    "show the login page" in new WithApplication {
      val result = loginController.login.apply(FakeRequest())
      status(result) must equalTo(OK)
    }

    "not allow login without credentials" in new WithApplication {
      val result = loginController.index.apply(FakeRequest())
      status(result) must equalTo(BAD_REQUEST)
      flash(result).get("error").get must equalTo("Please enter the credentials to login")
    }

    "not allow login with invalid credentials" in new WithApplication {
      when(loginUtility.authenticateUser(USERNAME, Some(PASSWORD))).thenReturn("fail")
      val result = loginController.index.apply(FakeRequest().withFormUrlEncodedBody(
        "username" -> USERNAME,
        "password" -> PASSWORD
      ))
      status(result) must equalTo(SEE_OTHER)
      flash(result).get("error").get must equalTo("Either username or password was incorrect")
    }

    "show add project page after login when no project is found for user" in new WithApplication {
      when(loginUtility.authenticateUser(USERNAME, Some(PASSWORD))).thenReturn(USERNAME)
      when(mockedProjectDbService.getProjectDetailsForNewUser(USERNAME)).thenReturn(Nil)
      when(mockedProjectDbService.getProjectNamesFromDashboardDetails).thenReturn(List(PROJECT_NAME2))
      val result = loginController.index.apply(FakeRequest().withFormUrlEncodedBody(
        "username" -> USERNAME,
        "password" -> PASSWORD
      ))
      status(result) must equalTo(SEE_OTHER)
      redirectLocation(result) must beSome.which(_ == "/welcome-user")
    }

    "show help page after login when project reports are not found for user" in new WithApplication {
      when(loginUtility.authenticateUser(USERNAME, Some(PASSWORD))).thenReturn(USERNAME)
      when(mockedProjectDbService.getProjectDetailsForNewUser(USERNAME)).thenReturn(List((PROJECT_NAME, USERNAME)))
      when(mockedProjectDbService.getProjectNamesFromDashboardDetails).thenReturn(List(PROJECT_NAME2))
      val result = loginController.index.apply(FakeRequest().withFormUrlEncodedBody(
        "username" -> USERNAME,
        "password" -> PASSWORD
      ))
      status(result) must equalTo(SEE_OTHER)
      redirectLocation(result) must beSome.which(_ == "/help")
    }

    "show dashboard page after login when project reports are found for user" in new WithApplication {
      when(loginUtility.authenticateUser(USERNAME, Some(PASSWORD))).thenReturn(USERNAME)
      when(mockedProjectDbService.getProjectDetailsForNewUser(USERNAME)).thenReturn(List((PROJECT_NAME, USERNAME)))
      when(mockedProjectDbService.getProjectNamesFromDashboardDetails).thenReturn(List(PROJECT_NAME))
      val result = loginController.index.apply(FakeRequest().withFormUrlEncodedBody(
        "username" -> USERNAME,
        "password" -> PASSWORD
      ))
      status(result) must equalTo(SEE_OTHER)
      redirectLocation(result) must beSome.which(_ == "/dashboard")
    }
  }

  "When activation link is provided, Login Controller" should {

    "show the error page if link is found to be expired" in new WithApplication {
      when(linksDbService.getLink(LINK_ID)).thenReturn(List(LinkDetail(LINK_ID, USERNAME, USERNAME, PROJECT_NAME, TRUE)))
      val result = loginController.login.apply(FakeRequest(GET, "/login").withHeaders("id" -> LINK_ID))
      status(result) must equalTo(OK)
    }

    "show the error page if link is found to be expired" in new WithApplication {
      when(linksDbService.getLink(LINK_ID)).thenReturn(List(LinkDetail(LINK_ID, USERNAME, USERNAME, PROJECT_NAME, TRUE)))
      val result = loginController.login.apply(FakeRequest(GET, "/login").withHeaders("id" -> LINK_ID))
      status(result) must equalTo(OK)
    }

    "show the login page if link is found valid and session is found expired" in new WithApplication {
      when(linksDbService.getLink(LINK_ID)).thenReturn(List(LinkDetail(LINK_ID, USERNAME, USERNAME, PROJECT_NAME, FALSE)))
      val result = loginController.login.apply(FakeRequest(GET, "/login").withHeaders("id" -> LINK_ID))
      status(result) must equalTo(OK)
    }

    "not process activation link and show the login page if user logins with another account rather than the username set for the link" in new WithApplication {
      when(linksDbService.getLink(LINK_ID)).thenReturn(List(LinkDetail(LINK_ID, USERNAME, USERNAME, PROJECT_NAME, FALSE)))
      val result = loginController.login.apply(FakeRequest(GET, "/login").withHeaders("id" -> LINK_ID))
      status(result) must equalTo(OK)
    }

  }
}