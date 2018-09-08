package com.spok.accountsservice.service

import com.spok.model.Account.{ MyDetails, SingleGroupDetails, UserGroupsDetails, _ }
import com.spok.model.OtpAuthToken
import com.spok.model.SpokModel.GroupsResponse

/**
 * Contains all the success commands when any view is done for an account
 */
object AccountSuccessViewReplies {

  case class ReadSuccess(userDetail: Option[User])

  case class FindOtpTokenSuccess(otpToken: OtpAuthToken)

  case class IsValidGroupAck(groupExistStatus: Boolean)

  case class ViewUserMinimalDetailsSuccessResponse(userMinimalDetails: UserMinimalDetailsResponse)

  case class UserProfileFullDetailsSuccess(userProfileFullDetails: UserProfileFullDetails)

  case class FollowersResponseSuccess(userFollowers: UserFollowers)

  case class FollowingsResponseSuccess(userFollowings: UserFollowings)

  case class GetGroupDetailsForSuccess(response: GroupsResponse)

  case class MyDetailsSuccess(myDetails: MyDetails)

  case class DisableResponseSuccess(message: String)

  case class GetSingleGroupDetailsSuccess(singleGroupDetails: SingleGroupDetails)

  case class IsUserSuspendedAsk(status: String)

  case class DetailsSuccess(loggedUsersDetails: LoggedUsersDetails)

  case class DetailsByAdminSuccess(spoker: SpokeFullDetails)

}
