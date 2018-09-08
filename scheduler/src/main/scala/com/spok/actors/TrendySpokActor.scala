package com.spok.actors

import akka.actor.{ Actor, ActorLogging }
import com.spok.messages.Spok.TrendySpok
import com.spok.services.SearchService

class TrendySpokActor(searchService: SearchService) extends Actor with ActorLogging {

  override def receive: Receive = {
    case TrendySpok => {
      log.info("<<<<Trendy spok batch is Started>>>>")
      searchService.triggerApiToGetTrendySpok
    }
  }

}
