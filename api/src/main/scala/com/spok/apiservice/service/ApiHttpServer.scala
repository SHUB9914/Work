package com.spok.apiservice.service

import com.spok.util.ConfigUtil._
import com.typesafe.scalalogging.LazyLogging
import org.fusesource.jansi.Ansi.Color._
import org.fusesource.jansi.Ansi._
import scala.util.{ Failure, Success }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ AuthorizationFailedRejection, RejectionHandler }
import com.spok.util.Constant._
import com.spok.util.{ JsonHelper, ResponseUtil }

// =========================
// API STARTER OBJECT
// =========================

object ApiHttpServer extends App with LazyLogging with JsonHelper with ResponseUtil {
  import ApiStarter._
  // use system's dispatcher as ExecutionContext
  val http = Http()

  implicit def myRejectionHandler: RejectionHandler =
    RejectionHandler.newBuilder()
      .handle {
        case AuthorizationFailedRejection => complete(HttpResponse(StatusCodes.NotFound, entity = HttpEntity(
          ContentTypes.`application/json`,
          write(sendFormattedError(SYST_404, "Service does not exist.", Some("invalid")))
        )))
      }
      .handleNotFound {
        complete(HttpResponse(StatusCodes.NotFound, entity = HttpEntity(
          ContentTypes.`application/json`,
          write(sendFormattedError(SYST_404, "Service does not exist.", Some("invalid")))
        )))
      }.result()
  val apiService = new ApiService
  val binding = http.bindAndHandle(apiService.routes, hostPoint, port)

  //scalastyle:off
  binding.onComplete {
    case Success(binding) ⇒
      val localAddress = binding.localAddress
      println(ansi().fg(GREEN).a(
        """
          _____                    _____                    _____
         /\    \                  /\    \                  /\    \
        /::\    \                /::\    \                /::\    \
       /::::\    \              /::::\    \               \:::\    \
      /::::::\    \            /::::::\    \               \:::\    \
     /:::/\:::\    \          /:::/\:::\    \               \:::\    \
    /:::/__\:::\    \        /:::/__\:::\    \               \:::\    \
   /::::\   \:::\    \      /::::\   \:::\    \              /::::\    \
  /::::::\   \:::\    \    /::::::\   \:::\    \    ____    /::::::\    \
 /:::/\:::\   \:::\    \  /:::/\:::\   \:::\____\  /\   \  /:::/\:::\    \
/:::/  \:::\   \:::\____\/:::/  \:::\   \:::|    |/::\   \/:::/  \:::\____\
\::/    \:::\  /:::/    /\::/    \:::\  /:::|____|\:::\  /:::/    \::/    /
 \/____/ \:::\/:::/    /  \/_____/\:::\/:::/    /  \:::\/:::/    / \/____/
          \::::::/    /            \::::::/    /    \::::::/    /
           \::::/    /              \::::/    /      \::::/____/
           /:::/    /                \::/____/        \:::\    \
          /:::/    /                  ~~               \:::\    \
         /:::/    /                                     \:::\    \
        /:::/    /                                       \:::\____\
        \::/    /                                         \::/    /
         \/____/                                           \/____/

        """
      ).reset())
      //scalastyle:on
      val akkaTimeoutServer = config.getString("akka.http.server.idle-timeout")
      val akkaTimeoutClient = config.getString("akka.http.client.idle-timeout")
      logger.info(">>>>>>>>timeout settings of Server -=>>>>>>>>>>> " + akkaTimeoutServer)
      logger.info(">>>>>>>>timeout settings of client -=>>>>>>>>>>> " + akkaTimeoutClient)
      logger.info(s"Server is listening on ${localAddress.getHostName}:${localAddress.getPort}")
    case Failure(e) ⇒
      logger.info(s"Binding failed with ${e.getMessage}")
      system.terminate()
  }
}
