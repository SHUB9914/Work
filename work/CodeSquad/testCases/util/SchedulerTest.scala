
package util

import akka.actor.{ ActorSystem, Props }
import akka.testkit.{ ImplicitSender, TestKit }
import com.google.inject.Inject
import dbservice.SchedulerDbProcess
import org.scalatest.{ BeforeAndAfterAll, FlatSpecLike, MustMatchers }
import play.api.test.WithApplication
import util.Scheduler.{ ScheduleActor, fetchS3Values }

import scala.concurrent.duration._
/**
 * Created by deepak on 24/7/16.
 */
class SchedulerTest @Inject() (schedulerDbProcess: SchedulerDbProcess) extends TestKit(ActorSystem("SchedulerSystem"))
    with FlatSpecLike
    with ImplicitSender
    with BeforeAndAfterAll
    with MustMatchers {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "Scheduler Actor" should "handle insert feature" in new WithApplication {

    val schedulerActor = system.actorOf(Props(classOf[ScheduleActor], schedulerDbProcess))
    implicit val ec = system.dispatcher
    schedulerActor ! fetchS3Values
    expectNoMsg(1.seconds)

  }

}

