package com.spok.actors

import akka.actor.{ Actor, ActorLogging }
import com.spok.messages.Spok.PopularSpoker
import com.spok.services.SearchService

class PopularSpokerActor(searchService: SearchService) extends Actor with ActorLogging {

  override def receive: Receive = {
    case PopularSpoker => {
      log.info("<<<<Hourly popular spoker batch is Started>>>>")
      searchService.triggerApiToGetPopularSpoker
    }
  }

}
