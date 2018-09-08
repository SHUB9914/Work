package dbservice

import helper.TestDatabaseHelper
import model.Collaborator.Link
import org.specs2.mock.Mockito
import play.api.db.Databases
import play.api.db.evolutions.Evolutions
import play.api.test.{ PlaySpecification, WithApplication }
import util.Constant.{ ErrorMessages, LinkType }

class CollaboratorDbServiceTest extends PlaySpecification with Mockito {

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

  val collaboratorService = new CollaboratorDbService(database)

  Databases.withInMemory() { db =>
    new TestDatabaseHelper(db).insertDummyRecords

    "Collaborator Service" should {
      "be able to get all users successfully" in new WithApplication() {
        assert(collaboratorService.getAllUsers === List("deepak", "divya", "raghav"))
      }

      "not be able to validate collaborator having invalid email id" in new WithApplication() {
        assert(collaboratorService.validateCollaborator("DQR", "harsh@") === Left(ErrorMessages.invalidEmailMessage))
      }

      "be able to validate collaborator and save email link for non existing user having by email id" in new WithApplication() {
        assert(collaboratorService.validateCollaborator("DQR", "harshs316@gmail.com") === Right(Link(None, "harshs316@gmail.com", "DQR", LinkType.registration)))
      }

      "be able to validate collaborator and save email link for existing user non existing collaborator having by email id" in new WithApplication() {
        assert(collaboratorService.validateCollaborator("DQR", "raghav@knoldus.com") === Right(Link(Some("raghav"), "raghav@knoldus.com", "DQR", LinkType.login)))
      }

      "not be able to validate collaborator and save email link for existing user and collaborator having by email id" in new WithApplication() {
        assert(collaboratorService.validateCollaborator("DQR", "harsh@knoldus.com") === Left("User harsh@knoldus.com for project DQR already exists"))
      }

      "not be able to validate collaborator having invalid username" in new WithApplication() {
        assert(collaboratorService.validateCollaborator("DQR", "ramesh") === Left(ErrorMessages.userNotFoundError.format("ramesh")))
      }

      "not be able to validate collaborator having username and collaborator exists" in new WithApplication() {
        assert(collaboratorService.validateCollaborator("DQR", "deepak") === Left(ErrorMessages.collaboratorFoundError.format("deepak", "DQR")))
      }

      "not be able to validate collaborator having username and collaborator exists" in new WithApplication() {
        assert(collaboratorService.validateCollaborator("DQR", "raghav").isRight)
      }

      "not be able to create new collaborator if it already exists" in new WithApplication() {
        assert(collaboratorService.createNewCollaborator("divya", "DQR") === Some(ErrorMessages.collaboratorFoundError.format("divya", "DQR")))
      }

      "be able to create new collaborator" in new WithApplication() {
        assert(collaboratorService.createNewCollaborator("raghav", "DQR") === None)
      }

      "be able to get collaborators for a particular project" in new WithApplication() {
        assert(collaboratorService.getCollaboratorsByProject("DQR").length === 3)
      }

      "not be able to get username by email" in new WithApplication() {
        assert(collaboratorService.getUsernameByEmail("harsh@knoldu.com") === None)
      }

      "be able to get username by email" in new WithApplication() {
        assert(collaboratorService.getUsernameByEmail("harsh@knoldus.com") === Some("divya"))
      }

      "be able to delete a collaborator from a project" in new WithApplication() {
        assert(collaboratorService.deleteCollaborator("DQR", "raghav") != 0)
      }
    }

  }

}