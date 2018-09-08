package com.spok.persistence.script

import java.util.Date
import java.util.concurrent.atomic._

import com.spok.model.Account.User
import com.spok.model.SpokModel.{ Geo, Spok }
import com.spok.model.{ InnerLocation, NorthEast, SouthWest, ViewPort, _ }
import com.spok.persistence.factory.DSEGraphPersistenceFactoryApi
import com.spok.persistence.factory.spokgraph.{ DSESpokApi, SpokCommentApi }
import com.spok.util.Constant._
import com.spok.util.{ LoggerUtil, RandomUtil }
import com.typesafe.config.ConfigFactory
import org.joda.time.{ DateTime, Seconds }

import scala.collection.mutable.ListBuffer
import scala.collection.parallel.immutable.ParSeq
import scala.util.Random

object GenerateData extends App with LoggerUtil with RandomUtil {

  val dseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = DSEGraphPersistenceFactoryApi
  val dseSpokFactoryApi: DSESpokApi = DSESpokApi
  val dseSpokCommentApi: SpokCommentApi = SpokCommentApi
  val config = ConfigFactory.load("references.conf")
  val userIds: ListBuffer[String] = ListBuffer()
  val spokIds: ListBuffer[String] = ListBuffer()
  val random = new Random
  val RANDOM_CEILING = 99999

  if (args(0) == "loadData") {
    generateUserData
    generateFollowerAndFollowingData
    generateSpokData
    generateCommentData
    // now kill jvm
    Runtime.getRuntime.halt(0);
  } else {
    info("No action is performed.")
  }

  private def generateUserData() = {
    val userCount = config.getLong("load.user.count")
    val startTime = new DateTime()
    info("Started inserting user data::" + startTime)
    createUser(userCount, startTime)
    info("Total time to generate user profiles::" + Seconds.secondsBetween(startTime, new DateTime()).getSeconds)
  }

  private def generateFollowerAndFollowingData() = {
    val followingCount = config.getInt("load.following.count")
    val followerCount = config.getInt("load.follower.count")
    val startTime = new DateTime()
    info("Started inserting creating follower and following ::" + startTime)
    createFollowersAndFollowings(followerCount, followingCount)
    info("Total time to generate follower and following  ::" + Seconds.secondsBetween(startTime, new DateTime()).getSeconds)
  }

  private def generateSpokData() = {
    val spokCount = config.getLong("load.spok.count")
    val startTime = new DateTime()
    info("Started inserting spok data :: " + startTime)
    createSpok(spokCount)
    info("Total time to generate spok :: " + Seconds.secondsBetween(startTime, new DateTime()).getSeconds)
  }

  private def generateCommentData() = {
    val commentCount = config.getInt("load.comment.count")
    val startTime = new DateTime()
    info("Started inserting comment data :: " + startTime)
    createComment(commentCount)
    info("Total time to generate comment data :: " + Seconds.secondsBetween(startTime, new DateTime()).getSeconds)
  }

  private def createUser(userCount: Long, startTime: DateTime) = {
    val userProfileCount: ParSeq[Long] = (1L to userCount).toList.par
    val countUsers = new AtomicLong(0L)
    userProfileCount map { count =>
      try {
        // create user A
        val (contact1, contact2, userId) = insertUser()
        // create user B and follow A
        val (_, _, followerId1) = insertUser(Some(contact1))
        dseGraphPersistenceFactoryApi.performFollowOrUnfollow(contact1, followerId1, userId)
        dseGraphPersistenceFactoryApi.updateDefaultGroup(followerId1, userId, FOLLOWS)
        // create user C and follow A
        val (_, _, followerId2) = insertUser(Some(contact2))
        dseGraphPersistenceFactoryApi.performFollowOrUnfollow(contact2, followerId2, userId)
        dseGraphPersistenceFactoryApi.updateDefaultGroup(followerId2, userId, FOLLOWS)
        userIds += (userId)
        info("Number of users has been inserted in batch :::::::::::::: " + countUsers.incrementAndGet() * 3)
      } catch {
        case ex: Exception => {
          error("Failed to create user. Count is " + count + " Reason:", ex)
        }
      }
    }
  }

  private def insertUser(mobileNumber: Option[String] = None): (String, String, String) = {
    val userId = getUUID()
    val date: Date = new java.util.Date()
    val nickname = "name_" + random.nextInt(RANDOM_CEILING).toString
    val gender = List("male", "female").apply(random.nextInt(2))
    val picture = "picture" + random.nextInt(RANDOM_CEILING).toString + ".jpg"
    val cover = "cover" + random.nextInt(RANDOM_CEILING).toString + ".jpg"
    val mobileNo = mobileNumber.getOrElse("+91" + (random.nextDouble() * 10000000000L).toLong.toString)
    val contact1 = "+91" + (random.nextDouble() * 10000000000L).toLong.toString
    val contact2 = "+91" + (random.nextDouble() * 10000000000L).toLong.toString
    val contact3 = "+91" + (random.nextDouble() * 10000000000L).toLong.toString
    val contact4 = "+91" + (random.nextDouble() * 10000000000L).toLong.toString

    val user = User(nickname, date, Location(List(LocationDetails(
      List(AddressComponents("Marsiele", "Marsiele", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(43.2805546, 5.2647101), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      gender, List(contact1, contact2, contact3, contact4), mobileNo, userId, Some("testuser.jpg"))

    dseGraphPersistenceFactoryApi.insertUser(user)
    dseGraphPersistenceFactoryApi.createUserSetting(userId)
    (contact1, contact2, userId)
  }

  private def createFollowersAndFollowings(followerCount: Int, followingCount: Int) = {
    val listUserId = userIds.toList
    val followingList = listUserId.take(followingCount).par
    val followerList = listUserId.slice(followingCount + 1, followerCount + followingCount + 1).par
    followingList.map { followingId =>
      followerList.map { followerId =>
        //perform follower action
        dseGraphPersistenceFactoryApi.performFollowOrUnfollow("", followerId, followingId)
        dseGraphPersistenceFactoryApi.updateDefaultGroup(followerId, followingId, FOLLOWS)
        dseGraphPersistenceFactoryApi.updateUserCountInGroup(ZERO, followingId)

        //perform following action
        dseGraphPersistenceFactoryApi.performFollowOrUnfollow("", followingId, followerId)
        dseGraphPersistenceFactoryApi.updateDefaultGroup(followingId, followerId, FOLLOWS)
        dseGraphPersistenceFactoryApi.updateUserCountInGroup(ZERO, followerId)
      }
    }
  }

  private def createSpok(count: Long): Unit = {
    val listUserId = userIds.toList
    val spokCount: ParSeq[Long] = (1L to count).toList.par
    val countSpoks = new AtomicLong(0L)
    val Sixty = 60
    spokCount map { count =>
      val userId = listUserId.apply(random.nextInt(listUserId.length))
      val geo = random.nextInt(Sixty).toDouble
      val spokId = getUUID()
      val instanceText = "instanceText_" + spokId
      val text = "text_" + spokId
      val spok: Spok = Spok("rawtext", Some("0"), Some("public"), Some(0), Some(instanceText), None, Some(text), None, None, None, Geo(geo, geo, geo), spokId)
      dseSpokFactoryApi.createSpok(userId, spok)
      dseSpokFactoryApi.linkSpokerFollowers(userId, spok.groupId, spok.spokId, spok.geo, spok.visibility, spok.headerText, spok.launched, 0)
      dseSpokFactoryApi.updateStats(spok.spokId, userId, spok.groupId, spok.geo)
      spokIds += spok.spokId
      info("Number of Spok has been inserted  :::::::::::::: " + countSpoks.incrementAndGet())
    }
  }

  private def createComment(count: Int) = {
    val spokIdList = spokIds.toList.take(50).par
    val commentCount: ParSeq[Long] = (1L to count).toList.par
    val listUserId = userIds.toList
    spokIdList.map { spokId =>
      commentCount.map { count =>
        val commentId = getUUID()
        val commenterId = listUserId.apply(random.nextInt(listUserId.length))
        val text = "comment_" + commentId
        val geo = random.nextInt(60).toDouble
        dseSpokCommentApi.addComment(spokId, commentId, commenterId, text, Geo(geo, geo, geo), Nil)
        dseSpokFactoryApi.updateStatsAfterAddComment(spokId)
      }
    }
  }

}
