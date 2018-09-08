package com.spok.apiservice.utility

import java.util.{Locale, Optional}

import akka.http.javadsl.model
import akka.http.javadsl.server.ExpectedWebSocketRequestRejection
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{Message, TextMessage, UpgradeToWebSocket}
import akka.http.scaladsl.server.{ExpectedWebSocketRequestRejection, _}
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.spok.persistence.factory.DSEGraphPersistenceFactoryApi
import com.spok.util.Constant._
import com.spok.util.{JWTTokenHelper, JsonHelper, LoggerUtil, ResponseUtil}
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s.{DefaultFormats, Formats, native}

/**
 * This object provides utility for HTTP routes
 */
trait ApiHttpUtil extends Directives with Json4sSupport with LoggerUtil with JsonHelper with ResponseUtil {

  implicit val serialization = native.Serialization

  implicit def json4sFormats: Formats = DefaultFormats

  val rejectionHandler = RejectionHandler.default

  val dseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = DSEGraphPersistenceFactoryApi

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

  def handleWebSocketMessagesForOptionalProtocol(
    handler: ((Option[String], Option[String])) => Flow[Message, Message, Any],
    subprotocol: String
  ): Route = {
    optionalHeaderValueByType[UpgradeToWebSocket](()) {
      case Some(upgrade) ⇒ handleWebsocketRequest(handler, subprotocol, upgrade)
      case None ⇒ {
        complete(HttpResponse(StatusCodes.NotFound, entity = HttpEntity(
          ContentTypes.`application/json`,
          write(sendFormattedError(SYST_404, NOT_FOUND, Some(INVALID)))
        )))
      }
    }
  }

  private def handleWebsocketRequest(
    handler: ((Option[String], Option[String])) => Flow[Message, Message, Any],
    subprotocol: String, upgrade: UpgradeToWebSocket
  ): StandardRoute = {
    if (upgrade.requestedProtocols.isEmpty) {
      complete(upgrade.handleMessages(handler((None, None)), upgrade.requestedProtocols.headOption))
    } else {
      getValidRequest(handler, subprotocol, upgrade)
    }
  }

  private def getValidRequest(
    handler: ((Option[String], Option[String])) => Flow[Message, Message, Any],
    subprotocol: String, upgrade: UpgradeToWebSocket
  ): StandardRoute = {
    val verifiedJwtString = upgrade.requestedProtocols.find { p =>
      val (protocol, token) = p.splitAt(subprotocol.length)
      JWTTokenHelper.isValid(token)
    }
    verifiedJwtString match {
      case Some(jwtString) => {
        val (protocol, token) = jwtString.splitAt(subprotocol.length)
        val (userId: String, phoneNumber: String) = getInfoFromToken(token)
        dseGraphPersistenceFactoryApi.isValidUserId(userId) match {
          case true => complete(upgrade.handleMessages(handler((Some(userId), Some(phoneNumber))), upgrade.requestedProtocols.headOption))
          case false => complete(upgrade.handleMessages(errorResponseForWebsocket, upgrade.requestedProtocols.headOption))
        }
      }
      case None => complete(upgrade.handleMessages(errorResponseForWebsocket, upgrade.requestedProtocols.headOption))
    }
  }

  def getInfoFromToken(token: String): (String, String) = {
    val combination = JWTTokenHelper.getInfoFromJwtToken(token)
    combination match {
      case Some((userId, phoneNumber)) => (userId, phoneNumber)
      case None => ("", "")
    }
  }

  /**
   * Handles WebSocket requests with the given handler if the given subprotocol is offered in the request and
   * rejects other requests with an [[ExpectedWebSocketRequestRejection]] or an [[UnsupportedWebSocketSubprotocolRejection]].
   *
   * @param handler
   * @param subprotocol
   * @return
   */
  def handleWebSocketMessagesForProtocol(handler: ((Option[String], Option[String])) => Flow[Message, Message, Any], subprotocol: String): Route = {
    handleWebSocketMessagesForOptionalProtocol(handler, subprotocol)
  }

  def bearerAuthRequest(r: HttpRequest): (String, Boolean) = {
    try {
      val AUTHORIZATION_KEYS: List[String] = List("Authorization")
      def authorizationKey: Option[String] = AUTHORIZATION_KEYS.find(r.getHeader(_) != null)
      authorizationKey.fold("not bearer Authentication", false) { authKey =>
        if (authKey == "Authorization") {
          val parts = r.getHeader("Authorization").get().value().split(" ", 2)
          def scheme: Option[String] = parts.headOption.map(sch => sch.toLowerCase(Locale.ENGLISH))
          def token: String = parts.lastOption getOrElse ""
          (token, true)
        } else {
          ("not bearer Authentication", false)
        }
      }
    } catch {
      case ex: Exception => ("not bearer Authentication", false)
    }
  }

  def handleHttpReqWithAuth(ctx: RequestContext): ((String, String), String) = {
    val (token, isAuth) = bearerAuthRequest(ctx.request)
    isAuth match {
      case true => {
        verifyToken(token)
      }
      case false => (("", ""), token)
    }
  }

  private def verifyToken(token: String): ((String, String), String) = {
    JWTTokenHelper.isValid(token) match {
      case true =>
        val (userId: String, phoneNumber: String) = getInfoFromToken(token)
        dseGraphPersistenceFactoryApi.isValidUserId(userId) match {
          case true => ((userId, phoneNumber), "validToken")
          case false => ((userId, phoneNumber), write(sendFormattedError(SYST_401, AUTHENTICATION_REQUIRED, Some(AUTHENTICATION))))
        }
      case false => (("", ""), write(sendFormattedError(SYST_411, INVALID_JWT_TOKEN, Some(AUTHENTICATION))))

    }
  }

  def errorResponseForWebsocket: Flow[Message, Message, Any] =
    Flow[Message].collect {
      case TextMessage.Strict(tm) => tm
    }.via(errorFlowForWebsocket)
      .map {
        case msg: String => TextMessage.Strict(msg)
      }

  def errorFlowForWebsocket: Flow[String, String, Any] =
    Flow.fromSinkAndSource(Sink.ignore, Source.single(write(sendFormattedError(SYST_411, INVALID_JWT_TOKEN, Some(AUTHENTICATION)))))
}
