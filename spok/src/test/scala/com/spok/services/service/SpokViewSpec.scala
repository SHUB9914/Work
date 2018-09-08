package com.spok.services.service

import java.util.{ Date, UUID }
import akka.actor.{ ActorSystem, Props }
import akka.testkit.{ ImplicitSender, TestKit }
import com.rbmhtechnology.eventuate.ReplicationEndpoint
import com.rbmhtechnology.eventuate.ReplicationEndpoint._
import com.rbmhtechnology.eventuate.log.leveldb.LeveldbEventLog
import com.spok.model.Account.UserMinimalDetailsResponse
import com.spok.model.SpokModel._
import com.spok.persistence.dse.DseGraphFactory
import com.spok.persistence.factory.spokgraph.{ DSESpokApi, DSESpokViewApi }
import com.spok.persistence.factory.{ DSEGraphPersistenceFactoryApi, DSEUserSpokFactoryApi }
import com.spok.services.util.TestHelper
import com.spok.util.Constant._
import org.joda.time.DateTime
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ BeforeAndAfterAll, MustMatchers, WordSpecLike }
import com.spok.services.service.SpokViewValidationCommands._
import com.spok.services.service.SpokViewCommands._
import com.spok.services.service.SpokViewValidationReplies._
import com.spok.services.service.SpokViewReplies._

class SpokViewSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
    with WordSpecLike with MustMatchers with BeforeAndAfterAll with TestHelper with MockitoSugar {

  def this() = this(ActorSystem("SpokActorSystem"))

  val endpoint = ReplicationEndpoint(id => LeveldbEventLog.props(logId = "SpokViewSpec"))(_system)
  val eventLog = endpoint.logs(DefaultLogName)
  val mockedDSEUserSpokFactoryApi: DSEUserSpokFactoryApi = mock[DSEUserSpokFactoryApi]
  val mockedDseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
  val mockedDSESpokFactoryApi: DSESpokApi = mock[DSESpokApi]
  val mockedDSESpokViewApi: DSESpokViewApi = mock[DSESpokViewApi]

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
    session.get.close()
  }

  "A SpokView" must {

    "check if the spok to be respoked has already been respoked or not" in {
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new SpokView(id, eventLog) {
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDSEUserSpokFactoryApi.validateSpokAndSendStatus(userId, "12345")) thenReturn (("spok is valid", true, None))
      actorRef ! IsValidSpokAndSendStatus(userId, "12345")
      expectMsgPF() { case IsValidSpokAck(status, isEnabled, None) => status mustBe "spok is valid" }
    }

    "able to validate if spok exists or not with enabled flag" in {
      val id = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new SpokView(id, eventLog) {
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDSEUserSpokFactoryApi.isValidSpokWithEnabledFlag(spokId)) thenReturn true
      actorRef ! IsValidSpokWithEnabledFlag(spokId)
      expectMsgPF() { case IsValidSpokWithEnabledAck(value) => value mustBe true }
    }

    "be validate  spok by id " in {
      val id = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new SpokView(id, eventLog) {
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDSEUserSpokFactoryApi.validateAbsoluteSpokById("0e5c45e0-cfb2-4333-bf30-76a01ea94d64")) thenReturn SPOK_VALID
      actorRef ! IsValidAbsoluteSpok("0e5c45e0-cfb2-4333-bf30-76a01ea94d64")
      expectMsgPF() { case IsValidAbsoluteSpokAck(value) => value mustBe SPOK_VALID }
    }

    "be able to view poll question given the question id" in {
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val questionId = UUID.randomUUID().toString
      val answerId1 = UUID.randomUUID().toString
      val answerId2 = UUID.randomUUID().toString
      val viewPollQuestion = ViewPollQuestion(None, ViewPollQuestionInternalResponse(questionId, "How many planets solar system has ?"),
        None, List(ViewPollAnswerResponse(answerId1, 1, "Nine"), ViewPollAnswerResponse(answerId2, 2, "Eight")))
      val actorRef = system.actorOf(Props(new SpokView(id, eventLog) {
        override val dseSpokFactoryApi = mockedDSESpokFactoryApi
      }))
      when(mockedDSESpokFactoryApi.viewPollQuestion(questionId, userId)) thenReturn ((Some(viewPollQuestion), None))
      actorRef ! ViewPollQuestionDetails(questionId, userId)
      expectMsgPF() {
        case ViewPollQuestionSuccess(response) =>
          response mustEqual viewPollQuestion
      }
    }

    "not be able to view poll question given the question id if any error occurs" in {
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val questionId = UUID.randomUUID().toString
      val answerId1 = UUID.randomUUID().toString
      val answerId2 = UUID.randomUUID().toString
      val viewPollQuestion = ViewPollQuestion(None, ViewPollQuestionInternalResponse(questionId, "How many planets solar system has ?"),
        None, List(ViewPollAnswerResponse(answerId1, 1, "Nine"), ViewPollAnswerResponse(answerId2, 2, "Eight")))
      val actorRef = system.actorOf(Props(new SpokView(id, eventLog) {
        override val dseSpokFactoryApi = mockedDSESpokFactoryApi
      }))
      when(mockedDSESpokFactoryApi.viewPollQuestion(questionId, userId)) thenReturn ((None, Some(Error(SPK_126, s"Question $questionId Not Found"))))
      actorRef ! ViewPollQuestionDetails(questionId, userId)
      expectMsgPF() {
        case ViewPollQuestionFailure(errorId, errorMessage) =>
          errorId mustEqual SPK_126
          errorMessage mustEqual s"Question $questionId Not Found"
      }
    }

    "be able to validate a poll question as to whether it exists or not" in {
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val questionId = UUID.randomUUID().toString
      val spokId = Some(UUID.randomUUID().toString)
      val actorRef = system.actorOf(Props(new SpokView(id, eventLog) {
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDSEUserSpokFactoryApi.validatePollQuestionAndFetchSpokId(questionId)) thenReturn spokId
      actorRef ! IsValidPollQuestion(questionId)
      expectMsgPF() {
        case IsValidPollQuestionAck(response) =>
          response mustEqual spokId
      }
    }

    "be get spok stats " in {
      val id = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new SpokView(id, eventLog) {
        override val dseSpokViewApi = mockedDSESpokViewApi
      }))
      when(mockedDSESpokViewApi.getSpokStats("0e5c45e0-cfb2-4333-bf30-76a01ea94d64")) thenReturn (SpokStatistics(0.0, 1, 0, 0, 1, 0))
      actorRef ! GetSpokStats("0e5c45e0-cfb2-4333-bf30-76a01ea94d64")
      expectMsgPF() {
        case SpokStats(value) =>
          value mustEqual SpokStatistics(0.0, 1, 0, 0, 1, 0)
      }
    }

    "be able to get comments of a spok" in {
      val commentId = getUUID()
      val id = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val timestamp = DateTime.now()
      val actorRef = system.actorOf(Props(new SpokView(id, eventLog) {
        override val dseSpokViewApi = mockedDSESpokViewApi
      }))
      when(mockedDSESpokViewApi.getComments(spokId, "1")) thenReturn Some(CommentsResponse("0", "2", 1,
        List(Comments(commentId, timestamp.toDate, "text", UserMinimalDetailsResponse(userId, "name", "male", "picture")))))
      actorRef ! GetComments(spokId, "1")
      expectMsgPF() {
        case GetCommentsRes(value) =>
          value mustEqual Some(CommentsResponse("0", "2", 1, List(Comments(commentId, timestamp.toDate, "text",
            UserMinimalDetailsResponse(userId, "name", "male", "picture")))))
      }
    }

    "be able to get respokers of a spok" in {
      val id = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new SpokView(id, eventLog) {
        override val dseSpokViewApi = mockedDSESpokViewApi
      }))
      when(mockedDSESpokViewApi.getReSpokers(spokId, "1")) thenReturn Some(ReSpokerResponse("0", "2", List(ReSpoker(userId, "name", "male", "picture"))))
      actorRef ! GetReSpokers(spokId, "1")
      expectMsgPF() {
        case ReSpokersRes(value) =>
          value mustEqual Some(ReSpokerResponse("0", "2", List(ReSpoker(userId, "name", "male", "picture"))))
      }
    }

    "be able to get scoped user of a spok" in {
      val id = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new SpokView(id, eventLog) {
        override val dseSpokViewApi = mockedDSESpokViewApi
      }))
      when(mockedDSESpokViewApi.getScopedUsers(spokId, "1")) thenReturn Some(ScopedUsersResponse("0", "2", List(ScopedUsers(userId, "name", "male", "picture"))))
      actorRef ! GetScopedUsers(spokId, "1")
      expectMsgPF() {
        case ScopedUsersRes(value) =>
          value mustEqual Some(ScopedUsersResponse("0", "2", List(ScopedUsers(userId, "name", "male", "picture"))))
      }
    }

    "be get spoks stack" in {
      val launched = DateTime.now()
      val id = UUID.randomUUID().toString
      val id1 = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new SpokView(id, eventLog) {
        override val dseSpokViewApi = mockedDSESpokViewApi
      }))
      val author = Spoker(id, "ramesh", "male", "picture")
      val from = Spoker(id1, "ram", "male", "picture")
      val counters = Counters(1, 1, 1, 0.0)
      val content = Content(picturePreview = Some("picturePreview"), pictureFull = Some("pictureFull"))
      val spokStackResponse = Some(SpoksStackResponse("1", "2", List(ViewSpok(spokId, "text", 1000, new Date,
        "hi i am kias", Some(new Date), "curText", false, author, from, "Public", counters, content))))
      when(mockedDSESpokViewApi.getSpokStack("1234", "1")) thenReturn spokStackResponse
      actorRef ! GetSpokStack("1234", "1")
      expectMsgPF() {
        case SpoksStack(value) =>
          value mustEqual spokStackResponse
      }
    }

    "be able to view short spok" in {
      val id = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val targetUserId = UUID.randomUUID().toString
      val reSpokerId = UUID.randomUUID().toString
      val fromSpokerId = UUID.randomUUID().toString
      val launched = DateTime.now()
      val respoked = DateTime.now()
      DseGraphFactory.dseConn.executeGraph("graph.addVertex(label,'spok','spokId','" + spokId + "')")
      val spokVertex = DseGraphFactory.dseConn.executeGraph("g.V().hasLabel('spok').has('spokId','" + spokId + "')").one().asVertex();
      val viewSpok = ViewSpok(spokId, "rawtext", 0, launched.toDate, "text", Some(respoked.toDate), "curtext", false, Spoker(reSpokerId, "respoker", "male", "reSpoker.jpg"),
        Spoker(fromSpokerId, "fromSpoker", "male", "fromSpoker.jpg"), "Public", Counters(2, 2, 2, 1000), Content(rawText = Some("rawText")))
      val actorRef = system.actorOf(Props(new SpokView(id, eventLog) {
        override val dseSpokViewApi = mockedDSESpokViewApi
      }))
      when(mockedDSESpokViewApi.viewShortSpok(spokId, targetUserId, userId, spokVertex)) thenReturn Some(viewSpok)
      actorRef ! ViewShortSpok(spokId, targetUserId, userId, Some(spokVertex))
      expectMsgPF() {
        case ViewShortSpokResponse(value) =>
          value mustEqual Some(viewSpok)
      }
    }

    "be able to view spokers wall" in {
      val id = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val targetUserId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val reSpokerId = UUID.randomUUID().toString
      val launched = DateTime.now()
      val respoked = DateTime.now()
      val viewUsersWall = UsersWallResponse("0", "2", List(Spoks(spokId, "rawtext", launched.toDate, "text",
        respoked.toDate, "curtext", false, Spoker(reSpokerId, "respoker", "male", "reSpoker.jpg"),
        Counters(2, 2, 2, 1000), Content(rawText = Some("rawText")))))
      val actorRef = system.actorOf(Props(new SpokView(id, eventLog) {
        override val dseSpokViewApi = mockedDSESpokViewApi
      }))
      when(mockedDSESpokViewApi.getSpokersWallDetails(targetUserId, "1", userId)) thenReturn Some(viewUsersWall)
      actorRef ! ViewSpokersWall(targetUserId, "1", userId)
      expectMsgPF() {
        case ViewSpokersWallSuccess(value) =>
          value mustEqual viewUsersWall
      }
    }

    "not able to view spokers wall" in {
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val targetUserId = UUID.randomUUID().toString
      val viewUsersWall = s"Unable loading user $targetUserId wall (generic error)."
      val actorRef = system.actorOf(Props(new SpokView(id, eventLog) {
        override val dseSpokViewApi = mockedDSESpokViewApi
      }))
      when(mockedDSESpokViewApi.getSpokersWallDetails(targetUserId, "1", userId)) thenReturn None
      actorRef ! ViewSpokersWall(targetUserId, "1", userId)
      expectMsgPF() {
        case viewSpokersWallFailure: ViewSpokersWallFailure =>
          viewSpokersWallFailure.cause.getMessage mustEqual viewUsersWall
      }
    }

    "be able to view full spok" in {
      val id = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val targetUserId = UUID.randomUUID().toString
      val reSpokerId = UUID.randomUUID().toString
      val fromSpokerId = UUID.randomUUID().toString
      val timestamp = DateTime.now()
      val launched = DateTime.now()
      val respoked = DateTime.now()
      val reSpokers = List(ReSpoker(targetUserId, "name", "male", "picture"))
      val scopedUsers = List(ScopedUsers(targetUserId, "name", "male", "picture"))
      val comments: List[CommentsForFullSpok] = List(CommentsForFullSpok("text", timestamp.toDate,
        targetUserId, "name", "picture", "male"))
      val viewFullSpok = ViewFullSpok(spokId, "rawtext", 0, launched.toDate, "text", Some(respoked.toDate), "curtext", false, Spoker(reSpokerId, "respoker", "male", "reSpoker.jpg"),
        Spoker(fromSpokerId, "fromSpoker", "male", "fromSpoker.jpg"), "Public", Counters(2, 2, 2, 1000), reSpokers, scopedUsers, comments,
        Content(rawText = Some("rawText")))
      DseGraphFactory.dseConn.executeGraph("graph.addVertex(label,'spok','spokId','" + spokId + "')")
      val spokVertex = DseGraphFactory.dseConn.executeGraph("g.V().hasLabel('spok').has('spokId','" + spokId + "')").one().asVertex();

      val actorRef = system.actorOf(Props(new SpokView(id, eventLog) {
        override val dseSpokViewApi = mockedDSESpokViewApi
      }))
      when(mockedDSESpokViewApi.viewFullSpok(spokId, targetUserId, userId, spokVertex)) thenReturn Some(viewFullSpok)
      actorRef ! ViewFullSpokDetails(spokId, targetUserId, userId, Some(spokVertex))
      expectMsgPF() {
        case ViewFullSpokResponse(value) =>
          value mustEqual Some(viewFullSpok)
      }
    }

    "be able to view poll spok stats if executed successfully" in {
      val spokId = getUUID()
      val pollId = getUUID()
      val questionId = getUUID()
      val answerId = getUUID()
      val userId = getUUID()
      val id = getUUID()
      val actorRef = system.actorOf(Props(new SpokView(id, eventLog) {
        override val dseSpokViewApi = mockedDSESpokViewApi
      }))
      val pollStats: PollStats = PollStats(pollId, Some("poll test"), Some("testing the poll"), 1, List(
        PollQuestionsStats(questionId, "Who is the father of Computer?", List(PollAnswerStats(answerId, "Charles Babbage", 4)))
      ))
      when(mockedDSESpokViewApi.viewPollStats(spokId, userId)) thenReturn ((Some(pollStats), None))
      actorRef ! ViewPollStats(spokId, userId)
      expectMsgPF() {
        case viewPollStatsSuccess: ViewPollStatsSuccess =>
          viewPollStatsSuccess.pollStats mustEqual pollStats
      }
    }

    "be able to send error if view poll spok stats fails with an error" in {
      val spokId = getUUID()
      val userId = getUUID()
      val id = getUUID()
      val actorRef = system.actorOf(Props(new SpokView(id, eventLog) {
        override val dseSpokViewApi = mockedDSESpokViewApi
      }))
      val error = Error(SPK_014, CANNOT_VIEW_SPOK_STATS)
      when(mockedDSESpokViewApi.viewPollStats(spokId, userId)) thenReturn ((None, Some(error)))
      actorRef ! ViewPollStats(spokId, userId)
      expectMsgPF() {
        case viewPollStatsFailure: ViewPollStatsFailure =>
          viewPollStatsFailure.errorId mustEqual SPK_014
          viewPollStatsFailure.errorMessage mustEqual CANNOT_VIEW_SPOK_STATS
      }
    }

    "be able to chek spoker suspended or not" in {
      val id = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new SpokView(id, eventLog) {
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDSEUserSpokFactoryApi.spokerSuspendedOrNot(id)) thenReturn SPOKER_NOT_SUSPENDED
      actorRef ! IsUserSuspended(id)
      expectMsgPF() { case IsUserSuspendedAsk(value) => value mustBe SPOKER_NOT_SUSPENDED }
    }
  }
}
