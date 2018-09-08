package com.spok.actors

import akka.actor.{ ActorRef, ActorSystem, Props }
import com.spok.configuration.Configuration._
import com.spok.services.SearchService
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ BeforeAndAfterAll, MustMatchers, WordSpecLike }

class LocalActorRefFactorySpec extends WordSpecLike with MustMatchers with MockitoSugar with BeforeAndAfterAll with LocalActorRefFactory {

  override val actorSystem: ActorSystem = ActorSystem("LocalActorRefTestSystem")
  override val searchService: SearchService = mock[SearchService]
  override val actors: Map[String, ActorRef] = Map(
    TRENDY_SPOK_ACTOR -> actorSystem.actorOf(Props(classOf[TrendySpokActor], searchService), TRENDY_SPOK_ACTOR)
  )

  override def afterAll() {
    actorSystem.terminate()
  }

  "A LocalActorRefFactorySpec" must {

    "be able to get Receiver actor from actor map " in {
      val result = getReceiver(TRENDY_SPOK_ACTOR)
      assert(result.path.name == """trendy_spok_actor""")
    }

    "be able to get actor System" in {
      val result = system
      assert(result.name == """LocalActorRefTestSystem""")
    }

    "not be able to get Receiver actor from actor map when actor name is wrong " in {
      val result = try {
        getReceiver("spok_actor")
      } catch {
        case ex: IllegalArgumentException => ex.getMessage
      }
      assert(result == "No Actor could be looked up for the specified name spok_actor")
    }
  }
}
