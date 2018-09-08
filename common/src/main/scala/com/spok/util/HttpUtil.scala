package com.spok.util

import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpResponse }
import akka.http.scaladsl.server._
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s.native

/**
 * This object provides utility for HTTP routes
 */
trait HttpUtil extends Directives with Json4sSupport with LoggerUtil {

  implicit val serialization = native.Serialization

  val rejectionHandler = RejectionHandler.default

  /**
   * To log detailed of a route
   *
   * @example 10:52:43.261 [spok-akka.actor.default-dispatcher-4] INFO com.spok.services.SpokService - [101] GET http://localhost:9000/greeter took: 68ms
   * @param inner route
   * @return route with log
   */
  def logDuration(inner: Route): Route = { ctx =>
    val start = System.currentTimeMillis()
    // handling rejections here so that we get proper status codes
    val innerRejectionsHandled = handleRejections(rejectionHandler)(inner)
    mapResponse { resp =>
      val d = System.currentTimeMillis() - start
      info(s"[${resp.status.intValue()}] ${ctx.request.method.name} ${ctx.request.uri} took: ${d}ms")
      resp
    }(innerRejectionsHandled)(ctx)
  }

  def handleResponseWithEntity(response: String): HttpResponse = {
    HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, response))
  }

}
