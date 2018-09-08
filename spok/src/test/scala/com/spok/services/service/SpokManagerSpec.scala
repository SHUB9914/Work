package com.spok.services.service

import java.util.UUID
import akka.actor.{ ActorSystem, Props }
import akka.testkit.{ ImplicitSender, TestKit }
import com.rbmhtechnology.eventuate.ReplicationEndpoint
import com.rbmhtechnology.eventuate.ReplicationEndpoint._
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
import scala.concurrent.duration._
import com.spok.services.service.SpokManagerCommands._
import com.spok.services.service.SpokActorSuccessReplies._
import com.spok.services.service.SpokActorFailureReplies._

class SpokManagerSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
    with WordSpecLike with MustMatchers with BeforeAndAfterAll with TestHelper with MockitoSugar with JsonHelper {

  def this() = this(ActorSystem("SpokManagerSystem"))

  val endpoint = ReplicationEndpoint(id => LeveldbEventLog.props(logId = "SpokManagerSpec"))(_system)
  val eventLog = endpoint.logs(DefaultLogName)

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
    session.get.close()
  }

  "A SpokManager" must {

    "Create a spok by Spok manager" in {

      val userId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val url = Url("address", "url_title", "url_text", "url_preview", Some("url_type"))
      val geo = Geo(132233.67, 123244.56, 3133113.3)
      val spok = Spok("content_type", Some("0"), Some("public"), Some(0), Some("instance_text"), None, Some("text"), Some(url), None, None, geo, userId)
      val mockedDSESpokFactoryApi: DSESpokApi = mock[DSESpokApi]
      val mockedDseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = mock[DSEGraphPersistenceFactoryApi]
      val actorRef = system.actorOf(Props(new SpokManager(id, eventLog) {
        override def createActor(id: String): SpokActor = {
          new SpokActor(id, Some(id), eventLog) {
            override val dseSpokFactoryApi = mockedDSESpokFactoryApi
            override val dseGraphPersistenceFactoryApi = mockedDseGraphPersistenceFactoryApi
            when(mockedDseGraphPersistenceFactoryApi.isGroupExist(id, spok.groupId.getOrElse("0"))) thenReturn true
            when(dseSpokFactoryApi.createSpok(userId, spok)) thenReturn true
          }
        }
      }))
      actorRef ! Create(spok, userId)
      expectMsgType[SpokCreateSuccess](30 seconds)
    }

    "perform a respok for valid details when asked to respok" in {

      val userId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val geo = Geo(13.67, 12.56, 31.33)
      val respok = Respok(Some("0"), Some("Public"), Some("text"), geo, None)
      val respokResponse: (Option[RespokInterimResponse], Option[Error]) = (Some(RespokInterimResponse(spokId, RespokStatistics(50, 50, 50, 1000),
        List(), List())), None)
      val actorRef = system.actorOf(Props(new SpokManager(id, eventLog) {
        val mockedDSEUserSpokFactoryApi: DSEUserSpokFactoryApi = mock[DSEUserSpokFactoryApi]
        val mockedDSESpokFactoryApi: DSESpokApi = mock[DSESpokApi]

        override def createActor(id: String): SpokActor = {
          new SpokActor(id, Some(id), eventLog) {
            override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
            override val dseSpokFactoryApi = mockedDSESpokFactoryApi
            when(mockedDSEUserSpokFactoryApi.isValidGroup(userId, respok.groupId.getOrElse("0"))) thenReturn true
            when(mockedDSESpokFactoryApi.createRespok(spokId, userId, respok, None)) thenReturn respokResponse
          }
        }
      }))
      actorRef ! CreateRespok(respok, spokId, userId, None)
      expectMsgType[RespokCreateSuccess](30 seconds)
    }

    "perform a unspok for valid details when asked to unspok" in {

      val userId = UUID.randomUUID().toString
      val geo = Geo(13.67, 12.56, 31.33)
      val unspok = Unspok(geo)
      val status = PENDING
      val respokResponse = UnspokResponse(userId, RespokStatistics(50, 50, 50, 1000))
      val mockedDSESpokFactoryApi: DSESpokApi = mock[DSESpokApi]
      val actorRef = system.actorOf(Props(new SpokManager(userId, eventLog) {

        override def createActor(spokId: String): SpokActor = {
          new SpokActor(spokId, Some(spokId), eventLog) {
            override val dseSpokFactoryApi = mockedDSESpokFactoryApi
            when(dseSpokFactoryApi.createUnspok(userId, userId, unspok, status)) thenReturn Some(respokResponse)
          }
        }
      }))
      actorRef ! ExecuteUnspok(unspok, userId, userId, status)
      expectMsgType[UnspokPerformSuccess](30 seconds)
    }

    "Return error if unspoking fails due to generic error" in {

      val userId = UUID.randomUUID().toString
      val geo = Geo(13.67, 12.56, 31.33)
      val unspok = Unspok(geo)
      val status = PENDING
      val mockedDSESpokFactoryApi: DSESpokApi = mock[DSESpokApi]
      val actorRef = system.actorOf(Props(new SpokManager(userId, eventLog) {

        override def createActor(spokId: String): SpokActor = {
          new SpokActor(spokId, Some(spokId), eventLog) {
            override val dseSpokFactoryApi = mockedDSESpokFactoryApi
            when(dseSpokFactoryApi.createUnspok(userId, userId, unspok, status)) thenReturn None
          }
        }
      }))
      actorRef ! ExecuteUnspok(unspok, userId, userId, status)
      expectMsgType[UnspokPerformFailure](30 seconds)
    }

    "Add a Comment by Spok manager" in {

      val commentId = UUID.randomUUID().toString
      val spokId1 = UUID.randomUUID().toString
      val commenterUserId = UUID.randomUUID().toString
      val text = "text"
      val geo = Geo(13.67, 14.56, 33.3)
      val comment = Comment(commentId, text, geo, Nil)
      val response = s"""
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
    "commentId":"""" + commentId + """"}"""

      val mockedSpokCommentApi: SpokCommentApi = mock[SpokCommentApi]
      val mockedDSEUserSpokFactoryApi: DSEUserSpokFactoryApi = mock[DSEUserSpokFactoryApi]
      val actorRef = system.actorOf(Props(new SpokManager(commenterUserId, eventLog) {
        override def createActor(spokId: String): SpokActor = {
          val mockedSpokCommentApi: SpokCommentApi = mock[SpokCommentApi]
          val mockedDSEUserSpokFactoryApi: DSEUserSpokFactoryApi = mock[DSEUserSpokFactoryApi]
          new SpokActor(commenterUserId, Some(commenterUserId), eventLog) {
            override val dseSpokCommentApi = mockedSpokCommentApi
            when(dseSpokCommentApi.addComment(spokId1, commentId, commenterUserId, text, geo, Nil)) thenReturn ((Some(parse(response).extract[SpokCommentResponse]), 1l, None))
          }
        }
      }))
      actorRef ! CreateComment(comment, spokId1, commenterUserId)
      expectMsgType[AddCommentSuccess](30 seconds)
    }

    "Able to send success message if a user successfully updates a comment" in {

      val commentId = getUUID()
      val spokId = getUUID()
      val commenterUserId = getUUID()
      val text = "updated text"
      val geo = Geo(13.67, 14.56, 33.3)
      val comment = Comment(commentId, text, geo, Nil)
      val updateResponse = s"""
      {
    "spokId":"$spokId",
    "nbRespoked":"2",
    "nbLanded":"4",
    "nbComments":"3",
    "travelled":"6",
    "mentionUserId":[],
    "commentId":"$commentId"
     }
"""
      val mockedSpokCommentApi: SpokCommentApi = mock[SpokCommentApi]
      val mockedDSEUserSpokFactoryApi: DSEUserSpokFactoryApi = mock[DSEUserSpokFactoryApi]
      val actorRef = system.actorOf(Props(new SpokManager(commenterUserId, eventLog) {
        override def createActor(spokId: String): SpokActor = {
          val mockedSpokCommentApi: SpokCommentApi = mock[SpokCommentApi]
          new SpokActor(commenterUserId, Some(commenterUserId), eventLog) {
            override val dseSpokCommentApi = mockedSpokCommentApi
            when(mockedSpokCommentApi.updateComment(commenterUserId, comment)) thenReturn ((Some(parse(updateResponse).extract[CommentUpdateResponse]), 1l, None))
          }
        }
      }))

      actorRef ! UpdateComment(comment, commenterUserId)
      expectMsgType[UpdateCommentSuccess](30 seconds)
    }

    "Able to send error message if a comment is not found when user tries to update a comment" in {

      val commentId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val commenterUserId = UUID.randomUUID().toString
      val text = "updated text"
      val geo = Geo(13.67, 14.56, 33.3)
      val comment = Comment(commentId, text, geo, Nil)
      val mockedSpokCommentApi: SpokCommentApi = mock[SpokCommentApi]
      val actorRef = system.actorOf(Props(new SpokManager(userId, eventLog) {
        val mockedSpokCommentApi: SpokCommentApi = mock[SpokCommentApi]
        override def createActor(spokId: String): SpokActor = {
          new SpokActor(commenterUserId, Some(commenterUserId), eventLog) {
            override val dseSpokCommentApi = mockedSpokCommentApi
            when(mockedSpokCommentApi.updateComment(commenterUserId, comment)) thenReturn ((None, 1l, Some(Error(SPK_008, s"Comment $commentId not found"))))
          }
        }
      }))
      actorRef ! UpdateComment(comment, commenterUserId)
      expectMsgType[UpdateCommentFailure](30 seconds)
    }

    "Able to send error message if the comment is not updated due to some error" in {

      val commentId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val commenterUserId = UUID.randomUUID().toString
      val text = "updated text"
      val geo = Geo(13.67, 14.56, 33.3)
      val comment = Comment(commentId, text, geo, Nil)
      val updateResponse = s"""
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
      val actorRef = system.actorOf(Props(new SpokManager(userId, eventLog) {
        val mockedSpokCommentApi: SpokCommentApi = mock[SpokCommentApi]
        override def createActor(spokId: String): SpokActor = {
          new SpokActor(commenterUserId, Some(commenterUserId), eventLog) {
            override val dseSpokCommentApi = mockedSpokCommentApi
            when(mockedSpokCommentApi.updateComment(commenterUserId, comment)) thenReturn ((None, 1l, Some(Error(SPK_120, s"Unable updating comment $commentId(generic error)."))))
          }
        }
      }))
      actorRef ! UpdateComment(comment, commenterUserId)
      expectMsgType[UpdateCommentFailure](20 seconds)
    }

    "Able to send success message if a user successfully remove a comment" in {

      val commentId = UUID.randomUUID().toString
      val spokId1 = UUID.randomUUID().toString
      val commenterUserId = UUID.randomUUID().toString
      val geo = Geo(45.00, 45.00, 45.00)
      val removeResponse = s"""
      {
      "commentId":"$commentId",
     "spok" : {
    "spokId":"$spokId1",
    "nbRespoked":"2",
    "nbLanded":"4",
    "nbComments":"3",
    "travelled":"6"
    }
   }
"""
      val actorRef = system.actorOf(Props(new SpokManager(commenterUserId, eventLog) {
        val mockedSpokCommentApi: SpokCommentApi = mock[SpokCommentApi]
        override def createActor(spokId: String): SpokActor = {
          new SpokActor(commenterUserId, Some(commenterUserId), eventLog) {
            override val dseSpokCommentApi = mockedSpokCommentApi
            when(dseSpokCommentApi.removeComment(commentId, commenterUserId, geo)) thenReturn ((Some(parse(removeResponse).extract[RemoveCommentResponse]), 1l, None))
          }
        }
      }))
      actorRef ! RemoveComment(commentId, commenterUserId, geo)
      expectMsgType[RemoveCommentSuccess](30 seconds)
    }

    "Able to send error message if a comment is not found when user tries to remove a comment" in {

      val commentId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val spokId1 = UUID.randomUUID().toString
      val commenterUserId = UUID.randomUUID().toString
      val geo = Geo(45.00, 45.00, 45.00)
      val actorRef = system.actorOf(Props(new SpokManager(id, eventLog) {
        val mockedSpokCommentApi: SpokCommentApi = mock[SpokCommentApi]
        override def createActor(spokId: String): SpokActor = {
          new SpokActor(commenterUserId, Some(commenterUserId), eventLog) {
            override val dseSpokCommentApi = mockedSpokCommentApi
            when(dseSpokCommentApi.removeComment(commentId, commenterUserId, geo)) thenReturn ((None, 1l, Some(Error(SPK_008, s"Comment $commentId not found"))))

          }
        }
      }))
      actorRef ! RemoveComment(commentId, commenterUserId, geo)
      expectMsgType[RemoveCommentFailure](30 seconds)
    }

    "Able to send error message if the comment is not removed due to some error" in {

      val commentId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val commenterUserId = UUID.randomUUID().toString
      val spokId1 = UUID.randomUUID().toString
      val geo = Geo(45.00, 45.00, 45.00)
      val removeResponse = s"""
      {
      "commentId":"$commentId",
     "spok" : {
    "spokId":"$spokId1",
    "nbRespoked":"2",
    "nbLanded":"4",
    "nbComments":"3",
    "travelled":"6"
    }
   }
"""
      val actorRef = system.actorOf(Props(new SpokManager(id, eventLog) {
        val mockedSpokCommentApi: SpokCommentApi = mock[SpokCommentApi]
        val mockedDSEUserSpokFactoryApi: DSEUserSpokFactoryApi = mock[DSEUserSpokFactoryApi]
        override def createActor(spokId: String): SpokActor = {
          new SpokActor(commenterUserId, Some(commenterUserId), eventLog) {
            override val dseSpokCommentApi = mockedSpokCommentApi
            override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
            when(dseSpokCommentApi.removeComment(commentId, commenterUserId, geo)) thenReturn ((None, 1l, Some(Error(SPK_121, s"Unable removing comment $commentId(generic error)."))))
            when(dseUserSpokFactoryApi.validateCommentById(commentId, commenterUserId)) thenReturn true
            when(dseSpokCommentApi.getSpokCreaterIdByCommentId(commentId)) thenReturn Some(commenterUserId)

          }
        }
      }))
      actorRef ! RemoveComment(commentId, commenterUserId, geo)
      expectMsgType[RemoveCommentFailure](30 seconds)
    }

    "Able to send success message if a poll question is answered and saved successfully" in {

      val questionId = UUID.randomUUID().toString
      val pollSpokId = UUID.randomUUID().toString
      val answerId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val geo = Geo(13.67, 14.56, 33.3)
      val userPollAnswer = UserPollAnswer(answerId, geo)
      val actorRef = system.actorOf(Props(new SpokManager(userId, eventLog) {
        val mockedDSEUserSpokFactoryApi: DSEUserSpokFactoryApi = mock[DSEUserSpokFactoryApi]
        val mockedDSESpokFactoryApi: DSESpokApi = mock[DSESpokApi]

        override def createActor(spokId: String): SpokActor = {
          new SpokActor(spokId, Some(spokId), eventLog) {
            override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
            override val dseSpokFactoryApi = mockedDSESpokFactoryApi
            when(mockedDSEUserSpokFactoryApi.validatePollQuestionAndFetchSpokId(questionId)) thenReturn (Some(pollSpokId))
            when(mockedDSESpokFactoryApi.addAnswerToAPoll(questionId, pollSpokId, userId, userPollAnswer)) thenReturn None
          }
        }
      }))

      actorRef ! SavePollAnswer(questionId, userId, userPollAnswer)
      expectMsgType[PollAnswerSavedSuccess](30 seconds)
    }

    "Able to send error message if a poll question id is invalid while answering a poll question" in {

      val questionId = UUID.randomUUID().toString
      val answerId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val geo = Geo(13.67, 14.56, 33.3)
      val userPollAnswer = UserPollAnswer(answerId, geo)
      val actorRef = system.actorOf(Props(new SpokManager(userId, eventLog) {
        val mockedDSEUserSpokFactoryApi: DSEUserSpokFactoryApi = mock[DSEUserSpokFactoryApi]

        override def createActor(spokId: String): SpokActor = {
          new SpokActor(spokId, Some(spokId), eventLog) {
            override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
            when(mockedDSEUserSpokFactoryApi.validatePollQuestionAndFetchSpokId(questionId)) thenReturn None
          }
        }
      }))
      actorRef ! SavePollAnswer(questionId, userId, userPollAnswer)
      expectMsgType[PollAnswerSavedFailure](30 seconds)
    }

    "Able to send error message if answer id is invalid when answering a poll question" in {

      val questionId = UUID.randomUUID().toString
      val pollSpokId = UUID.randomUUID().toString
      val answerId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val geo = Geo(13.67, 14.56, 33.3)
      val userPollAnswer = UserPollAnswer(answerId, geo)
      val actorRef = system.actorOf(Props(new SpokManager(userId, eventLog) {
        val mockedDSEUserSpokFactoryApi: DSEUserSpokFactoryApi = mock[DSEUserSpokFactoryApi]
        val mockedDSESpokFactoryApi: DSESpokApi = mock[DSESpokApi]

        override def createActor(spokId: String): SpokActor = {
          new SpokActor(spokId, Some(spokId), eventLog) {
            override val dseUserSpokFactoryApi = mockedDSEUserSpokFactoryApi
            when(mockedDSEUserSpokFactoryApi.validatePollQuestionAndFetchSpokId(questionId)) thenReturn (Some(pollSpokId))
            when(mockedDSESpokFactoryApi.addAnswerToAPoll(questionId, pollSpokId, userId, userPollAnswer)) thenReturn Some(Error(s"Invalid answer to question $questionId.", SPK_010))
          }
        }
      }))
      actorRef ! SavePollAnswer(questionId, userId, userPollAnswer)
      expectMsgType[PollAnswerSavedFailure](30 seconds)
    }

    "Able to disable a spok " in {

      val disbleSpokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val launchedTime = System.currentTimeMillis()
      val geo = Geo(45.00, 45.00, 45.00)
      val actorRef = system.actorOf(Props(new SpokManager(userId, eventLog) {
        val mockedDSESpokFactoryApi: DSESpokApi = mock[DSESpokApi]

        override def createActor(spokId: String): SpokActor = {
          new SpokActor(spokId, Some(spokId), eventLog) {
            override val dseSpokFactoryApi: DSESpokApi = mockedDSESpokFactoryApi
            when(dseSpokFactoryApi.disableSpok(disbleSpokId, userId, geo)) thenReturn SPOK_DISABLED
          }
        }
      }))
      actorRef ! Disable(disbleSpokId, userId, launchedTime, geo)
      expectMsgType[DisableSpokSuccess](30 seconds)
    }

    "able to remove a spok from wall " in {

      val spokInstanceId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val launchedTime = System.currentTimeMillis()
      val geo = Geo(45.00, 45.00, 45.00)
      val actorRef = system.actorOf(Props(new SpokManager(userId, eventLog) {
        val mockedDSESpokFactoryApi: DSESpokApi = mock[DSESpokApi]
        override def createActor(spokId: String): SpokActor = {
          new SpokActor(spokId, Some(spokId), eventLog) {
            override val dseSpokFactoryApi: DSESpokApi = mockedDSESpokFactoryApi
            when(dseSpokFactoryApi.removeSpokFromWall(spokInstanceId, userId, launchedTime, geo)) thenReturn RemoveSpokResponse(Some(spokInstanceId))

          }
        }
      }))
      actorRef ! RemoveSpok(spokInstanceId, userId, launchedTime, geo)
      expectMsgType[RemoveWallSpokSuccess](30 seconds)
    }

    "able to send success message if all poll's questions are successfully answered" in {
      val questionId = UUID.randomUUID().toString
      val pollSpokId = UUID.randomUUID().toString
      val answerId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val geo = Geo(13.67, 14.56, 33.3)
      val userPollAnswer = UserPollAnswer(answerId, geo)
      val allAnswers: AllAnswers = AllAnswers(spokId, List(OneAnswer(questionId, answerId)), geo)
      val actorRef = system.actorOf(Props(new SpokManager(userId, eventLog) {
        val mockedDSESpokFactoryApi: DSESpokApi = mock[DSESpokApi]
        override def createActor(spokId: String): SpokActor = {
          new SpokActor(spokId, Some(spokId), eventLog) {
            override val dseSpokFactoryApi = mockedDSESpokFactoryApi
            when(mockedDSESpokFactoryApi.validateAbsoluteSpokById(allAnswers.spokId)) thenReturn (SPOK_VALID)
            when(mockedDSESpokFactoryApi.addAllAnswersToAPoll(userId, allAnswers)) thenReturn ((None, Nil))
          }
        }
      }))
      actorRef ! SaveAllPollAnswers(userId, allAnswers)
      expectMsgType[PollAllAnswersSavedSuccess](30 seconds)
    }

    "able to send failure message if all poll's questions are not successfully answered" in {
      val questionId = UUID.randomUUID().toString
      val pollSpokId = UUID.randomUUID().toString
      val answerId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val spokId = UUID.randomUUID().toString
      val geo = Geo(13.67, 14.56, 33.3)
      val userPollAnswer = UserPollAnswer(answerId, geo)
      val allAnswers: AllAnswers = AllAnswers(spokId, List(OneAnswer(questionId, answerId)), geo)
      val actorRef = system.actorOf(Props(new SpokManager(userId, eventLog) {
        val mockedDSESpokFactoryApi: DSESpokApi = mock[DSESpokApi]
        override def createActor(spokId: String): SpokActor = {
          new SpokActor(spokId, Some(spokId), eventLog) {
            override val dseSpokFactoryApi = mockedDSESpokFactoryApi
            when(mockedDSESpokFactoryApi.validateAbsoluteSpokById(allAnswers.spokId)) thenReturn (SPOK_VALID)
            when(mockedDSESpokFactoryApi.addAllAnswersToAPoll(userId, allAnswers)) thenReturn
              ((Some(Error(SPK_135, s"Spok ${allAnswers.spokId} already completed.")), Nil))
          }
        }
      }))
      actorRef ! SaveAllPollAnswers(userId, allAnswers)
      expectMsgType[PollAllAnswersSavedFailure](30 seconds)
    }
  }
}
