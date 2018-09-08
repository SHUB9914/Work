package com.spok.actors

import akka.actor.{ Actor, ActorLogging }
import com.spok.messages.Spok.LastSpok
import com.spok.services.SearchService

class LastSpokActor(searchService: SearchService) extends Actor with ActorLogging {

  override def receive: Receive = {
    case LastSpok => {
      log.info("<<<<Last spok batch is Started>>>>")
      searchService.triggerApiToGetLastSpok
    }
  }

}
