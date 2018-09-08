package com.spok.accountsservice.service

import java.util.UUID

import akka.actor.{ ActorSystem, PoisonPill, Props }
import akka.testkit.{ ImplicitSender, TestKit }
import com.datastax.driver.dse.graph.Vertex
import com.rbmhtechnology.eventuate.ReplicationEndpoint
import com.rbmhtechnology.eventuate.ReplicationEndpoint._
import com.rbmhtechnology.eventuate.log.leveldb.LeveldbEventLog
import com.spok.model.Account._
import com.spok.model.SpokModel.{ Error, Geo, GroupsResponse }
import com.spok.model.{ Location, OtpAuthToken }
import com.spok.persistence.cassandra.CassandraProvider
import com.spok.persistence.factory.{ DSEGraphPersistenceFactoryApi, DSEUserSpokFactoryApi }
import com.spok.util.Constant._
import com.spok.util.JsonHelper
import org.joda.time.DateTime
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ BeforeAndAfterAll, MustMatchers, WordSpecLike }

import scala.concurrent.duration._
import com.spok.accountsservice.service.AccountActorCommand._
import com.spok.accountsservice.service.AccountActorSuccessReplies._
import com.spok.accountsservice.service.AccountViewCommands._
import com.spok.accountsservice.service.AccountSuccessViewReplies._
import com.spok.accountsservice.service.AccountViewFailureReplies._
import com.spok.persistence.dse.DseGraphFactory

class AccountViewSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
    with WordSpecLike with MustMatchers with BeforeAndAfterAll with JsonHelper with MockitoSugar {

  val userString = """{
                       "nickname":"Sonu",
                       "birthdate":"1992-10-25",
                       "location":{
                          "results" : [
                             {
                                "address_components" : [
                                   {
                                      "long_name" : "Noida",
                                      "short_name" : "Noida",
                                      "types" : ["locality", "political" ]
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
                       "gender":"male",
                       "contacts":["+91837588298","+91981896713"]
                       "phone_number":"+919582311051"
                       }
                   """
  val userLocation = (parse(userString) \ "location").extract[Location]

  val session = CassandraProvider.session

  def this() = this(ActorSystem("AccountViewSystem"))

  val endpoint = ReplicationEndpoint(id => LeveldbEventLog.props(logId = "AccountViewSpec"))(_system)
  val eventLog = endpoint.logs(DefaultLogName)
  val mockedDseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
  val mockedDSEUserSpokFactoryApi: DSEUserSpokFactoryApi = mock[DSEUserSpokFactoryApi]
  override def afterAll() {
    TestKit.shutdownActorSystem(system)
    session.get.close()
  }

  "A AccountView" must {
    "Fetch a OTP Token by using phone number" in {
      val id = UUID.randomUUID().toString
      val phoneNumber = "+919540608095"
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.isExistsMobileNo("919540608095")) thenReturn false
      actorRef ! ValidateUser(phoneNumber)
      expectMsgType[ValidateUserSuccess](10 seconds)
      actorRef ! PoisonPill
      val actorRef2 = system.actorOf(Props(new AccountView(endpoint.id, eventLog)))
      actorRef2 ! GetOTPToken(phoneNumber)
      expectMsgPF() {
        case FindOtpTokenSuccess(otpToken) => otpToken.otp.size mustBe 4
      }
    }

    "Fetch a OTP Token after sending to new number" in {
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val oldNumber = "+919540608066"
      val newNumber = "+919540608088"
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.isMobileNoExists("919540608066", userId)) thenReturn true
      when(mockedDseGraphPersistenceFactoryApi.isExistsMobileNo("919540608088")) thenReturn false
      actorRef ! ValidatePhoneNumber(oldNumber, newNumber, userId)
      expectMsgType[ValidatePhoneNumberSuccess](10 seconds)
      actorRef ! PoisonPill
      val actorRef2 = system.actorOf(Props(new AccountView(endpoint.id, eventLog)))
      actorRef2 ! GetOTPToken(newNumber)
      expectMsgPF() {
        case FindOtpTokenSuccess(otpToken) => otpToken.otp.size mustBe 4
      }
    }

    "Generic OTP error message" in {
      val id = UUID.randomUUID().toString
      val phoneNumber = "+91123456789"
      val actorRef2 = system.actorOf(Props(new AccountView(endpoint.id, eventLog)))
      actorRef2 ! GetOTPToken(phoneNumber)
      expectMsgType[FindOtpTokenFailure]
    }

    "Fetch validated user" in {
      val id = UUID.randomUUID().toString
      val phoneNumber = "+91123456789"
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      actorRef ! AllowUser(phoneNumber)
      val actorRef2 = system.actorOf(Props(new AccountView(endpoint.id, eventLog)))
      Thread.sleep(5000)
      actorRef2 ! GetValidUser(phoneNumber)
      expectMsgPF() {
        case result => result mustBe true
      }
      actorRef2 ! GetValidUser("phoneNumber")
      expectMsgPF() {
        case result => result mustBe false
      }
    }

    "Update OTP token" in {
      val id = UUID.randomUUID().toString
      val phoneNumber = "+919540608095"
      val otpToken = OtpAuthToken("12", phoneNumber, new DateTime().plusMinutes(10), 2)
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      actorRef ! UpdateOtpToken(otpToken)
      expectMsgType[UpdateOtpTokenSuccess](10 seconds)
      actorRef ! PoisonPill

      val actorRef2 = system.actorOf(Props(new AccountView(endpoint.id, eventLog)))
      actorRef2 ! GetOTPToken(phoneNumber)
      expectMsgPF() {
        case FindOtpTokenSuccess(otpToken) => otpToken.retryCount mustBe 2
      }
    }

    "OTP token cleared after successful validation" in {
      val id = UUID.randomUUID().toString
      val phoneNumber = "+919540608095"
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      actorRef ! ValidateUser(phoneNumber)
      expectMsgType[ValidateUserSuccess](10 seconds)
      actorRef ! ClearOtpToken(phoneNumber)
      actorRef ! PoisonPill

      val actorRef2 = system.actorOf(Props(new AccountView(endpoint.id, eventLog)))
      actorRef2 ! GetOTPToken(phoneNumber)
      expectMsgType[FindOtpTokenFailure]
    }

    "Validate a group correctly" in {

      val groupId = "group12345"
      val id = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountView(endpoint.id, eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.isGroupExist(id, groupId)) thenReturn true
      actorRef ! ValidateGroup(groupId, id)
      expectMsgType[IsValidGroupAck]

    }

    "Able to view a User's minimal details by its id successfully" in {

      val userId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountView(endpoint.id, eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      val userMinimalDetailsResponse = UserMinimalDetailsResponse(userId, "nickName", "gender", "picture")
      when(mockedDSEUserSpokFactoryApi.isExistsUser(userId)) thenReturn true
      when(mockedDseGraphPersistenceFactoryApi.getUserMinimalDetails(userId)) thenReturn Some(userMinimalDetailsResponse)
      actorRef ! ViewUserMinimalDetails(userId)
      expectMsgType[ViewUserMinimalDetailsSuccessResponse]
    }

    "Not able to view a User's minimal details by its id Failure" in {

      val userId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountView(endpoint.id, eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDSEUserSpokFactoryApi.isExistsUser(userId)) thenReturn true
      when(mockedDseGraphPersistenceFactoryApi.getUserMinimalDetails(userId)) thenReturn None
      actorRef ! ViewUserMinimalDetails(userId)
      expectMsgType[ViewUserMinimalDetailsFailureResponse]
    }

    "Not able to view a User's minimal details by its id if id is not exist in dse" in {

      val userId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountView(endpoint.id, eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDSEUserSpokFactoryApi.isExistsUser(userId)) thenReturn false
      actorRef ! ViewUserMinimalDetails(userId)
      expectMsgType[ViewUserMinimalDetailsFailureResponse]
    }

    "Able to view a User's full details by its id success case" in {
      val userId = UUID.randomUUID().toString
      val targetUserId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountView(endpoint.id, eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      val viewFullUserProfileDetails = UserProfileFullDetails(targetUserId, "prashant", "male", "piyush.jpg", "", 1, 1, 1, true, false)
      when(mockedDSEUserSpokFactoryApi.isExistsUser(targetUserId)) thenReturn true
      when(mockedDseGraphPersistenceFactoryApi.viewFullUserProfile(targetUserId, userId)) thenReturn Some(viewFullUserProfileDetails)
      actorRef ! GetUserProfileFullDetails(targetUserId, userId)
      expectMsgType[UserProfileFullDetailsSuccess]
    }

    "Not able to view a User's full details by its id Failure case" in {
      val userId = UUID.randomUUID().toString
      val targetUserId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountView(endpoint.id, eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      val viewFullUserProfileDetails = UserProfileFullDetails(targetUserId, "prashant", "male", "piyush.jpg", "", 1, 1, 1, false, false)
      when(mockedDSEUserSpokFactoryApi.isExistsUser(targetUserId)) thenReturn true
      when(mockedDseGraphPersistenceFactoryApi.viewFullUserProfile(targetUserId, userId)) thenReturn None
      actorRef ! GetUserProfileFullDetails(targetUserId, userId)
      expectMsgType[UserProfileFullDetailsFailure]
    }

    "Not able to view a User's full details by its id if id is not exist in dse" in {
      val userId = UUID.randomUUID().toString
      val targetUserId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountView(endpoint.id, eventLog) {
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDSEUserSpokFactoryApi.isExistsUser(targetUserId)) thenReturn false
      actorRef ! GetUserProfileFullDetails(targetUserId, userId)
      expectMsgType[UserProfileFullDetailsFailure]
    }

    "Able to get list of followers by user's id success case" in {
      val targetUserId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountView(endpoint.id, eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      val userFollowers = UserFollowers("0", "2", List(com.spok.model.Account.Follow(targetUserId, "Prashant", "male", "picture.jpg")))
      when(mockedDSEUserSpokFactoryApi.isExistsUser(targetUserId)) thenReturn true
      when(mockedDseGraphPersistenceFactoryApi.fetchUserSettings(targetUserId, "follower")) thenReturn true
      when(mockedDseGraphPersistenceFactoryApi.fetchFollowers(targetUserId, "1")) thenReturn Some(userFollowers)
      actorRef ! GetFollowers(userId, targetUserId, "1")
      expectMsgType[FollowersResponseSuccess]
    }

    "Able to disable the user account by User" in {

      val userId = UUID.randomUUID().toString

      val vertex: Vertex = DseGraphFactory.dseConn.executeGraph(s"""graph.addVertex(label,"$USER",'$USER_ID',"$userId") """).one().asVertex()

      val actorRef = system.actorOf(Props(new AccountView(endpoint.id, eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      //val userFollowers = UserFollowers("0", "2", List(com.spok.model.Account.Follow(targetUserId, "Prashant", "male", "picture.jpg")))
      when(mockedDseGraphPersistenceFactoryApi.fetchUserAccountDisableOrNot(userId)) thenReturn Some(vertex)
      when(mockedDseGraphPersistenceFactoryApi.disableUserAccount(userId, vertex)) thenReturn true
      actorRef ! DisableUser(userId)
      expectMsgType[DisableResponseSuccess]
    }

    "Not able to disable the user account by User it'self when user account disable already " in {

      val userId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountView(endpoint.id, eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.fetchUserAccountDisableOrNot(userId)) thenReturn None
      actorRef ! DisableUser(userId)
      expectMsgType[DisableResponseFailure]
    }

    "Able to disable the user account by Admin" in {

      val userId = UUID.randomUUID().toString
      val vertex: Vertex = DseGraphFactory.dseConn.executeGraph(s"""g.V().hasLabel('$USER').limit(1)""").one().asVertex()
      val targetUserId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountView(endpoint.id, eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.checkUserAdminOrNot(userId)) thenReturn Some(true)
      when(mockedDseGraphPersistenceFactoryApi.checkUserAdminOrNot(targetUserId)) thenReturn Some(false)
      when(mockedDseGraphPersistenceFactoryApi.fetchUserAccountDisableOrNot(targetUserId)) thenReturn Some(vertex)
      when(mockedDseGraphPersistenceFactoryApi.disableUserAccount(targetUserId, vertex)) thenReturn true
      actorRef ! Disable(userId, targetUserId)
      expectMsgType[DisableResponseSuccess]
    }

    " Not able to disable the Admin account by AnotherAdmin" in {

      val userId = UUID.randomUUID().toString
      // val vertex: Vertex = DseGraphFactory.dseConn.executeGraph(s"""g.V().hasLabel('$USER').limit(1)""").one().asVertex()
      val targetUserId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountView(endpoint.id, eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.checkUserAdminOrNot(userId)) thenReturn Some(true)
      when(mockedDseGraphPersistenceFactoryApi.checkUserAdminOrNot(targetUserId)) thenReturn Some(true)
      actorRef ! Disable(userId, targetUserId)
      expectMsgType[DisableResponseFailure]
    }

    " Not able to disable the user account by Admin when generic error comes to Find Target is Admin or Not" in {

      val userId = UUID.randomUUID().toString
      val targetUserId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountView(endpoint.id, eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.checkUserAdminOrNot(userId)) thenReturn Some(true)
      when(mockedDseGraphPersistenceFactoryApi.checkUserAdminOrNot(targetUserId)) thenReturn None
      actorRef ! Disable(userId, targetUserId)
      expectMsgType[DisableResponseFailure]
    }

    "not able to disable the user account by Admin when user account disable already" in {

      val userId = UUID.randomUUID().toString
      val targetUserId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountView(endpoint.id, eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.checkUserAdminOrNot(userId)) thenReturn Some(true)
      when(mockedDseGraphPersistenceFactoryApi.checkUserAdminOrNot(targetUserId)) thenReturn Some(false)
      when(mockedDseGraphPersistenceFactoryApi.fetchUserAccountDisableOrNot(targetUserId)) thenReturn None

      actorRef ! Disable(userId, targetUserId)
      expectMsgType[DisableResponseFailure]
    }

    "not able to disable the userAccount by Another user " in {

      val userId = UUID.randomUUID().toString
      val targetUserId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountView(endpoint.id, eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.checkUserAdminOrNot(userId)) thenReturn Some(false)
      actorRef ! Disable(userId, targetUserId)
      expectMsgType[DisableResponseFailure]
    }

    "not able to disable the userAccount when geniric error comes " in {

      val userId = UUID.randomUUID().toString
      val targetUserId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountView(endpoint.id, eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.checkUserAdminOrNot(userId)) thenReturn None
      actorRef ! Disable(userId, targetUserId)
      expectMsgType[DisableResponseFailure]
    }

    "Not able to get list of followers by user's id failure case" in {

      val targetUserId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountView(endpoint.id, eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDSEUserSpokFactoryApi.isExistsUser(targetUserId)) thenReturn true
      when(mockedDseGraphPersistenceFactoryApi.fetchUserSettings(targetUserId, "follower")) thenReturn true
      when(mockedDseGraphPersistenceFactoryApi.fetchFollowers(targetUserId, "1")) thenReturn None
      actorRef ! GetFollowers(userId, targetUserId, "1")
      expectMsgType[FollowersResponseFailure]
    }

    "Not able to get list of followers when user id doesn't exists" in {
      val targetUserId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountView(endpoint.id, eventLog) {
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDSEUserSpokFactoryApi.isExistsUser(targetUserId)) thenReturn false
      actorRef ! GetFollowers(userId, targetUserId, "1")
      expectMsgType[FollowersResponseFailure]
    }

    "Not able to get list of followers when setting is disabled" in {
      val targetUserId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountView(endpoint.id, eventLog) {
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDSEUserSpokFactoryApi.isExistsUser(targetUserId)) thenReturn true
      when(mockedDseGraphPersistenceFactoryApi.fetchUserSettings(targetUserId, "follower")) thenReturn false
      actorRef ! GetFollowers(userId, targetUserId, "1")
      expectMsgType[FollowersResponseFailure]
    }

    "Able to get list of followers of his account when setting is disabled" in {
      val targetUserId = UUID.randomUUID().toString
      val userId = targetUserId
      val actorRef = system.actorOf(Props(new AccountView(endpoint.id, eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      val userFollowers = UserFollowers("0", "2", List(com.spok.model.Account.Follow(targetUserId, "Prashant", "male", "picture.jpg")))
      when(mockedDSEUserSpokFactoryApi.isExistsUser(targetUserId)) thenReturn true
      when(mockedDseGraphPersistenceFactoryApi.fetchUserSettings(targetUserId, "follower")) thenReturn false
      when(mockedDseGraphPersistenceFactoryApi.fetchFollowers(targetUserId, "1")) thenReturn Some(userFollowers)
      actorRef ! GetFollowers(userId, targetUserId, "1")
      expectMsgType[FollowersResponseSuccess]
    }
    "Able to get list of followings by user's id success case" in {
      val targetUserId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountView(endpoint.id, eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      val userFollowings = UserFollowings("0", "2", List(com.spok.model.Account.Follow(targetUserId, "Prashant", "male", "picture.jpg")))
      when(mockedDSEUserSpokFactoryApi.isExistsUser(targetUserId)) thenReturn true
      when(mockedDseGraphPersistenceFactoryApi.fetchUserSettings(targetUserId, "followings")) thenReturn true
      when(mockedDseGraphPersistenceFactoryApi.fetchFollowings(targetUserId, "1")) thenReturn Some(userFollowings)
      actorRef ! GetFollowings(userId, targetUserId, "1")
      expectMsgType[FollowingsResponseSuccess]
    }

    "Not able to get list of followings by user's id failure case" in {
      val targetUserId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountView(endpoint.id, eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDSEUserSpokFactoryApi.isExistsUser(targetUserId)) thenReturn true
      when(mockedDseGraphPersistenceFactoryApi.fetchFollowings(targetUserId, "1")) thenReturn None
      actorRef ! GetFollowings(userId, targetUserId, "1")
      expectMsgType[FollowingsResponseFailure]
    }

    "Not able to get list of followings when user id doesn't exists" in {
      val targetUserId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountView(endpoint.id, eventLog) {
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDSEUserSpokFactoryApi.isExistsUser(targetUserId)) thenReturn false
      actorRef ! GetFollowings(userId, targetUserId, "1")
      expectMsgType[FollowingsResponseFailure]
    }

    "Not able to get list of followings when setting is disabled" in {
      val targetUserId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountView(endpoint.id, eventLog) {
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDSEUserSpokFactoryApi.isExistsUser(targetUserId)) thenReturn true
      when(mockedDseGraphPersistenceFactoryApi.fetchUserSettings(targetUserId, "followings")) thenReturn false
      actorRef ! GetFollowings(userId, targetUserId, "1")
      expectMsgType[FollowingsResponseFailure]
    }

    "Able to get list of followings of his Account when setting is disabled" in {
      val targetUserId = UUID.randomUUID().toString
      val userId = targetUserId
      val actorRef = system.actorOf(Props(new AccountView(endpoint.id, eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      val userFollowings = UserFollowings("0", "2", List(com.spok.model.Account.Follow(targetUserId, "Prashant", "male", "picture.jpg")))
      when(mockedDSEUserSpokFactoryApi.isExistsUser(targetUserId)) thenReturn true
      when(mockedDseGraphPersistenceFactoryApi.fetchUserSettings(targetUserId, "followings")) thenReturn false
      when(mockedDseGraphPersistenceFactoryApi.fetchFollowings(targetUserId, "1")) thenReturn Some(userFollowings)
      actorRef ! GetFollowings(userId, targetUserId, "1")
      expectMsgType[FollowingsResponseSuccess]
    }

    "Able to get details of all the groups of the user" in {

      val userId = UUID.randomUUID().toString
      val groupId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountView(endpoint.id, eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      val userGroupDetails = UserGroupsDetails(groupId, "Spartians", List("John", "Mathew"), 6, 3, 3)
      when(mockedDseGraphPersistenceFactoryApi.fetchGroupDetailsForAUser(userId, "")) thenReturn (Some(GroupsResponse("", "", List(userGroupDetails))))
      actorRef ! GetGroupDetailsForUser(userId, "")
      expectMsgType[GetGroupDetailsForSuccess]
    }

    "Able to send error message when get details of all the groups of the user" in {

      val userId = UUID.randomUUID().toString
      val groupId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountView(endpoint.id, eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
        // when(mockedDseGraphPersistenceFactoryApi.fetchGroupDetailsForAUser(userId , "")) thenReturn None
      }))
      val userGroupDetails = UserGroupsDetails(groupId, "Spartians", List("John", "Mathew"), 6, 3, 3)
      when(mockedDseGraphPersistenceFactoryApi.fetchGroupDetailsForAUser(userId, "")) thenReturn None
      actorRef ! GetGroupDetailsForUser(userId, "")
      expectMsgType[GetGroupDetailsForFailure]
    }

    "Able to view my details success case" in {
      val userId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountView(endpoint.id, eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      val myDetails = MyDetails(userId, "prashant", "male", "piyush.jpg", "", 1, 1, 1)
      when(mockedDseGraphPersistenceFactoryApi.viewMyProfile(userId)) thenReturn Some(myDetails)
      actorRef ! GetMyDetails(userId)
      expectMsgType[MyDetailsSuccess]
    }

    "Not able to view my details Failure case" in {
      val userId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountView(endpoint.id, eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      val myDetails = MyDetails(userId, "prashant", "male", "piyush.jpg", "", 1, 1, 1)
      when(mockedDseGraphPersistenceFactoryApi.viewMyProfile(userId)) thenReturn None
      actorRef ! GetMyDetails(userId)
      expectMsgType[MyDetailsFailure]
    }

    "Able to get details of one specific group of user" in {

      val userId = UUID.randomUUID().toString
      val groupId = UUID.randomUUID().toString
      val user2Id = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountView(endpoint.id, eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      val userGroupDetails = SingleGroupDetails(groupId, "Dynamos", "0", "", 2, 1, 1,
        List(
          FollowerDetailsForSingleGroup("spoker", user2Id, "cyril", "male", ""),
          ContactDetailsForSingleGroup("contact", "kais", "6178453423")
        ))
      when(mockedDseGraphPersistenceFactoryApi.getSingleGroupDetails(userId, groupId, "1")) thenReturn ((Some(userGroupDetails), None))
      actorRef ! GetSingleGroupDetails(userId, groupId, "1")
      expectMsgType[GetSingleGroupDetailsSuccess]
    }

    "Able to send group not found error message if the specific group is not found" in {

      val userId = UUID.randomUUID().toString
      val groupId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountView(endpoint.id, eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.getSingleGroupDetails(userId, groupId, "1")) thenReturn ((None, Some(Error(GRP_001, GROUP_NOT_FOUND))))
      actorRef ! GetSingleGroupDetails(userId, groupId, "1")
      expectMsgType[GetSingleGroupDetailsFailure]
    }

    "Able to get my details success case" in {
      val userId = UUID.randomUUID().toString
      val vertex: Vertex = DseGraphFactory.dseConn.executeGraph(s"""graph.addVertex(label,"$USER",'$USER_ID',"$userId") """).one().asVertex()
      val actorRef = system.actorOf(Props(new AccountView(endpoint.id, eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      val myDetails = LoggedUsersDetails(userId, "33", "7509779910", "Sat", "nickname", "gender", Geo(10.0, 10.0, 0.0), "india")
      when(mockedDseGraphPersistenceFactoryApi.fetchMyProfile(userId)) thenReturn ((Some(myDetails), None, Some(vertex)))
      actorRef ! GetDetails(userId, None)
      expectMsgType[DetailsSuccess]
    }

    "Not able to get my details Failure case" in {
      val userId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountView(endpoint.id, eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.fetchMyProfile(userId)) thenReturn ((None, Some(Error(SYST_401, "Not Available")), None))
      actorRef ! GetDetails(userId, None)
      expectMsgType[DetailsFailure]
    }

    "Able to get user details by admin success case" in {
      val userId = UUID.randomUUID().toString
      val targetId = UUID.randomUUID().toString
      val vertex: Vertex = DseGraphFactory.dseConn.executeGraph(s"""graph.addVertex(label,"$USER",'$USER_ID',"$targetId") """).one().asVertex()
      val actorRef = system.actorOf(Props(new AccountView(endpoint.id, eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      val myDetails = LoggedUsersDetails(targetId, "33", "7509779910", "Sat", "nickname", "gender", Geo(10.0, 10.0, 0.0), "india")
      val spokerDetails = SpokerFewDetails("", "lastActivity", Geo(1.1, 1.2, 1.3), "picture", "token")
      when(mockedDseGraphPersistenceFactoryApi.fetchMyProfile(targetId)) thenReturn ((Some(myDetails), None, Some(vertex)))
      when(mockedDseGraphPersistenceFactoryApi.checkUserAdminOrNot(userId)) thenReturn (Some(true))
      when(mockedDseGraphPersistenceFactoryApi.fetchUserInfo(targetId, vertex)) thenReturn ((Some(spokerDetails), None))
      when(mockedDseGraphPersistenceFactoryApi.fetchUserAccountDisableOrNot(targetId)) thenReturn ((Some(vertex)))
      actorRef ! GetDetails(userId, Some(targetId))
      expectMsgType[DetailsByAdminSuccess]
    }

    "Not be Able to get user details by user which is not admin" in {
      val userId = UUID.randomUUID().toString
      val targetId = UUID.randomUUID().toString
      val vertex: Vertex = DseGraphFactory.dseConn.executeGraph(s"""graph.addVertex(label,"$USER",'$USER_ID',"$targetId") """).one().asVertex()
      val actorRef = system.actorOf(Props(new AccountView(endpoint.id, eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      val myDetails = LoggedUsersDetails(targetId, "33", "7509779910", "Sat", "nickname", "gender", Geo(10.0, 10.0, 0.0), "india")
      val spokerDetails = SpokerFewDetails("", "last_activity", Geo(1.1, 1.2, 1.3), "picture", "token")
      when(mockedDseGraphPersistenceFactoryApi.checkUserAdminOrNot(userId)) thenReturn (Some(false))
      actorRef ! GetDetails(userId, Some(targetId))
      expectMsgType[DetailsByAdminFailure]
    }

    "Not be Able to get user details by admin when generic error comes" in {
      val userId = UUID.randomUUID().toString
      val targetId = UUID.randomUUID().toString
      val vertex: Vertex = DseGraphFactory.dseConn.executeGraph(s"""graph.addVertex(label,"$USER",'$USER_ID',"$targetId") """).one().asVertex()
      val actorRef = system.actorOf(Props(new AccountView(endpoint.id, eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      val myDetails = LoggedUsersDetails(userId, "33", "7509779910", "Sat", "nickname", "gender", Geo(10.0, 10.0, 0.0), "india")
      val spokerDetails = SpokerFewDetails("cover", "last_activity", Geo(1.1, 1.2, 1.3), "picture", "token")
      when(mockedDseGraphPersistenceFactoryApi.checkUserAdminOrNot(userId)) thenReturn (None)
      actorRef ! GetDetails(userId, Some(targetId))
      expectMsgType[DetailsByAdminFailure]
    }

    "Not able to get user details by admin successfully when user not found" in {
      val userId = UUID.randomUUID().toString
      val targetId = UUID.randomUUID().toString
      val vertex: Vertex = DseGraphFactory.dseConn.executeGraph(s"""graph.addVertex(label,"$USER",'$USER_ID',"$targetId") """).one().asVertex()
      val actorRef = system.actorOf(Props(new AccountView(endpoint.id, eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      val myDetails = LoggedUsersDetails(targetId, "33", "7509779910", "Sat", "nickname", "gender", Geo(10.0, 10.0, 0.0), "india")
      val spokerDetails = SpokerFewDetails("", "lastActivity", Geo(1.1, 1.2, 1.3), "picture", "token")
      when(mockedDseGraphPersistenceFactoryApi.checkUserAdminOrNot(userId)) thenReturn (Some(true))
      when(mockedDseGraphPersistenceFactoryApi.fetchUserAccountDisableOrNot(targetId)) thenReturn (None)
      actorRef ! GetDetails(userId, Some(targetId))
      expectMsgType[DetailsByAdminFailure]
    }
  }
}

