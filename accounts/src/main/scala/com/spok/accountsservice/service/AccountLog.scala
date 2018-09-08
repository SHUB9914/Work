package com.spok.accountsservice.service

import java.util.Date

import com.datastax.driver.core.ResultSet
import com.spok.model.Account.{ FollowUnfollow, User, UserMinimalDetailsResponse, UserProfile }
import com.spok.model.SpokModel.SpokerHistory
import com.spok.persistence.dse.DseConnectionUri._
import com.spok.persistence.factory.spoklog.SpokLogging
import com.spok.util.Constant._
import com.spok.util.RandomUtil

case class UpdateUserProfileLog(nickname: String, birthdate: Date, gender: String, picture: Option[String], cover: Option[String])
case class FollowingId(following: String)
case class SpokerDetails(spokerId: String, nickname: String, gender: String, picture: String)

/**
 * A logging trait being used for all account related activities
 */

trait AccountLog extends SpokLogging with RandomUtil {

  /**
   * Method to log the data in cassandra when a user profile is updated
   * @param userId of the user to be logged
   * @param userProfile contains the data to be logged
   * @return
   */
  def logUserProfileUpdate(userId: String, userProfile: UserProfile): ResultSet = {

    val data = UpdateUserProfileLog(userProfile.nickname, userProfile.birthDate, userProfile.gender, userProfile.picture, userProfile.cover)
    val logJson = SpokerHistory(userId, timeStamp, write(data), userProfile.geo.latitude,
      userProfile.geo.longitude, userProfile.geo.elevation, PROFILE_UPDATED)
    insertHistory(write(logJson), spoker)
  }

  def logUserFollowUnfollow(followUnfollow: FollowUnfollow, action: String): ResultSet = {

    val followingIdJson = write(FollowingId(followUnfollow.followingId))
    val logJson = SpokerHistory(followUnfollow.followerId, timeStamp,
      followingIdJson, 0.0, 0.0, 0.0, action)
    insertHistory(write(logJson), spoker)
  }

  /**
   * This will store user details in database
   *
   * @param userId
   * @param nickname
   * @param gender
   * @param picture
   * @return
   */
  def logSpokerDetails(userId: String, nickname: String, gender: String, picture: String): ResultSet = {
    val logJson = SpokerDetails(userId, nickname, gender, picture)
    insertMesssagingDetails(write(logJson), spokerDetails)
  }

}
