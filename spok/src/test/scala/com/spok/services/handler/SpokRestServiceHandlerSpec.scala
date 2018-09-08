package com.spok.services.handler

import java.io.File
import java.util.{ Date, UUID }

import akka.actor.{ Actor, ActorSystem }
import akka.http.scaladsl.model.ws.{ Message, TextMessage }
import akka.http.scaladsl.model.{ ContentTypes, StatusCodes }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Flow, Keep, Source }
import akka.stream.testkit.scaladsl.{ TestSink, TestSource }
import akka.testkit.TestActorRef
import com.rbmhtechnology.eventuate.ReplicationEndpoint
import com.rbmhtechnology.eventuate.ReplicationEndpoint._
import com.rbmhtechnology.eventuate.log.leveldb.LeveldbEventLog
import com.spok.model.Account.UserMinimalDetailsResponse
import com.spok.model.SpokModel.{ PollQuestions, _ }
import com.spok.persistence.dse.DseGraphFactory
import com.spok.persistence.factory.DSEUserSpokFactoryApi
import com.spok.persistence.redis.RedisFactory
import com.spok.services.service.SpokActorFailureReplies._
import com.spok.services.service.SpokActorSuccessReplies._
import com.spok.services.service.SpokManagerCommands._
import com.spok.services.service.SpokViewCommands._
import com.spok.services.service.SpokViewReplies._
import com.spok.services.service.SpokViewValidationCommands._
import com.spok.services.service.SpokViewValidationReplies._
import com.spok.services.service._
import com.spok.services.util.SpokValidationUtil
import com.spok.util.Constant._
import com.spok.util.{ FileUploadUtility, JsonHelper }
import org.joda.time.DateTime
import org.mockito.Mockito.when
import org.scalatest.WordSpec
import org.scalatest.mock.MockitoSugar

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ Await, Future }

class SpokRestServiceHandlerSpec extends WordSpec with SpokRestServiceHandler with MockitoSugar with JsonHelper {
  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  val endpoint = ReplicationEndpoint(id => LeveldbEventLog.props(logId = "SpokManagerHandlerSpec"))(system)
  val eventLog = endpoint.logs(DefaultLogName)
  val id = "randomId"
  override val dseUserSpokFactoryApi: DSEUserSpokFactoryApi = mock[DSEUserSpokFactoryApi]
  override val redisFactory: RedisFactory = mock[RedisFactory]
  override val spokLogger: SpokLogger = mock[SpokLogger]
  override val fileUploadUtility: FileUploadUtility = mock[FileUploadUtility]

  val spokValidationUtil = mock[SpokValidationUtil]

  val textMessage = TextMessage(Source.single(id))

  val textInput =
    """{
        "contentType":"rawtext",
        "groupId" :"0",
        "visibility" : "public",
        "ttl":0,
        "headerText":"instance_text",
        "text":"text",
          "geo":{
             "latitude" : 13.67,
             "longitude" : 14.56,
             "elevation" : 33.34
             }
        }"""

  val mediaInput =
    """{
        "contentType":"picture",
        "groupId" :"0",
        "visibility" : "public",
        "ttl":0,
        "headerText":"instance_text",
        "file":"image.jpg",
          "geo":{
             "latitude" : 13.67,
             "longitude" : 14.56,
             "elevation" : 33.34
             }
        }"""

  val urlInput =
    """{
        "contentType":"url",
        "groupId" :"0",
        "visibility" : "public",
        "ttl":0,
        "headerText":"instance_text",
        "url":{
              "address":"https://www.google.com",
              "title": "url_title",
              "text" : "url_text",
              "preview": "url_preview",
              "type": "url_type"
              },
        "geo":{
              "latitude" : 13.67,
              "longitude" : 14.56,
              "elevation" : 33.34
             }
        }"""

  "Spok Rest service handler" should {

    "store diffusion settings for text message" in {
      val userId = UUID.randomUUID().toString
      val command = TestActorRef(new Actor {
        def receive = {
          case Create(spok, userId) ⇒ {
            sender ! SpokCreateSuccess(spok)
          }
        }
      })

      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ {
            sender() ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
          }
        }
      })

      val phoneNumber = "9999912345"
      val result = Await.result(storeSpokSettings(query, command, userId, textInput, None), 40 seconds)
      assert(result.status equals (StatusCodes.OK))
      assert(result.entity.contentType equals (ContentTypes.`application/json`))
    }

    " not able to store diffusion settings for text message when user suspended" in {
      val userId = UUID.randomUUID().toString
      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ {
            sender() ! IsUserSuspendedAsk(SPOKER_SUSPENDED)
          }
        }
      })

      val phoneNumber = "9999912345"
      val result = Await.result(storeSpokSettings(query, query, userId, textInput, None), 40 seconds)
      assert(result.status equals (StatusCodes.BadRequest))
      assert(result.entity.contentType equals (ContentTypes.`application/json`))
    }

    " not able to store diffusion settings for text message when user suspend property not found" in {
      val userId = UUID.randomUUID().toString
      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ {
            sender() ! IsUserSuspendedAsk(SPOKER_SUSPENDED)
          }
        }
      })

      val phoneNumber = "9999912345"
      val result = Await.result(storeSpokSettings(query, query, userId, textInput, None), 40 seconds)
      assert(result.status equals (StatusCodes.BadRequest))
      assert(result.entity.contentType equals (ContentTypes.`application/json`))
    }

    "store diffusion settings for media type spok" in {
      val userId = UUID.randomUUID().toString
      val command = TestActorRef(new Actor {
        def receive = {
          case Create(spok, userId) ⇒ {
            sender ! SpokCreateSuccess(spok)
          }
        }
      })

      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ {
            sender() ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
          }
        }
      })

      val file = new File("/README.md")
      val phoneNumber = "9999912345"
      when(fileUploadUtility.mediaUpload(file)).thenReturn((Some("url/filename.jpg"), None))
      val result = Await.result(storeSpokSettings(query, command, userId, mediaInput, Some(file)), 15 second)
      assert(result.status equals (StatusCodes.OK))
      assert(result.entity.contentType equals (ContentTypes.`application/json`))
    }

    "send invalid json message if json sent to create a spok is incorrect" in {

      val userId = getUUID()
      val textInput =
        """{
        "action":"Action",
        "groupId" :"0",
        "visibility" : "public",
        "ttl":0,
        "headerText":"instance_text",
        "text":"text",
          "geo":{
             "latitude" : 123.67,
             "longitude" : 142.56,
             "elevation" : 323.34
             }
        }"""
      val command = TestActorRef(new Actor {
        def receive = {
          case Create(spok: Spok, id) ⇒ {
            sender ! SpokCreateFailure("PRS-001", new Exception(INVALID_JSON))
          }
        }
      })
      val phoneNumber = "9999912345"
      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ {
            sender() ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
          }
        }
      })
      val result = Await.result(storeSpokSettings(query, command, userId, textInput, None), 40 seconds)
      assert(result.status equals (StatusCodes.BadRequest))
      assert(result.entity.contentType equals (ContentTypes.`application/json`))
    }

    "store diffusion settings for url message" in {
      val userId = UUID.randomUUID().toString

      val command = TestActorRef(new Actor {
        def receive = {
          case Create(spok, userId) ⇒ {
            sender ! SpokCreateSuccess(spok)
          }
        }
      })

      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ {
            sender() ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
          }
        }
      })
      val result = Await.result(storeSpokSettings(query, command, userId, urlInput, None), 40 seconds)
      assert(result.status equals (StatusCodes.OK))
      assert(result.entity.contentType equals (ContentTypes.`application/json`))
    }

    "send error message, if it is failed to store diffusion settings" in {
      val actorRef = TestActorRef(new Actor {
        def receive = {
          case Create(spok: Spok, "5ad25ab8-e44f-4590-8e82-8bf0c974991e") ⇒ {
            sender ! SpokCreateFailure("SPK-106", (new Exception("Test Error")))
          }
        }
      })

      val phoneNumber = "9999912345"

      val result = detectRequestAndPerform(actorRef, actorRef, "5ad25ab8-e44f-4590-8e82-8bf0c974991e", phoneNumber)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()

      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectEvent
    }

    "create spok of text type" in {
      val input =
        """{
        "contentType":"rawtext",
        "groupId" :"0",
        "visibility" : "public",
        "ttl":0,
        "headerText":"instance_text",
        "text":"text",
        "geo":{
             "latitude" : 13.67,
             "longitude" : 14.56,
             "elevation" : 33.34
             }
        }"""
      val result = super.createSpok(input, None)
      val geo = Geo(13.67, 14.56, 33.34)
      val spokId = result._1 match {
        case Some(user) => user.spokId
      }
      val launched = result._1 match {
        case Some(user) => user.launched
      }
      val output = Some(Spok("rawtext", Some("0"), Some("public"), Some(0), Some("instance_text"), None, Some("text"), None, None, None, geo, spokId, launched))
      assert(result._1 == output)
    }

    "create spok of text type with special characters" in {
      val input =
        """{
        "contentType":"rawtext",
        "groupId" :"0",
        "visibility" : "public",
        "ttl":0,
        "headerText":"N@#e",
        "text":"$pok w1th \"$pec!@l\" characters",
        "geo":{
             "latitude" : 13.67,
             "longitude" : 14.56,
             "elevation" : 33.34
             }
        }"""
      val result = super.createSpok(input, None)
      val geo = Geo(13.67, 14.56, 33.34)
      val spokId = result._1 match {
        case Some(user) => user.spokId
      }
      val launched = result._1 match {
        case Some(user) => user.launched
      }
      val output = Some(Spok("rawtext", Some("0"), Some("public"), Some(0), Some("N@#e"), None, Some("""$pok w1th "$pec!@l" characters"""), None, None, None, geo, spokId, launched))
      assert(result._1 == output)
    }

    "create spok of URL type" in {
      val input =
        """{
          "contentType":"url",
          "groupId" :"0",
          "visibility" : "public",
          "ttl":0,
          "headerText":"instance_text",
          "url":{
               "address":"https://www.google.com",
               "title": "url_title",
               "text" : "url_text",
               "preview": "url_preview",
               "urlType": "text"
               },
          "geo":{
              "latitude" : 13.67,
              "longitude" : 14.56,
              "elevation" : 33.34
              }
          }
        """
      val result = super.createSpok(input, None)
      val url = Url("https://www.google.com", "url_title", "url_text", "url_preview", Some("text"))
      val geo = Geo(13.67, 14.56, 33.34)
      val spokId = result._1 match {
        case Some(user) => user.spokId
      }
      val launched = result._1 match {
        case Some(user) => user.launched
      }
      val output = Some(Spok("url", Some("0"), Some("public"), Some(0), Some("instance_text"), None, None, Some(url), None, None, geo, spokId, launched))
      assert(result._1 == output)
    }

    "Able to create a poll type spok" in {
      val textInput =
        """
          {
          "action":"create spok",
          "contentType":"poll",
          "groupId" :"0",
          "visibility" : "public",
          "ttl":0,
          "headerText":"url spok",
          "poll":{
          "title":"MyPoll",
          "desc":"Check Knowledge",
          "questions":[{
              "text":"How many planets are there in the Universe?",
              "contentType":"text",
              "preview":"preview",
              "rank":1,
              "answers":[{
                  "text":"Seven",
                  "contentType":"text",
                  "preview":"preview",
                  "rank":1
                  },
                  {
                  "text":"Eight",
                  "contentType":"text",
                  "preview":"preview",
                  "rank":2
                  },
                  {
                  "text":"Nine",
                  "contentType":"text",
                  "preview":"preview",
                  "rank":3
                  }
                ]
              }
             ]
          },
          "geo":{
              "latitude" : 13.67,
              "longitude" : 14.56,
              "elevation" : 33.34
              }
          }
        """.stripMargin

      val result = super.createSpok(textInput, None)
      val geo = Geo(13.67, 14.56, 33.34)
      val spokId = result._1 match {
        case Some(user) => user.spokId
      }
      val launched = result._1 match {
        case Some(user) => user.launched
      }

      val poll = Poll("MyPoll", Some("Check Knowledge"), List(PollQuestions("How many planets are there in the Universe?", Some("text"), Some("preview"), 1, List(PollAnswers("Seven", Some("text"), Some("preview"), 1), PollAnswers("Eight", Some("text"), Some("preview"), 2), PollAnswers("Nine", Some("text"), Some("preview"), 3)))))
      val output = Some(Spok("poll", Some("0"), Some("public"), Some(0), Some("url spok"), None, None, None, Some(poll), None, geo, spokId, launched))
      assert(result._1 == output)

    }

    "not Able to create a poll type spok" in {
      val textInput =
        """
          {
          "action":"create spok",
          "contentType":"poll",
          "groupId" :"0",
          "visibility" : "public",
          "ttl":0,
          "headerText":"url spok",
          "poll":{
          "title":"MyPoll",
          "desc":"Check Knowledge",
          "questions":[{
              "text":"",
              "contentType":"text",
              "preview":"preview",
              "rank":1,
              "answers":[{
                  "text":"Seven",
                  "contentType":"text",
                  "preview":"preview",
                  "rank":1
                  },
                  {
                  "text":"Eight",
                  "contentType":"text",
                  "preview":"preview",
                  "rank":2
                  },
                  {
                  "text":"Nine",
                  "contentType":"text",
                  "preview":"preview",
                  "rank":3
                  }
                ]
              }
             ]
          },
          "geo":{
              "latitude" : 13.67,
              "longitude" : 14.56,
              "elevation" : 33.34
              }
          }
        """.stripMargin

      val result = super.createSpok(textInput, None)
      val geo = Geo(13.67, 14.56, 33.34)
      val spokId = result._1 match {
        case Some(user) => user.spokId
      }
      val launched = result._1 match {
        case Some(user) => user.launched
      }

      val poll = Poll("MyPoll", Some("Check Knowledge"), List(PollQuestions("", Some("text"), Some("preview"), 1, List(PollAnswers("Seven", Some("text"), Some("preview"), 1), PollAnswers("Eight", Some("text"), Some("preview"), 2), PollAnswers("Nine", Some("text"), Some("preview"), 3)))))
      val output = Some(Spok("poll", Some("0"), Some("public"), Some(0), Some("url spok"), None, None, None, Some(poll), None, geo, spokId, launched))
      assert(result._1 == output)

    }

    "hit greeter method" in {
      val result = greeter
      assert(result.isInstanceOf[Flow[Message, Message, Any]])
    }

    "not create spok of text type if geo latitude is invalid" in {
      val input =
        """{
        "contentType":"rawtext",
        "groupId" :"0",
        "visibility" : "public",
        "ttl":0,
        "headerText":"instance_text",
        "text":"text",
        "geo":{
             "latitude" : 92.67,
             "longitude" : 34.33,
             "elevation" : 33.34
             }
        }"""
      val result = super.createSpok(input, None)
      assert(result._1.isEmpty)
    }

    "not create spok of text type if geo longitude is invalid" in {
      val input =
        """{
        "contentType":"rawtext",
        "groupId" :"0",
        "visibility" : "public",
        "ttl":0,
        "headerText":"instance_text",
        "text":"text",
        "geo":{
             "latitude" : 35.55,
             "longitude" : 181.55,
             "elevation" : 33.34
             }
        }"""
      val result = super.createSpok(input, None)
      assert(result._1.isEmpty)
    }

    "not create spok of text type if geo elevation is invalid" in {
      val input =
        """{
        "contentType":"rawtext",
        "groupId" :"0",
        "visibility" : "public",
        "ttl":0,
        "headerText":"instance_text",
        "text":"text",
        "geo":{
             "latitude" : 31.67,
             "longitude" : 34.33,
             "elevation" : 93.34
             }
        }"""
      val result = super.createSpok(input, None)
      assert(result._1.isEmpty)
    }

    "not create spok of text type if content type is invalid" in {
      val input =
        """{
        "contentType":"xyz",
        "groupId" :"0",
        "visibility" : "public",
        "ttl":0,
        "headerText":"instance_text",
        "text":"text",
        "geo":{
             "latitude" : 33.67,
             "longitude" : 45.56,
             "elevation" : 33.34
             }
        }"""
      val result = super.createSpok(input, None)
      assert(result._1.isEmpty)
    }

    "not create spok of URL type if URL format is wrong" in {
      val input =
        """{
          "contentType":"url",
          "groupId" :"0",
          "visibility" : "public",
          "ttl":0,
          "headerText":"instance_text",
          "url":{
               "address":"htt://www.google.com",
               "title": "url_title",
               "text" : "url_text",
               "preview": "url_preview",
               "urlType": "text"
               },
          "geo":{
              "latitude" : 13.67,
              "longitude" : 14.56,
              "elevation" : 33.34
              }
          }
        """
      val result = super.createSpok(input, None)
      assert(result._1.isEmpty)
    }

    "create spok of URL type even if the content type is not sent" in {
      val input =
        """{
          "contentType":"url",
          "groupId" :"0",
          "visibility" : "public",
          "ttl":0,
          "headerText":"instance_text",
          "url":{
               "address":"https://www.google.com",
               "title": "url_title",
               "text" : "url_text",
               "preview": "url_preview"
               },
          "geo":{
              "latitude" : 13.67,
              "longitude" : 14.56,
              "elevation" : 33.34
              }
          }
        """
      val result = super.createSpok(input, None)
      val url = Url("https://www.google.com", "url_title", "url_text", "url_preview", None)
      val geo = Geo(13.67, 14.56, 33.34)
      val spokId = result._1 match {
        case Some(user) => user.spokId
      }
      val launched = result._1 match {
        case Some(user) => user.launched
      }
      val output = Some(Spok("url", Some("0"), Some("public"), Some(0), Some("instance_text"), None, None, Some(url), None, None, geo, spokId, launched))
      assert(result._1 == output)
    }

    "not create spok of URL type if the content type is wrong" in {
      val input =
        """{
          "contentType":"url",
          "groupId" :"0",
          "visibility" : "public",
          "ttl":0,
          "headerText":"instance_text",
          "url":{
               "address":"https://www.google.com",
               "title": "url_title",
               "text" : "url_text",
               "preview": "url_preview",
               "urlType" : "xyz"
               },
          "geo":{
              "latitude" : 13.67,
              "longitude" : 14.56,
              "elevation" : 33.34
              }
          }
        """
      val result = super.createSpok(input, None)
      assert(result._1.isEmpty)
    }

    "perform respok when the respok details are properly validated" in {

      val userId = getUUID()
      val spokId = getUUID()
      val respokDetails =
        s"""
          {
          "action":"respok",
          "spokId":"$spokId",
          "groupId":"0",
          "visibility":"public"
          "text":"Hello"
          "geo":{
              "latitude" : 13.67,
              "longitude" : 14.56,
              "elevation" : 33.34
              }
          }
        """
      val respokResponse: RespokInterimResponse = RespokInterimResponse(spokId, RespokStatistics(50, 50, 50, 1000), List(), List())
      val command = TestActorRef(new Actor {
        def receive = {
          case CreateRespok(respok: Respok, spokId, userId, None) ⇒ {
            sender ! RespokCreateSuccess(respokResponse, spokId)
          }
        }
      })

      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidSpokAndSendStatus(userId, spokId) =>
            sender() ! IsValidSpokAck(PENDING, true)

          case IsUserSuspended(spokId) ⇒
            sender() ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)

        }
      })
      val expectedOutput = s"""{"resource":"respok","status":"success","errors":[],"data":{"respokResponse":{"spokId":"$spokId","counters":{"numberOfRespoked":50,"numberOfLanded":50,"numberOfComment":50,"travelled":1000.0},"mentionUserId":[]}}}"""
      val result = detectRequestAndPerform(command, query, userId, "9999999900")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(respokDetails))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send JSON ERROR if the JSON structure is wrong for respok" in {

      val userId = getUUID()
      val spokId = getUUID()
      val respokDetails =
        s"""
          {
          "action":"respok",
          "spokId":"$spokId",
          "groupId":"0",
          "visibility":"public"
          "text":"Hello"
          }
        """
      val command = TestActorRef(new Actor {
        def receive = {
          case INVALID_JSON ⇒ {
            sender ! INVALID_JSON
          }
        }
      })

      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ {
            sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
          }
        }
      })

      val expectedOutput = """{"resource":"respok","status":"failed","errors":[{"id":"PRS-001","message":"Invalid JSON"}],"data":{}}"""
      val result = detectRequestAndPerform(command, query, userId, "9999999900")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(respokDetails))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send JSON ERROR if the spokId field is not present in the json sent for respok" in {

      val userId = getUUID()
      val spokId = getUUID()
      val respokDetails =
        s"""
          {
          "action":"respok",
          "groupId":"0",
          "visibility":"public"
          "text":"Hello"
          "geo":{
              "latitude" : 13.67,
              "longitude" : 14.56,
              "elevation" : 33.34
              }
          }
        """
      val command = TestActorRef(new Actor {
        def receive = {
          case INVALID_JSON ⇒ {
            sender ! INVALID_JSON
          }
        }
      })

      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ {
            sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
          }
        }
      })

      val expectedOutput = """{"resource":"respok","status":"failed","errors":[{"id":"PRS-001","message":"Invalid JSON"}],"data":{}}"""
      val result = detectRequestAndPerform(command, query, userId, "9999999900")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(respokDetails))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send respok failure message if the group targeted for respok is not found" in {

      val userId = getUUID()
      val spokId = getUUID()
      val groupId = getUUID()
      val respokDetails =
        s"""
          {
          "action":"respok",
          "spokId":"$spokId",
          "groupId":"0",
          "visibility":"public"
          "text":"Hello"
          "geo":{
              "latitude" : 13.67,
              "longitude" : 14.56,
              "elevation" : 33.34
              }
          }
        """
      val command = TestActorRef(new Actor {
        def receive = {
          case CreateRespok(respok: Respok, spokId, userId, None) ⇒ {
            sender ! RespokCreateFailure(GRP_001, new Exception(s"Group $groupId not found."))
          }
        }
      })

      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidSpokAndSendStatus(userId, spokId) =>
            sender() ! IsValidSpokAck(PENDING, true)

          case IsUserSuspended(spokId) ⇒
            sender() ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
        }
      })
      val expectedOutput = s"""{"resource":"respok","status":"failed","errors":[{"id":"GRP-001","message":"Group $groupId not found."}],"data":{}}"""
      val result = detectRequestAndPerform(command, query, userId, "9999999900")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(respokDetails))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "not perform respok when the respok details are invalid" in {

      val userId = getUUID()
      val spokId = getUUID()
      val respokDetails =
        s"""
          {
          "action":"respok",
          "spokId":"$spokId",
          "groupId":"0",
          "visibility":"public"
          "text":"Hello"
          "geo":{
              "latitude" : 13.67,
              "longitude" : 1444.56,
              "elevation" : 33.34
              },
              "launched":13.55
          }
      """
      val validRespok: InterimRespok = InterimRespok(Some("0"), Some("public"), Some("Hello"), Geo(13.44, 1644.44, 65.44), None)
      when(spokValidationUtil.validateRespokDetails(validRespok)).thenReturn((false, Some(List(Error("GEO-002", INVALID_LONGITUDE)))))
      val command = TestActorRef(new Actor {
        def receive = {
          case INVALID_LONGITUDE ⇒ {
            sender ! INVALID_LONGITUDE
          }
        }
      })

      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ {
            sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
          }
        }
      })

      val expectedOutput = """{"resource":"respok","status":"failed","errors":[{"id":"GEO-002","message":"Invalid longitude"}],"data":{}}"""
      val result = detectRequestAndPerform(command, query, userId, "9999009999")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(respokDetails))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "not perform respok when the spok is already respoked and return respoked message" in {

      val userId = getUUID()
      val spokId = getUUID()
      val respokDetails =
        s"""
          {
          "action":"respok",
          "spokId":"$spokId",
          "groupId":"0",
          "visibility":"public"
          "text":"Hello"
          "geo":{
              "latitude" : 13.67,
              "longitude" : 14.56,
              "elevation" : 33.34
              }
          }
        """
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidSpokAndSendStatus(userId, spokId) =>
            sender() ! IsValidSpokAck(RESPOKED, true)

          case IsUserSuspended(spokId) ⇒ sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
        }
      })
      val expectedOutput = s"""{"resource":"respok","status":"failed","errors":[{"id":"SPK-006","message":"Spok $spokId already respoked"}],"data":{}}"""
      val result = detectRequestAndPerform(query, query, "1234", "5ad25")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(respokDetails))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "not perform respok when the spok is already removed and return removed message" in {

      val spokId = getUUID()
      val respokDetails =
        s"""
          {
          "action":"respok",
          "spokId":"$spokId",
          "groupId":"0",
          "visibility":"public"
          "text":"Hello"
          "geo":{
              "latitude" : 13.67,
              "longitude" : 14.56,
              "elevation" : 33.34
              }
          }
        """
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidSpokAndSendStatus(userId, spokId) =>
            sender() ! IsValidSpokAck(REMOVED, true)

          case IsUserSuspended(spokId) ⇒ sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
        }
      })
      val expectedOutput = s"""{"resource":"respok","status":"failed","errors":[{"id":"SPK-001","message":"Spok $spokId has been removed"}],"data":{}}"""
      val result = detectRequestAndPerform(query, query, "1234", "5ad25")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(respokDetails))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "not perform respok when the spok is not found and return not found message" in {

      val spokId = getUUID()
      val userId = getUUID()
      val respokDetails =
        s"""
          {
          "action":"respok",
          "spokId":"$spokId",
          "groupId":"0",
          "visibility":"public"
          "text":"Hello"
          "geo":{
              "latitude" : 13.67,
              "longitude" : 14.56,
              "elevation" : 33.34
              }
          }
        """
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidSpokAndSendStatus(userId, spokId) =>
            sender() ! IsValidSpokAck(SPOK_NOT_FOUND, true)
          case IsUserSuspended(spokId) ⇒ sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
        }
      })
      val expectedOutput = s"""{"resource":"respok","status":"failed","errors":[{"id":"SPK-001","message":"Spok $spokId not found."}],"data":{}}"""
      val result = detectRequestAndPerform(query, query, "1234", "5ad25")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(respokDetails))
      sub.expectNext(TextMessage.Strict(expectedOutput))
    }

    "not perform respok when the spok has been disabled" in {

      val spokId = getUUID()
      val userId = getUUID()
      val respokDetails =
        s"""
          {"action":"respok",
          "spokId":"$spokId",
          "groupId":"0",
          "visibility":"public"
          "text":"Hello"
          "geo":{
              "latitude" : 13.67,
              "longitude" : 14.56,
              "elevation" : 33.34
              }
          }
        """
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidSpokAndSendStatus(userId, spokId) =>
            sender() ! IsValidSpokAck(PENDING, false)
          case IsUserSuspended(spokId) ⇒ sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
        }
      })
      val expectedOutput = """{"resource":"respok","status":"failed","errors":[{"id":"SPK-016","message":"spok is already disabled"}],"data":{}}"""
      val result = detectRequestAndPerform(query, query, userId, "9900990099")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(respokDetails))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "perform unspok when the unspok details are properly validated" in {
      val userId = getUUID()
      val spokId = getUUID()
      val unspokDetails =
        s"""
          {"action":"unspok",
          "spokId":"$spokId",
          "geo":{
              "latitude" : 13.67,
              "longitude" : 14.56,
              "elevation" : 33.34
              }
          }
        """
      val unspokResponse = UnspokResponse(spokId, RespokStatistics(50, 50, 50, 1000))
      val command = TestActorRef(new Actor {
        def receive = {
          case ExecuteUnspok(unspok: Unspok, spokId, userId, status) ⇒ {
            sender ! UnspokPerformSuccess(unspokResponse, spokId)
          }
        }
      })
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidSpokAndSendStatus(userId, spokId) =>
            sender() ! IsValidSpokAck(PENDING, true)
        }
      })
      val expectedOutput = s"""{"resource":"unspok","status":"success","errors":[],"data":{"unspokResponse":{"spokId":"$spokId","counters":{"numberOfRespoked":50,"numberOfLanded":50,"numberOfComment":50,"travelled":1000.0}}}}"""
      val result = detectRequestAndPerform(command, query, userId, "9988008899")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(unspokDetails))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send JSON ERROR if the JSON structure is wrong for unspok" in {
      val userId = getUUID()
      val spokId = getUUID()
      val unspokDetails =
        s"""
          {"action":"unspok","spokId":"$spokId",
          "groupId":"0",
          "visibility":"public"
          "text":"Hello"
          }
        """
      val query = TestActorRef(new Actor {
        def receive = {
          case INVALID_JSON => INVALID_JSON
        }
      })
      val expectedOutput = """{"resource":"unspok","status":"failed","errors":[{"id":"PRS-001","message":"Invalid JSON"}],"data":{}}"""
      val result = detectRequestAndPerform(query, query, userId, "9999990033")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(unspokDetails))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send JSON ERROR if spokId field is not present in the json sent for unspok" in {

      val userId = getUUID()
      val spokId = getUUID()
      val unspokDetails =
        s"""
          {"action":"unspok",
          "geo":{
              "latitude" : 13.67,
              "longitude" : 14.56,
              "elevation" : 33.34
              }
          }
        """
      val query = TestActorRef(new Actor {
        def receive = {
          case INVALID_JSON => INVALID_JSON
        }
      })
      val expectedOutput = """{"resource":"unspok","status":"failed","errors":[{"id":"PRS-001","message":"Invalid JSON"}],"data":{}}"""
      val result = detectRequestAndPerform(query, query, userId, "9999990033")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(unspokDetails))
      sub.expectNext(TextMessage(expectedOutput))
    }
    "not perform unspok when the unspok details are invalid" in {
      val userId = getUUID()
      val spokId = getUUID()
      val unspokDetails =
        """{"action":"unspok","spokId":"1234",
          "geo":{
              "latitude" : 13.44,
              "longitude" : 1644.44,
              "elevation" : 65.44
              }
              }
        """
      val query = TestActorRef(new Actor {
        def receive = {
          case INVALID_LONGITUDE => INVALID_LONGITUDE
        }
      })
      val expectedOutput = """{"resource":"unspok","status":"failed","errors":[{"id":"GEO-002","message":"Invalid longitude"}],"data":{}}"""
      val result = detectRequestAndPerform(query, query, userId, "9900443322")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(unspokDetails))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "not perform unspok when the spok is already respoked and return respoked message" in {
      val userId = getUUID()
      val spokId = getUUID()
      val unspokDetails =
        s"""
           {"action":"unspok","spokId":"$spokId",
          "geo":{
              "latitude" : 13.67,
              "longitude" : 14.56,
              "elevation" : 33.34
              }
              }
        """
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidSpokAndSendStatus(userId, spokId) =>
            sender() ! IsValidSpokAck(RESPOKED, true)
        }
      })
      val expectedOutput = s"""{"resource":"unspok","status":"failed","errors":[{"id":"SPK-006","message":"Spok $spokId already respoked"}],"data":{}}"""
      val result = detectRequestAndPerform(query, query, userId, "9900998800")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(unspokDetails))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "not perform unspok when the spok is not found and return not found message" in {
      val userId = getUUID()
      val spokId = getUUID()
      val unspokDetails =
        s"""
          {"action":"unspok","spokId":"$spokId",
          "geo":{
              "latitude" : 13.67,
              "longitude" : 14.56,
              "elevation" : 33.34
              }
          }
        """
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidSpokAndSendStatus(userId, spokId) =>
            sender() ! IsValidSpokAck(SPOK_NOT_FOUND, true)
        }
      })
      val expectedOutput = s"""{"resource":"unspok","status":"failed","errors":[{"id":"SPK-001","message":"Spok $spokId not found."}],"data":{}}"""
      val result = detectRequestAndPerform(query, query, userId, "9988770099")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(unspokDetails))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "not perform unspok when the spok has been disabled" in {
      val userId = getUUID()
      val spokId = getUUID()
      val unspokDetails =
        s"""
          {"action":"unspok","spokId":"$spokId",
          "geo":{
              "latitude" : 13.67,
              "longitude" : 14.56,
              "elevation" : 33.34
              }
          }
        """
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidSpokAndSendStatus(userId, spokId) =>
            sender() ! IsValidSpokAck(PENDING, false)
        }
      })
      val expectedOutput = """{"resource":"unspok","status":"failed","errors":[{"id":"SPK-016","message":"spok is already disabled"}],"data":{}}"""
      val result = detectRequestAndPerform(query, query, userId, "9900998899")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(unspokDetails))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "not perform unspok when the spok is removed and return removed message" in {
      val userId = getUUID()
      val spokId = getUUID()
      val unspokDetails =
        s"""
           {"action":"unspok","spokId":"$spokId",
          "geo":{
              "latitude" : 13.67,
              "longitude" : 14.56,
              "elevation" : 33.34
              }
              }
        """
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidSpokAndSendStatus(userId, spokId) =>
            sender() ! IsValidSpokAck(REMOVED, true)
        }
      })
      val expectedOutput = s"""{"resource":"unspok","status":"failed","errors":[{"id":"SPK-001","message":"Spok $spokId has been removed"}],"data":{}}"""
      val result = detectRequestAndPerform(query, query, userId, "9900998800")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(unspokDetails))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "return error message if the unspoking of a spok fails due to generic error" in {

      val userId = getUUID()
      val spokId = getUUID()
      val status = PENDING
      val unspokDetails =
        s"""
          {"action":"unspok",
          "spokId":"$spokId",
          "geo":{
              "latitude" : 13.67,
              "longitude" : 14.56,
              "elevation" : 33.34
              }
          }
        """
      val command = TestActorRef(new Actor {
        def receive = {
          case ExecuteUnspok(unspok: Unspok, spokId, userId, status) ⇒ {
            sender ! UnspokPerformFailure(SPK_118, new Exception(s"Unable un-spoking spok $spokId (generic error)."))
          }
        }
      })
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidSpokAndSendStatus(userId, spokId) =>
            sender() ! IsValidSpokAck(PENDING, true)
        }
      })
      val expectedOutput = s"""{"resource":"unspok","status":"failed","errors":[{"id":"SPK-118","message":"Unable un-spoking spok $spokId (generic error)."}],"data":{}}"""
      val result = detectRequestAndPerform(command, query, userId, "9988008899")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(unspokDetails))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send success message, if it is success to add comment" in {

      val commentId = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val commenterUserId = UUID.randomUUID().toString
      val absoluteSpokId = UUID.randomUUID().toString
      val textInput = """{"action":"addComment","spokId":"""" + spokId + """","text":"text","geo":{"latitude" : 13.67,"longitude" : 14.56,"elevation" : 33.34}}"""

      val text = "text"
      val geo = Geo(13.67, 14.56, 33.3)
      val comment = Comment(commentId, text, geo, Nil)
      val response = s"""{"spok":{"spokId":"$spokId","nbRespoked":"2","nbLanded":"4","nbComments":"3","travelled":"6"},"user":{"id":"$commenterUserId","nickName":"10","gender":"20","picture":"30"}}"""
      val command = TestActorRef(new Actor {
        def receive = {
          case CreateComment(comment, spokInstanceId, commenterUserId) ⇒ {
            sender ! AddCommentSuccess(Some(parse(response).extract[SpokCommentResponse]))
          }
        }
      })

      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
        }
      })

      val expectedOutput = s"""{"resource":"addComment","status":"success","errors":[],"data":{"addCommentResponse":{"spok":{"spokId":"$spokId","nbRespoked":"2","nbLanded":"4","nbComments":"3","travelled":"6"},"user":{"id":"$commenterUserId","nickName":"10","gender":"20","picture":"30"}}}}"""
      when(redisFactory.storeSubscriber(absoluteSpokId, commenterUserId)) thenReturn (Future(1l))
      val result = detectRequestAndPerform(command, query, commenterUserId, commenterUserId)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage.Strict(expectedOutput))
    }

    "send fail message, if it is fail to add comment" in {

      val commentId = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val commenterUserId = UUID.randomUUID().toString
      val textInput = """{"action":"addComment","spokId":"""" + spokId + """","text":"text","geo":{"latitude" : 13.67,"longitude" : 14.56,"elevation" : 33.34}}"""

      val text = "text"
      val geo = Geo(13.67, 14.56, 33.3)
      val comment = Comment(commentId, text, geo, Nil)
      val response = new Exception(UNABLE_COMMENTING_SPOK)
      val actorRef = TestActorRef(new Actor {
        def receive = {
          case CreateComment(comment, spokInstanceId, commenterUserId) ⇒ {
            sender ! AddCommentFailure(new Exception(UNABLE_COMMENTING_SPOK), SPK_119)
          }
        }
      })

      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
        }
      })

      val expectedOutput = """{"resource":"addComment","status":"failed","errors":[{"id":"SPK-119","message":"Unable commenting spok (generic error)"}],"data":{}}"""
      val result = detectRequestAndPerform(actorRef, query, commenterUserId, commenterUserId)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send failed message, if it is fail to add comment when user Suspended" in {

      val commentId = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val commenterUserId = UUID.randomUUID().toString
      val absoluteSpokId = UUID.randomUUID().toString
      val textInput = """{"action":"addComment","spokId":"""" + spokId + """","text":"text","geo":{"latitude" : 13.67,"longitude" : 14.56,"elevation" : 33.34}}"""

      val text = "text"
      val geo = Geo(13.67, 14.56, 33.3)
      val comment = Comment(commentId, text, geo, Nil)
      val response = s"""{"spok":{"spokId":"$spokId","nbRespoked":"2","nbLanded":"4","nbComments":"3","travelled":"6"},"user":{"id":"$commenterUserId","nickName":"10","gender":"20","picture":"30"}}"""
      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ sender ! IsUserSuspendedAsk(SPOKER_SUSPENDED)
        }
      })
      val expectedOutput = """{"resource":"addComment","status":"failed","errors":[{"id":"SPK-014","message":"Action impossible for a suspended account"}],"data":{}}"""
      val result = detectRequestAndPerform(query, query, commenterUserId, commenterUserId)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage.Strict(expectedOutput))
    }

    "send failed message, if it is fail to add comment when generic error comes" in {

      val commentId = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val commenterUserId = UUID.randomUUID().toString
      val absoluteSpokId = UUID.randomUUID().toString
      val textInput = """{"action":"addComment","spokId":"""" + spokId + """","text":"text","geo":{"latitude" : 13.67,"longitude" : 14.56,"elevation" : 33.34}}"""

      val text = "text"
      val geo = Geo(13.67, 14.56, 33.3)
      val comment = Comment(commentId, text, geo, Nil)
      val response = s"""{"spok":{"spokId":"$spokId","nbRespoked":"2","nbLanded":"4","nbComments":"3","travelled":"6"},"user":{"id":"$commenterUserId","nickName":"10","gender":"20","picture":"30"}}"""
      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ sender ! IsUserSuspendedAsk(PROPERTY_NOT_FOUND)
        }
      })
      val expectedOutput = """{"resource":"addComment","status":"failed","errors":[{"id":"SPK-119","message":"Unable commenting spok (generic error)"}],"data":{}}"""
      val result = detectRequestAndPerform(query, query, commenterUserId, commenterUserId)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage.Strict(expectedOutput))
    }

    "send text is too short, if an user sends too short text to add comment" in {

      val commentId = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val commenterUserId = UUID.randomUUID().toString
      val textInput = """{"action":"addComment","spokId":"""" + spokId + """","text":"","geo":{"latitude" : 13.67,"longitude" : 14.56,"elevation" : 33.34}}"""

      val text = "text"
      val geo = Geo(13.67, 14.56, 33.3)
      val comment = Comment(commentId, text, geo, Nil)
      val response = """{"error":"Text is too short"}"""
      val actorRef = TestActorRef(new Actor {
        def receive = {
          case CreateComment(comment, spokInstanceId, commenterUserId) ⇒ {
            sender ! TEXT_SHORT_ERROR
          }
        }
      })

      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
        }
      })
      val expectedOutput = """{"resource":"addComment","status":"failed","errors":[{"id":"RGX-009","message":"Text is too short"}],"data":{}}"""
      val result = detectRequestAndPerform(actorRef, query, commenterUserId, commenterUserId)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send invalid Json, if an user sends invalid json to add comment" in {

      val commentId = UUID.randomUUID().toString
      val spokInstanceId = UUID.randomUUID().toString
      val commenterUserId = UUID.randomUUID().toString

      val textInput = """{"action":"addComment","instanceId":"""" + spokInstanceId + """","geo":{"latitude" : 13.67,"longitude" : 14.56,"elevation" : 33.34}}"""
      val text = "text"
      val geo = Geo(13.67, 14.56, 33.3)
      val comment = Comment(commentId, text, geo, Nil)
      val response = TextMessage(write(Map("error" -> INVALID_JSON)))
      val actorRef = TestActorRef(new Actor {
        def receive = {
          case CreateComment(comment, spokInstanceId, commenterUserId) ⇒ {
            sender ! TextMessage(write(Map("error" -> INVALID_JSON)))
          }
        }
      })

      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
        }
      })
      val expectedOutput = s"""{"resource":"addComment","status":"failed","errors":[{"id":"PRS-001","message":"Invalid JSON"}],"data":{}}"""
      val result = detectRequestAndPerform(actorRef, query, commenterUserId, commenterUserId)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send success message if the comment is updated successfully" in {

      val commentId = UUID.randomUUID().toString
      val spokInstanceId = UUID.randomUUID().toString
      val commenterUserId = UUID.randomUUID().toString
      val response: Some[CommentUpdateResponse] = Some(CommentUpdateResponse(spokInstanceId, "1", "1", "1", "0"))
      val textInput = """{"action":"updateComment","commentId":"""" + commentId + """","text":"updated text","geo":{"latitude" : 90.00,"longitude" : 14.56,"elevation" : 33.34}}"""

      val actorRef = TestActorRef(new Actor {
        def receive = {
          case UpdateComment(comment, userId) ⇒ {
            sender ! UpdateCommentSuccess(response)
          }
        }
      })

      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
        }
      })
      val expectedOutput = s"""{"resource":"updateComment","status":"success","errors":[],"data":{"updateCommentResponse":{"spokId":"$spokInstanceId","nbRespoked":"1","nbLanded":"1","nbComments":"1","travelled":"0"}}}"""
      val result = detectRequestAndPerform(actorRef, query, commenterUserId, commenterUserId)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage.Strict(expectedOutput))
    }

    "not allow to update a comment when user is suspended" in {

      val commentId = UUID.randomUUID().toString
      val spokInstanceId = UUID.randomUUID().toString
      val commenterUserId = UUID.randomUUID().toString
      val response: Some[CommentUpdateResponse] = Some(CommentUpdateResponse(spokInstanceId, "1", "1", "1", "0"))
      val textInput = """{"action":"updateComment","commentId":"""" + commentId + """","text":"updated text","geo":{"latitude" : 90.00,"longitude" : 14.56,"elevation" : 33.34}}"""
      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ sender ! IsUserSuspendedAsk(SPOKER_SUSPENDED)
        }
      })
      val expectedOutput = """{"resource":"updateComment","status":"failed","errors":[{"id":"SPK-014","message":"Action impossible for a suspended account"}],"data":{}}"""
      val result = detectRequestAndPerform(query, query, commenterUserId, commenterUserId)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage.Strict(expectedOutput))
    }

    "not allow to update a comment when generic error comes in  updating comment" in {

      val commentId = UUID.randomUUID().toString
      val spokInstanceId = UUID.randomUUID().toString
      val commenterUserId = UUID.randomUUID().toString
      val response: Some[CommentUpdateResponse] = Some(CommentUpdateResponse(spokInstanceId, "1", "1", "1", "0"))
      val textInput = """{"action":"updateComment","commentId":"""" + commentId + """","text":"updated text","geo":{"latitude" : 90.00,"longitude" : 14.56,"elevation" : 33.34}}"""
      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ sender ! IsUserSuspendedAsk(PROPERTY_NOT_FOUND)
        }
      })
      val expectedOutput = """{"resource":"updateComment","status":"failed","errors":[{"id":"SPK-120","message":"Unable updating comment (generic error)"}],"data":{}}"""
      val result = detectRequestAndPerform(query, query, commenterUserId, commenterUserId)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage.Strict(expectedOutput))
    }

    "send error message if the comment validation gives an error when the user tries to update a comment" in {

      val commentId = UUID.randomUUID().toString
      val commenterUserId = UUID.randomUUID().toString
      val response = """{"error":"Invalid Latitude"}"""
      val actorRef = TestActorRef(new Actor {
        def receive = {
          case _ => sender ! INVALID_LATITUDE
        }
      })

      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
        }
      })
      val textInput = """{"action":"updateComment","commentId":"""" + commentId + """","text":"updated text","geo":{"latitude" : 291.00,"longitude" : 14.56,"elevation" : 33.34}}"""
      val expecteOutput = s"""{"resource":"updateComment","status":"failed","errors":[{"id":"GEO-001","message":"Invalid Latitude"}],"data":{}}"""
      val result = detectRequestAndPerform(actorRef, query, commenterUserId, commenterUserId)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage(expecteOutput))
    }

    "send error message if the json to update the comment is invalid" in {

      val commentId = UUID.randomUUID().toString
      val commenterUserId = UUID.randomUUID().toString
      val response = """{"error":"Invalid JSON"}"""
      val textInput = """{"action":"updateComment","commentId":"""" + commentId + """","text":"updated text","geo":{"longitude" : 14.56,"elevation" : 33.34}}"""
      val expectedOutput = s"""{"resource":"updateComment","status":"failed","errors":[{"id":"PRS-001","message":"Invalid JSON"}],"data":{}}"""
      val actorRef = TestActorRef(new Actor {
        def receive = {
          case _ => sender ! INVALID_JSON
        }
      })

      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
        }
      })
      val result = detectRequestAndPerform(actorRef, query, commenterUserId, commenterUserId)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send json error message if the comment id is not in the json to update the comment" in {

      val commentId = UUID.randomUUID().toString
      val commenterUserId = UUID.randomUUID().toString
      val response = """{"error":"Invalid JSON"}"""
      val textInput = """{"action":"updateComment","text":"updated text","geo":{"latitude":"50.9","longitude" : 14.56,"elevation" : 33.34}}"""
      val expectedOutput = s"""{"resource":"updateComment","status":"failed","errors":[{"id":"PRS-001","message":"Invalid JSON"}],"data":{}}"""
      val actorRef = TestActorRef(new Actor {
        def receive = {
          case _ => sender ! INVALID_JSON
        }
      })

      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ {
            sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
          }
        }
      })
      val result = detectRequestAndPerform(actorRef, query, commenterUserId, commenterUserId)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send error message if the comment to be updated is not found" in {

      val commentId = UUID.randomUUID().toString
      val commenterUserId = UUID.randomUUID().toString
      val response = new Exception(s"Comment $commentId not found")
      val textInput = """{"action":"updateComment","commentId":"""" + commentId + """","text":"updated text","geo":{"latitude" : 90.00,"longitude" : 14.56,"elevation" : 33.34}}"""

      val actorRef = TestActorRef(new Actor {
        def receive = {
          case UpdateComment(comment, userId) ⇒ {
            sender ! UpdateCommentFailure(response, SPK_008)
          }
        }
      })

      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ {
            sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
          }
        }
      })
      val expectedOutput = s"""{"resource":"updateComment","status":"failed","errors":[{"id":"SPK-008","message":"Comment $commentId not found"}],"data":{}}"""
      val result = detectRequestAndPerform(actorRef, query, commenterUserId, commenterUserId)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send error message if the comment fails to get updated" in {

      val commentId = UUID.randomUUID().toString
      val commenterUserId = UUID.randomUUID().toString
      val response = new Exception(s"Unable updating comment $commentId(generic error).")
      val textInput = """{"action":"updateComment","commentId":"""" + commentId + """","text":"updated text","geo":{"latitude" : 90.00,"longitude" : 14.56,"elevation" : 33.34}}"""

      val actorRef = TestActorRef(new Actor {
        def receive = {
          case UpdateComment(comment, userId) ⇒ {
            sender ! UpdateCommentFailure(response, SPK_120)
          }
        }
      })

      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ {
            sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
          }
        }
      })
      val expectedOutput = s"""{"resource":"updateComment","status":"failed","errors":[{"id":"SPK-120","message":"Unable updating comment $commentId(generic error)."}],"data":{}}"""
      val result = detectRequestAndPerform(actorRef, query, commenterUserId, commenterUserId)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send success message if the comment is removed successfully" in {

      val commentId = UUID.randomUUID().toString
      val spokInstanceId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val textInput = """{"action":"removeComment","commentId":"""" + commentId + """","text":"updated text","geo":{"latitude" : 90.00,"longitude" : 14.56,"elevation" : 33.34}}"""

      val commentInternalSpokResponse = CommentInternalSpokResponse(spokInstanceId, "1", "1", "1", "0")
      val response = Some(RemoveCommentResponse(commentId, commentInternalSpokResponse))
      val actorRef = TestActorRef(new Actor {
        def receive = {
          case RemoveComment(commentId, userId, geo) ⇒ {
            sender ! RemoveCommentSuccess(response)
          }
        }
      })

      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ {
            sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
          }
        }
      })
      val expectedOutput = s"""{"resource":"removeComment","status":"success","errors":[],"data":{"removeCommentResponse":{"commentId":"$commentId","spok":{"spokId":"$spokInstanceId","nbRespoked":"1","nbLanded":"1","nbComments":"1","travelled":"0"}}}}"""
      val result = detectRequestAndPerform(actorRef, query, userId, userId)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage.Strict(expectedOutput))
    }

    "send failed message if user is suspended and going to removeComment" in {

      val commentId = UUID.randomUUID().toString
      val spokInstanceId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val textInput = """{"action":"removeComment","commentId":"""" + commentId + """","text":"updated text","geo":{"latitude" : 90.00,"longitude" : 14.56,"elevation" : 33.34}}"""

      val commentInternalSpokResponse = CommentInternalSpokResponse(spokInstanceId, "1", "1", "1", "0")
      val response = Some(RemoveCommentResponse(commentId, commentInternalSpokResponse))
      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ {
            sender ! IsUserSuspendedAsk(SPOKER_SUSPENDED)
          }
        }
      })

      val expectedOutput = """{"resource":"removeComment","status":"failed","errors":[{"id":"SPK-014","message":"Action impossible for a suspended account"}],"data":{}}"""

      val result = detectRequestAndPerform(query, query, userId, userId)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage.Strict(expectedOutput))
    }

    "send failed message if generic error comes in  removeComment" in {

      val commentId = UUID.randomUUID().toString
      val spokInstanceId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val textInput = """{"action":"removeComment","commentId":"""" + commentId + """","text":"updated text","geo":{"latitude" : 90.00,"longitude" : 14.56,"elevation" : 33.34}}"""

      val commentInternalSpokResponse = CommentInternalSpokResponse(spokInstanceId, "1", "1", "1", "0")
      val response = Some(RemoveCommentResponse(commentId, commentInternalSpokResponse))
      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ {
            sender ! IsUserSuspendedAsk(PROPERTY_NOT_FOUND)
          }
        }
      })

      val expectedOutput = """{"resource":"removeComment","status":"failed","errors":[{"id":"SPK-121","message":"Unable removing comment (generic error)"}],"data":{}}"""
      val result = detectRequestAndPerform(query, query, userId, userId)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage.Strict(expectedOutput))
    }

    "send error messsage if the geo validation fails while removing comment" in {

      val commentId = UUID.randomUUID().toString
      val spokInstanceId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val textInput = s"""{"action":"removeComment","commentId":"$commentId","text":"updated text","geo":{"latitude" :900.00,"longitude" : 214.56,"elevation" : 33.34}}"""
      val actorRef = TestActorRef(new Actor {
        def receive = {
          case RemoveComment(commentId, userId, geo) ⇒ {
            sender ! JSONERROR
          }
        }
      })

      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ {
            sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
          }
        }
      })
      val expectedOutput = s"""{"resource":"removeComment","status":"failed","errors":[{"id":"GEO-001","message":"Invalid Latitude"},{"id":"GEO-002","message":"Invalid longitude"}],"data":{}}"""
      val result = detectRequestAndPerform(actorRef, query, userId, userId)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage.Strict(expectedOutput))
    }

    "send json error message if the comment id is not in the json when trying to remove the comment" in {

      val commentId = UUID.randomUUID().toString
      val spokInstanceId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val textInput = """{"action":"removeComment","text":"updated text","geo":{"latitude" : 90.00,"longitude" : 14.56,"elevation" : 33.34}}"""

      val commentInternalSpokResponse = CommentInternalSpokResponse(spokInstanceId, "1", "1", "1", "0")
      val response = Some(RemoveCommentResponse(commentId, commentInternalSpokResponse))
      val actorRef = TestActorRef(new Actor {
        def receive = {
          case RemoveComment(commentId, userId, geo) ⇒ {
            sender ! RemoveCommentSuccess(response)
          }
        }
      })

      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ {
            sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
          }
        }
      })
      val expectedOutput = s"""{"resource":"removeComment","status":"failed","errors":[{"id":"PRS-001","message":"Invalid JSON"}],"data":{}}"""
      val result = detectRequestAndPerform(actorRef, query, userId, userId)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage.Strict(expectedOutput))
    }

    "send error message if the comment to be removed is not found" in {

      val commentId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val response = new Exception(s"Comment $commentId not found")
      val textInput = """{"action":"removeComment","commentId":"""" + commentId + """","text":"updated text","geo":{"latitude" : 90.00,"longitude" : 14.56,"elevation" : 33.34}}"""

      val actorRef = TestActorRef(new Actor {
        def receive = {
          case RemoveComment(commentId, userId, geo) ⇒ {
            sender ! RemoveCommentFailure(response, SPK_008)
          }
        }
      })

      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ {
            sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
          }
        }
      })
      val expectedOutput = s"""{"resource":"removeComment","status":"failed","errors":[{"id":"SPK-008","message":"Comment $commentId not found"}],"data":{}}"""
      val result = detectRequestAndPerform(actorRef, query, userId, userId)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send error message if the comment fails to get removed" in {

      val commentId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val response = new Exception(s"Unable removing comment $commentId(generic error).")
      val textInput = """{"action":"removeComment","commentId":"""" + commentId + """","text":"updated text","geo":{"latitude" : 90.00,"longitude" : 14.56,"elevation" : 33.34}}"""

      val actorRef = TestActorRef(new Actor {
        def receive = {
          case RemoveComment(commentId, userId, geo) ⇒ {
            sender ! RemoveCommentFailure(response, SPK_121)
          }
        }
      })

      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ {
            sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
          }
        }
      })
      val expectedOutput = s"""{"resource":"removeComment","status":"failed","errors":[{"id":"SPK-121","message":"Unable removing comment $commentId(generic error)."}],"data":{}}"""
      val result = detectRequestAndPerform(actorRef, query, userId, userId)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "not subscribe/unsubscribe a spok's feed when spok is not valid" in {
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val textInput = s"""{"action":"subscribe","spokId":"$spokId","geo":{"latitude" : 90.00,"longitude" : 14.56,"elevation" : 33.34}}"""
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidSpokWithEnabledFlag(spokId) ⇒ {
            sender() ! IsValidSpokWithEnabledAck(false)
          }
          case IsUserSuspended(spokId) ⇒ sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
        }
      })
      val expectedOutput = s"""{"resource":"subscribe","status":"failed","errors":[{"id":"SPK-001","message":"Spok $spokId not found."}],"data":{}}"""
      val result = detectRequestAndPerform(query, query, userId, "12345")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "not subscribe/unsubscribe a spok's feed when user is suspended" in {
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val textInput = s"""{"action":"subscribe","spokId":"$spokId","geo":{"latitude" : 90.00,"longitude" : 14.56,"elevation" : 33.34}}"""
      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ sender ! IsUserSuspendedAsk(SPOKER_SUSPENDED)
        }
      })

      val expectedOutput = """{"resource":"subscribe","status":"failed","errors":[{"id":"SPK-014","message":"Action impossible for a suspended account"}],"data":{}}"""
      val result = detectRequestAndPerform(query, query, userId, "12345")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "not subscribe/unsubscribe a spok's feed when suspend property of user not found" in {
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val textInput = s"""{"action":"subscribe","spokId":"$spokId","geo":{"latitude" : 90.00,"longitude" : 14.56,"elevation" : 33.34}}"""
      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ sender ! IsUserSuspendedAsk(PROPERTY_NOT_FOUND)
        }
      })
      val expectedOutput = """{"resource":"subscribe","status":"failed","errors":[{"id":"SPK-123","message":"Unable susbcribing to /unsubscribing from spok feed (generic error)."}],"data":{}}"""
      val result = detectRequestAndPerform(query, query, userId, "12345")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "be disable a spok" in {
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val textInput = s"""{"action":"disable","spokId":"$spokId","geo":{"latitude" : 90.00,"longitude" : 14.56,"elevation" : 33.34}}"""
      val launchedTime = System.currentTimeMillis()
      val command = TestActorRef(new Actor {
        def receive = {
          case Disable(spokId, userId, launchedTime, geo) ⇒ {
            sender() ! DisableSpokSuccess(SPOK_DISABLED)
          }
        }
      })

      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ {
            sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
          }
        }
      })
      val expectedOutput = """{"resource":"disable","status":"success","errors":[],"data":{"spokDisableResponse":"Spok has been disabled"}}"""
      val result = detectRequestAndPerform(command, query, userId, "12345")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage(expectedOutput))
    }

    " not allow to disable spok when user is suspended" in {
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val textInput = s"""{"action":"disable","spokId":"$spokId","geo":{"latitude" : 90.00,"longitude" : 14.56,"elevation" : 33.34}}"""
      val launchedTime = System.currentTimeMillis()
      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ {
            sender ! IsUserSuspendedAsk(SPOKER_SUSPENDED)
          }
        }
      })
      val expectedOutput = """{"resource":"disable","status":"failed","errors":[{"id":"SPK-014","message":"Action impossible for a suspended account"}],"data":{}}"""
      val result = detectRequestAndPerform(query, query, userId, "12345")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage(expectedOutput))
    }

    " not be allow to disable spok when suspended property not found" in {
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val textInput = s"""{"action":"disable","spokId":"$spokId","geo":{"latitude" : 90.00,"longitude" : 14.56,"elevation" : 33.34}}"""
      val launchedTime = System.currentTimeMillis()

      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ {
            sender ! IsUserSuspendedAsk(PROPERTY_NOT_FOUND)
          }
        }
      })
      val expectedOutput = """{"resource":"disable","status":"failed","errors":[{"id":"SPK-115","message":"Unable disabling spok (generic error)"}],"data":{}}"""
      val result = detectRequestAndPerform(query, query, userId, "12345")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send Json error if spokId is not mentioned in the json sent for disable spok" in {
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val textInput = s"""{"action":"disable","spokId1":"$spokId","geo":{"latitude" : 90.00,"longitude" : 14.56,"elevation" : 33.34}}"""
      val launchedTime = System.currentTimeMillis()
      val command = TestActorRef(new Actor {
        def receive = {
          case Disable(spokId, userId, launchedTime, geo) ⇒ {
            sender() ! DisableSpokFailure(new Exception(INVALID_JSON), PRS_001)
          }
        }
      })

      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ {
            sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
          }
        }
      })
      val expectedOutput = """{"resource":"disable","status":"failed","errors":[{"id":"PRS-001","message":"Invalid JSON"}],"data":{}}"""
      val result = detectRequestAndPerform(command, query, userId, "12345")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send error message if the geo validation fails while disabling a spok" in {
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val textInput = s"""{"action":"disable","spokId":"$spokId","geo":{"latitude" : 900.00,"longitude" : 214.56,"elevation" : 33.34}}"""
      val launchedTime = System.currentTimeMillis()
      val command = TestActorRef(new Actor {
        def receive = {
          case Disable(spokId, userId, launchedTime, geo) ⇒ {
            sender() ! INVALID_LONGITUDE
          }
        }
      })

      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ {
            sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
          }
        }
      })
      val expectedOutput = """{"resource":"disable","status":"failed","errors":[{"id":"GEO-001","message":"Invalid Latitude"},{"id":"GEO-002","message":"Invalid longitude"}],"data":{}}"""
      val result = detectRequestAndPerform(command, query, userId, "12345")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "return generic error message if disabling a spok fails" in {
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val textInput = s"""{"action":"disable","spokId":"$spokId","geo":{"latitude" : 90.00,"longitude" : 14.56,"elevation" : 33.34}}"""
      val launchedTime = System.currentTimeMillis()
      val command = TestActorRef(new Actor {
        def receive = {
          case Disable(spokId, userId, launchedTime, geo) ⇒ {
            sender() ! DisableSpokFailure(new Exception(UNABLE_DISABLING_SPOK), SPK_115)
          }
        }
      })

      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ {
            sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
          }
        }
      })
      val expectedOutput = """{"resource":"disable","status":"failed","errors":[{"id":"SPK-115","message":"Unable disabling spok (generic error)"}],"data":{}}"""
      val result = detectRequestAndPerform(command, query, userId, "12345")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "not subscribe/unsubscribe a spok's feed when spok is disabled " in {
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val textInput = s"""{"action":"subscribe","spokId":"$spokId","geo":{"latitude" : 90.00,"longitude" : 14.56,"elevation" : 33.34}}"""
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidSpokWithEnabledFlag(spokId) ⇒ {
            sender() ! IsValidSpokWithEnabledAck(false)
          }
          case IsUserSuspended(spokId) ⇒ sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
        }
      })
      val expectedOutput = s"""{"resource":"subscribe","status":"failed","errors":[{"id":"SPK-001","message":"Spok $spokId not found."}],"data":{}}"""
      val result = detectRequestAndPerform(query, query, userId, "12345")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "be subscribe a spok's feed when spok is valid" in {
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val geo = Geo(90.00, 14.56, 33.34)
      val textInput = s"""{"action":"subscribe","spokId":"$spokId","geo":{"latitude" : 90.00,"longitude" : 14.56,"elevation" : 33.34}}"""
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidSpokWithEnabledFlag(spokId) ⇒ {
            sender() ! IsValidSpokWithEnabledAck(true)
          }
          case IsUserSuspended(spokId) ⇒ sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
        }
      })
      val expectedOutput = """{"resource":"subscribe","status":"success","errors":[],"data":{"message":"subscribe spok's feed"}}"""
      when(redisFactory.isSubscriberExist(spokId, userId)) thenReturn (Future(false))
      when(redisFactory.storeSubscriber(spokId, userId)) thenReturn (Future(1l))
      when(spokLogger.insertSpokEvent(userId, spokId, timeStamp, SUBSCRIBED_EVENT, geo)) thenReturn true
      val result = detectRequestAndPerform(query, query, userId, "12345")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "be unsubscribe a spok's feed when spok is valid" in {
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val geo = Geo(90.00, 14.56, 33.34)
      val textInput = s"""{"action":"subscribe","spokId":"$spokId","geo":{"latitude" : 90.00,"longitude" : 14.56,"elevation" : 33.34}}"""
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidSpokWithEnabledFlag(spokId) ⇒ {
            sender() ! IsValidSpokWithEnabledAck(true)
          }
          case IsUserSuspended(spokId) ⇒ sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
        }
      })
      val expectedOutput = """{"resource":"subscribe","status":"success","errors":[],"data":{"message":"unsubscribe spok's feed"}}"""
      when(redisFactory.isSubscriberExist(spokId, userId)) thenReturn (Future(true))
      when(redisFactory.removeSubscriber(spokId, userId)) thenReturn (Future(1l))
      when(spokLogger.insertSpokEvent(userId, spokId, timeStamp, SUBSCRIBED_EVENT, geo)) thenReturn true
      val result = detectRequestAndPerform(query, query, userId, "12345")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "be able to send error message if the geo validation fails for subscribe/unsubscribe" in {
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val textInput = s"""{"action":"subscribe","spokId":"$spokId","geo":{"latitude" : 900.00,"longitude" :214.56,"elevation" : 33.34}}"""
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidSpokWithEnabledFlag(spokId) ⇒ {
            sender() ! INVALID_LONGITUDE
          }
          case IsUserSuspended(spokId) ⇒ sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
        }
      })
      val expectedOutput = """{"resource":"subscribe","status":"failed","errors":[{"id":"GEO-001","message":"Invalid Latitude"},{"id":"GEO-002","message":"Invalid longitude"}],"data":{}}"""
      val result = detectRequestAndPerform(query, query, userId, "12345")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "be able to send json error message if the json sent subscribe/unsubscribe is incorrect" in {
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val textInput = s"""{"action":"subscribe","spokId1":"$spokId","geo":{"latitude" : 90.00,"longitude" :14.56,"elevation" : 33.34}}"""
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidSpokWithEnabledFlag(spokId) ⇒ {
            sender() ! JSONERROR
          }
          case IsUserSuspended(spokId) ⇒ sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
        }
      })
      val expectedOutput = """{"resource":"subscribe","status":"failed","errors":[{"id":"PRS-001","message":"Invalid JSON"}],"data":{}}"""
      val result = detectRequestAndPerform(query, query, userId, "12345")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "view poll question if question id is valid" in {

      val questionId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val answerId1 = UUID.randomUUID().toString
      val answerId2 = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      when(redisFactory.storeSubscriber(spokId, userId)) thenReturn (Future(1l))
      val viewPollQuestion = ViewPollQuestion(None, ViewPollQuestionInternalResponse(questionId, "How many planets solar system has ?"),
        None, List(ViewPollAnswerResponse(answerId1, 1, "Nine"), ViewPollAnswerResponse(answerId2, 2, "Eight")))
      val query = TestActorRef(new Actor {
        def receive = {
          case ViewPollQuestionDetails(questionId, userId) ⇒ {
            sender() ! ViewPollQuestionSuccess(viewPollQuestion)
          }
        }
      })
      val result = Await.result(viewPollQuestionHandler(query, questionId, userId), 5 second)
      val output = s"""{"resource":"viewPollQuestion","status":"success","errors":[],"data":{"current":{"id":"$questionId","text":"How many planets solar system has ?"},"answers":[{"id":"$answerId1","rank":1,"text":"Nine"},{"id":"$answerId2","rank":2,"text":"Eight"}]}}"""
      assert(result === output)
    }

    "not view poll question if question id is invalid" in {

      val questionId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val query = TestActorRef(new Actor {
        def receive = {
          case ViewPollQuestionDetails(questionId, userId) ⇒ {
            sender() ! ViewPollQuestionFailure(SPK_126, s"Question $questionId Not Found")
          }
        }
      })
      val result = Await.result(viewPollQuestionHandler(query, questionId, userId), 5 second)
      val output = s"""{"resource":"viewPollQuestion","status":"failed","errors":[{"id":"SPK-126","message":"Question $questionId Not Found"}],"data":{}}"""
      assert(result === output)
    }

    "give generic error if not able to view poll question" in {

      val questionId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val query = TestActorRef(new Actor {
        def receive = {
          case ViewPollQuestionDetails(questionId, userId) ⇒ {
            sender() ! ViewPollQuestionFailure(SPK_112, s"Unable viewing question $questionId of the spok poll $spokId (generic error).")
          }
        }
      })
      val result = Await.result(viewPollQuestionHandler(query, questionId, userId), 5 second)
      val output = s"""{"resource":"viewPollQuestion","status":"failed","errors":[{"id":"SPK-112","message":"Unable viewing question $questionId of the spok poll $spokId (generic error)."}],"data":{}}"""
      assert(result === output)
    }

    "send success message, when a question of the poll is answered successfully" in {

      val questionId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val answerId = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val geo = Geo(13.67, 14.56, 33.3)
      val input =
        """{"action":"answerPoll","questionId":"""" + questionId +
          """",
                    "answerId":"""" + answerId +
          """",
                    "geo":{
                        "latitude":45.00,
                        "longitude":45.00,
                        "elevation":45.00
                        }
                    }"""

      val comment = UserPollAnswer(answerId, geo)
      val userPollAnswer = UserPollAnswer(answerId, geo)
      val response = TextMessage(write(Map("success" -> spokId)))
      val actorRef = TestActorRef(new Actor {
        def receive = {
          case SavePollAnswer(questionId, userId, userPollAnswer) ⇒ {
            sender ! PollAnswerSavedSuccess(spokId)
          }
        }
      })
      val expectedOutput = """{"resource":"answerPoll","status":"success","errors":[],"data":{"message":"Response Saved"}}"""
      when(redisFactory.storeSubscriber(spokId, userId)) thenReturn (Future(1l))
      val result = detectRequestAndPerform(actorRef, actorRef, userId, userId)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict((input)))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send json error message, when question id in json is not present while answering poll question" in {

      val questionId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val answerId = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val geo = Geo(13.67, 14.56, 33.3)
      val input =
        """{"action":"answerPoll",
                    "answerId":"""" + answerId +
          """",
                    "geo":{
                        "latitude":45.00,
                        "longitude":45.00,
                        "elevation":45.00
                        }
                    }"""

      val actorRef = TestActorRef(new Actor {
        def receive = {
          case SavePollAnswer(questionId, userId, userPollAnswer) ⇒ {
            sender ! INVALID_JSON
          }
        }
      })
      val expectedOutput = write(sendJsonErrorWithEmptyData(Some(ANSWER_POLL)))
      val result = detectRequestAndPerform(actorRef, actorRef, userId, userId)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict((input)))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send error message, when action is present but internal json is incorrect while answering poll question" in {

      val questionId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val answerId = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val geo = Geo(13.67, 14.56, 33.3)
      val input =
        """{"action":"answerPoll","questionId":"""" + questionId +
          """",
                    "answerId":"""" + answerId +
          """",
                    "geo":{
                        "longitude":45.00,
                        "elevation":45.00
                        }
                    }"""

      val actorRef = TestActorRef(new Actor {
        def receive = {
          case SavePollAnswer(questionId, userId, userPollAnswer) ⇒ {
            sender ! INVALID_JSON
          }
        }
      })
      val expectedOutput = """{"resource":"answerPoll","status":"failed","errors":[{"id":"PRS-001","message":"Invalid JSON"}],"data":{}}"""
      val result = detectRequestAndPerform(actorRef, actorRef, userId, userId)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict((input)))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send error message, when question id of the question to be answered is invalid" in {

      val questionId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val answerId = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val input =
        """{"action":"answerPoll","questionId":"""" + questionId +
          """",
                    "answerId":"""" + answerId +
          """",
                    "geo":{
                        "latitude":45.00,
                        "longitude":45.00,
                        "elevation":45.00
                        }
                    }"""

      val geo = Geo(13.67, 14.56, 33.3)
      val userPollAnswer = UserPollAnswer(answerId, geo)
      val response = Map("error" -> s"Question $questionId not found")
      val actorRef = TestActorRef(new Actor {
        def receive = {
          case SavePollAnswer(questionId, userId, userPollAnswer) ⇒ {
            sender ! PollAnswerSavedFailure(new Exception(s"Question $questionId not found"), "SPK-126")
          }
        }
      })
      val expectedOutput = s"""{"resource":"answerPoll","status":"failed","errors":[{"id":"SPK-126","message":"Question $questionId not found"}],"data":{}}"""
      val result = detectRequestAndPerform(actorRef, actorRef, userId, userId)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict((input)))
      sub.expectNext(TextMessage.Strict(expectedOutput))
    }

    "send error message, when answer id in the question to be answered is invalid" in {

      val questionId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val answerId = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val input =
        """{"action":"answerPoll","questionId":"""" + questionId +
          """",
                    "answerId":"""" + answerId +
          """",
                    "geo":{
                        "latitude":45.00,
                        "longitude":45.00,
                        "elevation":45.00
                        }
                    }"""
      val geo = Geo(13.67, 14.56, 33.3)
      val userPollAnswer = UserPollAnswer(answerId, geo)
      val response = Map("error" -> s"Invalid answer to question $questionId.")
      val actorRef = TestActorRef(new Actor {
        def receive = {
          case SavePollAnswer(questionId, userId, userPollAnswer) ⇒ {
            sender ! PollAnswerSavedFailure(new Exception(s"Invalid answer to question $questionId."), SPK_010)
          }
        }
      })
      val expectedOutput = s"""{"resource":"answerPoll","status":"failed","errors":[{"id":"SPK-010","message":"Invalid answer to question $questionId."}],"data":{}}"""
      val result = detectRequestAndPerform(actorRef, actorRef, userId, userId)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(input))
      sub.expectNext(TextMessage.Strict(expectedOutput))
    }

    "send error message, when geo is wrong while answering the poll question" in {

      val questionId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val answerId = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val geo = Geo(225.00, 225.00, 225.00)
      val userPollAnswer = UserPollAnswer(answerId, geo)
      val input =
        """{"action":"answerPoll","questionId":"""" + questionId +
          """",
                    "answerId":"""" + answerId +
          """",
                    "geo":{
                        "latitude":451.00,
                        "longitude":45.00,
                        "elevation":45.00
                        }
                    }"""
      val expectedOutput = """{"resource":"answerPoll","status":"failed","errors":[{"id":"GEO-001","message":"Invalid Latitude"}],"data":{}}"""
      val actorRef = TestActorRef(new Actor {
        def receive = {
          case SavePollAnswer(questionId, userId, userPollAnswer) ⇒ {
            sender ! INVALID_JSON
          }
        }
      })
      val result = detectRequestAndPerform(actorRef, actorRef, userId, userId)

      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(input))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "not remove spok when the spok is not found" in {
      val spokId = getUUID()
      val textInput = s"""{"action":"removeSpok","spokId":"$spokId","geo":{"latitude" : 90.00,"longitude" : 14.56,"elevation" : 33.34}}"""
      val launchedTime = System.currentTimeMillis()
      val command = TestActorRef(new Actor {
        def receive = {
          case RemoveSpok(spokId, "5ad25", launchedTime, geo) ⇒ {
            sender ! RemoveWallSpokSuccess(RemoveSpokResponse(Some("1234")))
          }
        }
      })
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidAbsoluteSpok(spokId) =>
            sender() ! IsValidAbsoluteSpokAck(SPOK_NOT_FOUND)

          case IsUserSuspended(spokId) ⇒ sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
        }
      })
      val expectedOutput = s"""{"resource":"removeSpok","status":"failed","errors":[{"id":"SPK-001","message":"Spok $spokId not found."}],"data":{}}"""
      val result = detectRequestAndPerform(command, query, "1234", "5ad25")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "not remove spok when the user is suspended" in {
      val spokId = getUUID()
      val textInput = s"""{"action":"removeSpok","spokId":"$spokId","geo":{"latitude" : 90.00,"longitude" : 14.56,"elevation" : 33.34}}"""
      val launchedTime = System.currentTimeMillis()

      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ sender ! IsUserSuspendedAsk(SPOKER_SUSPENDED)
        }
      })

      val expectedOutput = """{"resource":"removeSpok","status":"failed","errors":[{"id":"SPK-014","message":"Action impossible for a suspended account"}],"data":{}}"""

      val result = detectRequestAndPerform(query, query, "1234", "5ad25")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "not remove spok when suspended property not found" in {
      val spokId = getUUID()
      val textInput = s"""{"action":"removeSpok","spokId":"$spokId","geo":{"latitude" : 90.00,"longitude" : 14.56,"elevation" : 33.34}}"""
      val launchedTime = System.currentTimeMillis()

      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(spokId) ⇒ sender ! IsUserSuspendedAsk(PROPERTY_NOT_FOUND)
        }
      })

      val expectedOutput = """{"resource":"removeSpok","status":"failed","errors":[{"id":"SPK-116","message":"Unable removing spok (generic error)"}],"data":{}}"""
      val result = detectRequestAndPerform(query, query, "1234", "5ad25")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "not remove spok when the spok is Disabled" in {
      val spokId = getUUID()
      val textInput = s"""{"action":"removeSpok","spokId":"$spokId","geo":{"latitude" : 90.00,"longitude" : 14.56,"elevation" : 33.34}}"""
      val launchedTime = System.currentTimeMillis()
      val command = TestActorRef(new Actor {
        def receive = {
          case RemoveSpok(spokId, "5ad25", launchedTime, geo) ⇒ {
            sender ! RemoveWallSpokSuccess(RemoveSpokResponse(Some("1234")))
          }
        }
      })
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidAbsoluteSpok(spokId) =>
            sender() ! IsValidAbsoluteSpokAck(DISABLED_SPOK)
          case IsUserSuspended(spokId) ⇒ sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
        }
      })
      val expectedOutput = """{"resource":"removeSpok","status":"failed","errors":[{"id":"SPK-016","message":"spok is already disabled"}],"data":{}}"""
      val result = detectRequestAndPerform(command, query, "1234", "5ad25")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "not remove spok when the Failure occurs" in {
      val spokId = getUUID()
      val textInput = s"""{"action":"removeSpok","spokId":"$spokId","geo":{"latitude" : 90.00,"longitude" : 14.56,"elevation" : 33.34}}"""
      val launchedTime = System.currentTimeMillis()
      val command = TestActorRef(new Actor {
        def receive = {
          case RemoveSpok(someSpokId, "5ad25", launchedTime, geo) ⇒ {
            sender ! RemoveWallSpokFailure(new Exception(s"Unable removing spok $spokId (generic error)."), "SPK-116")
          }
        }
      })
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidAbsoluteSpok(someSpokId) =>
            sender() ! IsValidAbsoluteSpokAck(SPOK_VALID)
          case IsUserSuspended(spokId) ⇒ sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
        }
      })
      val expectedOutput = s"""{"resource":"removeSpok","status":"failed","errors":[{"id":"SPK-116","message":"Unable removing spok $spokId (generic error)."}],"data":{}}"""
      val result = detectRequestAndPerform(command, query, "5ad25", "5ad25")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "remove spok when the spok is Valid" in {
      val spokId = getUUID()
      val textInput = s"""{"action":"removeSpok","spokId":"$spokId","geo":{"latitude" : 90.00,"longitude" : 14.56,"elevation" : 33.34}}"""
      val launchedTime = System.currentTimeMillis()
      val res = RemoveSpokResponse(Some(spokId))
      val command = TestActorRef(new Actor {
        def receive = {
          case RemoveSpok(someSpokId, "5ad25", launchedTime, geo) ⇒ {
            sender ! RemoveWallSpokSuccess(res)
          }
        }
      })
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidAbsoluteSpok(someSpokId) =>
            sender() ! IsValidAbsoluteSpokAck(SPOK_VALID)
          case IsUserSuspended(spokId) ⇒ sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
        }
      })
      val expectedOutput = s"""{"resource":"removeSpok","status":"success","errors":[],"data":{"removeSpokResponse":{"spokId":"$spokId"}}}"""
      val result = detectRequestAndPerform(command, query, "5ad25", "5ad25")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send error message if the geo validation fails while removing a spok" in {
      val spokId = getUUID()
      val textInput = s"""{"action":"removeSpok","spokId":"$spokId","geo":{"latitude" : 900.00,"longitude" : 214.56,"elevation" : 33.34}}"""
      val launchedTime = System.currentTimeMillis()
      val res = RemoveSpokResponse(Some(spokId))
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidAbsoluteSpok(someSpokId) =>
            sender() ! INVALID_LONGITUDE
          case IsUserSuspended(spokId) ⇒ sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
        }
      })
      val expectedOutput = """{"resource":"removeSpok","status":"failed","errors":[{"id":"GEO-001","message":"Invalid Latitude"},{"id":"GEO-002","message":"Invalid longitude"}],"data":{}}"""
      val result = detectRequestAndPerform(query, query, "5ad25", "5ad25")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send error message if the json is incorrect while removing a spok" in {
      val spokId = getUUID()
      val textInput = s"""{"action":"removeSpok","spokId1":"$spokId","geo":{"latitude" : 90.00,"longitude" : 14.56,"elevation" : 33.34}}"""
      val launchedTime = System.currentTimeMillis()
      val res = RemoveSpokResponse(Some(spokId))
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidAbsoluteSpok(someSpokId) =>
            sender() ! INVALID_JSON
          case IsUserSuspended(spokId) ⇒ sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
        }
      })
      val expectedOutput = """{"resource":"removeSpok","status":"failed","errors":[{"id":"PRS-001","message":"Invalid JSON"}],"data":{}}"""
      val result = detectRequestAndPerform(query, query, "5ad25", "5ad25")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(textInput))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "perform get Spok Stats when spokId is valid" in {
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidAbsoluteSpok("1234") ⇒ {
            sender() ! IsValidAbsoluteSpokAck(SPOK_VALID)
          }
          case GetSpokStats("1234") ⇒ {
            sender() ! SpokStats(SpokStatistics(0.0, 1, 0, 0, 1, 0))
          }
        }
      })
      val result = Await.result(spokStatsHandler(query, "1234"), 5 second)
      val output = """{"resource":"spokStatistics","status":"success","errors":[],"data":{"totalTravelledDis":0.0,"numberOfUser":1,"numberOfRespoked":0,"numberOfUnspoked":0,"numberOfPending":1,"numberOfComment":0}}"""
      assert(result === output)
    }

    "not perform get Spok Stats when spokId is not valid" in {
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidAbsoluteSpok("1234") ⇒ {
            sender() ! IsValidAbsoluteSpokAck(SPOK_NOT_FOUND)
          }
          case GetSpokStats("1234") ⇒ {
            sender() ! SpokStats(SpokStatistics(0.0, 1, 0, 0, 1, 0))
          }
        }
      })
      val output = """{"resource":"spokStatistics","status":"failed","errors":[{"id":"SPK-001","message":"Spok 1234 not found."}],"data":{}}"""
      val result = Await.result(spokStatsHandler(query, "1234"), 5 second)
      assert(result === output)
    }

    "not perform get Spok Stats when spokId is Disabled" in {
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidAbsoluteSpok("1234") ⇒ {
            sender() ! IsValidAbsoluteSpokAck(DISABLED_SPOK)
          }
          case GetSpokStats("1234") ⇒ {
            sender() ! SpokStats(SpokStatistics(0.0, 1, 0, 0, 1, 0))
          }
        }
      })
      val output = """{"resource":"spokStatistics","status":"failed","errors":[{"id":"SPK-016","message":"spok is already disabled"}],"data":{}}"""
      val result = Await.result(spokStatsHandler(query, "1234"), 5 second)
      assert(result === output)
    }

    "get comments of a spok when get comments successfully" in {
      val spokId = getUUID()
      val userId = getUUID()
      val commentId = getUUID()
      val timestamp = DateTime.now()
      val commentsResponse = CommentsResponse("0", "2", 1, List(Comments(
        commentId,
        timestamp.toDate, "text", UserMinimalDetailsResponse(userId, "name", "male", "picture")
      )))

      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidAbsoluteSpok(spokId) ⇒ {
            sender() ! IsValidAbsoluteSpokAck(SPOK_VALID)
          }
          case GetComments(spokId, "1") ⇒ {
            sender() ! GetCommentsRes(Some(commentsResponse))
          }
        }
      })
      val result = Await.result(getCommentsHandler(query, spokId, "1"), 5 second)
      val output = s"""{"resource":"getComments","status":"success","errors":[],"data":{"previous":"0","next":"2""""
      assert(result contains output)
    }

    "return generic error message if unable to load comments of a spok" in {
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val timestamp = DateTime.now()
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidAbsoluteSpok(spokId) ⇒ {
            sender() ! IsValidAbsoluteSpokAck(SPOK_VALID)
          }
          case GetComments(spokId, "1") ⇒ {
            sender() ! GetCommentsRes(None)
          }
        }
      })
      val result = Await.result(getCommentsHandler(query, spokId, "1"), 5 second)
      val output = s"""{"resource":"getComments","status":"failed","errors":[{"id":"SPK-122","message":"Unable loading spok $spokId comments (generic error)."}],"data":{}}"""
      assert(result === output)
    }

    "not get comments of a spok when spok is not valid" in {
      val spokId = UUID.randomUUID().toString
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidAbsoluteSpok(spokId) ⇒ {
            sender() ! IsValidAbsoluteSpokAck(SPOK_NOT_FOUND)
          }
          case GetComments(spokId, "1") ⇒ {
            sender() ! GetCommentsRes(None)
          }
        }
      })
      val result = Await.result(getCommentsHandler(query, spokId, "1"), 5 second)
      val output = s"""{"resource":"getComments","status":"failed","errors":[{"id":"SPK-001","message":"Spok $spokId not found."}],"data":{}}"""
      assert(result === output)
    }

    "not get comments of a spok when spok is Disabled" in {
      val spokId = UUID.randomUUID().toString
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidAbsoluteSpok(spokId) ⇒ {
            sender() ! IsValidAbsoluteSpokAck(DISABLED_SPOK)
          }
          case GetComments(spokId, "1") ⇒ {
            sender() ! GetCommentsRes(None)
          }
        }
      })
      val result = Await.result(getCommentsHandler(query, spokId, "1"), 5 second)
      val output = s"""{"resource":"getComments","status":"failed","errors":[{"id":"SPK-002","message":"Spok $spokId is not available anymore."}],"data":{}}"""
      assert(result === output)
    }

    "get respokers of a spok when get respokers successfully" in {
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidAbsoluteSpok(spokId) ⇒ {
            sender() ! IsValidAbsoluteSpokAck(SPOK_VALID)
          }
          case GetReSpokers(spokId, "1") ⇒ {
            sender() ! ReSpokersRes(Some(ReSpokerResponse("0", "2", List(ReSpoker(userId, "name", "male", "picture")))))
          }
        }
      })
      val result = Await.result(getReSpokersHandler(query, spokId, "1"), 5 second)
      val output = s"""{"resource":"loadRespokers","status":"success","errors":[],"data":{"previous":"0","next":"2","reSpoker":[{"id":"$userId","name":"name","gender":"male","picture":"picture"}]}}"""
      assert(result === output)
    }

    "not get respokers of a spok when spok is Disabled" in {
      val spokId = UUID.randomUUID().toString
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidAbsoluteSpok(spokId) ⇒ {
            sender() ! IsValidAbsoluteSpokAck(DISABLED_SPOK)
          }
          case GetReSpokers(spokId, "1") ⇒ {
            sender() ! ReSpokersRes(None)
          }
        }
      })
      val result = Await.result(getReSpokersHandler(query, spokId, "1"), 5 second)
      val output = s"""{"resource":"loadRespokers","status":"failed","errors":[{"id":"SPK-002","message":"Spok $spokId is not available anymore."}],"data":{}}"""
      assert(result === output)
    }

    "not get respokers of a spok when spok is not valid" in {
      val spokId = UUID.randomUUID().toString
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidAbsoluteSpok(spokId) ⇒ {
            sender() ! IsValidAbsoluteSpokAck(SPOK_NOT_FOUND)
          }
          case GetReSpokers(spokId, "1") ⇒ {
            sender() ! ReSpokersRes(None)
          }
        }
      })
      val result = Await.result(getReSpokersHandler(query, spokId, "1"), 5 second)
      val output = s"""{"resource":"loadRespokers","status":"failed","errors":[{"id":"SPK-001","message":"Spok $spokId not found."}],"data":{}}"""
      assert(result === output)
    }

    "Generic error while getting respokers of a spok" in {
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidAbsoluteSpok(spokId) ⇒ {
            sender() ! IsValidAbsoluteSpokAck(SPOK_VALID)
          }
          case GetReSpokers(spokId, "1") ⇒ {
            sender() ! ReSpokersRes(None)
          }
        }
      })
      val result = Await.result(getReSpokersHandler(query, spokId, "1"), 5 second)
      val output = s"""{"resource":"loadRespokers","status":"failed","errors":[{"id":"SPK-102","message":"Unable loading spok $spokId re-spokers (generic error)."}],"data":{}}"""
      assert(result === output)
    }

    "get Scoped users of a spok when get scoped users successfully" in {
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidAbsoluteSpok(spokId) ⇒ {
            sender() ! IsValidAbsoluteSpokAck(SPOK_VALID)
          }
          case GetScopedUsers(spokId, "1") ⇒ {
            sender() ! ScopedUsersRes(Some(ScopedUsersResponse("1", "2", List(ScopedUsers(userId, "name", "male", "picture")))))
          }
        }
      })
      val result = Await.result(scopedUsersHandler(query, spokId, "1"), 5 second)
      val output = s"""{"resource":"loadScopedUsers","status":"success","errors":[],"data":{"previous":"1","next":"2","scopedUsers":[{"id":"$userId","name":"name","gender":"male","picture":"picture"}]}}"""
      assert(result === output)
    }

    "not get Scoped users of a spok when spok is not valid" in {
      val spokId = UUID.randomUUID().toString
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidAbsoluteSpok(spokId) ⇒ {
            sender() ! IsValidAbsoluteSpokAck(SPOK_NOT_FOUND)
          }
          case GetScopedUsers(spokId, "1") ⇒ {
            sender() ! ScopedUsersRes(None)
          }
        }
      })
      val result = Await.result(scopedUsersHandler(query, spokId, "1"), 5 second)
      val output = s"""{"resource":"loadScopedUsers","status":"failed","errors":[{"id":"SPK-001","message":"Spok $spokId not found."}],"data":{}}"""
      assert(result === output)
    }

    "not get Scoped users of a spok when spok is Disabled" in {
      val spokId = UUID.randomUUID().toString
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidAbsoluteSpok(spokId) ⇒ {
            sender() ! IsValidAbsoluteSpokAck(DISABLED_SPOK)
          }
          case GetScopedUsers(spokId, "1") ⇒ {
            sender() ! ScopedUsersRes(None)
          }
        }
      })
      val result = Await.result(scopedUsersHandler(query, spokId, "1"), 5 second)
      val output = s"""{"resource":"loadScopedUsers","status":"failed","errors":[{"id":"SPK-002","message":"Spok $spokId is not available anymore."}],"data":{}}"""
      assert(result === output)
    }

    "Generic error while getting Scoped users of a spok" in {
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidAbsoluteSpok(spokId) ⇒ {
            sender() ! IsValidAbsoluteSpokAck(SPOK_VALID)
          }
          case GetScopedUsers(spokId, "1") ⇒ {
            sender() ! ScopedUsersRes(None)
          }
        }
      })
      val result = Await.result(scopedUsersHandler(query, spokId, "1"), 5 second)
      val output = s"""{"resource":"loadScopedUsers","status":"failed","errors":[{"id":"SPK-103","message":"Unable loading spok $spokId scoped users (generic error)."}],"data":{}}"""
      assert(result === output)
    }

    "perform get Spoks Stack when get spoks successfully" in {

      val id = "928cc1e5-c0f9-4d58-b0c3-9b66a4d190b3"
      val id1 = "13bca28f-5f84-4084-8311-84b723913ab5"
      val spokId = "928cc1e5-c0f9-4d58-b0c3-9b66a4d190b3"
      val author = Spoker(id, "ramesh", "male", "picture")
      val from = Spoker(id1, "ram", "male", "picture")
      val counters = Counters(1, 1, 1, 0.0)
      val content = Content(picturePreview = Some("picturePreview"), pictureFull = Some("pictureFull"))
      val spokStackResponse = Some(SpoksStackResponse("1", "2", List(ViewSpok(spokId, "text", 1000, new Date(1233333),
        "hi i am kias", Some(new Date(1233333)), "curText", false, author, from, "public", counters, content))))

      val query = TestActorRef(new Actor {
        def receive = {
          case GetSpokStack("1234", "1") ⇒ {
            sender() ! SpoksStack(spokStackResponse)
          }
        }
      })
      val result = Await.result(spokStackHandler(query, "1", "1234"), 5 second)
      val spokStackResponseJson = write(spokStackResponse)
      val output = s"""{"resource":"loadStack","status":"success","errors":[],"data":$spokStackResponseJson}"""
      assert(result === output)
    }

    "not perform get Spoks Stack when get spoks Failure" in {
      val userId = "userId"
      val query = TestActorRef(new Actor {
        def receive = {
          case GetSpokStack(userId, "11") ⇒ {
            sender() ! SpoksStack(None)
          }
        }
      })
      val result = Await.result(spokStackHandler(query, "11", "1234"), 5 second)
      val output = """{"resource":"loadStack","status":"failed","errors":[{"id":"SPK-105","message":"Unable loading spok's stack (generic error)"}],"data":{}}"""
      assert(result === output)
    }

    "view short spok of a spok if spok id is valid" in {
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val userId1 = UUID.randomUUID().toString
      val launched = new Date()
      val respoked = new Date()
      DseGraphFactory.dseConn.executeGraph("graph.addVertex(label,'spok','spokId','" + spokId + "')")
      val spokVertex = DseGraphFactory.dseConn.executeGraph("g.V().hasLabel('spok').has('spokId','" + spokId + "')").one().asVertex();
      val viewSpok = ViewSpok(spokId, "rawtext", 0, launched, "text", Some(respoked), "curtext", false,
        Spoker(userId, "spoker", "male", "spokerjpg"), Spoker(userId1, "from", "male", "fromjpg"), "public", Counters(1, 1, 1, 1000),
        Content(rawText = Some("text")))
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidSpokById(spokId) ⇒ {
            sender() ! IsValidSpokByIdAck(SPOK_VALID, Some(spokVertex))
          }
          case ViewShortSpok(spokId, targetUserId, userId, Some(spokVertex)) ⇒ {
            sender() ! ViewShortSpokResponse(Some(viewSpok))
          }
        }
      })
      val result = Await.result(viewShortSpok(query, spokId, "", userId), 5 second)
      val viewSpokJson = write(viewSpok)
      val output = s"""{"resource":"viewShortSpok","status":"success","errors":[],"data":$viewSpokJson}"""
      assert(result contains output)
    }

    "not view short spok of a spok if spok id is not valid" in {
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val userId1 = UUID.randomUUID().toString
      val viewSpok = ViewSpok(spokId, "rawtext", 0, DateTime.now().toDate, "text", Some(DateTime.now().toDate), "curtext", false,
        Spoker(userId, "spoker", "male", "spoker.jpg"), Spoker(userId1, "from", "male", "from.jpg"), "public", Counters(1, 1, 1, 1000),
        Content())
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidSpokById(spokId) ⇒ {
            sender() ! IsValidSpokByIdAck(SPOK_NOT_FOUND, None)
          }
        }
      })
      val result = Await.result(viewShortSpok(query, spokId, "", userId), 5 second)
      val output = s"""{"resource":"viewShortSpok","status":"failed","errors":[{"id":"SPK-001","message":"Spok $spokId not found."}],"data":{}}"""
      assert(result === output)
    }

    "not view short spok of a spok if it is Disabled" in {
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val userId1 = UUID.randomUUID().toString
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidSpokById(spokId) ⇒ {
            sender() ! IsValidSpokByIdAck(DISABLED_SPOK, None)
          }
        }
      })
      val result = Await.result(viewShortSpok(query, spokId, "", userId), 5 second)
      val output = s"""{"resource":"viewShortSpok","status":"failed","errors":[{"id":"SPK-002","message":"Spok $spokId is not available anymore."}],"data":{}}"""
      assert(result === output)
    }

    "Generic error while viewing short spok of a spok" in {
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val userId1 = UUID.randomUUID().toString
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidSpokById(spokId) ⇒ {
            sender() ! IsValidSpokByIdAck(SPOK_VALID, None)
          }
          case ViewShortSpok(spokId, targetUserId, userId, None) ⇒ {
            sender() ! ViewShortSpokResponse(None)
          }
        }
      })
      val result = Await.result(viewShortSpok(query, spokId, "", userId), 5 second)
      val output = s"""{"resource":"viewShortSpok","status":"failed","errors":[{"id":"SPK-101","message":"Unable loading spok $spokId (generic error)."}],"data":{}}"""
      assert(result === output)
    }

    "view spokers wall successfully" in {
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val launched = DateTime.now()
      val respoked = DateTime.now()
      val userId1 = UUID.randomUUID().toString
      val viewSpokersWall = UsersWallResponse("0", "2", List(Spoks(spokId, "rawtext", launched.toDate, "text", respoked.toDate, "curtext", false, Spoker(userId, "respoker", "male", "reSpoker.jpg"),
        Counters(2, 2, 2, 1000), Content(rawText = Some("rawText")))))
      val query = TestActorRef(new Actor {
        def receive = {
          case ViewSpokersWall(userId1, "1", userId) ⇒ {
            sender() ! ViewSpokersWallSuccess(viewSpokersWall)
          }
        }
      })
      val result = Await.result(viewSpokersWallHandler(query, userId1, "1", userId), 5 second)
      val output = s"""{"resource":"viewSpokersWall","status":"success","errors":[],"data":{"previous":"0","next":"2","spoks":[{"id":"$spokId","spokType":"rawtext","launched":"""
      assert(result contains output)
    }

    "Generic error while viewing spokers wall" in {
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val userId1 = UUID.randomUUID().toString
      val query = TestActorRef(new Actor {
        def receive = {
          case ViewSpokersWall(userId1, "1", userId) ⇒ {
            sender() ! ViewSpokersWallFailure(new Exception(s"Unable loading user $userId wall (generic error)."), USR_102)
          }
        }
      })
      val result = Await.result(viewSpokersWallHandler(query, userId1, "1", userId), 5 second)
      val output = s"""{"resource":"viewSpokersWall","status":"failed","errors":[{"id":"USR-102","message":"Unable loading user $userId wall (generic error)."}],"data":{}}"""
      assert(result === output)
    }

    "view full spok of a spok if spok id is valid" in {
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val targetUserId = UUID.randomUUID().toString
      val userId1 = UUID.randomUUID().toString
      val timestamp = DateTime.now()
      val reSpokers = List(ReSpoker(targetUserId, "name", "male", "picture"))
      val scopedUsers = List(ScopedUsers(targetUserId, "name", "male", "picture"))
      val comments: List[CommentsForFullSpok] = List(CommentsForFullSpok("text", timestamp.toDate,
        targetUserId, "name", "picture", "male"))

      val viewFullSpokDetails = ViewFullSpok(spokId, "rawtext", 0, DateTime.now().toDate, "text", Some(DateTime.now().toDate), "curtext", false, Spoker(userId, "respoker", "male", "reSpoker.jpg"),
        Spoker(userId1, "fromSpoker", "male", "fromSpoker.jpg"), "public", Counters(2, 2, 2, 1000), reSpokers, scopedUsers, comments,
        Content(rawText = Some("rawText")))
      DseGraphFactory.dseConn.executeGraph("graph.addVertex(label,'spok','spokId','" + spokId + "')")
      val spokVertex = DseGraphFactory.dseConn.executeGraph("g.V().hasLabel('spok').has('spokId','" + spokId + "')").one().asVertex();

      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidSpokById(spokId) ⇒ {
            sender() ! IsValidSpokByIdAck(SPOK_VALID, Some(spokVertex))
          }
          case ViewFullSpokDetails(spokId, targetUserId, userId, Some(spokVertex)) ⇒ {
            sender() ! ViewFullSpokResponse(Some(viewFullSpokDetails))
          }
        }
      })
      val result = Await.result(viewFullSpok(query, spokId, targetUserId, userId), 5 second)
      val viewSpokJson = write(viewFullSpokDetails)
      val output = s"""{"resource":"viewFullSpok","status":"success","errors":[],"data":$viewSpokJson}"""
      assert(result contains output)
    }

    "not view full spok of a spok if spok id is not valid" in {
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val targetUserId = UUID.randomUUID().toString
      val userId1 = UUID.randomUUID().toString
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidSpokById(spokId) ⇒ {
            sender() ! IsValidSpokByIdAck(SPOK_NOT_FOUND, None)
          }
        }
      })
      val result = Await.result(viewFullSpok(query, spokId, targetUserId, userId), 5 second)
      val output = s"""{"resource":"viewFullSpok","status":"failed","errors":[{"id":"SPK-001","message":"Spok $spokId not found."}],"data":{}}"""
      assert(result === output)
    }

    "not view full spok of a spok if it is Disabled" in {
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val targetUserId = UUID.randomUUID().toString
      val userId1 = UUID.randomUUID().toString
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidSpokById(spokId) ⇒ {
            sender() ! IsValidSpokByIdAck(DISABLED_SPOK, None)
          }
        }
      })
      val result = Await.result(viewFullSpok(query, spokId, targetUserId, userId), 5 second)
      val output = s"""{"resource":"viewFullSpok","status":"failed","errors":[{"id":"SPK-002","message":"Spok $spokId is not available anymore."}],"data":{}}"""
      assert(result === output)
    }

    "Generic error while viewing full spok details of a spok" in {
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val targetUserId = UUID.randomUUID().toString
      val userId1 = UUID.randomUUID().toString
      val timestamp = DateTime.now()
      val reSpokers = List(ReSpoker(targetUserId, "name", "male", "picture"))
      val scopedUsers = List(ScopedUsers(targetUserId, "name", "male", "picture"))
      val query = TestActorRef(new Actor {
        def receive = {
          case IsValidSpokById(spokId) ⇒ {
            sender() ! IsValidSpokByIdAck(SPOK_VALID, None)
          }
          case ViewFullSpokDetails(spokId, targetUserId, userId, None) ⇒ {
            sender() ! ViewFullSpokResponse(None)
          }
        }
      })
      val result = Await.result(viewFullSpok(query, spokId, targetUserId, userId), 5 second)
      val output = s"""{"resource":"viewFullSpok","status":"failed","errors":[{"id":"SPK-101","message":"Unable loading spok $spokId (generic error)."}],"data":{}}"""
      assert(result === output)
    }

    "Return success result when view poll stats is successfully executed" in {
      val spokId = getUUID()
      val userId = getUUID()
      val pollId = getUUID()
      val questionId = getUUID()
      val answerId = getUUID()
      val pollStats: PollStats = PollStats(pollId, Some("poll test"), Some("testing the poll"), 1, List(
        PollQuestionsStats(questionId, "Who is the father of Computer?", List(PollAnswerStats(answerId, "Charles Babbage", 4)))
      ))
      val query = TestActorRef(new Actor {
        def receive = {
          case ViewPollStats(spokId, userId) ⇒ {
            sender() ! ViewPollStatsSuccess(pollStats)
          }
        }
      })
      val result = Await.result(viewPollStatsHandler(query, spokId, userId), 5 second)
      val output = s"""{"resource":"pollResults","status":"success","errors":[],"data":{"id":"$pollId","text":"poll test","description":"testing the poll","nb":1,"questions":[{"id":"$questionId","text":"Who is the father of Computer?","answers":[{"id":"$answerId","text":"Charles Babbage","nb":4}]}]}}"""
      assert(result === output)
    }

    "Return error result when view poll stats fails with an error" in {
      val spokId = getUUID()
      val userId = getUUID()
      val pollId = getUUID()
      val questionId = getUUID()
      val answerId = getUUID()
      val query = TestActorRef(new Actor {
        def receive = {
          case ViewPollStats(spokId, userId) ⇒ {
            sender() ! ViewPollStatsFailure(SPK_101, s"Spok $spokId not found.")
          }
        }
      })
      val result = Await.result(viewPollStatsHandler(query, spokId, userId), 5 second)
      val output = s"""{"resource":"pollResults","status":"failed","errors":[{"id":"SPK-101","message":"Spok $spokId not found."}],"data":{}}"""
      assert(result === output)
    }

    "perform get my Spoks when get spoks successfully" in {

      val id = "928cc1e5-c0f9-4d58-b0c3-9b66a4d190b3"
      val id1 = "13bca28f-5f84-4084-8311-84b723913ab5"
      val spokId = "928cc1e5-c0f9-4d58-b0c3-9b66a4d190b3"
      val author = Spoker(id, "ramesh", "male", "picture")
      val from = Spoker(id1, "ram", "male", "picture")
      val counters = Counters(1, 1, 1, 0.0)
      val content = Content(picturePreview = Some("picturePreview"), pictureFull = Some("pictureFull"))
      val spokStackResponse = Some(SpoksStackResponse("1", "2", List(ViewSpok(spokId, "text", 1000, new Date(1233333),
        "hi i am kias", Some(new Date(1233333)), "curText", false, author, from, "public", counters, content))))

      val query = TestActorRef(new Actor {
        def receive = {
          case GetMySpoks("1234", "1") ⇒ {
            sender() ! SpoksStack(spokStackResponse)
          }
        }
      })
      val result = Await.result(viewMySpokHandler(query, "1234", "1"), 5 second)

      val spokStackResponseJson = write(spokStackResponse)
      val output = s"""{"resource":"myspoks","status":"success","errors":[],"data":$spokStackResponseJson}"""
      assert(result === output)
    }

    "not perform get my Spoks  when get spoks Failure" in {
      val userId = "userId"
      val query = TestActorRef(new Actor {
        def receive = {
          case GetMySpoks(userId, "11") ⇒ {
            sender() ! SpoksStack(None)
          }
        }
      })
      val result = Await.result(viewMySpokHandler(query, "1234", "11"), 5 second)
      val output = """{"resource":"myspoks","status":"failed","errors":[{"id":"SPK-110","message":"Unable loading my spoks list (generic error)."}],"data":{}}"""
      assert(result === output)
    }

    "send success message, when an entire poll is completed successfully dementors" in {

      val questionId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val answerId = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val geo = Geo(13.67, 14.56, 33.3)
      val input =
        s"""
          |{
          |"action": "answersPoll",
          |"spokId": "$spokId",
          |"oneAnswer":[
          |{"questionId":"$questionId",
          |"answerId":"$answerId"}
          |]
          |"geo": {
          |    "latitude": 13.67,
          |    "longitude": 14.56,
          |    "elevation": 33.34
          |}
          |}
        """.stripMargin

      val actorRef = TestActorRef(new Actor {
        def receive = {
          case SaveAllPollAnswers(userId, allAnswers) ⇒ {
            sender ! PollAllAnswersSavedSuccess(spokId)
          }
        }
      })
      val expectedOutput = """{"resource":"answersPoll","status":"success","errors":[],"data":{"response":"Response Saved"}}"""
      val result = detectRequestAndPerform(actorRef, actorRef, userId, userId)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict((input)))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send error message, when an entire poll is not completed because database sent failure response dementors" in {

      val questionId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val answerId = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val geo = Geo(13.67, 14.56, 33.3)
      val input =
        s"""
           |{
           |"action": "answersPoll",
           |"spokId": "$spokId",
           |"oneAnswer":[
           |{"questionId":"$questionId",
           |"answerId":"$answerId"}
           |]
           |"geo": {
           |    "latitude": 13.67,
           |    "longitude": 14.56,
           |    "elevation": 33.34
           |}
           |}
        """.stripMargin

      val actorRef = TestActorRef(new Actor {
        def receive = {
          case SaveAllPollAnswers(userId, allAnswers) ⇒ {
            sender ! PollAllAnswersSavedFailure(SPK_124, s"Unable saving answers to poll spok ${spokId} (generic error).")
          }
        }
      })
      val expectedOutput = s"""{"resource":"answersPoll","status":"failed","errors":[{"id":"SPK-124","message":"Unable saving answers to poll spok ${spokId} (generic error)."}],"data":{}}"""
      val result = detectRequestAndPerform(actorRef, actorRef, userId, userId)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict((input)))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send failure message, when an entire poll is not completed successfully because json was wrong dementors" in {

      val questionId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val answerId = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val geo = Geo(13.67, 14.56, 33.3)
      val input =
        s"""
           |{
           |"action": "answersPoll",
           |"oneAnswer":[
           |{"questionId":"$questionId",
           |"answerId":"$answerId"}
           |]
           |"geo": {
           |    "latitude": 13.67,
           |    "longitude": 14.56,
           |    "elevation": 33.34
           |}
           |}
        """.stripMargin

      val actorRef = TestActorRef(new Actor {
        def receive = {
          case SaveAllPollAnswers(userId, allAnswers) ⇒ {
            sender ! JSONERROR
          }
        }
      })
      val expectedOutput = """{"resource":"answersPoll","status":"failed","errors":[{"id":"PRS-001","message":"Invalid JSON"}],"data":{}}"""
      val result = detectRequestAndPerform(actorRef, actorRef, userId, userId)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict((input)))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send failure message, when an entire poll is not completed successfully because geo location was wrong dementors" in {

      val questionId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val answerId = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val geo = Geo(13.67, 14.56, 33.3)
      val input =
        s"""
           |{
           |"action": "answersPoll",
           |"spokId": "$spokId",
           |"oneAnswer":[
           |{"questionId":"$questionId",
           |"answerId":"$answerId"}
           |]
           |"geo": {
           |    "latitude": 1333.67,
           |    "longitude": 14.56,
           |    "elevation": 33.34
           |}
           |}
        """.stripMargin

      val actorRef = TestActorRef(new Actor {
        def receive = {
          case SaveAllPollAnswers(userId, allAnswers) ⇒ {
            sender ! INVALID_LATITUDE
          }
        }
      })
      val expectedOutput = """{"resource":"answersPoll","status":"failed","errors":[{"id":"GEO-001","message":"Invalid Latitude"}],"data":{}}"""
      val result = detectRequestAndPerform(actorRef, actorRef, userId, userId)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict((input)))
      sub.expectNext(TextMessage(expectedOutput))
    }

  }
}