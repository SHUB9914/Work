package com.spok.accountsservice.service

import akka.actor.ActorRef
import com.datastax.driver.dse.graph.Vertex
import com.rbmhtechnology.eventuate.EventsourcedView
import com.spok.model.Account._
import com.spok.model.{ Account, OtpAuthToken }
import com.spok.persistence.factory.{ DSEGraphPersistenceFactoryApi, DSEUserSpokFactoryApi }
import com.spok.util.Constant._
import com.spok.util.{ JsonHelper, OtpGenerationUtil }
import com.spok.accountsservice.service.AccountActorEvents._
import com.spok.accountsservice.service.AccountActorUpdateEvents._
import com.spok.accountsservice.service.AccountViewCommands._
import com.spok.accountsservice.service.AccountSuccessViewReplies._
import com.spok.accountsservice.service.AccountViewFailureReplies._
import com.spok.model.SpokModel.Error
import com.spok.persistence.dse.DseGraphFactory
// Replies

class AccountView(replicaId: String, override val eventLog: ActorRef)
    extends EventsourcedView with OtpGenerationUtil with JsonHelper {

  //scalastyle:off
  import context.dispatcher
  private var phoneOtpMap: Map[String, OtpAuthToken] = Map()
  override val id = s"s-av-$replicaId"
  private var validatedUsers: Set[String] = Set.empty
  //scalastyle:on

  val dseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = DSEGraphPersistenceFactoryApi
  val dseUserSpokFactoryApi: DSEUserSpokFactoryApi = DSEUserSpokFactoryApi

  /**
   * Command handler.
   */
  override def onCommand: Receive = {

    case GetOTPToken(phoneNumber) =>
      phoneOtpMap.get(phoneNumber) match {
        case Some(otpToken) =>

          sender ! (if (otpToken.isExpired) {
            FindOtpTokenFailure(OTP_EXPIRED)
          } else {
            FindOtpTokenSuccess(otpToken)
          })
        case None => sender ! FindOtpTokenFailure(GENERIC_OTP_ERROR_MESSAGE)
      }

    case ValidateGroup(groupId, userId) => sender() ! IsValidGroupAck(dseGraphPersistenceFactoryApi.isGroupExist(userId, groupId))

    case GetValidUser(phoneNumber) => sender ! validatedUsers.contains(phoneNumber)

    case ViewUserMinimalDetails(targetUserId) => {
      val isUserExists = checkUserExists(targetUserId)
      if (isUserExists) {
        val userMinimalDetails = dseGraphPersistenceFactoryApi.getUserMinimalDetails(targetUserId)
        userMinimalDetails match {
          case Some(minimalDetail) => sender() ! ViewUserMinimalDetailsSuccessResponse(minimalDetail)
          case None => sender() ! ViewUserMinimalDetailsFailureResponse(new Exception(s"Unable loading user $targetUserId (generic error)"), USR_101)
        }
      } else sender() ! ViewUserMinimalDetailsFailureResponse(new Exception(s"User $targetUserId not found"), USR_001)
    }

    /**
     * This will get user profile full details.
     */
    case GetUserProfileFullDetails(targetUserId, userId) => {
      val isUserExists = checkUserExists(targetUserId)
      if (isUserExists) {
        val userProfileDetails = dseGraphPersistenceFactoryApi.viewFullUserProfile(targetUserId, userId)
        userProfileDetails match {
          case Some(userProfileDetails) => sender() ! UserProfileFullDetailsSuccess(userProfileDetails)
          case None => sender() ! UserProfileFullDetailsFailure(
            new Exception(s"Unable loading user $userId (generic error)."), USR_101
          )
        }
      } else {
        sender() ! UserProfileFullDetailsFailure(new Exception(s"User $userId not found"), USR_001)
      }
    }
    case Disable(userId, targetUserId) => {
      val isUserAdmin: Option[Boolean] = dseGraphPersistenceFactoryApi.checkUserAdminOrNot(userId)
      isUserAdmin match {
        case Some(true) =>
          val isTargetAdmin = dseGraphPersistenceFactoryApi.checkUserAdminOrNot(targetUserId)
          isTargetAdmin match {
            case Some(true) =>
              sender() ! DisableResponseFailure(new Exception(s"Unable to disbale the account because $targetUserId is a admin"), USR_107)
            case Some(false) =>
              val response: Option[Vertex] = dseGraphPersistenceFactoryApi.fetchUserAccountDisableOrNot(targetUserId)
              response match {
                case None => sender() ! DisableResponseFailure(new Exception(s"user $targetUserId not found"), USR_001)
                case Some(userV) =>
                  if (dseGraphPersistenceFactoryApi.disableUserAccount(targetUserId, userV)) {
                    sender() ! DisableResponseSuccess("user disabled successfully")
                    self ! performAfterUserDisableCleanUp(targetUserId)
                  } else sender() ! DisableResponseFailure(new Exception(s"Unable to disable the user $userId "), USR_104)
              }
            case None => sender() ! DisableResponseFailure(new Exception(s" Unable disabling account (generic error) $targetUserId "), MYA_106)
          }
        case Some(false) => sender() ! DisableResponseFailure(new Exception(s" User_Id $userId is not a Admin"), USR_106)
        case None => sender() ! DisableResponseFailure(new Exception(s" Unable disabling account (generic error) $targetUserId "), MYA_106)
      }
    }

    case performAfterUserDisableCleanUp(userId: String) => {
      dseGraphPersistenceFactoryApi.performCleanUp(userId)
    }

    case DisableUser(userId: String) => {
      val response: Option[Vertex] = dseGraphPersistenceFactoryApi.fetchUserAccountDisableOrNot(userId)
      response match {
        case None => sender() ! DisableResponseFailure(new Exception(s"user $userId not found"), USR_001)
        case Some(userV) =>
          if (dseGraphPersistenceFactoryApi.disableUserAccount(userId, userV)) {
            sender() ! DisableResponseSuccess("user disabled successfully")
            self ! performAfterUserDisableCleanUp(userId)
          } else sender() ! DisableResponseFailure(new Exception(s"Unable to disable the user $userId "), USR_104)

      }

    }
    /**
     * This will get list of followers of an user.
     */
    case GetFollowers(userId, targetUserId, pos) => {
      val isUserExists = checkUserExists(targetUserId)
      isUserExists match {
        case true =>
          val isSettingAlowed = if (userId == targetUserId) true else dseGraphPersistenceFactoryApi.fetchUserSettings(targetUserId, FOLLOWER)
          isSettingAlowed match {
            case true =>
              val userFollowers = dseGraphPersistenceFactoryApi.fetchFollowers(targetUserId, pos)
              userFollowers match {
                case Some(followers) => sender() ! FollowersResponseSuccess(followers)
                case None =>
                  sender() ! FollowersResponseFailure(new Exception(s"Unable loading user $targetUserId followers (generic error)"), FLW_102)
              }
            case false =>
              sender() ! FollowersResponseFailure(new Exception(s"Not allowed to load user $targetUserId followers."), FLW_001)
          }
        case false =>
          sender() ! FollowersResponseFailure(new Exception(s"User $targetUserId not found"), USR_001)
      }
    }

    /**
     * This will get list of followings of an user.
     */
    case GetFollowings(userId, targetUserId, pos) => {
      val isUserExists = checkUserExists(targetUserId)
      isUserExists match {
        case true =>
          val isSettingAlowed = if (userId == targetUserId) true else dseGraphPersistenceFactoryApi.fetchUserSettings(targetUserId, FOLLOWINGS)
          isSettingAlowed match {
            case true =>
              val userFollowings = dseGraphPersistenceFactoryApi.fetchFollowings(targetUserId, pos)
              userFollowings match {
                case Some(followings) => sender() ! FollowingsResponseSuccess(followings)
                case None => sender() ! FollowingsResponseFailure(new Exception(
                  s"Unable loading user $targetUserId followings (generic error)."
                ), "FLW-103")
              }
            case false => sender() ! FollowingsResponseFailure(new Exception(s"Not allowed to load user $targetUserId followings."), FLW_002)
          }
        case false => sender() ! FollowingsResponseFailure(new Exception(s"User $targetUserId not found."), USR_001)
      }
    }

    case GetGroupDetailsForUser(userId, pos) => {
      logger.info("Hitting fetch group request !!!!!Account View!!!!!!!!!!!!!!!!!!!!" + userId)
      val groupDetails = dseGraphPersistenceFactoryApi.fetchGroupDetailsForAUser(userId, pos)
      logger.info("Got Result for fetch group request !!!!!Account View!!!!!!!!!!!!!!!!!!!!" + userId)
      groupDetails match {
        case None => sender() ! GetGroupDetailsForFailure(new Exception(LOAD_GROUP_DETAILS_GENERIC_ERROR))
        case Some(groups) => sender() ! GetGroupDetailsForSuccess(groups)
      }
    }

    /**
     * This will get my details.
     */
    case GetMyDetails(userId) => {
      val myDetails = dseGraphPersistenceFactoryApi.viewMyProfile(userId)
      myDetails match {
        case Some(myDetail) => sender() ! MyDetailsSuccess(myDetail)
        case None => sender() ! MyDetailsFailure(new Exception(USER_PROFILE_LOADING_GENERIC_ERROR), MYA_104)
      }
    }

    case IsUserSuspended(userId) => {
      val isSpokerSuspended = dseGraphPersistenceFactoryApi.isUserSuspendAlready(userId)
      isSpokerSuspended match {
        case Some(false) => sender ! IsUserSuspendedAsk(SPOKER_NOT_SUSPENDED)
        case Some(true) => sender() ! IsUserSuspendedAsk(SPOKER_SUSPENDED)
        case None => sender() ! IsUserSuspendedAsk(PROPERTY_NOT_FOUND)
      }
    }

    /**
     * This will get logged in user details.
     */
    case GetDetails(userId, targetId) => {
      if (!targetId.isDefined) {
        val myDetails: (Option[LoggedUsersDetails], Option[Error], Option[Vertex]) = dseGraphPersistenceFactoryApi.fetchMyProfile(userId)
        myDetails match {
          case (Some(myDetail), None, Some(userV)) => sender() ! DetailsSuccess(myDetail)
          case (None, Some(err), _) => sender() ! DetailsFailure(err.id, err.message)
        }
      } else {
        val isAdmin = dseGraphPersistenceFactoryApi.checkUserAdminOrNot(userId)
        isAdmin match {
          case Some(true) =>
            val isTragetIdExist = dseGraphPersistenceFactoryApi.fetchUserAccountDisableOrNot(targetId.get)
            isTragetIdExist match {
              case Some(targetUser) =>
                val myDetails: (Option[LoggedUsersDetails], Option[Error], Option[Vertex]) = dseGraphPersistenceFactoryApi.fetchMyProfile(targetId.get)
                myDetails match {
                  case (Some(myDetail), None, Some(userV)) =>
                    val isAdmin = dseGraphPersistenceFactoryApi.checkUserAdminOrNot(userId)
                    val userDetails: (Option[Account.SpokerFewDetails], Option[Error]) = dseGraphPersistenceFactoryApi.fetchUserInfo(targetId.get, userV)
                    userDetails match {
                      case (Some(userDetails), None) =>
                        val spoker = Spoker(myDetail.id, myDetail.countryCode, myDetail.phoneNumber, myDetail.birthDate, myDetail.nickname, myDetail.gender,
                          myDetail.geo, myDetail.geoText, userDetails.cover, userDetails.last_activity, userDetails.last_position, userDetails.picture,
                          userDetails.token)
                        sender() ! DetailsByAdminSuccess(SpokeFullDetails(spoker))

                      case (None, Some(err)) => sender() ! DetailsFailure(err.id, err.message)
                    }
                  case (None, Some(err), _) => sender() ! DetailsFailure(err.id, err.message)
                }
              case None => sender() ! DetailsByAdminFailure(s"user $userId not found", USR_001)
            }
          case Some(false) => sender() ! DetailsByAdminFailure(s" User_Id $userId is not an Admin", USR_106)
          case None => sender() ! DetailsByAdminFailure(s" Unable to view a user account  (generic error) $userId ", MYA_107)
        }
      }
    }

    case GetSingleGroupDetails(userId, groupId, pos) => {
      logger.info("Fetching details of a group for ::: " + userId)
      val (groupDetails, error) = dseGraphPersistenceFactoryApi.getSingleGroupDetails(userId, groupId, pos)
      logger.info("Got response for specific group ::: " + userId)
      (groupDetails, error) match {
        case (Some(details), None) => sender() ! GetSingleGroupDetailsSuccess(details)
        case (None, Some(err)) => sender() ! GetSingleGroupDetailsFailure(err.id, err.message)
      }
    }

  }

  private def checkUserExists(userId: String): Boolean = {
    dseUserSpokFactoryApi.isExistsUser(userId)
  }

  /**
   * Event handlers.
   */
  override val onEvent: Receive = {

    case ValidatedUser(otpToken) => {
      if (!otpToken.isExpired) {
        phoneOtpMap += (otpToken.phoneNumber -> otpToken)
      }
    }

    case UpdatedOTPToken(otpToken) => {
      if (!otpToken.isExpired) {
        phoneOtpMap += (otpToken.phoneNumber -> otpToken)
      }
    }

    case PhoneNumberValidated(oldNumber, newNumber, otpToken) => {
      if (!otpToken.isExpired) {
        phoneOtpMap += (otpToken.phoneNumber -> otpToken)
      }
    }

    case OtpCleared(phoneNumber) => {
      phoneOtpMap -= phoneNumber
    }

    case OtpExpiredCleared => {
      phoneOtpMap.map {
        case (phoneNumber, token) =>
          if (token.isExpired) {
            phoneOtpMap -= phoneNumber
          }
      }

    }

    case UserAllowed(phoneNumber) => {
      validatedUsers += phoneNumber
      phoneOtpMap -= phoneNumber
    }

    case AccountCreated(user) => {
      validatedUsers -= user.userNumber
    }
  }

}

