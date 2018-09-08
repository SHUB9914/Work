package com.spok.messaging.routes

import akka.actor.ActorRef
import akka.http.scaladsl.model.Multipart.BodyPart
import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import com.spok.messaging.handler.MessagingRestServiceHandler
import com.spok.model.Messaging.MessageJsonData
import com.spok.util.Constant._
import com.spok.util.HttpUtil
import net.liftweb.json.JsonParser.ParseException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

trait MessagingRestService extends HttpUtil with MessagingRestServiceHandler {

  // ==============================
  //     REST ROUTES
  // ==============================

  def sampleRoutes: Route = path(PING) {
    logDuration(complete("pong"))
  }

  /**
   * This route will create spok.
   *
   * @param command
   * @return
   */
  def sendMessage(command: ActorRef): Route = path("talk" / Segment) {
    friendUserId =>
      sendMessageResponseHandler(command, friendUserId)
  }

  /**
   * This method will handle send message response.
   *
   * @param command
   * @return
   */
  private def sendMessageResponseHandler(command: ActorRef, friendUserId: String) = {
    (post & entity(as[Multipart.FormData])) { formData =>
      parameters('userId, 'phone_number) { (userId, phoneNumber) =>
        val extractedData: Future[Map[String, Any]] = formData.parts.mapAsync[(String, Any)](1) {
          case data: BodyPart => data.toStrict(5.seconds).map { strict =>
            data.name -> strict.entity.data.utf8String
          }
        }.runFold(Map.empty[String, Any])((map, tuple) => map + tuple)
        complete(
          extractedData.flatMap { data => extractSendMessageData(command, data, userId, friendUserId)
          }.recover {
            case ex: ParseException =>
              HttpResponse(StatusCodes.BadRequest, entity = HttpEntity(
                ContentTypes.`application/json`,
                write(sendFormattedError(SYST_503, SERVICE_UNAVAILABLE, Some(TALK)))
              ))
            case ex: Exception =>
              info(s"Error in processing multipart form data due to ${ex.getMessage}")
              HttpResponse(StatusCodes.BadRequest, entity = HttpEntity(
                ContentTypes.`application/json`,
                write(sendFormattedError(MSG_103, UNABLE_SEND_MESSAGE, Some(TALK)))
              ))
          }
        )
      }
    }
  }

  /**
   * This method will use to extract data from http request.
   *
   * @param command
   * @param data
   * @param userId
   * @return
   */
  private def extractSendMessageData(command: ActorRef, data: Map[String, Any], userId: String, friendUserId: String): Future[HttpResponse] = {
    val dataValue = data.get("data").getOrElse("").toString
    val jsonData = parse(dataValue).extractOpt[MessageJsonData]
    if (jsonData.isDefined) {
      toHandleMessage(command, userId, jsonData.get, friendUserId)
    } else {
      Future(HttpResponse(StatusCodes.BadRequest, entity = HttpEntity(
        ContentTypes.`application/json`,
        write(sendJsonErrorWithEmptyData(Some(TALK)))
      )))
    }
  }

  /**
   * Route to view all the talks of the Spoker.
   * [pos] is optional
   *
   * @param query
   * @return
   */
  def getTalks(query: ActorRef): Route = get {
    pathPrefix(TALKS) {
      pathEnd {
        parameters('userId, 'phone_number) { (userId, phoneNumber) =>
          val result: Future[HttpResponse] = viewTalks(query, None, userId).map(handleResponseWithEntity(_))
          logDuration(complete(result))
        }
      } ~
        path(Segment) { (pos) =>
          {
            parameters('userId, 'phone_number) { (userId, phoneNumber) =>
              val result: Future[HttpResponse] = viewTalks(query, Some(pos), userId).map(handleResponseWithEntity(_))
              logDuration(complete(result))
            }
          }
        }
    }
  }

  /**
   * Route to view all messages of a single route.
   * [pos] is optional
   *
   * @param query
   * @return
   */
  def getSingleTalk(query: ActorRef): Route = get {
    pathPrefix(TALK / Segment) { targetUserId =>
      pathEnd {
        parameters('order, 'userId, 'phone_number) { (order, userId, phoneNumber) =>
          singleTalkHandler(query, targetUserId, None, userId, order)
        }
      } ~
        path(Segment) { (messageId) =>
          {
            parameters('order, 'userId, 'phone_number) { (order, userId, phoneNumber) =>
              singleTalkHandler(query, targetUserId, Some(messageId), userId, order)
            }
          }
        }
    }
  }

  /**
   *  This method will handle request to get single talk.
   *
   * @param query
   * @param targetUserId
   * @param messageId
   * @param userId
   * @param order
   * @return
   */
  private def singleTalkHandler(query: ActorRef, targetUserId: String, messageId: Option[String], userId: String, order: String) = {
    if (order.equals("asc")) {
      val result: Future[HttpResponse] = viewSingleTalk(query, targetUserId, messageId, userId, order).map(handleResponseWithEntity(_))
      logDuration(complete(result))
    } else {
      val result: Future[HttpResponse] = viewSingleTalk(query, targetUserId, messageId, userId, "desc").map(handleResponseWithEntity(_))
      logDuration(complete(result))
    }
  }

  /**
   * Route to search message as full text search
   *
   * @param query
   * @return
   */
  def searchByMessage(query: ActorRef): Route = get {
    path(SEARCHTALKS / SEARCHMSG) {
      parameters('msg, 'userId, 'phone_number) { (msg, userId, phoneNumber) =>
        val result: Future[HttpResponse] = getByMessage(query, msg, userId).map(handleResponseWithEntity(_))
        logDuration(complete(result))
      }
    }
  }

  /**
   * Route to search Talkers
   *
   * @param query
   * @return
   */
  def searchByTalker(query: ActorRef): Route = get {
    path(SEARCHTALKS / SEARCHTALKER) {
      parameters('talkers, 'userId, 'phone_number) { (talkers, userId, phoneNumber) =>
        val result: Future[HttpResponse] = getByTalkers(query, talkers).map(handleResponseWithEntity(_))
        logDuration(complete(result))
      }
    }
  }

  def messages(command: ActorRef, query: ActorRef): Route = pathSingleSlash {
    parameters('userId, 'phone_number) { (userId, phoneNumber) =>
      logDuration(handleWebSocketMessages(detectRequestAndPerform(command, query, userId, phoneNumber)))
    }
  }

  def routes(query: ActorRef, command: ActorRef): Route = messages(command, query) ~ sampleRoutes ~ sendMessage(command) ~
    getTalks(query) ~ getSingleTalk(query) ~ searchByMessage(query) ~ searchByTalker(query)

}
