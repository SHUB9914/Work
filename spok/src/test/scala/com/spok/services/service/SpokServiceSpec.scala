package com.spok.services.service

import java.io.File

import akka.actor.{ ActorRef, Props }
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{ CustomHeader, Upgrade, UpgradeProtocol }
import akka.http.scaladsl.model.ws.{ Message, UpgradeToWebSocket }
import akka.http.scaladsl.testkit.{ ScalatestRouteTest, WSProbe }
import akka.stream.{ FlowShape, Graph }
import akka.util.ByteString
import com.rbmhtechnology.eventuate.ReplicationEndpoint
import com.rbmhtechnology.eventuate.ReplicationEndpoint._
import com.rbmhtechnology.eventuate.log.leveldb.LeveldbEventLog
import com.spok.services.routes.SpokRestService
import com.spok.util.Constant._
import org.scalatest.{ Matchers, WordSpec }

import scala.concurrent.Future

class SpokServiceSpec extends WordSpec with Matchers with ScalatestRouteTest with SpokRestService {

  val endpoint = ReplicationEndpoint(id => LeveldbEventLog.props(logId = "SpokServiceSpec"))(system)
  // Initialise event log
  val eventLog = endpoint.logs(DefaultLogName)

  // Init Spok Manager
  val manager = system.actorOf(Props(new SpokManager(endpoint.id, eventLog)))

  // Init Spok View
  val view = system.actorOf(Props(new SpokView(endpoint.id, eventLog)))

  override def viewPollQuestionHandler(query: ActorRef, questionId: String, userId: String) = Future.successful("Success")
  override def spokStatsHandler(query: ActorRef, spokId: String): Future[String] = Future.successful("Success")
  override def getCommentsHandler(query: ActorRef, spokId: String, pos: String) = Future.successful("Success")
  override def getReSpokersHandler(query: ActorRef, spokId: String, pos: String) = Future.successful("Success")
  override def scopedUsersHandler(query: ActorRef, spokId: String, pos: String) = Future.successful("Success")
  override def spokStackHandler(query: ActorRef, pos: String, userId: String) = Future.successful("Success")
  override def viewShortSpok(query: ActorRef, spokId: String, targetUserId: String, userId: String) = Future.successful("Success")
  override def viewSpokersWallHandler(query: ActorRef, targetUserId: String, pos: String, userId: String) = Future.successful("Success")
  override def viewFullSpok(query: ActorRef, spokId: String, targetUserId: String, userId: String) = Future.successful("Success")
  override def storeSpokSettings(query: ActorRef, command: ActorRef, userId: String, json: String, file: Option[File]) = Future.successful(HttpResponse(StatusCodes.OK))

  val route = routes(manager, view)

  "The spok api" should {
    "send pong as a reply" in {
      Get("/ping") ~> route ~> check {
        responseAs[HttpResponse].entity shouldEqual HttpEntity.Strict(ContentTypes.`text/plain(UTF-8)`, ByteString("pong", "UTF-8"))
      }
    }
    "handle web socket request" in {
      val wsClient = WSProbe()

      WS(
        "/" + GREETER,
        wsClient.flow
      ) ~> route ~>
        check {
          // check response for WS Upgrade headers
          isWebSocketUpgrade shouldEqual true

          wsClient.sendMessage("Peter")
          wsClient.expectMessage("Hello ping check Peter!")

          wsClient.sendMessage("John")
          wsClient.expectMessage("Hello ping check John!")

          wsClient.sendCompletion()
          wsClient.expectCompletion()
        }
    }

    "return ok if the spok step one route is hit" in {
      Get("/spoks?userId=1233445&phone_number=918534566899") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
        status shouldEqual StatusCodes.SwitchingProtocols
      }
    }

    "return ok if view poll question route is hit" in {
      Get("/poll/12345?userId=234&phone_number=9876543210") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return ok if spok stats route is hit" in {
      Get("/spok/1234/stats?userId=234&phone_number=9876543210") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return ok if get comments of a spok route is hit" in {
      Get("/spok/12345/comments/1?userId=234&phone_number=9876543210") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return ok if get respokers route is hit" in {
      Get("/spok/12345/respokers/1?userId=234&phone_number=9876543210") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return ok if get scoped user route is hit" in {
      Get("/spok/12345/scoped/1?userId=234&phone_number=9876543210") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return ok if  spoks stack route is hit" in {
      Get("/spoks/1234?userId=234&phone_number=9876543210") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return ok if view short spok route of target user is hit" in {
      Get("/stack/12345/1234?userId=234&phone_number=9876543210") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return ok if view short spok route is hit" in {
      Get("/stack/12345?userId=234&phone_number=9876543210") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return ok if get spoker's wall route is hit" in {
      Get("/user/12345/wall/1?userId=234&phone_number=9876543210") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return ok if view full spok route of target user is hit" in {
      Get("/spok/12345/full/1234?userId=234&phone_number=9876543210") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return ok if view full spok route is hit" in {
      Get("/spok/12345/full?userId=234&phone_number=9876543210") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return ok if poll stats route is hit" in {
      Get("/spok/1234/pollResults?userId=234&phone_number=9876543210") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return ok to create spok route is hit with file" in {
      val data = Multipart.FormData.BodyPart.Strict("data", """{"contentType":"picture","groupId":"0","visibility":"public","geo":{"latitude":60.0,"longitude":60.0,"elevation":60.0}}""")
      val picture = Multipart.FormData.BodyPart.fromFile("file", ContentTypes.`application/octet-stream`, File.createTempFile("test", ".jpg"))
      val formData = Multipart.FormData(data, picture)
      Post("/create/spok?userId=234&phone_number=9876543210", formData) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }
    "return ok if get my spoks route is hit" in {
      Get("/my/spoks/1?userId=234&phone_number=9876543210") ~> route ~> check {
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
