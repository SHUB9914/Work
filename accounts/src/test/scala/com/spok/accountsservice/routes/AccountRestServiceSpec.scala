package com.spok.accountsservice.routes

import java.io.File

import akka.actor.{ ActorRef, Props }
import akka.http.scaladsl.model.headers.{ CustomHeader, Upgrade, UpgradeProtocol }
import akka.http.scaladsl.model.ws.{ Message, UpgradeToWebSocket }
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.{ RouteTestTimeout, ScalatestRouteTest }
import akka.io.Dns.Command
import akka.stream.{ FlowShape, Graph }
import com.rbmhtechnology.eventuate.ReplicationEndpoint
import com.rbmhtechnology.eventuate.ReplicationEndpoint._
import com.rbmhtechnology.eventuate.log.leveldb.LeveldbEventLog
import com.spok.accountsservice.service.{ AccountManager, AccountView }
import org.scalatest.{ Matchers, WordSpec }

import scala.concurrent.Future
import scala.concurrent.duration._

class AccountRestServiceSpec extends WordSpec with Matchers with ScalatestRouteTest with AccountRestService {

  val endpoint = ReplicationEndpoint(id => LeveldbEventLog.props(logId = "AccountRestServiceSpec"))(system)
  // Initialise event log
  val eventLog = endpoint.logs(DefaultLogName)

  // Init Spok Manager
  val manager = system.actorOf(Props(new AccountManager(endpoint.id, eventLog)))

  // Init Spok View
  val view = system.actorOf(Props(new AccountView(endpoint.id, eventLog)))

  override def viewFullDetail(query: ActorRef, questionId: String, userId: String) = Future.successful("Success")
  override def removeUserFromCache(query: ActorRef, targetUserId: String, userId: String) = Future.successful("Success")
  override def getUserFollowers(query: ActorRef, targetUserId: String, userId: String, pos: String) = Future.successful("Success")
  override def getUserFollowings(query: ActorRef, targetUserId: String, userId: String, pos: String) = Future.successful("Success")
  override def getDetailsOfGroupsForUser(query: ActorRef, userId: String, pos: Option[String]) = Future.successful("Success")
  override def viewMyDetail(query: ActorRef, userId: String) = Future.successful("Success")
  override def viewShortDetail(query: ActorRef, questionId: String, userId: String) = Future.successful("Success")
  override def viewOneGroup(query: ActorRef, userId: String, groupId: String, position: Option[String]) = Future.successful("Success")
  override def viewDetailByAdmin(query: ActorRef, userId: String, targetId: String) = Future.successful("Success")
  override def updateUserProfileHandler(command: ActorRef, query: ActorRef, userId: String, phoneNumber: String, nickName: Option[String],
    birthDate: Option[String], gender: Option[String], geoLat: Option[Double], getLong: Option[Double], geoElev: Option[Double],
    geoText: Option[String], coverFile: Option[File], pictureFile: Option[File]) = Future.successful("Success")

  val route = routes(manager, view)

  "Account service" should {
    "return a response when register path is hit" in {
      Get("/register") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~>
        route ~> check {
          status shouldEqual StatusCodes.SwitchingProtocols
        }
    }

    "return a response when the account path is hit" in {
      Get("/?userId=2344&phone_number=9876543210") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~>
        route ~> check {
          status shouldEqual StatusCodes.SwitchingProtocols
        }
    }

    "return ok if view minimal user detail route is hit" in {
      Get("/user/12345/minimal?userId=234&phone_number=9876543210") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return ok if view full user detail route is hit" in {
      Get("/user/12345?userId=234&phone_number=9876543210") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return ok if route to remove user from cache is hit" in {
      Get("/cache/12345?userId=234&phone_number=9876543210") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return ok if get list of followers of an user route is hit" in {
      Get("/profile/12345/followers/1?userId=234&phone_number=9876543210") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return ok if list of followings of an user route is hit" in {
      Get("/profile/12345/followings/1?userId=234&phone_number=9876543210") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return ok if path to get list of the details of a user's groups is hit" in {
      Get("/allgroups/1?userId=234&phone_number=9876543210") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return ok if to view my details route is hit" in {
      Get("/my/details?userId=234&phone_number=9876543210") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return ok route to view details for specific group is hit with position" in {
      Get("/group/456/1?userId=234&phone_number=9876543210") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return ok route to view details for specific group is hit without position" in {
      Get("/group/456?userId=234&phone_number=9876543210") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return ok to update user profile route is hit without file" in {
      val data = Multipart.FormData.BodyPart.Strict("nickname", "shubham")
      val formData = Multipart.FormData(data)
      Put("/my/profile?userId=234&phone_number=9876543210", formData) ~> route ~> check {
        status shouldBe StatusCodes.OK

      }
    }

    "return ok to update user profile route when give only birthdate" in {
      val data = Multipart.FormData.BodyPart.Strict("birthDate", "1992-05-06")
      val formData = Multipart.FormData(data)
      Put("/my/profile?userId=234&phone_number=9876543210", formData) ~> route ~> check {
        status shouldBe StatusCodes.OK

      }
    }

    "return ok to update user profile route when give only gender" in {
      val data = Multipart.FormData.BodyPart.Strict("gender", "male")
      val formData = Multipart.FormData(data)
      Put("/my/profile?userId=234&phone_number=9876543210", formData) ~> route ~> check {
        status shouldBe StatusCodes.OK

      }
    }

    "return ok to update user profile route when give only geoLat" in {
      val data = Multipart.FormData.BodyPart.Strict("geoLat", "32.1")
      val formData = Multipart.FormData(data)
      Put("/my/profile?userId=234&phone_number=9876543210", formData) ~> route ~> check {
        status shouldBe StatusCodes.OK

      }
    }

    "return ok to update user profile route when give only geoLong" in {
      val data = Multipart.FormData.BodyPart.Strict("geoLong", "30.2")
      val formData = Multipart.FormData(data)
      Put("/my/profile?userId=234&phone_number=9876543210", formData) ~> route ~> check {
        status shouldBe StatusCodes.OK

      }
    }

    "return ok to update user profile route when give only geoElev" in {
      val data = Multipart.FormData.BodyPart.Strict("geoElev", "30.3")
      val formData = Multipart.FormData(data)
      Put("/my/profile?userId=234&phone_number=9876543210", formData) ~> route ~> check {
        status shouldBe StatusCodes.OK

      }
    }

    "return ok route to fetch user profile by admin" in {
      Get("/admin/user/1?userId=234&phone_number=9876543210") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    implicit val defaultTimeout = RouteTestTimeout(30.seconds)

    "return ok if to view details route is hit" in {
      Get("/my/profile?userId=1234&phone_number=213123") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

  }

  private def emulateHttpCore(req: HttpRequest): HttpRequest =
    req.header[Upgrade] match {
      case Some(upgrade) if upgrade.hasWebSocket => req.copy(headers = req.headers :+ upgradeToWebsocketHeaderMock)
      case _ => req
    }

  private def upgradeToWebsocketHeaderMock: UpgradeToWebSocket =
    new CustomHeader() with UpgradeToWebSocket {
      override def requestedProtocols = Nil
      override def name = "dummy"
      override def value = "dummy"

      override def renderInRequests = true
      override def renderInResponses = true

      override def handleMessages(handlerFlow: Graph[FlowShape[Message, Message], Any], subprotocol: Option[String]): HttpResponse =
        HttpResponse(StatusCodes.SwitchingProtocols)
    }

}