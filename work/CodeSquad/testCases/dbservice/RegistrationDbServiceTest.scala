package dbservice

import org.mockito.Mockito._
import org.specs2.mock.Mockito
import play.api.db.evolutions.Evolutions
import play.api.db.{ Database, Databases }
import play.api.test.{ PlaySpecification, WithApplication }

/**
 * Created by knoldus on 18/9/16.
 */
class RegistrationDbServiceTest extends PlaySpecification with Mockito {
  val mockedDbUtil = mock[Database]
  val registrationDbService = new RegistrationDbService(mockedDbUtil)
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
    "be able to get username password" in new WithApplication {
      val getUserName = List("test")
      when(registrationDbService.getUserName("codesquad")) thenReturn (getUserName)
      val result = registrationDbService.getUserName("codesquad")
      result.length must beEqualTo(1)
    }

  }
}
