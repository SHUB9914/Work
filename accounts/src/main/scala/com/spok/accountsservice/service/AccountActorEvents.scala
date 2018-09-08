package com.spok.accountsservice.service

import com.spok.model.Account.{ UserProfile, UserSetting, _ }
import com.spok.model.OtpAuthToken

object AccountActorEvents {

  case class AccountCreated(userDetail: User)

  case class ValidatedUser(otpToken: OtpAuthToken)

  case class OtpCleared(phoneNumber: String)

  case class FollowedOrUnfollowed(followUnfollow: FollowUnfollow)

  case class GroupRemoved(groupId: String)

  case class UserAllowed(phoneNumber: String)

  case class GroupCreated(group: Group)

  case class FollowersAddedInGroup(userGroup: UserGroup)

  case class FollowersRemovedInGroup(removeUserGroup: RemoveUserGroup)

  case class PhoneNumberValidated(oldNumber: String, newNumber: String, otpToken: OtpAuthToken)

  case class SupportProvided(userId: String, phoneNumber: String, message: String)

}

object AccountActorUpdateEvents {

  case class UpdatedOTPToken(otpToken: OtpAuthToken)

  case class GroupUpdated(group: Group)

  case class UserProfileUpdated(userId: String, userProfile: UserProfile)

  case class SettingsUpdated(userSetting: UserSetting)

  case class HelpSettingsUpdated(helpSetting: String)

  case class PhoneNumberUpdated(userId: String, newNumber: String)

}
