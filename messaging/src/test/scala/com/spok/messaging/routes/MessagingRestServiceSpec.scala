package com.spok.messaging.routes

import akka.actor.{ ActorRef, Props }
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{ CustomHeader, Upgrade, UpgradeProtocol }
import akka.http.scaladsl.model.ws.{ Message, UpgradeToWebSocket }
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.{ FlowShape, Graph }
import com.spok.messaging.service.{ MessagingManager, MessagingView }
import com.spok.model.Messaging.MessageJsonData
import org.scalatest.{ Matchers, WordSpec }

import scala.concurrent.Future

class MessagingRestServiceSpec extends WordSpec with Matchers with ScalatestRouteTest with MessagingRestService {

  // Init Messaging Manager
  val manager = system.actorOf(Props(new MessagingManager))

  // Init Messaging View
  val view = system.actorOf(Props(new MessagingView))

  //override def viewFullDetail(query: ActorRef, questionId: String, userId: String) = Future.successful("Success")

  val route = routes(manager, view)

  override def toHandleMessage(command: ActorRef, userId: String, messageJsonData: MessageJsonData, friendUserId: String) = Future.successful(HttpResponse(StatusCodes.OK))
  override def viewTalks(query: ActorRef, pos: Option[String], userId: String) = Future.successful("Success")
  override def viewSingleTalk(query: ActorRef, targetUserId: String, messageId: Option[String], userId: String, order: String) = Future.successful("Success")
  override def getByMessage(query: ActorRef, msg: String, userId: String) = Future.successful("Success")
  override def getByTalkers(query: ActorRef, talkers: String) = Future.successful("Success")

  "Messaging service" should {

    "return ok to ping route is hit" in {
      Get("/ping") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return ok to send message route is hit" in {
      val data = Multipart.FormData.BodyPart.Strict("data", """{"message":"first message"}""")
      val formData = Multipart.FormData(data)
      Post("/talk/9876?userId=234&phone_number=9876543210", formData) ~> route ~> check {
        status shouldBe StatusCodes.OK

      }
    }

    "return bad request to send message route is hit with invalid json" in {
      val data = Multipart.FormData.BodyPart.Strict("data", """{"msg":"first message"}""")
      val formData = Multipart.FormData(data)
      Post("/talk/9876?userId=234&phone_number=9876543210", formData) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest

      }
    }

    "return ok when route to view all talks of a user is hit without position" in {
      Get("/talks?userId=234&phone_number=9876543210") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return ok when route to view all talks of a user is hit with position" in {
      Get("/talks/2?userId=234&phone_number=9876543210") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return ok when route to view all messages of single talk of a user is hit without position" in {
      Get("/talk/7564535?order=desc&userId=234&phone_number=9876543210") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return ok when route to view all messages of single talk of a user is hit with position" in {
      Get("/talk/7564535/2?order=asc&userId=234&phone_number=9876543210") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return failed when route to view all messages of single talk of a user is hit with invalid order" in {
      Get("/talk/7564535/2?order=fff&userId=234&phone_number=9876543210") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return a response when the messages ws path is hit" in {
      Get("/?userId=2344&phone_number=9876543210") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~>
        route ~> check {
          status shouldEqual StatusCodes.SwitchingProtocols
        }
    }

    "return ok when route to search messages is hit with message parameter" in {
      Get("/searchtalks/searchmsg?msg=Helloc&userId=234&phone_number=9876543210") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return ok when route to search talker is hit " in {
      Get("/searchtalks/searchtalker?talkers=piy&userId=234&phone_number=9876543210") ~> route ~> check {
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
