package com.spok.services.service

import java.util.UUID

import akka.actor.{ ActorSystem, Props }
import akka.testkit.{ ImplicitSender, TestKit }
import com.spok.model.SpokModel.{ PollAnswers, PollQuestions, _ }
import com.spok.persistence.factory.spokgraph.DSESpokApi
import com.spok.services.util.TestHelper
import com.spok.util.JsonHelper
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ BeforeAndAfterAll, MustMatchers, WordSpecLike }
import com.spok.services.service.SpokPerformAfterCommands._

class SpokChildActorSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
    with WordSpecLike with MustMatchers with BeforeAndAfterAll with TestHelper with MockitoSugar with JsonHelper {

  def this() = this(ActorSystem("SpokChildActorSystem"))

  "A SpokChildActor" must {
    "Perform other operations, after respoking a spok" in {
      val id = UUID.randomUUID().toString
      val spokInstanceId = UUID.randomUUID().toString
      val geo = Geo(13.67, 12.56, 31.3)
      val respok = Respok(Some("0"), Some("public"), Some("text"), geo, None)
      val mockedDSESpokFactoryApi: DSESpokApi = mock[DSESpokApi]
      val mockedSpokLogger: SpokLogger = mock[SpokLogger]
      val actorRef = system.actorOf(Props(new SpokChildActor {
        override val dseSpokFactoryApi = mockedDSESpokFactoryApi
        override val spokLogger = mockedSpokLogger
      }))

      when(mockedSpokLogger.insertRespokCreationEvent(respok, id, spokInstanceId)) thenReturn null
      actorRef ! PerformAfterRespok(respok, id, spokInstanceId, Nil, actorRef)
      expectNoMsg()
    }

    "Perform other operations, after respoking a spok when respoking in private group" in {
      val id = UUID.randomUUID().toString
      val spokInstanceId = UUID.randomUUID().toString
      val geo = Geo(13.67, 12.56, 31.3)
      val respok = Respok(Some("groupID"), Some("public"), Some("text"), geo, None)
      val mockedDSESpokFactoryApi: DSESpokApi = mock[DSESpokApi]
      val mockedSpokLogger: SpokLogger = mock[SpokLogger]
      val actorRef = system.actorOf(Props(new SpokChildActor {
        override val dseSpokFactoryApi = mockedDSESpokFactoryApi
        override val spokLogger = mockedSpokLogger
      }))

      when(mockedSpokLogger.insertRespokCreationEvent(respok, id, spokInstanceId)) thenReturn null
      when(mockedDSESpokFactoryApi.sendSMSToContactFromGroup(respok.groupId.get.toString, id, spokInstanceId)) thenReturn List()
      actorRef ! PerformAfterRespok(respok, id, spokInstanceId, Nil, actorRef)
      expectNoMsg()
    }

    "Perform other operations, after creating a poll spok" in {
      val id = UUID.randomUUID().toString
      val geo = Geo(13.67, 12.56, 31.3)
      val spokId = UUID.randomUUID().toString

      val poll = Poll("MyPoll", Some("Check Knowledge"), List(PollQuestions("How many planets are there in the Universe?", Some("text"), Some("preview"), 1, List(PollAnswers("Seven", Some("text"), Some("preview"), 1), PollAnswers("Eight", Some("text"), Some("preview"), 2), PollAnswers("Nine", Some("text"), Some("preview"), 3)))))
      val spok = Spok("poll", Some("0"), Some("public"), Some(0), Some("url spok"), None, None, None, Some(poll), None, geo, spokId, timeStamp)

      val mockedDSESpokFactoryApi: DSESpokApi = mock[DSESpokApi]
      val mockedSpokLogger: SpokLogger = mock[SpokLogger]
      val actorRef = system.actorOf(Props(new SpokChildActor {
        override val dseSpokFactoryApi = mockedDSESpokFactoryApi
        override val spokLogger = mockedSpokLogger
      }))
      actorRef ! PerformAfterSpok(id, spok, actorRef)
      expectNoMsg()
    }
  }
}
