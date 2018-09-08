package parsersTest

/**
 * Created by knoldus on 6/11/17.
 */

import scala.concurrent.duration._

import akka.actor.{ ActorSystem, Props }
import akka.testkit.{ ImplicitSender, TestKit }
import akka.util.Timeout
import model.ParsersResponse.ScapegoatReport
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpecLike }
import service.ParserCalling.Parse
import service._

class ScapegoatParserTest extends TestKit(ActorSystem("MySpec")) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "Scapegoat Parser Actor" must {

    "send back scapegoat parser report" in {
      implicit val timeout = Timeout(2 minute)
      val xmlContent =
        """<scapegoat count="86" warns="9" errors="6" infos="71">
          |</scapegoat>"""
          .stripMargin
      val xmlFile = scala.xml.XML.loadString(xmlContent)
      val system = ActorSystem("Parsers")
      val scapegoatParserActorRef = system.actorOf(Props[ScapegoatParserActor])
      scapegoatParserActorRef ! Parse(xmlFile)

      expectMsgType[Option[ScapegoatReport]]
    }
  }

  "Scapegoat Parser Actor" must {

    "not send back scapegoat parser report" in {
      implicit val timeout = Timeout(2 minute)
      val xmlContent =
        """<scape count="86" warns="9" errors="6" infos="71">
          |</scape>"""
          .stripMargin
      val xmlFile = scala.xml.XML.loadString(xmlContent)
      val system = ActorSystem("Parsers")
      val scapegoatParserActorRef = system.actorOf(Props[ScapegoatParserActor])
      scapegoatParserActorRef ! Parse(xmlFile)

      expectMsgType[Option[ScapegoatReport]]
    }
  }

}

