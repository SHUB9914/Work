package com.spok.model

import java.util.Date

import com.spok.model.Account.{ MyDetails, UserGroupsDetails, UserMinimalDetailsResponse }
import com.spok.util.Constant._
import com.spok.util.RandomUtil
import org.joda.time.DateTime

trait SpokDataResponse

object SpokModel extends RandomUtil {

  case class StandardResponseForString(
    resource: Option[String],
    status: String,
    errors: Option[List[Error]],
    data: Option[String]
  )

  case class EmptyData(data: Option[String])

  case class StandardResponseForStringError(
    resource: Option[String],
    status: String,
    errors: Option[List[Error]],
    data: Option[EmptyData]
  )

  case class StandardResponseForCaseClass(
    resource: Option[String],
    status: String,
    errors: Option[List[Error]],
    data: Option[SpokDataResponse]
  )

  case class StandardResponseForListCaseClass(
    resource: Option[String],
    status: String,
    errors: Option[List[Error]],
    data: Option[List[SpokDataResponse]]
  )

  case class Error(
    id: String,
    message: String,
    fields: Option[String] = None
  )

  case class Response(
    spokResponse: Option[SpokResponse] = None,
    respokResponse: Option[RespokResponse] = None,
    unspokResponse: Option[UnspokResponse] = None,
    addCommentResponse: Option[SpokCommentResponse] = None,
    removeCommentResponse: Option[RemoveCommentResponse] = None,
    updateCommentResponse: Option[CommentUpdateResponse] = None,
    removeSpokResponse: Option[RemoveSpokResponse] = None
  ) extends SpokDataResponse

  case class InterimRespok(
    groupId: Option[String],
    visibility: Option[String],
    text: Option[String],
    geo: Geo,
    mention: Option[List[String]]
  )

  case class Geo(
    latitude: Double,
    longitude: Double,
    elevation: Double
  )

  case class Url(
    address: String,
    title: String,
    text: String,
    preview: String,
    urlType: Option[String]
  )

  case class UserPollAnswer(
    answerId: String,
    geo: Geo
  )

  case class PollAnswers(
    text: String,
    contentType: Option[String],
    preview: Option[String],
    rank: Int
  )

  case class PollQuestions(
    text: String,
    contentType: Option[String],
    preview: Option[String],
    rank: Int,
    answers: List[PollAnswers]
  )

  case class RiddleQuestion(
    text: String,
    `type`: Option[String],
    preview: Option[String]
  )

  case class RiddleAnswer(
    text: String,
    `type`: Option[String],
    preview: Option[String]
  )

  case class Poll(
    title: String,
    desc: Option[String],
    questions: List[PollQuestions]
  )

  case class Riddle(
    title: String,
    question: RiddleQuestion,
    answer: RiddleAnswer
  )

  case class Spok(
    contentType: String,
    groupId: Option[String],
    visibility: Option[String],
    ttl: Option[Int],
    headerText: Option[String],
    file: Option[String],
    text: Option[String],
    url: Option[Url],
    poll: Option[Poll],
    riddle: Option[Riddle],
    geo: Geo,
    spokId: String = getUUID,
    launched: Long = timeStamp
  )

  case class SpokAttributes(
    contentType: String,
    groupId: Option[String],
    visibility: Option[String],
    ttl: Option[Int],
    instanceText: Option[String],
    text: Option[String],
    spokId: String
  )

  case class SpokInstance(
    absoluteSpokId: String,
    instanceId: String,
    status: String,
    visibility: String,
    text: Option[String],
    groupId: Option[String],
    launched: Date = DateTime.now().toDate
  )

  case class SpokStatistics(
    totalTravelledDis: Double,
    numberOfUser: Int,
    numberOfRespoked: Int,
    numberOfUnspoked: Int,
    numberOfPending: Int,
    numberOfComment: Int
  ) extends SpokDataResponse

  case class Respok(
    groupId: Option[String],
    visibility: Option[String],
    text: Option[String],
    geo: Geo,
    mention: Option[List[String]],
    launched: Long = timeStamp
  )

  case class RespokStatistics(
    numberOfRespoked: Long,
    numberOfLanded: Long,
    numberOfComment: Long,
    travelled: Double
  )

  case class RespokInterimResponse(
    spokId: String,
    counters: RespokStatistics,
    mentionUserId: List[String],
    followers: List[(String, Double, Double, Double, Long)]
  ) extends SpokDataResponse

  case class RespokResponse(
    spokId: String,
    counters: RespokStatistics,
    mentionUserId: List[String]
  ) extends SpokDataResponse

  case class UnspokResponse(
    spokId: String,
    counters: RespokStatistics
  ) extends SpokDataResponse

  case class Unspok(geo: Geo, launched: Long = timeStamp)

  case class InterimUnspok(geo: Geo)

  case class InterimRemoveComment(commentId: String, geo: Geo)

  case class InterimDisableSpok(spokId: String, geo: Geo)

  case class InterimRemoveSpok(spokId: String, geo: Geo)

  case class InterimSubscribeUnsubscribe(spokId: String, geo: Geo)

  case class SpokResponse(
    spokId: String,
    mentionUserId: List[String]
  ) extends SpokDataResponse

  case class Comment(
    commentId: String,
    text: String,
    geo: Geo,
    mentionUserId: List[String]
  )

  case class SpoksStackResponse(previous: String, next: String, spoks: List[ViewSpok]) extends SpokDataResponse

  case class SpoksResponse(previous: String, next: String, spoks: List[LastSpoks]) extends SpokDataResponse

  case class GroupsResponse(previous: String, next: String, groups: List[UserGroupsDetails]) extends SpokDataResponse

  case class CommentInternalSpokResponse(spokId: String, nbRespoked: String, nbLanded: String, nbComments: String, travelled: String)

  case class CommentUpdateResponse(spokId: String, nbRespoked: String, nbLanded: String, nbComments: String,
    travelled: String, mentionUserId: Option[List[String]] = None, commentId: Option[String] = None)

  case class CommenterUserResponse(id: String, nickName: String, gender: String, picture: String)

  case class SpokCommentResponse(spok: CommentInternalSpokResponse, user: CommenterUserResponse,
    mentionUserId: Option[List[String]], commentId: Option[String] = None)

  case class RemoveCommentResponse(commentId: String, spok: CommentInternalSpokResponse)

  case class RemoveSpokResponse(spokId: Option[String] = None, message: Option[String] = None)

  /**
   * Scoped User
   *
   * @param id      scoped user's id
   * @param name    scoped user's name
   * @param gender  scoped user's gender
   * @param picture scoped user's picture
   */
  case class ScopedUsers(
    id: String,
    name: String,
    gender: String,
    picture: String
  )

  /**
   * Scoped Users with pagination
   *
   * @param previous    previous page index
   * @param next        next page index
   * @param scopedUsers List of scoped users
   */
  case class ScopedUsersResponse(
    previous: String,
    next: String,
    scopedUsers: List[ScopedUsers]
  ) extends SpokDataResponse

  /**
   * ReSpoker User
   *
   * @param id      ReSpoker user's id
   * @param name    ReSpoker user's name
   * @param gender  ReSpoker user's gender
   * @param picture ReSpoker user's picture
   */
  case class ReSpoker(
    id: String,
    name: String,
    gender: String,
    picture: String
  )

  /**
   * ReSpoker Users with pagination
   *
   * @param previous previous page index
   * @param next     next page index
   * @param reSpoker List of ReSpoker users
   */
  case class ReSpokerResponse(
    previous: String,
    next: String,
    reSpoker: List[ReSpoker]
  ) extends SpokDataResponse

  case class Spoker(
    id: String,
    name: String,
    gender: String,
    picture: String
  )

  case class Counters(
    nbSpoked: Long,
    nbScoped: Long,
    nbComments: Long,
    distance: Double
  )

  case class Answers(
    id: String,
    text: String
  )

  case class Questions(
    id: String,
    question: String,
    answers: List[Answers]
  )

  case class Content(
    picturePreview: Option[String] = None,
    pictureFull: Option[String] = None,
    animatedGif: Option[String] = None,
    videoPreview: Option[String] = None,
    video: Option[String] = None,
    soundPreview: Option[String] = None,
    sound: Option[String] = None,
    url: Option[String] = None,
    urlPreview: Option[String] = None,
    urlType: Option[String] = None,
    urlTitle: Option[String] = None,
    urlText: Option[String] = None,
    rawText: Option[String] = None,
    htmlText: Option[String] = None,
    pollTitle: Option[String] = None,
    pollDescription: Option[String] = None,
    pollQuestions: Option[List[Questions]] = None,
    riddleTitle: Option[String] = None,
    riddleQuestion: Option[String] = None,
    riddleAnswer: Option[String] = None
  )

  case class ViewSpok(
    id: String,
    spokType: String,
    ttl: Int,
    launched: Date,
    text: String,
    respoked: Option[Date],
    curtext: String,
    subscriptionStatus: Boolean,
    author: Spoker,
    respoker: Spoker,
    visibility: String,
    counters: Counters,
    content: Content,
    flag: Option[String] = None
  ) extends SpokDataResponse

  case class LastSpoks(
    id: String,
    spokType: String,
    ttl: Int,
    launched: Date,
    text: String,
    author: Spoker,
    visibility: String,
    counters: Counters,
    content: Content,
    subscriptionStatus: Boolean = false,
    flag: Option[String] = None
  ) extends SpokDataResponse

  case class ViewFullSpok(
    id: String,
    spokType: String,
    ttl: Int,
    launched: Date,
    text: String,
    respoked: Option[Date],
    curtext: String,
    subscriptionStatus: Boolean,
    author: Spoker,
    respoker: Spoker,
    visibility: String,
    counters: Counters,
    reSpokers: List[ReSpoker],
    scopedUsers: List[ScopedUsers],
    comments: List[CommentsForFullSpok],
    content: Content,
    flag: Option[String] = None
  ) extends SpokDataResponse

  case class ViewPollQuestionInternalResponse(
    id: String,
    text: String
  )

  case class ViewPollAnswerResponse(
    id: String,
    rank: Int,
    text: String
  )

  case class ViewPollQuestion(
    previous: Option[ViewPollQuestionInternalResponse],
    current: ViewPollQuestionInternalResponse,
    next: Option[ViewPollQuestionInternalResponse],
    answers: List[ViewPollAnswerResponse]
  ) extends SpokDataResponse

  case class ReSpokerDetails(
    id: String,
    name: String,
    gender: String,
    picture: String,
    respoked: String,
    curtext: String,
    visibility: String
  )

  case class SpokDetails(
    spokType: String,
    ttl: Int,
    launched: String,
    text: String
  )

  case class UrlDetails(
    address: String,
    title: String,
    text: String,
    preview: String,
    urlType: String
  )

  /**
   *
   * @param text      Comment's text
   * @param date Comment's timestamp
   * @param id        Commentator's identifier
   * @param user      Commentator's details
   */
  case class Comments(
    id: String,
    date: Date,
    text: String,
    user: UserMinimalDetailsResponse
  )

  case class CommentsForFullSpok(
    text: String,
    timestamp: Date,
    id: String,
    name: String,
    picture: String,
    gender: String
  )

  /**
   *
   * @param previous previous page index
   * @param next     next page index
   * @param publicationComments List of Comments of a spok
   */
  case class CommentsResponse(
    previous: String,
    next: String,
    nbComments: Int,
    publicationComments: List[Comments]
  ) extends SpokDataResponse

  case class Spoks(
    id: String,
    spokType: String,
    launched: Date,
    text: String,
    respoked: Date,
    curtext: String,
    subscriptionStatus: Boolean,
    from: Spoker,
    counters: Counters,
    content: Content,
    flag: Option[String] = None
  )

  case class UsersWallResponse(
    previous: String,
    next: String,
    spoks: List[Spoks]
  ) extends SpokDataResponse

  case class SpokHistory(
    spokId: String,
    loggedtime: Long,
    data: String,
    geoLatitude: Double,
    geoLongitude: Double,
    geoElevation: Double,
    spokerId: String,
    eventname: String
  )

  case class RemoveCommentSpokHistory(
    spokId: String,
    loggedtime: Long,
    data: String,
    geoLatitude: Double,
    geoLongitude: Double,
    geoElevation: Double,
    eventname: String
  )

  case class SpokerHistory(
    spokerId: String,
    loggedtime: Long,
    data: String,
    geoLatitude: Double,
    geoLongitude: Double,
    geoElevation: Double,
    eventname: String
  )

  case class ArchiveData(
    spokerId: String,
    loggedtime: Long,
    data: String,
    mobileNo: String
  )

  case class RemoveCommentSpokerHistory(
    spokerId: String,
    loggedtime: Long,
    data: String,
    geoLatitude: Double,
    geoLongitude: Double,
    geoElevation: Double,
    eventname: String
  )

  case class SpokVertex(
    spokId: String,
    ttl: Int,
    contentType: String,
    enabled: Boolean,
    headerText: String,
    launched: Long,
    geo: Geo,
    author: String,
    orginalVisibility: String = PUBLIC,
    actualQuestions: Int = 0
  )

  case class SpokEdge(
    status: String,
    launched: Long,
    geo: Geo,
    from: String,
    groupId: String,
    visibility: String,
    headerText: String,
    pendingQuestions: Int
  )

  case class Statistics(
    numberOfPending: Long,
    numberOfUnspoked: Long,
    numberOfRespoked: Long,
    numberOfLanded: Long,
    numberOfComment: Long,
    travelled: Double
  )

  case class Nickname(nickname: String, id: String) extends SpokDataResponse

  case class NicknameResponse(nickname: String, userId: String)

  case class HashTag(hashtags: List[String]) extends SpokDataResponse

  case class PopularSpokerRes(
    previous: String,
    next: String,
    spokers: List[MyDetails]
  ) extends SpokDataResponse

  case class PollAnswerStats(
    id: String,
    text: String,
    nb: Int
  )

  case class PollQuestionsStats(
    id: String,
    text: String,
    answers: List[PollAnswerStats]
  )

  case class PollStats(
    id: String,
    text: Option[String],
    description: Option[String],
    nb: Int,
    questions: List[PollQuestionsStats]
  ) extends SpokDataResponse

  case class AnswersInterim(
    id: String,
    text: String,
    rank: String
  )

  case class QuestionsInterim(
    id: String,
    question: String,
    answers: List[Answers],
    rank: String
  )

  case class SubscriberDetails(spokId: String, userIds: List[String])

  case class PollAnswersStatsInterim(answerId: String, answerText: String, nbAnswered: Int, rank: Int)

  case class PollQuestionsStatsInterim(
    id: String,
    text: String,
    answers: List[PollAnswerStats],
    rank: Int
  )

  case class OneAnswer(
    questionId: String,
    answerId: String
  )

  case class AllAnswers(
    spokId: String,
    oneAnswer: List[OneAnswer],
    geo: Geo
  )

  case class CompletePollResponse(
    response: String
  ) extends SpokDataResponse

  case class Message(message: String) extends SpokDataResponse

}

