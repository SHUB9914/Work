package com.spok.notification.routes

import akka.actor.{ ActorSystem, Props }
import akka.http.scaladsl.Http
import akka.routing.RoundRobinPool
import akka.stream.ActorMaterializer
import com.rbmhtechnology.eventuate.ReplicationEndpoint
import com.rbmhtechnology.eventuate.ReplicationEndpoint._
import com.rbmhtechnology.eventuate.log.cassandra.CassandraEventLog
import com.spok.notification.service.{ NotificationManager, NotificationView }
import com.spok.persistence.cassandra.CassandraProvider
import com.spok.util.ConfigUtil._
import org.fusesource.jansi.Ansi.Color._
import org.fusesource.jansi.Ansi._

import scala.util.{ Failure, Success }

// =========================
// NOTIFICATION STARTER OBJECT
// =========================

object NotificationHttpServer extends App with NotificationRestService {

  implicit val system: ActorSystem = ActorSystem("notification")
  implicit val ec = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  //Init cassandra session to store event
  CassandraProvider

  val endpoint: ReplicationEndpoint = {
    ReplicationEndpoint(id => CassandraEventLog.props(id))(system)
  }

  endpoint.activate()

  // Initialise event log
  val eventLog = endpoint.logs(DefaultLogName)

  val concurrency = Runtime.getRuntime.availableProcessors() * concurrencyLevel
  // Init Notification Manager
  val manager = system.actorOf(RoundRobinPool(concurrency).props(Props(new NotificationManager(endpoint.id, eventLog))))

  // Init Notification View
  val view = system.actorOf(RoundRobinPool(concurrency).props(Props(new NotificationView(endpoint.id, eventLog))))

  val binding = Http().bindAndHandle(routes(manager, view), hostPoint, port)
  //scalastyle:off
  binding.onComplete {
    case Success(binding) ⇒
      val localAddress = binding.localAddress
      println(ansi().fg(GREEN).a(
        """
  _  _    ___    _____   ___   ___   ___    ___     _     _____   ___    ___    _  _
 | \| |  / _ \  |_   _| |_ _| | __| |_ _|  / __|   /_\   |_   _| |_ _|  / _ \  | \| |
 | .` | | (_) |   | |    | |  | _|   | |  | (__   / _ \    | |    | |  | (_) | | .` |
 |_|\_|  \___/    |_|   |___| |_|   |___|  \___| /_/ \_\   |_|   |___|  \___/  |_|\_|
      """
      ).reset())
      info(s"Server is listening on ${localAddress.getHostName}:${localAddress.getPort}")
    case Failure(e) ⇒
      info(s"Binding failed with ${e.getMessage}")
      system.terminate()

    //scalastyle:on
  }

}
