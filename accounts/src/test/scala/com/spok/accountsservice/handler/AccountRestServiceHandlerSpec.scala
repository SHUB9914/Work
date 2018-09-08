package com.spok.accountsservice.handler

import java.text.SimpleDateFormat
import java.util.{ Date, UUID }

import akka.actor.{ Actor, ActorSystem }
import akka.http.scaladsl.model.ws.{ Message, TextMessage }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Keep, Source }
import akka.stream.testkit.scaladsl.{ TestSink, TestSource }
import akka.testkit.TestActorRef
import com.rbmhtechnology.eventuate.ReplicationEndpoint
import com.rbmhtechnology.eventuate.ReplicationEndpoint._
import com.rbmhtechnology.eventuate.log.leveldb.LeveldbEventLog
import com.spok.accountsservice.service.AccountActorCommand._
import com.spok.accountsservice.service.AccountActorFailureReplies._
import com.spok.accountsservice.service.AccountActorSuccessReplies._
import com.spok.accountsservice.service.AccountAlreadyRegisters._
import com.spok.accountsservice.service.AccountManagerCommands._
import com.spok.accountsservice.service.AccountSuccessViewReplies.{ DisableResponseSuccess, _ }
import com.spok.accountsservice.service.AccountViewCommands._
import com.spok.accountsservice.service.AccountViewFailureReplies.{ DisableResponseFailure, _ }
import com.spok.model.Account._
import com.spok.model.SpokModel.{ Geo, GroupsResponse }
import com.spok.model._
import com.spok.persistence.dse.DseGraphFactory
import com.spok.persistence.factory.DSEGraphPersistenceFactoryApi
import com.spok.persistence.factory.messaging.MessagingApi
import com.spok.persistence.redis.RedisFactory
import com.spok.util.Constant._
import com.spok.util.{ FileUploadUtility, JWTTokenHelper }
import org.joda.time.DateTime
import org.mockito.Mockito._
import org.scalatest.{ BeforeAndAfterAll, WordSpec }
import org.scalatest.mock.MockitoSugar

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
/**
 * Test spec for account rest service
 */
class AccountRestServiceHandlerSpec extends WordSpec with AccountRestServiceHandler with MockitoSugar with BeforeAndAfterAll {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  val endpoint = ReplicationEndpoint(id => LeveldbEventLog.props(logId = "AccountRestHandlerSpec"))(system)
  val eventLog = endpoint.logs(DefaultLogName)
  val id = "randomId"
  override val dseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]

  override val jwtTokenHelper: JWTTokenHelper = mock[JWTTokenHelper]
  override val fileUploadUtility: FileUploadUtility = mock[FileUploadUtility]
  override val redisFactory: RedisFactory = mock[RedisFactory]
  override val dseMessagingApi: MessagingApi = mock[MessagingApi]

  val cyrilId = getUUID()
  val date = "1994-01-18"
  val format = new SimpleDateFormat("yyyy-MM-dd")
  val validDate = format.parse(date)

  override def beforeAll {
    val user1 = User("Cyril", validDate, Location(List(LocationDetails(
      List(AddressComponents("Marsiele", "Marsiele", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.63, 77.50), SouthWest(28.63, 77.50)), InnerLocation(43.28, 5.26), "APPROXIMATE",
        ViewPort(NorthEast(28.63, 77.50), SouthWest(28.63, 77.50))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List(), "+919638527401", cyrilId, Some("testuser.jpg"), None, "india")

    DSEGraphPersistenceFactoryApi.insertUser(user1)
    DSEGraphPersistenceFactoryApi.createUserSetting(user1.userId)
  }

  override def afterAll: Unit = {
    DseGraphFactory.dseConn.executeGraph("g.V().drop()")

  }
  override def getUUID(): String = "abcspok123"

  val userDetails =
    """
          {"action":"details",
          "nickname":"sonu",
          "birthdate":"1992-10-25",
          "location":{
             "results" : [
                {
                   "address_components" : [
                      {
                         "long_name" : "Noida",
                         "short_name" : "Noida",
                         "types" : [ "locality", "political" ]
                      }
                   ],
                   "formatted_address" : "Noida, Uttar Pradesh 201301, India",
                   "geometry" : {
                      "bounds" : {
                         "northeast" : {
                            "lat" : 28.6363011,
                            "lng" : 77.5025632
                         },
                         "southwest" : {
                            "lat" : 28.3972059,
                            "lng" : 77.2936967
                         }
                      },
                      "location" : {
                         "lat" : 28.5355161,
                         "lng" : 77.3910265
                      },
                      "location_type" : "APPROXIMATE",
                      "viewport" : {
                         "northeast" : {
                            "lat" : 28.6363011,
                            "lng" : 77.5025632
                         },
                         "southwest" : {
                            "lat" : 28.3972059,
                            "lng" : 77.2936967
                         }
                      }
                   },
                   "place_id" : "ChIJezVzMaTlDDkRP8B8yDDO_zc",
                   "types" : [ "locality", "political" ]
                }
                ],
             "status" : "OK"
          },
          "gender":"male"
          "contacts":["+919582311051","+919582611051"]
          "phone_number":"+918258996467",
          "geoText":"india"
          }
    """

  "Account service" should {

    "return the appropriate message for the phone number" in {

      val numberInput =
        """{
               "action":"register",
          "country_code":"+91",
          "phone_number":"8510013658"
          }
            """

      val successOutput = """{"resource":"register","status":"success","data":{"message":"OTP has been sent to 8510013658."}}"""

      val message = "OTP has been sent to 8510013658."
      val command = TestActorRef(new Actor {
        def receive = {
          case Validate(phoneNumber: String) ⇒ {
            sender ! ValidateUserSuccess(message)
          }
        }
      })
      val result = detectRegistrationRequestAndPerform(command, command)
      val s = Source.single[String](numberInput)

      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Streamed(s))
      sub.expectNext(TextMessage.Strict(successOutput))
    }

    "return json error message when the phone number json is wrong" in {

      val numberInput =
        """{
               "action":"register",
          "phone_number":"8510013658"
          }"""

      val expectedOutput =
        """{"resource":"register","status":"failed","errors":[{"id":"PRS-001","message":"Invalid JSON"}],"data":{}}"""

      val command = TestActorRef(new Actor {
        def receive = {
          case Validate(phoneNumber: String) ⇒ TextMessage(write(Map("error" -> INVALID_JSON)))
        }
      })
      val result = detectRegistrationRequestAndPerform(command, command)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(numberInput))
      sub.expectNext(TextMessage.Strict(expectedOutput))
    }

    "return json error message when action is not found" in {

      val numberInput =
        """{
               "action":"register1",
          "phone_number":"8510013658"
          }"""
      val expectedOutput =
        """{"resource":"action","status":"failed","errors":[{"id":"SYST-401","message":"Unable authenticating user (generic error)"}],"data":{}}"""
      val command = TestActorRef(new Actor {
        def receive = {
          case Validate(phoneNumber: String) ⇒ TextMessage(write(Map("error" -> INVALID_JSON)))
        }
      })
      val result = detectRegistrationRequestAndPerform(command, command)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(numberInput))
      sub.expectNext(TextMessage.Strict(expectedOutput))
    }

    "return json error message when action is missing" in {

      val numberInput =
        """{
          "phone_number":"8510013658"
          }"""

      val expectedOutput =
        s"""{"resource":"ws://api.spok.me","status":"failed","errors":[{"id":"ACT-101","message":"Action is missing."}],"data":{}}""".stripMargin

      val command = TestActorRef(new Actor {
        def receive = {
          case Validate(phoneNumber: String) ⇒ TextMessage(write(Map("error" -> INVALID_JSON)))
        }
      })
      val result = detectRegistrationRequestAndPerform(command, command)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(numberInput))
      sub.expectNext(TextMessage.Strict(expectedOutput))
    }

    "return the error message if the phone number is wrong" in {

      val numberInput =
        """{"action":"register",
          "country_code":"91",
          "phone_number":"9582311555"
          }
            """

      val expectedOutput =
        """{"resource":"register","status":"failed","errors":[{"id":"RGX-001","message":"Invalid phone number."}],"data":{}}"""

      val command = TestActorRef(new Actor {
        def receive = {
          case Validate(phoneNumber: String) ⇒ {
            sender ! ValidateUserFailure("Invalid Phone Number", new Exception("Error"))
          }
        }
      })
      val result = detectRegistrationRequestAndPerform(command, command)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(numberInput))
      sub.expectNext(TextMessage.Strict(expectedOutput))
    }

    "return the appropriate message for the Authenticate phone number" in {

      val numberInput =
        """{
               "action":"authenticate",
          "country_code":"+91",
          "phone_number":"8510013658"
          }
            """

      val successOutput = """{"resource":"authenticate","status":"success","data":{"message":"OTP has been sent to 8510013658."}}"""

      val message = "OTP has been sent to 8510013658."
      val command = TestActorRef(new Actor {
        def receive = {
          case Authenticate(phoneNumber: String) ⇒ {
            sender ! AuthenticateUserSuccess(message)
          }
        }
      })
      val result = detectRegistrationRequestAndPerform(command, command)
      val s = Source.single[String](numberInput)

      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Streamed(s))
      sub.expectNext(TextMessage.Strict(successOutput))
    }

    "return the error message if the phone number is wrong in Authentication" in {

      val numberInput =
        """{"action":"authenticate",
              "country_code":"91",
              "phone_number":"9582311555"
              }
                """

      val expectedOutput =
        """{"resource":"authenticate","status":"failed","errors":[{"id":"RGX-001","message":"Invalid phone number."}],"data":{}}"""

      val command = TestActorRef(new Actor {
        def receive = {
          case Authenticate(phoneNumber: String) ⇒ {
            sender ! AuthenticateUserFailure("Invalid Phone Number", new Exception("Error"))
          }
        }
      })
      val result = detectRegistrationRequestAndPerform(command, command)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(numberInput))
      sub.expectNext(TextMessage.Strict(expectedOutput))
    }

    "return the error message if unable to register a phone number" in {

      val numberInput =
        """{"action":"register",
              "country_code":"+91",
              "phone_number":"9582311555"
              }
                """

      val expectedOutput =
        """{"resource":"register","status":"failed","errors":[{"id":"IDT-102","message":"Unable registering phone (generic error)."}],"data":{}}"""

      val command = TestActorRef(new Actor {
        def receive = {
          case Validate(phoneNumber: String) ⇒ {
            sender ! ValidateUserFailure(GENERIC_ERROR_MESSAGE, new Exception("Error"))
          }
        }
      })
      val result = detectRegistrationRequestAndPerform(command, command)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(numberInput))
      sub.expectNext(TextMessage.Strict(expectedOutput))
    }

    "return the error message if unable to authenticate a phone number" in {

      val numberInput =
        """{"action":"authenticate",
              "country_code":"+91",
              "phone_number":"9582311555"
              }
                """

      val expectedOutput =
        """{"resource":"authenticate","status":"failed","errors":[{"id":"IDT-102","message":"Unable authenticate phone (generic error)."}],"data":{}}"""

      val command = TestActorRef(new Actor {
        def receive = {
          case Authenticate(phoneNumber: String) ⇒ {
            sender ! AuthenticateUserFailure(GENERIC_ERROR_MESSAGE_AUTHENTICATE, new Exception("Error"))
          }
        }
      })
      val result = detectRegistrationRequestAndPerform(command, command)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(numberInput))
      sub.expectNext(TextMessage.Strict(expectedOutput))
    }

    "return the error message if number is not registered" in {

      val numberInput =
        """{"action":"authenticate",
              "country_code":"+91",
              "phone_number":"9582311555"
              }
                """

      val expectedOutput =
        """{"resource":"authenticate","status":"failed","errors":[{"id":"IDT-001","message":"User not regitered."}],"data":{}}"""

      val command = TestActorRef(new Actor {
        def receive = {
          case Authenticate(phoneNumber: String) ⇒ {
            sender ! UserNotRegitered(USER_NOT_REGISTERD)
          }
        }
      })
      val result = detectRegistrationRequestAndPerform(command, command)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(numberInput))
      sub.expectNext(TextMessage.Strict(expectedOutput))
    }

    "return the error message if number is already in use" in {

      val numberInput =
        """{"action":"register",
          "country_code":"+91",
          "phone_number":"9582311555"
          }
            """

      val expectedOutput =
        """{"resource":"register","status":"failed","errors":[{"id":"IDT-001","message":"Phone number already used."}],"data":{}}"""

      val command = TestActorRef(new Actor {
        def receive = {
          case Validate(phoneNumber: String) ⇒ {
            sender ! AllReadyRegisteredUser(ALREADY_USED_NUMBER)
          }
        }
      })
      val result = detectRegistrationRequestAndPerform(command, command)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(numberInput))
      sub.expectNext(TextMessage.Strict(expectedOutput))
    }

    "return success message if the OTP number is correct" in {

      val numberAndOtpInput =
        """{"action":"code",
          "phone_number":"912316531",
          "code":"1234"
          }
            """
      val otpToken = OtpAuthToken("1234", "912316531", new DateTime().plusMinutes(5), 0)

      val query = TestActorRef(new Actor {
        def receive = {
          case GetOTPToken(phoneNumber: String) ⇒ {
            sender ! FindOtpTokenSuccess(otpToken)
          }
        }
      })
      val command = TestActorRef(new Actor {
        def receive = {
          case UpdateOtpToken(newOtpToken: OtpAuthToken) ⇒ {
            sender ! ValidateUserSuccess(("OTP has been updated."))
          }
        }
      })

      val result = detectRegistrationRequestAndPerform(command, query)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(numberAndOtpInput))
      sub.expectEvent()
    }

    "return error message if the OTP number is incorrect" in {

      val numberAndOtpInput =
        """{"action":"code",
          "phone_number":"912316531",
          "code":"1335"
          }
            """

      val expectedOutput = """{"resource":"code","status":"failed","errors":[{"id":"IDT-005","message":"Wrong confirmation code (unrelated to this phone)."}],"data":{}}"""

      val otpToken = OtpAuthToken("1234", "912316531", new DateTime().plusMinutes(5), 0)

      val query = TestActorRef(new Actor {
        def receive = {
          case GetOTPToken(phoneNumber: String) ⇒ {
            sender ! FindOtpTokenSuccess(otpToken)
          }
        }
      })
      val command = TestActorRef(new Actor {
        def receive = {
          case UpdateOtpToken(newOtpToken: OtpAuthToken) ⇒ {
            sender ! ValidateUserSuccess(("OTP has been updated."))
          }
        }
      })

      val result = detectRegistrationRequestAndPerform(command, query)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(numberAndOtpInput))
      sub.expectNext(TextMessage.Strict(expectedOutput))
    }

    "return error message if the OTP number is invalid, i.e., not of 4 digits" in {

      val numberAndOtpInput =
        """{"action":"code",
          "phone_number":"912316531",
          "code":"133"
          }
            """

      val expectedOutput = """{"resource":"code","status":"failed","errors":[{"id":"RGX-002","message":"Invalid confirmation code."}],"data":{}}"""
      val otpToken = OtpAuthToken("1234", "912316531", new DateTime().plusMinutes(5), 0)

      val query = TestActorRef(new Actor {
        def receive = {
          case GetOTPToken(phoneNumber: String) ⇒ {
            sender ! FindOtpTokenSuccess(otpToken)
          }
        }
      })
      val command = TestActorRef(new Actor {
        def receive = {
          case UpdateOtpToken(newOtpToken: OtpAuthToken) ⇒ {
            sender ! ValidateUserSuccess("OTP has been updated.")
          }
        }
      })

      val result = detectRegistrationRequestAndPerform(command, query)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(numberAndOtpInput))
      sub.expectNext(TextMessage.Strict(expectedOutput))
    }

    "return success message if the OTP number is entered correctly after one retry" in {

      val invalidnumberAndOtpInput =
        """{"action":"code",
          "phone_number":"912316531",
          "code":"1334"
          }
            """

      val validnumberAndOtpInput =
        """{"action":"code",
          "phone_number":"912316531",
          "code":"1234"
          }
            """

      val expectedOutput = """{"resource":"code","status":"failed","errors":[{"id":"IDT-005","message":"Wrong confirmation code (unrelated to this phone)."}],"data":{}}"""
      val otpToken = OtpAuthToken("1234", "912316531", new DateTime().plusMinutes(5), 0)

      val query = TestActorRef(new Actor {
        def receive = {
          case GetOTPToken(phoneNumber: String) ⇒ {
            sender ! FindOtpTokenSuccess(otpToken)
          }
        }
      })
      val command = TestActorRef(new Actor {
        def receive = {
          case UpdateOtpToken(newOtpToken: OtpAuthToken) ⇒ {
            sender ! ValidateUserSuccess(("OTP has been updated."))
          }
        }
      })

      val result = detectRegistrationRequestAndPerform(command, query)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(2)
      pub.sendNext(TextMessage.Strict(invalidnumberAndOtpInput))
      sub.expectNext(TextMessage.Strict(expectedOutput))
      pub.sendNext(TextMessage.Strict(validnumberAndOtpInput))
      sub.expectEvent()
    }

    "return error message if the wrong OTP number is entered after maximum number of retries" in {

      val numberAndOtpInput =
        """{"action":"code",
          "phone_number":"912316531",
          "code":"1334"
          }
            """
      val expectedOutput = """{"resource":"code","status":"failed","errors":[{"id":"IDT-110","message":"OTP expired, Retries exceeded max allowed tries"}],"data":{}}"""
      val otpToken = OtpAuthToken("1234", "912316531", new DateTime().plusMinutes(5), 3)

      val query = TestActorRef(new Actor {
        def receive = {
          case GetOTPToken(phoneNumber: String) ⇒ {
            sender ! FindOtpTokenSuccess(otpToken)
          }
        }
      })
      val command = TestActorRef(new Actor {
        def receive = {
          case UpdateOtpToken(newOtpToken: OtpAuthToken) ⇒ {
            sender ! ValidateUserSuccess(("OTP has been updated."))
          }
        }
      })

      val result = detectRegistrationRequestAndPerform(command, query)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(numberAndOtpInput))
      pub.sendNext(TextMessage.Strict(numberAndOtpInput))
      pub.sendNext(TextMessage.Strict(numberAndOtpInput))
      pub.sendNext(TextMessage.Strict(numberAndOtpInput))
      sub.expectNext(TextMessage.Strict(expectedOutput))
    }

    "return error message when otp phone number json is incorrect" in {
      val numberAndOtpInput =
        """{"action":"code",
          "phone_number":"912316531"
          }
            """
      val expectedOutput = """{"resource":"code","status":"failed","errors":[{"id":"PRS-001","message":"Invalid JSON"}],"data":{}}"""
      val otpToken = OtpAuthToken("1234", "912316531", new DateTime().plusMinutes(5), 0)
      val query = TestActorRef(new Actor {
        def receive = {
          case GetOTPToken(phoneNumber: String) ⇒ TextMessage(write(Map("error" -> INVALID_JSON)))

        }
      })
      val command = TestActorRef(new Actor {
        def receive = {
          case UpdateOtpToken(newOtpToken: OtpAuthToken) ⇒ TextMessage(write(Map("error" -> INVALID_JSON)))
        }
      })
      val result = detectRegistrationRequestAndPerform(command, query)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(numberAndOtpInput))
      sub.expectNext(TextMessage.Strict(expectedOutput))
    }

    "return error message if the OTP number is not found" in {
      val numberAndOtpInput =
        """{"action":"code",
          "phone_number":"912316531",
          "code":"1334"
          }
            """
      val expectedOutput = """{"resource":"code","status":"failed","errors":[{"id":"IDT-109","message":"Unable validating phone (generic error)."}],"data":{}}"""
      val otpToken = OtpAuthToken("1234", "912316531", new DateTime().plusMinutes(5), 0)
      val query = TestActorRef(new Actor {
        def receive = {
          case GetOTPToken(phoneNumber: String) ⇒ {
            sender ! FindOtpTokenFailure(GENERIC_OTP_ERROR_MESSAGE)
          }
        }
      })
      val command = TestActorRef(new Actor {
        def receive = {
          case UpdateOtpToken(newOtpToken: OtpAuthToken) ⇒ {
            sender ! ValidateUserSuccess(("OTP has been updated."))
          }
        }
      })
      val result = detectRegistrationRequestAndPerform(command, query)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(numberAndOtpInput))
      sub.expectNext(TextMessage.Strict(expectedOutput))
    }

    "send success message when account is registered" in {
      val userId = getUUID
      val command = TestActorRef(new Actor {
        def receive = {
          case Create(user: User) ⇒ {
            sender ! AccountCreateSuccess((user))
          }
        }
      })
      val query = TestActorRef(new Actor {
        def receive = {
          case GetValidUser(phoneNumber) ⇒ sender ! true
        }
      })
      val expectedOutput = """{"resource":"details","status":"success","errors":[],"data":{"token":"token123","userId":"abcspok123","userContactsIds":[]}}"""
      when(dseGraphPersistenceFactoryApi.getRegisteredUsers("918258996467")) thenReturn (List())
      when(dseGraphPersistenceFactoryApi.performFollow(FollowUnfollow("918258996467", "userid123", "userid321"))) thenReturn (None)
      when(jwtTokenHelper.createJwtTokenWithRole(userId, "+918258996467", USER_ROLE)) thenReturn ("token123")
      val result = detectRegistrationRequestAndPerform(command, query)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(userDetails))
      sub.expectNext(TextMessage.Strict(expectedOutput))
    }

    "send error message when user is trying to register without verifying OTP" in {
      val command = TestActorRef(new Actor {
        def receive = {
          case Create(user: User) ⇒ {
            sender ! AccountCreateSuccess((user))
          }
        }
      })
      val query = TestActorRef(new Actor {
        def receive = {
          case GetValidUser(phoneNumber) ⇒ sender ! false
        }
      })
      val expectedOutput = """{"resource":"details","status":"failed","errors":[{"id":"IDT-121","message":"Please go to step1 and verify you number"}],"data":{}}"""
      val result = detectRegistrationRequestAndPerform(command, query)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(userDetails))
      sub.expectNext(TextMessage.Strict(expectedOutput))
    }

    "send failure message when account is not registered" in {
      val incorrectUserDetails =
        """{"action":"details",
        "nickname":"sonu",
        "birthdate":"199210-25",
        "location":"noida",
        "gender":"male",
        "contacts":["+919582311051","+919582611051"]
        "phone_number":"+918258996467",
        "geoText":"india"
      }"""
      val command = TestActorRef(new Actor {
        def receive = {
          case Create(user: User) ⇒ {
            sender ! AccountCreateFailure(user, (new Exception("Invalid date")))
          }
        }
      })
      val expectedOutput = """{"resource":"details","status":"failed","errors":[{"id":"TIME-008","message":"Invalid date"},{"id":"RGX-005","message":"Invalid Location"}],"data":{}}"""
      val result = detectRegistrationRequestAndPerform(command, command)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(incorrectUserDetails))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send incorrect user details JSON message when the JSON is not correct" in {
      val userDetailsInput =
        """
                  {"action":"details",
                  "birthdate":"1992-10-25",
                  "location":{
                     "results" : [
                        {
                           "address_components" : [
                              {
                                 "long_name" : "Noida",
                                 "short_name" : "Noida",
                                 "types" : [ "locality", "political" ]
                              }
                           ],
                           "formatted_address" : "Noida, Uttar Pradesh 201301, India",
                           "geometry" : {
                              "bounds" : {
                                 "northeast" : {
                                    "lat" : 28.6363011,
                                    "lng" : 77.5025632
                                 },
                                 "southwest" : {
                                    "lat" : 28.3972059,
                                    "lng" : 77.2936967
                                 }
                              },
                              "location" : {
                                 "lat" : 28.5355161,
                                 "lng" : 77.3910265
                              },
                              "location_type" : "APPROXIMATE",
                              "viewport" : {
                                 "northeast" : {
                                    "lat" : 28.6363011,
                                    "lng" : 77.5025632
                                 },
                                 "southwest" : {
                                    "lat" : 28.3972059,
                                    "lng" : 77.2936967
                                 }
                              }
                           },
                           "place_id" : "ChIJezVzMaTlDDkRP8B8yDDO_zc",
                           "types" : [ "locality", "political" ]
                        }
                        ],
                     "status" : "OK"
                  },
                  "gender":"male"
                  "contacts":["+919582311051","+919582611051"]
                  "phone_number":"+918258996467"
                  }
            """
      val command = TestActorRef(new Actor {
        def receive = {
          case Create(user: User) ⇒ {
            sender ! AccountCreateFailure(user, (new Exception("Invalid JSON")))
          }
        }
      })
      val result = detectRegistrationRequestAndPerform(command, command)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(userDetailsInput))
      sub.expectEvent()
    }

    "send generic error message when unable to register " in {

      val command = TestActorRef(new Actor {
        def receive = {
          case Create(user: User) ⇒ {
            sender ! AccountCreateFailure(user, new Exception(s"Unable registering nickname sonu (generic error)"))
          }
        }
      })

      val query = TestActorRef(new Actor {
        def receive = {
          case GetValidUser(phoneNumber) ⇒ sender ! true
        }
      })

      val expectedOutput = """{"resource":"details","status":"failed","errors":[{"id":"IDT-104","message":"Unable registering nickname sonu (generic error)"}],"data":{}}"""
      when(dseGraphPersistenceFactoryApi.getRegisteredUsers("918258996467")) thenReturn (List())
      when(dseGraphPersistenceFactoryApi.performFollow(FollowUnfollow("918258996467", "userid123", "userid321"))) thenReturn (None)
      when(jwtTokenHelper.createJwtTokenWithRole(getUUID, "+918258996467", USER_ROLE)) thenReturn ("token123")
      val result = detectRegistrationRequestAndPerform(command, query)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(userDetails))
      sub.expectNext(TextMessage.Strict(expectedOutput))
    }

    "not able to registe if nickname is not unique" in {
      val command = TestActorRef(new Actor {
        def receive = {
          case Create(user: User) ⇒ {
            sender ! AccountCreateFailure(user, new Exception(s"Nickname is already in use."))
          }
        }
      })
      val query = TestActorRef(new Actor {
        def receive = {
          case GetValidUser(phoneNumber) ⇒ sender ! true
        }
      })
      val expectedOutput = """{"resource":"details","status":"failed","errors":[{"id":"IDT-122","message":"Nickname is already in use."}],"data":{}}"""
      when(dseGraphPersistenceFactoryApi.getRegisteredUsers("918258996467")) thenReturn (List())
      when(dseGraphPersistenceFactoryApi.isUniqueNickname("sonu")) thenReturn true
      when(dseGraphPersistenceFactoryApi.performFollow(FollowUnfollow("918258996467", "userid123", "userid321"))) thenReturn (None)
      when(jwtTokenHelper.createJwtTokenWithRole(getUUID, "+918258996467", USER_ROLE)) thenReturn ("token123")
      val result = detectRegistrationRequestAndPerform(command, query)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(userDetails))
      sub.expectNext(TextMessage.Strict(expectedOutput))
    }

    "not parse the incorrect phone number json" in {
      val numberInput =
        """{
          "phone_number":"9582311"
          }
            """
      val result = super.extractPhoneNumber(numberInput)
      assert(result == (None, None))
    }

    "not parse incorrect phone and otp json" in {
      val otpNumberInput =
        """{
            "code":"1234"
          }"""
      val result = super.extractOTPNumber(otpNumberInput)
      assert(result == (None, None))
    }

    "send follow/unfollow generic error when user is unable to complete follow/unfollow an user" in {

      val dateNew: Date = new java.util.Date()
      val input = """{"action":"followUnfollow","followingId":"userid321"}"""
      val followUnfollow = FollowUnfollow("919582311059", "userid123", "userid321")
      val command = TestActorRef(new Actor {
        def receive = {
          case FollowUnfollowAction(followUnfollow: FollowUnfollow) ⇒ {
            sender ! FollowUnfollowFailure(new Exception(FOLLOW_UNFOLLOW_ERROR))
          }
        }
      })
      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(targetId) ⇒ {
            sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
          }
        }
      })
      val expectedOutput = """{"resource":"followUnfollow","status":"failed","errors":[{"id":"FLW-101","message":"Unable to Follow/Unfollow user (Generic Error)"}],"data":{}}"""
      when(dseGraphPersistenceFactoryApi.performFollowOrUnfollow("919582311059", "userid123", "userid321")) thenReturn None
      val result = detectRequestAndPerform(command, query, "userid123", "userid123")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(input))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send follow/unfollow error if the user tries to follow himself/herself" in {
      val userId = getUUID()
      val input = s"""{"action":"followUnfollow","followingId": "$userId"}"""
      val command = TestActorRef(new Actor {
        def receive = {
          case FollowUnfollowAction(followUnfollow: FollowUnfollow) ⇒ {
            sender ! NOT_FOLLOW_ITSELF
          }
        }
      })
      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(targetId) ⇒ {
            sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
          }
        }
      })

      val expectedOutput = """{"resource":"followUnfollow","status":"failed","errors":[{"id":"FLW-104","message":"User cannot follow himself/herself"}],"data":{}}"""
      val result = detectRequestAndPerform(command, query, userId, "919582311059")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(input))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send error message when suspended user tries to interact with follow/Unfollow " in {
      val userId = getUUID()
      val input = s"""{"action":"followUnfollow","followingId": "$userId"}"""
      val command = TestActorRef(new Actor {
        def receive = {
          case FollowUnfollowAction(followUnfollow: FollowUnfollow) ⇒ {
            sender ! NOT_FOLLOW_ITSELF
          }
        }
      })
      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(targetId) ⇒ {
            sender ! IsUserSuspendedAsk(SPOKER_SUSPENDED)
          }
        }
      })

      val expectedOutput = """{"resource":"followUnfollow","status":"failed","errors":[{"id":"SPK-014","message":"Action impossible for a suspended account"}],"data":{}}"""
      val result = detectRequestAndPerform(command, query, userId, "919582311059")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(input))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send error message when suspended property of user not found in follow/Unfollow " in {
      val userId = getUUID()
      val input = s"""{"action":"followUnfollow","followingId": "$userId"}"""
      val command = TestActorRef(new Actor {
        def receive = {
          case FollowUnfollowAction(followUnfollow: FollowUnfollow) ⇒ {
            sender ! NOT_FOLLOW_ITSELF
          }
        }
      })
      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(targetId) ⇒ {
            sender ! IsUserSuspendedAsk(PROPERTY_NOT_FOUND)
          }
        }
      })

      val expectedOutput = """{"resource":"followUnfollow","status":"failed","errors":[{"id":"FLW-101","message":"Unable to Follow/Unfollow user (Generic Error)"}],"data":{}}"""
      val result = detectRequestAndPerform(command, query, userId, "919582311059")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(input))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send follow successful response when user follow an user successfully" in {

      val followUnfollow = FollowUnfollow("919582311059", "userid123", "userid321")
      val command = TestActorRef(new Actor {
        def receive = {
          case FollowUnfollowAction(followUnfollow: FollowUnfollow) ⇒ {
            sender ! FollowUnfollowSuccess(FOLLOWS)
          }
        }
      })
      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(targetId) ⇒ {
            sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
          }
        }
      })

      val input = """{"action":"followUnfollow","followingId":"userid321"}"""
      when(dseGraphPersistenceFactoryApi.performFollowOrUnfollow("919582311059", "userid123", "userid321")) thenReturn Some(FOLLOWS)
      val result = detectRequestAndPerform(command, query, "userid123", "userid123")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(input))
      sub.expectNext(TextMessage("""{"resource":"followUnfollow","status":"success","errors":[],"data":{"followResponse":{"userMobileNumber":"userid123","followerId":"userid123","followingId":"userid321"}}}"""))
    }

    "send error response when user sends json with wrong action" in {

      val followUnfollow = FollowUnfollow("919582311059", "userid123", "userid321")
      val command = TestActorRef(new Actor {
        def receive = {
          case FollowUnfollowAction(followUnfollow: FollowUnfollow) ⇒ {
            sender ! FollowUnfollowFailure
          }
        }
      })

      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(targetId) ⇒ {
            sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
          }
        }
      })

      val input = """{"action":"followUnfollow1","followingId":"userid321"}"""
      val expectedOutput = """{"resource":"ws://api.spok.me","status":"failed","errors":[{"id":"ACT-101","message":"Action is missing."}],"data":{}}"""
      val result = detectRequestAndPerform(command, query, "userid123", "userid123")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(input))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send error response when user sends json without action" in {

      val followUnfollow = FollowUnfollow("919582311059", "userid123", "userid321")
      val command = TestActorRef(new Actor {
        def receive = {
          case FollowUnfollowAction(followUnfollow: FollowUnfollow) ⇒ {
            sender ! FollowUnfollowFailure
          }
        }
      })
      val input = """{"followingId":"userid321"}"""
      val expectedOutput = """{"resource":"ws://api.spok.me","status":"failed","errors":[{"id":"ACT-101","message":"Action is missing."}],"data":{}}"""
      val result = detectRequestAndPerform(command, command, "userid123", "userid123")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(input))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send unfollow successful response when user unfollow an user successfully" in {
      val followUnfollow = FollowUnfollow("919582311059", "userid123", "userid321")
      val command = TestActorRef(new Actor {
        def receive = {
          case FollowUnfollowAction(followUnfollow: FollowUnfollow) ⇒ {
            sender ! FollowUnfollowSuccess(UNFOLLOWS)
          }
        }
      })
      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(targetId) ⇒ {
            sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
          }
        }
      })
      val input = """{"action":"followUnfollow","followingId":"userid321"}"""
      when(dseGraphPersistenceFactoryApi.performFollowOrUnfollow("919582311059", "userid123", "userid321")) thenReturn Some(UNFOLLOWS)
      when(redisFactory.isTalkExist("userid123", "userid321")) thenReturn Future(false)
      when(redisFactory.removeTalkId("userid123", "userid321")) thenReturn Future(0l)
      val result = detectRequestAndPerform(command, query, "userid123", "userid123")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(input))
      sub.expectNext(TextMessage("""{"resource":"followUnfollow","status":"success","errors":[],"data":{"unFollowResponse":{"userMobileNumber":"userid123","followerId":"userid123","followingId":"userid321"}}}"""))
    }

    "send follow and friend successful response when user becomes friend successfully" in {

      val followingId = getUUID()
      val followerId = getUUID()
      val followUnfollow = FollowUnfollow("919582311059", followerId, followingId)
      val followUnfollow1 = FollowUnfollow("919582311057", followingId, followerId)
      val command = TestActorRef(new Actor {
        def receive = {
          case FollowUnfollowAction(followUnfollow: FollowUnfollow) ⇒ {
            sender ! FollowUnfollowSuccess(FOLLOWS)
          }
        }
      })
      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(targetId) ⇒ {
            sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
          }
        }
      })

      val input = """{"action":"followUnfollow","followingId":"""" + followingId + """"}"""
      when(dseGraphPersistenceFactoryApi.performFollowOrUnfollow("919582311059", followerId, followingId)) thenReturn Some(FOLLOWS)
      when(dseGraphPersistenceFactoryApi.performFollowOrUnfollow("919582311057", followingId, followerId)) thenReturn Some(FOLLOWS)
      when(dseGraphPersistenceFactoryApi.isFollowingExists(followingId, followerId)) thenReturn true
      val result1 = detectRequestAndPerform(command, query, "userid123", "userid123")
      val (pub, sub) = TestSource.probe[Message]
        .via(result1)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(input))
      sub.expectNext(TextMessage("""{"resource":"followUnfollow","status":"success","errors":[],"data":{"followResponse":{"userMobileNumber":"userid123","followerId":"userid123","followingId":"abcspok123"}}}"""))
    }

    "send group creation successful response when user creats a group successfully" in {
      val inputJson = """{"action":"createGroup","title":"TestTitle"}"""
      val groupId = getUUID()
      val userId = getUUID()
      val group = Group(groupId, "title")
      val command = TestActorRef(new Actor {
        def receive = {
          case CreateUserGroup(group: Group, phoneNumber: String, userId: String) ⇒ {
            sender ! GroupCreateSuccess(group)
          }
        }
      })
      val data = """{"groupId":"abcspok123"}"""
      val expectedOutput = s"""{"resource":"createGroup","status":"success","errors":[],"data":""" + data + """}"""
      when(dseGraphPersistenceFactoryApi.createGroup(userId, group)) thenReturn (Some("Group created"))
      val result = detectRequestAndPerform(command, command, userId, "1231233214")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(inputJson))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send group creation failure response when user fails to creats a group" in {
      val inputJson = """{"action":"createGroup","title":"TestTitle"}"""
      val groupId = getUUID()
      val userId = getUUID()
      val group = Group(groupId, "title")
      val command = TestActorRef(new Actor {
        def receive = {
          case CreateUserGroup(group: Group, phoneNumber: String, userId: String) ⇒ {
            sender ! GroupCreateFailure(group, new Exception(GROUP_CREATION_ERROR))
          }
        }
      })
      val data = """{"groupId":"abcspok123"}"""
      val expectedOutput = """{"resource":"createGroup","status":"failed","errors":[{"id":"GRP-101","message":"Unable to create group (generic error)"}],"data":{}}"""
      when(dseGraphPersistenceFactoryApi.createGroup(userId, group)) thenReturn (Some("Group created"))
      val result = detectRequestAndPerform(command, command, userId, "1231233214")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(inputJson))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send Invalid JSON response when user sends invalid JSON while creating a group" in {
      val inputJson =
        """{"action":"createGroup","titles":"TestTitle"}"""
      val groupId = getUUID()
      val userId = getUUID()
      val group = Group(groupId, "title")
      val command = TestActorRef(new Actor {
        def receive = {
          case CreateUserGroup(group: Group, phoneNumber: String, userId: String) ⇒ {
            sender ! TextMessage(write(Map("error" -> INVALID_JSON)))
          }
        }
      })
      val expectedOutput = """{"resource":"createGroup","status":"failed","errors":[{"id":"PRS-001","message":"Invalid JSON"}],"data":{}}"""
      val result = detectRequestAndPerform(command, command, userId, "1231233214")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(inputJson))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send group removal success response when user remove a group successfully" in {

      val groupId = getUUID()
      val userId = getUUID()
      val phoneNumber = "1234556"
      val inputJson =
        """{"action":"removeGroup","groupId":"""" + groupId + """"}"""
      val query = TestActorRef(new Actor {
        def receive = {
          case ValidateGroup(groupId, userId) =>
            sender() ! IsValidGroupAck(true)
        }
      })

      val command = TestActorRef(new Actor {
        def receive = {
          case RemoveGroup(groupId: String, userId, phoneNumber: String) ⇒ {
            sender ! GroupRemovedSuccess(groupId)
          }
        }
      })
      val data = """{"groupId":"abcspok123"}"""
      val expectedOutput = s"""{"resource":"removeGroup","status":"success","errors":[],"data":""" + data + """}"""
      when(dseGraphPersistenceFactoryApi.isGroupExist(userId, groupId)) thenReturn true
      when(dseGraphPersistenceFactoryApi.removeGroup(groupId, userId)) thenReturn true
      val result = detectRequestAndPerform(command, query, userId, phoneNumber)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(inputJson))
      sub.expectNext(TextMessage.Strict(expectedOutput))
    }

    "send group id not found while user removing a group" in {

      val groupId = getUUID()
      val userId = getUUID()
      val phoneNumber = "1234556"
      val inputJson =
        """{"action":"removeGroup","groupId1":"""" + groupId + """"}"""
      val query = TestActorRef(new Actor {
        def receive = {
          case ValidateGroup(groupId, userId) =>
            sender() ! IsValidGroupAck(true)
        }
      })

      val command = TestActorRef(new Actor {
        def receive = {
          case RemoveGroup(groupId: String, userId, phoneNumber: String) ⇒ {
            sender ! GroupRemovedSuccess(groupId)
          }
        }
      })
      val data = """{"groupId":"abcspok123"}"""
      val expectedOutput = """{"resource":"removeGroup","status":"failed","errors":[{"id":"GRP-001","message":"Group Id not found"}],"data":{}}"""
      when(dseGraphPersistenceFactoryApi.isGroupExist(userId, groupId)) thenReturn true
      when(dseGraphPersistenceFactoryApi.removeGroup(groupId, userId)) thenReturn true
      val result = detectRequestAndPerform(command, query, userId, phoneNumber)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(inputJson))
      sub.expectNext(TextMessage.Strict(expectedOutput))
    }

    "send group updation successful response when user updates the name of a group successfully" in {
      val userId = getUUID()
      val groupId = getUUID()
      val inputJson = """{"action":"updateGroup","groupId":"""" + groupId + """","title":"TestTitle"}"""

      val group = Group(groupId, "TestTitle")
      val command = TestActorRef(new Actor {
        def receive = {
          case UpdateUserGroup(group: Group, phoneNumber: String, userId: String) ⇒ {
            sender ! GroupUpdateSuccess(group)
          }
        }
      })
      val data = """{"groupId":"abcspok123"}"""
      val expectedOutput = s"""{"resource":"updateGroup","status":"success","errors":[],"data":""" + data + """}"""
      val query = TestActorRef(new Actor {
        def receive = {
          case ValidateGroup(groupId, userId) =>
            sender() ! IsValidGroupAck(true)
        }
      })
      when(dseGraphPersistenceFactoryApi.isGroupExist(userId, groupId)) thenReturn true
      when(dseGraphPersistenceFactoryApi.updateGroup(userId, group)) thenReturn true
      val result = detectRequestAndPerform(command, query, userId, "1231233214")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(inputJson))
      sub.expectNext(TextMessage.Strict(expectedOutput))
    }

    "send group id not found response when user send invalid group id in json" in {
      val userId = getUUID()
      val groupId = getUUID()
      val inputJson = """{"action":"updateGroup","groupId1":"""" + groupId + """","title":"TestTitle"}"""

      val group = Group(groupId, "TestTitle")
      val command = TestActorRef(new Actor {
        def receive = {
          case UpdateUserGroup(group: Group, phoneNumber: String, userId: String) ⇒ {
            sender ! GroupUpdateSuccess(group)
          }
        }
      })
      val data = """{"groupId":"abcspok123"}"""
      val expectedOutput = """{"resource":"updateGroup","status":"failed","errors":[{"id":"GRP-001","message":"Group Id not found"}],"data":{}}"""
      val query = TestActorRef(new Actor {
        def receive = {
          case ValidateGroup(groupId, userId) =>
            sender() ! IsValidGroupAck(true)
        }
      })
      when(dseGraphPersistenceFactoryApi.isGroupExist(userId, groupId)) thenReturn true
      when(dseGraphPersistenceFactoryApi.updateGroup(userId, group)) thenReturn true
      val result = detectRequestAndPerform(command, query, userId, "1231233214")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(inputJson))
      sub.expectNext(TextMessage.Strict(expectedOutput))
    }

    "send group removal failure response when user unable to remove a group" in {

      val groupId = getUUID()
      val userId = getUUID()
      val phoneNumber = "1234556"
      val inputJson =
        """{"action":"removeGroup","groupId":"""" + groupId + """"}"""
      val command = TestActorRef(new Actor {
        def receive = {
          case RemoveGroup(groupId: String, userId, phoneNumber: String) ⇒ {
            sender ! GroupRemovedFailure(groupId, (new Exception("Exception")))
          }
        }
      })
      val query = TestActorRef(new Actor {
        def receive = {
          case ValidateGroup(groupId, userId) =>
            sender() ! IsValidGroupAck(true)
        }
      })
      val expectedOutput = """{"resource":"removeGroup","status":"failed","errors":[{"id":"GRP-103","message":"Unable to remove group abcspok123 (generic error)"}],"data":{}}"""
      when(dseGraphPersistenceFactoryApi.isGroupExist(userId, groupId)) thenReturn true
      when(dseGraphPersistenceFactoryApi.removeGroup(groupId, userId)) thenReturn false
      val result = detectRequestAndPerform(command, query, userId, phoneNumber)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(inputJson))
      sub.expectNext(TextMessage.Strict(expectedOutput))
    }

    "send group not found response when group to be removed is not found" in {

      val groupId = getUUID()
      val userId = getUUID()
      val phoneNumber = "1234556"
      val inputJson =
        """{"action":"removeGroup","groupId":"""" + groupId + """"}"""
      val command = TestActorRef(new Actor {
        def receive = {
          case GroupRemove(groupId: String, userId: String) ⇒ {
            sender ! GroupRemovedFailure(groupId, (new Exception("Exception")))
          }
        }
      })
      val query = TestActorRef(new Actor {
        def receive = {
          case ValidateGroup(groupId, userId) =>
            sender() ! IsValidGroupAck(false)
        }
      })
      val expectedOutput = s"""{"resource":"removeGroup","status":"failed","errors":[{"id":"GRP-001","message":"Group $groupId not found"}],"data":{}}"""
      when(dseGraphPersistenceFactoryApi.isGroupExist(userId, groupId)) thenReturn false
      when(dseGraphPersistenceFactoryApi.removeGroup(userId, groupId)) thenReturn false
      val result = detectRequestAndPerform(command, query, userId, phoneNumber)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(inputJson))
      sub.expectNext(TextMessage.Strict(expectedOutput))
    }

    "send group updation failure response when user unable to update a group" in {
      val userId = getUUID()
      val groupId = getUUID()
      val group = Group(groupId, "TestTitle")
      val inputJson = """{"action":"updateGroup","groupId":"""" + groupId + """","title":"TestTitle"}"""
      val command = TestActorRef(new Actor {
        def receive = {
          case UpdateUserGroup(group: Group, phoneNumber: String, userId: String) ⇒ {
            sender ! GroupUpdateFailure(group, (new Exception("Exception")))
          }
        }
      })
      val query = TestActorRef(new Actor {
        def receive = {
          case ValidateGroup(groupId, userId) =>
            sender() ! IsValidGroupAck(true)
        }
      })
      val expectedOutput = """{"resource":"updateGroup","status":"failed","errors":[{"id":"GRP-102","message":"Unable updating group abcspok123 (generic error)"}],"data":{}}"""
      when(dseGraphPersistenceFactoryApi.isGroupExist(userId, groupId)) thenReturn true
      when(dseGraphPersistenceFactoryApi.updateGroup(userId, group)) thenReturn false
      val result = detectRequestAndPerform(command, query, userId, "123456")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(inputJson))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send group cannot be deleted response when user tries to delete the default group with id 0" in {

      val groupId = "0"
      val userId = getUUID()
      val phoneNumber = "1234556"
      val inputJson =
        """{"action":"removeGroup","groupId":"""" + groupId + """"}"""
      val command = TestActorRef(new Actor {
        def receive = {
          case GroupRemove(groupId: String, userId) ⇒ TextMessage(GROUP_CANNOT_BE_DELETED)
        }
      })
      val query = TestActorRef(new Actor {
        def receive = {
          case ValidateGroup(groupId, userId) =>
            sender() ! IsValidGroupAck(false)
        }
      })
      val expectedOutput = """{"resource":"removeGroup","status":"failed","errors":[{"id":"GRP-002","message":"This group cannot be deleted"}],"data":{}}"""
      val result = detectRequestAndPerform(command, query, userId, phoneNumber)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(inputJson))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send Title is too short response when user sends short title while updating a group" in {

      val userId = getUUID()
      val group = Group("12345", "title")
      val inputJson = """{"action":"updateGroup","groupId":"""" + group.id + """","title":""}"""
      val command = TestActorRef(new Actor {
        def receive = {
          case UpdateUserGroup(group: Group, phoneNumber: String, userId: String) ⇒ {
            sender ! GROUP_TITLE_SHORT
          }
        }
      })
      val query = TestActorRef(new Actor {
        def receive = {
          case ValidateGroup(groupId, userId) =>
            sender() ! IsValidGroupAck(false)
        }
      })
      val expectedOutput = """{"resource":"updateGroup","status":"failed","errors":[{"id":"RGX-014","message":"Title is too short"}],"data":{}}"""
      when(dseGraphPersistenceFactoryApi.isGroupExist(userId, group.id)) thenReturn true
      val result = detectRequestAndPerform(command, query, userId, "123456")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(inputJson))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send group not found response when group to be updated is not found" in {
      val userId = getUUID()
      val groupId = getUUID()
      val group = Group(groupId, "title")
      val inputJson = """{"action":"updateGroup","groupId":"""" + group.id + """","title":"TestTitle"}"""
      val command = TestActorRef(new Actor {
        def receive = {
          case UpdateUserGroup(group: Group, phoneNumber: String, userId: String) ⇒ {
            sender ! TextMessage(s"Group $groupId not found")
          }
        }
      })
      val query = TestActorRef(new Actor {
        def receive = {
          case ValidateGroup(groupId, userId) =>
            sender() ! IsValidGroupAck(false)
        }
      })
      val expectedOutput = s"""{"resource":"updateGroup","status":"failed","errors":[{"id":"GRP-001","message":"Group $groupId not found"}],"data":{}}"""
      when(dseGraphPersistenceFactoryApi.isGroupExist(userId, groupId)) thenReturn false
      val result = detectRequestAndPerform(command, query, userId, "123456")

      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(inputJson))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send add followers in group successful response when user add followers in group successfully" in {

      val expectedOutput = """{"resource":"addFollower","status":"success","errors":[],"data":{"message":"Follower(s)/contact(s) are added in group","invalidContacts":[],"invalidUserIds":[]}}"""
      val userId = UUID.randomUUID().toString
      val groupId = UUID.randomUUID().toString
      val userGroupJson =
        """
               {"action":"addFollower",
              "groupId":"""" + groupId + """",
              "userIds":["123","456"],
              "contacts": [{
               "name": "contact1",
               "phone": "1234565548"
                },
               {
               "name": "contact2",
               "phone": "1234567148"
               }]
              }
                                         """

      val command = TestActorRef(new Actor {
        def receive = {
          case AddFollowers(userGroup: UserGroup, phoneNumber: String, userId: String) ⇒ {
            sender ! AddFollowerInGroupSuccess(FOLLOWERS_ADDED_IN_GROUP, Nil, Nil)
          }
        }
      })
      val result = detectRequestAndPerform(command, command, userId, "1256455666")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(userGroupJson))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send Invalid json response when user add followers in group" in {

      val userId = UUID.randomUUID().toString
      val groupId = UUID.randomUUID().toString
      val userGroupJson =
        """
               {"action":"addFollower",
                "groupId":"""" + groupId + """",
              "userIds":["123","456"],
              "contacts": [{
               "wrong": "conact1",
               "phone": "1234565548"
                },
               {
               "name": "contact2",
               "phone": "1234567148"
               }]
              }
            """
      val expectedOutput = """{"resource":"addFollower","status":"failed","errors":[{"id":"PRS-001","message":"Invalid JSON"}],"data":{}}"""
      val command = TestActorRef(new Actor {
        def receive = {
          case AddFollowers(userGroup: UserGroup, phoneNumber: String, userId: String) ⇒ {
            sender ! AddFollowerInGroupSuccess(FOLLOWERS_ADDED_IN_GROUP, Nil, Nil)
          }
        }
      })
      val result = detectRequestAndPerform(command, command, userId, "1256455666")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(userGroupJson))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send generic error response when user add followers in group" in {

      val userId = UUID.randomUUID().toString
      val groupId = UUID.randomUUID().toString
      val userGroupJson =
        """
               {"action":"addFollower","groupId":"""" + groupId + """",
              "userIds":["123","456"],
              "contacts": [{
               "name": "conact1",
               "phone": "1234565548"
                },
               {
               "name": "contact2",
               "phone": "1234567148"
               }]
              }
            """
      val expectedOutput = """{"resource":"addFollower","status":"failed","errors":[{"id":"GRP-105","message":"Exception   invalidContact: List(1234565548, 1234567148)  invalidUserIds: List(123, 456)"}],"data":{}}"""

      val command = TestActorRef(new Actor {
        def receive = {
          case AddFollowers(userGroup: UserGroup, phoneNumber: String, userId: String) ⇒ {
            sender ! AddFollowerInGroupFailure(userGroup, GRP_105, (new Exception("Exception")))
          }
        }
      })
      val result = detectRequestAndPerform(command, command, userId, "1256455666")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(userGroupJson))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send remove followers from group successful response when user remove followers from group successfully" in {

      val userId = UUID.randomUUID().toString
      val groupId = UUID.randomUUID().toString
      val expectedOutput = """{"resource":"removeFollower","status":"success","errors":[],"data":{"message":"Follower(s)/contact(s) are removed from group","invalidContacts":[],"invalidUserIds":[]}}"""

      val removeUserGroupJson =
        """
               {"action":"removeFollower","groupId":"""" + groupId + """",
              "userIds":["1234","4566"],
              "phones": ["+1223455648","+1245784578"]
              }
                                                                     """
      val command = TestActorRef(new Actor {
        def receive = {
          case RemoveFollowers(removeUserGroup: RemoveUserGroup, phoneNumber: String, userId: String) ⇒ {
            sender ! RemoveFollowerInGroupSuccess(FOLLOWERS_REMOVED_IN_GROUP, Nil, Nil)
          }
        }
      })
      val result = detectRequestAndPerform(command, command, userId, "1256455666")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(removeUserGroupJson))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send invalid json response when user try to remove followers from group" in {

      val expectedOutput = """{"resource":"removeFollower","status":"failed","errors":[{"id":"PRS-001","message":"Invalid JSON"}],"data":{}}"""
      val userId = UUID.randomUUID().toString
      val groupId = UUID.randomUUID().toString
      val removeUserGroupJson =
        """
               {"action":"removeFollower","groupId":"""" + groupId + """",
              "wrong":["1234","4566"],
              "phones": ["+1223455648","+1245784578"]
              }
                                                                     """
      val command = TestActorRef(new Actor {
        def receive = {
          case RemoveFollowers(removeUserGroup: RemoveUserGroup, phoneNumber: String, userId: String) ⇒ {
            sender ! INVALID_JSON
          }
        }
      })
      val result = detectRequestAndPerform(command, command, userId, "1256455666")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(removeUserGroupJson))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send invalid json response when user try to remove followers from without group id" in {

      val expectedOutput = """{"resource":"removeFollower","status":"failed","errors":[{"id":"GRP-001","message":"Group Id not found"}],"data":{}}"""
      val userId = UUID.randomUUID().toString
      val groupId = UUID.randomUUID().toString
      val removeUserGroupJson =
        """
               {"action":"removeFollower",
              "wrong":["1234","4566"],
              "phones": ["+1223455648","+1245784578"]
              }
                                                                     """
      val command = TestActorRef(new Actor {
        def receive = {
          case RemoveFollowers(removeUserGroup: RemoveUserGroup, phoneNumber: String, userId: String) ⇒ {
            sender ! INVALID_JSON
          }
        }
      })
      val result = detectRequestAndPerform(command, command, userId, "1256455666")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(removeUserGroupJson))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send error response when user try to remove followers from group and a generic error occurs" in {

      val userId = UUID.randomUUID().toString
      val groupId = UUID.randomUUID().toString
      val removeUserGroupJson =
        """
               {"action":"removeFollower","groupId":"""" + groupId + """",
              "userIds":["1234","4566"],
              "phones": ["+1223455648","+1245784578"]
              }"""
      val command = TestActorRef(new Actor {
        def receive = {
          case RemoveFollowers(removeUserGroup: RemoveUserGroup, phoneNumber: String, userId: String) ⇒ {
            sender ! RemoveFollowerInGroupFailure(removeUserGroup, new Exception(s"Unable removing user(s) or contact(s) from group ${removeUserGroup.groupId} (generic error)."))
          }
        }
      })
      val expectedOutput = s"""{"resource":"removeFollower","status":"failed","errors":[{"id":"GRP-106","message":"Unable removing user(s) or contact(s) from group $groupId (generic error)."}],"data":{}}"""
      val result = detectRequestAndPerform(command, command, userId, "1256455666")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(removeUserGroupJson))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send error response when user try to remove followers from group with id 0 (default group)" in {

      val userId = UUID.randomUUID().toString
      val groupId = UUID.randomUUID().toString
      val removeUserGroupJson =
        """
               {"action":"removeFollower","groupId":"0",
              "userIds":["1234","4566"],
              "phones": ["+1223455648","+1245784578"]
              }"""
      val command = TestActorRef(new Actor {
        def receive = {
          case RemoveFollowers(removeUserGroup: RemoveUserGroup, phoneNumber: String, userId: String) ⇒ {
            sender ! RemoveFollowerInGroupSuccess("Unable removing user(s) or contact(s) from group 0 (generic error).", List("+1223455648", "+1245784578"), List("1234", "4566"))
          }
        }
      })
      val expectedOutput = """{"resource":"removeFollower","status":"failed","errors":[{"id":"GRP-106","message":"Unable removing user(s) or contact(s) from group 0 (generic error)."}],"data":{}}"""
      val result = detectRequestAndPerform(command, command, userId, "1256455666")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(removeUserGroupJson))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send success message if user profile is updated successfully when give only nickname" in {
      //val userId = UUID.randomUUID().toString
      val userV = DseGraphFactory.dseConn.executeGraph(s"""g.V().hasLabel('$USER').has('$USER_ID','$cyrilId')""").one().asVertex()
      val command = TestActorRef(new Actor {
        def receive = {
          case UpdateProfile(phoneNumber: String, userId: String, userProfile: UserProfile) ⇒ {
            sender ! UserProfileUpdateSuccess(USER_PROFILE_UPDATE_SUCCESS)
          }
        }
      })
      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(targetId) ⇒ {
            sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
          }
        }
      })
      when(dseGraphPersistenceFactoryApi.isUniqueNickname("Cyril")) thenReturn false
      val expectedOutput = """{"resource":"updateUserProfile","status":"success","errors":[],"data":{"message":"User profile updated successfully"}}"""
      val result = Await.result(updateUserProfileHandler(command, query, cyrilId, "1256455666", Some("Cyril"), None, None, None, None, None, None, None, None), 5 second)
      assert(result.equals(expectedOutput))
    }

    "send success message if user profile is updated successfully when give only birthDate" in {
      val userV = DseGraphFactory.dseConn.executeGraph(s"""g.V().hasLabel('$USER').has('$USER_ID','$cyrilId')""").one().asVertex()
      val command = TestActorRef(new Actor {
        def receive = {
          case UpdateProfile(phoneNumber: String, userId: String, userProfile: UserProfile) ⇒ {
            sender ! UserProfileUpdateSuccess(USER_PROFILE_UPDATE_SUCCESS)
          }
        }
      })
      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(targetId) ⇒ {
            sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
          }
        }
      })

      when(dseGraphPersistenceFactoryApi.isUniqueNickname("Cyril")) thenReturn false
      val expectedOutput = """{"resource":"updateUserProfile","status":"success","errors":[],"data":{"message":"User profile updated successfully"}}"""
      val result = Await.result(updateUserProfileHandler(command, query, cyrilId, "1256455666", None, Some("1994-01-18"), None, None, None, None, None, None, None), 5 second)
      assert(result.equals(expectedOutput))
    }

    "send success message if user profile is updated successfully when give only gender" in {
      //val userId = UUID.randomUUID().toString
      val userV = DseGraphFactory.dseConn.executeGraph(s"""g.V().hasLabel('$USER').has('$USER_ID','$cyrilId')""").one().asVertex()
      val command = TestActorRef(new Actor {
        def receive = {
          case UpdateProfile(phoneNumber: String, userId: String, userProfile: UserProfile) ⇒ {
            sender ! UserProfileUpdateSuccess(USER_PROFILE_UPDATE_SUCCESS)
          }
        }
      })
      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(targetId) ⇒ {
            sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
          }
        }
      })
      when(dseGraphPersistenceFactoryApi.isUniqueNickname("Cyril")) thenReturn false
      val expectedOutput = """{"resource":"updateUserProfile","status":"success","errors":[],"data":{"message":"User profile updated successfully"}}"""
      val result = Await.result(updateUserProfileHandler(command, query, cyrilId, "1256455666", None, Some("1994-01-18"), Some("male"), None, None, None, None, None, None), 5 second)
      assert(result.equals(expectedOutput))
    }

    "send success message if user profile is updated successfully when give only geoText" in {
      //val userId = UUID.randomUUID().toString
      val userV = DseGraphFactory.dseConn.executeGraph(s"""g.V().hasLabel('$USER').has('$USER_ID','$cyrilId')""").one().asVertex()
      val command = TestActorRef(new Actor {
        def receive = {
          case UpdateProfile(phoneNumber: String, userId: String, userProfile: UserProfile) ⇒ {
            sender ! UserProfileUpdateSuccess(USER_PROFILE_UPDATE_SUCCESS)
          }
        }
      })
      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(targetId) ⇒ {
            sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
          }
        }
      })
      when(dseGraphPersistenceFactoryApi.isUniqueNickname("Cyril")) thenReturn false
      val expectedOutput = """{"resource":"updateUserProfile","status":"success","errors":[],"data":{"message":"User profile updated successfully"}}"""
      val result = Await.result(updateUserProfileHandler(command, query, cyrilId, "1256455666", None, Some("1994-01-18"), Some("male"), None, None, None, Some("this is geoText"), None, None), 5 second)
      assert(result.equals(expectedOutput))
    }

    "send success message if user profile is updated successfully when give only geoLat" in {
      //val userId = UUID.randomUUID().toString
      val userV = DseGraphFactory.dseConn.executeGraph(s"""g.V().hasLabel('$USER').has('$USER_ID','$cyrilId')""").one().asVertex()
      val command = TestActorRef(new Actor {
        def receive = {
          case UpdateProfile(phoneNumber: String, userId: String, userProfile: UserProfile) ⇒ {
            sender ! UserProfileUpdateSuccess(USER_PROFILE_UPDATE_SUCCESS)
          }
        }
      })
      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(targetId) ⇒ {
            sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
          }
        }
      })
      when(dseGraphPersistenceFactoryApi.isUniqueNickname("Cyril")) thenReturn false
      val expectedOutput = """{"resource":"updateUserProfile","status":"success","errors":[],"data":{"message":"User profile updated successfully"}}"""
      val result = Await.result(updateUserProfileHandler(command, query, cyrilId, "1256455666", None, Some("1994-01-18"), Some("male"), Some(30.5), None, None, None, None, None), 5 second)
      assert(result.equals(expectedOutput))
    }

    "send success message if user profile is updated successfully when give only geoLong" in {
      //val userId = UUID.randomUUID().toString
      val userV = DseGraphFactory.dseConn.executeGraph(s"""g.V().hasLabel('$USER').has('$USER_ID','$cyrilId')""").one().asVertex()
      val command = TestActorRef(new Actor {
        def receive = {
          case UpdateProfile(phoneNumber: String, userId: String, userProfile: UserProfile) ⇒ {
            sender ! UserProfileUpdateSuccess(USER_PROFILE_UPDATE_SUCCESS)
          }
        }
      })
      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(targetId) ⇒ {
            sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
          }
        }
      })
      when(dseGraphPersistenceFactoryApi.isUniqueNickname("Cyril")) thenReturn false
      val expectedOutput = """{"resource":"updateUserProfile","status":"success","errors":[],"data":{"message":"User profile updated successfully"}}"""
      val result = Await.result(updateUserProfileHandler(command, query, cyrilId, "1256455666", None, Some("1994-01-18"), Some("male"), Some(30.5), Some(30.6), None, None, None, None), 5 second)
      assert(result.equals(expectedOutput))
    }

    "send success message if user profile is updated successfully when give only geoElev" in {
      //val userId = UUID.randomUUID().toString
      val userV = DseGraphFactory.dseConn.executeGraph(s"""g.V().hasLabel('$USER').has('$USER_ID','$cyrilId')""").one().asVertex()
      val command = TestActorRef(new Actor {
        def receive = {
          case UpdateProfile(phoneNumber: String, userId: String, userProfile: UserProfile) ⇒ {
            sender ! UserProfileUpdateSuccess(USER_PROFILE_UPDATE_SUCCESS)
          }
        }
      })
      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(targetId) ⇒ {
            sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
          }
        }
      })
      when(dseGraphPersistenceFactoryApi.isUniqueNickname("Cyril")) thenReturn false
      val expectedOutput = """{"resource":"updateUserProfile","status":"success","errors":[],"data":{"message":"User profile updated successfully"}}"""
      val result = Await.result(updateUserProfileHandler(command, query, cyrilId, "1256455666", None, Some("1994-01-18"), Some("male"), Some(30.5), Some(30.6), Some(30.72), None, None, None), 5 second)
      assert(result.equals(expectedOutput))
    }

    "send error message if Suspended user tries to update his profile" in {
      val userV = DseGraphFactory.dseConn.executeGraph(s"""g.V().hasLabel('$USER').has('$USER_ID','$cyrilId')""").one().asVertex()
      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(targetId) ⇒ {
            sender ! IsUserSuspendedAsk(SPOKER_SUSPENDED)
          }
        }
      })
      val expectedOutput = """{"resource":"updateUserProfile","status":"failed","errors":[{"id":"SPK-014","message":"Action impossible for a suspended account"}],"data":{}}"""
      val result = Await.result(updateUserProfileHandler(query, query, cyrilId, "1256455666", Some("Cyril"), None, None, None, None, None, None, None, None), 5 second)
      assert(result.equals(expectedOutput))
    }

    "send error message if Supended property not found" in {
      val userV = DseGraphFactory.dseConn.executeGraph(s"""g.V().hasLabel('$USER').has('$USER_ID','$cyrilId')""").one().asVertex()
      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(targetId) ⇒ {
            sender ! IsUserSuspendedAsk(PROPERTY_NOT_FOUND)
          }
        }
      })
      val expectedOutput = """{"resource":"updateUserProfile","status":"failed","errors":[{"id":"MYA-101","message":"Unable updating profile (generic error)"}],"data":{}}"""
      val result = Await.result(updateUserProfileHandler(query, query, cyrilId, "1256455666", Some("Cyril"), None, None, None, None, None, None, None, None), 5 second)
      assert(result.equals(expectedOutput))
    }

    "send error message if user profile validation gives an error when the user tries to update profile" in {
      val userId = UUID.randomUUID().toString
      val command = TestActorRef(new Actor {
        def receive = {
          case UpdateProfile(phoneNumber: String, userId: String, userProfile: UserProfile) ⇒ {
            sender ! INVALID_LATITUDE
          }
        }
      })
      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(targetId) ⇒ {
            sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
          }
        }
      })
      val expectedOutput = """{"resource":"updateUserProfile","status":"failed","errors":[{"id":"GEO-001","message":"Invalid Latitude"}],"data":{}}"""
      val result = Await.result(updateUserProfileHandler(command, query, cyrilId, "1256455666", None, Some("1994-01-18"), Some("male"), Some(3000000.5), Some(30.6), Some(30.72), None, None, None), 5 second)
      assert(result.equals(expectedOutput))
    }

    "send error message if user profile fails to get updated" in {
      val userId = UUID.randomUUID().toString
      val command = TestActorRef(new Actor {
        def receive = {
          case UpdateProfile(phoneNumber: String, userId: String, userProfile: UserProfile) ⇒ {
            sender ! UserProfileUpdateFailure(new Exception(USER_PROFILE_UPDATE_GENERIC_ERROR), "MYA-101")
          }
        }
      })
      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(targetId) ⇒ {
            sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
          }
        }
      })
      when(dseGraphPersistenceFactoryApi.isUniqueNickname("Cyril")) thenReturn false
      val expectedOutput = s"""{"resource":"updateUserProfile","status":"failed","errors":[{"id":"MYA-101","message":"Unable updating profile (generic error)"}],"data":{}}"""
      val result = Await.result(updateUserProfileHandler(command, query, cyrilId, "1256455666", None, Some("1994-01-18"), Some("male"), Some(30.5), Some(30.6), Some(30.72), None, None, None), 5 second)
      assert(result.equals(expectedOutput))
    }

    "send user id not found when user tries to view a User's minimal details by its id" in {

      val userId = getUUID()
      val targetUserId = getUUID()
      val userMinimalDetailsResponse = new Exception(s"User $targetUserId not found")
      val query = TestActorRef(new Actor {
        def receive = {
          case ViewUserMinimalDetails(targetUserId) =>
            sender() ! ViewUserMinimalDetailsFailureResponse(userMinimalDetailsResponse, "USR-001")
        }
      })
      val result = Await.result(viewShortDetail(query, targetUserId, userId), 5 second)
      val output = s"""{"resource":"$VIEW_MINIMAL_DETAILS","status":"failed","errors":[{"id":"USR-001","message":"User $targetUserId not found"}],"data":{}}"""
      assert(result === output)
    }

    "send User's minimal details when user tries to view a User's minimal details by its id successfully" in {

      val dateNew: Date = new java.util.Date()
      val userId = getUUID()
      val targetUserId = getUUID()
      val userMinimalDetailsResponse = UserMinimalDetailsResponse(targetUserId, "Dhiru", "male", "Dhiru.jpg")
      val query = TestActorRef(new Actor {
        def receive = {
          case ViewUserMinimalDetails(targetUserId) =>
            sender() ! ViewUserMinimalDetailsSuccessResponse(userMinimalDetailsResponse)
        }
      })
      val result = Await.result(viewShortDetail(query, targetUserId, userId), 5 second)
      val output = s"""{"resource":"$VIEW_MINIMAL_DETAILS","status":"success","errors":[],"data":{"id":"$targetUserId","nickname":"Dhiru","gender":"male","picture":"Dhiru.jpg"}}"""
      assert(result === output)
    }

    "send User's full details by its id successfully" in {
      val userId = getUUID()
      val targetUserId = getUUID()
      val viewFullUserProfileDetails = UserProfileFullDetails(targetUserId, "prashant", "male", "piyush.jpg", "", 1, 1, 1, true, true)
      val query = TestActorRef(new Actor {
        def receive = {
          case GetUserProfileFullDetails(targetUserId: String, userId: String) ⇒ {
            sender ! UserProfileFullDetailsSuccess(viewFullUserProfileDetails)
          }
        }
      })
      val output = s"""{"resource":"$VIEW_FULL_DETAILS","status":"success","errors":[],"data":{"id":"$targetUserId","nickname":"prashant","gender":"male","picture":"piyush.jpg","cover":"","nbFollowers":1,"nbFollowing":1,"nbSpoks":1,"isFollower":true,"isFollowing":true}}"""
      val result = Await.result(viewFullDetail(query, targetUserId, userId), 5 second)
      assert(result === output)
    }

    " disable the account by admin" in {
      val json = """{
                 "action":"disableUser",
                 "targetUserId":"12345678"
                 }"""
      val userId = getUUID()
      val targetUserId = 12345678
      val uid = getUUID()

      val query = TestActorRef(new Actor {
        def receive = {
          case Disable(userId: String, targetUserId: String) ⇒ {
            sender ! DisableResponseSuccess("user disabled successfully")
          }
        }
      })
      val output = """{"resource":"disableUser","status":"success","errors":[],"data":{"message":"user disabled successfully"}}"""
      val result = Await.result(disableUser(query, uid, "12345678", json), 5 second)
      assert(result.getStrictText === output)
    }

    " not be able to disable the account of admin by another admin" in {
      val userId = getUUID()
      val targetUserId = getUUID()

      val query = TestActorRef(new Actor {
        def receive = {
          case Disable(userId: String, targetUserId: String) ⇒ {
            sender ! DisableResponseFailure(new Exception(s"Unable to disbale the account because $targetUserId is a Admin"), USR_107)
          }
        }
      })
      val output = s"""{"resource":"disableUser","status":"failed","errors":[{"id":"USR-107","message":"Unable to disbale the account because $targetUserId is a Admin"}],"data":{}}"""
      val result = Await.result(disableAccountOfUser(query, targetUserId, userId), 5 second)
      assert(result.getStrictText === output)
    }

    "not be able to disable by another user" in {
      val userId = getUUID()
      val targetUserId = getUUID()

      val query = TestActorRef(new Actor {
        def receive = {
          case Disable(userId: String, targetUserId: String) ⇒ {
            sender ! DisableResponseFailure(new Exception(s"User_Id $userId is not a Admin"), USR_106)
          }
        }
      })

      val output = s"""{"resource":"disableUser","status":"failed","errors":[{"id":"USR-106","message":"User_Id $userId is not a Admin"}],"data":{}}"""
      val result = Await.result(disableAccountOfUser(query, targetUserId, userId), 5 second)
      assert(result.getStrictText === output)
    }

    "able to disable it's account " in {
      val userId = getUUID()

      val query = TestActorRef(new Actor {
        def receive = {
          case DisableUser(userId: String) ⇒ {
            sender ! DisableResponseSuccess("user disabled successfully")
          }
        }
      })
      val output = """{"resource":"disableUser","status":"success","errors":[],"data":{"message":"user disabled successfully"}}"""
      val result = Await.result(disableAccountOfUserByHimeSelf(query, userId), 5 second)
      assert(result.getStrictText === output)
    }

    " Not be Able to suspend the account of user" in {
      val userId = getUUID()
      val targetUserId = getUUID()
      val phoneNumber = "1234567890"

      val command = TestActorRef(new Actor {
        def receive = {
          case Suspend(userId, targetUserId, phoneNumber) ⇒ {
            sender ! SuspendResponseFailure(new Exception(s"Unable to suspend the account because $targetUserId is a Admin"), USR_107)
          }
        }
      })
      val output = s"""{"resource":"suspendSpoker","status":"failed","errors":[{"id":"USR-107","message":"Unable to suspend the account because $targetUserId is a Admin"}],"data":{}}"""
      val result = Await.result(suspendAccountOfUser(command, targetUserId, userId, phoneNumber), 5 second)
      assert(result.getStrictText === output)
    }

    " be Able to suspend the account of user" in {
      val userId = getUUID()
      val targetUserId = getUUID()
      val phoneNumber = "1234567890"
      val command = TestActorRef(new Actor {
        def receive = {
          case Suspend(userId, targetUserId, phoneNumber) ⇒ {
            sender ! SuspendResponseSuccess("Spoker suspended successfully")
          }
        }
      })
      val output = """{"resource":"suspendSpoker","status":"success","errors":[],"data":{"message":"Spoker suspended successfully"}}"""
      val result = Await.result(suspendAccountOfUser(command, targetUserId, userId, phoneNumber), 5 second)
      assert(result.getStrictText === output)
    }

    " Not be Able to reactivate the account of user" in {
      val userId = getUUID()
      val targetUserId = getUUID()
      val phoneNumber = "1234567890"

      val command = TestActorRef(new Actor {
        def receive = {
          case Recativate(userId, targetUserId, phoneNumber) ⇒ {
            sender ! ReactivateResponseFailure(new Exception(s"Unable to recativate $targetUserId (generic error)"), ADM_105)
          }
        }
      })
      val output = s"""{"resource":"reactivateSpoker","status":"failed","errors":[{"id":"ADM-105","message":"Unable to recativate $targetUserId (generic error)"}],"data":{}}"""
      val result = Await.result(reactivateAccountOfUser(command, targetUserId, userId, phoneNumber), 5 second)
      assert(result.getStrictText === output)
    }

    " be Able to recativate the account of user" in {
      val userId = getUUID()
      val targetUserId = getUUID()
      val phoneNumber = "1234567890"
      val command = TestActorRef(new Actor {
        def receive = {
          case Recativate(userId, targetUserId, phoneNumber) ⇒ {
            sender ! ReactivatedResponseSuccess("Spoker recativated successfully")
          }
        }
      })
      val output = """{"resource":"reactivateSpoker","status":"success","errors":[],"data":{"message":"Spoker recativated successfully"}}"""
      val result = Await.result(reactivateAccountOfUser(command, targetUserId, userId, phoneNumber), 5 second)
      assert(result.getStrictText === output)
    }

    "send user id not found when user tries to view a User's full details by its id" in {
      val userId = getUUID()
      val userId1 = getUUID()
      val query = TestActorRef(new Actor {
        def receive = {
          case GetUserProfileFullDetails(targetUserId: String, userId: String) ⇒ {
            sender ! UserProfileFullDetailsFailure(new Exception(USER_NOT_FOUND), "USR-001")
          }
        }
      })
      val output = s"""{"resource":"$VIEW_FULL_DETAILS","status":"failed","errors":[{"id":"USR-001","message":"User not found"}],"data":{}}"""
      val result = Await.result(viewFullDetail(query, userId, userId1), 5 second)
      assert(result === output)
    }

    "send list of followers by user id successfully" in {
      val targetUserId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val userFollowers = UserFollowers("0", "2", List(com.spok.model.Account.Follow(targetUserId, "Prashant", "male", "picture.jpg")))
      val query = TestActorRef(new Actor {
        def receive = {
          case GetFollowers(userId: String, targetUserId: String, pos: String) ⇒ {
            sender ! FollowersResponseSuccess(userFollowers)
          }
        }
      })
      val output = s"""{"resource":"$GET_FOLLOWERS","status":"success","errors":[],"data":{"previous":"0","next":"2","followers":[{"id":"$targetUserId","nickname":"Prashant","gender":"male","picture":"picture.jpg"}]}}"""
      val result = Await.result(getUserFollowers(query, targetUserId, userId, "1"), 5 second)
      assert(result === output)
    }

    "send user id not found when user tries to get list of followers by user id" in {
      val targetUserId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val query = TestActorRef(new Actor {
        def receive = {
          case GetFollowers(userId: String, targetUserId: String, pos: String) ⇒ {
            sender ! FollowersResponseFailure(new Exception(s"User $targetUserId not found"), "USR-001")
          }
        }
      })
      val output = s"""{"resource":"$GET_FOLLOWERS","status":"failed","errors":[{"id":"USR-001","message":"User $targetUserId not found"}],"data":{}}"""
      val result = Await.result(getUserFollowers(query, targetUserId, userId, "1"), 5 second)
      assert(result === output)
    }

    "send list of followings by user id successfully" in {
      val targetUserId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val userFollowings = UserFollowings("0", "2", List(com.spok.model.Account.Follow(targetUserId, "Prashant", "male", "picture.jpg")))
      val query = TestActorRef(new Actor {
        def receive = {
          case GetFollowings(userId: String, targetUserId: String, pos: String) ⇒ {
            sender ! FollowingsResponseSuccess(userFollowings)
          }
        }
      })
      val output = s"""{"resource":"$GET_FOLLOWINGS","status":"success","errors":[],"data":{"previous":"0","next":"2","followings":[{"id":"$targetUserId","nickname":"Prashant","gender":"male","picture":"picture.jpg"}]}}"""
      val result = Await.result(getUserFollowings(query, targetUserId, userId, "1"), 5 second)
      assert(result === output)
    }

    "send user id not found when user tries to get list of followings by user id" in {
      val targetUserId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val query = TestActorRef(new Actor {
        def receive = {
          case GetFollowings(userId: String, targetUserId: String, pos: String) ⇒ {
            sender ! FollowingsResponseFailure(new Exception(s"User $targetUserId not found"), "USR-001")
          }
        }
      })
      val result = Await.result(getUserFollowings(query, targetUserId, userId, "1"), 5 second)
      val output = s"""{"resource":"$GET_FOLLOWINGS","status":"failed","errors":[{"id":"USR-001","message":"User $targetUserId not found"}],"data":{}}"""
      assert(result === output)
    }

    "send the details of all groups of a user successfully" in {

      val userId = UUID.randomUUID().toString
      val groupId = UUID.randomUUID().toString
      val userGroupDetails = UserGroupsDetails(groupId, "Spartians", List("John", "Mathew"), 6, 3, 3)
      val query = TestActorRef(new Actor {
        def receive = {
          case GetGroupDetailsForUser(userId: String, "") ⇒ {
            sender ! GetGroupDetailsForSuccess(GroupsResponse("", "", List(userGroupDetails)))
          }
        }
      })
      val output = s"""{"resource":"$GET_GROUPS","status":"success","errors":[],"data":{"previous":"","next":"","groups":[{"id":"$groupId","title":"Spartians","nickname":["John","Mathew"],"nbUsers":6,"followers":3,"contacts":3}]}}"""
      val result = Await.result(getDetailsOfGroupsForUser(query, userId, Some("")), 5 second)
      assert(result === output)
    }

    "send the error message when not able to send the details of all groups of a user successfully" in {

      val userId = UUID.randomUUID().toString
      val groupId = UUID.randomUUID().toString
      val query = TestActorRef(new Actor {
        def receive = {
          case GetGroupDetailsForUser(userId: String, "") ⇒ {
            sender ! GetGroupDetailsForFailure(new Exception(LOAD_GROUP_DETAILS_GENERIC_ERROR))
          }
        }
      })
      val output = s"""{"resource":"$GET_GROUPS","status":"failed","errors":[{"id":"GRP-104","message":"Unable listing groups (generic error)"}],"data":{}}"""
      val result = Await.result(getDetailsOfGroupsForUser(query, userId, Some("")), 5 second)
      assert(result === output)
    }

    "send success message if user phone number is updated successfully" in {
      val phoneNumbers = """{
                           "action":"updatePhoneStepOne",
                           "oldCountryCode":"+91",
                           "oldNumber":"9711234556",
                           "newCountryCode":"+91",
                           "newNumber":"9711235181"
                            }"""
      val userId = UUID.randomUUID().toString
      val command = TestActorRef(new Actor {
        def receive = {
          case ValidateNumber(phoneNumber: String, oldNumber: String, newNumber: String, userId: String) ⇒ {
            sender ! ValidatePhoneNumberSuccess("OTP has been sent to +919711235181.")
          }
        }
      })
      val expectedOutput = """{"resource":"updatePhoneStepOne","status":"success","errors":[],"data":{"message":"OTP has been sent to +919711235181."}}"""
      val result = detectRequestAndPerform(command, command, userId, "userid123")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(phoneNumbers))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send error message if get generic error while updating user phone number" in {
      val phoneNumbers = """{
                           "action":"updatePhoneStepOne",
                           "oldCountryCode":"+91",
                           "oldNumber":"9711234556",
                           "newCountryCode":"+91",
                           "newNumber":"9711235181"
                            }"""
      val userId = UUID.randomUUID().toString
      val command = TestActorRef(new Actor {
        def receive = {
          case ValidateNumber(phoneNumber: String, oldNumber: String, newNumber: String, userId: String) ⇒ {
            sender ! ValidatePhoneNumberFailure(new Exception(UNABLE_SENDING_OTP_GENERIC_ERROR), "IDT-106")
          }
        }
      })
      val expectedOutput = """{"resource":"updatePhoneStepOne","status":"failed","errors":[{"id":"IDT-106","message":"Unable sending changing phone number confirmation code (generic error)"}],"data":{}}"""
      val result = detectRequestAndPerform(command, command, userId, "userid123")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(phoneNumbers))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send error message if if the json to update user phone number is invalid" in {
      val phoneNumbers = """{
                           "action":"updatePhoneStepOne",
                           "oldCountryCode":"+91",
                           "oldNumber":"9711234556"
                           }"""
      val userId = UUID.randomUUID().toString
      val command = TestActorRef(new Actor {
        def receive = {
          case ValidateNumber(phoneNumber: String, oldNumber: String, newNumber: String, userId: String) ⇒ {
            sender ! INVALID_JSON
          }
        }
      })
      val expectedOutput = """{"resource":"updatePhoneStepOne","status":"failed","errors":[{"id":"PRS-001","message":"Invalid JSON"}],"data":{}}"""
      val result = detectRequestAndPerform(command, command, userId, "userid123")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(phoneNumbers))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send error message if user phone number validation gives an error while updating number" in {
      val phoneNumbers = """{
                           "action":"updatePhoneStepOne",
                           "oldCountryCode":"+91",
                           "oldNumber":"9711234556",
                           "newCountryCode":"+91",
                           "newNumber":"97112"
                            }"""
      val userId = UUID.randomUUID().toString
      val command = TestActorRef(new Actor {
        def receive = {
          case ValidateNumber(phoneNumber: String, oldNumber: String, newNumber: String, userId: String) ⇒ {
            sender ! INVALID_PHONE_NUMBERS
          }
        }
      })
      val expectedOutput = """{"resource":"updatePhoneStepOne","status":"failed","errors":[{"id":"RGX-001","message":"Invalid phone number"}],"data":{}}"""
      val result = detectRequestAndPerform(command, command, userId, "userid123")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(phoneNumbers))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send update setting successful response when user update his setttings successfully" in {
      val myAccountFollowsjson =
        """{
             "action":"followSettings",
             "followers" : false ,
             "following" : false
             }"""
      val userId = UUID.randomUUID().toString
      val command = TestActorRef(new Actor {
        def receive = {
          case UpdateUserSettings(userSetting: UserSetting, userId: String) ⇒ {
            sender ! FollowSettingUpdateSuccess(USER_SETTINGS_UPDATE_SUCCESS)
          }
        }
      })
      val expectedOutput = """{"resource":"followSettings","status":"success","errors":[],"data":{"message":"User Settings updated successfully"}}"""
      val result = detectRequestAndPerform(command, command, "userid123", "userid123")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(myAccountFollowsjson))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send update setting failure response when user fails to update his setttings " in {

      val myAccountFollowsjson =
        """{
             "action":"followSettings",
             "followers" : false ,
             "following" : false
             }"""
      val userId = UUID.randomUUID().toString
      val command = TestActorRef(new Actor {
        def receive = {
          case UpdateUserSettings(userSetting: UserSetting, userId: String) ⇒ {
            sender ! FollowSettingUpdateFailure(
              new Exception(FOLLOWS_SETTING_UPDATE_GENERIC_ERROR), MYA_105
            )
          }
        }
      })
      val expectedOutput = """{"resource":"followSettings","status":"failed","errors":[{"id":"MYA-105","message":"Unable updating follows setting (generic error)"}],"data":{}}"""
      val result = detectRequestAndPerform(command, command, "userid123", "userid123")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(myAccountFollowsjson))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send invalid json response when user try to update his setttings with invalid json as input " in {
      val myAccountFollowsjson =
        """{
             "action":"followSettings",
             "followers" : false ,
             "followings" : false
             }"""
      val userId = UUID.randomUUID().toString
      val command = TestActorRef(new Actor {
        def receive = {
          case UpdateUserSettings(userSetting: UserSetting, userId: String) ⇒ {
            sender ! INVALID_JSON
          }
        }
      })
      val expectedOutput = """{"resource":"followSettings","status":"failed","errors":[{"id":"PRS-001","message":"Invalid JSON"}],"data":{}}"""
      val result = detectRequestAndPerform(command, command, "userid123", "userid123")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(myAccountFollowsjson))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "return success if phone number updated successfully" in {

      val numberAndOtpInput =
        """{
      "action":"updatePhoneStepTwo",
      "phone_number":"912316531",
      "code":"1234"
      }
        """
      val otpToken = OtpAuthToken("1234", "912316531", new DateTime().plusMinutes(5), 0)
      val userId = getUUID()
      val query = TestActorRef(new Actor {
        def receive = {
          case GetOTPToken(phoneNumber: String) ⇒ {
            sender ! FindOtpTokenSuccess(otpToken)
          }
        }
      })
      val command = TestActorRef(new Actor {
        def receive = {
          case UpdateOtpToken(newOtpToken: OtpAuthToken) ⇒ {
            sender ! ValidateUserSuccess(("OTP has been updated."))
          }
          case UpdateNumber(userId: String, phoneNumber: String, newNumber: String) ⇒ {
            sender ! UpdatePhoneNumberSuccess(PHONE_NUMBER_UPDATED)
          }
        }
      })

      val expectedOutput = """{"resource":"updatePhoneStepTwo","status":"success","errors":[],"data":null}"""
      val result = detectRequestAndPerform(command, query, "userid123", "userid123")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(numberAndOtpInput))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "return generic error if phone number not updated" in {

      val numberAndOtpInput =
        """{
      "action":"updatePhoneStepTwo",
      "phone_number":"912316531",
      "code":"1234"
      }
        """
      val otpToken = OtpAuthToken("1234", "912316531", new DateTime().plusMinutes(5), 0)
      val userId = getUUID()
      val query = TestActorRef(new Actor {
        def receive = {
          case GetOTPToken(phoneNumber: String) ⇒ {
            sender ! FindOtpTokenSuccess(otpToken)
          }
        }
      })
      val command = TestActorRef(new Actor {
        def receive = {
          case UpdateOtpToken(newOtpToken: OtpAuthToken) ⇒ {
            sender ! ValidateUserSuccess(("OTP has been updated."))
          }
          case UpdateNumber(userId: String, phoneNumber: String, newNumber: String) ⇒ {
            sender ! UpdatePhoneNumberFailure(new Exception(UNABLE_CHANGING_PHONE_NUMBER), "IDT-107")
          }
        }
      })
      removeUserFromCache(query, userId, userId)
      val expectedOutput = """{"resource":"updatePhoneStepTwo","status":"failed","errors":[{"id":"IDT-107","message":"Unable changing phone number (generic error)"}],"data":{}}"""
      val result = detectRequestAndPerform(command, query, "userid123", "userid123")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(numberAndOtpInput))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "return json error while updating a user phone number" in {

      val numberAndOtpInput =
        """{
      "action":"updatePhoneStepTwo",
      "phone_number1":"912316531",
      "code1":"1234"
      }
        """
      val otpToken = OtpAuthToken("1234", "912316531", new DateTime().plusMinutes(5), 0)
      val userId = getUUID()
      val query = TestActorRef(new Actor {
        def receive = {
          case GetOTPToken(phoneNumber: String) ⇒ {
            sender ! FindOtpTokenSuccess(otpToken)
          }
        }
      })
      val command = TestActorRef(new Actor {
        def receive = {
          case UpdateOtpToken(newOtpToken: OtpAuthToken) ⇒ {
            sender ! ValidateUserSuccess(("OTP has been updated."))
          }
          case UpdateNumber(userId: String, phoneNumber: String, newNumber: String) ⇒ {
            sender ! UpdatePhoneNumberFailure(new Exception(UNABLE_CHANGING_PHONE_NUMBER), "IDT-107")
          }
        }
      })
      val expectedOutput = """{"resource":"updatePhoneStepTwo","status":"failed","errors":[{"id":"PRS-001","message":"Invalid JSON"}],"data":{}}"""
      val result = detectRequestAndPerform(command, query, "userid123", "userid123")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(numberAndOtpInput))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send help setting updated successful response when user updates help setting" in {

      val myAccountHelpjson =
        """{
             "action":"helpSettings"
             }"""
      val userId = UUID.randomUUID().toString
      val command = TestActorRef(new Actor {
        def receive = {
          case UpdateUserHelpSettings(userId) ⇒ {
            sender ! HelpSettingUpdateSuccess(HELP_SETTING_UPDATE_SUCCESS)
          }
        }
      })

      val expectedOutput = raw"""{"resource":"helpSettings","status":"success","errors":[],"data":{"message":"User help setting updated successfully"}}"""
      val result = detectRequestAndPerform(command, command, userId, "userid123")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(myAccountHelpjson))
      sub.expectNext(TextMessage(expectedOutput))

    }

    "send help setting updated Unsuccessful response when user gets error on updating help setting" in {

      val myAccountHelpjson =
        """{
             "action":"helpSettings"
             }"""
      val userId = UUID.randomUUID().toString
      val command = TestActorRef(new Actor {
        def receive = {
          case UpdateUserHelpSettings(userId) ⇒ {
            sender ! HelpSettingUpdateFailure(new Exception(HELP_SETTING_UPDATE_GENERIC_ERROR), "MYA-104")
          }
        }
      })
      val expectedOutput = s"""{"resource":"$HELP_SETTINGS","status":"failed","errors":[{"id":"MYA-104","message":"Unable updating help setting (generic error)"}],"data":{}}"""
      val result = detectRequestAndPerform(command, command, userId, "userid123")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(myAccountHelpjson))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send to view my details successfully" in {
      val userId = getUUID()
      val myDetails = MyDetails(userId, "prashant", "male", "piyush.jpg", "", 1, 1, 1)
      val query = TestActorRef(new Actor {
        def receive = {
          case GetMyDetails(userId: String) ⇒ {
            sender ! MyDetailsSuccess(myDetails)
          }
        }
      })
      val output = s"""{"resource":"$VIEW_MY_DETAILS","status":"success","errors":[],"data":{"id":"$userId","nickname":"prashant","gender":"male","picture":"piyush.jpg","cover":"","nbFollowers":1,"nbFollowing":1,"nbSpoks":1}}"""
      val result = Await.result(viewMyDetail(query, userId), 5 second)
      assert(result === output)
    }

    "send to view my details Failure" in {
      val userId = getUUID()
      val query = TestActorRef(new Actor {
        def receive = {
          case GetMyDetails(userId: String) ⇒ {
            sender ! MyDetailsFailure(new Exception(USER_PROFILE_LOADING_GENERIC_ERROR), MYA_104)
          }
        }
      })
      val output = s"""{"resource":"$VIEW_MY_DETAILS","status":"failed","errors":[{"id":"MYA-104","message":"Unable loading user (generic error)"}],"data":{}}"""
      val result = Await.result(viewMyDetail(query, userId), 5 second)
      assert(result === output)
    }

    "send success message when successfully support request is handled" in {

      val message = "Where is the Respok button ?"
      val myAccountHelpjson =
        """{
          "action":"support",
          "message":"Where is the Respok button ?"
          }"""
      val userId = UUID.randomUUID().toString
      val command = TestActorRef(new Actor {
        def receive = {
          case AskSupport(someUserId, somePhoneNumber, someMessage) ⇒ {
            sender ! SupportProvidedSuccess(userId, "9988776655", message)
          }
        }
      })

      val expectedOutput = s"""{"resource":"support","status":"success","errors":[],"data":{"message":"Where is the Respok button ?"}}"""
      val result = detectRequestAndPerform(command, command, userId, "9988776655")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(myAccountHelpjson))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send error message when message for support is not valid" in {

      val myAccountHelpjson =
        """{
          "action":"support",
          "message":"Hey"
          }"""
      val userId = UUID.randomUUID().toString
      val command = TestActorRef(new Actor {
        def receive = {
          case AskSupport(someUserId, somePhoneNumber, someMessage) ⇒ {
            sender ! INVALID_SUPPORT_MESSAGE
          }
        }
      })

      val expectedOutput = s"""{"resource":"support","status":"failed","errors":[{"id":"IDT-008","message":"Invalid support message."}],"data":{}}"""
      val result = detectRequestAndPerform(command, command, userId, "9988776655")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(myAccountHelpjson))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send json error message when support message field is not present in the json" in {

      val myAccountHelpjson =
        """{
          "action":"support"
          }"""
      val userId = UUID.randomUUID().toString
      val command = TestActorRef(new Actor {
        def receive = {
          case AskSupport(someUserId, somePhoneNumber, someMessage) ⇒ {
            sender ! INVALID_JSON
          }
        }
      })

      val expectedOutput = s"""{"resource":"support","status":"failed","errors":[{"id":"PRS-001","message":"Invalid JSON"}],"data":{}}"""
      val result = detectRequestAndPerform(command, command, userId, "9988776655")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(myAccountHelpjson))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send the message when account suspend successfully" in {

      val numberInput = """{
                 "action":"suspendSpoker",
                 "spokerId":"12345678"
                 }"""

      val command = TestActorRef(new Actor {
        def receive = {
          case Suspend(userId, targetUserId, phoneNumber) ⇒ {
            sender ! SuspendResponseSuccess("Spoker suspended successfully")
          }
        }
      })
      val successOutput = """{"resource":"suspendSpoker","status":"success","errors":[],"data":{"message":"Spoker suspended successfully"}}"""
      val result = detectRequestAndPerform(command, command, cyrilId, "12345678")

      val s = Source.single[String](numberInput)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Streamed(s))
      sub.expectNext(TextMessage.Strict(successOutput))
    }

    "send the message when account reactivated successfully" in {

      val numberInput = """{
                 "action":"reactivateSpoker",
                 "spokerId":"12345678"
                 }"""

      val command = TestActorRef(new Actor {
        def receive = {
          case Recativate(userId, targetUserId, phoneNumber) ⇒ {
            sender ! ReactivatedResponseSuccess("Spoker reactivated sucessfully")
          }
        }
      })
      val successOutput = """{"resource":"reactivateSpoker","status":"success","errors":[],"data":{"message":"Spoker reactivated sucessfully"}}"""
      val result = detectRequestAndPerform(command, command, cyrilId, "12345678")

      val s = Source.single[String](numberInput)
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Streamed(s))
      sub.expectNext(TextMessage.Strict(successOutput))
    }

    "send error message when generic error occurs while user requests for support" in {

      val message = "Where is the Respok button ?"
      val myAccountHelpjson =
        """{
          "action":"support",
          "message":"Where is the Respok button ?"
          }"""
      val userId = UUID.randomUUID().toString
      val command = TestActorRef(new Actor {
        def receive = {
          case AskSupport(someUserId, somePhoneNumber, someMessage) ⇒ {
            sender ! SupportProvidedFailure(new Exception("Unable sending message to the support (generic error)."), "IDT-108")
          }
        }
      })
      val expectedOutput = s"""{"resource":"support","status":"failed","errors":[{"id":"IDT-108","message":"Unable sending message to the support (generic error)."}],"data":{}}"""
      val result = detectRequestAndPerform(command, command, userId, "9988776655")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(myAccountHelpjson))
      sub.expectNext(TextMessage(expectedOutput))
    }

    "send the details of specific group to the user successfully" in {
      val userId = UUID.randomUUID().toString
      val user2Id = UUID.randomUUID().toString
      val groupId = UUID.randomUUID().toString
      val singleGroupDetails = SingleGroupDetails(groupId, "Dynamos", "0", "", 2, 1, 1,
        List(
          FollowerDetailsForSingleGroup("spoker", user2Id, "cyril", "male", ""),
          ContactDetailsForSingleGroup("contact", "kais", "6178453423")
        ))
      val query = TestActorRef(new Actor {
        def receive = {
          case GetSingleGroupDetails(someUserId, someGroupId, "1") ⇒ {
            sender ! GetSingleGroupDetailsSuccess(singleGroupDetails)
          }
        }
      })
      val output = s"""{"resource":"specificGroup","status":"success","errors":[],"data":{"id":"$groupId","title":"Dynamos","previous":"0","next":"","numberOfUsers":2,"numberOfContacts":1,"numberOfFollowers":1,"users":[{"type":"spoker","id":"$user2Id","nickname":"cyril","gender":"male","picture":""},{"type":"contact","nickname":"kais","phoneNumber":"6178453423"}]}}"""
      val result = Await.result(viewOneGroup(query, userId, groupId, Some("1")), 5 second)
      assert(result === output)
    }

    "send the details of specific group to the user successfully even if the position is not sent" in {
      val userId = UUID.randomUUID().toString
      val user2Id = UUID.randomUUID().toString
      val groupId = UUID.randomUUID().toString
      val singleGroupDetails = SingleGroupDetails(groupId, "Dynamos", "0", "", 2, 1, 1,
        List(
          FollowerDetailsForSingleGroup("spoker", user2Id, "cyril", "male", ""),
          ContactDetailsForSingleGroup("contact", "kais", "6178453423")
        ))
      val query = TestActorRef(new Actor {
        def receive = {
          case GetSingleGroupDetails(someUserId, someGroupId, "1") ⇒ {
            sender ! GetSingleGroupDetailsSuccess(singleGroupDetails)
          }
        }
      })
      val output = s"""{"resource":"specificGroup","status":"success","errors":[],"data":{"id":"$groupId","title":"Dynamos","previous":"0","next":"","numberOfUsers":2,"numberOfContacts":1,"numberOfFollowers":1,"users":[{"type":"spoker","id":"$user2Id","nickname":"cyril","gender":"male","picture":""},{"type":"contact","nickname":"kais","phoneNumber":"6178453423"}]}}"""
      val result = Await.result(viewOneGroup(query, userId, groupId, None), 5 second)
      assert(result === output)
    }

    "send the error if the specific group is not found" in {
      val userId = UUID.randomUUID().toString
      val user2Id = UUID.randomUUID().toString
      val groupId = UUID.randomUUID().toString
      val query = TestActorRef(new Actor {
        def receive = {
          case GetSingleGroupDetails(someUserId, someGroupId, "1") ⇒ {
            sender ! GetSingleGroupDetailsFailure(GRP_001, GROUP_NOT_FOUND)
          }
        }
      })
      val output = s"""{"resource":"specificGroup","status":"failed","errors":[{"id":"GRP-001","message":"Group not found"}],"data":{}}"""
      val result = Await.result(viewOneGroup(query, userId, groupId, Some("1")), 5 second)
      assert(result === output)
    }

    "send unfollow successful response and remove user from redis when user unfollow an user successfully" in {
      val followUnfollow = FollowUnfollow("919582311059", "userid123", "userid321")
      val command = TestActorRef(new Actor {
        def receive = {
          case FollowUnfollowAction(followUnfollow: FollowUnfollow) ⇒ {
            sender ! FollowUnfollowSuccess(UNFOLLOWS)
          }
        }
      })
      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(targetId) ⇒ {
            sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
          }
        }
      })
      val input = """{"action":"followUnfollow","followingId":"userid321"}"""
      when(dseGraphPersistenceFactoryApi.performFollowOrUnfollow("919582311059", "userid123", "userid321")) thenReturn Some(UNFOLLOWS)
      when(redisFactory.isTalkExist("userid123", "userid321")) thenReturn Future(true)
      when(redisFactory.removeTalkId("userid123", "userid321")) thenReturn Future(1l)
      val result = detectRequestAndPerform(command, query, "userid123", "userid123")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(input))
      sub.expectNext(TextMessage("""{"resource":"followUnfollow","status":"success","errors":[],"data":{"unFollowResponse":{"userMobileNumber":"userid123","followerId":"userid123","followingId":"userid321"}}}"""))
    }

    "send unfollow successful response and not remove user from redis when user unfollow an user but not have a talk id successfully" in {
      val followUnfollow = FollowUnfollow("919582311059", "userid123", "userid321")
      val command = TestActorRef(new Actor {
        def receive = {
          case FollowUnfollowAction(followUnfollow: FollowUnfollow) ⇒ {
            sender ! FollowUnfollowSuccess(UNFOLLOWS)
          }
        }
      })

      val query = TestActorRef(new Actor {
        def receive = {
          case IsUserSuspended(targetId) ⇒ {
            sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
          }
        }
      })
      val input = """{"action":"followUnfollow","followingId":"userid321"}"""
      when(dseGraphPersistenceFactoryApi.performFollowOrUnfollow("919582311059", "userid123", "userid321")) thenReturn Some(UNFOLLOWS)
      when(redisFactory.isTalkExist("userid123", "userid321")) thenReturn Future(false)
      when(redisFactory.removeTalkId("userid123", "userid321")) thenReturn Future(0l)
      val result = detectRequestAndPerform(command, query, "userid123", "userid123")
      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Strict(input))
      sub.expectNext(TextMessage("""{"resource":"followUnfollow","status":"success","errors":[],"data":{"unFollowResponse":{"userMobileNumber":"userid123","followerId":"userid123","followingId":"userid321"}}}"""))
    }

    "send to fetch my details successfully" in {
      val userId = getUUID()
      val myDetails = LoggedUsersDetails(userId, "33", "7509779910", "Sat", "nickname", "gender", Geo(10.0, 10.0, 0.0), "india")
      val query = TestActorRef(new Actor {
        def receive = {
          case GetDetails(userId: String, None) ⇒ {
            sender ! DetailsSuccess(myDetails)
          }
        }
      })
      val output = s"""{"resource":"$GET_MY_PROFILE","status":"success","errors":[],"data":{"id":"$userId","countryCode":"33","phoneNumber":"7509779910","birthDate":"Sat","nickname":"nickname","gender":"gender","geo":{"latitude":10.0,"longitude":10.0,"elevation":0.0},"geoText":"india"}}"""
      val result = Await.result(viewDetail(query, userId), 5 second)
      assert(result === output)
    }

    "send to fetch my details Failure" in {
      val userId = getUUID()
      val query = TestActorRef(new Actor {
        def receive = {
          case GetDetails(userId: String, None) ⇒ {
            sender ! DetailsFailure("Not Available", SYST_401)
          }
        }
      })
      val output = s"""{"resource":"$GET_MY_PROFILE","status":"failed","errors":[{"id":"SYST-401","message":"Not Available"}],"data":{}}"""
      val result = Await.result(viewDetail(query, userId), 5 second)
      assert(result === output)
    }

    "send to fetch user details by admin successfully" in {
      val userId = getUUID()
      val targetId = getUUID()
      val myDetails = LoggedUsersDetails(targetId, "33", "7509779910", "Sat", "nickname", "gender", Geo(10.0, 10.0, 0.0), "india")
      val spoker = Spoker(myDetails.id, myDetails.countryCode, myDetails.phoneNumber, myDetails.birthDate, myDetails.nickname, myDetails.gender, myDetails.geo, myDetails.geoText, "", "", Geo(1.1, 1.2, 1.3), "", "")

      val query = TestActorRef(new Actor {
        def receive = {
          case GetDetails(userId: String, Some(targetId)) ⇒ {
            sender ! DetailsByAdminSuccess(SpokeFullDetails(spoker))
          }
        }
      })
      val output =
        s"""{"resource":"getUserDetailsByAdmin","status":"success","errors":[],"data":{"spoker":{"id":"abcspok123","countryCode":"33","phoneNumber":"7509779910","birthDate":"Sat","nickname":"nickname","gender":"gender","geo":{"latitude":10.0,"longitude":10.0,"elevation":0.0},"geoText":"india","cover":"","last_activity":"","last_position":{"latitude":1.1,"longitude":1.2,"elevation":1.3},"picture":"","token":""}}}"""
      val result = Await.result(viewDetailByAdmin(query, userId, targetId), 5 second)
      assert(result === output)
    }

    "send to fetch user details by admin Failure" in {
      val userId = getUUID()
      val targetId = getUUID()
      val query = TestActorRef(new Actor {
        def receive = {
          case GetDetails(userId: String, Some(targetId)) ⇒ {
            sender ! DetailsByAdminFailure("Not Available", SYST_401)
          }
        }
      })
      val output = s"""{"resource":"$GET_USER_DETAILS_BY_ADMIN","status":"failed","errors":[{"id":"SYST-401","message":"Not Available"}],"data":{}}"""
      val result = Await.result(viewDetailByAdmin(query, userId, targetId), 5 second)
      assert(result === output)
    }

    "send user promote successfully message " in {
      val json = """{
                     "action" : "updateLevel" ,
                     "level"  : "admin" ,
                      "spokerId" : "123456789"
                  }"""
      val userId = getUUID()
      val spokerId = "123456789"

      val command = TestActorRef(new Actor {
        def receive = {
          case promotUser(userId: String, level: String, spokerId: String) ⇒ {
            sender ! PromotUserAccountSuccess(s"$spokerId level updated successfully")
          }
        }
      })
      val output = """{"resource":"updateLevel","status":"success","errors":[],"data":{"message":"123456789 level updated successfully"}}"""
      val result = Await.result(promoteUser(command, userId, "12345678", json), 5 second)
      assert(result.getStrictText === output)
    }

    "return the appropriate message when promote user" in {

      val numberInput = """{
                     "action" : "updateLevel" ,
                     "level"  : "admin" ,
                      "spokerId" : "ABCDE123"
                  }"""

      val successOutput = """{"resource":"updateLevel","status":"success","errors":[],"data":{"message":"ABCDE123 level updated successfully"}}"""
      val command = TestActorRef(new Actor {
        def receive = {
          case promotUser(userId: String, level: String, spokerId: String) ⇒ {
            sender ! PromotUserAccountSuccess(s"$spokerId level updated successfully")
          }
        }
      })
      val result = detectRequestAndPerform(command, command, cyrilId, "12345678")
      val s = Source.single[String](numberInput)

      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Streamed(s))
      sub.expectNext(TextMessage.Strict(successOutput))
    }

    "return the appropriate message when disable user account by admin" in {

      val numberInput = """{
                       "action":"disableUser",
                       "targetUserId":"12345678"
                   }"""
      val command = TestActorRef(new Actor {
        def receive = {
          case Disable(userId: String, targetUserId: String) ⇒ {
            sender ! DisableResponseSuccess("user disabled successfully")
          }
        }
      })
      val successOutput = """{"resource":"disableUser","status":"success","errors":[],"data":{"message":"user disabled successfully"}}"""
      val result = detectRequestAndPerform(command, command, cyrilId, "12345678")
      // val result = detectRegistrationRequestAndPerform(command, command)
      val s = Source.single[String](numberInput)

      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Streamed(s))
      sub.expectNext(TextMessage.Strict(successOutput))
    }

    "return the appropriate message when disable user account by himSelf" in {

      val numberInput = """{
                       "action":"myaccountdisable"
                   }"""

      val command = TestActorRef(new Actor {
        def receive = {
          case DisableUser(targetUserId: String) ⇒ {
            sender ! DisableResponseSuccess("user disabled successfully")
          }
        }
      })
      val successOutput = """{"resource":"disableUser","status":"success","errors":[],"data":{"message":"user disabled successfully"}}"""
      val result = detectRequestAndPerform(command, command, cyrilId, "12345678")
      val s = Source.single[String](numberInput)

      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Streamed(s))
      sub.expectNext(TextMessage.Strict(successOutput))
    }

    "return the error message when give invalid json" in {

      val numberInput = """{
                     'action' : "updateLevel" ,
                     "level"  : "admin" ,
                      "spokerId" : "ABCDE123"
                  }"""

      val successOutput = """{"resource":"ws://api.spok.me","status":"failed","errors":[{"id":"SYST-503","message":"Service unavailable."}],"data":{}}"""
      val command = TestActorRef(new Actor {
        def receive = {
          case promotUser(userId: String, level: String, spokerId: String) ⇒ {
            sender ! PromotUserAccountSuccess(s"$spokerId level updated successfully")
          }
        }
      })
      val result = detectRequestAndPerform(command, command, cyrilId, "12345678")
      val s = Source.single[String](numberInput)

      val (pub, sub) = TestSource.probe[Message]
        .via(result)
        .toMat(TestSink.probe[Message])(Keep.both)
        .run()
      sub.request(1)
      pub.sendNext(TextMessage.Streamed(s))
      sub.expectNext(TextMessage.Strict(successOutput))
    }
  }
}