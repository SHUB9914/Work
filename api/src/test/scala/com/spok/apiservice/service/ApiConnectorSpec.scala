package com.spok.apiservice.service

import akka.actor.{ ActorSystem, Props }
import akka.testkit.{ ImplicitSender, TestKit }
import com.spok.util.JsonHelper
import org.scalatest._
import org.scalatest.mock.MockitoSugar

class ApiConnectorSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
    with WordSpecLike with MustMatchers with BeforeAndAfterAll with MockitoSugar with JsonHelper {

  def this() = this(ActorSystem("ApiActorSystem"))

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  "ApiConnector" must {

    "be able to disconnect all the services" in {
      val actorRef = system.actorOf(Props(new ApiConnector))
      actorRef ! DisconnectService(Some("1234"))
      expectNoMsg()
    }
  }
}
