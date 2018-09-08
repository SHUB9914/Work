
import akka.actor.{ ActorSystem, Props }
import akka.testkit.{ ImplicitSender, TestKit }
import com.codesquad.util.JsonHelper
import dbServiceUtil.DbServiceApi
import domain.AccountActor
import model.CodeSquadModel.UserInformation
import model.Commands.RegisterUser
import model.SuccessReplies.InsertingUserSuccess
import org.scalatest.{ BeforeAndAfterAll, MustMatchers, WordSpecLike }
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AccountActorSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
    with WordSpecLike with MustMatchers with BeforeAndAfterAll with JsonHelper with MockitoSugar {

  val mockedDbServiceApi: DbServiceApi = mock[DbServiceApi]

  "A AccountActor" must {

    "Persist a user when asked to store user details" in {

      println(">>>>>>>>>>>>>>>>>>>>>>>>.")
      val user = UserInformation("shubham", "shubham@gmail.com", "12345")
      val actorRef = system.actorOf(Props(new AccountActor() {
        override val dbServiceApi = mockedDbServiceApi
      }))
      when(mockedDbServiceApi.insertUser(user)) thenReturn Future(1)
      actorRef ! RegisterUser(user)
      expectMsgType[InsertingUserSuccess](10 seconds)
    }
  }
}
