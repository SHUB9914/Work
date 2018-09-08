package dbservice

import model.DashboardReports.{ DashboardDetails, ProjectDetails }
import model.Login.UserLogin
import org.joda.time.DateTime
import org.mockito.Mockito._
import play.api.db.{ Database, Databases }
import play.api.db.evolutions.Evolutions
import play.api.test.{ PlaySpecification, WithApplication }
import org.specs2.mock.Mockito

/**
 * Created by knoldus on 26/7/16.
 */
class ProjectDbServiceTest extends PlaySpecification with Mockito {
  val mockedDbUtil = mock[Database]
  val projectDbService = new ProjectDbService(mockedDbUtil)
  val database = Databases.inMemory(
    name = "default",
    urlOptions = Map(
      "MODE" -> "MYSQL"
    ),
    config = Map(
      "logStatements" -> true
    )
  )

  Evolutions.applyEvolutions(database)

  "Tests " should {
    "be able to get ScalaStyle Report" in new WithApplication {
      val getScalastyleReport = "Scalstyle Reports"
      when(projectDbService.getScalaStyleReport("codesquad", "client")) thenReturn (getScalastyleReport)
      val result = projectDbService.getScalaStyleReport("codesquad", "client")
      result.length must beEqualTo(17)
    }

    "be able to get Scapegoat Report" in new WithApplication {
      val getScapegoatReport = "Scapegoat Reports"
      when(projectDbService.getScapeGoatReport("codesquad", "client")) thenReturn (getScapegoatReport)
      val result = projectDbService.getScapeGoatReport("codesquad", "client")
      result.length must beEqualTo(17)
    }

  }

}

