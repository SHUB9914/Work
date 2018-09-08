package com.spok.notification.routes

import akka.actor.{ ActorRef, Props }
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, StatusCodes }
import akka.http.scaladsl.model.headers.{ CustomHeader, Upgrade, UpgradeProtocol }
import akka.http.scaladsl.model.ws.{ Message, UpgradeToWebSocket }
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.{ FlowShape, Graph }
import com.rbmhtechnology.eventuate.ReplicationEndpoint
import com.rbmhtechnology.eventuate.ReplicationEndpoint._
import com.rbmhtechnology.eventuate.log.leveldb.LeveldbEventLog
import com.spok.model.NotificationDetail
import com.spok.notification.service.{ NotificationManager, NotificationView }
import org.scalatest.{ Matchers, WordSpec }

import scala.concurrent.Future

class NotificationRestServiceSpec extends WordSpec with Matchers with ScalatestRouteTest with NotificationRestService {

  val endpoint = ReplicationEndpoint(id => LeveldbEventLog.props(logId = "NotificationRestServiceSpec"))(system)
  // Initialise event log
  val eventLog = endpoint.logs(DefaultLogName)

  // Init Notification Manager
  val manager = system.actorOf(Props(new NotificationManager(endpoint.id, eventLog)))

  // Init Notification View
  val view = system.actorOf(Props(new NotificationView(endpoint.id, eventLog)))

  override def storeNotificationEvent(command: ActorRef, notificationDetail: NotificationDetail): Future[String] = Future.successful("success")
  override def getNotificationHandler(query: ActorRef, userId: String, pos: Option[String]) = Future.successful("Success")
  val route = routes(manager, view)
  "The Notification Api" should {
    "return ok if the store notification event route is hit" in {
      val notification = NotificationDetail(List(""), "notification1", "comment1", "user id1", "emmiter id")
      Post("/notification/add", notification) ~> route ~> check {
        status should be(StatusCodes.OK)
      }
    }

    "return ok if the send notification event route is hit" in {
      val notification = NotificationDetail(List("userid"), "notification1", "comment1", "user id1", "emmiter id")
      Post("/notification/send", notification) ~> route ~> check {
        status should be(StatusCodes.OK)
      }
    }

    "return a response when the user is connected to receive notification" in {
      Get("/?userId=1233445&phone_number=918534566899") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~>
        route ~> check {
          status shouldEqual StatusCodes.SwitchingProtocols
        }
    }

    "return ok if route to view wall's notifications is hit" in {
      Get("/my/allnotifications/1?userId=234&phone_number=9876543210") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return ok if route to view wall's notifications is hit without segment" in {
      Get("/my/allnotifications?userId=234&phone_number=9876543210") ~> route ~> check {
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
