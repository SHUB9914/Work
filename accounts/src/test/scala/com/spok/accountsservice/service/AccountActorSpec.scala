package com.spok.accountsservice.service

import java.util.{ Date, UUID }

import akka.actor.{ ActorSystem, Props }
import akka.testkit.{ ImplicitSender, TestKit }
import com.rbmhtechnology.eventuate.ReplicationEndpoint
import com.rbmhtechnology.eventuate.ReplicationEndpoint._
import com.rbmhtechnology.eventuate.log.leveldb.LeveldbEventLog
import com.spok.model.Account._
import com.spok.model.SpokModel.Geo
import com.spok.model.{ Location, OtpAuthToken }
import com.spok.persistence.cassandra.CassandraProvider
import com.spok.persistence.factory.{ DSEGraphPersistenceFactoryApi, DSEUserSpokFactoryApi }
import com.spok.util.Constant._
import com.spok.util.{ JsonHelper, MailingApi }
import org.joda.time.DateTime
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ BeforeAndAfterAll, MustMatchers, WordSpecLike }
import scala.concurrent.duration._
import com.spok.accountsservice.service.AccountActorCommand._
import com.spok.accountsservice.service.AccountActorUpdateCommands._
import com.spok.accountsservice.service.AccountActorSuccessReplies._
import com.spok.accountsservice.service.AccountActorFailureReplies._
import com.spok.accountsservice.service.AccountAlreadyRegisters._

class AccountActorSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
    with WordSpecLike with MustMatchers with BeforeAndAfterAll with JsonHelper with MockitoSugar {

  val session = CassandraProvider.session

  def this() = this(ActorSystem("AccountActorSystem"))

  val endpoint = ReplicationEndpoint(id => LeveldbEventLog.props(logId = "AccountActorSpec"))(_system)
  val eventLog = endpoint.logs(DefaultLogName)
  val mockedDseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
  val mockedDSEUserSpokFactoryApi: DSEUserSpokFactoryApi = mock[DSEUserSpokFactoryApi]
  val mockedMailingApi: MailingApi = mock[MailingApi]

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
    session.get.close()
  }

  "A AccountActor" must {

    val userString =
      """{
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

    "Persists a user when asked to store user detail" in {
      val id = UUID.randomUUID().toString
      val user = User("dhiru", new Date(), userLocation, "male", List("+919582311050", "+919582311059"),
        "+919687778978", "1", None)
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.insertUser(user)) thenReturn true
      actorRef ! CreateAccount(user)
      expectMsgType[AccountCreateSuccess](10 seconds)
    }

    "not Persists a user when asked to store user detail" in {
      val id = UUID.randomUUID().toString
      val user = User("dhiru", new Date(), userLocation, "male", List("+919582311050", "+919582311059"),
        "+919687778978", "1", None)
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.insertUser(user)) thenReturn false
      actorRef ! CreateAccount(user)
      expectMsgType[AccountCreateFailure](10 seconds)
    }

    "Persists a user when asked to store user detail and automatically follow the previous user" in {
      val id = UUID.randomUUID().toString
      val user = User("diksha", new Date(), userLocation, "female", List("+919582311056", "+919582311057"),
        "+919582311050", "2", None)
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.insertUser(user)) thenReturn true
      actorRef ! CreateAccount(user)
      expectMsgType[AccountCreateSuccess](10 seconds)
    }

    "Validate a user's phone number if not exist " in {
      val id = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.isExistsMobileNo("919540608095")) thenReturn false
      actorRef ! ValidateUser("+919540608095")
      expectMsgType[ValidateUserSuccess](10 seconds)
    }
    "AuthenticateUser a user's phone number if exist " in {
      val id = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.isExistsMobileNo("919540608095")) thenReturn true
      actorRef ! AuthenticateUser("+919540608095")
      expectMsgType[AuthenticateUserSuccess](10 seconds)
    }

    "AuthenticateUser a user's phone number if not exist " in {
      val id = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.isExistsMobileNo("919540608095")) thenReturn false
      actorRef ! AuthenticateUser("+919540608095")
      expectMsgType[UserNotRegitered](10 seconds)
    }

    "Validate a user's phone number if  exist " in {
      val id = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.isExistsMobileNo("919540608095")) thenReturn true
      actorRef ! ValidateUser("+919540608095")
      expectMsgType[AllReadyRegisteredUser](10 seconds)
    }

    "Able to update OTP Token " in {
      val id = UUID.randomUUID().toString
      val otpToken = OtpAuthToken("1234", "+919540608095", new DateTime().plusMinutes(5), 0)
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      actorRef ! UpdateOtpToken(otpToken)
      expectMsgType[UpdateOtpTokenSuccess](10 seconds)
    }

    "Able to clear OTP Token " in {
      val id = UUID.randomUUID().toString
      val phoneNumber = "+919540608095"
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      actorRef ! ClearOtpToken(phoneNumber)
      expectNoMsg(10 seconds)
    }

    "Able to allow user " in {
      val id = UUID.randomUUID().toString
      val phoneNumber = "+919540608095"
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      actorRef ! AllowUser(phoneNumber)
      expectNoMsg(10 seconds)
    }

    "Able to follow a User" in {

      val follow = FollowUnfollow("919582311059", "5678", "1234")
      val id = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.performFollowOrUnfollow("919582311059", "5678", "1234")) thenReturn Some(FOLLOWS)
      actorRef ! FollowUnfollowCommand(follow)
      expectMsgType[FollowUnfollowSuccess](10 seconds)

    }

    "Able to unfollow a User" in {

      val unfollow = FollowUnfollow("919582311059", "5678", "1234")
      val id = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.performFollowOrUnfollow("919582311059", "5678", "1234")) thenReturn Some(UNFOLLOWS)
      actorRef ! FollowUnfollowCommand(unfollow)
      expectMsgType[FollowUnfollowSuccess](10 seconds)
    }

    "Able to create a group successfully" in {
      val groupId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val group = Group(groupId, "title")
      val actorRef = system.actorOf(Props(new AccountActor(userId, Some(userId), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      val result = (Some("Group created"))
      when(mockedDseGraphPersistenceFactoryApi.createGroup(userId, group)) thenReturn result
      actorRef ! CreateGroup(group, userId)
      expectMsgType[GroupCreateSuccess](10 seconds)
    }

    "to send proper message if a group is not created successfully" in {
      val groupId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val group = Group(groupId, "title")
      val actorRef = system.actorOf(Props(new AccountActor(userId, Some(userId), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.createGroup(userId, group)) thenReturn None
      actorRef ! CreateGroup(group, userId)
      expectMsgType[GroupCreateFailure](10 seconds)
    }

    "Able to update a group successfully" in {
      val groupId = "groupId12345"
      val userId = UUID.randomUUID().toString
      val group = Group(groupId, "titles")
      val actorRef = system.actorOf(Props(new AccountActor(userId, Some(userId), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.updateGroup(userId, group)) thenReturn true
      actorRef ! UpdateGroup(group, userId)
      expectMsgType[GroupUpdateSuccess](10 seconds)
    }

    "Able to send proper message if a group is not updated successfully" in {
      val groupId = "groupId12345"
      val userId = UUID.randomUUID().toString
      val group = Group(groupId, "titles")
      val actorRef = system.actorOf(Props(new AccountActor(userId, Some(userId), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.updateGroup(userId, group)) thenReturn false
      actorRef ! UpdateGroup(group, userId)
      expectMsgType[GroupUpdateFailure](10 seconds)
    }

    "Able to update follow setting successfully" in {

      val userId = UUID.randomUUID().toString
      val userSettigns = UserSetting(true, true)

      val actorRef = system.actorOf(Props(new AccountActor(userId, Some(userId), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.updateUserSettings(userId, userSettigns)) thenReturn (true)
      actorRef ! UpdateSettings(userSettigns, userId)
      expectMsgType[FollowSettingUpdateSuccess](10 seconds)
    }

    "Able to send proper message if the follow settings are not updated successfully" in {
      val userId = UUID.randomUUID().toString
      val userSettigns = UserSetting(true, true)
      val actorRef = system.actorOf(Props(new AccountActor(userId, Some(userId), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.updateUserSettings(userId, userSettigns)) thenReturn (false)
      actorRef ! UpdateSettings(userSettigns, userId)
      expectMsgType[FollowSettingUpdateFailure](10 seconds)
    }

    "Able to remove a group successfully" in {
      val groupId = "groupId12345"
      val userId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountActor(userId, Some(userId), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.removeGroup(groupId, userId)) thenReturn true
      actorRef ! GroupRemove(groupId, userId)
      expectMsgType[GroupRemovedSuccess](10 seconds)
    }

    "Able to send proper message if a group is not removed successfully" in {
      val groupId = "groupId1234"
      val userId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountActor(userId, Some(userId), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.removeGroup(groupId, userId)) thenReturn false
      actorRef ! GroupRemove(groupId, userId)
      expectMsgType[GroupRemovedFailure](10 seconds)
    }

    "Able to send success message if a user is able to add followers in group successfully" in {
      val id = UUID.randomUUID().toString
      val groupId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val userId1 = UUID.randomUUID().toString
      val userId2 = UUID.randomUUID().toString
      val contact1 = Contact("contact1", "+919582311057")
      val contact2 = Contact("contact2", "+919990388522")
      val userGroup = UserGroup(groupId, List(userId1, userId2), List(contact1, contact2))
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.isGroupExist(userId, userGroup.groupId)) thenReturn true
      when(mockedDseGraphPersistenceFactoryApi.validateUsersOrContactByGroupId(userId, userGroup.groupId, userGroup.userIds, List("919582311057", "919990388522"))) thenReturn ((Nil, Nil))
      when(mockedDseGraphPersistenceFactoryApi.insertFollowersInGroup(userId, userGroup)) thenReturn ((Nil, Nil))
      actorRef ! AddFollowersInGroup(userGroup, userId)
      expectMsgType[AddFollowerInGroupSuccess](10 seconds)
    }

    "Able to send success message if a user is able to add valid followers and contacts in group successfully" in {
      val id = UUID.randomUUID().toString
      val groupId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val userId1 = UUID.randomUUID().toString
      val userId2 = UUID.randomUUID().toString
      val contact1 = Contact("contact1", "+919582311057")
      val contact2 = Contact("contact2", "+919990388522")
      val userGroup = UserGroup(groupId, List(userId1, userId2), List(contact1, contact2))
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.isGroupExist(userId, userGroup.groupId)) thenReturn true
      when(mockedDseGraphPersistenceFactoryApi.validateUsersOrContactByGroupId(userId, userGroup.groupId, userGroup.userIds, List("919582311057", "919990388522"))) thenReturn ((List("919582311057"), Nil))
      when(mockedDseGraphPersistenceFactoryApi.insertFollowersInGroup(userId, UserGroup(groupId, List(userId1, userId2), List(contact2)))) thenReturn ((Nil, List(userId1)))
      actorRef ! AddFollowersInGroup(userGroup, userId)
      expectMsgType[AddFollowerInGroupSuccess](10 seconds)
    }

    "Able to send error message if a user sends already added followers and contacts in group" in {
      val id = UUID.randomUUID().toString
      val groupId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val userId1 = UUID.randomUUID().toString
      val userId2 = UUID.randomUUID().toString
      val contact1 = Contact("contact1", "+919582311057")
      val contact2 = Contact("contact2", "+919990388522")
      val userGroup = UserGroup(groupId, List(userId1, userId2), List(contact1, contact2))
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.isGroupExist(userId, userGroup.groupId)) thenReturn true
      when(mockedDseGraphPersistenceFactoryApi.validateUsersOrContactByGroupId(userId, userGroup.groupId, userGroup.userIds, List("919582311057", "919990388522"))) thenReturn ((List("919582311057", "919990388522"), List(userId1, userId2)))
      when(mockedDseGraphPersistenceFactoryApi.insertFollowersInGroup(userId, userGroup)) thenReturn ((Nil, Nil))
      actorRef ! AddFollowersInGroup(userGroup, userId)
      expectMsgType[AddFollowerInGroupFailure](10 seconds)
    }

    "Able to send error message if a user is not able to add followers and contacts in a group" in {
      val id = UUID.randomUUID().toString
      val groupId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val userId1 = UUID.randomUUID().toString
      val userId2 = UUID.randomUUID().toString
      val contact1 = Contact("contact1", "+919582311057")
      val contact2 = Contact("contact2", "+919990388522")
      val userGroup = UserGroup(groupId, List(userId1, userId2), List(contact1, contact2))
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.isGroupExist(userId, userGroup.groupId)) thenReturn true
      when(mockedDseGraphPersistenceFactoryApi.validateUsersOrContactByGroupId(userId, userGroup.groupId, userGroup.userIds, List("919582311057", "919990388522"))) thenReturn ((Nil, Nil))
      when(mockedDseGraphPersistenceFactoryApi.insertFollowersInGroup(userId, userGroup)) thenReturn ((List("919582311057", "919990388522"), List(userId1, userId2)))
      actorRef ! AddFollowersInGroup(userGroup, userId)
      expectMsgType[AddFollowerInGroupFailure](10 seconds)
    }
    "Able to send error message if while adding the followers and contacts, the group is not found" in {
      val id = UUID.randomUUID().toString
      val groupId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val userId1 = UUID.randomUUID().toString
      val userId2 = UUID.randomUUID().toString
      val contact1 = Contact("contact1", "919582311057")
      val contact2 = Contact("contact2", "919990388522")
      val userGroup = UserGroup(groupId, List(userId1, userId2), List(contact1, contact2))
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.isGroupExist(userId, userGroup.groupId)) thenReturn false
      actorRef ! AddFollowersInGroup(userGroup, userId)
      expectMsgType[AddFollowerInGroupFailure](10 seconds)
    }

    "Able to send success message when the user successfully removes followers and contacts from group" in {
      val id = UUID.randomUUID().toString
      val groupId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val userId1 = UUID.randomUUID().toString
      val userId2 = UUID.randomUUID().toString
      val contact1 = "+1234567148"
      val removeUserGroup = RemoveUserGroup(groupId, List(userId1, userId2), List(contact1))
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.isGroupExist(userId, removeUserGroup.groupId)) thenReturn true
      when(mockedDseGraphPersistenceFactoryApi.validateUsersOrContactByGroupId(userId, removeUserGroup.groupId, removeUserGroup.userIds, List("1234567148"))) thenReturn ((List(userId1, userId2), List("1234567148")))
      when(mockedDseGraphPersistenceFactoryApi.removeFollowersFromGroup(userId, List("1234567148"),
        List(userId1, userId2), groupId)) thenReturn true
      actorRef ! RemoveFollowersInGroup(removeUserGroup, userId)
      expectMsgType[RemoveFollowerInGroupSuccess](10 seconds)
    }

    "Able to send error message if user is not able to remove followers and contacts from group" in {
      val id = UUID.randomUUID().toString
      val groupId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val userId1 = UUID.randomUUID().toString
      val userId2 = UUID.randomUUID().toString
      val contact1 = "+1234567148"
      val removeUserGroup = RemoveUserGroup(groupId, List(userId1, userId2), List(contact1))
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.isGroupExist(userId, removeUserGroup.groupId)) thenReturn true
      when(mockedDseGraphPersistenceFactoryApi.validateUsersOrContactByGroupId(userId, removeUserGroup.groupId, removeUserGroup.userIds, List("1234567148"))) thenReturn ((List(userId1, userId2), List("1234567148")))
      when(mockedDseGraphPersistenceFactoryApi.removeFollowersFromGroup(userId, List(userId1, userId2),
        List(contact1), groupId)) thenReturn false
      actorRef ! RemoveFollowersInGroup(removeUserGroup, userId)
      expectMsgType[RemoveFollowerInGroupFailure](10 seconds)
    }

    "Able to send error message if the group is not found when trying to remove followers and contacts from group" in {
      val id = UUID.randomUUID().toString
      val groupId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val userId1 = UUID.randomUUID().toString
      val userId2 = UUID.randomUUID().toString
      val contact1 = "+1234567148"
      val removeUserGroup = RemoveUserGroup(groupId, List(userId1, userId2), List(contact1))
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.isGroupExist(userId, removeUserGroup.groupId)) thenReturn false
      actorRef ! RemoveFollowersInGroup(removeUserGroup, userId)
      expectMsgType[RemoveFollowerInGroupFailure](10 seconds)
    }

    "Able to update user profile when user exists" in {
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val geo = Geo(123, 123, 123)
      val birthDate = new Date()
      val userProfile = UserProfile("updatenickname", birthDate, "updategender", Some("updatepicture"), Some("updatecover"), geo)

      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDSEUserSpokFactoryApi.isExistsUser(userId)) thenReturn true
      when(mockedDseGraphPersistenceFactoryApi.updateUserProfile(userId, userProfile)) thenReturn true
      actorRef ! UpdateUserProfile(userId, userProfile)
      expectMsgType[UserProfileUpdateSuccess](10 seconds)
    }

    "Not able to update user profile when user exists" in {
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val geo = Geo(123, 123, 123)
      val birthDate = new Date()
      val userProfile = UserProfile("updatenickname", birthDate, "updategender", Some("updatepicture"), Some("updatecover"), geo)

      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDSEUserSpokFactoryApi.isExistsUser(userId)) thenReturn true
      when(mockedDseGraphPersistenceFactoryApi.updateUserProfile(userId, userProfile)) thenReturn false
      actorRef ! UpdateUserProfile(userId, userProfile)
      expectMsgType[UserProfileUpdateFailure](10 seconds)
    }

    "Not able to update user profile when user doesn't exist" in {
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val geo = Geo(123, 123, 123)
      val birthDate = new Date()
      val userProfile = UserProfile("updatenickname", birthDate, "updategender", Some("updatepicture"), Some("updatecover"), geo)

      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDSEUserSpokFactoryApi.isExistsUser(userId)) thenReturn false
      actorRef ! UpdateUserProfile(userId, userProfile)
      expectMsgType[UserProfileUpdateFailure](10 seconds)
    }

    "Able to validate old number and send OTP to new number" in {
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.isMobileNoExists("919540608095", userId)) thenReturn true
      when(mockedDseGraphPersistenceFactoryApi.isExistsMobileNo("919711235181")) thenReturn false
      actorRef ! ValidatePhoneNumber("+919540608095", "+919711235181", userId)
      expectMsgType[ValidatePhoneNumberSuccess](10 seconds)
    }

    "Not able to send OTP to new number if old number doesn't exists" in {
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.isMobileNoExists("919540608095", userId)) thenReturn false
      when(mockedDseGraphPersistenceFactoryApi.isExistsMobileNo("919711235181")) thenReturn false
      actorRef ! ValidatePhoneNumber("+919540608095", "+919711235181", userId)
      expectMsgType[ValidatePhoneNumberFailure](10 seconds)
    }

    "able to update user help setting " in {
      val userId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.updateUserHelpSetting(userId)) thenReturn true
      actorRef ! UpdateHelpSettings(userId)
      expectMsgType[HelpSettingUpdateSuccess](10 seconds)
    }

    " not be able to update user help setting " in {
      val userId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.updateUserHelpSetting(userId)) thenReturn false
      actorRef ! UpdateHelpSettings(userId)
      expectMsgType[HelpSettingUpdateFailure](10 seconds)
    }

    "Able to update user's new phone number" in {
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val newNumber = "+919711235181"
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.updatePhoneNumber(userId, newNumber.substring(1))) thenReturn true
      actorRef ! UpdatePhoneNumber(userId, newNumber)
      expectMsgType[UpdatePhoneNumberSuccess](10 seconds)
    }

    "Not able to update user's new phone number" in {
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val newNumber = "9711235181"
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.updatePhoneNumber(userId, newNumber)) thenReturn false
      actorRef ! UpdatePhoneNumber(userId, newNumber)
      expectMsgType[UpdatePhoneNumberFailure](10 seconds)
    }

    "send success message if the mailing api successfully sends the mail to the support team" in {
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val message = "Where is the Respok button ?"
      val phoneNumber = "+919711235181"
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val mailingApi = mockedMailingApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.getNickname(userId)) thenReturn Some("Roger")
      when(mockedMailingApi.sendMail(SUPPORT, message, "Roger", userId)) thenReturn Some(5)
      actorRef ! ProvideSupport(userId, phoneNumber, message)
      expectMsgType[SupportProvidedSuccess](10 seconds)
    }

    "send error message if the mailing api fails to send the mail to the support team" in {
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val message = "Where is the Respok button ?"
      val phoneNumber = "+919711235181"
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val mailingApi = mockedMailingApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.getNickname(userId)) thenReturn Some("Roger")
      when(mockedMailingApi.sendMail(SUPPORT, message, "Roger", userId)) thenReturn None
      actorRef ! ProvideSupport(userId, phoneNumber, message)
      expectMsgType[SupportProvidedFailure](10 seconds)
    }

    "send error message if the users nickname is not found" in {
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val message = "Where is the Respok button ?"
      val phoneNumber = "+919711235181"
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.getNickname(userId)) thenReturn None
      actorRef ! ProvideSupport(userId, phoneNumber, message)
      expectMsgType[SupportProvidedFailure](10 seconds)
    }

    "Able to promote user account successfully" in {
      val level = "admin"
      val id = UUID.randomUUID().toString
      val targetUserId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString

      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDSEUserSpokFactoryApi.isExistsUser(userId)) thenReturn true
      when(mockedDseGraphPersistenceFactoryApi.checkUserSuperAdminOrNot(userId)) thenReturn Some(true)
      when(mockedDseGraphPersistenceFactoryApi.fetchUserLevel(targetUserId)) thenReturn Some("user")
      when(mockedDseGraphPersistenceFactoryApi.setUserLevel(targetUserId, level)) thenReturn Some(true)
      actorRef ! promotUserAccount(userId, level, targetUserId)
      expectMsgType[PromotUserAccountSuccess](10 seconds)
    }

    "not able to promote user account successfully when generic error comes" in {
      val level = "admin"
      val id = UUID.randomUUID().toString
      val targetUserId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString

      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDSEUserSpokFactoryApi.isExistsUser(userId)) thenReturn true
      when(mockedDseGraphPersistenceFactoryApi.checkUserSuperAdminOrNot(userId)) thenReturn Some(true)
      when(mockedDseGraphPersistenceFactoryApi.fetchUserLevel(targetUserId)) thenReturn Some("user")
      when(mockedDseGraphPersistenceFactoryApi.setUserLevel(targetUserId, level)) thenReturn Some(false)
      actorRef ! promotUserAccount(userId, level, targetUserId)
      expectMsgType[PromotUserAccountFailure](10 seconds)
    }

    "not able to promote user account successfully when generic error in fetching user level" in {
      val level = "admin"
      val id = UUID.randomUUID().toString
      val targetUserId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString

      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDSEUserSpokFactoryApi.isExistsUser(userId)) thenReturn true
      when(mockedDseGraphPersistenceFactoryApi.checkUserSuperAdminOrNot(userId)) thenReturn Some(true)
      when(mockedDseGraphPersistenceFactoryApi.fetchUserLevel(targetUserId)) thenReturn None
      actorRef ! promotUserAccount(userId, level, targetUserId)
      expectMsgType[PromotUserAccountFailure](10 seconds)
    }

    "not able to promote user account successfully by another user(not super admin)" in {
      val level = "admin"
      val id = UUID.randomUUID().toString
      val targetUserId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString

      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDSEUserSpokFactoryApi.isExistsUser(userId)) thenReturn true
      when(mockedDseGraphPersistenceFactoryApi.checkUserSuperAdminOrNot(userId)) thenReturn Some(false)

      actorRef ! promotUserAccount(userId, level, targetUserId)
      expectMsgType[PromotUserAccountFailure](10 seconds)
    }

    "not able to promote user account when generic error come in fetching user is superAdminOrNot" in {
      val level = "admin"
      val id = UUID.randomUUID().toString
      val targetUserId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString

      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDSEUserSpokFactoryApi.isExistsUser(userId)) thenReturn true
      when(mockedDseGraphPersistenceFactoryApi.checkUserSuperAdminOrNot(userId)) thenReturn None

      actorRef ! promotUserAccount(userId, level, targetUserId)
      expectMsgType[PromotUserAccountFailure](10 seconds)
    }

    "Able to suspend the Spoker account by Admin" in {

      val userId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val targetId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.checkUserAdminOrNot(userId)) thenReturn Some(true)
      when(mockedDseGraphPersistenceFactoryApi.checkUserAdminOrNot(targetId)) thenReturn Some(false)
      when(mockedDseGraphPersistenceFactoryApi.isUserSuspendAlready(targetId)) thenReturn Some(false)
      when(mockedDseGraphPersistenceFactoryApi.suspendUserAccount(targetId)) thenReturn Some(true)
      actorRef ! SuspendAccount(userId, targetId)
      expectMsgType[SuspendResponseSuccess]
    }

    "not able to suspend the Spoker account when generic error comes in admin details" in {

      val userId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val targetId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.checkUserAdminOrNot(userId)) thenReturn None
      actorRef ! SuspendAccount(userId, targetId)
      expectMsgType[SuspendResponseFailure]
    }

    "Not able to suspend the Spoker account by Admin when Spoker is also admin" in {

      val userId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val targetId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.checkUserAdminOrNot(userId)) thenReturn Some(true)
      when(mockedDseGraphPersistenceFactoryApi.checkUserAdminOrNot(targetId)) thenReturn Some(true)
      actorRef ! SuspendAccount(userId, targetId)
      expectMsgType[SuspendResponseFailure]
    }

    "not able to suspend the Spoker account when spoker is suspended already" in {

      val userId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val targetId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.checkUserAdminOrNot(userId)) thenReturn Some(true)
      when(mockedDseGraphPersistenceFactoryApi.checkUserAdminOrNot(targetId)) thenReturn Some(false)
      when(mockedDseGraphPersistenceFactoryApi.isUserSuspendAlready(targetId)) thenReturn Some(true)
      actorRef ! SuspendAccount(userId, targetId)
      expectMsgType[SuspendResponseFailure]
    }

    "not able to suspend the Spoker account when generic error comes in  isUserSuspendAlready" in {

      val userId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val targetId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.checkUserAdminOrNot(userId)) thenReturn Some(true)
      when(mockedDseGraphPersistenceFactoryApi.checkUserAdminOrNot(targetId)) thenReturn Some(true)
      when(mockedDseGraphPersistenceFactoryApi.isUserSuspendAlready(targetId)) thenReturn None
      actorRef ! SuspendAccount(userId, targetId)
      expectMsgType[SuspendResponseFailure]
    }

    "Handle generic error which comes to Suspend the spoker account" in {

      val userId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val targetId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.checkUserAdminOrNot(userId)) thenReturn Some(true)
      when(mockedDseGraphPersistenceFactoryApi.checkUserAdminOrNot(targetId)) thenReturn Some(false)
      when(mockedDseGraphPersistenceFactoryApi.isUserSuspendAlready(targetId)) thenReturn Some(false)
      when(mockedDseGraphPersistenceFactoryApi.suspendUserAccount(targetId)) thenReturn None
      actorRef ! SuspendAccount(userId, targetId)
      expectMsgType[SuspendResponseFailure]
    }

    "not able to suspend the Spoker account when user is not admin" in {

      val userId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val targetId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.checkUserAdminOrNot(userId)) thenReturn Some(false)
      actorRef ! SuspendAccount(userId, targetId)
      expectMsgType[SuspendResponseFailure]
    }

    "not able to suspend the Spoker account when generic error comes when fetch targeId details " in {

      val userId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val targetId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.checkUserAdminOrNot(userId)) thenReturn Some(true)
      when(mockedDseGraphPersistenceFactoryApi.checkUserAdminOrNot(targetId)) thenReturn None
      actorRef ! SuspendAccount(userId, targetId)
      expectMsgType[SuspendResponseFailure]
    }

    "Able to Reactivate  Spoker account by Admin" in {

      val userId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val targetId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.checkUserAdminOrNot(userId)) thenReturn Some(true)
      when(mockedDseGraphPersistenceFactoryApi.isUserSuspendAlready(targetId)) thenReturn Some(true)
      when(mockedDseGraphPersistenceFactoryApi.reactiveUserAccount(targetId)) thenReturn Some(true)
      actorRef ! ReactivateAccount(userId, targetId)
      expectMsgType[ReactivatedResponseSuccess]
    }

    "not able to Reactivate  Spoker account when generic error comes" in {

      val userId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val targetId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.checkUserAdminOrNot(userId)) thenReturn Some(true)
      when(mockedDseGraphPersistenceFactoryApi.isUserSuspendAlready(targetId)) thenReturn Some(true)
      when(mockedDseGraphPersistenceFactoryApi.reactiveUserAccount(targetId)) thenReturn None
      actorRef ! ReactivateAccount(userId, targetId)
      expectMsgType[ReactivateResponseFailure]
    }

    "not able to reactivate  spoker account when spokerId not found" in {

      val userId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val targetId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.checkUserAdminOrNot(userId)) thenReturn Some(true)
      when(mockedDseGraphPersistenceFactoryApi.isUserSuspendAlready(targetId)) thenReturn None
      actorRef ! ReactivateAccount(userId, targetId)
      expectMsgType[ReactivateResponseFailure]
    }

    "not able to reactivate  spoker account when spoker not suspended" in {

      val userId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val targetId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.checkUserAdminOrNot(userId)) thenReturn Some(true)
      when(mockedDseGraphPersistenceFactoryApi.isUserSuspendAlready(targetId)) thenReturn Some(false)
      actorRef ! ReactivateAccount(userId, targetId)
      expectMsgType[ReactivateResponseFailure]
    }

    "not able to reactivate  spoker account when user is not admin" in {

      val userId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val targetId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.checkUserAdminOrNot(userId)) thenReturn Some(false)
      actorRef ! ReactivateAccount(userId, targetId)
      expectMsgType[ReactivateResponseFailure]
    }

    "not able to reactivate  spoker account when generic error comes to see user is admin or not" in {

      val userId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val targetId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountActor(id, Some(id), eventLog) {
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.checkUserAdminOrNot(userId)) thenReturn None
      actorRef ! ReactivateAccount(userId, targetId)
      expectMsgType[ReactivateResponseFailure]
    }

  }
}
