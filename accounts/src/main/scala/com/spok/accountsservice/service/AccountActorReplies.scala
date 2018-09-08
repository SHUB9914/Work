package com.spok.accountsservice.service

import com.spok.model.Account.{ Group, RemoveUserGroup, User, UserGroup }
import com.spok.model.SpokDataResponse

sealed trait AccountAck

object AccountActorSuccessReplies {

  case class AccountCreateSuccess(userDetail: User) extends AccountAck

  case class SuspendResponseSuccess(message: String) extends AccountAck

  case class ReactivatedResponseSuccess(message: String) extends AccountAck

  case class ValidateUserSuccess(message: String) extends AccountAck

  case class AuthenticateUserSuccess(message: String) extends AccountAck

  case class UpdateOtpTokenSuccess(phoneNumber: String) extends AccountAck

  case class FollowUnfollowSuccess(followUnfollows: String) extends AccountAck

  case class GroupRemovedSuccess(groupId: String) extends AccountAck

  case class GroupCreateSuccess(group: Group) extends AccountAck

  case class AddFollowerInGroupSuccess(message: String, invalidContacts: List[String], invalidUserIds: List[String]) extends AccountAck with SpokDataResponse

  case class RemoveFollowerInGroupSuccess(message: String, invalidContacts: List[String], invalidUserIds: List[String]) extends AccountAck with SpokDataResponse

  case class GroupUpdateSuccess(group: Group) extends AccountAck

  case class UserProfileUpdateSuccess(updateUserProfileResponse: String) extends AccountAck

  case class ValidatePhoneNumberSuccess(message: String) extends AccountAck

  case class FollowSettingUpdateSuccess(message: String) extends AccountAck

  case class UpdatePhoneNumberSuccess(message: String) extends AccountAck

  case class HelpSettingUpdateSuccess(message: String) extends AccountAck

  case class PromotUserAccountSuccess(message: String) extends AccountAck

  case class SupportProvidedSuccess(userId: String, phoneNumber: String, message: String) extends AccountAck

}

object AccountActorFailureReplies {

  case class AccountCreateFailure(userDetail: User, cause: Throwable) extends AccountAck

  case class SuspendResponseFailure(cause: Throwable, errorCode: String) extends AccountAck

  case class ReactivateResponseFailure(cause: Throwable, errorCode: String) extends AccountAck

  case class ValidateUserFailure(message: String, cause: Throwable) extends AccountAck

  case class AuthenticateUserFailure(message: String, cause: Throwable) extends AccountAck

  case class UpdateOtpTokenFailure(phoneNumber: String, cause: Throwable) extends AccountAck

  case class FollowUnfollowFailure(cause: Throwable) extends AccountAck

  case class GroupRemovedFailure(groupId: String, cause: Throwable) extends AccountAck

  case class GroupCreateFailure(group: Group, cause: Throwable) extends AccountAck

  case class AddFollowerInGroupFailure(userGroup: UserGroup, errorCode: String, cause: Throwable) extends AccountAck

  case class RemoveFollowerInGroupFailure(removeUserGroup: RemoveUserGroup, cause: Throwable) extends AccountAck

  case class GroupUpdateFailure(group: Group, cause: Throwable) extends AccountAck

  case class UserProfileUpdateFailure(cause: Throwable, errorCode: String) extends AccountAck

  case class PromotUserAccountFailure(cause: Throwable, errorCode: String) extends AccountAck

  case class ValidatePhoneNumberFailure(cause: Throwable, errorCode: String) extends AccountAck

  case class FollowSettingUpdateFailure(cause: Throwable, errorCode: String) extends AccountAck

  case class UpdatePhoneNumberFailure(cause: Throwable, errorCode: String) extends AccountAck

  case class HelpSettingUpdateFailure(cause: Throwable, errorCode: String) extends AccountAck

  case class SupportProvidedFailure(cause: Throwable, errorCode: String) extends AccountAck
}

object AccountAlreadyRegisters {

  case class AllReadyRegisteredUser(message: String) extends AccountAck

  case class UserNotRegitered(message: String) extends AccountAck
}
