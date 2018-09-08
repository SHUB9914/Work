package parsersTest

/**
 * Created by knoldus on 6/11/17.
 */
import scala.concurrent.duration._

import akka.actor.{ ActorSystem, Props }
import akka.testkit.{ ImplicitSender, TestKit }
import akka.util.Timeout
import model.ParsersResponse.ScoverageReport
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpecLike }
import service.ParserCalling.Parse
import service._

class ScoverageParserTest extends TestKit(ActorSystem("MySpec")) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "Scoverage Parser Actor" must {

    "send back Scoverage parser report" in {
      implicit val timeout = Timeout(2 minute)
      val xmlContent = """<scoverage
                         |<statement-count="1823" statements-invoked="1494" statement-rate="81.95" branch-rate="43.33" version="1.0" timestamp="1460027705822">
                         |</scoverage>"""
        .stripMargin
      val xmlFile = scala.xml.XML.loadString(xmlContent)
      val system = ActorSystem("Parsers")
      val scoverageParserActorRef = system.actorOf(Props[ScoverageParserActor])
      scoverageParserActorRef ! Parse(xmlFile)

      expectMsgType[Option[ScoverageReport]]
    }
  }

  "Scoverage Parser Actor" must {

    "not send back Scoverage parser report" in {
      implicit val timeout = Timeout(2 minute)

      val xmlContent = """<scoverage
                          |<statement-count="1823" statements-invoked="1494" statement-rate="81.95" branch-rate="43.33" version="1.0" timestamp="1460027705822">
                          |</scoverage>"""
        .stripMargin
      val xmlFile = scala.xml.XML.loadString(xmlContent)
      val system = ActorSystem("Parsers")
      val scoverageParserActorRef = system.actorOf(Props[ScoverageParserActor])
      scoverageParserActorRef ! Parse(xmlFile)

      expectMsgType[Option[ScoverageReport]]
    }
  }

}

