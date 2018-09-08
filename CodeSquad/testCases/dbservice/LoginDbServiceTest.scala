package dbservice

import model.Login.UserLogin
import org.mockito.Mockito._
import org.specs2.mock.Mockito
import play.Logger
import play.api.db.evolutions.Evolutions
import play.api.db.{ Database, Databases }
import play.api.test.{ FakeRequest, PlaySpecification, WithApplication }

/**
 * Created by knoldus on 18/9/16.
 */
class LoginDbServiceTest extends PlaySpecification with Mockito {
  val mockedDbUtil = mock[Database]
  val loginDbService = new LoginDbService(mockedDbUtil)
  val username = "codesquad"
  val password = "codesquad"

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
      pending
      val usernamePassword = List("codesquad", "codesquad")
      when(loginDbService.getUsernamePassword(username, password)) thenReturn (usernamePassword)
      val result = loginDbService.getUsernamePassword(username, password)
      result.length must beEqualTo(1)
    }

  }

}