package com.spok.apiservice.handler

import com.spok.model.Messaging.{ MessageResponse, UserTypingResponse }
import com.spok.model.NotificationDetail
import com.spok.model.SpokModel.{ StandardResponseForCaseClass, StandardResponseForString }
import com.spok.util.Constant._

trait MessagingServiceHandler extends NotificationServiceHandler {

  def checkMessagingResponseAndTakeAction(json: String, userId: String): Boolean = {
    logger.info(s"${userId}: Got Response and Now taking further action !!!!!" + json)
    val contentOpt = parse(json).extractOpt[StandardResponseForCaseClass]
    for (
      content <- contentOpt;
      errors <- content.errors if errors.isEmpty
    ) {
      content.resource.get match {
        case TYPING => sendTypingMessageNotification(json, userId)
        case _ => sendMessageNotification(json, userId)
      }
    }
    true
  }

  /**
   * Send notification to registered users of my contacts
   *
   * @param txt
   * @return
   */
  private def sendMessageNotification(txt: String, userId: String) = {
    val sendMessageResponseOpt = (parse(txt) \ "data").extractOpt[MessageResponse]
    for (sendMessageResponse <- sendMessageResponseOpt) {
      logger.info("Sending messaging notification ::" + sendMessageResponse)
      val validUserIds = List(sendMessageResponse.sender.id, sendMessageResponse.receiver.id)
      val notificationDetail = NotificationDetail(validUserIds, getUUID(), write(sendMessageResponse), userId, userId)
      sendNotification(notificationDetail, SEND)
    }
  }

  /**
   * Send Notification by typing user
   *
   * @param json
   * @param userId
   * @return
   */

  private def sendTypingMessageNotification(json: String, userId: String): Boolean = {
    logger.info("sending the typing user notification for json " + json)
    val standardResponseOpt = (parse(json) \ "data").extractOpt[UserTypingResponse]
    for (standardResponse <- standardResponseOpt) {
      val notificationId = getUUID()
      val notificationDetail = NotificationDetail(List(standardResponse.targetUserId), notificationId, TYPING, standardResponse.userId, standardResponse.userId)
      sendNotification(notificationDetail, "send")
    }
    true
  }

}
