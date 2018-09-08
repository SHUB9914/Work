package com.spok.apiservice.service

import akka.Done
import akka.actor.{ Actor, ActorRef }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.TextMessage.Strict
import akka.http.scaladsl.model.ws.{ Message, WebSocketRequest, WebSocketUpgradeResponse }
import akka.stream.scaladsl.{ Flow, Keep, Sink, Source, SourceQueueWithComplete }
import com.spok.apiservice.handler.{ AccountServiceHandler, ApiServiceHandler, MessagingServiceHandler, SpokServiceHandler }
import com.spok.model.Messaging.UserTypingResponse
import com.spok.model.SpokModel.StandardResponseForCaseClass
import com.spok.util.ConfigUtil._
import com.spok.util.Constant._

import scala.concurrent.{ Future, Promise }

case class ConnectService(actorRef: ActorRef, userId: Option[String], phoneNumber: Option[String],
  source: Source[Message, SourceQueueWithComplete[Message]],
  notificationSource: Source[Message, SourceQueueWithComplete[Message]],
  accountSource: Source[Message, SourceQueueWithComplete[Message]],
  messagingSource: Source[Message, SourceQueueWithComplete[Message]])

case class DisconnectService(phoneNumber: Option[String])

/**
 * This class is used to create web socket connection
 * with Account, Notification and Spok service
 */

class ApiConnector extends Actor with ApiServiceHandler with SpokServiceHandler with AccountServiceHandler with MessagingServiceHandler {
  import ApiStarter._
  //scalastyle:off
  var spokPromise: Option[Promise[Option[Message]]] = None
  var notificationPromise: Option[Promise[Option[Message]]] = None
  var accountPromise: Option[Promise[Option[Message]]] = None
  var messagingPromise: Option[Promise[Option[Message]]] = None
  //scalastyle:on

  def receive: PartialFunction[Any, Unit] = {

    /**
     * Disconnect all services, if user logged out
     */
    case DisconnectService(phoneNumber) => {
      logger.info(s" ${phoneNumber} is disconnected. So disconnecting all connection for this user.")
      disconnectingService
    }

    /**
     * Connect all services, if user logged in
     */
    case ConnectService(actor, userId, phoneNumber, source, notificationSource, accountSource, messagingSource) => {
      connectAccountService(actor, userId, phoneNumber, accountSource)
      (userId, phoneNumber) match {
        case (Some(uId), Some(pNumber)) => {
          connectSpokService(actor, uId, pNumber, source)
          connectNotificationService(actor, uId, pNumber, notificationSource)
          connectMessagingService(actor, uId, pNumber, messagingSource)
        }
        case _ => // Do Nothing
      }
    }

    case _ => //Do Nothing
  }

  private def disconnectingService: Unit = {
    disconnectPromise(notificationPromise)
    disconnectPromise(accountPromise)
    disconnectPromise(spokPromise)
    disconnectPromise(messagingPromise)
  }

  def disconnectPromise(promise: Option[Promise[Option[Message]]]): Unit = {
    if (promise.isDefined) {
      if (!promise.get.isCompleted) promise.get.success(None)
    }
  }

  private def connectAccountService(actor: ActorRef, userId: Option[String], phoneNumber: Option[String],
    source: Source[Message, SourceQueueWithComplete[Message]]): Unit = {
    val sink: Sink[Message, Future[Done]] = handleSink(actor, userId, phoneNumber, checkAccountResponseAndTakeAction)
    val flow: Flow[Message, Message, Promise[Option[Message]]] = createFlow(source, sink)

    val ws: WebSocketRequest = (userId, phoneNumber) match {
      case (Some(uId), Some(pNumber)) => WebSocketRequest(WS + lookupService(accountServiceName) + ":" + accountPort + DELIMITER +
        "?userId=" + uId + "&phone_number=" + pNumber)
      case _ => WebSocketRequest(WS + lookupService(accountServiceName) + ":" + accountPort + DELIMITER + REGISTER)
    }

    val (accountConnectionResponse, accountPromiseC) =
      Http().singleWebSocketRequest(ws, flow)
    accountPromise = Some(accountPromiseC)
    val connected = checkConnection(phoneNumber, accountConnectionResponse)
    connected.onComplete(res => logger.info(s"Account microservice is connected for ${phoneNumber}  ${res}"))
  }

  private def connectSpokService(actor: ActorRef, userId: String, phoneNumber: String,
    source: Source[Message, SourceQueueWithComplete[Message]]): Unit = {
    val sink: Sink[Message, Future[Done]] = handleSink(actor, Some(userId), Some(phoneNumber), checkResponseAndTakeAction)
    val flow: Flow[Message, Message, Promise[Option[Message]]] = createFlow(source, sink)
    val ws: WebSocketRequest =
      WebSocketRequest(WS + lookupService(spokServiceName) + ":" + spokPort + DELIMITER + SPOKS +
        "?userId=" + userId + "&phone_number=" + phoneNumber)
    val (spokConnectionResponse, spokPromiseC) =
      Http().singleWebSocketRequest(ws, flow)
    spokPromise = Some(spokPromiseC)
    val connected = checkConnection(Some(phoneNumber), spokConnectionResponse)
    connected.onComplete(res => logger.info(s"Spok microservice is connected for ${phoneNumber}  ${res}"))
  }

  private def connectNotificationService(actor: ActorRef, userId: String, phoneNumber: String,
    source: Source[Message, SourceQueueWithComplete[Message]]): Unit = {
    val sink: Sink[Message, Future[Done]] = createNotificationSink(actor, userId, phoneNumber)
    val flow: Flow[Message, Message, Promise[Option[Message]]] = createFlow(source, sink)
    val ws: WebSocketRequest =
      WebSocketRequest(WS + lookupService(notificationServiceName) + ":" + notificationPort + DELIMITER +
        "?userId=" + userId + "&phone_number=" + phoneNumber)
    val (notificationConnectionResponse, notificationPromiseC) =
      Http().singleWebSocketRequest(ws, flow)
    notificationPromise = Some(notificationPromiseC)
    val connected = checkConnection(Some(phoneNumber), notificationConnectionResponse)
    connected.onComplete(res => logger.info(s"Notification microservice is connected for ${phoneNumber}  ${res}"))
  }

  private def connectMessagingService(actor: ActorRef, userId: String, phoneNumber: String,
    source: Source[Message, SourceQueueWithComplete[Message]]): Unit = {
    val sink: Sink[Message, Future[Done]] = createMessagingSink(actor, Some(userId), Some(phoneNumber), checkMessagingResponseAndTakeAction)
    val flow: Flow[Message, Message, Promise[Option[Message]]] = createFlow(source, sink)
    val ws: WebSocketRequest =
      WebSocketRequest(WS + lookupService(messagingServiceName) + ":" + messagingPort + DELIMITER +
        "?userId=" + userId + "&phone_number=" + phoneNumber)
    val (messagingConnectionResponse, messagingPromiseC) =
      Http().singleWebSocketRequest(ws, flow)
    messagingPromise = Some(messagingPromiseC)
    val connected = checkConnection(Some(phoneNumber), messagingConnectionResponse)
    connected.onComplete(res => logger.info(s"Messaging microservice is connected for ${phoneNumber}  ${res}"))
  }

  private def checkConnection(phoneNumber: Option[String], spokConnectionResponse: Future[WebSocketUpgradeResponse]) = {
    spokConnectionResponse.map { upgrade =>
      // just like a regular http request we can get 404 NotFound,
      // with a response body, that will be available from upgrade.response
      if (upgrade.response.status == StatusCodes.SwitchingProtocols || upgrade.response.status == StatusCodes.OK) {
        Done
      } else {
        throw new RuntimeException(s"Connection failed for ${phoneNumber}: ${upgrade.response.status}")
      }
    }
  }

  private def createFlow(
    source: Source[Message, SourceQueueWithComplete[Message]],
    sink: Sink[Message, Future[Done]]
  ): Flow[Message, Message, Promise[Option[Message]]] = {
    Flow.fromSinkAndSourceMat(sink, source.mergeMat(Source.maybe[Message])(Keep.right))(Keep.right)
  }

  private def handleSink(actor: ActorRef, userId: Option[String], phoneNumber: Option[String],
    handler: (String, Option[String], Option[String]) => Boolean): Sink[Message, Future[Done]] = {
    Sink.foreach {
      case message: Strict => {
        actor ! message.text
        handler(message.text, userId, phoneNumber)
      }
      case any => logger.info(s"Received unknown message format for ${phoneNumber}" + any)
    }
  }

  private def createNotificationSink(actor: ActorRef, userId: String, phoneNumber: String): Sink[Message, Future[Done]] = {
    Sink.foreach {
      case message: Strict => {
        logger.info(s"${phoneNumber}: Got Notification Response and Now taking further action !!!!!" + message)
        actor ! message.text
      }
      case any => logger.info(s"Received unknown message format for ${phoneNumber}" + any)
    }
  }

  private def createMessagingSink(actor: ActorRef, userId: Option[String], phoneNumber: Option[String],
    handler: (String, String) => Boolean): Sink[Message, Future[Done]] = {
    Sink.foreach {
      case message: Strict => {
        val standardResponseOpt = parse(message.text).extractOpt[StandardResponseForCaseClass]
        (standardResponseOpt.get.resource, standardResponseOpt.get.status) match {
          case (Some(TYPING), SUCCESS) => //DO NOTHING
          case _ => actor ! message.text
        }
        handler(message.text, userId.get)
      }
      case any => logger.info(s"Received unknown message format for ${phoneNumber}" + any)
    }
  }
}
