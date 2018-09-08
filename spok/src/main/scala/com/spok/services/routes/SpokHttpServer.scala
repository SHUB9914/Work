package com.spok.services.routes

import akka.actor.{ ActorSystem, Props }
import akka.http.scaladsl.Http
import akka.routing.RoundRobinPool
import akka.stream.ActorMaterializer
import com.rbmhtechnology.eventuate.ReplicationEndpoint
import com.rbmhtechnology.eventuate.ReplicationEndpoint._
import com.rbmhtechnology.eventuate.log.cassandra.CassandraEventLog
import com.spok.persistence.cassandra.CassandraProvider
import com.spok.services.service.SpokManagerCommands.FillRedisWithSubscriberDetails
import com.spok.services.service.{ SpokManager, SpokView }
import com.spok.util.ConfigUtil._
import org.fusesource.jansi.Ansi.Color._
import org.fusesource.jansi.Ansi._

import scala.concurrent.duration._
import scala.util.{ Failure, Success }

// =========================
// SPOK STARTER OBJECT
// =========================

object SpokHttpServer extends App with SpokRestService {
  implicit val system: ActorSystem = ActorSystem("spok")

  import system.dispatcher
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

  // Init Spok Manager
  val manager = system.actorOf(RoundRobinPool(concurrency).props(Props(new SpokManager(endpoint.id, eventLog))))

  // Init Spok View
  val view = system.actorOf(RoundRobinPool(concurrency).props(Props(new SpokView(endpoint.id, eventLog))))

  val binding = Http().bindAndHandle(routes(manager, view), hostPoint, port)

  //Fill redis with subscriber details on application start up
  system.scheduler.scheduleOnce(0 second, manager, FillRedisWithSubscriberDetails)

  //scalastyle:off
  binding.onComplete {
    case Success(binding) ⇒
      val localAddress = binding.localAddress
      println(ansi().fg(GREEN).a(
        """
          _____                    _____                   _______                   _____
         /\    \                  /\    \                 /::\    \                 /\    \
        /::\    \                /::\    \               /::::\    \               /::\____\
       /::::\    \              /::::\    \             /::::::\    \             /:::/    /
      /::::::\    \            /::::::\    \           /::::::::\    \           /:::/    /
     /:::/\:::\    \          /:::/\:::\    \         /:::/~~\:::\    \         /:::/    /
    /:::/__\:::\    \        /:::/__\:::\    \       /:::/    \:::\    \       /:::/____/
    \:::\   \:::\    \      /::::\   \:::\    \     /:::/    / \:::\    \     /::::\    \
  ___\:::\   \:::\    \    /::::::\   \:::\    \   /:::/____/   \:::\____\   /::::::\____\________
 /\   \:::\   \:::\    \  /:::/\:::\   \:::\____\ |:::|    |     |:::|    | /:::/\:::::::::::\    \
/::\   \:::\   \:::\____\/:::/  \:::\   \:::|    ||:::|____|     |:::|    |/:::/  |:::::::::::\____\
\:::\   \:::\   \::/    /\::/    \:::\  /:::|____| \:::\    \   /:::/    / \::/   |::|~~~|~~~~~
 \:::\   \:::\   \/____/  \/_____/\:::\/:::/    /   \:::\    \ /:::/    /   \/____|::|   |
  \:::\   \:::\    \               \::::::/    /     \:::\    /:::/    /          |::|   |
   \:::\   \:::\____\               \::::/    /       \:::\__/:::/    /           |::|   |
    \:::\  /:::/    /                \::/____/         \::::::::/    /            |::|   |
     \:::\/:::/    /                  ~~                \::::::/    /             |::|   |
      \::::::/    /                                      \::::/    /              |::|   |
       \::::/    /                                        \::/____/               \::|   |
        \::/    /                                          ~~                      \:|   |
         \/____/                                                                    \|___|
        """
      ).reset())
      //scalastyle:on

      info(s"Server is listening on ${localAddress.getHostName}:${localAddress.getPort}!!!!")
    case Failure(e) ⇒
      info(s"Binding failed with ${e.getMessage}")
      system.terminate()
  }

}
