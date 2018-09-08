package integrations

import java.util.concurrent.TimeUnit
import org.openqa.selenium.WebDriver
import org.openqa.selenium.firefox.{ FirefoxDriver, FirefoxProfile }
import play.api.test.{ PlaySpecification, _ }

class IntegrationTest extends PlaySpecification {

  def firefoxWebDriver: WebDriver = {
    val firefoxProfile = new FirefoxProfile()
    firefoxProfile.setPreference("startup.homepage_welcome_url.additional", "")
    new FirefoxDriver(firefoxProfile)
  }

  val time = 10

  "User" should {
    /* "be able to go to login page" in new WithBrowser(firefoxWebDriver) {
      browser.goTo("/login")
      browser.manage.timeouts().implicitlyWait(time, TimeUnit.SECONDS)
      browser.pageSource must contain("Login")
    }

    "be able to get the correct project name" in new WithBrowser(firefoxWebDriver) {
      browser.goTo("/login")
      browser.$("#username").text("knoldus")
      browser.$("#password").text("kNoLdUs@18")
      browser.$(".btn").click()
      browser.manage.timeouts().implicitlyWait(time, TimeUnit.SECONDS)
      browser.pageSource must contain("project-bi6")
    }

    "be able to get the number of warnings" in new WithBrowser(firefoxWebDriver) {
      browser.goTo("/login")
      browser.$("#username").text("knoldus")
      browser.$("#password").text("kNoLdUs@18")
      browser.$(".btn").click()
      browser.manage.timeouts().implicitlyWait(time, TimeUnit.SECONDS)
      browser.pageSource must contain("127")
    }

    "get the correct project code analysis for warns" in new WithBrowser(firefoxWebDriver) {
      browser.goTo("/login")
      browser.$("#username").text("knoldus")
      browser.$("#password").text("kNoLdUs@18")
      browser.$(".btn").click()
      browser.manage.timeouts().implicitlyWait(time, TimeUnit.SECONDS)
      browser.pageSource must contain("213")
    }
<<<<<<< HEAD
=======

>>>>>>> 65118089cb3506f7ddd65fe8559d5b736bca4b32
    "get the correct project code analysis for infos" in new WithBrowser(firefoxWebDriver) {
      browser.goTo("/login")
      browser.$("#username").text("knoldus")
      browser.$("#password").text("kNoLdUs@18")
      browser.$(".btn").click()
      browser.manage.timeouts().implicitlyWait(time, TimeUnit.SECONDS)
      browser.pageSource must contain("261")
    }

    "get the correct test coverage" in new WithBrowser(firefoxWebDriver) {
      browser.goTo("/login")
      browser.$("#username").text("knoldus")
      browser.$("#password").text("kNoLdUs@18")
      browser.$(".btn").click()
      browser.manage.timeouts().implicitlyWait(time, TimeUnit.SECONDS)
      browser.pageSource must contain("31.35")
    }

    "get the correct copy paste detector report" in new WithBrowser(firefoxWebDriver) {
      browser.goTo("/login")
      browser.$("#username").text("knoldus")
      browser.$("#password").text("kNoLdUs@18")
      browser.$(".btn").click()
      browser.manage.timeouts().implicitlyWait(time, TimeUnit.SECONDS)
      browser.pageSource must contain("0")
    }

    "get the danger image on missing dependencies" in new WithBrowser(firefoxWebDriver) {
      browser.goTo("/login")
      browser.$("#username").text("knoldus")
      browser.$("#password").text("kNoLdUs@18")
      browser.$(".btn").click()
      browser.manage.timeouts().implicitlyWait(time, TimeUnit.SECONDS)
      browser.pageSource must contain("img")
    }

    "get the last modified date of the reports" in new WithBrowser(firefoxWebDriver) {
      browser.goTo("/login")
      browser.$("#username").text("knoldus")
      browser.$("#password").text("kNoLdUs@18")
      browser.$(".btn").click()
      browser.manage.timeouts().implicitlyWait(time, TimeUnit.SECONDS)
      browser.pageSource must contain("2016")
    }
<<<<<<< HEAD
=======

>>>>>>> 65118089cb3506f7ddd65fe8559d5b736bca4b32
    "User able to registerd in codesquad with valid username and valid email" in new WithBrowser(firefoxWebDriver) {
      browser.goTo("/registerForm")
      browser.$("#username").text("knoldus2")
      browser.$("#email").text("a@knoldus.com")
      browser.$("#password").text("kNoLdUs@18")
      browser.$("#confirmPassword").text("kNoLdUs@18")
      browser.$("#registerForm").submit()
      browser.manage.timeouts().implicitlyWait(time, TimeUnit.SECONDS)
      browser.pageSource must contain("welcomeknoldus")
    }

    "User not able to registraterd in codesquad with existing username and valid email  " in new WithBrowser(firefoxWebDriver) {

      browser.goTo("/registerForm")
      browser.$("#username").text("knoldus")
      browser.$("#email").text("ad@knoldus.com")
      browser.$("#password").text("kNoLdUs@18")
      browser.$("#confirmPassword").text("kNoLdUs@18")
      browser.$("#registerForm").submit()
      browser.manage.timeouts().implicitlyWait(time, TimeUnit.SECONDS)
      browser.pageSource must contain("This username is already exist.")

    }
    "User not able to registraterd in codesquad with valid username and invalid email  " in new WithBrowser(firefoxWebDriver) {

      browser.goTo("/registerForm")
      browser.$("#username").text("knoldus3")
      browser.$("#email").text("@knoldus.com")
      browser.$("#password").text("kNoLdUs@18")
      browser.$("#confirmPassword").text("kNoLdUs@18")
      browser.$("#registerForm").submit()
      browser.manage.timeouts().implicitlyWait(time, TimeUnit.SECONDS)
      browser.pageSource must contain("Please enter a valid email address.")

    }
    "User not able to registraterd in codesquad when password and confirm password not matched " in new WithBrowser(firefoxWebDriver) {

      browser.goTo("/registerForm")
      browser.$("#username").text("knoldus3")
      browser.$("#email").text("@knoldus.com")
      browser.$("#password").text("kNoLdUs@181")
      browser.$("#confirmPassword").text("kNoLdUs@18")
      browser.$("#registerForm").submit()
      browser.manage.timeouts().implicitlyWait(time, TimeUnit.SECONDS)
      browser.pageSource must contain("Please enter a valid email address.")

    }
    "User not able to registraterd in codesquad with empty value in textbox " in new WithBrowser(firefoxWebDriver) {

      browser.goTo("/registerForm")
      browser.$("#username").text("")
      browser.$("#email").text("")
      browser.$("#password").text("")
      browser.$("#confirmPassword").text("")
      browser.$("#registerForm").submit()
      browser.manage.timeouts().implicitlyWait(time, TimeUnit.SECONDS)
      browser.pageSource must contain("Please enter a username")
    }*/

    "User able to add  new project in codesquad" in new WithBrowser(firefoxWebDriver) {

      browser.goTo("/registerForm")
      browser.manage.window().maximize()
      browser.$("#username").text("dummy_data2111")
      browser.$("#email").text("dummy@gmail.com")
      browser.$("#password").text("123456789")
      browser.$("#confirmPassword").text("123456789")
      browser.$("#registerForm").submit()
      browser.manage.timeouts().implicitlyWait(time, TimeUnit.SECONDS)
      browser.pageSource must contain("Create new project")
      browser.manage.timeouts().implicitlyWait(time, TimeUnit.SECONDS)
      browser.$("#projectName").text("tomleei1w1")
      browser.$("#register").click()
      browser.manage.timeouts().implicitlyWait(time, TimeUnit.SECONDS)
      browser.pageSource must contain("dummy_data2111")
    }

    "User not able to add existing project in codesquad" in new WithBrowser(firefoxWebDriver) {

      browser.goTo("/registerForm")
      browser.manage.window().maximize()
      browser.$("#username").text("dummy_data3111")
      browser.$("#email").text("dummy@gmail.com")
      browser.$("#password").text("123456789")
      browser.$("#confirmPassword").text("123456789")
      browser.$("#registerForm").submit()
      browser.manage.timeouts().implicitlyWait(time, TimeUnit.SECONDS)
      browser.pageSource must contain("Create new project")
      browser.manage.timeouts().implicitlyWait(time, TimeUnit.SECONDS)
      browser.$("#projectName").text("tomlee")
      browser.$("#register").click()
      browser.manage.timeouts().implicitlyWait(time, TimeUnit.SECONDS)
      browser.pageSource must contain("Project is already setup in another account by project admin. " +
        "To view Dashboard, request project admin to add you as team member.")
    }

    "User not able to add  empty project in codesquad" in new WithBrowser(firefoxWebDriver) {

      browser.goTo("/registerForm")
      browser.manage.window().maximize()
      browser.$("#username").text("dummy_data41111")
      browser.$("#email").text("dummy@gmail.com")
      browser.$("#password").text("123456789")
      browser.$("#confirmPassword").text("123456789")
      browser.$("#registerForm").submit()
      browser.manage.timeouts().implicitlyWait(time, TimeUnit.SECONDS)
      browser.pageSource must contain("Create new project")
      browser.manage.timeouts().implicitlyWait(time, TimeUnit.SECONDS)
      browser.$("#projectName").text("")
      browser.$("#register").click()
      browser.manage.timeouts().implicitlyWait(time, TimeUnit.SECONDS)
      browser.pageSource must contain("Please enter a Project name")
    }

    "User not able to add special character like @ - in project name in codesquad" in new WithBrowser(firefoxWebDriver) {

      browser.goTo("/registerForm")
      browser.manage.window().maximize()
      browser.$("#username").text("dummy_data5111")
      browser.$("#email").text("dummy@gmail.com")
      browser.$("#password").text("123456789")
      browser.$("#confirmPassword").text("123456789")
      browser.$("#registerForm").submit()
      browser.manage.timeouts().implicitlyWait(time, TimeUnit.SECONDS)
      browser.pageSource must contain("Create new project")
      browser.manage.timeouts().implicitlyWait(time, TimeUnit.SECONDS)
      browser.$("#projectName").text("demo-test")
      browser.$("#register").click()
      browser.manage.timeouts().implicitlyWait(time, TimeUnit.SECONDS)
      browser.pageSource must contain("Letters, numbers, and underscores only please")
    }
  }
}
