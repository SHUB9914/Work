package controllers

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import play.api.test.{ FakeRequest, PlaySpecification, WithApplication }

@RunWith(classOf[JUnitRunner])
class HomePageControllerTest extends PlaySpecification {

  val homePageController = new HomePageController

  "User" should {

    "be able view homepage" in new WithApplication {
      val result = homePageController.home.apply(FakeRequest())
      status(result) must equalTo(200)
    }
  }
}

