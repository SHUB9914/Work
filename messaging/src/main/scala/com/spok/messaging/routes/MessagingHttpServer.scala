package com.spok.messaging.routes

import akka.actor.{ ActorSystem, Props }
import akka.http.scaladsl.Http
import akka.routing.RoundRobinPool
import akka.stream.ActorMaterializer
import com.spok.messaging.service.{ MessagingManager, MessagingView }
import com.spok.persistence.cassandra.CassandraMessageProvider
import com.spok.util.ConfigUtil._
import org.fusesource.jansi.Ansi.Color._
import org.fusesource.jansi.Ansi._

import scala.util.{ Failure, Success }

object MessagingHttpServer extends App with MessagingRestService {

  implicit val system = ActorSystem("messaging")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  CassandraMessageProvider

  val concurrentActor = Runtime.getRuntime.availableProcessors() * concurrencyLevel
  val view = system.actorOf(RoundRobinPool(concurrentActor).props(Props(new MessagingView)))
  val command = system.actorOf(RoundRobinPool(concurrentActor).props(Props(new MessagingManager)))

  val binding = Http().bindAndHandle(routes(view, command), hostPoint, port)

  //scalastyle:off
  binding.onComplete {
    case Success(binding) ⇒
      val localAddress = binding.localAddress
      println(ansi().fg(GREEN).a(
        """
            __  __   ___   ___   ___     _      ___   ___   _  _    ___
           |  \/  | | __| / __| / __|   /_\    / __| |_ _| | \| |  / __|
           | |\/| | | _|  \__ \ \__ \  / _ \  | (_ |  | |  | .` | | (_ |
           |_|  |_| |___| |___/ |___/ /_/ \_\  \___| |___| |_|\_|  \___|

        """
      ).reset())
      //scalastyle:on

      info(s"Server is listening on ${localAddress.getHostName}:${localAddress.getPort}")
    case Failure(e) ⇒
      logger.info(s"Binding failed with ${e.getMessage}")
      system.terminate()
  }

}
