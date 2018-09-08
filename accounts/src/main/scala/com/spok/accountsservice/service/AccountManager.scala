package com.spok.accountsservice.service

import akka.actor.{ ActorRef, Props }
import akka.util.Timeout
import com.rbmhtechnology.eventuate.EventsourcedActor
import scala.collection.mutable.Map
import scala.concurrent.duration._
import com.spok.accountsservice.service.AccountActorCommand._
import com.spok.accountsservice.service.AccountActorUpdateCommands._
import com.spok.accountsservice.service.AccountManagerCommands._
import com.spok.accountsservice.service.AccountActorEvents._

case object OtpExpiredCleared

class AccountManager(replicaId: String, override val eventLog: ActorRef)
    extends EventsourcedActor {

  private implicit val timeout = Timeout(10.seconds)

  private val accountActors: Map[String, ActorRef] = Map.empty

  override val id = s"s-am-$replicaId"

  /**
   * Command handler.
   */
  override def onCommand: Receive = {

    case Create(userDetail) =>
      accountActor(userDetail.userNumber) forward CreateAccount(userDetail)

    case Suspend(userId, targetUserId, phoneNumber) =>
      accountActor(phoneNumber) forward SuspendAccount(userId, targetUserId)

    case Recativate(userId, targetUserId, phoneNumber) =>
      accountActor(phoneNumber) forward ReactivateAccount(userId, targetUserId)

    case Validate(phoneNumber) =>
      accountActor(phoneNumber) forward ValidateUser(phoneNumber)

    case Authenticate(phoneNumber) =>
      accountActor(phoneNumber) forward AuthenticateUser(phoneNumber)

    case UpdateOtpToken(otpAuthToken) =>
      accountActor(otpAuthToken.phoneNumber) forward UpdateOtpToken(otpAuthToken)

    case ClearOtpToken(phoneNumber) =>
      accountActor(phoneNumber) forward ClearOtpToken(phoneNumber)

    case FollowUnfollowAction(followUnfollow) =>
      accountActor(followUnfollow.userMobileNumber) forward FollowUnfollowCommand(followUnfollow)

    case RemoveGroup(groupId, userId, phoneNumber) =>
      accountActor(phoneNumber) forward GroupRemove(groupId, userId)

    case CreateUserGroup(group, phoneNumber, userId) =>
      accountActor(phoneNumber) forward CreateGroup(group, userId)

    case UpdateUserGroup(group, phoneNumber, userId) =>
      accountActor(phoneNumber) forward UpdateGroup(group, userId)

    case ClearExpiredOtpToken => {
      persist(OtpExpiredCleared) {
        case _ =>
      }
    }

    case AddFollowers(group, phoneNumber, userId) =>
      accountActor(phoneNumber) forward AddFollowersInGroup(group, userId)

    case RemoveFollowers(removeUserGroup, phoneNumber, userId) =>
      accountActor(phoneNumber) forward RemoveFollowersInGroup(removeUserGroup, userId)

    case AllowUser(phoneNumber) =>
      accountActor(phoneNumber) forward AllowUser(phoneNumber)

    /**
     * This will update user profile.
     */
    case UpdateProfile(phoneNumber, userId, userProfile) =>
      accountActor(phoneNumber) forward UpdateUserProfile(userId, userProfile)

    case promotUser(userId, level, spokerId) =>
      accountActor(userId) forward promotUserAccount(userId, level, spokerId)

    /**
     * This will update user follower adn following settingg
     */
    case UpdateUserSettings(userSettings, userId) =>
      accountActor(userId) forward UpdateSettings(userSettings, userId)

    /**
     * This will validate old number and send OTP to new number
     */
    case ValidateNumber(phoneNumber, oldNumber, newNumber, userId) =>
      accountActor(phoneNumber) forward ValidatePhoneNumber(oldNumber, newNumber, userId)

    /**
     * This will update user's phone number
     */
    case UpdateNumber(userId, phoneNumber, newNumber) =>
      accountActor(phoneNumber) forward UpdatePhoneNumber(userId, newNumber)

    /**
     * This will update user help setting
     */
    case UpdateUserHelpSettings(userId) =>
      accountActor(userId) forward UpdateHelpSettings(userId)

    case AskSupport(userId, phoneNumber, message) =>
      accountActor(phoneNumber) forward ProvideSupport(userId, phoneNumber, message)
  }

  /**
   * Event handler.
   */
  override def onEvent: Receive = {
    case AccountCreated(user) if !accountActors.contains(user.userNumber) => accountActor(user.userNumber)
  }

  /**
   * Find or create and return the Account actor by id.
   *
   * @param phoneNumber the user id.
   * @return the Notification actor ActorRef.
   */
  private def accountActor(phoneNumber: String): ActorRef = {
    accountActors.get(phoneNumber) match {
      case Some(accountActor) => accountActor
      case None =>
        accountActors += (phoneNumber -> context.actorOf(Props(
          createActor(phoneNumber)
        ), phoneNumber))
        accountActors(phoneNumber)

    }
  }

  def createActor(phoneNumber: String): AccountActor = {
    new AccountActor(phoneNumber, Some(phoneNumber), eventLog)
  }
}
