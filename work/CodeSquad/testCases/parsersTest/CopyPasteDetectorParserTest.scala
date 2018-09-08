package parsersTest

/**
 * Created by knoldus on 6/11/17.
 */

import scala.concurrent.duration._

import akka.actor.{ ActorSystem, Props }
import akka.testkit.{ ImplicitSender, TestKit }
import akka.util.Timeout
import model.ParsersResponse.CopyPasteDetectorReport
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpecLike }
import service.ParserCalling.Parse
import service._

class CopyPasteDetectorParserTest extends TestKit(ActorSystem("MySpec")) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "Copy Paste Detector Parser Actor" must {

    "send back copy paste detector parser report" in {
      implicit val timeout = Timeout(2 minute)
      val xmlContent =
        """<pmd-cpd>
          | <duplication lines="19" tokens="292">
          | </duplication>
          |</pmd-cpd>""".stripMargin
      val xmlFile = scala.xml.XML.loadString(xmlContent)
      val system = ActorSystem("Parsers")
      val copyPasteDetectorActorRef = system.actorOf(Props[CopyPasteDetectorParserActor])
      copyPasteDetectorActorRef ! Parse(xmlFile)

      expectMsgType[Option[CopyPasteDetectorReport]]
    }
  }

  "Copy Paste Detector Parser Actor" must {

    "not send back copy paste detector parser report" in {
      implicit val timeout = Timeout(2 minute)
      val xmlContent =
        """<pmd>
          | <duplication lines="19" tokens="292">
          | </duplication>
          |</pmd>""".stripMargin
      val xmlFile = scala.xml.XML.loadString(xmlContent)
      val system = ActorSystem("Parsers")
      val copyPasteDetectorActorRef = system.actorOf(Props[CopyPasteDetectorParserActor])
      copyPasteDetectorActorRef ! Parse(xmlFile)

      expectMsgType[Option[CopyPasteDetectorReport]]
    }
  }

}

