package com.spok.accountsservice.service

import java.util.{ Date, UUID }
import akka.actor.{ ActorSystem, PoisonPill, Props }
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
import com.spok.accountsservice.service.AccountActorSuccessReplies._
import com.spok.accountsservice.service.AccountActorFailureReplies._
import com.spok.accountsservice.service.AccountManagerCommands._

class AccountManagerSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
    with WordSpecLike with MustMatchers with BeforeAndAfterAll with JsonHelper with MockitoSugar {

  val session = CassandraProvider.session

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

  def this() = this(ActorSystem("AccountManagerSystem"))

  val endpoint = ReplicationEndpoint(id => LeveldbEventLog.props(logId = "AccountManagerSpec"))(_system)
  val eventLog = endpoint.logs(DefaultLogName)

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
    session.get.close()
  }

  "A AccountManager" must {

    "Able to create an account by AccountManager" in {
      val id = UUID.randomUUID().toString
      val user = User("dhiru", new Date(), userLocation, "male", List("+9195823110", "+91934455778"),
        "+919687778978", id, Some("picture.jpg"))
      val actorRef = system.actorOf(Props(new AccountManager(id, eventLog) {
        override def createActor(userId: String): AccountActor = {
          new AccountActor(userId, Some(userId), eventLog) {
            override val dseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
            when(dseGraphPersistenceFactoryApi.insertUser(user)) thenReturn (true)
          }
        }
      }))
      actorRef ! Create(user)
      expectMsgType[AccountCreateSuccess](5 seconds)
      actorRef ! PoisonPill
    }

    "Able to validate an account" in {
      val id = UUID.randomUUID().toString
      val phoneNumber1 = "+918910013458"
      val actorRef = system.actorOf(Props(new AccountManager(id, eventLog) {
        override def createActor(userId: String): AccountActor = {
          new AccountActor(userId, Some(userId), eventLog) {
            override val dseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
            when(dseGraphPersistenceFactoryApi.isExistsMobileNo(phoneNumber1.substring(1))) thenReturn (false)
          }
        }
      }))
      actorRef ! Validate(phoneNumber1)
      expectMsgType[ValidateUserSuccess](10 seconds)
    }

    "Able to update OTP by AccountManager" in {
      val id = UUID.randomUUID().toString
      val phoneNumber1 = "+918910013549"
      val otpToken = OtpAuthToken("1234", "+918910013549", new DateTime().plusSeconds(5), 1)
      val actorRef = system.actorOf(Props(new AccountManager(id, eventLog)))
      actorRef ! UpdateOtpToken(otpToken)
      expectMsgType[UpdateOtpTokenSuccess](10 seconds)
    }

    "Able to clear OTP Token by AccountManager" in {
      val id = UUID.randomUUID().toString
      val phoneNumber1 = "+91891041234"
      val actorRef1 = system.actorOf(Props(new AccountManager(id, eventLog)))
      actorRef1 ! ClearOtpToken(phoneNumber1)
      expectNoMsg(20 seconds)
    }

    "Able to allow user by AccountManager" in {
      val id = UUID.randomUUID().toString
      val phoneNumber1 = "+91891041234"
      val actorRef1 = system.actorOf(Props(new AccountManager(id, eventLog)))
      actorRef1 ! AllowUser(phoneNumber1)
      expectNoMsg(20 seconds)
    }

    "Able to Clear Expired Otp Token by AccountManager" in {
      val id = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountManager(id, eventLog)))
      actorRef ! ClearExpiredOtpToken
      expectNoMsg(10 seconds)
    }

    "Able to follow the user" in {
      val followUnfollow = FollowUnfollow("9840608095", "c6094363-619d-4739-9e58-4b4d603ac07b", "4e663afb-c27e-47cd-8739-22b61c81ab40")
      val userId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountManager(userId, eventLog) {
        override def createActor(id: String): AccountActor = {
          new AccountActor(id, Some(id), eventLog) {
            override val dseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
            when(dseGraphPersistenceFactoryApi.performFollowOrUnfollow("9840608095", "c6094363-619d-4739-9e58-4b4d603ac07b", "4e663afb-c27e-47cd-8739-22b61c81ab40")) thenReturn Some(FOLLOWS)
          }
        }
      }))
      actorRef ! FollowUnfollowAction(followUnfollow)
      expectMsgType[FollowUnfollowSuccess](10 seconds)
    }

    "Able to unfollow the user" in {
      val followUnfollow = FollowUnfollow("9840608095", "c6094363-619d-4739-9e58-4b4d603ac07b", "4e663afb-c27e-47cd-8739-22b61c81ab40")
      val userId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountManager(userId, eventLog) {
        override def createActor(id: String): AccountActor = {
          new AccountActor(id, Some(id), eventLog) {
            override val dseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
            when(dseGraphPersistenceFactoryApi.performFollowOrUnfollow("9840608095", "c6094363-619d-4739-9e58-4b4d603ac07b", "4e663afb-c27e-47cd-8739-22b61c81ab40")) thenReturn Some(UNFOLLOWS)
          }
        }
      }))
      actorRef ! FollowUnfollowAction(followUnfollow)
      expectMsgType[FollowUnfollowSuccess](10 seconds)
    }

    "Able to create a group successfully" in {
      val id = UUID.randomUUID().toString
      val groupId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val group = Group(groupId, "titles")
      val actorRef = system.actorOf(Props(new AccountManager(userId, eventLog) {
        override def createActor(id: String): AccountActor = {
          new AccountActor(id, Some(id), eventLog) {
            override val dseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
            when(dseGraphPersistenceFactoryApi.createGroup(userId, group)) thenReturn Some("Group created")
          }
        }
      }))
      actorRef ! CreateUserGroup(group, "12457896547", userId)
      expectMsgType[GroupCreateSuccess](10 seconds)
    }

    "Able to remove a group successfully" in {
      val groupId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val group = Group(groupId, "titles")
      val actorRef = system.actorOf(Props(new AccountManager(id, eventLog) {
        override def createActor(id: String): AccountActor = {
          new AccountActor(id, Some(id), eventLog) {
            override val dseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
            when(dseGraphPersistenceFactoryApi.removeGroup(groupId, userId)) thenReturn true
          }
        }
      }))
      actorRef ! RemoveGroup(groupId, userId, "1245789650")
      expectMsgType[GroupRemovedSuccess](10 seconds)
    }

    "Able to update a group successfully" in {
      val id = UUID.randomUUID().toString
      val groupId = "groupId12345"
      val userId = UUID.randomUUID().toString
      val group = Group(groupId, "titles")
      val actorRef = system.actorOf(Props(new AccountManager(userId, eventLog) {
        override def createActor(id: String): AccountActor = {
          new AccountActor(id, Some(id), eventLog) {
            override val dseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
            when(dseGraphPersistenceFactoryApi.updateGroup(userId, group)) thenReturn true
          }
        }
      }))
      actorRef ! UpdateUserGroup(group, "12457896547", userId)
      expectMsgType[GroupUpdateSuccess](10 seconds)
    }

    "Able to send success message when user successfully adds followers and contacts in group successfully" in {
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val groupId = UUID.randomUUID().toString
      val userId1 = UUID.randomUUID().toString
      val userId2 = UUID.randomUUID().toString
      val contact1 = Contact("contact1", "+919582311057")
      val contact2 = Contact("contact2", "+919990388522")
      val userGroup = UserGroup(groupId, List(userId1, userId2), List(contact1, contact2))
      val actorRef = system.actorOf(Props(new AccountManager(id, eventLog) {
        override def createActor(id: String): AccountActor = {
          new AccountActor(id, Some(id), eventLog) {
            override val dseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
            when(dseGraphPersistenceFactoryApi.isGroupExist(userId, userGroup.groupId)) thenReturn true
            when(dseGraphPersistenceFactoryApi.validateUsersOrContactByGroupId(userId, userGroup.groupId, userGroup.userIds, List("919582311057", "919990388522"))) thenReturn ((Nil, Nil))
            when(dseGraphPersistenceFactoryApi.insertFollowersInGroup(userId, userGroup)) thenReturn ((Nil, Nil))
          }
        }
      }))
      actorRef ! AddFollowers(userGroup, "4556455645", userId)
      expectMsgType[AddFollowerInGroupSuccess](10 seconds)
    }

    "Able to send error message if user is not able to add followers and contacts in group successfully" in {
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val groupId = UUID.randomUUID().toString
      val userId1 = UUID.randomUUID().toString
      val userId2 = UUID.randomUUID().toString
      val contact1 = Contact("contact1", "+919582311057")
      val contact2 = Contact("contact2", "+919990388522")
      val userGroup = UserGroup(groupId, List(userId1, userId2), List(contact1, contact2))
      val actorRef = system.actorOf(Props(new AccountManager(id, eventLog) {
        override def createActor(id: String): AccountActor = {
          new AccountActor(id, Some(id), eventLog) {
            override val dseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
            when(dseGraphPersistenceFactoryApi.isGroupExist(userId, userGroup.groupId)) thenReturn true
            when(dseGraphPersistenceFactoryApi.validateUsersOrContactByGroupId(userId, userGroup.groupId, userGroup.userIds, List("919582311057", "919990388522"))) thenReturn ((Nil, Nil))
            when(dseGraphPersistenceFactoryApi.insertFollowersInGroup(userId, userGroup)) thenReturn ((List("919582311057", "919990388522"), List(userId1, userId2)))
          }
        }
      }))
      actorRef ! AddFollowers(userGroup, "4556455645", userId)
      expectMsgType[AddFollowerInGroupFailure](10 seconds)
    }

    "Able to send proper message if not able to find group when trying to add followers and contacts in group" in {
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val groupId = UUID.randomUUID().toString
      val userId1 = UUID.randomUUID().toString
      val userId2 = UUID.randomUUID().toString
      val contact1 = Contact("contact1", "1234567148")
      val contact2 = Contact("contact2", "1234567148")
      val userGroup = UserGroup(groupId, List(userId1, userId2), List(contact1, contact2))
      val actorRef = system.actorOf(Props(new AccountManager(id, eventLog) {
        override def createActor(id: String): AccountActor = {
          new AccountActor(id, Some(id), eventLog) {
            override val dseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
            when(dseGraphPersistenceFactoryApi.isGroupExist(userId, userGroup.groupId)) thenReturn false
          }
        }
      }))
      actorRef ! AddFollowers(userGroup, "4556455645", userId)
      expectMsgType[AddFollowerInGroupFailure](10 seconds)
    }

    "Able to remove followers and contacts from group successfully" in {
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val groupId = UUID.randomUUID().toString
      val userId1 = UUID.randomUUID().toString
      val userId2 = UUID.randomUUID().toString
      val contact1 = "+1234567148"
      val removeUserGroup = RemoveUserGroup(groupId, List(userId1, userId2), List(contact1))
      val actorRef = system.actorOf(Props(new AccountManager(id, eventLog) {
        override def createActor(id: String): AccountActor = {
          new AccountActor(id, Some(id), eventLog) {
            override val dseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
            when(dseGraphPersistenceFactoryApi.isGroupExist(userId, removeUserGroup.groupId)) thenReturn true
            when(dseGraphPersistenceFactoryApi.validateUsersOrContactByGroupId(userId, removeUserGroup.groupId, removeUserGroup.userIds, List("1234567148"))) thenReturn ((List(userId1, userId2), List("1234567148")))
            when(dseGraphPersistenceFactoryApi.removeFollowersFromGroup(userId, List("1234567148"),
              List(userId1, userId2), groupId)) thenReturn true
          }
        }
      }))
      actorRef ! RemoveFollowers(removeUserGroup, "4556455645", userId)
      expectMsgType[RemoveFollowerInGroupSuccess](10 seconds)
    }

    "Able to send message if not able remove followers and contacts from group" in {
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val groupId = UUID.randomUUID().toString
      val userId1 = UUID.randomUUID().toString
      val userId2 = UUID.randomUUID().toString
      val contact1 = "+1234567148"
      val removeUserGroup = RemoveUserGroup(groupId, List(userId1, userId2), List(contact1))
      val actorRef = system.actorOf(Props(new AccountManager(id, eventLog) {
        override def createActor(id: String): AccountActor = {
          new AccountActor(id, Some(id), eventLog) {
            override val dseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
            when(dseGraphPersistenceFactoryApi.isGroupExist(userId, removeUserGroup.groupId)) thenReturn true
            when(dseGraphPersistenceFactoryApi.validateUsersOrContactByGroupId(userId, removeUserGroup.groupId, removeUserGroup.userIds, List("1234567148"))) thenReturn ((List(userId1, userId2), List("1234567148")))
            when(dseGraphPersistenceFactoryApi.removeFollowersFromGroup(userId, List("1234567148"),
              List(userId1, userId2), groupId)) thenReturn false
          }
        }
      }))
      actorRef ! RemoveFollowers(removeUserGroup, "4556455645", userId)
      expectMsgType[RemoveFollowerInGroupFailure](10 seconds)
    }

    "Able to send error message if group not found and user is trying to remove followers and contacts from group" in {
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val groupId = UUID.randomUUID().toString
      val userId1 = UUID.randomUUID().toString
      val userId2 = UUID.randomUUID().toString
      val contact1 = "+1234567148"
      val removeUserGroup = RemoveUserGroup(groupId, List(userId1, userId2), List(contact1))
      val actorRef = system.actorOf(Props(new AccountManager(id, eventLog) {
        override def createActor(id: String): AccountActor = {
          new AccountActor(id, Some(id), eventLog) {
            override val dseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
            when(dseGraphPersistenceFactoryApi.validateUsersOrContactByGroupId(userId, removeUserGroup.groupId, removeUserGroup.userIds, List("1234567148"))) thenReturn ((List(userId1, userId2), List("1234567148")))
            when(dseGraphPersistenceFactoryApi.isGroupExist(userId, removeUserGroup.groupId)) thenReturn false
          }
        }
      }))
      actorRef ! RemoveFollowers(removeUserGroup, "4556455645", userId)
      expectMsgType[RemoveFollowerInGroupFailure](10 seconds)
    }

    "Able to update user profile" in {
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val geo = Geo(123, 123, 123)
      val birthDate = new Date()
      val userProfile = UserProfile("updatenickname", birthDate, "updategender", Some("updatepicture"), Some("updatecover"), geo)
      val actorRef = system.actorOf(Props(new AccountManager(id, eventLog) {
        override def createActor(id: String): AccountActor = {
          new AccountActor(id, Some(id), eventLog) {
            override val dseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
            override val dseUserSpokFactoryApi: DSEUserSpokFactoryApi = mock[DSEUserSpokFactoryApi]
            when(dseUserSpokFactoryApi.isExistsUser(userId)) thenReturn true
            when(dseGraphPersistenceFactoryApi.updateUserProfile(userId, userProfile)) thenReturn true
          }
        }
      }))
      actorRef ! UpdateProfile("1122334466", userId, userProfile)
      expectMsgType[UserProfileUpdateSuccess](10 seconds)
    }

    "Able to update user phone number" in {
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val oldNumber = "+919711235181"
      val newNumber = "+919711235182"
      val actorRef = system.actorOf(Props(new AccountManager(id, eventLog) {
        override def createActor(id: String): AccountActor = {
          new AccountActor(id, Some(id), eventLog) {
            override val dseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
            when(dseGraphPersistenceFactoryApi.isMobileNoExists("919711235181", userId)) thenReturn true
            when(dseGraphPersistenceFactoryApi.isExistsMobileNo("919711235182")) thenReturn false
          }
        }
      }))
      actorRef ! ValidateNumber("9711235181", oldNumber, newNumber, userId)
      expectMsgType[ValidatePhoneNumberSuccess](10 seconds)
    }

    "Able to update user Settings" in {
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val userSetting = UserSetting(true, false)
      val actorRef = system.actorOf(Props(new AccountManager(id, eventLog) {
        override def createActor(id: String): AccountActor = {
          new AccountActor(id, Some(id), eventLog) {
            override val dseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
            when(dseGraphPersistenceFactoryApi.updateUserSettings(userId, userSetting)) thenReturn true
          }
        }
      }))
      actorRef ! UpdateUserSettings(userSetting, userId)
      expectMsgType[FollowSettingUpdateSuccess](10 seconds)
    }

    "Able to update user help setting" in {
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountManager(id, eventLog) {
        override def createActor(id: String): AccountActor = {
          new AccountActor(id, Some(id), eventLog) {
            override val dseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
            when(dseGraphPersistenceFactoryApi.updateUserHelpSetting(userId)) thenReturn true
          }
        }
      }))
      actorRef ! UpdateUserHelpSettings(userId)
      expectMsgType[HelpSettingUpdateSuccess](10 seconds)
    }

    "Able to update user's new phone number" in {
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val newNumber = "+919711235181"
      val actorRef = system.actorOf(Props(new AccountManager(id, eventLog) {
        override def createActor(id: String): AccountActor = {
          new AccountActor(id, Some(id), eventLog) {
            override val dseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
            when(dseGraphPersistenceFactoryApi.updatePhoneNumber(userId, newNumber.substring(1))) thenReturn true
          }
        }
      }))
      actorRef ! UpdateNumber(userId, "1245781256", newNumber)
      expectMsgType[UpdatePhoneNumberSuccess](10 seconds)
    }

    "send success message if the mailing api successfully sends the mail to the support team" in {
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val message = "Where is the Respok button ?"
      val actorRef = system.actorOf(Props(new AccountManager(id, eventLog) {
        override def createActor(id: String): AccountActor = {
          new AccountActor(id, Some(id), eventLog) {
            override val dseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
            override val mailingApi = mock[MailingApi]
            when(dseGraphPersistenceFactoryApi.getNickname(userId)) thenReturn Some("Roger")
            when(mailingApi.sendMail(SUPPORT, message, "Roger", userId)) thenReturn Some(5)
          }
        }
      }))
      actorRef ! AskSupport(userId, "9988776655", message)
      expectMsgType[SupportProvidedSuccess](10 seconds)
    }

    "send error message if the mailing api fails to send the mail to the support team" in {
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val message = "Where is the Respok button ?"
      val actorRef = system.actorOf(Props(new AccountManager(id, eventLog) {
        override def createActor(id: String): AccountActor = {
          new AccountActor(id, Some(id), eventLog) {
            override val dseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
            override val mailingApi: MailingApi = mock[MailingApi]
            when(dseGraphPersistenceFactoryApi.getNickname(userId)) thenReturn Some("Roger")
            when(mailingApi.sendMail(SUPPORT, message, "Roger", userId)) thenReturn None
          }
        }
      }))
      actorRef ! AskSupport(userId, "9988776655", message)
      expectMsgType[SupportProvidedFailure](10 seconds)
    }

    "send error message if the users nickname is not found" in {
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val message = "Where is the Respok button ?"
      val actorRef = system.actorOf(Props(new AccountManager(id, eventLog) {
        override def createActor(id: String): AccountActor = {
          new AccountActor(id, Some(id), eventLog) {
            override val dseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
            when(dseGraphPersistenceFactoryApi.getNickname(userId)) thenReturn None
          }
        }
      }))
      actorRef ! AskSupport(userId, "9988776655", message)
      expectMsgType[SupportProvidedFailure](10 seconds)
    }

    "Able to promote user account successfully" in {
      val id = UUID.randomUUID().toString
      val targetUserId = UUID.randomUUID().toString
      val level = "level"
      val userId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountManager(id, eventLog) {
        override def createActor(id: String): AccountActor = {
          new AccountActor(id, Some(id), eventLog) {
            override val dseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
            override val dseUserSpokFactoryApi: DSEUserSpokFactoryApi = mock[DSEUserSpokFactoryApi]

            when(dseUserSpokFactoryApi.isExistsUser(userId)) thenReturn true
            when(dseGraphPersistenceFactoryApi.checkUserSuperAdminOrNot(userId)) thenReturn Some(true)
            when(dseGraphPersistenceFactoryApi.fetchUserLevel(targetUserId)) thenReturn Some("user")
            when(dseGraphPersistenceFactoryApi.setUserLevel(targetUserId, level)) thenReturn Some(true)
          }
        }
      }))
      actorRef ! promotUser(userId, level, targetUserId)
      expectMsgType[PromotUserAccountSuccess](10 seconds)
    }

    "Able to suspend user account successfully" in {
      val id = UUID.randomUUID().toString
      val targetUserId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountManager(id, eventLog) {
        override def createActor(id: String): AccountActor = {
          new AccountActor(id, Some(id), eventLog) {
            override val dseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
            override val dseUserSpokFactoryApi: DSEUserSpokFactoryApi = mock[DSEUserSpokFactoryApi]

            when(dseGraphPersistenceFactoryApi.checkUserAdminOrNot(userId)) thenReturn Some(true)
            when(dseGraphPersistenceFactoryApi.checkUserAdminOrNot(targetUserId)) thenReturn Some(false)
            when(dseGraphPersistenceFactoryApi.isUserSuspendAlready(targetUserId)) thenReturn Some(false)
            when(dseGraphPersistenceFactoryApi.suspendUserAccount(targetUserId)) thenReturn Some(true)
          }
        }
      }))
      actorRef ! Suspend(userId, targetUserId, "1234567890")
      expectMsgType[SuspendResponseSuccess](10 seconds)
    }

    "Able to reactiavte user account successfully" in {
      val id = UUID.randomUUID().toString
      val targetUserId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new AccountManager(id, eventLog) {
        override def createActor(id: String): AccountActor = {
          new AccountActor(id, Some(id), eventLog) {
            override val dseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
            override val dseUserSpokFactoryApi: DSEUserSpokFactoryApi = mock[DSEUserSpokFactoryApi]

            when(dseGraphPersistenceFactoryApi.checkUserAdminOrNot(userId)) thenReturn Some(true)
            when(dseGraphPersistenceFactoryApi.isUserSuspendAlready(targetUserId)) thenReturn Some(true)
            when(dseGraphPersistenceFactoryApi.reactiveUserAccount(targetUserId)) thenReturn Some(true)
          }
        }
      }))
      actorRef ! Recativate(userId, targetUserId, "1234567890")
      expectMsgType[ReactivatedResponseSuccess](10 seconds)
    }
  }
}
