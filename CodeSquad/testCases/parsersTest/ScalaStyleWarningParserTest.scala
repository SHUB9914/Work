package parsersTest

/**
 * Created by knoldus on 6/11/17.
 */

import scala.concurrent.duration._

import akka.actor.{ ActorSystem, Props }
import akka.testkit.{ ImplicitSender, TestKit }
import akka.util.Timeout
import model.ParsersResponse.ScalaStyleWarningReport
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpecLike }
import service.ParserCalling.Parse
import service._

class ScalaStyleWarningParserTest extends TestKit(ActorSystem("MySpec")) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "ScalaStyleWarning Parser Actor" must {

    "send back ScalaStyleWarning parser report" in {
      implicit val timeout = Timeout(2 minute)
      val xmlContent = """<checkstyle version="5.0">
                           |</checkstyle>"""
        .stripMargin
      val xmlFile = scala.xml.XML.loadString(xmlContent)
      val system = ActorSystem("Parsers")
      val ref4 = system.actorOf(Props[ScalaStyleWarningParserActor])
      ref4 ! Parse(xmlFile)

      expectMsgType[Option[ScalaStyleWarningReport]]
    }
  }

  "ScalaStyleWarning Parser Actor" must {

    "not send back ScalaStyleWarning parser report" in {
      implicit val timeout = Timeout(2 minute)
      val xmlContent = """<checkstyle version="5.0">
                           |</checkstyle>"""
        .stripMargin
      val xmlFile = scala.xml.XML.loadString(xmlContent)
      val system = ActorSystem("Parsers")
      val scalaStyleWarningParserActorRef = system.actorOf(Props[ScalaStyleWarningParserActor])
      scalaStyleWarningParserActorRef ! Parse(xmlFile)

      expectMsgType[Option[ScalaStyleWarningReport]]
    }
  }

}

