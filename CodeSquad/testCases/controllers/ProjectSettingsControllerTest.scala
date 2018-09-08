package controllers

import java.sql.Date

import org.junit.runner.RunWith
import org.specs2.mock.Mockito
import org.specs2.runner.JUnitRunner
import play.api.test.{ FakeRequest, PlaySpecification, WithApplication }
import org.mockito.Mockito.when
import util._
import dbservice._
import akka.util.Timeout
import model.DashboardReports._
import org.joda.time.DateTime
import helper.TestData._

import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class ProjectSettingsControllerTest extends PlaySpecification with Mockito {

  val timeout: Timeout = Timeout(1 minute)
  val mockedProjectDbService = mock[ProjectDbService]
  val mockedAccountSettingDbService = mock[AccountSettingsDbService]
  val mockedProjectSettingsDbService = mock[ProjectSettingsDbService]
  val mockedIAMUtil = mock[IAMUtil]
  val projectSettingsController = new ProjectSettingsController(mockedAccountSettingDbService, mockedProjectSettingsDbService, mockedIAMUtil)

  "User" should {

    "be able to see the welcome-user page" in new WithApplication {
      val result = projectSettingsController.welcomeUser.apply(FakeRequest())
      status(result) must equalTo(303)
    }

    "be able to see whether the project name already exists" in new WithApplication {
      val projectDetails = List("codesquad")
      when(mockedProjectSettingsDbService.getProjectName("codesquad")) thenReturn (projectDetails)
      val result = projectSettingsController.isProjectNameExist("codesquad").apply(FakeRequest("GET", "/isProjectNameExist"))
      status(result) must equalTo(200)
    }

    "not be able to add Project to a free account if I have added 2 projects" in new WithApplication {
      when(mockedProjectSettingsDbService.getNumberOfProjectsAddedByUser("codesquad")) thenReturn (NO_OF_PROJECTS_FOR_FREE_USER)
      val result = projectSettingsController.createNewProject().apply(
        FakeRequest("POST", "/add-project").withSession("username" -> "codesquad")
          .withFormUrlEncodedBody("projectName" -> "project1")
      )
      status(result) must equalTo(SEE_OTHER)
    }

  }
}

