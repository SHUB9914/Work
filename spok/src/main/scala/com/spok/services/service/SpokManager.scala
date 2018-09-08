package com.spok.services.service

import akka.actor.{ ActorRef, Props }
import akka.util.Timeout
import com.rbmhtechnology.eventuate.{ EventsourcedActor, EventsourcedView }
import com.spok.persistence.redis.RedisFactory

import scala.collection.mutable.Map
import scala.concurrent.duration._
import com.spok.services.service.SpokManagerCommands._
import com.spok.services.service.SpokActorCommands._
import com.spok.services.service.SpokActorEvents._

case object RedisFilled

class SpokManager(replicaId: String, override val eventLog: ActorRef)
    extends EventsourcedView {

  private implicit val timeout = Timeout(10.seconds)

  val spokActors: Map[String, ActorRef] = Map.empty

  val statsActors: Map[String, ActorRef] = Map.empty
  val spokLogger: SpokLogger = SpokLogger
  val redisFactory: RedisFactory = RedisFactory

  override val id = s"s-sm-$replicaId"

  /**
   * Command handler.
   */
  override def onCommand: Receive = {
    case Create(spok, userId) =>
      spokActor(userId) forward CreateSpok(spok, userId)
    case CreateRespok(respok, spokId, userId, edgeOpt) =>
      spokActor(userId) forward RespokCreate(respok, spokId, userId, edgeOpt)
    case ExecuteUnspok(unspok, spokId, userId, status) =>
      spokActor(userId) forward PerformUnspok(unspok, spokId, userId, status)
    case CreateComment(addComment, spokId, userId) =>
      spokActor(userId) forward CommentAdd(addComment, spokId, userId)
    case UpdateComment(updateComment, userId) =>
      spokActor(userId) forward CommentUpdate(updateComment, userId)
    case RemoveComment(commentId, userId, geo) =>
      spokActor(userId) forward CommentRemove(commentId, userId, geo)
    case SavePollAnswer(questionId, userId, userPollAnswer) =>
      spokActor(userId) forward SaveAnswer(questionId, userId, userPollAnswer)
    case Disable(spokId, userId, launchedTime, geo) =>
      spokActor(userId) forward DisableSpok(spokId, userId, launchedTime, geo)
    case RemoveSpok(spokId, userId, launchedTime, geo) =>
      spokActor(userId) forward RemoveWallSpok(spokId, userId, launchedTime, geo)
    case FillRedisWithSubscriberDetails => {
      spokLogger.fetchSubscriberDetails.map { subscriber =>
        subscriber.userIds.map { userId =>
          redisFactory.storeSubscriber(subscriber.spokId, userId)
        }
      }
    }
    case SaveAllPollAnswers(userId, allAnswers) =>
      spokActor(userId) forward SaveAllAnswersOfPoll(userId, allAnswers)

  }

  /**
   * Event handler.
   */
  override def onEvent: Receive = {
    case SpokCreated(spok, userId) if !spokActors.contains(userId) => spokActor(userId)
  }

  /**
   * Find or create and return the Spok actor by id.
   *
   * @param userId the spok diffusion id.
   * @return the Spok actor ActorRef.
   */
  private def spokActor(userId: String): ActorRef = {
    spokActors.get(userId) match {
      case Some(spokActor) => spokActor
      case None =>
        spokActors += (userId -> context.actorOf(Props(createActor(userId)), userId))
        spokActors(userId)
    }
  }

  def createActor(spokId: String): SpokActor = {
    new SpokActor(spokId, Some(spokId), eventLog)
  }
}
