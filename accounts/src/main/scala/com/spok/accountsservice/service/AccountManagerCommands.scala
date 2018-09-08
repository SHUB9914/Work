package com.spok.accountsservice.service

import com.spok.model.Account.{ UserProfile, UserSetting, _ }

/**
 * Contains all the manager commands for an account
 */
object AccountManagerCommands {

  case class Create(userDetail: User)

  case class Suspend(userId: String, targetUserId: String, phoneNumber: String)

  case class Recativate(userId: String, targetUserId: String, phoneNumber: String)

  case class Validate(phoneNumber: String)

  case class Authenticate(phoneNumber: String)

  case object ClearExpiredOtpToken

  case class FollowUnfollowAction(followUnfollow: FollowUnfollow)

  case class RemoveGroup(groupId: String, userId: String, phoneNumber: String)

  case class CreateUserGroup(group: Group, phoneNumber: String, userId: String)

  case class UpdateUserGroup(groupId: Group, phoneNumber: String, userId: String)

  case class AddFollowers(userGroup: UserGroup, phoneNumber: String, userId: String)

  case class RemoveFollowers(removeUserGroup: RemoveUserGroup, phoneNumber: String, userId: String)

  case class UpdateProfile(phoneNumber: String, userId: String, userProfile: UserProfile)

  case class UpdateUserSettings(userSetting: UserSetting, userId: String)

  case class ValidateNumber(phoneNumber: String, oldNumber: String, newNumber: String, userId: String)

  case class UpdateUserHelpSettings(userId: String)

  case class UpdateNumber(userId: String, phoneNumber: String, newNumber: String)

  case class AskSupport(userId: String, phoneNumber: String, message: String)

  case class promotUser(userId: String, level: String, spokerId: String)

}
