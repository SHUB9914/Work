package dbservice

import org.mockito.Mockito._
import org.specs2.mock.Mockito
import play.api.db.evolutions.Evolutions
import play.api.db.{ Database, Databases }
import play.api.test.{ PlaySpecification, WithApplication }

/**
 * Created by knoldus on 18/9/16.
 */
class ProjectSettingsDbServiceTest extends PlaySpecification with Mockito {
  val mockedDbUtil = mock[Database]
  val projectSettingsDbService = new ProjectSettingsDbService(mockedDbUtil)
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
    "be able to get " in new WithApplication {
      val getProjectName = List("codesquad")
      when(projectSettingsDbService.getProjectName("codesquad")) thenReturn (getProjectName)
      val result = projectSettingsDbService.getProjectName("codesquad")
      result.length must beEqualTo(1)
    }
  }
}