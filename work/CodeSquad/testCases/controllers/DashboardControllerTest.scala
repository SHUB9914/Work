package controllers
/*
import akka.util.Timeout
import dbservice._
import model.DashboardReports._
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.mockito.Mockito.when
import org.specs2.mock.Mockito
import org.specs2.runner.JUnitRunner
import play.api.cache.CacheApi
import play.api.libs.json.{ JsString, Json }
import play.api.test.{ FakeRequest, PlaySpecification, WithApplication }
import util.Constant.ErrorMessages
import helper.TestData._
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class DashboardControllerTest extends PlaySpecification with Mockito {

  val timeout: Timeout = Timeout(1 minute)
  val mockedProjectDbService = mock[ProjectDbService]
  val mockedSchedulerDbProcess = mock[SchedulerDbProcess]
  val mockedAccountSettingsDbService = mock[AccountSettingsDbService]
  val cache = mock[CacheApi]

  val dashboardController = new DashboardController(mockedAccountSettingsDbService, mockedProjectDbService, mockedSchedulerDbProcess) {

  }
  val user = "codesquad"
  val projectList = List("Codesquad")

  "Dashboard" should {
    "be able to get scalastyle reports" in new WithApplication {
      val scalastyleReports = "These are the scala style reports"
      when(mockedProjectDbService.getScalaStyleReport("spok", "accounts")) thenReturn (scalastyleReports)
      val result = dashboardController.getScalaStyleReport("spok", "accounts").apply(FakeRequest("GET", "/getScalaStyleReport/spok/accounts"))
      status(result) must equalTo(200)
      // contentAsString must contain("\"scala_style\" : \"scalaStyleTest\"")
    }

    "be able to get scapegoat reports" in new WithApplication() {
      val scapegoatReport = "Scapegoat Reports in HTML"
      when(mockedProjectDbService.getScapeGoatReport("spok", "accounts")) thenReturn (scapegoatReport)
      val result = dashboardController.getScapeGoatReport("spok", "accounts").apply(FakeRequest("GET", "/getScapegoatReport/spok/accounts"))
      status(result) must equalTo(200)

    }

    "be able to get all details for the charts" in new WithApplication() {
      val getChartDetails = List(ProjectDetails("12", "23", "11", "78", "0", "90.0", new DateTime()))
      when(mockedProjectDbService.getAllDetails("spok", "accounts", "5")) thenReturn (getChartDetails)
      val result = dashboardController.getAllDetails("spok", "accounts", "5").apply(FakeRequest("GET", "/getAllDetails/spok/accounts/5"))

      status(result) must equalTo(200)

    }

    "be able to insert reports" in new WithApplication() {
      val userDashboardDetails = List(DashboardDetails("spok", "accounts", "45", "56", "60", "70", "90.1", "456", "789", "2016-05-10"))
      when(mockedProjectDbService.getDashboardDetailsFromDb("spok")) thenReturn (userDashboardDetails)
      val result = dashboardController.dashboard.apply(FakeRequest("GET", "/dashboard"))
      status(result) must equalTo(303)

    }

    "not be able to force build the project when session is expired" in new WithApplication() {
      val result = dashboardController.forceBuildByProjectName(PROJECT_NAME).apply(FakeRequest("GET", s"forceBuild/$PROJECT_NAME"))
      status(result) must equalTo(OK)
    }

  }
}*/

