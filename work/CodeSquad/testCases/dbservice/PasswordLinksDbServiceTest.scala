package dbservice

import helper.TestDatabaseHelper
import org.specs2.mock.Mockito
import play.api.db.Databases
import play.api.db.evolutions.Evolutions
import play.api.test.{ PlaySpecification, WithApplication }

class PasswordLinksDbServiceTest extends PlaySpecification with Mockito {

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

  val passwordService = new PasswordLinksDbService(database)

  Databases.withInMemory() { db =>
    new TestDatabaseHelper(db).insertDummyPasswordLink

    "PasswordLinkService" should {
      "return particular link by id" in new WithApplication() {
        assert(passwordService.getLink("111").nonEmpty)
      }

      "destroy a link id" in new WithApplication() {
        assert(passwordService.destroyLink("111") === 1)
      }
    }
  }

}
