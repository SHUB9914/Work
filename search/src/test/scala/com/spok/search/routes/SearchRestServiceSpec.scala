package com.spok.search.routes

import akka.actor.Props
import akka.http.scaladsl.model.headers.{ CustomHeader, Upgrade }
import akka.http.scaladsl.model.ws.{ Message, UpgradeToWebSocket }
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, StatusCodes }
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.{ FlowShape, Graph }
import com.spok.search.handler.SearchRestServiceHandler
import com.spok.search.service.SearchView
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ Matchers, WordSpec }

import scala.concurrent.Future

class SearchRestServiceSpec extends WordSpec with Matchers with ScalatestRouteTest with SearchRestService with MockitoSugar {

  override val searchRestServiceHandler: SearchRestServiceHandler = mock[SearchRestServiceHandler]
  val view = system.actorOf(Props(new SearchView))

  val route = routes(view)
  "SearchRestServiceSpec" should {

    "return ok if search nickname route is hit" in {
      when(searchRestServiceHandler.getByNickname(view, "cyril")) thenReturn (Future.successful("Success"))
      Get("/search/autonick?nickname=cyril&userId=234&phone_number=9876543210") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return ok if search hashtag route is hit" in {
      when(searchRestServiceHandler.getByHashtag(view, "awe")) thenReturn (Future.successful("Success"))
      Get("/search/autohash?hashtag=awe&userId=234&phone_number=9876543210") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return ok if search popular spoker route is hit" in {
      when(searchRestServiceHandler.getPopularSpoker(view, "1")) thenReturn (Future.successful("Success"))
      Get("/search/popular/1?userId=234&phone_number=9876543210") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return ok if search last spoks route is hit" in {
      when(searchRestServiceHandler.getLastSpoks(view, "234", "1")) thenReturn (Future.successful("Success"))
      Get("/search/last/1?userId=234&phone_number=9876543210") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return ok if search trendy spoks route is hit" in {
      when(searchRestServiceHandler.getTrendySpoks(view, "1", "234")) thenReturn (Future.successful("Success"))
      Get("/search/trendy/1?userId=234&phone_number=9876543210") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return ok if search last spoks of my friend route is hit" in {
      when(searchRestServiceHandler.getFriendSpoks(view, "234", "1")) thenReturn (Future.successful("Success"))
      Get("/search/friends/1?userId=234&phone_number=9876543210") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return ok if url to launch search is hit " in {
      when(searchRestServiceHandler.getlaunchSearch(view, "1", Some("cyrilid"), Some("abc"),
        "11.22", "22.13", "111", "999", Some("picture"), "234")) thenReturn (Future.successful("Success"))
      Get("/search/1?userids=cyrilid&hashtags=abc&latitude=11.22&longitude=22.13&start=111&end=999&content_types=picture&userId=234&phone_number=9876543210") ~> route ~> check {
        status shouldBe StatusCodes.OK
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

