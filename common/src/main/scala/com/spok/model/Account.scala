package com.spok.model

import java.util.Date

import com.spok.model.SpokModel.Geo

trait SpecificGroupResponse

object Account {

  /**
   * Account response to collect responses of all apis
   */
  case class AccountResponse(
    followResponse: Option[FollowUnfollow] = None,
    unFollowResponse: Option[FollowUnfollow] = None,
    isFriend: Option[Boolean] = None,
    updateUserProfileResponse: Option[String] = None
  ) extends SpokDataResponse

  case class User(
    nickname: String,
    birthDate: Date,
    location: Location,
    gender: String,
    contacts: List[String],
    userNumber: String,
    userId: String,
    picture: Option[String],
    cover: Option[String] = None,
    geoText: String = "",
    level: String = "",
    isSuspended: Boolean = false
  )

  case class FollowUnfollow(
    userMobileNumber: String,
    followerId: String,
    followingId: String
  )

  case class userDisableDetails(
    nickname: String,
    birthDate: String,
    location: String,
    gender: String,
    userNumber: String,
    userId: String,
    picture: String,
    cover: String,
    geoText: String,
    level: String
  )

  /**
   * Sent User Registration response
   * to API service
   *
   * @param token
   * @param userContactsIds
   */
  case class UserRegistrationResponse(
    token: String,
    userId: String,
    userContactsIds: List[String]
  ) extends SpokDataResponse

  case class UserAuthenticationResponse(
    token: String,
    userId: String
  ) extends SpokDataResponse

  /**
   * Group object:- Associated with users
   *
   * @param title
   */
  case class Group(
    id: String,
    title: String
  )

  case class UserGroup(
    groupId: String,
    userIds: List[String],
    contacts: List[Contact]
  )

  case class Contact(
    name: String,
    phone: String
  )

  case class UserGroupDetails(
    userIds: List[String],
    contacts: List[Contact]
  )

  case class RemoveUserGroup(
    groupId: String,
    userIds: List[String],
    phones: List[String]
  )

  case class RemoveUserGroupDetails(
    userIds: List[String],
    phones: List[String]
  )

  case class UserProfileFullDetails(
    id: String,
    nickname: String,
    gender: String,
    picture: String,
    cover: String,
    nbFollowers: Long,
    nbFollowing: Long,
    nbSpoks: Long,
    isFollower: Boolean,
    isFollowing: Boolean
  ) extends SpokDataResponse

  case class UserProfile(
    nickname: String,
    birthDate: Date,
    gender: String,
    picture: Option[String],
    cover: Option[String],
    geo: Geo,
    geoText: String = ""
  )

  case class UserProfileJson(
    nickname: String,
    birthDate: String,
    gender: String,
    picture: Option[String],
    cover: Option[String],
    geo: Geo,
    geoText: String = ""
  )

  case class UserMinimalDetailsResponse(
    id: String,
    nickname: String,
    gender: String,
    picture: String
  ) extends SpokDataResponse

  case class Follow(
    id: String,
    nickname: String,
    gender: String,
    picture: String
  )

  case class UserFollowers(
    previous: String,
    next: String,
    followers: List[Follow]
  ) extends SpokDataResponse

  case class UserFollowings(
    previous: String,
    next: String,
    followings: List[Follow]
  ) extends SpokDataResponse

  case class UserGroupsDetails(
    id: String,
    title: String,
    nickname: List[String],
    nbUsers: Int,
    followers: Int,
    contacts: Int
  ) extends SpokDataResponse

  /**
   * Update phone number
   *
   * @param oldCountryCode old country code
   * @param oldNumber      old phone number
   * @param newCountryCode new country code
   * @param newNumber      new phone number
   */
  case class PhoneNumbers(
    oldCountryCode: String,
    oldNumber: String,
    newCountryCode: String,
    newNumber: String
  )

  case class UserSetting(
    followers: Boolean = true,
    following: Boolean = true
  )

  case class MyDetails(
    id: String,
    nickname: String,
    gender: String,
    picture: String,
    cover: String,
    nbFollowers: Long,
    nbFollowing: Long,
    nbSpoks: Long
  ) extends SpokDataResponse

  case class PopularSpokerDetails(
    id: String,
    nickname: String,
    gender: String,
    picture: String,
    cover: String,
    launched: Long,
    nbFollowers: Long,
    nbFollowing: Long,
    nbSpoks: Long
  ) extends SpokDataResponse

  case class FollowerDetailsForSingleGroup(
    `type`: String,
    id: String,
    nickname: String,
    gender: String,
    picture: String
  ) extends SpecificGroupResponse

  case class ContactDetailsForSingleGroup(
    `type`: String,
    nickname: String,
    phoneNumber: String
  ) extends SpecificGroupResponse

  case class SingleGroupDetails(
    id: String,
    title: String,
    previous: String,
    next: String,
    numberOfUsers: Int,
    numberOfContacts: Int,
    numberOfFollowers: Int,
    users: List[SpecificGroupResponse]
  ) extends SpokDataResponse

  case class LoggedUsersDetails(
    id: String,
    countryCode: String,
    phoneNumber: String,
    birthDate: String,
    nickname: String,
    gender: String,
    geo: Geo,
    geoText: String
  ) extends SpokDataResponse

  case class SpokerFewDetails(
    cover: String,
    last_activity: String,
    last_position: Geo,
    picture: String,
    token: String
  )

  case class Spoker(
    id: String,
    countryCode: String,
    phoneNumber: String,
    birthDate: String,
    nickname: String,
    gender: String,
    geo: Geo,
    geoText: String,
    cover: String,
    last_activity: String,
    last_position: Geo,
    picture: String,
    token: String
  )

  case class Message(message: String) extends SpokDataResponse

  case class GroupId(groupId: String) extends SpokDataResponse

  case class SpokeFullDetails(spoker: Spoker) extends SpokDataResponse

}
