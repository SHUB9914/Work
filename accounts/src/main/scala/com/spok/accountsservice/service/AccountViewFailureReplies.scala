package com.spok.accountsservice.service

/**
 * Contains all the replies for account view failures.
 */
object AccountViewFailureReplies {

  case class FindOtpTokenFailure(message: String)

  case class ViewUserMinimalDetailsFailureResponse(cause: Throwable, errorCode: String)

  case class UserProfileFullDetailsFailure(cause: Throwable, errorCode: String)

  case class FollowersResponseFailure(cause: Throwable, errorCode: String)

  case class DisableResponseFailure(cause: Throwable, errorCode: String)

  case class FollowingsResponseFailure(cause: Throwable, errorCode: String)

  case class GetGroupDetailsForFailure(cause: Throwable)

  case class MyDetailsFailure(cause: Throwable, errorCode: String)

  case class GetSingleGroupDetailsFailure(errorId: String, errorMessage: String)

  case class DetailsFailure(cause: String, errorCode: String)

  case class DetailsByAdminFailure(cause: String, errorCode: String)

}
