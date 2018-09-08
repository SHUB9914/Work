package com.spok.services.service

import java.util.UUID
import akka.actor.{ ActorSystem, Props }
import akka.testkit.{ ImplicitSender, TestKit }
import com.rbmhtechnology.eventuate.ReplicationEndpoint
import com.rbmhtechnology.eventuate.ReplicationEndpoint.DefaultLogName
import com.rbmhtechnology.eventuate.log.leveldb.LeveldbEventLog
import com.spok.model.SpokModel._
import com.spok.persistence.factory.spokgraph.{ DSESpokApi, SpokCommentApi }
import com.spok.persistence.factory.{ DSEGraphPersistenceFactoryApi, DSEUserSpokFactoryApi }
import com.spok.services.util.TestHelper
import com.spok.util.Constant._
import com.spok.util.JsonHelper
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ BeforeAndAfterAll, MustMatchers, WordSpecLike }
import com.spok.services.service.SpokActorCommands._
import com.spok.services.service.SpokActorSuccessReplies._
import com.spok.services.service.SpokActorFailureReplies._

import scala.concurrent.duration._

class SpokActorSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
    with WordSpecLike with MustMatchers with BeforeAndAfterAll with TestHelper with MockitoSugar with JsonHelper {

  def this() = this(ActorSystem("SpokActorSystem"))

  val endpoint = ReplicationEndpoint(id => LeveldbEventLog.props(logId = "SpokActorSpec"))(_system)
  val eventLog = endpoint.logs(DefaultLogName)

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
    session.get.close()
  }

  "A SpokActor" must {

    "Persists a spok when asked to Create a valid text spok" in {

      val id = UUID.randomUUID().toString
      val url = Url("address", "url_title", "url_text", "url_preview", Some("url_type"))
      val geo = Geo(132233.67, 123244.56, 3133113.3)
      val spok = Spok("content_type", Some("0"), Some("public"), Some(0), Some("instance_text"), None, Some("text"), Some(url), None,
        None, geo, "0e5c45e0-cfb2-4333-bf30-76a01ea94d64")
      val mockedDSESpokFactoryApi: DSESpokApi = mock[DSESpokApi]
      val mockedDseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
      val actorRef = system.actorOf(Props(new SpokActor(id, Some(id), eventLog) {
        override val dseSpokFactoryApi = mockedDSESpokFactoryApi
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.isGroupExist(id, spok.groupId.getOrElse("0"))) thenReturn true
      when(mockedDSESpokFactoryApi.createSpok(id, spok)) thenReturn true
      actorRef ! CreateSpok(spok, id)
      expectMsgType[SpokCreateSuccess](10 seconds)
    }

    "Persists a spok failure when asked to Create a valid text spok with not existing a group id" in {

      val id = UUID.randomUUID().toString
      val url = Url("address", "url_title", "url_text", "url_preview", Some("url_type"))
      val geo = Geo(132233.67, 123244.56, 3133113.3)
      val spok = Spok("content_type", Some("123"), Some("Private"), Some(0), Some("instance_text"), None, Some("text"), Some(url), None,
        None, geo, "0e5c45e0-cfb2-4333-bf30-76a01ea94d64")
      val mockedDSESpokFactoryApi: DSESpokApi = mock[DSESpokApi]
      val mockedDseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
      val actorRef = system.actorOf(Props(new SpokActor(id, Some(id), eventLog) {
        override val dseSpokFactoryApi = mockedDSESpokFactoryApi
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.isGroupExist(id, spok.groupId.getOrElse("0"))) thenReturn false
      when(mockedDSESpokFactoryApi.createSpok(id, spok)) thenReturn false
      actorRef ! CreateSpok(spok, id)
      expectMsgType[SpokCreateFailure](10 seconds)
    }

    "Persists a spok failure when asked to Create a valid text spok" in {

      val id = UUID.randomUUID().toString
      val url = Url("address", "url_title", "url_text", "url_preview", Some("url_type"))
      val geo = Geo(132233.67, 123244.56, 3133113.3)
      val spok = Spok("content_type", Some("0"), Some("Public"), Some(0), Some("instance_text"), None, Some("text"), Some(url), None,
        None, geo, "0e5c45e0-cfb2-4333-bf30-76a01ea94d64")
      val mockedDSESpokFactoryApi: DSESpokApi = mock[DSESpokApi]
      val mockedDseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
      val actorRef = system.actorOf(Props(new SpokActor(id, Some(id), eventLog) {
        override val dseSpokFactoryApi = mockedDSESpokFactoryApi
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
      }))
      when(mockedDseGraphPersistenceFactoryApi.isGroupExist(id, spok.groupId.getOrElse("0"))) thenReturn true
      when(mockedDSESpokFactoryApi.createSpok(id, spok)) thenReturn false
      actorRef ! CreateSpok(spok, id)
      expectMsgType[SpokCreateFailure](10 seconds)
    }

    "Persist a respok when asked to Respok a valid spok" in {

      val userId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val spokInstanceId = UUID.randomUUID().toString
      val geo = Geo(13.67, 12.56, 31.3)
      val respok = Respok(Some("0"), Some("Public"), Some("text"), geo, None)
      val mockedDSEUserSpokFactoryApi: DSEUserSpokFactoryApi = mock[DSEUserSpokFactoryApi]
      val mockedDseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
      val mockedDSESpokFactoryApi: DSESpokApi = mock[DSESpokApi]
      val actorRef = system.actorOf(Props(new SpokActor(id, Some(id), eventLog) {
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseSpokFactoryApi = mockedDSESpokFactoryApi
      }))

      val respokStatistics = RespokStatistics(1, 2, 3, 4.444)
      val spokInstance = SpokInstance("12345", "9876", "status", "Public", Some("text"), Some("0"))
      val result = (Some(RespokInterimResponse(spokInstanceId, respokStatistics, List(), List())), None)
      when(mockedDSEUserSpokFactoryApi.isValidGroup(userId, spokInstance.groupId.getOrElse("0"))) thenReturn true
      when(mockedDSESpokFactoryApi.createRespok(spokInstanceId, userId, respok, None)) thenReturn result
      actorRef ! RespokCreate(respok, spokInstanceId, userId, None)
      expectMsgType[RespokCreateSuccess](10 seconds)
    }

    "Return error message when response is empty for Respok" in {

      val userId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val geo = Geo(13.67, 12.56, 31.3)
      val respok = Respok(Some("0"), Some("Public"), Some("text"), geo, None)
      val mockedDSEUserSpokFactoryApi: DSEUserSpokFactoryApi = mock[DSEUserSpokFactoryApi]
      val mockedDseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
      val mockedDSESpokFactoryApi: DSESpokApi = mock[DSESpokApi]
      val actorRef = system.actorOf(Props(new SpokActor(id, Some(id), eventLog) {
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseSpokFactoryApi = mockedDSESpokFactoryApi
      }))

      val respokStatistics = RespokStatistics(1, 2, 3, 4.444)
      val spokInstance = SpokInstance("12345", "9876", "status", "Public", Some("text"), Some("0"))
      val result = (None, Some(Error(SPK_117, s"Unable re-spoking spok $spokId (generic error).")))
      when(mockedDSEUserSpokFactoryApi.isValidGroup(userId, spokInstance.groupId.getOrElse("0"))) thenReturn true
      when(mockedDSESpokFactoryApi.createRespok(spokId, userId, respok, None)) thenReturn result
      actorRef ! RespokCreate(respok, spokId, userId, None)
      expectMsgType[RespokCreateFailure](10 seconds)
    }

    "Return error message when response is empty for Respok beacuse user tried to alter the visibility of a private spok" in {

      val userId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val geo = Geo(13.67, 12.56, 31.3)
      val respok = Respok(Some("0"), Some("public"), Some("text"), geo, None)
      val mockedDSEUserSpokFactoryApi: DSEUserSpokFactoryApi = mock[DSEUserSpokFactoryApi]
      val mockedDseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
      val mockedDSESpokFactoryApi: DSESpokApi = mock[DSESpokApi]
      val actorRef = system.actorOf(Props(new SpokActor(id, Some(id), eventLog) {
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseSpokFactoryApi = mockedDSESpokFactoryApi
      }))
      val result: (Option[RespokInterimResponse], Option[Error]) = (None, Some(Error(SPK_107, NOT_ALTER_VISIBILITY)))
      when(mockedDSEUserSpokFactoryApi.isValidGroup(userId, "0")) thenReturn true
      when(mockedDSESpokFactoryApi.createRespok(spokId, userId, respok, None)) thenReturn result
      actorRef ! RespokCreate(respok, spokId, userId, None)
      expectMsgType[RespokCreateFailure](10 seconds)
    }

    "Persist a respok failure when asked to Respok a valid spok" in {

      val userId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val geo = Geo(13.67, 12.56, 31.3)
      val respok = Respok(Some("0"), Some("public"), Some("text"), geo, None)
      val mockedDSEUserSpokFactoryApi: DSEUserSpokFactoryApi = mock[DSEUserSpokFactoryApi]
      val mockedDseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
      val mockedDSESpokFactoryApi: DSESpokApi = mock[DSESpokApi]
      val actorRef = system.actorOf(Props(new SpokActor(id, Some(id), eventLog) {
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseSpokFactoryApi = mockedDSESpokFactoryApi
      }))
      val result: (Option[RespokInterimResponse], Option[Error]) = (None, Some(Error(SPK_117, s"Unable re-spoking spok $spokId (generic error).")))
      when(mockedDseGraphPersistenceFactoryApi.isGroupExist(userId, "0")) thenReturn true
      when(mockedDSESpokFactoryApi.createRespok(spokId, userId, respok, None)) thenReturn result
      actorRef ! RespokCreate(respok, spokId, userId, None)
      expectMsgType[RespokCreateFailure](10 seconds)
    }

    "Persist a respok failure when asked to Respok a valid spok with not existing a group id" in {

      val userId = getUUID()
      val id = getUUID()
      val spokId = getUUID()
      val groupId = getUUID()
      val geo = Geo(13.67, 12.56, 31.3)
      val respok = Respok(Some("123"), Some("Private"), Some("text"), geo, None)
      val mockedDSEUserSpokFactoryApi: DSEUserSpokFactoryApi = mock[DSEUserSpokFactoryApi]
      val mockedDseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
      val mockedDSESpokFactoryApi: DSESpokApi = mock[DSESpokApi]
      val actorRef = system.actorOf(Props(new SpokActor(id, Some(id), eventLog) {
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
        override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
        override val dseSpokFactoryApi = mockedDSESpokFactoryApi
      }))

      val respokStatistics = RespokStatistics(1, 2, 3, 4.444)
      when(mockedDseGraphPersistenceFactoryApi.isGroupExist(userId, groupId)) thenReturn false
      actorRef ! RespokCreate(respok, spokId, userId, None)
      expectMsgType[RespokCreateFailure](10 seconds)
    }

    "Persist a unspok when asked to Unspok a valid spok" in {
      val userId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val status = PENDING
      val geo = Geo(13.67, 12.56, 31.3)
      val unspok = Unspok(geo)
      val mockedDSESpokFactoryApi: DSESpokApi = mock[DSESpokApi]
      val actorRef = system.actorOf(Props(new SpokActor(id, Some(id), eventLog) {
        override val dseSpokFactoryApi = mockedDSESpokFactoryApi
      }))
      val respokStatistics = RespokStatistics(1, 2, 3, 4.444)
      val result: UnspokResponse = UnspokResponse(spokId, respokStatistics)
      when(mockedDSESpokFactoryApi.createUnspok(spokId, userId, unspok, status)) thenReturn Some(result)
      actorRef ! PerformUnspok(unspok, spokId, userId, status)
      expectMsgType[UnspokPerformSuccess](10 seconds)
    }

    "Return error when unspoking fails with generic error" in {
      val userId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val status = PENDING
      val geo = Geo(13.67, 12.56, 31.3)
      val unspok = Unspok(geo)
      val mockedDSESpokFactoryApi: DSESpokApi = mock[DSESpokApi]
      val actorRef = system.actorOf(Props(new SpokActor(id, Some(id), eventLog) {
        override val dseSpokFactoryApi = mockedDSESpokFactoryApi
      }))
      when(mockedDSESpokFactoryApi.createUnspok(spokId, userId, unspok, status)) thenReturn None
      actorRef ! PerformUnspok(unspok, spokId, userId, status)
      expectMsgType[UnspokPerformFailure](10 seconds)
    }

    "Able to get success message if a user is successfully able to add a comment on a spok" in {

      val commentId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val spokId1 = UUID.randomUUID().toString
      val commenterUserId = UUID.randomUUID().toString
      val text = "text"
      val geo = Geo(13.67, 14.56, 33.3)
      val comment = Comment(commentId, text, geo, Nil)
      val response =
        s"""
         {"spok" : {
        "spokId":"$spokId1",
        "nbRespoked":"2",
        "nbLanded":"4",
        "nbComments":"3",
        "travelled":"6"
        },
       "user" : {
       "id":"$commenterUserId",
       "nickName":"10",
       "gender":"20",
       "picture":"30"
        },
        "mentionUserId":[],
        "commentId":"""" + commentId +
          """"}
          """
      val mockedSpokCommentApi: SpokCommentApi = mock[SpokCommentApi]
      val mockedDSEUserSpokFactoryApi: DSEUserSpokFactoryApi = mock[DSEUserSpokFactoryApi]

      val actorRef = system.actorOf(Props(new SpokActor(id, Some(id), eventLog) {
        override val dseSpokCommentApi = mockedSpokCommentApi
      }))
      when(mockedSpokCommentApi.addComment(spokId1, commentId, commenterUserId, text, geo, Nil)) thenReturn ((Some(parse(response).extract[SpokCommentResponse]), 1l, None))
      actorRef ! CommentAdd(comment, spokId1, commenterUserId)
      expectMsgType[AddCommentSuccess](10 seconds)
    }

    "Able to get an error message if the user is not successfully able to add a comment on a spok " in {

      val commentId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val spokId1 = UUID.randomUUID().toString
      val commenterUserId = UUID.randomUUID().toString
      val text = "text"
      val geo = Geo(13.67, 14.56, 33.3)
      val comment = Comment(commentId, text, geo, Nil)

      val mockedSpokCommentApi: SpokCommentApi = mock[SpokCommentApi]
      val mockedDSEUserSpokFactoryApi: DSEUserSpokFactoryApi = mock[DSEUserSpokFactoryApi]
      val actorRef = system.actorOf(Props(new SpokActor(id, Some(id), eventLog) {
        override val dseSpokCommentApi = mockedSpokCommentApi
      }))
      when(mockedSpokCommentApi.addComment(spokId1, commentId, commenterUserId, text, geo, Nil)) thenReturn ((None, 1l, Some(Error(SPK_001, s"Spok $spokId1 not found"))))
      actorRef ! CommentAdd(comment, spokId1, commenterUserId)
      expectMsgType[AddCommentFailure](10 seconds)
    }

    "Able to get an error message if spok is not found trying to comment on it " in {

      val commentId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val spokId1 = UUID.randomUUID().toString
      val commenterUserId = UUID.randomUUID().toString
      val text = "text"
      val geo = Geo(13.67, 14.56, 33.3)
      val comment = Comment(commentId, text, geo, Nil)
      val mockedSpokCommentApi: SpokCommentApi = mock[SpokCommentApi]
      val mockedDSEUserSpokFactoryApi: DSEUserSpokFactoryApi = mock[DSEUserSpokFactoryApi]
      val actorRef = system.actorOf(Props(new SpokActor(id, Some(id), eventLog) {
        override val dseSpokCommentApi = mockedSpokCommentApi
      }))
      when(mockedSpokCommentApi.addComment(spokId1, commentId, commenterUserId, text, geo, Nil)) thenReturn ((None, 1l, Some(Error(SPK_001, s"Spok $spokId1 not found"))))
      actorRef ! CommentAdd(comment, spokId1, commenterUserId)
      expectMsgType[AddCommentFailure](10 seconds)
    }

    "Able to get success message if comment is updated successfully " in {

      val commentId = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val commenterUserId = UUID.randomUUID().toString
      val text = "updated text"
      val geo = Geo(13.67, 14.56, 33.3)
      val comment = Comment(commentId, text, geo, Nil)
      val updateResponse =
        s"""
          {
        "spokId":"$spokId",
        "nbRespoked":"2",
        "nbLanded":"4",
        "nbComments":"3",
        "travelled":"6"
        "mentionUserId":"$None"
        "commentId":"$commentId"
       }
    """
      val mockedSpokCommentApi: SpokCommentApi = mock[SpokCommentApi]
      val actorRef = system.actorOf(Props(new SpokActor(id, Some(id), eventLog) {
        override val dseSpokCommentApi = mockedSpokCommentApi
      }))
      when(mockedSpokCommentApi.updateComment(commenterUserId, comment)) thenReturn ((Some(parse(updateResponse).extract[CommentUpdateResponse]), 1l, None))
      actorRef ! CommentUpdate(comment, commenterUserId)
      expectMsgType[UpdateCommentSuccess](10 seconds)
    }

    "Able to get error message if comment is not found while updating" in {

      val commentId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val commenterUserId = UUID.randomUUID().toString
      val text = "updated text"
      val geo = Geo(13.67, 14.56, 33.3)
      val comment = Comment(commentId, text, geo, Nil)
      val mockedSpokCommentApi: SpokCommentApi = mock[SpokCommentApi]
      val actorRef = system.actorOf(Props(new SpokActor(id, Some(id), eventLog) {
        override val dseSpokCommentApi = mockedSpokCommentApi
      }))
      when(mockedSpokCommentApi.updateComment(commenterUserId, comment)) thenReturn ((None, 1l, Some(Error(SPK_008, s"Comment $commentId not found"))))
      actorRef ! CommentUpdate(comment, commenterUserId)
      expectMsgType[UpdateCommentFailure](10 seconds)
    }

    "Able to get error message if comment is not updated successfully " in {

      val commentId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val commenterUserId = UUID.randomUUID().toString
      val text = "updated text"
      val geo = Geo(13.67, 14.56, 33.3)
      val comment = Comment(commentId, text, geo, Nil)
      val mockedSpokCommentApi: SpokCommentApi = mock[SpokCommentApi]
      val actorRef = system.actorOf(Props(new SpokActor(id, Some(id), eventLog) {
        override val dseSpokCommentApi = mockedSpokCommentApi
      }))
      when(mockedSpokCommentApi.updateComment(commenterUserId, comment)) thenReturn ((None, 1l, Some(Error(SPK_120, s"Unable updating comment $commentId(generic error)."))))
      actorRef ! CommentUpdate(comment, commenterUserId)
      expectMsgType[UpdateCommentFailure](10 seconds)
    }

    "Able to get success message if comment is remove successfully " in {

      val commentId = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val commenterUserId = UUID.randomUUID().toString
      val geo = Geo(45.00, 45.00, 45.00)
      val removeResponse =
        s"""
          {
          "commentId":"$commentId",
         "spok" : {
        "spokId":"$spokId",
        "nbRespoked":"2",
        "nbLanded":"4",
        "nbComments":"3",
        "travelled":"6"
        }
       }
    """

      val mockedSpokCommentApi: SpokCommentApi = mock[SpokCommentApi]
      val mockedDSEUserSpokFactoryApi: DSEUserSpokFactoryApi = mock[DSEUserSpokFactoryApi]
      val actorRef = system.actorOf(Props(new SpokActor(id, Some(id), eventLog) {
        override val dseSpokCommentApi = mockedSpokCommentApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDSEUserSpokFactoryApi.validateCommentById(commentId, commenterUserId)) thenReturn true
      when(mockedSpokCommentApi.getSpokCreaterIdByCommentId(commentId)) thenReturn Some(commenterUserId)
      when(mockedSpokCommentApi.removeComment(commentId, commenterUserId, geo)) thenReturn ((Some(parse(removeResponse).extract[RemoveCommentResponse]), 1l, None))

      actorRef ! CommentRemove(commentId, commenterUserId, geo)
      expectMsgType[RemoveCommentSuccess](10 seconds)
    }

    "Able to get error message if comment is not found while removing" in {

      val commentId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val commenterUserId = UUID.randomUUID().toString
      val geo = Geo(45.00, 45.00, 45.00)
      val mockedSpokCommentApi: SpokCommentApi = mock[SpokCommentApi]
      val mockedDSEUserSpokFactoryApi: DSEUserSpokFactoryApi = mock[DSEUserSpokFactoryApi]
      val actorRef = system.actorOf(Props(new SpokActor(id, Some(id), eventLog) {
        override val dseSpokCommentApi = mockedSpokCommentApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDSEUserSpokFactoryApi.validateCommentById(commentId, commenterUserId)) thenReturn false
      when(mockedSpokCommentApi.getSpokCreaterIdByCommentId(commentId)) thenReturn Some(id)
      when(mockedSpokCommentApi.removeComment(commentId, commenterUserId, geo)) thenReturn ((None, 1l, Some(Error(SPK_121, s"Unable removing comment $commentId(generic error)."))))

      actorRef ! CommentRemove(commentId, commenterUserId, geo)
      expectMsgType[RemoveCommentFailure](10 seconds)
    }

    "Able to get error message if comment is not remove successfully " in {

      val commentId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val commenterUserId = UUID.randomUUID().toString
      val geo = Geo(45.00, 45.00, 45.00)
      val mockedSpokCommentApi: SpokCommentApi = mock[SpokCommentApi]
      val mockedDSEUserSpokFactoryApi: DSEUserSpokFactoryApi = mock[DSEUserSpokFactoryApi]
      val actorRef = system.actorOf(Props(new SpokActor(id, Some(id), eventLog) {
        override val dseSpokCommentApi = mockedSpokCommentApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDSEUserSpokFactoryApi.validateCommentById(commentId, commenterUserId)) thenReturn false
      when(mockedSpokCommentApi.getSpokCreaterIdByCommentId(commentId)) thenReturn Some(commenterUserId)
      when(mockedSpokCommentApi.removeComment(commentId, commenterUserId, geo)) thenReturn ((None, 1l, Some(Error(SPK_121, s"Unable removing comment $commentId(generic error)."))))

      actorRef ! CommentRemove(commentId, commenterUserId, geo)
      expectMsgType[RemoveCommentFailure](10 seconds)
    }

    "Able to get error message if comment is not done by the user who is trying to remove comment " in {

      val commentId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val commenterUserId = UUID.randomUUID().toString
      val geo = Geo(45.00, 45.00, 45.00)
      val mockedSpokCommentApi: SpokCommentApi = mock[SpokCommentApi]
      val mockedDSEUserSpokFactoryApi: DSEUserSpokFactoryApi = mock[DSEUserSpokFactoryApi]
      val actorRef = system.actorOf(Props(new SpokActor(id, Some(id), eventLog) {
        override val dseSpokCommentApi = mockedSpokCommentApi
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDSEUserSpokFactoryApi.validateCommentById(commentId, commenterUserId)) thenReturn false
      when(mockedSpokCommentApi.getSpokCreaterIdByCommentId(commentId)) thenReturn Some("45646")
      when(mockedSpokCommentApi.removeComment(commentId, commenterUserId, geo)) thenReturn ((None, 1l, Some(Error(SPK_121, s"Unable removing comment $commentId(generic error)."))))

      actorRef ! CommentRemove(commentId, commenterUserId, geo)
      expectMsgType[RemoveCommentFailure](10 seconds)
    }

    "Able to get success message if a poll question is answered and saved successfully " in {

      val questionId = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val answerId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val geo = Geo(13.67, 14.56, 33.3)
      val userPollAnswer = UserPollAnswer(answerId, geo)
      val mockedDSEUserSpokFactoryApi: DSEUserSpokFactoryApi = mock[DSEUserSpokFactoryApi]
      val mockedDSESpokFactoryApi: DSESpokApi = mock[DSESpokApi]
      val actorRef = system.actorOf(Props(new SpokActor(id, Some(id), eventLog) {
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
        override val dseSpokFactoryApi = mockedDSESpokFactoryApi
      }))
      when(mockedDSEUserSpokFactoryApi.validatePollQuestionAndFetchSpokId(questionId)) thenReturn (Some(spokId))
      when(mockedDSESpokFactoryApi.addAnswerToAPoll(questionId, spokId, userId, userPollAnswer)) thenReturn None
      actorRef ! SaveAnswer(questionId, userId, userPollAnswer)
      expectMsgType[PollAnswerSavedSuccess](10 seconds)
    }

    "Able to send error message if a poll question id is not valid when answering a poll question " in {

      val questionId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val answerId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val geo = Geo(13.67, 14.56, 33.3)
      val userPollAnswer = UserPollAnswer(answerId, geo)
      val mockedDSEUserSpokFactoryApi: DSEUserSpokFactoryApi = mock[DSEUserSpokFactoryApi]
      val actorRef = system.actorOf(Props(new SpokActor(id, Some(id), eventLog) {
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
      }))
      when(mockedDSEUserSpokFactoryApi.validatePollQuestionAndFetchSpokId(questionId)) thenReturn None
      actorRef ! SaveAnswer(questionId, userId, userPollAnswer)
      expectMsgType[PollAnswerSavedFailure](10 seconds)
    }

    "Able to send error message if a poll answer id is not valid when answering a poll question " in {

      val questionId = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val answerId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val geo = Geo(13.67, 14.56, 33.3)
      val userPollAnswer = UserPollAnswer(answerId, geo)
      val mockedDSEUserSpokFactoryApi: DSEUserSpokFactoryApi = mock[DSEUserSpokFactoryApi]
      val mockedDSESpokFactoryApi: DSESpokApi = mock[DSESpokApi]
      val actorRef = system.actorOf(Props(new SpokActor(id, Some(id), eventLog) {
        override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
        override val dseSpokFactoryApi = mockedDSESpokFactoryApi
      }))
      when(mockedDSEUserSpokFactoryApi.validatePollQuestionAndFetchSpokId(questionId)) thenReturn (Some(spokId))
      when(mockedDSESpokFactoryApi.addAnswerToAPoll(questionId, spokId, userId, userPollAnswer)) thenReturn Some(Error(s"Invalid answer to question $questionId.", SPK_010))
      actorRef ! SaveAnswer(questionId, userId, userPollAnswer)
      expectMsgType[PollAnswerSavedFailure](10 seconds)
    }

    "Able to disable a spok " in {
      val id = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val launchedTime = System.currentTimeMillis()
      val geo = Geo(45.00, 45.00, 45.00)
      val mockedDSESpokFactoryApi: DSESpokApi = mock[DSESpokApi]
      val actorRef = system.actorOf(Props(new SpokActor(id, Some(id), eventLog) {
        override val dseSpokFactoryApi: DSESpokApi = mockedDSESpokFactoryApi
      }))
      when(mockedDSESpokFactoryApi.disableSpok(spokId, userId, geo)) thenReturn SPOK_DISABLED
      actorRef ! DisableSpok(spokId, userId, launchedTime, geo)
      expectMsgType[DisableSpokSuccess](10 seconds)
    }

    "Able to return generic error message if unable to disable a spok " in {
      val id = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val geo = Geo(45.00, 45.00, 45.00)
      val launchedTime = System.currentTimeMillis()
      val mockedDSESpokFactoryApi: DSESpokApi = mock[DSESpokApi]
      val actorRef = system.actorOf(Props(new SpokActor(id, Some(id), eventLog) {
        override val dseSpokFactoryApi: DSESpokApi = mockedDSESpokFactoryApi
      }))
      when(mockedDSESpokFactoryApi.disableSpok(spokId, userId, geo)) thenReturn UNABLE_DISABLING_SPOK
      actorRef ! DisableSpok(spokId, userId, launchedTime, geo)
      expectMsgType[DisableSpokFailure](10 seconds)
    }

    "Able to return invalid user error message if user not found while disabling a spok" in {
      val id = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val geo = Geo(45.00, 45.00, 45.00)
      val launchedTime = System.currentTimeMillis()
      val mockedDSESpokFactoryApi: DSESpokApi = mock[DSESpokApi]
      val actorRef = system.actorOf(Props(new SpokActor(id, Some(id), eventLog) {
        override val dseSpokFactoryApi: DSESpokApi = mockedDSESpokFactoryApi
      }))
      when(mockedDSESpokFactoryApi.disableSpok(spokId, userId, geo)) thenReturn INVALID_USER
      actorRef ! DisableSpok(spokId, userId, launchedTime, geo)
      expectMsgType[DisableSpokFailure](10 seconds)
    }

    "Able to return spok already disabled error message if spok is already disabled while disabling a spok" in {
      val id = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val launchedTime = System.currentTimeMillis()
      val geo = Geo(45.00, 45.00, 45.00)
      val mockedDSESpokFactoryApi: DSESpokApi = mock[DSESpokApi]
      val actorRef = system.actorOf(Props(new SpokActor(id, Some(id), eventLog) {
        override val dseSpokFactoryApi: DSESpokApi = mockedDSESpokFactoryApi
      }))
      when(mockedDSESpokFactoryApi.disableSpok(spokId, userId, geo)) thenReturn DISABLED_SPOK
      actorRef ! DisableSpok(spokId, userId, launchedTime, geo)
      expectMsgType[DisableSpokFailure](10 seconds)
    }

    "Able to return spok not found error message if spok is not found while disabling a spok" in {
      val id = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val launchedTime = System.currentTimeMillis()
      val geo = Geo(45.00, 45.00, 45.00)
      val mockedDSESpokFactoryApi: DSESpokApi = mock[DSESpokApi]
      val actorRef = system.actorOf(Props(new SpokActor(id, Some(id), eventLog) {
        override val dseSpokFactoryApi: DSESpokApi = mockedDSESpokFactoryApi
      }))
      when(mockedDSESpokFactoryApi.disableSpok(spokId, userId, geo)) thenReturn SPOK_NOT_FOUND
      actorRef ! DisableSpok(spokId, userId, launchedTime, geo)
      expectMsgType[DisableSpokFailure](10 seconds)
    }

    "able to remove a spok from wall " in {
      val id = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val launchedTime = System.currentTimeMillis()
      val geo = Geo(45.00, 45.00, 45.00)
      val mockedDSESpokFactoryApi: DSESpokApi = mock[DSESpokApi]
      val actorRef = system.actorOf(Props(new SpokActor(id, Some(id), eventLog) {
        override val dseSpokFactoryApi: DSESpokApi = mockedDSESpokFactoryApi
      }))
      when(mockedDSESpokFactoryApi.removeSpokFromWall(spokId, userId, launchedTime, geo)) thenReturn RemoveSpokResponse(Some(spokId))
      actorRef ! RemoveWallSpok(spokId, userId, launchedTime, geo)
      expectMsgType[RemoveWallSpokSuccess](10 seconds)
    }

    "able to return error message while removing spok from wall if spok is not respoked" in {
      val id = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val launchedTime = System.currentTimeMillis()
      val geo = Geo(45.00, 45.00, 45.00)
      val mockedDSESpokFactoryApi: DSESpokApi = mock[DSESpokApi]
      val actorRef = system.actorOf(Props(new SpokActor(id, Some(id), eventLog) {
        override val dseSpokFactoryApi: DSESpokApi = mockedDSESpokFactoryApi
      }))
      when(mockedDSESpokFactoryApi.removeSpokFromWall(spokId, userId, launchedTime, geo)) thenReturn RemoveSpokResponse(message = Some(SPOK_STATUS_NOT_RESPOKED))
      actorRef ! RemoveWallSpok(spokId, userId, launchedTime, geo)
      expectMsgType[RemoveWallSpokFailure](10 seconds)
    }

    "able to return error message while removing spok from wall if there is a generic error" in {
      val id = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val launchedTime = System.currentTimeMillis()
      val geo = Geo(45.00, 45.00, 45.00)
      val mockedDSESpokFactoryApi: DSESpokApi = mock[DSESpokApi]
      val actorRef = system.actorOf(Props(new SpokActor(id, Some(id), eventLog) {
        override val dseSpokFactoryApi: DSESpokApi = mockedDSESpokFactoryApi
      }))
      when(mockedDSESpokFactoryApi.removeSpokFromWall(spokId, userId, launchedTime, geo)) thenReturn RemoveSpokResponse(message = Some(UNABLE_REMOVING_SPOK))
      actorRef ! RemoveWallSpok(spokId, userId, launchedTime, geo)
      expectMsgType[RemoveWallSpokFailure](10 seconds)
    }

    "able to get success message if all of the poll's questions are answered successfully " in {

      val questionId = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val answerId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val geo = Geo(13.67, 14.56, 33.3)
      val mockedDSESpokFactoryApi: DSESpokApi = mock[DSESpokApi]
      val actorRef = system.actorOf(Props(new SpokActor(id, Some(id), eventLog) {
        override val dseSpokFactoryApi = mockedDSESpokFactoryApi
      }))
      val allAnswers: AllAnswers = AllAnswers(spokId, List(OneAnswer(questionId, answerId)), geo)
      when(mockedDSESpokFactoryApi.validateAbsoluteSpokById(allAnswers.spokId)) thenReturn (SPOK_VALID)
      when(mockedDSESpokFactoryApi.addAllAnswersToAPoll(userId, allAnswers)) thenReturn ((None, Nil))
      actorRef ! SaveAllAnswersOfPoll(userId, allAnswers)
      expectMsgType[PollAllAnswersSavedSuccess](10 seconds)
    }

    "able to get error message if all poll's questions are not answered successfully" in {

      val questionId = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val answerId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val geo = Geo(13.67, 14.56, 33.3)
      val mockedDSESpokFactoryApi: DSESpokApi = mock[DSESpokApi]
      val actorRef = system.actorOf(Props(new SpokActor(id, Some(id), eventLog) {
        override val dseSpokFactoryApi = mockedDSESpokFactoryApi
      }))
      val allAnswers: AllAnswers = AllAnswers(spokId, List(OneAnswer(questionId, answerId)), geo)
      when(mockedDSESpokFactoryApi.validateAbsoluteSpokById(allAnswers.spokId)) thenReturn (SPOK_VALID)
      when(mockedDSESpokFactoryApi.addAllAnswersToAPoll(userId, allAnswers)) thenReturn
        ((Some(Error(SPK_135, s"Spok ${allAnswers.spokId} already completed.")), Nil))
      actorRef ! SaveAllAnswersOfPoll(userId, allAnswers)
      expectMsgType[PollAllAnswersSavedFailure](10 seconds)
    }

  }
}
