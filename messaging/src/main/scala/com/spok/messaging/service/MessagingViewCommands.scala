package com.spok.messaging.service

import com.spok.model.Messaging.{ SearchMessageResponse, TalkResponse, TalksResponse, User }
import com.spok.model.SpokDataResponse

object MessagingViewCommands {

  case class ViewTalks(userId: String, pos: String)

  case class ViewSingleTalk(targetUserId: String, userId: String, messageId: Option[String], order: String)

  case class SearchMessage(userId: String, message: String)

  case class SearchTalker(talker: String)

  case class IsSpokerSuspended(targerId: String)

}

object MessagingViewSuccessReplies {

  case class ViewTalksSuccess(response: TalksResponse) extends SpokDataResponse

  case class ViewSingleTalkSuccess(response: TalkResponse) extends SpokDataResponse

  case class SearchMessageSuccess(response: List[SearchMessageResponse]) extends SpokDataResponse

  case class SearchTalkerSuccess(response: List[User]) extends SpokDataResponse
}

object MessagingViewFailureReplies {

  case class ViewTalksFailure(errorId: String, errorMessage: String)

  case class ViewSingleTalkFailure(errorId: String, errorMessage: String)

  case class SearchMessageFailure(errorId: String, errorMessage: String)

  case class SearchTalkerFailure(errorId: String, errorMessage: String)

}

