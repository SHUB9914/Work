package com.spok.accountsservice.routes

import akka.actor.{ ActorSystem, Props }
import akka.http.scaladsl.Http
import akka.routing.RoundRobinPool
import akka.stream.ActorMaterializer
import com.rbmhtechnology.eventuate.ReplicationEndpoint
import com.rbmhtechnology.eventuate.ReplicationEndpoint._
import com.rbmhtechnology.eventuate.log.cassandra.CassandraEventLog
import com.spok.accountsservice.service.AccountManagerCommands.ClearExpiredOtpToken
import com.spok.accountsservice.service.{ AccountManager, AccountView }
import com.spok.persistence.cassandra.{ CassandraMessageProvider, CassandraProvider }
import com.spok.util.ConfigUtil._
import com.typesafe.config.ConfigFactory
import org.fusesource.jansi.Ansi.Color._
import org.fusesource.jansi.Ansi._
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

/**
 * Accounts service
 */

object AccountsHttpServer extends App with AccountRestService {

  implicit val system: ActorSystem = ActorSystem("accounts")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  import system.dispatcher

  //Init cassandra session to store event
  CassandraProvider
  CassandraMessageProvider

  val endpoint: ReplicationEndpoint = {
    ReplicationEndpoint(id => CassandraEventLog.props(id))(system)
  }
  endpoint.activate()

  // Initialise event log
  val eventLog = endpoint.logs(DefaultLogName)

  val concurrency = Runtime.getRuntime.availableProcessors() * concurrencyLevel
  // Init Spok Manager
  val manager = system.actorOf(RoundRobinPool(concurrency).props(Props(new AccountManager(endpoint.id, eventLog))))

  // Init Spok View
  val view = system.actorOf(RoundRobinPool(concurrency).props(Props(new AccountView(endpoint.id, eventLog))))

  val resetTokenDuration = ConfigFactory.load.getInt("app.scheduler.CLEAR_EXPIRED_OTP_TOKEN_SCHEDULE")

  //Clear expired tokens
  system.scheduler.schedule(0 second, resetTokenDuration seconds, manager, ClearExpiredOtpToken)

  val binding = Http().bindAndHandle(routes(manager, view), hostPoint, port)

  //scalastyle:off
  binding.onComplete {
    case Success(binding) ⇒
      val localAddress = binding.localAddress
      println(ansi().fg(GREEN).a(
        """
              _      ___    ___    ___    _   _   _  _   _____   ___
             /_\    / __|  / __|  / _ \  | | | | | \| | |_   _| / __|
            / _ \  | (__  | (__  | (_) | | |_| | | .` |   | |   \__ \
           /_/ \_\  \___|  \___|  \___/   \___/  |_|\_|   |_|   |___/

        """
      ).reset())
      //scalastyle:on

      info(s"Server is listening on ${localAddress.getHostName}:${localAddress.getPort}")
    case Failure(e) ⇒
      logger.info(s"Binding failed with ${e.getMessage}")
      system.terminate()
  }
}
