package com.spok.model

import java.util.Date

import com.spok.util.RandomUtil

object Messaging {

  case class User(id: String, nickname: String, gender: String, picture: String)

  case class MessageJsonData(message: String)

  case class MessageDetail(id: String, text: String, file: Option[String] = None)

  case class Message(senderId: String, receiverId: String, messageId: String, text: String,
    time: Long = RandomUtil.timeStamp, file: Option[String] = None)

  case class MessageResponse(sender: User, receiver: User, message: MessageDetail) extends SpokDataResponse

  case class UserMessage(user1Id: String, user2Id: String, messageId: String, message: String,
    launchedtime: Long = RandomUtil.timeStamp, senderId: String, read: Long = 0)

  case class TalkDetails(user1Id: String, user2Id: String, lastMsgTs: Long, lastMsg: String, senderId: String, user2Nickname: String)

  case class TalksResponse(previous: String, next: String, talks: List[Talks])

  case class Talks(user: User, last: LastMessage)

  case class LastMessage(senderId: String, timestamp: Date, text: String)

  case class UserMessageDetail(id: String, text: String, timestamp: Date, from: String)

  case class TalkResponse(me: User, user: User, messages: List[UserMessageDetail])

  case class RemoveMessage(targetUserId: String, messageId: String)

  case class ReadMessageUpdate(userId: String, messageId: String)

  case class SearchMessageResponse(id: String, text: String, launchedTime: Date, userId: String, userNickname: String, userGender: String, userMainPic: String,
    senderId: String)

  case class UserTypingResponse(userId: String, targetUserId: String) extends SpokDataResponse

  case class UserTyping(targetedUserId: String)

  case class SpokerDetails(spokerid: String, nickname: String, gender: String, picture: String)

}
