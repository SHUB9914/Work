package com.spok.apiservice.handler

import com.spok.model.Account.{ AccountResponse, UserRegistrationResponse }
import com.spok.model.NotificationDetail
import com.spok.model.SpokModel.{ StandardResponseForCaseClass, StandardResponseForString }
import com.spok.util.Constant._

import scala.concurrent.ExecutionContext.Implicits.global

trait AccountServiceHandler extends NotificationServiceHandler {

  /**
   * Check Response from microservice
   * and take appropriate action
   *
   * @param json
   * @param userId
   * @param phoneNumber
   * @return
   */

  def checkAccountResponseAndTakeAction(json: String, userId: Option[String], phoneNumber: Option[String]): Boolean = {
    logger.info(s"${phoneNumber}: Got Response and Now taking further action !!!!!" + json)
    /* val contentOpt = parse(json).extractOpt[StandardResponseForCaseClass]
    for (
      content <- contentOpt;
      errors <- content.errors if errors.isEmpty
    ) {
      content.resource.get match {
        case UPDATE_USER_PROFILE => sendProfileUpdateNotification(json, userId)
        case _ => sendUserRegistrationNotification(json)
      }
    }
    true*/

    if (userId.isEmpty && phoneNumber.isEmpty) {
      val contentOpt = parse(json).extractOpt[StandardResponseForCaseClass]
      for (
        content <- contentOpt;
        errors <- content.errors if errors.isEmpty
      ) {
        sendUserRegistrationNotification(json)
      }
      true
    } else {
      checkAccountResponse(json, userId)

    }
  }

  private def checkAccountResponse(json: String, userId: Option[String]): Boolean = {
    val standardResponseOpt = parse(json).extractOpt[StandardResponseForString]
    for (
      standardResponse <- standardResponseOpt;
      accountResponse <- standardResponse.data
    ) {
      val responseOpt = parse(accountResponse).extractOpt[AccountResponse]
      for (response <- responseOpt) {
        handleResponse(userId, response)
      }
    }
    true
  }

  /* private def sendProfileUpdateNotification(json: String, userId: Option[String]): Boolean = {
    if (userId.isDefined) {
      logger.info("user id received after account update ::: ", userId)
      val updateUserProfileNotification = handleUpdateUserProfileNotification(userId.get)
      Thread.sleep(1000)
      updateUserProfileNotification map (notification => sendNotification(notification, "send"))
    }
    true
  }*/

  private def handleResponse(userId: Option[String], response: AccountResponse): Any = {
    response match {
      case AccountResponse(Some(followResponse), _, Some(isFriend), _) => {
        sendNotificationToFollowersAndVisitedUsers(
          handleNotificationOnUnfollowOrFollow(followResponse.followerId, followResponse.followingId, FOLLOWS)
        )
        sendFriendNotification(followResponse.followerId, followResponse.followingId)
      }
      case AccountResponse(Some(followResponse), _, _, _) =>
        sendNotificationToFollowersAndVisitedUsers(
          handleNotificationOnUnfollowOrFollow(followResponse.followerId, followResponse.followingId, FOLLOWS)
        )
      case AccountResponse(_, Some(unfollowResponse), _, _) =>
        sendNotificationToFollowersAndVisitedUsers(
          handleNotificationOnUnfollowOrFollow(unfollowResponse.followerId, unfollowResponse.followingId, UNFOLLOWS)
        )
      case AccountResponse(_, _, _, Some(updateUserProfileResponse)) => {
        if (userId.isDefined) {
          logger.info("user id received after account update ::: ", userId)
          val updateUserProfileNotification = handleUpdateUserProfileNotification(userId.get)
          updateUserProfileNotification map (notification => sendNotification(notification, "send"))
        }
      }
      case _ => // Do Nothing
    }
  }

  private def sendFriendNotification(followingId: String, followerId: String): Any = {
    val notificationId = getUUID()
    val notificationDetail = NotificationDetail(List(followingId, followerId), notificationId, FRIEND, followingId, followerId)
    sendNotification(notificationDetail, "send")
  }

  /**
   * Send notification to registered users of my contacts
   *
   * @param txt
   * @return
   */
  private def sendUserRegistrationNotification(txt: String): Any = {
    val userRegistrationResponseOpt = (parse(txt) \ "data").extractOpt[UserRegistrationResponse]
    for (userRegistrationResponse <- userRegistrationResponseOpt) {
      logger.info("Sending User Registration notification::" + userRegistrationResponse)
      if (userRegistrationResponse.userContactsIds.nonEmpty) {
        val (userId, _) = getInfoFromToken(userRegistrationResponse.token)
        val notificationDetail = NotificationDetail(userRegistrationResponse.userContactsIds, getUUID(), REGISTERED, userId, userId)
        sendNotification(notificationDetail, ADD)
      }
    }
  }
}
