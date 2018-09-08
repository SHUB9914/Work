package com.spok.actors

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.routing.RoundRobinPool
import com.spok.configuration.Configuration._
import com.spok.services.SearchService
import com.typesafe.config.ConfigFactory

trait LocalActorRefFactory {

  val actorSystem: ActorSystem
  val searchService: SearchService
  val actors: Map[String, ActorRef]

  def getReceiver(name: String): ActorRef = {
    actors.get(name) match {
      case None => throw new IllegalArgumentException("No Actor could be looked up for the specified name " + name)
      case Some(actorRef) => actorRef
    }
  }

  def system: ActorSystem = {
    actorSystem
  }

}

// $COVERAGE-OFF$
object LocalActorRefFactory extends LocalActorRefFactory {
  val actorSystem: ActorSystem = ActorSystem.create("SearchScheduler", ConfigFactory.load)
  val searchService: SearchService = SearchService
  val concurrency = Runtime.getRuntime.availableProcessors() * CONCURRENCY_LEVEL.toInt

  val actors: Map[String, ActorRef] = Map(
    TRENDY_SPOK_ACTOR -> actorSystem.actorOf(Props(classOf[TrendySpokActor], searchService)
      .withRouter(RoundRobinPool(concurrency)), TRENDY_SPOK_ACTOR),
    LAST_SPOK_ACTOR -> actorSystem.actorOf(Props(classOf[LastSpokActor], searchService)
      .withRouter(RoundRobinPool(concurrency)), LAST_SPOK_ACTOR),
    POPULAR_SPOKER_ACTOR -> actorSystem.actorOf(Props(classOf[PopularSpokerActor], searchService)
      .withRouter(RoundRobinPool(concurrency)), POPULAR_SPOKER_ACTOR)
  )
}
// $COVERAGE-ON$

