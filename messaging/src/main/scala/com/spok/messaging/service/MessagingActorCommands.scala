package com.spok.messaging.service

import com.spok.model.Messaging.{ Message, MessageResponse }
import com.spok.model.SpokDataResponse

sealed trait MessagingAck

object MessagingActorCommands {

  case class CreateMessage(message: Message)

  case class RemoveTalk(targetUserId: String, userId: String)

  case class SingleMessageRemove(targetUserId: String, messageId: String, userId: String)

  case class UpdateReadMessageFlag(userId: String, targetUserId: String, messageId: String)
}

object MessagingActorSuccessReplies {

  case class CreateMessageSuccess(messageResponse: MessageResponse) extends MessagingAck

  case class RemoveTalkSuccess(removeResponse: String) extends SpokDataResponse

  case class RemoveSingleMessageSuccess(removeResponse: String) extends SpokDataResponse

  case class UpdateReadMessageFlagSuccess(updateReadMessageFlagResponse: String) extends SpokDataResponse

  case class UserTypingResponseSuccess(userTypingRes: String)

}

object MessagingActorFailureReplies {

  case class CreateMessageFailure(cause: Throwable, errorCode: String) extends MessagingAck

  case class RemoveTalkFailure(errorId: String, errorMessage: String)

  case class RemoveSingleMessageFailure(errorId: String, errorMessage: String)

  case class UpdateReadMessageFlagFailure(errorId: String, errorMessage: String)

  case class UserTypingResonseFailure(errorId: String, errorMessage: String)

}
