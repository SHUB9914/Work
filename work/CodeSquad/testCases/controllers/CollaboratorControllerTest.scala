package controllers

import com.sun.jersey.api.client.ClientResponse
import dbservice.{ CollaboratorDbService, LinksDbService }
import helper.TestData._
import model.Collaborator.Link
import org.mockito.Mockito.when
import org.specs2.mock.Mockito
import play.api.libs.json.Json
import play.api.test.{ FakeRequest, PlaySpecification, WithApplication }
import util.CommonUtil
import util.Constant._

class CollaboratorControllerTest extends PlaySpecification with Mockito {

  val mockedCollaboratorService = mock[CollaboratorDbService]
  val mockedCommonUtil = mock[CommonUtil]
  val mockedLinkDbService = mock[LinksDbService]

  val collaboratorController = new CollaboratorController(mockedCollaboratorService, mockedCommonUtil, mockedLinkDbService)

  "Collaborator Controller" should {

    "not be able to delete the collaborator if session does not exist" in new WithApplication {
      when(mockedCollaboratorService.deleteCollaborator(PROJECT_NAME, TEST_USERNAME)).thenReturn(1)
      val result = collaboratorController.deleteCollaborator(PROJECT_NAME, TEST_USERNAME).apply(FakeRequest("DELETE", "/deleteCollaborator").withHeaders("X-Requested-With" -> "XMLHttpRequest"))
      status(result) must equalTo(OK)
    }

    "be able to delete the collaborator" in new WithApplication {
      when(mockedCollaboratorService.deleteCollaborator(PROJECT_NAME, "deepak")).thenReturn(1)
      val result = collaboratorController.deleteCollaborator(PROJECT_NAME, "deepak").apply(FakeRequest("DELETE", "/deleteCollaborator")
        .withSession("username" -> TEST_USERNAME)
        .withHeaders("X-Requested-With" -> "XMLHttpRequest"))
      contentAsJson(result) === Json.toJson(Map("message" -> SuccessMessage.collaboratorDeletedMessage.format("deepak")))
    }

    "be able to get all users" in new WithApplication {
      when(mockedCollaboratorService.getAllUsers).thenReturn(List("harsh", "deepak"))
      val result = collaboratorController.getAllUsers().apply(FakeRequest("GET", "/getAllUsers")
        .withSession("username" -> TEST_USERNAME)
        .withHeaders("X-Requested-With" -> "XMLHttpRequest"))
      contentAsJson(result) === Json.toJson(Map("data" -> List("harsh", "deepak")))
    }
  }

  "Collaborator Controller" should {

    "not be able to generate activation link when limit of sending invitation got exceeded" in new WithApplication {
      when(mockedCollaboratorService.getCollaboratorsByProject(PROJECT_NAME)).thenReturn(List(COLLABORATOR, COLLABORATOR, COLLABORATOR))
      when(mockedLinkDbService.countPendingInvites(TEST_USERNAME, PROJECT_NAME)).thenReturn(3l)
      val result = collaboratorController.generateActivationLink(PROJECT_NAME, TEST_USERNAME).apply(FakeRequest()
        .withSession("username" -> TEST_USERNAME)
        .withHeaders("X-Requested-With" -> "XMLHttpRequest"))
      contentAsJson(result) === Json.toJson(Map("message" -> ErrorMessages.linksLimitExceedError, "status" -> "error"))
    }

    "not be able to generate activation link due to error in validating collaborator" in new WithApplication {
      when(mockedCollaboratorService.getCollaboratorsByProject(PROJECT_NAME)).thenReturn(List(COLLABORATOR))
      when(mockedLinkDbService.countPendingInvites(TEST_USERNAME, PROJECT_NAME)).thenReturn(1l)
      when(mockedCollaboratorService.validateCollaborator(PROJECT_NAME, "deepak")).thenReturn(Left(ErrorMessages.collaboratorFoundError.format("deepak", PROJECT_NAME)))
      val result = collaboratorController.generateActivationLink(PROJECT_NAME, "deepak").apply(FakeRequest()
        .withSession("username" -> TEST_USERNAME)
        .withHeaders("X-Requested-With" -> "XMLHttpRequest"))
      contentAsJson(result) === Json.toJson(Map("message" -> ErrorMessages.collaboratorFoundError.format("deepak", PROJECT_NAME), "status" -> "error"))
    }

    "be able to generate and send activation link" in new WithApplication {
      when(mockedCollaboratorService.getCollaboratorsByProject(PROJECT_NAME)).thenReturn(List(COLLABORATOR))
      when(mockedLinkDbService.countPendingInvites(TEST_USERNAME, PROJECT_NAME)).thenReturn(1l)
      when(mockedCollaboratorService.validateCollaborator(PROJECT_NAME, "deepak")).thenReturn(Right(Link(Some("deepak"), "deepak@knoldus.com", PROJECT_NAME, LinkType.login)))
      when(mockedCommonUtil.generateInviteLink("localhost:9000", Link(Some("deepak"), "deepak@knoldus.com", PROJECT_NAME,
        LinkType.login))).thenReturn((LOGIN_LINK.format("localhost:9000", "12345"), "deepak", "12345"))
      when(mockedCommonUtil.sendEmail("deepak@knoldus.com", INVITATION_MAIL_SUBJECT, "message")).thenReturn(any[Option[ClientResponse]])
      val result = collaboratorController.generateActivationLink(PROJECT_NAME, "deepak").apply(FakeRequest(GET, "http://localhost:9000/")
        .withSession("username" -> TEST_USERNAME, "host" -> "dd")
        .withHeaders("X-Requested-With" -> "XMLHttpRequest"))
      contentAsJson(result) === Json.toJson(Map("message" -> SuccessMessage.inviteSentMessage.format("deepak"), "status" -> "success"))
    }

  }
}