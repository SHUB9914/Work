
package controllers

import akka.util.Timeout
import org.junit.runner.RunWith
import org.specs2.mock.Mockito
import org.specs2.runner.JUnitRunner
import play.api.test.{ FakeRequest, PlaySpecification, WithApplication }

import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class LogoutControllerTest extends PlaySpecification with Mockito {

  val timeout: Timeout = Timeout(1 minute)
  val logoutController = new LogoutController

  "Logout" should {

    "show the login page on logout" in new WithApplication {
      val result = logoutController.logout.apply(FakeRequest(GET, "/logout").withSession("username" -> ""))
      status(result) must equalTo(303)
    }
  }
}

