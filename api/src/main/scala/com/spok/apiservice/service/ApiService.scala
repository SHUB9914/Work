package com.spok.apiservice.service

import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

import akka.actor.{ ActorSystem, Props }
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.TextMessage.Strict
import akka.http.scaladsl.model.ws._
import akka.http.scaladsl.server._
import akka.http.scaladsl.{ Http, HttpExt }
import akka.stream._
import akka.stream.scaladsl.{ Flow, Sink, Source, SourceQueue, SourceQueueWithComplete }
import com.spok.apiservice.handler.{ AccountServiceHandler, ApiServiceHandler, MessagingServiceHandler, SpokServiceHandler }
import com.spok.persistence.redis.RedisFactory
import com.spok.util.ConfigUtil._
import com.spok.util.Constant.{ DISABLE, DISABLEACCOUNT, MY_ACCOUNT_DISABLE, _ }
import com.spok.util._

import scala.concurrent.Future
import scala.concurrent.duration._

object ApiStarter {
  implicit val system = ActorSystem("spokApi")
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()
}

/**
 * All REST Routes will be handlled by Api Service
 */

class ApiService(implicit fm: Materializer, system: ActorSystem) extends JsonHelper with ApiServiceHandler with SpokServiceHandler
    with AccountServiceHandler with MessagingServiceHandler with ResponseUtil {

  import system.dispatcher

  override val redisFactory: RedisFactory = RedisFactory
  val http: HttpExt = Http()
  val fileUploadUtility: FileUploadUtility = FileUploadUtility

  // ==============================
  //     SAMPLE ROUTES
  // ==============================

  def webSocketResponse: Flow[Message, Message, Future[WebSocketUpgradeResponse]] = {
    http.webSocketClientFlow(WebSocketRequest(WS + lookupService(spokServiceName) + ":" + spokPort + DELIMITER + GREETER))
  }

  def sampleRoutes: Route = path(PING) {
    logDuration(complete("pong"))
  } ~
    path(GREETER) {
      logDuration(handleWebSocketMessages(webSocketResponse))
    }

  def addMessageInSourceQueue(userInfo: (Option[String], Option[String])): Flow[Message, Message, _] = {
    val (userId, phoneNumber) = userInfo
    val (spokSource, spokeQueue) = sourceQueue
    val (notificationSource, notificationQueue) = sourceQueue
    val (accountSource, accountQueue) = sourceQueue
    val (messagingSource, messagingQueue) = sourceQueue
    Flow[Message]
      .collect {
        case TextMessage.Strict(txt) ⇒ {
          logger.info(s"${phoneNumber}: Getting action messages !!! ${txt}")
          val message = TextMessage(txt)
          logger.info(s"${phoneNumber}: Getting action message {$message}")
          val actionOpt = try {
            (parse(txt) \ (ACTION)).extractOpt[String]
          } catch {
            case ex: Exception => Some(INVALID_JSON)
          }
          logger.info(s"${phoneNumber}: Getting action actionOpt !!! ${actionOpt}")

          actionOpt match {
            case Some(RESPOK) | Some(UNSPOK) | Some(ADD_COMMENT)
              | Some(REMOVE_COMMENT) | Some(UPDATE_COMMENT) | Some(SUBSCRIBE)
              | Some(DISABLE) | Some(REMOVE_SPOK) | Some(ANSWER_POLL) | Some(ANSWERS_POLL) => {
              logger.info(s"${phoneNumber}: Got the request. Now redirecting to spok api !!!!!!!!")
              addInQueue(spokeQueue, message)
            }
            case Some(FOLLOW_UNFOLLOW) | Some(CREATE_GROUP)
              | Some(UPDATE_GROUP) | Some(REMOVE_GROUP)
              | Some(ADD_FOLLOWER_GROUP) | Some(REMOVE_FOLLOWER_GROUP)
              | Some(UPDATE_USER_PROFILE) | Some(FOLLOW_SETTINGS)
              | Some(HELP_SETTINGS) | Some(UPDATE_PHONE_STEP_ONE)
              | Some(UPDATE_PHONE_STEP_TWO) | Some(REGISTER) | Some(AUTHENTICATE) | Some(AUTH_CODE)
              | Some(DISABLE_USER) | Some(MY_ACCOUNT_DISABLE) | Some(UPDATE_LEVEL)
              | Some(CODE) | Some(DETAILS) | Some(SUPPORT) | Some(SUSPEND_SPOKER) | Some(REACTIVATE_SPOKER) => {
              logger.info(s"${phoneNumber}: Got the request. Now redirecting to account api !!!!!!!!")
              addInQueue(accountQueue, message)
            }
            case Some(REMOVE_NOTIFICATION) => {
              logger.info(s"${phoneNumber}: Got the request. Now redirecting to notification api !!!!!!!!")
              addInQueue(notificationQueue, message)
            }
            case Some(REMOVE_TALK | REMOVE_MESSAGE | READ_MESSAGE | TYPING) => {
              logger.info(s"${phoneNumber}: Got the request. Now redirecting to messaging api !!!!!!!!")
              addInQueue(messagingQueue, message)
            }
            case _ => addInQueue(accountQueue, message)
          }

          message
        }
        case _ => TextMessage(INVALID_ACTION)
      }
      .via(connectSource(spokSource, userId, phoneNumber, notificationSource, accountSource, messagingSource)) // ... and route them through the receiveNotification ...
      .map {
        case msg: String ⇒ {
          info(s"Huhh !! Why I am getting this message ${msg}")
          TextMessage.Strict(msg)
        }
      }
  }

  def addInQueue(spokeQueue: Future[SourceQueue[Message]], message: Strict): Future[Future[QueueOfferResult]] = {
    spokeQueue.map(q => {
      q.offer(message)
    })
  }

  def connectSource(
    source: Source[Message, SourceQueueWithComplete[Message]],
    userId: Option[String], phoneNumber: Option[String],
    notificationSource: Source[Message, SourceQueueWithComplete[Message]],
    accountSource: Source[Message, SourceQueueWithComplete[Message]],
    messagingSource: Source[Message, SourceQueueWithComplete[Message]]
  ): Flow[Message, String, Any] = {
    logger.info(s" ${phoneNumber} is connected with microservices!!!!")
    val apiConnector = system.actorOf(Props(new ApiConnector()))
    val in: Sink[Message, _] = Sink.actorRef[Message](apiConnector, DisconnectService(phoneNumber))

    val out =
      Source.actorRef[String](Int.MaxValue, OverflowStrategy.dropBuffer)
        .mapMaterializedValue(apiConnector ! ConnectService(_, userId, phoneNumber, source, notificationSource, accountSource, messagingSource))
    Flow.fromSinkAndSource(in, out)
  }

  // ==============================
  //     SPOK INSTANCE STATS ROUTES
  // ==============================
  def getSpokStats: Route = path(SPOK / Segment / STATS) {
    spokId =>
      {
        optionalHeaderValueByType[UpgradeToWebSocket](()) {
          case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
          case None ⇒ get {
            ctx: RequestContext =>

              ctx.request.uri
              val url = HTTP + lookupService(spokServiceName) + ":" + spokPort + DELIMITER + SPOK + DELIMITER + spokId + DELIMITER + STATS
              validatedToken(url, ctx)
          }
        }
      }
  }

  // Route to receive notification from websocket server

  def webSocketRoute: Route = pathEndOrSingleSlash {
    logDuration(handleWebSocketMessagesForProtocol(addMessageInSourceQueue _, protocol))
  }

  def otherRoutes: Route = path(Segment) {
    random =>
      optionalHeaderValueByType[UpgradeToWebSocket](()) {
        case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
        case None ⇒ reject(AuthorizationFailedRejection)
      }
  }

  def errorResponse: Flow[Message, Message, Any] =
    Flow[Message].collect {
      case TextMessage.Strict(tm) => tm
    }.via(errorFlow)
      .map {
        case msg: String => TextMessage.Strict(msg)
      }

  def errorFlow: Flow[String, String, Any] =
    Flow.fromSinkAndSource(Sink.ignore, Source.single(write(sendFormattedError(SYST_404, NOT_FOUND, Some(INVALID)))))

  def fetchGroups: Route = pathPrefix(GROUPS) {
    pathEnd {
      optionalHeaderValueByType[UpgradeToWebSocket](()) {
        case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
        case None ⇒ get {
          ctx: RequestContext =>
            val url = HTTP + lookupService(accountServiceName) + ":" + accountPort + DELIMITER + "all" + GROUPS
            validatedToken(url, ctx)
        }
      }
    } ~
      path(Segment) {
        (pos) =>
          {
            optionalHeaderValueByType[UpgradeToWebSocket](()) {
              case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse))
              case None ⇒ get {
                ctx: RequestContext =>
                  val url = HTTP + lookupService(accountServiceName) + ":" + accountPort + DELIMITER + "all" + GROUPS + DELIMITER + pos
                  validatedToken(url, ctx)
              }
            }
          }
      }
  }

  // ==============================
  //    SPOKS STACK ROUTES
  // ==============================
  def getSpokStack: Route =
    pathPrefix(SPOKS) {
      pathEnd {
        optionalHeaderValueByType[UpgradeToWebSocket](()) {
          case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
          case None ⇒ post {
            ctx: RequestContext =>
              {
                val ((userId, phone_number), validToken) = handleHttpReqWithAuth(ctx)

                if (validToken.equals("validToken")) {
                  val url = HTTP + lookupService(spokServiceName) + ":" + spokPort + DELIMITER + "create" + DELIMITER + "spok"
                  val httpResponse = http.singleRequest(ctx.request.copy(uri = url + "?userId=" + userId + "&phone_number=" + phone_number))
                  httpResponse.map { res =>
                    res.entity.toStrict(3000.millis).map { entity =>
                      val data = Charset.forName("UTF-8").decode(entity.data.asByteBuffer)
                      checkResponseAndTakeAction(data.toString, Some(userId), Some(phone_number))
                    }
                  }
                  ctx.complete(httpResponse)
                } else {
                  ctx.complete(HttpResponse(status = StatusCodes.Unauthorized, entity = HttpEntity(
                    ContentTypes.`application/json`,
                    write(sendFormattedError(SYST_401, AUTHENTICATION_REQUIRED, Some(AUTHENTICATION)))
                  )))
                }
              }
          }
        }
      } ~
        path(Segment) {
          pos =>
            {
              optionalHeaderValueByType[UpgradeToWebSocket](()) {
                case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
                case None ⇒ get {
                  ctx: RequestContext =>
                    val url = HTTP + lookupService(spokServiceName) + ":" + spokPort + DELIMITER + SPOKS + DELIMITER + pos
                    validatedToken(url, ctx)
                }
              }
            }
        }
    }






  // ==============================
  //     SCOPED USERS ROUTE
  // ==============================
  def getScopedUsers: Route = path(SPOK / Segment / SCOPED / Segment) {
    (spokId, pos) =>
      {
        optionalHeaderValueByType[UpgradeToWebSocket](()) {
          case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
          case None ⇒ get {
            ctx: RequestContext =>
              val url = HTTP + lookupService(spokServiceName) + ":" + spokPort + DELIMITER + SPOK + DELIMITER + spokId + DELIMITER + SCOPED + DELIMITER + pos
              validatedToken(url, ctx)
          }
        }
      }
  }

  // ==============================
  //     GET RESPOKERS ROUTE
  // ==============================
  def getReSpokers: Route = path(SPOK / Segment / RESPOKERS / Segment) {
    (spokId, pos) =>
      {
        optionalHeaderValueByType[UpgradeToWebSocket](()) {
          case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
          case None ⇒ get {
            ctx: RequestContext =>
              val url = HTTP + lookupService(spokServiceName) + ":" + spokPort + DELIMITER + SPOK + DELIMITER + spokId + DELIMITER + RESPOKERS + DELIMITER + pos
              validatedToken(url, ctx)
          }
        }
      }
  }

  // ==============================
  //     VIEW SHORT SPOK
  // ==============================
  def viewShortSpok: Route =
    pathPrefix(STACK / Segment) { (spokId) =>
      pathEnd {
        optionalHeaderValueByType[UpgradeToWebSocket](()) {
          case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
          case None ⇒ get {
            ctx: RequestContext =>
              val url = HTTP + lookupService(spokServiceName) + ":" + spokPort + DELIMITER + STACK + DELIMITER + spokId
              validatedToken(url, ctx)
          }
        }
      } ~
        path(Segment) {
          (targetUserId) =>
            {
              optionalHeaderValueByType[UpgradeToWebSocket](()) {
                case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
                case None ⇒ get {
                  ctx: RequestContext =>
                    val url = HTTP + lookupService(spokServiceName) + ":" + spokPort + DELIMITER + STACK + DELIMITER + spokId + DELIMITER + targetUserId
                    validatedToken(url, ctx)
                }
              }
            }
        }
    }

  // ==============================
  //     VIEW FULL SPOK
  // ==============================
  def viewFullSpok: Route = pathPrefix(SPOK / Segment / FULL) { (spokId) =>
    pathEnd {
      optionalHeaderValueByType[UpgradeToWebSocket](()) {
        case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
        case None ⇒ get {
          ctx: RequestContext =>
            val url = HTTP + lookupService(spokServiceName) + ":" + spokPort + DELIMITER + SPOK + DELIMITER + spokId + DELIMITER + FULL
            validatedToken(url, ctx)
        }
      }
    } ~
      path(Segment) {
        (targetUserId) =>
          {
            optionalHeaderValueByType[UpgradeToWebSocket](()) {
              case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
              case None ⇒ get {
                ctx: RequestContext =>
                  val url = HTTP + lookupService(spokServiceName) + ":" + spokPort + DELIMITER +
                    SPOK + DELIMITER + spokId + DELIMITER + FULL + DELIMITER + targetUserId
                  validatedToken(url, ctx)
              }
            }
          }
      }
  }

  // ==============================
  //     VIEW Minimal user detail
  // ==============================
  def viewMinimalUserDetail: Route = path(USER_ROLE / Segment / "minimal") {
    userId =>
      {
        optionalHeaderValueByType[UpgradeToWebSocket](()) {
          case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
          case None ⇒ get {
            ctx: RequestContext =>
              val url = HTTP + lookupService(accountServiceName) + ":" + accountPort + DELIMITER + USER_ROLE + DELIMITER + userId + DELIMITER + "minimal"
              validatedToken(url, ctx)
          }
        }
      }
  }

  def disableAccount: Route = path(DISABLEACCOUNT / Segment) {
    targetId =>
      {
        optionalHeaderValueByType[UpgradeToWebSocket](()) {
          case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
          case None ⇒ get {
            ctx: RequestContext =>
              val url = HTTP + lookupService(accountServiceName) + ":" + accountPort + DELIMITER + DISABLEACCOUNT + DELIMITER + targetId
              validatedToken(url, ctx)
          }
        }
      }
  }

  def myAccountDisable: Route = path(MY_ACCOUNT_DISABLE) {

    optionalHeaderValueByType[UpgradeToWebSocket](()) {
      case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
      case None ⇒ get {
        ctx: RequestContext =>
          val url = HTTP + lookupService(accountServiceName) + ":" + accountPort + DELIMITER + MY_ACCOUNT_DISABLE
          validatedToken(url, ctx)
      }
    }

  }

  // ==============================
  //     VIEW Full user detail
  // ==============================
  def viewFullUserDetail: Route = path(USER_ROLE / Segment) {
    userId =>
      {
        optionalHeaderValueByType[UpgradeToWebSocket](()) {
          case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
          case None ⇒ get {
            ctx: RequestContext =>
              val url = HTTP + lookupService(accountServiceName) + ":" + accountPort + DELIMITER + USER_ROLE + DELIMITER + userId
              validatedToken(url, ctx)
          }
        }
      }
  }

  // ==============================
  //    Remove USER from cache
  // ==============================
  def removeUserFromCache: Route = path("cache" / Segment) {
    id =>
      {
        optionalHeaderValueByType[UpgradeToWebSocket](()) {
          case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
          case None ⇒ get {
            ctx: RequestContext =>
              val url = HTTP + lookupService(accountServiceName) + ":" + accountPort + DELIMITER + "cache" + DELIMITER + id
              validatedToken(url, ctx)
          }
        }
      }
  }

  private def validatedToken(url: String, ctx: RequestContext, isSearch: Boolean = false) = {
    val start = System.currentTimeMillis()
    val ((userId, phone_number), validToken) = handleHttpReqWithAuth(ctx)
    if (validToken.equals("validToken")) {
      val httpRequest = if (isSearch) {
        HttpRequest(uri = url + "&userId=" + userId + "&phone_number=" + phone_number)
      } else {
        HttpRequest(uri = url + "?userId=" + userId + "&phone_number=" + phone_number)
      }
      val responseFuture: Future[HttpResponse] = http.singleRequest(httpRequest)
      val totalDuration = System.currentTimeMillis() - start
      info(s"[${StatusCodes.OK.intValue}] ${ctx.request.method.name} ${ctx.request.uri} took: ${totalDuration}ms")
      val outGoingResponse = responseFuture.map { response =>
        response match {
          case HttpResponse(StatusCodes.OK, headers, entity, _) => response
          case HttpResponse(code, _, _, _) => HttpResponse(status = StatusCodes.Unauthorized, entity = HttpEntity(
            ContentTypes.`application/json`,
            write(sendFormattedError(SYST_401, "Not Available", Some(AUTHENTICATION)))
          ))
        }
      }
      ctx.complete(outGoingResponse)
    } else {
      val totalDuration = System.currentTimeMillis() - start
      info(s"[${StatusCodes.Unauthorized.intValue}] ${ctx.request.method.name} ${ctx.request.uri} took: ${totalDuration}ms")
      ctx.complete(HttpResponse(status = StatusCodes.Unauthorized, entity = HttpEntity(
        ContentTypes.`application/json`,
        write(sendFormattedError(SYST_401, AUTHENTICATION_REQUIRED, Some(AUTHENTICATION)))
      )))
    }
  }

  // ==============================
  //     GET COMMENTS OF A SPOK
  // ==============================
  def getSpokComment: Route = path(SPOK / Segment / COMMENTS / Segment) {
    (spokId, pos) =>
      {
        optionalHeaderValueByType[UpgradeToWebSocket](()) {
          case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
          case None ⇒ get {
            ctx: RequestContext =>
              val url = HTTP + lookupService(spokServiceName) + ":" + spokPort + DELIMITER + SPOK + DELIMITER + spokId + DELIMITER + COMMENTS + DELIMITER + pos
              validatedToken(url, ctx)
          }
        }
      }
  }

  // ==============================
  //     VIEW SPOKER'S  WALL
  // ==============================
  def viewSpokersWall: Route = path(USER_ROLE / Segment / WALL / Segment) {
    (userId, pos) =>
      {
        optionalHeaderValueByType[UpgradeToWebSocket](()) {
          case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
          case None ⇒ get {
            ctx: RequestContext =>
              val url = HTTP + lookupService(spokServiceName) + ":" + spokPort + DELIMITER + USER_ROLE + DELIMITER + userId + DELIMITER + WALL + DELIMITER + pos
              validatedToken(url, ctx)
          }
        }
      }
  }

  // ==============================
  //     VIEW WALL'S NOTIFICATION
  // ==============================
  def getNotificationRoute: Route = pathPrefix("my" / "allnotifications") {
    pathEnd {
      optionalHeaderValueByType[UpgradeToWebSocket](()) {
        case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
        case None ⇒ get {
          ctx: RequestContext =>
            val url = HTTP + lookupService(notificationServiceName) + ":" + notificationPort + DELIMITER +
              "my" + DELIMITER + "allnotifications"
            validatedToken(url, ctx)
        }
      }
    } ~
      path(Segment) {
        (pos) =>
          {
            optionalHeaderValueByType[UpgradeToWebSocket](()) {
              case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse))
              case None ⇒ get {
                ctx: RequestContext =>
                  val url = HTTP + lookupService(notificationServiceName) + ":" + notificationPort + DELIMITER +
                    "my" + DELIMITER + "allnotifications" + DELIMITER + pos
                  validatedToken(url, ctx)
              }
            }
          }
      }
  }

  // ==============================
  //     GET LIST OF FOLLOWERS
  // ==============================
  def getFollowers: Route = path(PROFILE / Segment / FOLLOWERS / Segment) {
    (userId, pos) =>
      {
        optionalHeaderValueByType[UpgradeToWebSocket](()) {
          case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
          case None ⇒ get {
            ctx: RequestContext =>
              val url = HTTP + lookupService(accountServiceName) + ":" + accountPort + DELIMITER + PROFILE + DELIMITER + userId + DELIMITER +
                FOLLOWERS + DELIMITER + pos
              validatedToken(url, ctx)
          }
        }
      }
  }

  // ==============================
  //     GET LIST OF FOLLOWINGS
  // ==============================
  def getFollowings: Route = path(PROFILE / Segment / FOLLOWINGS / Segment) {
    (userId, pos) =>
      {
        optionalHeaderValueByType[UpgradeToWebSocket](()) {
          case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
          case None ⇒ get {
            ctx: RequestContext =>
              val url = HTTP + lookupService(accountServiceName) + ":" + accountPort + DELIMITER + PROFILE + DELIMITER + userId + DELIMITER +
                FOLLOWINGS + DELIMITER + pos
              validatedToken(url, ctx)
          }
        }
      }
  }

  // ==============================
  //    POLL VIEW AND ANSWER ROUTES
  // ==============================

  def pollQuestionRoute: Route =
    path(POLL / Segment) {
      questionId =>
        {
          optionalHeaderValueByType[UpgradeToWebSocket](()) {
            case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
            case None ⇒ get {
              ctx: RequestContext =>
                val url = HTTP + lookupService(spokServiceName) + ":" + spokPort + DELIMITER + POLL + DELIMITER + questionId
                validatedToken(url, ctx)
            }
          }
        }
    }

  // ==============================
  //    VIEW POLL STATS ROUTE
  // ==============================

  def viewPollStats: Route =
    path(SPOK / Segment / POLL_RESULTS) {
      spokId =>
        {
          optionalHeaderValueByType[UpgradeToWebSocket](()) {
            case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
            case None ⇒ get {
              ctx: RequestContext =>
                val url = HTTP + lookupService(spokServiceName) + ":" + spokPort + DELIMITER + SPOK + DELIMITER + spokId + DELIMITER + POLL_RESULTS
                validatedToken(url, ctx)
            }
          }
        }
    }

  /**
   * This route will hit for Authentication token
   *
   * @return
   */

  // ==============================
  //     VIEW MY DETAILS
  // ==============================

  def getMyDetails: Route = path(MY / DETAILS) {
    optionalHeaderValueByType[UpgradeToWebSocket](()) {
      case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
      case None ⇒ get {
        ctx: RequestContext =>
          val url = HTTP + lookupService(accountServiceName) + ":" + accountPort + DELIMITER + MY + DELIMITER + DETAILS
          validatedToken(url, ctx)
      }
    }
  }

  // ==============================
  //     SEARCH BY NICKNAME
  // ==============================

  def searchByNickname: Route = path(SEARCH / AUTONICK) {
    parameters('nickname) {
      nickname =>
        {
          optionalHeaderValueByType[UpgradeToWebSocket](()) {
            case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
            case None ⇒ get {
              ctx: RequestContext =>
                val url = HTTP + lookupService(searchServiceName) + ":" + searchPort + DELIMITER + SEARCH + DELIMITER + AUTONICK + "?nickname=" + nickname
                validatedToken(url, ctx, true)
            }
          }
        }
    }
  }

  // ==============================
  //     SEARCH BY HASHTAG
  // ==============================

  def searchByHashtag: Route = path(SEARCH / AUTOHASH) {
    parameters('hashtag) {
      hashtag =>
        {
          optionalHeaderValueByType[UpgradeToWebSocket](()) {
            case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
            case None ⇒ get {
              ctx: RequestContext =>
                val url = HTTP + lookupService(searchServiceName) + ":" + searchPort + DELIMITER + SEARCH + DELIMITER + AUTOHASH + "?hashtag=" + hashtag
                validatedToken(url, ctx, true)
            }
          }
        }
    }
  }

  // ==============================
  //     GET LIST OF POPULAR SPOKERS
  // ==============================
  def getPopularSpokers: Route = path(SEARCH / POPULAR / Segment) {
    pos =>
      {
        optionalHeaderValueByType[UpgradeToWebSocket](()) {
          case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
          case None ⇒ get {
            ctx: RequestContext =>
              val url = HTTP + lookupService(searchServiceName) + ":" + searchPort + DELIMITER + SEARCH + DELIMITER + POPULAR + DELIMITER + pos
              validatedToken(url, ctx)
          }
        }
      }
  }

  // ==============================
  //     SEARCH LAST SPOKS
  // ==============================

  def searchLastSpoks: Route = path(SEARCH / LAST / Segment) {
    pos =>
      {
        optionalHeaderValueByType[UpgradeToWebSocket](()) {
          case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
          case None ⇒ get {
            ctx: RequestContext =>
              val url = HTTP + lookupService(searchServiceName) + ":" + searchPort + DELIMITER + SEARCH + DELIMITER + LAST + DELIMITER + pos
              validatedToken(url, ctx)
          }
        }
      }
  }

  // ==============================
  //     SEARCH FRIEND'S SPOKS
  // ==============================

  def searchFriendSpoks: Route = path(SEARCH / FRIENDS / Segment) {
    pos =>
      {
        optionalHeaderValueByType[UpgradeToWebSocket](()) {
          case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
          case None ⇒ get {
            ctx: RequestContext =>
              val url = HTTP + lookupService(searchServiceName) + ":" + searchPort + DELIMITER + SEARCH + DELIMITER + FRIENDS + DELIMITER + pos
              validatedToken(url, ctx)
          }
        }
      }
  }

  // ==============================
  //     SEARCH TRENDY SPOKS
  // ==============================

  def searchTrendySpoks: Route = path(SEARCH / TRENDY / Segment) {
    pos =>
      {
        optionalHeaderValueByType[UpgradeToWebSocket](()) {
          case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
          case None ⇒ get {
            ctx: RequestContext =>
              val url = HTTP + lookupService(searchServiceName) + ":" + searchPort + DELIMITER + SEARCH + DELIMITER + TRENDY + DELIMITER + pos
              validatedToken(url, ctx)
          }
        }
      }
  }

  // ==============================
  //     VIEW SPECIFIC GROUP
  // ==============================
  def getSpecificGroupDetail: Route = pathPrefix("group" / Segment) { (groupId) =>
    pathEnd {
      optionalHeaderValueByType[UpgradeToWebSocket](()) {
        case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
        case None ⇒ get {
          ctx: RequestContext =>
            val url = HTTP + lookupService(accountServiceName) + ":" + accountPort + DELIMITER + "group" + DELIMITER + groupId
            validatedToken(url, ctx)
        }
      }
    } ~
      path(Segment) {
        (pos) =>
          {
            optionalHeaderValueByType[UpgradeToWebSocket](()) {
              case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse))
              case None ⇒ get {
                ctx: RequestContext =>
                  val url = HTTP + lookupService(accountServiceName) + ":" + accountPort + DELIMITER + "group" + DELIMITER + groupId + DELIMITER + pos
                  validatedToken(url, ctx)
              }
            }
          }
      }
  }

  // ==============================
  //     FULL SEARCH
  // ==============================

  def launchSearch: Route =
    path(SEARCH / Segment) {
      pos =>
        {
          parameters('userids.*, 'hashtags.*, 'latitude.?, 'longitude.?, 'start.?, 'end.?, 'content_types.*) {
            (userIds, hashtags, latitude, longitude, start, end, content_types) =>
              optionalHeaderValueByType[UpgradeToWebSocket](()) {
                case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
                case None ⇒ get {
                  {
                    ctx: RequestContext =>
                      val url = HTTP + lookupService(searchServiceName) + ":" + searchPort + DELIMITER +
                        SEARCH + DELIMITER + pos + "?userids=" + userIds.mkString(",") + "&hashtags=" + hashtags.mkString(",") +
                        "&latitude=" + latitude.getOrElse("") + "&longitude=" + longitude.getOrElse("") +
                        "&start=" + start.getOrElse("") + "&end=" + end.getOrElse("") + "&content_types=" + content_types.mkString(",")
                      validatedToken(url, ctx, true)
                  }
                }
              }
          }
        }
    }

  def getUserDetailsByAdmin: Route = path(ADMIN / USER_ROLE / Segment) {
    pos =>
      {
        optionalHeaderValueByType[UpgradeToWebSocket](()) {
          case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
          case None ⇒ get {
            ctx: RequestContext =>
              val url = HTTP + lookupService(accountServiceName) + ":" + accountPort + DELIMITER + ADMIN + DELIMITER + USER_ROLE + DELIMITER + pos
              validatedToken(url, ctx)
          }
        }
      }
  }

  def updateUserProfile: Route = {
    path("my" / "profile") {
      optionalHeaderValueByType[UpgradeToWebSocket](()) {
        case Some(upgrade) ⇒
          complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
        case None ⇒ put {
          ctx: RequestContext =>
            {
              val ((userId, phone_number), validToken) = handleHttpReqWithAuth(ctx)
              if (validToken.equals("validToken")) {
                val url = HTTP + lookupService(accountServiceName) + ":" + accountPort + DELIMITER + "my" + DELIMITER + "profile"
                val httpResponse = http.singleRequest(ctx.request.copy(uri = url + "?userId=" + userId + "&phone_number=" + phone_number))
                httpResponse.map { res =>
                  res.entity.toStrict(3000.millis).map { entity =>
                    val data = Charset.forName("UTF-8").decode(entity.data.asByteBuffer)
                    checkAccountResponseAndTakeAction(data.toString, Some(userId), Some(phone_number))
                  }
                }
                ctx.complete(httpResponse)
              } else {
                ctx.complete(HttpResponse(status = StatusCodes.Unauthorized, entity = HttpEntity(
                  ContentTypes.`application/json`,
                  write(sendFormattedError(SYST_401, AUTHENTICATION_REQUIRED, Some(AUTHENTICATION)))
                )))
              }
            }
        } ~
          get {
            ctx: RequestContext =>
              val url = HTTP + lookupService(accountServiceName) + ":" + accountPort + DELIMITER + "my" + DELIMITER + "profile"
              validatedToken(url, ctx)
          }
      }
    }
  }

  /**
   * send message route.
   *
   * @return
   */
  def sendMessage: Route = pathPrefix(TALK / Segment) {
    friendUserId =>
      {
        optionalHeaderValueByType[UpgradeToWebSocket](()) {
          case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
          case None ⇒ post { ctx: RequestContext => sendMessageResponseHandler(ctx, friendUserId) }
        }
      }
  }

  /**
   * This method will redirect request to messaging service and send notification.
   *
   * @param ctx
   * @return
   */
  private def sendMessageResponseHandler(ctx: RequestContext, friendUserId: String): Future[RouteResult] = {
    val ((userId, phone_number), validToken) = handleHttpReqWithAuth(ctx)
    if (validToken.equals("validToken")) {
      val url = HTTP + lookupService(messagingServiceName) + ":" + messagingPort + DELIMITER + TALK + DELIMITER + friendUserId
      val httpResponse = http.singleRequest(ctx.request.copy(uri = url + "?userId=" + userId + "&phone_number=" + phone_number))
      httpResponse.map { res =>
        res.entity.toStrict(3000.millis).map { entity =>
          val data = Charset.forName("UTF-8").decode(entity.data.asByteBuffer)
          checkMessagingResponseAndTakeAction(data.toString, userId)
        }
      }
      ctx.complete(httpResponse)
    } else {
      ctx.complete(HttpResponse(status = StatusCodes.Unauthorized, entity = HttpEntity(
        ContentTypes.`application/json`,
        write(sendFormattedError(SYST_401, AUTHENTICATION_REQUIRED, Some(AUTHENTICATION)))
      )))
    }
  }

  // ==============================
  //    VIEW ALL TALKS ROUTE
  // ==============================

  def viewAllTalks: Route =
    pathPrefix(TALKS) {
      pathEnd {
        optionalHeaderValueByType[UpgradeToWebSocket](()) {
          case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
          case None ⇒ get {
            ctx: RequestContext =>
              val url = HTTP + lookupService(messagingServiceName) + ":" + messagingPort + DELIMITER + TALKS
              validatedToken(url, ctx)
          }
        }
      } ~
        path(Segment) {
          (pos) =>
            {
              optionalHeaderValueByType[UpgradeToWebSocket](()) {
                case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
                case None ⇒ get {
                  ctx: RequestContext =>
                    val url = HTTP + lookupService(messagingServiceName) + ":" + messagingPort + DELIMITER + TALKS + DELIMITER + pos
                    validatedToken(url, ctx)
                }
              }
            }
        }
    }

  // ==============================
  //    VIEW SINGLE TALK ROUTE
  // ==============================

  def viewSingleTalk: Route =
    pathPrefix(TALK / Segment) { targetUserId =>
      pathEnd {
        parameters('order ? "desc") { order =>
          optionalHeaderValueByType[UpgradeToWebSocket](()) {
            case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
            case None ⇒ get {
              ctx: RequestContext =>
                val url = HTTP + lookupService(messagingServiceName) + ":" + messagingPort + DELIMITER + TALK +
                  DELIMITER + targetUserId + "?order=" + order.toLowerCase
                validatedToken(url, ctx, true)
            }
          }
        }
      } ~
        path(Segment) {
          (messageId) =>
            {
              parameters('order ? "desc") { order =>
                optionalHeaderValueByType[UpgradeToWebSocket](()) {
                  case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
                  case None ⇒ get {
                    ctx: RequestContext =>
                      val url = HTTP + lookupService(messagingServiceName) + ":" + messagingPort + DELIMITER + TALK +
                        DELIMITER + targetUserId + DELIMITER + messageId + "?order=" + order.toLowerCase
                      validatedToken(url, ctx, true)
                  }
                }
              }
            }
        }
    }

  // ==============================
  //     SEARCH BY MESSAGE
  // ==============================

  def searchByMessage: Route = path(SEARCHTALKS / SEARCHMSG) {
    parameters('msg) {
      msg =>
        {
          optionalHeaderValueByType[UpgradeToWebSocket](()) {
            case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
            case None ⇒ get {
              ctx: RequestContext =>
                val url = HTTP + lookupService(messagingServiceName) + ":" + messagingPort + DELIMITER + SEARCHTALKS + DELIMITER + SEARCHMSG + "?msg=" + msg
                validatedToken(url, ctx, true)
            }
          }
        }
    }
  }

  // ==============================
  //     SEARCH BY TALKERS
  // ==============================

  def searchByTalkers: Route = path(SEARCHTALKS / SEARCHTALKER) {
    parameters('talkers) {
      talkers =>
        {
          optionalHeaderValueByType[UpgradeToWebSocket](()) {
            case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
            case None ⇒ get {
              ctx: RequestContext =>
                val url = HTTP + lookupService(messagingServiceName) + ":" + messagingPort + DELIMITER + SEARCHTALKS + DELIMITER + SEARCHTALKER + "?talkers=" + talkers
                validatedToken(url, ctx, true)
            }
          }
        }
    }
  }

  // ==============================
  //     GET MY SPOK
  // ==============================
  def getMySpoks: Route = path(MY / SPOKS / Segment) {
    pos =>
      {
        optionalHeaderValueByType[UpgradeToWebSocket](()) {
          case Some(upgrade) ⇒ complete(upgrade.handleMessages(errorResponse, upgrade.requestedProtocols.headOption))
          case None ⇒ get {
            ctx: RequestContext =>
              val url = HTTP + lookupService(spokServiceName) + ":" + spokPort + DELIMITER + MY + DELIMITER + SPOKS + DELIMITER + pos
              validatedToken(url, ctx)
          }
        }
      }
  }

  // ==============================
  //     ALL ROUTES
  // ==============================

  // combination of all routes

  val exceptionHandler = ExceptionHandler {
    case _: StreamTcpException =>
      complete(HttpResponse(StatusCodes.ServiceUnavailable, entity = HttpEntity(
        ContentTypes.`application/json`,
        write(sendFormattedError(SYST_503, SERVICE_UNAVAILABLE, Some(UNAVAILABLE)))
      )))
  }

  def getRoutes: Route = fetchGroups ~ getSpokStats ~ getSpokStack ~ getScopedUsers ~
    getReSpokers ~ viewShortSpok ~ getSpokComment ~ viewFullSpok ~ viewMinimalUserDetail ~ viewFullUserDetail ~
    removeUserFromCache ~ viewSpokersWall ~ getNotificationRoute ~ getFollowers ~ getFollowings ~
    pollQuestionRoute ~ getMyDetails ~ searchByNickname ~ searchByHashtag ~ getPopularSpokers ~ searchLastSpoks ~
    searchFriendSpoks ~ searchTrendySpoks ~ getSpecificGroupDetail ~ launchSearch ~
    updateUserProfile ~ viewPollStats ~ sendMessage ~ viewAllTalks ~ viewSingleTalk ~ searchByMessage ~
    searchByTalkers ~ getMySpoks ~ disableAccount ~ myAccountDisable ~ getUserDetailsByAdmin

  def routes: Route = handleExceptions(exceptionHandler) {
    sampleRoutes ~ getRoutes ~ webSocketRoute ~ otherRoutes
  }

}
