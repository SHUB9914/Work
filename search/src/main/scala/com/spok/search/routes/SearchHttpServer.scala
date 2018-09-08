package com.spok.search.routes

import akka.actor.{ ActorSystem, Props }
import akka.http.scaladsl.Http
import akka.routing.RoundRobinPool
import akka.stream.ActorMaterializer
import com.spok.persistence.cassandra.CassandraSearchProvider
import com.spok.search.service.SearchView
import com.spok.util.ConfigUtil._
import org.fusesource.jansi.Ansi.Color._
import org.fusesource.jansi.Ansi._

import scala.util.{ Failure, Success }

object SearchHttpServer extends App with SearchRestService {

  implicit val system = ActorSystem("search")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  CassandraSearchProvider

  val concurrentActor = Runtime.getRuntime.availableProcessors() * concurrencyLevel
  val view = system.actorOf(RoundRobinPool(concurrentActor).props(Props(new SearchView)))

  val binding = Http().bindAndHandle(routes(view), hostPoint, port)

  //scalastyle:off
  binding.onComplete {
    case Success(binding) ⇒
      val localAddress = binding.localAddress
      println(ansi().fg(GREEN).a(
        """
            ___   ___     _     ___    ___   _  _
           / __| | __|   /_\   | _ \  / __| | || |
           \__ \ | _|   / _ \  |   / | (__  | __ |
           |___/ |___| /_/ \_\ |_|_\  \___| |_||_|

        """
      ).reset())
      //scalastyle:on

      info(s"Server is listening on ${localAddress.getHostName}:${localAddress.getPort}")
    case Failure(e) ⇒
      logger.info(s"Binding failed with ${e.getMessage}")
      system.terminate()
  }
}
