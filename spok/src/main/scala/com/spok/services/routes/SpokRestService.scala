package com.spok.services.routes

import java.io.File

import akka.actor.ActorRef
import akka.http.scaladsl.model.Multipart.BodyPart
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.FileIO
import com.spok.services.handler.SpokRestServiceHandler
import com.spok.util.Constant._
import net.liftweb.json.JsonParser.ParseException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

/**
 * All REST routes of Spok are defined here. The Spok Service would be responding to these routes.
 */

trait SpokRestService extends SpokRestServiceHandler {

  val secretKey = "secret"
  // ==============================
  //     REST ROUTES
  // ==============================

  def spokRoute(command: ActorRef, query: ActorRef): Route = path(SPOKS) {
    parameters('userId, 'phone_number) { (userId, phoneNumber) =>
      logDuration(handleWebSocketMessages(detectRequestAndPerform(command, query, userId, phoneNumber)))
    }
  }

  // ==============================
  //    POLL VIEW AND ANSWER ROUTES
  // ==============================
  def viewPollQuestion(query: ActorRef): Route = get {
    path(POLL / Segment) {
      questionId =>
        {
          parameters('userId, 'phone_number) { (userId, phoneNumber) =>
            val result: Future[HttpResponse] = viewPollQuestionHandler(query, questionId, userId).map(handleResponseWithEntity(_))
            logDuration(complete(result))
          }
        }
    }
  }

  /**
   * This method is use for get absolute spok  stats
   *
   * @param query
   * @return
   */
  def getSpokStats(query: ActorRef): Route = get {
    path("spok" / Segment / "stats") {
      spokId =>
        {
          parameters('userId, 'phone_number) { (userId, phoneNumber) =>
            val result: Future[HttpResponse] = spokStatsHandler(query, spokId).map(handleResponseWithEntity(_))
            logDuration(complete(result))
          }
        }
    }
  }

  /**
   * This method is used to get 10 comments of a spok.
   *
   * @param query
   * @return
   */
  def getComments(query: ActorRef): Route = get {
    path("spok" / Segment / "comments" / Segment) {
      (spokId, pos) =>
        {
          parameters('userId, 'phone_number) { (userId, phoneNumber) =>
            val result: Future[HttpResponse] = getCommentsHandler(query, spokId, pos).map(handleResponseWithEntity(_))
            logDuration(complete(result))
          }
        }
    }
  }

  /**
   * This method is use get 10 respokers of a spok.
   *
   * @param query
   * @return
   */
  def getReSpokers(query: ActorRef): Route = get {
    path("spok" / Segment / "respokers" / Segment) {
      (spokId, pos) =>
        {
          parameters('userId, 'phone_number) { (userId, phoneNumber) =>
            val result: Future[HttpResponse] = getReSpokersHandler(query, spokId, pos).map(handleResponseWithEntity(_))
            logDuration(complete(result))
          }
        }
    }
  }

  /**
   * This method is use get 10 scoped users of a spok.
   *
   * @param query
   * @return
   */
  def getScopedUsers(query: ActorRef): Route = get {
    path("spok" / Segment / "scoped" / Segment) {
      (spokId, pos) =>
        {
          parameters('userId, 'phone_number) { (userId, phoneNumber) =>
            val result: Future[HttpResponse] = scopedUsersHandler(query, spokId, pos).map(handleResponseWithEntity(_))
            logDuration(complete(result))
          }
        }
    }
  }

  /**
   * This method is use for get spoks stack
   *
   * @param query
   * @return
   */
  def getSpoksStack(query: ActorRef): Route = get {
    path("spoks" / Segment) {
      pos =>
        {
          parameters('userId, 'phone_number) { (userId, phoneNumber) =>
            val result: Future[HttpResponse] = spokStackHandler(query, pos, userId).map(handleResponseWithEntity(_))
            logDuration(complete(result))
          }
        }
    }
  }

  /**
   * This method is use get quick view of one spok.
   *
   * @param query
   * @return
   */
  def viewShortSpok(query: ActorRef): Route = get {
    pathPrefix("stack" / Segment) { (spokId) =>
      pathEnd {
        parameters('userId, 'phone_number) { (userId, phoneNumber) =>
          val result: Future[HttpResponse] = viewShortSpok(query, spokId, "", userId).map(handleResponseWithEntity(_))
          logDuration(complete(result))
        }
      } ~
        path(Segment) {
          (targetUserId) =>
            {
              parameters('userId, 'phone_number) { (userId, phoneNumber) =>
                val result: Future[HttpResponse] = viewShortSpok(query, spokId, targetUserId, userId).map(handleResponseWithEntity(_))
                logDuration(complete(result))
              }
            }
        }
    }
  }

  /**
   * This method is used to view a user's wall.
   *
   * @param query
   * @return
   */
  def viewSpokersWall(query: ActorRef): Route = get {
    path("user" / Segment / "wall" / Segment) {
      (targetUserId, pos) =>
        {
          parameters('userId, 'phone_number) { (userId, phoneNumber) =>
            val result: Future[HttpResponse] = viewSpokersWallHandler(query, targetUserId, pos, userId).map(handleResponseWithEntity(_))
            logDuration(complete(result))
          }
        }
    }
  }

  /**
   * This method is use get full view of one spok.
   *
   * @param query
   * @return
   */
  def viewFullSpok(query: ActorRef): Route = get {
    pathPrefix("spok" / Segment / "full") { (spokId) =>
      pathEnd {
        parameters('userId, 'phone_number) { (userId, phoneNumber) =>
          val result: Future[HttpResponse] = viewFullSpok(query, spokId, "", userId).map(handleResponseWithEntity(_))
          logDuration(complete(result))
        }
      } ~
        path(Segment) {
          (targetUserId) =>
            {
              parameters('userId, 'phone_number) { (userId, phoneNumber) =>
                val result: Future[HttpResponse] = viewFullSpok(query, spokId, targetUserId, userId).map(handleResponseWithEntity(_))
                logDuration(complete(result))
              }
            }
        }
    }
  }

  /**
   * This route will create spok.
   *
   * @param command
   * @return
   */
  def createSpok(command: ActorRef, query: ActorRef): Route = {
    path("create" / "spok") {
      (post & entity(as[Multipart.FormData])) { formData =>
        {
          parameters('userId, 'phone_number) { (userId, phoneNumber) =>
            {
              val extractedData: Future[Map[String, Any]] = formData.parts.mapAsync[(String, Any)](1) {
                case file: BodyPart if file.name == "file" =>
                  val extension = file.entity.getContentType().toString.split("/")
                  val tempFile: File = File.createTempFile("file", "." + extension.apply(1))
                  file.entity.dataBytes.runWith(FileIO.toPath(tempFile.toPath)).map { ioResult =>
                    "file" -> tempFile
                  }
                case data: BodyPart => data.toStrict(5.seconds).map { strict =>
                  data.name -> strict.entity.data.utf8String
                }
              }.runFold(Map.empty[String, Any])((map, tuple) => map + tuple)
              complete(
                extractedData.flatMap { data =>
                  storeSpokSettings(query, command, userId, data.get("data").getOrElse("").toString, data.get("file").map(_.asInstanceOf[File]))
                }.recover {
                  case ex: ParseException =>
                    HttpResponse(StatusCodes.BadRequest, entity = HttpEntity(
                      ContentTypes.`application/json`,
                      write(sendFormattedError(SYST_503, SERVICE_UNAVAILABLE, Some(CREATE_SPOK)))
                    ))
                  case ex: Exception => HttpResponse(StatusCodes.BadRequest, entity = HttpEntity(
                    ContentTypes.`application/json`,
                    write(sendFormattedError(SPK_202, s"Error in processing multipart form data due to ${ex.getMessage}", Some(CREATE_SPOK)))
                  ))
                }
              )
            }
          }
        }
      }
    }
  }

  /**
   * Route to display a poll's stats only to the creator or to the user who has completed the poll
   *
   * @param query
   * @return
   */
  def viewPollStats(query: ActorRef): Route = get {
    path(SPOK / Segment / POLL_RESULTS) {
      (spokId) =>
        {
          parameters('userId, 'phone_number) { (userId, phoneNumber) =>
            val result: Future[HttpResponse] = viewPollStatsHandler(query, spokId, userId).map(handleResponseWithEntity(_))
            logDuration(complete(result))
          }
        }
    }
  }

  /**
   * Route to get the spoks, ctreated by me
   *
   * @param query
   * @return
   */
  def getMySpok(query: ActorRef): Route = get {
    path(MY / SPOKS / Segment) {
      pos =>
        {
          parameters('userId, 'phone_number) { (userId, phoneNumber) =>
            val result: Future[HttpResponse] = viewMySpokHandler(query, userId, pos).map(handleResponseWithEntity(_))
            logDuration(complete(result))
          }
        }
    }
  }
  /**
   * Sample web socket request to check HTTP connection
   */
  def sampleRoute: Route =
    path(PING) {
      get {
        logDuration(complete("pong"))
      }
    } ~
      path(GREETER) {
        logDuration(handleWebSocketMessages(greeter))
      }

  def routes(command: ActorRef, query: ActorRef): Route = sampleRoute ~ spokRoute(command, query) ~ viewPollQuestion(query) ~
    getSpokStats(query) ~ getComments(query) ~ getReSpokers(query) ~ getScopedUsers(query) ~ getSpoksStack(query) ~ viewShortSpok(query) ~
    viewSpokersWall(query) ~ viewFullSpok(query) ~ createSpok(command, query) ~ viewPollStats(query) ~ getMySpok(query)

}

