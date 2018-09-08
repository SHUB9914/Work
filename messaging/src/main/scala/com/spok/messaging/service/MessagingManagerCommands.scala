package com.spok.messaging.service

import com.spok.model.Messaging.Message

object MessagingManagerCommands {

  case class SendMessage(userId: String, message: Message)

  case class TalkRemove(targetUserId: String, userId: String)

  case class RemoveSingleMessage(targetUserId: String, messageId: String, userId: String)

  case class ReadMessageUpdateFlag(userId: String, targetUserId: String, messageId: String)

}
