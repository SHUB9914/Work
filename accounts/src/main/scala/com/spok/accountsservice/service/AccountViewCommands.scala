package com.spok.accountsservice.service

import com.spok.model.Account.{ MyDetails, SingleGroupDetails, UserGroupsDetails, _ }
import com.spok.model.OtpAuthToken

object AccountViewCommands {

  case class GetAccount(accountId: String)

  case class GetOTPToken(phoneNumber: String)

  case class ValidateGroup(groupId: String, userId: String)

  case class GetValidUser(phoneNumber: String)

  case class ViewUserMinimalDetails(targetUserId: String)

  case class GetUserProfileFullDetails(targetUserId: String, userId: String)

  case class Disable(userId: String, targetUserId: String)

  //case class Suspend(userId: String, targetUserId: String)

  case class DisableUser(userId: String)

  case class GetMyDetails(userId: String)

  case class GetFollowers(userId: String, targetUserId: String, pos: String)

  case class GetFollowings(userId: String, targetUserId: String, pos: String)

  case class GetGroupDetailsForUser(userId: String, pos: String)

  case class GetSingleGroupDetails(userId: String, groupId: String, pos: String)

  case class GetDetails(userId: String, targetId: Option[String])

  case class IsUserSuspended(targetId: String)

  case class performAfterUserDisableCleanUp(userId: String)

}

