package com.spok.accountsservice.service

import akka.actor.ActorRef
import com.rbmhtechnology.eventuate.EventsourcedActor
import com.spok.model.Account._
import com.spok.model.OtpAuthToken
import com.spok.persistence.factory.{ DSEGraphPersistenceFactoryApi, DSEUserSpokFactoryApi }
import com.spok.util.Constant._
import com.spok.util.{ MailingApi, TwilioSMSSender }
import scala.util.{ Failure, Success }
import com.spok.accountsservice.service.AccountActorCommand._
import com.spok.accountsservice.service.AccountActorUpdateCommands._
import com.spok.accountsservice.service.AccountActorEvents._
import com.spok.accountsservice.service.AccountActorUpdateEvents._
import com.spok.accountsservice.service.AccountActorSuccessReplies._
import com.spok.accountsservice.service.AccountActorFailureReplies._
import com.spok.accountsservice.service.AccountAlreadyRegisters._

case class CreateUserSetting(userDetail: User)

class AccountActor(override val id: String, override val aggregateId: Option[String], val eventLog: ActorRef)
    extends EventsourcedActor with TwilioSMSSender with AccountLog {

  val dseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = DSEGraphPersistenceFactoryApi
  val dseUserSpokFactoryApi: DSEUserSpokFactoryApi = DSEUserSpokFactoryApi
  val mailingApi: MailingApi = MailingApi

  /**
   * Command handlers.
   */
  override val onCommand: Receive = {

    case CreateAccount(userDetail: User) => {
      persist(AccountCreated(userDetail)) {
        case Success(evt) => {
          val createAccount = dseGraphPersistenceFactoryApi.insertUser(userDetail)
          createAccount match {
            case true => {
              sender() ! AccountCreateSuccess(userDetail)
              self ! CreateUserSetting(userDetail)
            }
            case false => sender() ! AccountCreateFailure(userDetail, new Exception(s"Unable registering nickname ${userDetail.nickname} (generic error)"))
          }
        }
        case Failure(err) =>
          sender() ! AccountCreateFailure(userDetail, err)
      }
    }

    case SuspendAccount(userId, targetUserId) => {
      val isUserAdmin: Option[Boolean] = dseGraphPersistenceFactoryApi.checkUserAdminOrNot(userId)
      isUserAdmin match {
        case Some(true) =>
          val isTargetAdmin = dseGraphPersistenceFactoryApi.checkUserAdminOrNot(targetUserId)
          isTargetAdmin match {
            case Some(true) =>
              sender() ! SuspendResponseFailure(new Exception(s"Unable to suspend the account because $targetUserId is a admin"), USR_107)
            case Some(false) =>
              val response: Option[Boolean] = dseGraphPersistenceFactoryApi.isUserSuspendAlready(targetUserId)
              response match {
                case None => sender() ! SuspendResponseFailure(new Exception(s"Spoker $targetUserId not found"), USR_001)
                case Some(true) => sender() ! SuspendResponseFailure(new Exception(s"Spoker $targetUserId is already suspended"), ADM_007)
                case Some(false) =>
                  val isSupendedSuccessfully = dseGraphPersistenceFactoryApi.suspendUserAccount(targetUserId)
                  if (isSupendedSuccessfully.isDefined) {
                    sender() ! SuspendResponseSuccess("Spoker suspended successfully")
                  } else sender() ! SuspendResponseFailure(new Exception(s"Unable suspending spoker $targetUserId (generic error)"), ADM_104)
              }
            case None => sender() ! SuspendResponseFailure(new Exception(s"Spoker $targetUserId not found"), USR_001)
          }
        case Some(false) => sender() ! SuspendResponseFailure(new Exception(s" User_Id $userId is not a Admin"), USR_106)
        case None => sender() ! SuspendResponseFailure(new Exception(s" Unable suspending spoker $targetUserId (generic error)"), ADM_104)
      }
    }

    case ReactivateAccount(userId, targetUserId) => {
      val isUserAdmin: Option[Boolean] = dseGraphPersistenceFactoryApi.checkUserAdminOrNot(userId)
      isUserAdmin match {
        case None => sender ! ReactivateResponseFailure(new Exception(s"Unable suspending spoker $targetUserId (generic error)"), ADM_105)
        case Some(false) => sender ! ReactivateResponseFailure(new Exception(s" User_Id $userId is not a Admin"), USR_106)
        case Some(true) =>
          val isUserSuspendedAlraedy = dseGraphPersistenceFactoryApi.isUserSuspendAlready(targetUserId)
          isUserSuspendedAlraedy match {
            case None => sender ! ReactivateResponseFailure(new Exception(s"Spoker $targetUserId not found"), USR_001)
            case Some(false) => sender ! ReactivateResponseFailure(new Exception(s"Spoker $targetUserId is not suspended."), ADM_008)
            case Some(true) =>
              val isUserReactivatedSuccesfully = dseGraphPersistenceFactoryApi.reactiveUserAccount(targetUserId)
              if (isUserReactivatedSuccesfully.isDefined) {
                sender() ! ReactivatedResponseSuccess("Spoker Reactivated successfully")
              } else sender ! ReactivateResponseFailure(new Exception(s"Unable suspending spoker $targetUserId (generic error)"), ADM_105)
          }
      }
    }

    case CreateUserSetting(userDetail: User) => {
      dseGraphPersistenceFactoryApi.createUserSetting(userDetail.userId)
      logSpokerDetails(userDetail.userId, userDetail.nickname, userDetail.gender, userDetail.picture.getOrElse(""))
    }

    case ValidateUser(phoneNumber: String) => {
      val phoneNumberValidation = dseGraphPersistenceFactoryApi.isExistsMobileNo(phoneNumber.substring(1))
      phoneNumberValidation match {
        case false =>
          val otpToken = OtpAuthToken(phoneNumber)
          persist(ValidatedUser(otpToken)) {
            case Success(evt) =>
              if (phoneNumber.substring(0, 3) == "+33") {
                sendSMS(phoneNumber, "Bienvenue sur Spok !\nTu y es presque!\nDans l'application Spok saisi le code ci-dessous pour confirmer ton inscription: " + otpToken.otp + "\nSpokly <3")
              } else {
                sendSMS(phoneNumber, "Welcome to Spok! \n You're almost done! \n In the Spok app, enter the code below to confirm your registration:" + otpToken.otp + "\n Spokly yours <3")
              }
              sender ! ValidateUserSuccess(s"OTP has been sent to $phoneNumber.")
            case Failure(err) => sender ! ValidateUserFailure(GENERIC_ERROR_MESSAGE, err)
          }
        case true =>
          sender ! AllReadyRegisteredUser(ALREADY_USED_NUMBER)
      }
    }
    case AuthenticateUser(phoneNumber: String) => {
      val phoneNumberValidation = dseGraphPersistenceFactoryApi.isExistsMobileNo(phoneNumber.substring(1))
      phoneNumberValidation match {
        case true =>
          val otpToken = OtpAuthToken(phoneNumber)
          persist(ValidatedUser(otpToken)) {
            case Success(evt) =>
              if (phoneNumber.substring(0, 3) == "+33") {
                sendSMS(phoneNumber, "Bienvenue sur Spok !\nTu y es presque!\nDans l'application Spok saisi le code ci-dessous pour confirmer ton inscription: " + otpToken.otp + "\nSpokly <3")
              } else {
                sendSMS(phoneNumber, "Welcome to Spok! \n You're almost done! \n In the Spok app, enter the code below to confirm your registration:" + otpToken.otp + "\n Spokly yours <3")
              }
              sender ! AuthenticateUserSuccess(s"OTP has been sent to $phoneNumber.")
            case Failure(err) => sender ! AuthenticateUserFailure(GENERIC_ERROR_MESSAGE_AUTHENTICATE, err)
          }
        case false =>
          sender ! UserNotRegitered(USER_NOT_REGISTERD)
      }
    }

    case UpdateOtpToken(otpToken) => {
      persist(UpdatedOTPToken(otpToken)) {
        case Success(evt) => sender ! UpdateOtpTokenSuccess(otpToken.phoneNumber)
        case Failure(err) => sender ! UpdateOtpTokenFailure(otpToken.phoneNumber, err)
      }
    }

    case ClearOtpToken(phoneNumber: String) => {
      persist(OtpCleared(phoneNumber)) {
        case _ =>
      }
    }

    /**
     * Update phone number step 1
     */
    case ValidatePhoneNumber(oldNumber: String, newNumber: String, userId: String) => {
      val isOldNumberExists = dseGraphPersistenceFactoryApi.isMobileNoExists(oldNumber.substring(1), userId)
      val isNewNumberExists = dseGraphPersistenceFactoryApi.isExistsMobileNo(newNumber.substring(1))
      (isOldNumberExists, isNewNumberExists) match {
        case (true, false) =>
          val otpToken = OtpAuthToken(newNumber)
          persist(PhoneNumberValidated(oldNumber, newNumber, otpToken)) {
            case Success(evt) =>
              sendSMS(newNumber, "OTP - " + otpToken.otp)
              sender ! ValidatePhoneNumberSuccess("OTP has been sent to " + newNumber + ".")
            case Failure(err) => sender ! ValidatePhoneNumberFailure(new Exception(UNABLE_SENDING_OTP_GENERIC_ERROR), IDT_106)
          }
        case (_, _) => sender() ! ValidatePhoneNumberFailure(new Exception(WRONG_PHONE_NUMBER), IDT_004)
      }
    }

    case UpdatePhoneNumber(userId: String, newNumber: String) => {
      persist(PhoneNumberUpdated(userId, newNumber)) {
        case Success(evt) =>
          val isUpdated = dseGraphPersistenceFactoryApi.updatePhoneNumber(userId, newNumber.substring(1))
          isUpdated match {
            case true =>
              ClearOtpToken(newNumber)
              sender ! UpdatePhoneNumberSuccess(PHONE_NUMBER_UPDATED)
            case false => sender ! UpdatePhoneNumberFailure(new Exception(UNABLE_CHANGING_PHONE_NUMBER), IDT_107)
          }
        case Failure(err) => sender ! UpdatePhoneNumberFailure(new Exception(UNABLE_CHANGING_PHONE_NUMBER), IDT_107)
      }
    }

    case FollowUnfollowCommand(followUnfollow) => {
      persist(FollowedOrUnfollowed(followUnfollow)) {
        case Success(evt) => {
          val followedOrUnfollowed = dseGraphPersistenceFactoryApi.performFollowOrUnfollow(
            followUnfollow.userMobileNumber, followUnfollow.followerId, followUnfollow.followingId
          )
          followedOrUnfollowed match {
            case Some(message) => {
              sender ! FollowUnfollowSuccess(message)
              message match {
                case FOLLOWS =>
                  self ! logUserFollowUnfollow(followUnfollow, FOLLOWS)
                  self ! dseGraphPersistenceFactoryApi.updateDefaultGroup(followUnfollow.followerId, followUnfollow.followingId, FOLLOWS)
                  self ! PerformAfterRemovingAndAddingInGroup(ZERO, followUnfollow.followingId)
                case UNFOLLOWS =>
                  self ! logUserFollowUnfollow(followUnfollow, UNFOLLOWS)
                  self ! dseGraphPersistenceFactoryApi.performAfterUnfollowActions(followUnfollow)
                  self ! UpdateAllGroupsWhenUserIsUnfollowed(followUnfollow.followingId)
              }
            }
            case None => sender ! FollowUnfollowFailure(new Exception(FOLLOW_UNFOLLOW_ERROR))
          }
        }
        case Failure(err) => sender ! FollowUnfollowFailure(err)
      }
    }

    case UpdateAllGroupsWhenUserIsUnfollowed(userId: String) => {
      dseGraphPersistenceFactoryApi.updateAllGroupsAfterUserUnfollowed(userId)
    }

    /**
     * Allow user after verifying OTP
     */
    case AllowUser(phoneNumber: String) => {
      persist(UserAllowed(phoneNumber)) {
        case _ =>
      }
    }

    case GroupRemove(groupId: String, userId: String) => {
      persist(GroupRemoved(groupId)) {
        case Success(evt) =>
          val result = dseGraphPersistenceFactoryApi.removeGroup(groupId, userId)
          result match {
            case true => sender() ! GroupRemovedSuccess(groupId)
            case false => sender() ! GroupRemovedFailure(
              groupId,
              new Exception(s"Unable removing group $groupId (generic error).")
            )
          }
        case Failure(err) =>
          sender() ! GroupRemovedFailure(groupId, err)
      }
    }

    case CreateGroup(group: Group, userId: String) => {
      persist(GroupCreated(group)) {
        case Success(evt) => {
          val createGroup = dseGraphPersistenceFactoryApi.createGroup(userId, group)
          createGroup match {
            case Some(msg) => sender() ! GroupCreateSuccess(group)
            case None => sender() ! GroupCreateFailure(group, new Exception(GROUP_CREATION_ERROR))
          }
        }
        case Failure(err) =>
          sender() ! GroupCreateFailure(group, err)
      }
    }
    case UpdateGroup(group: Group, userId: String) => {
      persist(GroupUpdated(group)) {
        case Success(evt) => {
          val updateGroup = dseGraphPersistenceFactoryApi.updateGroup(userId, group)
          updateGroup match {
            case true => sender() ! GroupUpdateSuccess(group)
            case false => sender() ! GroupUpdateFailure(group, new Exception(GROUP_UPDATE_ERROR))
          }
        }
        case Failure(err) =>
          sender() ! GroupUpdateFailure(group, err)
      }
    }

    /**
     * This will update public visibility of the list of followers/following.
     */
    case UpdateSettings(userSetting: UserSetting, userId: String) => {
      persist(SettingsUpdated(userSetting)) {
        case Success(evt) => {
          val updatedRes = dseGraphPersistenceFactoryApi.updateUserSettings(userId, userSetting)
          updatedRes match {
            case true => sender() ! FollowSettingUpdateSuccess(USER_SETTINGS_UPDATE_SUCCESS)
            case false => sender() ! FollowSettingUpdateFailure(
              new Exception(FOLLOWS_SETTING_UPDATE_GENERIC_ERROR), MYA_105
            )
          }
        }
        case Failure(err) =>
          sender() ! FollowSettingUpdateFailure(new Exception(FOLLOWS_SETTING_UPDATE_GENERIC_ERROR), MYA_105)
      }
    }

    case AddFollowersInGroup(userGroup: UserGroup, userId: String) => {
      persist(FollowersAddedInGroup(userGroup)) {
        case Success(evt) => {
          val groupExist = dseGraphPersistenceFactoryApi.isGroupExist(userId, userGroup.groupId)
          if (groupExist) {
            val contactList = userGroup.contacts.map(x => x.phone.substring(1))
            val (alReadyContacts, alReadyUserId) = dseGraphPersistenceFactoryApi.validateUsersOrContactByGroupId(
              userId,
              userGroup.groupId, userGroup.userIds, contactList
            )
            if (alReadyContacts.equals(contactList) && alReadyUserId.equals(userGroup.userIds)) {
              sender() ! AddFollowerInGroupFailure(UserGroup(userGroup.groupId, Nil, Nil), GRP_107, new Exception(USER_AND_CONTACTS_ALREDY_IN_GROUP))
            } else {
              val validUserIdList = userGroup.userIds diff alReadyUserId
              val validListOfContact = userGroup.contacts.filterNot(x => alReadyContacts contains x.phone.substring(1))
              val (invalidContacts, invalidUserIds) = dseGraphPersistenceFactoryApi.insertFollowersInGroup(
                userId,
                UserGroup(userGroup.groupId, validUserIdList, validListOfContact)
              )
              (invalidContacts, invalidUserIds) match {
                case (Nil, Nil) =>
                  sender() ! AddFollowerInGroupSuccess(FOLLOWERS_ADDED_IN_GROUP, Nil, Nil)
                  self ! PerformAfterRemovingAndAddingInGroup(userGroup.groupId, userId)
                case (_, _) =>
                  if (invalidContacts.length.equals(validListOfContact.length) && invalidUserIds.length.equals(validUserIdList.length)) {
                    sender() ! AddFollowerInGroupFailure(
                      UserGroup(userGroup.groupId, validUserIdList, validListOfContact),
                      GRP_105, new Exception(ADD_FOLLOWERS_IN_GROUP_ERROR)
                    )
                  } else {
                    sender() ! AddFollowerInGroupSuccess(FOLLOWERS_ADDED_IN_GROUP, invalidContacts, invalidUserIds)
                    self ! PerformAfterRemovingAndAddingInGroup(userGroup.groupId, userId)
                  }
              }
            }

          } else sender() ! AddFollowerInGroupFailure(userGroup, GRP_105, new Exception(ADD_FOLLOWERS_IN_GROUP_ERROR))
        }
        case Failure(err) =>
          sender() ! AddFollowerInGroupFailure(userGroup, GRP_105, err)
      }
    }

    case PerformAfterRemovingAndAddingInGroup(groupId, userId) => {
      dseGraphPersistenceFactoryApi.updateUserCountInGroup(groupId, userId)
      logger.info("All action performed after adding members in group " + groupId)
    }

    case RemoveFollowersInGroup(removeUserGroup: RemoveUserGroup, userId: String) => {
      persist(FollowersRemovedInGroup(removeUserGroup)) {
        case Success(evt) => {
          val groupExist = dseGraphPersistenceFactoryApi.isGroupExist(userId, removeUserGroup.groupId)
          if (groupExist) {
            val contactList = removeUserGroup.phones map (x => x.substring(1))
            val (alReadyContacts, alReadyUserId) = dseGraphPersistenceFactoryApi.validateUsersOrContactByGroupId(
              userId,
              removeUserGroup.groupId, removeUserGroup.userIds, contactList
            )
            val removeUser = dseGraphPersistenceFactoryApi.removeFollowersFromGroup(
              userId, alReadyUserId, alReadyContacts, removeUserGroup.groupId
            )
            val invalidContacts = contactList diff alReadyContacts
            val invalidUserIds = removeUserGroup.userIds diff alReadyUserId
            removeUser match {
              case true => {
                sender() ! RemoveFollowerInGroupSuccess(FOLLOWERS_REMOVED_IN_GROUP, invalidContacts, invalidUserIds)
                self ! PerformAfterRemovingAndAddingInGroup(removeUserGroup.groupId, userId)
              }
              case false => removeFollowersInGroupFailureResponse(RemoveUserGroup(removeUserGroup.groupId, invalidUserIds, invalidContacts))
            }
          } else removeFollowersInGroupFailureResponse(removeUserGroup)
        }
        case Failure(err) =>
          sender() ! RemoveFollowerInGroupFailure(removeUserGroup, err)
      }
    }

    /**
     * This will update user profile.
     */
    case UpdateUserProfile(userId, userProfile) => {
      persist(UserProfileUpdated(userId, userProfile)) {
        case Success(evt) => {
          val isUserExists: Boolean = dseUserSpokFactoryApi.isExistsUser(userId)
          if (isUserExists) {
            val isUserProfileUpdated: Boolean = dseGraphPersistenceFactoryApi.updateUserProfile(userId, userProfile)
            isUserProfileUpdated match {
              case true => {
                sender() ! UserProfileUpdateSuccess(USER_PROFILE_UPDATE_SUCCESS)
                self ! PerformAfterUserProfileUpdate(userId, userProfile)
              }
              case false => sender() ! UserProfileUpdateFailure(new Exception(USER_PROFILE_UPDATE_GENERIC_ERROR), MYA_101)
            }
          } else {
            sender() ! UserProfileUpdateFailure(new Exception(USER_NOT_FOUND), USR_001)
          }
        }
        case Failure(err) => sender() ! UserProfileUpdateFailure(new Exception(USER_PROFILE_UPDATE_GENERIC_ERROR), MYA_101)
      }
    }

    case promotUserAccount(userId: String, level: String, targetUserId: String) => {
      val isUserSuperAdmin = dseGraphPersistenceFactoryApi.checkUserSuperAdminOrNot(userId)
      isUserSuperAdmin match {
        case Some(true) =>
          val targetUserLevel = dseGraphPersistenceFactoryApi.fetchUserLevel(targetUserId)
          targetUserLevel match {
            case Some(userLevel) => if (userLevel == level) sender() ! PromotUserAccountFailure(new Exception(s"Already promoted spoker $targetUserId  to this level"), ADM_004)
            else {
              val setLevel = dseGraphPersistenceFactoryApi.setUserLevel(targetUserId, level)
              setLevel match {
                case Some(true) => sender() ! PromotUserAccountSuccess(s"$targetUserId level updated successfully")
                case Some(false) =>
                  sender() ! PromotUserAccountFailure(new Exception(s" Unable updating spoker Level (generic error) $targetUserId "), ADM_101)
                case None => sender() ! PromotUserAccountFailure(new Exception(s" Unable updating spoker Level (generic error) $targetUserId "), ADM_101)
              }
            }
            case None => sender() ! PromotUserAccountFailure(new Exception(s" Spoker $targetUserId not found"), USR_001)
          }
        case Some(false) => sender() ! PromotUserAccountFailure(new Exception(s"Not allowed to promote a spoker."), ADM_001)
        case None => sender() ! PromotUserAccountFailure(new Exception(s" Unable updating spoker Level (generic error) $targetUserId "), ADM_101)
      }

    }

    case PerformAfterUserProfileUpdate(userId, userProfile) => {
      logUserProfileUpdate(userId, userProfile)
      logSpokerDetails(userId, userProfile.nickname, userProfile.gender, userProfile.picture.getOrElse(""))
    }

    /**
     * This will update user help Setting.
     */
    case UpdateHelpSettings(userId: String) => {
      persist(HelpSettingsUpdated(userId)) {
        case Success(evt) => {
          val updatedRes = dseGraphPersistenceFactoryApi.updateUserHelpSetting(userId)
          updatedRes match {
            case true => sender() ! HelpSettingUpdateSuccess(HELP_SETTING_UPDATE_SUCCESS)
            case false => sender() ! HelpSettingUpdateFailure(new Exception(HELP_SETTING_UPDATE_GENERIC_ERROR), MYA_104)
          }
        }
        case Failure(err) =>
          sender() ! HelpSettingUpdateFailure(new Exception(HELP_SETTING_UPDATE_GENERIC_ERROR), MYA_104)
      }
    }

    /**
     * This will send email to the support team when user requires support
     */
    case ProvideSupport(userId, phoneNumber, message) => {
      persist(SupportProvided(userId, phoneNumber, message)) {
        case Success(evt) => {
          val nickname = dseGraphPersistenceFactoryApi.getNickname(userId)
          nickname match {
            case Some(name) => {
              val updatedRes = mailingApi.sendMail(SUPPORT, message, name, userId)
              updatedRes match {
                case Some(num) => sender() ! SupportProvidedSuccess(userId, phoneNumber, message)
                case None => sender() ! SupportProvidedFailure(new Exception("Unable sending message to the support (generic error)."), IDT_108)
              }
            }
            case None => sender() ! SupportProvidedFailure(new Exception("Unable sending message to the support (generic error)."), IDT_108)
          }
        }
        case Failure(err) =>
          sender() ! SupportProvidedFailure(new Exception("Unable sending message to the support (generic error)."), IDT_108)
      }
    }

  }

  private def removeFollowersInGroupFailureResponse(removeUserGroup: RemoveUserGroup) = {
    sender() ! RemoveFollowerInGroupFailure(
      removeUserGroup,
      new Exception(s"Unable removing user(s) or contact(s) from group ${removeUserGroup.groupId} (generic error).  " +
        s"invalidContact: ${removeUserGroup.phones}  invalidUserIds: ${removeUserGroup.userIds}")
    )
  }

  /**
   * Event handlers.
   */
  override val onEvent: Receive = {

    case _ =>

  }

}
