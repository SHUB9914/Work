package com.spok.accountsservice.service

import com.spok.model.Account.{ UserProfile, UserSetting, _ }
import com.spok.model.OtpAuthToken

object AccountActorCommand {

  case class Token()

  case class CreateAccount(userDetail: User)

  case class SuspendAccount(userId: String, targetUserId: String)

  case class ReactivateAccount(userId: String, targetUserId: String)

  case class ValidateUser(phoneNumber: String)

  case class AuthenticateUser(phoneNumber: String)

  case class UpdateOtpToken(otpToken: OtpAuthToken)

  case class ClearOtpToken(phoneNumber: String)

  case class FollowUnfollowCommand(followUnfollow: FollowUnfollow)

  case class GroupRemove(groupId: String, userId: String)

  case class AllowUser(phoneNumber: String)

  case class CreateGroup(group: Group, userId: String)

  case class AddFollowersInGroup(userGroup: UserGroup, userId: String)

  case class RemoveFollowersInGroup(removeUserGroup: RemoveUserGroup, userId: String)

  case class ValidatePhoneNumber(oldNumber: String, newNumber: String, userId: String)

  case class ProvideSupport(userId: String, phoneNumber: String, message: String)

  case class PerformAfterRemovingAndAddingInGroup(groupId: String, userId: String)

  case class PerformAfterUserProfileUpdate(userId: String, userProfile: UserProfile)

}

