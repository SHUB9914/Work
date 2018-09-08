package com.spok.accountsservice.routes

import java.io.File

import akka.actor.ActorRef
import akka.http.scaladsl.model.Multipart.BodyPart
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.FileIO
import com.spok.accountsservice.handler.AccountRestServiceHandler
import com.spok.util.Constant._
import net.liftweb.json.JsonParser.ParseException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

/**
 * Service to handle the account of the user
 */
trait AccountRestService extends AccountRestServiceHandler {

  // ==============================
  //     REST ROUTES
  // ==============================

  // ===================
  // Register User(Step 1)
  // ===================

  /**
   * After connecting websocket connection
   *
   * @example https://github.com/sinaibay/spok-api/wiki/JSON-Formats#1-account-service
   * @return Register web socket route
   */
  def accountRegister(command: ActorRef, query: ActorRef): Route = path(REGISTER) {
    logDuration(handleWebSocketMessages(detectRegistrationRequestAndPerform(command, query)))
  }

  /**
   * This method is use get quick view of one spok.
   *
   * @param query
   * @return
   */
  def viewMinimalDetail(query: ActorRef): Route = get {
    path(USER_ROLE / Segment / "minimal") {
      targetUserId =>
        {
          parameters('userId, 'phone_number) { (userId, phoneNumber) =>
            val result: Future[HttpResponse] = viewShortDetail(query, targetUserId, userId).map(handleResponseWithEntity(_))
            logDuration(complete(result))
          }
        }
    }
  }

  /**
   * This method is used to get full view of one spok.
   *
   * @param query
   * @return
   */
  def viewFullDetail(query: ActorRef): Route = get {
    path(USER_ROLE / Segment) {
      targetUserId =>
        {
          parameters('userId, 'phone_number) { (userId, phoneNumber) =>
            val result: Future[HttpResponse] = viewFullDetail(query, targetUserId, userId).map(handleResponseWithEntity(_))
            logDuration(complete(result))
          }
        }
    }
  }

  /**
   * This method is used to remove user from cache.
   *
   * @param query
   * @return
   */
  def removeUserFromCache(query: ActorRef): Route = get {
    path("cache" / Segment) {
      targetId =>
        {
          parameters('userId, 'phone_number) { (userId, phoneNumber) =>
            val result: Future[HttpResponse] = removeUserFromCache(query, targetId, userId).map(handleResponseWithEntity(_))
            logDuration(complete(result))
          }
        }
    }
  }

  /**
   * This method is used to get list of followers of an user.
   *
   * @param query
   * @return
   */
  def getFollowers(query: ActorRef): Route = get {
    path(PROFILE / Segment / FOLLOWERS / Segment) {
      (targetUserId, pos) =>
        {
          parameters('userId, 'phone_number) { (userId, phoneNumber) =>
            val result: Future[HttpResponse] = getUserFollowers(query, targetUserId, userId, pos).map(handleResponseWithEntity(_))
            logDuration(complete(result))
          }
        }
    }
  }

  /**
   * This method is used to get list of followings of an user.
   *
   * @param query
   * @return
   */
  def getFollowings(query: ActorRef): Route = get {
    path(PROFILE / Segment / FOLLOWINGS / Segment) {
      (targetUserId, pos) =>
        {
          parameters('userId, 'phone_number) { (userId, phoneNumber) =>
            val result: Future[HttpResponse] = getUserFollowings(query, targetUserId, userId, pos).map(handleResponseWithEntity(_))
            logDuration(complete(result))
          }
        }
    }
  }

  /**
   * This method is used to get list of the details of a user's groups.
   *
   * @param query
   * @return
   */
  def getGroups(query: ActorRef): Route = get {
    pathPrefix(ALLGROUPS) {
      pathEnd {
        parameters('userId, 'phone_number) { (userId, phoneNumber) =>
          logger.info("Now hitting group request :: " + userId + phoneNumber)
          val result: Future[HttpResponse] = getDetailsOfGroupsForUser(query, userId, None).map(handleResponseWithEntity(_))
          logDuration(complete(result))
        }
      }
    } ~
      path(ALLGROUPS / Segment) {
        (pos) =>
          {
            parameters('userId, 'phone_number) { (userId, phoneNumber) =>
              val result: Future[HttpResponse] = getDetailsOfGroupsForUser(query, userId, Some(pos)).map(handleResponseWithEntity(_))
              logDuration(complete(result))
            }
          }
      }
  }

  /**
   * This method is used to get my details.
   *
   * @param query
   * @return
   */
  def getMyDetails(query: ActorRef): Route = get {
    path(MY / DETAILS) {
      parameters('userId, 'phone_number) { (userId, phoneNumber) =>
        val result: Future[HttpResponse] = viewMyDetail(query, userId).map(handleResponseWithEntity(_))
        logDuration(complete(result))
      }
    }
  }

  def accounts(command: ActorRef, query: ActorRef): Route = pathSingleSlash {
    parameters('userId, 'phone_number) { (userId, phoneNumber) =>
      logDuration(handleWebSocketMessages(detectRequestAndPerform(command, query, userId, phoneNumber)))
    }
  }

  // ==============================
  //     VIEW SPECIFIC GROUP
  // ==============================
  def getGroupDetail(query: ActorRef): Route = get {
    pathPrefix("group" / Segment) { (groupId) =>
      pathEnd {
        parameters('userId, 'phone_number) { (userId, phoneNumber) =>
          val result: Future[HttpResponse] = viewOneGroup(query, userId, groupId, None).map(handleResponseWithEntity(_))
          logDuration(complete(result))
        }
      } ~
        path(Segment) {
          (pos) =>
            {
              parameters('userId, 'phone_number) { (userId, phoneNumber) =>
                val result: Future[HttpResponse] = viewOneGroup(query, userId, groupId, Some(pos)).map(handleResponseWithEntity(_))
                logDuration(complete(result))
              }
            }
        }
    }
  }

  def getUserDetailsByAdmin(query: ActorRef): Route = get {
    path(ADMIN / USER_ROLE / Segment) { targetId =>
      parameters('userId, 'phone_number) { (userId, phoneNumber) =>
        val result: Future[HttpResponse] = viewDetailByAdmin(query, userId, targetId).map(handleResponseWithEntity(_))
        logDuration(complete(result))
      }
    }
  }

  def updateUserProfile(command: ActorRef, query: ActorRef): Route = {
    path("my" / "profile") {
      (put & entity(as[Multipart.FormData])) { formData =>
        {
          parameters('userId, 'phone_number) { (userId, phoneNumber) =>
            {
              val Four = 4
              val extractedData: Future[Map[String, Any]] = formData.parts.mapAsync[(String, Any)](Four) {
                case picture: BodyPart if picture.name == "picture" =>
                  val extension = picture.entity.getContentType().toString.split("/")
                  val tempFile: File = File.createTempFile("picture", "." + extension.apply(1))
                  picture.entity.dataBytes.runWith(FileIO.toPath(tempFile.toPath)).map { ioResult =>
                    "picture" -> tempFile
                  }
                case cover: BodyPart if cover.name == "cover" =>
                  val extension = cover.entity.getContentType().toString.split("/")
                  val tempFile: File = File.createTempFile("cover", "." + extension.apply(1))
                  cover.entity.dataBytes.runWith(FileIO.toPath(tempFile.toPath)).map { ioResult =>
                    "cover" -> tempFile
                  }
                case data: BodyPart if data.name == "nickname" => data.toStrict(5.seconds).map { strict =>
                  data.name -> strict.entity.data.utf8String

                } case data: BodyPart if data.name == "birthDate" => data.toStrict(5.seconds).map { strict =>
                  data.name -> strict.entity.data.utf8String

                } case data: BodyPart if data.name == "gender" => data.toStrict(5.seconds).map { strict =>
                  data.name -> strict.entity.data.utf8String

                } case data: BodyPart if data.name == "geoLat" => data.toStrict(5.seconds).map { strict =>
                  data.name -> strict.entity.data.utf8String

                } case data: BodyPart if data.name == "geoLong" => data.toStrict(5.seconds).map { strict =>
                  data.name -> strict.entity.data.utf8String

                } case data: BodyPart if data.name == "geoElev" => data.toStrict(5.seconds).map { strict =>
                  data.name -> strict.entity.data.utf8String

                } case data: BodyPart if data.name == "geoText" => data.toStrict(5.seconds).map { strict =>
                  data.name -> strict.entity.data.utf8String
                }
              }.runFold(Map.empty[String, Any])((map, tuple) => map + tuple)
              complete(
                extractedData.flatMap { data =>
                  val result: Future[String] = updateUserProfileHandler(command, query, userId, phoneNumber,
                    data.get("nickname").map(_.toString), data.get("birthDate").map(_.toString), data.get("gender").map(_.toString),
                    data.get("geoLat").map(_.toString.toDouble), data.get("geoLong").map(_.toString.toDouble), data.get("geoElev").map(_.toString.toDouble),
                    data.get("geoText").map(_.toString), data.get("cover").map(_.asInstanceOf[File]), data.get("picture").map(_.asInstanceOf[File]))
                  result.map { res => HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentTypes.`application/json`, res)) }
                }.recover {
                  case ex: ParseException =>
                    HttpResponse(StatusCodes.BadRequest, entity = HttpEntity(
                      ContentTypes.`application/json`,
                      write(sendFormattedError(SYST_503, SERVICE_UNAVAILABLE, Some(UPDATE_USER_PROFILE)))
                    ))
                  case ex: Exception => HttpResponse(StatusCodes.BadRequest, entity = HttpEntity(
                    ContentTypes.`application/json`,
                    write(sendFormattedError(SYST_400, s"Invalid call to the service $UPDATE_USER_PROFILE (wrong parameters).", Some(UPDATE_USER_PROFILE)))
                  ))
                }
              )
            }
          }
        }
      } ~
        get {
          parameters('userId, 'phone_number) { (userId, phoneNumber) =>
            val result: Future[HttpResponse] = viewDetail(query, userId).map(handleResponseWithEntity(_))
            logDuration(complete(result))
          }
        }
    }
  }

  def routes(command: ActorRef, query: ActorRef): Route = accountRegister(command, query) ~
    viewMinimalDetail(query) ~ viewFullDetail(query) ~ removeUserFromCache(query) ~
    getFollowers(query) ~ getFollowings(query) ~ getGroups(query) ~
    getMyDetails(query) ~ accounts(command, query) ~ getGroupDetail(query) ~
    updateUserProfile(command, query) ~ getUserDetailsByAdmin(query)

}
