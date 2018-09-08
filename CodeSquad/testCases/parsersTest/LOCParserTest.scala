package parsersTest

/**
 * Created by knoldus on 6/11/17.
 */

import scala.concurrent.duration._

import akka.actor.{ ActorSystem, Props }
import akka.testkit.{ ImplicitSender, TestKit }
import akka.util.Timeout
import model.ParsersResponse.LOCReport
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpecLike }
import service.ParserCalling.StringParser
import service._

class LOCParserTest extends TestKit(ActorSystem("MySpec")) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "LOC Parser Actor" must {

    "send back LOC parser report" in {
      implicit val timeout = Timeout(2 minute)
      val xmlContent = List(
        "p3-core/src/main/scala/platform3/models/geo/Geometry.scala     13          3          21",
        "p3-core/src/main/scala/platform3/models/community/CommunityPromoCode.scala    187         11         195",
        "p3-core/src/main/scala/platform3/models/community/CommunityReferral.scala    107         18         122",
        "p3-core/src/main/scala/platform3/models/snap3/SmsPlatformModel.scala     30          0          34",

        "Total code: 16120",
        "Total comments: 1861",
        "Total raw lines: 22738",
        "Comment percentage: 10.349814"
      )

      val system = ActorSystem("Parsers")
      val locParserActorRef = system.actorOf(Props[LOCParserActor])

      locParserActorRef ! StringParser(xmlContent)

      expectMsgType[Option[LOCReport]]
    }
  }

  "LOC Parser Actor" must {

    "not send back LOC parser report" in {
      implicit val timeout = Timeout(2 minute)

      val xmlContent = List(
        "p3-core/src/main/scala/platform3/models/geo/Geometry.scala     13          3          21",
        "p3-core/src/main/scala/platform3/models/community/CommunityPromoCode.scala    187         11         195",
        "p3-core/src/main/scala/platform3/models/community/CommunityReferral.scala    107         18         122",
        "p3-core/src/main/scala/platform3/models/snap3/SmsPlatformModel.scala     30          0          34",
        "Total code: 16120",
        "Total comments: 1861",
        "Total raw lines: 22738",
        "Comment percentage: 10.349814"
      )

      val system = ActorSystem("Parsers")
      val locParserActorRef = system.actorOf(Props[LOCParserActor])
      locParserActorRef ! StringParser(xmlContent)

      expectMsgType[Option[LOCReport]]
    }
  }

}

