package com.spok.apiservice.service

import java.io.File

import akka.actor.Actor
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.ws.TextMessage.Strict
import akka.http.scaladsl.model.ws.{ Message, TextMessage, _ }
import akka.http.scaladsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.testkit.{ RouteTestTimeout, ScalatestRouteTest }
import akka.stream.scaladsl.{ Flow, Keep, Source, SourceQueue }
import akka.stream.testkit.scaladsl.{ TestSink, TestSource }
import akka.stream.{ FlowShape, Graph }
import akka.testkit.TestActorRef
import com.spok.apiservice.handler.ApiServiceHandler
import com.spok.model.Account.UserRegistrationResponse
import com.spok.persistence.factory.DSEGraphPersistenceFactoryApi
import com.spok.model.NotificationDetail
import com.spok.model.SpokModel._
import com.spok.persistence.redis.RedisFactory
import com.spok.util.ConfigUtil._
import com.spok.util.Constant._
import com.spok.util.JsonHelper
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpec }
import scala.concurrent.duration._

import scala.concurrent.Future

class ApiServiceSpec extends WordSpec with Matchers with MockitoSugar with BeforeAndAfterAll with ScalatestRouteTest with JsonHelper {

  val apiService = new ApiService() {
    override val http = mock[HttpExt]
    override val redisFactory = mock[RedisFactory]
    override val dseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
  }
  val route = apiService.routes

  "Api Service" should {

    "return ok if view details route is hit" in {
      when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
      val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
      val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + accountPort + DELIMITER + "my" + DELIMITER + "profile" +
        "?userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376")
      when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
      Get("/my/profile").withHeaders(rawHeader) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "return pong in response" in {

      when(apiService.http.singleRequest(HttpRequest(uri = HTTP + interface + ":" + spokPort + DELIMITER + PING)))
        .thenReturn(Future(HttpResponse(StatusCodes.OK)))
      Get("/ping") ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "return the bad request when server is not up" in {
      Get("/greeter") ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "return ok if the server is up test2" in {
      Get("/greeter") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
        status shouldEqual StatusCodes.SwitchingProtocols
      }
    }

    "return proper error response when random path is hit on get call" in {
      Get("/random") ~> route ~> check {
        rejection shouldEqual AuthorizationFailedRejection
      }
    }

    "return proper error response when random path is hit on websocket" in {
      Get("/random") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
        status shouldEqual StatusCodes.SwitchingProtocols
      }
    }

    "return Invalid JSON as response from Spok server, if INVALID_JSON is sent" in {
      val actorRef = TestActorRef(new Actor {
        def receive = {
          case ConnectService(actor, userId, phoneNumber, source, notificationSource, accountSource, messagingSource) => {
            sender ! INVALID_JSON
          }
          case DisconnectService(phoneNumber) => {
            info(s" ${phoneNumber} is disconnected. So disconnecting all connection for this user.")
          }
        }
      })

      val apiService = new ApiService() {
        override val http = mock[HttpExt]

        override def addInQueue(spokeQueue: Future[SourceQueue[Message]], message: Strict) = Future.failed(new Exception("Failed"))

        //override val apiConnector = actorRef
      }

      val result = (apiService.addMessageInSourceQueue(Some("userId"), Some("userNumber")))
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(
        """{
                                        "contentType":"rawtext",
                                        "groupId" :"0",
                                        "visibility" : "Public",
                                        "ttl":0,
                                        "instanceText":"first spok",
                                        "text":"text",
                                        "geo":{
                                        "latitude" : 13.67,
                                        "longitude" : 14.56,
                                        "elevation" : 33.34
                                        }
                                        }"""
      ))
      sub.ensureSubscription()
    }

    "return Invalid JSON as response from server for registration, if INVALID_JSON is sent" in {
      val actorRef = TestActorRef(new Actor {
        def receive = {
          case ConnectService(actor, userId, phoneNumber, source, notificationSource, accountSource, messagingSource) => {
            sender ! INVALID_JSON
          }
          case DisconnectService(phoneNumber) => {
            info(s" ${phoneNumber} is disconnected. So disconnecting all connection for this user.")
          }
        }
      })

      val apiService = new ApiService() {
        override val http = mock[HttpExt]

        override def addInQueue(spokeQueue: Future[SourceQueue[Message]], message: Strict) = Future.failed(new Exception("Failed"))

        //override val apiConnector = actorRef
      }

      val result = (apiService.addMessageInSourceQueue(None, None))
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(
        """{
                                        "contentType":"rawtext",
                                        "groupId" :"0",
                                        "visibility" : "Public",
                                        "ttl":0,
                                        "instanceText":"first spok",
                                        "text":"text",
                                        "geo":{
                                        "latitude" : 13.67,
                                        "longitude" : 14.56,
                                        "elevation" : 33.34
                                        }
                                        }"""
      ))
      sub.ensureSubscription()
    }

    "return valid JSON as response from Spok server, if token is sent from server and mention user id list is empty" in {
      val response = """{"spokId":"randomId","mentionUserId":[]}"""

      val actorRef = TestActorRef(new Actor {
        def receive = {
          case ConnectService(actor, userId, phoneNumber, source, notificationSource, accountSource, messagingSource) => {
            sender ! response
          }
          case DisconnectService(phoneNumber) => {
            info(s" ${phoneNumber} is disconnected. So disconnecting all connection for this user.")
          }
        }
      })

      val apiService = new ApiService() {
        override val http = mock[HttpExt]

        override def addInQueue(spokeQueue: Future[SourceQueue[Message]], message: Strict) = Future.failed(new Exception("Failed"))

        //override val apiConnector = actorRef
      }

      val result = (apiService.addMessageInSourceQueue(Some("userId"), Some("userNumber")))
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(
        """{
                                        "action":"create spok",
                                        "contentType":"rawtext",
                                        "groupId" :"0",
                                        "visibility" : "Public",
                                        "ttl":0,
                                        "instanceText":"first spok",
                                        "text":"text",
                                        "geo":{
                                        "latitude" : 13.67,
                                        "longitude" : 14.56,
                                        "elevation" : 33.34
                                        }
                                        }"""
      ))
      sub.ensureSubscription()
    }
    "return Unauthorized if the Spok Stats path is hit with invalid Token" in {

      val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOi9IUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
      Get("/spok/1234/stats").withHeaders(rawHeader) ~> route ~> check {
        status shouldBe StatusCodes.Unauthorized
      }
    }

    "return ok if the Spok Stats path is hit " in {

      when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
      val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + spokPort + DELIMITER + SPOK + DELIMITER + "1234" + DELIMITER + STATS +
        "?userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376")
      when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
      val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
      Get("/spok/1234/stats").withHeaders(rawHeader) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "return ok if create spok path is hit " in {
      when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
      val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
      val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + spokPort + DELIMITER + "create" + DELIMITER + "spok" +
        "?userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376", method = HttpMethods.POST).withHeaders(rawHeader).withEntity("""{"hi":"hello"}""")
      when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentTypes.`application/json`, VALID_SPOK))))
      Post("/spoks").withHeaders(rawHeader).withEntity("""{"hi":"hello"}""") ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "return ok if create spok path is hit with invalid Token" in {
      when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
      val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOi9IUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
      val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + spokPort + DELIMITER + "create" + DELIMITER + "spok" +
        "?userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376", method = HttpMethods.POST).withHeaders(rawHeader).withEntity("""{"hi":"hello"}""")
      when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentTypes.`application/json`, VALID_SPOK))))
      Post("/spoks").withHeaders(rawHeader).withEntity("""{"hi":"hello"}""") ~> route ~> check {
        status shouldBe StatusCodes.Unauthorized
      }
    }

    "return ok if the Spok Stack path is hit " in {
      when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
      val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + spokPort + DELIMITER + SPOKS + DELIMITER + "1234" +
        "?userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376")
      when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
      val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
      Get("/spoks/1234").withHeaders(rawHeader) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "return Unauthorized if the Spok Stack path is hit with invalid Token" in {

      val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOi9IUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
      Get("/spoks/1234").withHeaders(rawHeader) ~> route ~> check {
        status shouldBe StatusCodes.Unauthorized
      }
    }

    "return ok if get scoped users of a spok path is hit " in {
      when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
      val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + spokPort + DELIMITER + SPOK + DELIMITER + "spokId" + DELIMITER + SCOPED + DELIMITER + "pos" +
        "?userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376")
      when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
      val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
      Get("/spok/spokId/scoped/pos").withHeaders(rawHeader) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "return Unauthorized if get scoped users of a spok path is hit with invalid Token" in {

      val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOi9IUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
      Get("/spok/spokId/scoped/pos").withHeaders(rawHeader) ~> route ~> check {
        status shouldBe StatusCodes.Unauthorized
      }
    }

    "return ok if get respokers of a spok path is hit " in {
      when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
      val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + spokPort + DELIMITER + SPOK + DELIMITER + "spokId" + DELIMITER + RESPOKERS + DELIMITER + "pos" +
        "?userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376")
      when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
      val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
      Get("/spok/spokId/respokers/pos").withHeaders(rawHeader) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "return Unauthorized if get respokers of a spok path is hit with invalid Token" in {

      val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOi9IUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
      Get("/spok/spokId/respokers/pos").withHeaders(rawHeader) ~> route ~> check {
        status shouldBe StatusCodes.Unauthorized
      }
    }

    "return ok if view short spok of a spok path is hit " in {
      when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
      val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + spokPort + DELIMITER + STACK + DELIMITER + "spokId" + DELIMITER + "targetUserId" +
        "?userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376")
      when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
      val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
      Get("/stack/spokId/targetUserId").withHeaders(rawHeader) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "return ok if view full spok of a spok path is hit without target user id " in {
      when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
      val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + spokPort + DELIMITER + SPOK + DELIMITER + "spokId" + DELIMITER + FULL +
        "?userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376")
      when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
      val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
      Get("/spok/spokId/full").withHeaders(rawHeader) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "return ok if view short spok of a spok path is hit without target user id " in {
      when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
      val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + spokPort + DELIMITER + STACK + DELIMITER + "spokId" +
        "?userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376")
      when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
      val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
      Get("/stack/spokId").withHeaders(rawHeader) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "return ok if the follow/unfollow path is hit" in {
      val apiService = new ApiService() {
        override val http = mock[HttpExt]
        override val dseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
      }
      when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
      val route = apiService.routes
      Get("/") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
        status shouldEqual StatusCodes.SwitchingProtocols
      }
    }

    "return ok if the upload media path is hit" in {
      val apiService = new ApiService() {
        override val http = mock[HttpExt]
        override val dseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
      }
      when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)

      val route = apiService.routes
      Get("/upload") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
        status shouldEqual StatusCodes.SwitchingProtocols
      }
    }

    "return ok if view short spok of a spok path is hit with invalid user" in {
      when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (false)
      val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + spokPort + DELIMITER + STACK + DELIMITER + "spokId" + DELIMITER + "targetUserId" +
        "?userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376")
      when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
      val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
      Get("/stack/spokId/targetUserId").withHeaders(rawHeader) ~> route ~> check {
        status shouldBe StatusCodes.Unauthorized
      }
    }

    "return Unauthorized if view short spok of a spok path is hit with invalid Token" in {

      val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOi9IUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
      Get("/stack/spokId/targetUserId").withHeaders(rawHeader) ~> route ~> check {
        status shouldBe StatusCodes.Unauthorized
      }
    }

    "return ok if view full spok of a spok path is hit " in {
      when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
      val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + spokPort + DELIMITER + SPOK + DELIMITER + "spokId" + DELIMITER + FULL + DELIMITER + "targetUserId" +
        "?userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376")
      when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
      val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
      Get("/spok/spokId/full/targetUserId").withHeaders(rawHeader) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "return Unauthorized if view full spok of a spok path is hit with invalid Token" in {

      val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOi9IUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
      Get("/spok/spokId/full/targetUserId").withHeaders(rawHeader) ~> route ~> check {
        status shouldBe StatusCodes.Unauthorized
      }
    }

    "return ok if url to view full user profile is hit " in {
      when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
      val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + accountPort + DELIMITER + USER_ROLE + DELIMITER + "userId" +
        "?userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376")
      when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
      val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
      Get("/user/userId").withHeaders(rawHeader) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "return Unauthorized if url to view full user profile is hit with invalid Token" in {

      val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOi9IUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
      Get("/user/userId").withHeaders(rawHeader) ~> route ~> check {
        status shouldBe StatusCodes.Unauthorized
      }
    }

    "return ok if url to view minimal user profile is hit " in {
      when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
      val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + accountPort + DELIMITER + USER_ROLE + DELIMITER + "userId" + DELIMITER + "minimal" +
        "?userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376")
      when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
      val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
      Get("/user/userId/minimal").withHeaders(rawHeader) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "return Unauthorized if url to view minimal user profile is hit with invalid Token" in {

      val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOi9IUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
      Get("/user/userId/minimal").withHeaders(rawHeader) ~> route ~> check {
        status shouldBe StatusCodes.Unauthorized
      }
    }

    "return ok if url to remove user from cache is hit " in {
      when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
      val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + accountPort + DELIMITER + "cache" + DELIMITER + "userId" +
        "?userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376")
      when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
      val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
      Get("/cache/userId").withHeaders(rawHeader) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "return Unauthorized if url to remove user from cache is hit with invalid Token" in {

      val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOi9IUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
      Get("/cache/userId").withHeaders(rawHeader) ~> route ~> check {
        status shouldBe StatusCodes.Unauthorized
      }
    }

    "return ok if url to list of followers of an user is hit " in {
      when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
      val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + accountPort + DELIMITER + PROFILE + DELIMITER + "userId" + DELIMITER + FOLLOWERS + DELIMITER + "pos" +
        "?userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376")
      when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
      val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
      Get("/profile/userId/followers/pos").withHeaders(rawHeader) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "return Unauthorized if url to list of followers of an user is hit with invalid Token" in {

      val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOi9IUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
      Get("/profile/userId/followers/pos").withHeaders(rawHeader) ~> route ~> check {
        status shouldBe StatusCodes.Unauthorized
      }
    }

    "return ok if url to list of followings of an user is hit " in {
      when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
      val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + accountPort + DELIMITER + PROFILE + DELIMITER + "userId" + DELIMITER + FOLLOWINGS + DELIMITER + "pos" +
        "?userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376")
      when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
      val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
      Get("/profile/userId/followings/pos").withHeaders(rawHeader) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "return Unauthorized if url to list of followings of an user is hit with invalid Token" in {

      val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOi9IUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
      Get("/profile/userId/followings/pos").withHeaders(rawHeader) ~> route ~> check {
        status shouldBe StatusCodes.Unauthorized
      }
    }

    "return ok if get comments of a spok path is hit " in {
      when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
      val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + spokPort + DELIMITER + SPOK + DELIMITER + "spokId" + DELIMITER + COMMENTS + DELIMITER + "pos" +
        "?userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376")
      when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
      val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
      Get("/spok/spokId/comments/pos").withHeaders(rawHeader) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "return Unauthorized if get comments of a spok path is hit with invalid Token" in {

      val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOi9IUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
      Get("/spok/spokId/comments/pos").withHeaders(rawHeader) ~> route ~> check {
        status shouldBe StatusCodes.Unauthorized
      }
    }

    "return ok if url to view spoker's wall is hit " in {
      when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
      val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + spokPort + DELIMITER + USER_ROLE + DELIMITER + "userId" + DELIMITER + WALL + DELIMITER + "pos" +
        "?userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376")
      when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
      val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
      Get("/user/userId/wall/pos").withHeaders(rawHeader) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "return ok if url to view wall's notifications is hit " in {
      when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
      val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + notificationPort + DELIMITER + "my" + DELIMITER + "allnotifications" + DELIMITER + "pos" +
        "?userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376")
      when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
      val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
      Get("/my/allnotifications/pos").withHeaders(rawHeader) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "return ok when receive notification route is hit" in {
      when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
      Get("/") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
        status shouldEqual StatusCodes.SwitchingProtocols
      }
    }

    "return ok if the authenticate route is hit" in {
      when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
      Get("/authenticate") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
        status shouldEqual StatusCodes.SwitchingProtocols
      }
    }

    "return ok if url to view a question of a poll is hit " in {
      when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
      val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + spokPort + DELIMITER + "poll" + DELIMITER + "questionId" +
        "?userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376")
      when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
      val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
      Get("/poll/questionId").withHeaders(rawHeader) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "return Unauthorized if url to view a question of a poll is hit with invalid Token" in {

      val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOi9IUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
      Get("/poll/questionId").withHeaders(rawHeader) ~> route ~> check {
        status shouldBe StatusCodes.Unauthorized
      }
    }

    "return ok if url to view my details is hit " in {
      when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
      val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + accountPort + DELIMITER + MY + DELIMITER + DETAILS +
        "?userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376")
      when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
      val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
      Get("/my/details").withHeaders(rawHeader) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "return ok if url to fetch group is hit " in {
      when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
      val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + accountPort + DELIMITER + "all" + GROUPS + DELIMITER + "1" +
        "?userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376")
      when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
      val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
      Get("/groups/1").withHeaders(rawHeader) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "return ok if url to fetch details of user is hit " in {
      when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
      val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + accountPort + DELIMITER + ADMIN + DELIMITER + USER_ROLE + DELIMITER + "1" +
        "?userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376")
      when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
      val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
      Get("/admin/user/1").withHeaders(rawHeader) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "return ok if url to search by nickname is hit " in {
    when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
    val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + searchPort + DELIMITER + SEARCH + DELIMITER + AUTONICK + "?nickname=" + "cyril" +
      "&userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376")
    when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
    val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
    Get("/search/autonick?nickname=cyril").withHeaders(rawHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  "return ok if url to search by hashtag is hit " in {
    when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
    val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + searchPort + DELIMITER + SEARCH + DELIMITER + AUTOHASH + "?hashtag=" + "awe" +
      "&userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376")
    when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
    val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
    Get("/search/autohash?hashtag=awe").withHeaders(rawHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  "return ok if url to search last spoks is hit " in {
    when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
    val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + searchPort + DELIMITER + SEARCH + DELIMITER + LAST + DELIMITER + "1" +
      "?userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376")
    when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
    val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
    Get("/search/last/1").withHeaders(rawHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  "return ok if url to search last spoks of my friend is hit " in {
    when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
    val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + searchPort + DELIMITER + SEARCH + DELIMITER + FRIENDS + DELIMITER + "1" +
      "?userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376")
    when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
    val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
    Get("/search/friends/1").withHeaders(rawHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  "return ok if url to search popular spoker is hit " in {
    when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
    val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + searchPort + DELIMITER + SEARCH + DELIMITER + POPULAR + DELIMITER + "1" +
      "?userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376")
    when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
    val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
    Get("/search/popular/1").withHeaders(rawHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  "return ok if url to search trendy spoks is hit " in {
    when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
    val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + searchPort + DELIMITER + SEARCH + DELIMITER + TRENDY + DELIMITER + "1" +
      "?userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376")
    when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
    val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
    Get("/search/trendy/1").withHeaders(rawHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  "return ok if url to view spoker's wall is hit " in {
    when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
    val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + accountPort + DELIMITER + "group" + DELIMITER + "userId" + DELIMITER + WALL + DELIMITER + "pos" +
      "?userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376")
    when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
    val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
    Get("/user/userId/wall/pos").withHeaders(rawHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  "return ok if url to launch search is hit " in {
    when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
    val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + searchPort + DELIMITER + SEARCH + DELIMITER +
      "1?userids=cyrilid,kaisid,sonuid&hashtags=abc,xyz,lmn&latitude=11.22&longitude=22.13&start=123&end=456" +
      "&content_types=picture&userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376")
    when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
    val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
    Get("/search/1?userids=cyrilid,kaisid,sonuid&hashtags=abc,xyz,lmn&latitude=11.22&longitude=22.13&start=123&end=456&content_types=picture").withHeaders(rawHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  "return Unauthorized if url to launch search is hit with invalid Token" in {

    val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOi9IUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
    Get("/search/1?userIds=cyrilid,kaisid,sonuid&hashtags=abc,xyz,lmn&latitude=11.22&longitude=22.13&start=start&end=end&content_types=picture").withHeaders(rawHeader) ~> route ~> check {
      status shouldBe StatusCodes.Unauthorized
    }
  }

  "return proper error response when get groups is hit on websocket" in {
    Get("/groups") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
      status shouldEqual StatusCodes.SwitchingProtocols
    }
  }

  "return proper error response when get spok stats is hit on websocket" in {
    Get("/spok/123456/stats") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
      status shouldEqual StatusCodes.SwitchingProtocols
    }
  }

  "return proper error response when get spok stacks is hit on websocket" in {
    Get("/spoks/123456") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
      status shouldEqual StatusCodes.SwitchingProtocols
    }
  }

  "return proper error response when get scoped users is hit on websocket" in {
    Get("/spok/123456/scoped/1") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
      status shouldEqual StatusCodes.SwitchingProtocols
    }
  }

  "return proper error response when get respokers is hit on websocket" in {
    Get("/spok/123456/respokers/1") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
      status shouldEqual StatusCodes.SwitchingProtocols
    }
  }

  "return proper error response when view short spok without target user id is hit on websocket" in {
    Get("/stack/123456") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
      status shouldEqual StatusCodes.SwitchingProtocols
    }
  }

  "return proper error response when view short spok with target user id is hit on websocket" in {
    Get("/stack/123456/987654") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
      status shouldEqual StatusCodes.SwitchingProtocols
    }
  }

  "return proper error response when get spok comments is hit on websocket" in {
    Get("/spok/123456/comments/1") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
      status shouldEqual StatusCodes.SwitchingProtocols
    }
  }

  "return proper error response when view full spok without target userId is hit on websocket" in {
    Get("/spok/123456/full") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
      status shouldEqual StatusCodes.SwitchingProtocols
    }
  }

  "return proper error response when view full spok with target userId is hit on websocket" in {
    Get("/spok/123456/full/987654") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
      status shouldEqual StatusCodes.SwitchingProtocols
    }
  }

  "return proper error response when view minimal user details is hit on websocket" in {
    Get("/user/123456/minimal") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
      status shouldEqual StatusCodes.SwitchingProtocols
    }
  }

  "return proper error response when view full user details is hit on websocket" in {
    Get("/user/123456") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
      status shouldEqual StatusCodes.SwitchingProtocols
    }
  }

  "return proper error response when remove user from cache is hit on websocket" in {
    Get("/cache/123456") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
      status shouldEqual StatusCodes.SwitchingProtocols
    }
  }

  "return proper error response when view spokers wall is hit on websocket" in {
    Get("/user/123456/wall/1") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
      status shouldEqual StatusCodes.SwitchingProtocols
    }
  }

  "return proper error response when view wall's notification is hit on websocket" in {
    Get("/my/allnotifications/1") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
      status shouldEqual StatusCodes.SwitchingProtocols
    }
  }

  "return proper error response when get followers is hit on websocket" in {
    Get("/profile/123456/followers/1") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
      status shouldEqual StatusCodes.SwitchingProtocols
    }
  }

  "return proper error response when get followings is hit on websocket" in {
    Get("/profile/123456/followings/1") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
      status shouldEqual StatusCodes.SwitchingProtocols
    }
  }

  "return proper error response when view poll question is hit on websocket" in {
    Get("/poll/123") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
      status shouldEqual StatusCodes.SwitchingProtocols
    }
  }

  "return proper error response when get my details is hit on websocket" in {
    Get("/my/details") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
      status shouldEqual StatusCodes.SwitchingProtocols
    }
  }

  "return proper error response when search by nickname is hit on websocket" in {
    Get("/search/autonick?nickname=rob") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
      status shouldEqual StatusCodes.SwitchingProtocols
    }
  }

  "return proper error response when search by hashtags is hit on websocket" in {
    Get("/search/autohash?hashtag=rob") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
      status shouldEqual StatusCodes.SwitchingProtocols
    }
  }

  "return proper error response when get popular spokers is hit on websocket" in {
    Get("/search/popular/123456") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
      status shouldEqual StatusCodes.SwitchingProtocols
    }
  }

  "return proper error response when search last spok is hit on websocket" in {
    Get("/search/last/123456") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
      status shouldEqual StatusCodes.SwitchingProtocols
    }
  }

  "return proper error response when search friend's spok is hit on websocket" in {
    Get("/search/friends/123456") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
      status shouldEqual StatusCodes.SwitchingProtocols
    }
  }

  "return proper error response when search trendy spok is hit on websocket" in {
    Get("/search/trendy/123456") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
      status shouldEqual StatusCodes.SwitchingProtocols
    }
  }

  "return proper error response when get specific group detail without pos is hit on websocket" in {
    Get("/group/123456") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
      status shouldEqual StatusCodes.SwitchingProtocols
    }
  }

  "return proper error response when get specific group detail with pos is hit on websocket" in {
    Get("/group/123456/1") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
      status shouldEqual StatusCodes.SwitchingProtocols
    }
  }

  "return proper error response when launch full search is hit on websocket" in {
    Get("/search/1?userids=1234&hashtags=rob&latitude=45&longitude=45&start=1&end=3&content_types=text") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
      status shouldEqual StatusCodes.SwitchingProtocols
    }
  }

  "return error response when websocket route is hit on a get call with access token" in {
    val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
    Get("/").withHeaders(rawHeader) ~> route ~> check {
      status shouldBe StatusCodes.NotFound
    }
  }

  "return error response when websocket route is hit on a get call without access token" in {
    Get("/") ~> route ~> check {
      status shouldBe StatusCodes.NotFound
    }
  }

  "return valid JSON as response from Notification server , if remove notification is sent from server" in {
    val response = """{"resource":"ws://api.spok.me","status":"success","errors":[],"data":"Notification Removed Successfully"}"""

    val actorRef = TestActorRef(new Actor {
      def receive = {
        case ConnectService(actor, userId, phoneNumber, source, notificationSource, accountSource, messagingSource) => {
          sender ! response
        }
        case DisconnectService(phoneNumber) => {
          info(s" ${phoneNumber} is disconnected. So disconnecting all connection for this user.")
        }
      }
    })

    val apiService = new ApiService() {
      override val http = mock[HttpExt]

      override def addInQueue(spokeQueue: Future[SourceQueue[Message]], message: Strict) = Future.failed(new Exception("Failed"))

      //override val apiConnector = actorRef
    }

    val result = (apiService.addMessageInSourceQueue(Some("userId"), Some("userNumber")))
    val (pub, sub) = TestSource.probe[Message]
      .via(result)
      .toMat(TestSink.probe[Message])(Keep.both)
      .run()
    sub.request(1)
    pub.sendNext(TextMessage.Strict(
      """{
            "action":"removeNotification",
            "notificationId":"123456789"

        }"""
    ))
    pub.sendNext(TextMessage.Strict(
      """{
            "action1":"removeNotification",
            "notificationId":"123456789"

        }"""
    ))
    pub.sendNext(TextMessage.Strict(
      """{
            "action":"removeNotification1",
            "notificationId":"123456789"

        }"""
    ))
    sub.ensureSubscription()
  }

  "return valid JSON as response from Accounts server , if update profile is sent from server" in {
    val response = """{"resource":"ws://localhost:8080","status":"success","errors":[],"data":"\"User profile updated successfully\""}"""

    val actorRef = TestActorRef(new Actor {
      def receive = {
        case ConnectService(actor, userId, phoneNumber, source, notificationSource, accountSource, messagingSource) => {
          sender ! response
        }
        case DisconnectService(phoneNumber) => {
          info(s" ${phoneNumber} is disconnected. So disconnecting all connection for this user.")
        }
      }
    })

    val apiService = new ApiService() {
      override val http = mock[HttpExt]

      override def addInQueue(spokeQueue: Future[SourceQueue[Message]], message: Strict) = Future.failed(new Exception("Failed"))

      //override val apiConnector = actorRef
    }

    val result = (apiService.addMessageInSourceQueue(Some("userId"), Some("userNumber")))
    val (pub, sub) = TestSource.probe[Message]
      .via(result)
      .toMat(TestSink.probe[Message])(Keep.both)
      .run()
    sub.request(1)
    pub.sendNext(TextMessage.Strict(
      """{"action":"updateUserProfile","nickName":"updatenickname","birthDate":"1992-05-06","gender":"male","picture":"updatepicture","cover":"updatecover","geo"{"latitude":60.0,"longitude":60.0,"elevation":60.0}}"""
    ))
    sub.ensureSubscription()
  }

  "return valid JSON as response from Accounts server , if update profile with s3 url is sent from server" in {
    val response = """{"resource":"ws://localhost:8080","status":"success","errors":[],"data":"\"User profile updated successfully\""}"""

    val actorRef = TestActorRef(new Actor {
      def receive = {
        case ConnectService(actor, userId, phoneNumber, source, notificationSource, accountSource, messagingSource) => {
          sender ! response
        }
        case DisconnectService(phoneNumber) => {
          info(s" ${phoneNumber} is disconnected. So disconnecting all connection for this user.")
        }
      }
    })

    val apiService = new ApiService() {
      override val http = mock[HttpExt]

      override def addInQueue(spokeQueue: Future[SourceQueue[Message]], message: Strict) = Future.failed(new Exception("Failed"))

      //override val apiConnector = actorRef
    }

  }

  "return ok to update user profile route is hit " in {

    when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
    val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
    val req: HttpRequest = HttpRequest(method = HttpMethods.PUT, uri = HTTP + interface + ":" + accountPort + DELIMITER + "my" + DELIMITER + "profile" +
      "?userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376").withHeaders(rawHeader).withEntity("""{"nickname":"updatenickname","birthDate":"1992-05-06","gender":"male","geo":{"latitude":60.0,"longitude":60.0,"elevation":60.0}}""")
    when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
    Put("/my/profile").withHeaders(rawHeader).withEntity("""{"nickname":"updatenickname","birthDate":"1992-05-06","gender":"male","geo":{"latitude":60.0,"longitude":60.0,"elevation":60.0}}""") ~> route ~> check {
      status shouldBe StatusCodes.OK

    }
  }

  "return proper error response when update user profile route hit on websocket" in {
    Get("/my/profile") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
      status shouldEqual StatusCodes.SwitchingProtocols
    }
  }

  "return ok to initiate talk route is hit " in {

    when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
    val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
    val req: HttpRequest = HttpRequest(method = HttpMethods.POST, uri = HTTP + interface + ":" + messagingPort + DELIMITER + TALK + DELIMITER + "a26c57d5-a9d9-4b23-b47d-866fa1071c7c" +
      "?userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376").withHeaders(rawHeader).withEntity("""{"message":"first message" }""")
    when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
    Post("/talk/a26c57d5-a9d9-4b23-b47d-866fa1071c7c").withHeaders(rawHeader).withEntity("""{"message":"first message" }""") ~> route ~> check {
      status shouldBe StatusCodes.OK

    }
  }

  "return proper error response when initiate talk route hit on websocket" in {
    Get("/talk") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
      status shouldEqual StatusCodes.SwitchingProtocols
    }
  }

  "return proper error response when send message route hit on websocket" in {
    Get("/talk/baa7c387-905d-47dd-be5b-05b58aed5549") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
      status shouldEqual StatusCodes.SwitchingProtocols
    }
  }

  "return ok if view all talks route is hit without position" in {
    when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
    val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + messagingPort + DELIMITER + TALKS +
      "?userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376")
    when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
    val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
    Get("/talks").withHeaders(rawHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  "return ok if view all talks route is hit with position" in {
    when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
    val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + messagingPort + DELIMITER + TALKS + DELIMITER + "2" +
      "?userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376")
    when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
    val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
    Get("/talks/2").withHeaders(rawHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  "return proper error response when get all talks without pos is hit on websocket" in {
    Get("/talks") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
      status shouldEqual StatusCodes.SwitchingProtocols
    }
  }

  "return proper error response when get all talks with pos is hit on websocket" in {
    Get("/talks/2") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
      status shouldEqual StatusCodes.SwitchingProtocols
    }
  }

  "return ok if view all messages of a talk route is hit with position" in {
    when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
    val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + messagingPort + DELIMITER +
      TALK + DELIMITER + "1234" + DELIMITER + "e44f-4590-8e82-8bf0c974991e" + "?order=" + "desc" +
      "&userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376")
    when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
    val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
    Get("/talk/1234/e44f-4590-8e82-8bf0c974991e?order=desc").withHeaders(rawHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  "return ok if view all messages of a talk route is hit without position" in {
    when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
    val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + messagingPort + DELIMITER +
      TALK + DELIMITER + "1234" + "?order=" + "desc" +
      "&userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376")
    when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
    val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
    Get("/talk/1234?order=desc").withHeaders(rawHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  "return proper error response when view all messages of a talk without pos is hit on websocket" in {
    Get("/talk/1234") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
      status shouldEqual StatusCodes.SwitchingProtocols
    }
  }

  "return proper error response when view all messages of a talk with pos is hit on websocket" in {
    Get("/talk/1234/2") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
      status shouldEqual StatusCodes.SwitchingProtocols
    }
  }

  "return response from Messaging server, if remove all talks request is sent from API" in {

    val apiService = new ApiService() {
      override val http = mock[HttpExt]
      override def addInQueue(spokeQueue: Future[SourceQueue[Message]], message: Strict) = Future.failed(new Exception("Failed"))
    }

    val result = apiService.addMessageInSourceQueue(Some("userId"), Some("userNumber"))
    val (pub, sub) = TestSource.probe[Message]
      .via(result)
      .toMat(TestSink.probe[Message])(Keep.both)
      .run()
    sub.request(1)
    pub.sendNext(TextMessage.Strict(
      """{"action":"removeTalk","talkId":"123456"}"""
    ))
    sub.ensureSubscription()
  }

  "return response from Messaging server, if remove a message of a talk request is sent from API" in {

    val apiService = new ApiService() {
      override val http = mock[HttpExt]
      override def addInQueue(spokeQueue: Future[SourceQueue[Message]], message: Strict) = Future.failed(new Exception("Failed"))
    }

    val result = apiService.addMessageInSourceQueue(Some("userId"), Some("userNumber"))
    val (pub, sub) = TestSource.probe[Message]
      .via(result)
      .toMat(TestSink.probe[Message])(Keep.both)
      .run()
    sub.request(1)
    pub.sendNext(TextMessage.Strict(
      """{"action":"removeMessage","messageId":"23456","talkId":"123456"}"""
    ))
    sub.ensureSubscription()
  }

  "return ok if view all poll stats is hit" in {
    when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
    val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + spokPort + DELIMITER +
      SPOK + DELIMITER + "1234" + DELIMITER + POLL_RESULTS +
      "?userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376")
    when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
    val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
    Get("/spok/1234/pollResults").withHeaders(rawHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  "return proper error response when view poll stats is hit on websocket" in {
    Get("/spok/1234/pollResults") ~> Upgrade(List(UpgradeProtocol("websocket"))) ~> emulateHttpCore ~> route ~> check {
      status shouldEqual StatusCodes.SwitchingProtocols
    }
  }

  "return ok if url to search message is hit " in {
    when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
    val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + messagingPort + DELIMITER + SEARCHTALKS + DELIMITER + SEARCHMSG + "?msg=" + "message" +
      "&userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376")
    when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
    val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
    Get("/searchtalks/searchmsg?msg=message").withHeaders(rawHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  "return ok if url to search talkers is hit " in {
    when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
    val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + messagingPort + DELIMITER + SEARCHTALKS + DELIMITER + SEARCHTALKER + "?talkers=" + "piy" +
      "&userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376")
    when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
    val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
    Get("/searchtalks/searchtalker?talkers=piy").withHeaders(rawHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  "return ok if get my spok path is hit " in {
    when(apiService.dseGraphPersistenceFactoryApi.isValidUserId("5ad25ab8-e44f-4590-8e82-8bf0c974991e")) thenReturn (true)
    val req: HttpRequest = HttpRequest(uri = HTTP + interface + ":" + spokPort + DELIMITER + MY + DELIMITER + SPOKS + DELIMITER + "1" +
      "?userId=" + "5ad25ab8-e44f-4590-8e82-8bf0c974991e" + "&phone_number=" + "+33660760376")
    when(apiService.http.singleRequest(req)) thenReturn (Future(HttpResponse(StatusCodes.OK)))
    val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
    Get("/my/spoks/1").withHeaders(rawHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  "return Unauthorized if get my spok path is hit with invalid Token" in {

    val rawHeader = RawHeader("Authorization", "Bearer eyJhbGciOi9IUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")
    Get("/my/spoks/1").withHeaders(rawHeader) ~> route ~> check {
      status shouldBe StatusCodes.Unauthorized
    }
  }

  private def emulateHttpCore(req: HttpRequest): HttpRequest =
    req.header[Upgrade] match {
      case Some(upgrade) if upgrade.hasWebSocket => req.copy(headers = req.headers :+ handleWebSocketMessgae("access_token_"))
      case _ => req
    }

  private def handleWebSocketMessgae(protocol: String) = {

    new CustomHeader() with UpgradeToWebSocket {
      override def requestedProtocols = List(protocol + "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo")

      override def name = "dummy"

      override def value = "dummy"

      override def renderInRequests = true

      override def renderInResponses = true

      override def handleMessages(handlerFlow: Graph[FlowShape[Message, Message], Any], subprotocol: Option[String]): HttpResponse =
        HttpResponse(StatusCodes.SwitchingProtocols)
    }

  }
}