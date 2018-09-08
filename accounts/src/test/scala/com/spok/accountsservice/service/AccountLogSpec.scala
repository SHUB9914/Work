package com.spok.accountsservice.service

import java.util.Date

import com.spok.model.Account.{ FollowUnfollow, UserProfile }
import com.spok.model.SpokModel.{ Geo, SpokHistory, SpokerHistory }
import com.spok.persistence.cassandra.CassandraProvider._
import com.spok.persistence.dse.DseConnectionUri._
import com.spok.util.Constant._
import org.scalatest.{ FlatSpec, Matchers }

class AccountLogSpec extends FlatSpec with Matchers with AccountLog {

  behavior of "AccountLogSpec "

  it should "be able to insert spoker history details in cassandra when user profile is updated" in {

    val spokerId = getUUID().toString
    val date = new Date
    val userProfileDetails = UserProfile("Sheldon", date, "male", Some("picture"), Some("cover"), Geo(45.00, 45.00, 45.00))
    val data = write(UpdateUserProfileLog("Sheldon", date, "male", Some("picture"), Some("cover")))
    logUserProfileUpdate(spokerId, userProfileDetails)
    val res = cassandraConn.execute(s"SELECT * from $spoker where spokerid = '$spokerId' ")
    val row = res.one()
    assert(row.getString("spokerid") == spokerId)
    assert(row.getString("data") == data)
  }

  it should "be able to insert spoker history details in cassandra when a user follows another user" in {

    val spokerId = getUUID()
    val followingId = getUUID()
    val followUnfollow = FollowUnfollow("919583477591", spokerId, followingId)
    logUserFollowUnfollow(followUnfollow, FOLLOWS)
    val res = cassandraConn.execute(s"SELECT * from $spoker where spokerid = '$spokerId' ")
    val row = res.one()
    val followingIdJson = write(FollowingId(followUnfollow.followingId))
    assert(row.getString("spokerid") == spokerId)
    assert(row.getString("data") == followingIdJson)
    assert(row.getString("eventname") == FOLLOWS)
  }

  it should "be able to insert spoker history details in cassandra when a user unfollows another user" in {

    val spokerId = getUUID()
    val followingId = getUUID()
    val followUnfollow = FollowUnfollow("919583477591", spokerId, followingId)
    logUserFollowUnfollow(followUnfollow, UNFOLLOWS)
    val followingIdJson = write(FollowingId(followUnfollow.followingId))
    val res = cassandraConn.execute(s"SELECT * from $spoker where spokerid = '$spokerId' ")
    val row = res.one()
    assert(row.getString("spokerid") == spokerId)
    assert(row.getString("data") == followingIdJson)
    assert(row.getString("eventname") == UNFOLLOWS)
  }

}
