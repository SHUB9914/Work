package com.spok.services.util

import com.spok.model.SpokModel._
import org.scalatest.WordSpec
import com.spok.util.Constant._

class SpokValidationUtilSpec extends WordSpec with SpokValidationUtil {

  "Spok Validation Util" should {

    "correctly validate text spok details if all details are correct" in {

      val id = "randomId"
      val validSpok = Spok("rawtext", Some("12345"), Some("public"), Some(0), Some("Hello This is instance text"), None,
        Some("Hello This is text spok"), None, None, None, Geo(1, 2, 3), id)

      val result = isValidSpok(validSpok)
      assert(result._1 == (true))
      assert(!result._2.isDefined)
    }

    "return an error message if spok text is not given" in {

      val id = "randomId"
      val validSpok = Spok("rawtext", Some("12345"), Some("public"), Some(0), Some("Hello This is instance text"), None,
        None, None, None, None, Geo(1, 2, 3), id)

      val result = isValidSpok(validSpok)
      assert(result._1 == (false))
      assert(result._2 == Some(List(Error(RGX_019, INVALID_TEXT))))
    }

    "correctly validate url spok details if all details are correct" in {

      val id = "randomId"
      val validSpok = Spok("url", Some("12345"), Some("public"), Some(0), Some("Hello This is instance text"), None,
        None, Some(Url("https://www.google.com", "url_title", "url_text", "url_preview", Some("text"))), None, None, Geo(1, 2, 3), id)

      val result = isValidSpok(validSpok)
      assert(result._1 == (true))
      assert(!result._2.isDefined)
    }

    "return an error message if spok url is not given" in {

      val id = "randomId"
      val validSpok = Spok("url", Some("12345"), Some("public"), Some(0), Some("Hello This is instance text"), None,
        None, None, None, None, Geo(1, 2, 3), id)

      val result = isValidSpok(validSpok)
      assert(result._1 == (false))
      assert(result._2 == Some(List(Error(RGX_008, INVALID_URL))))
    }

    "correctly validate poll spok details if all details are correct" in {

      val id = "randomId"
      val poll = Poll("MyPoll", Some("Check Knowledge"), List(PollQuestions("How many planets are there in the Universe?", Some("text"), Some("preview"), 1, List(PollAnswers("Seven", Some("text"), Some("preview"), 1), PollAnswers("Eight", Some("text"), Some("preview"), 2), PollAnswers("Nine", Some("text"), Some("preview"), 3)))))

      val validSpok = Spok("poll", Some("12345"), Some("public"), Some(0), Some("Hello This is instance text"), None,
        None, None, Some(poll), None, Geo(1, 2, 3), id)

      val result = isValidSpok(validSpok)
      assert(result._1 == (true))
      assert(!result._2.isDefined)
    }

    "return error message if poll question rank is incorrect" in {

      val id = "randomId"
      val poll = Poll("MyPoll", Some("Check Knowledge"),
        List(PollQuestions("How many planets are there in the Universe?", Some("text"), Some("preview"), 2,
          List(
            PollAnswers("Seven", Some("text"), Some("preview"), 1),
            PollAnswers("Eight", Some("text"), Some("preview"), 2),
            PollAnswers("Nine", Some("text"), Some("preview"), 3)
          ))))

      val validSpok = Spok("poll", Some("12345"), Some("public"), Some(0), Some("Hello This is instance text"), None,
        None, None, Some(poll), None, Geo(1, 2, 3), id)

      val result = isValidSpok(validSpok)
      assert(result._1 == (false))
      assert(result._2.isDefined)
    }

    "return error message if poll answer rank is incorrect" in {

      val id = "randomId"
      val poll = Poll("MyPoll", Some("Check Knowledge"),
        List(PollQuestions("How many planets are there in the Universe?", Some("text"), Some("preview"), 1,
          List(
            PollAnswers("Seven", Some("text"), Some("preview"), 1),
            PollAnswers("Eight", Some("text"), Some("preview"), 2),
            PollAnswers("Nine", Some("text"), Some("preview"), 2)
          ))))

      val validSpok = Spok("poll", Some("12345"), Some("public"), Some(0), Some("Hello This is instance text"), None,
        None, None, Some(poll), None, Geo(1, 2, 3), id)

      val result = isValidSpok(validSpok)
      assert(result._1 == (false))
      assert(result._2.isDefined)
    }

    "return error message if both poll answer rank and poll question rank are incorrect" in {

      val id = "randomId"
      val poll = Poll("MyPoll", Some("Check Knowledge"),
        List(PollQuestions("How many planets are there in the Universe?", Some("text"), Some("preview"), 2,
          List(
            PollAnswers("Seven", Some("text"), Some("preview"), 1),
            PollAnswers("Eight", Some("text"), Some("preview"), 2),
            PollAnswers("Nine", Some("text"), Some("preview"), 2)
          ))))

      val validSpok = Spok("poll", Some("12345"), Some("public"), Some(0), Some("Hello This is instance text"), None,
        None, None, Some(poll), None, Geo(1, 2, 3), id)

      val result = isValidSpok(validSpok)
      assert(result._1 == (false))
      assert(result._2.isDefined)
    }

    "return an error message if spok poll is not given" in {

      val id = "randomId"
      val validSpok = Spok("poll", Some("12345"), Some("public"), Some(0), Some("Hello This is instance text"), None,
        None, None, None, None, Geo(1, 2, 3), id)

      val result = isValidSpok(validSpok)
      assert(result._1 == (false))
      assert(result._2 == Some(List(Error(RGX_017, INVALID_POLL))))
    }

    "correctly validate riddle spok details if all details are correct" in {

      val id = "randomId"
      val riddle = Riddle("riddle", RiddleQuestion("What is Pythagoras theorem", Some("riddle"), None),
        RiddleAnswer("b2+p2=h2", Some("riddle"), None))
      val validSpok = Spok("riddle", Some("12345"), Some("public"), Some(0), Some("Hello This is instance text"), None,
        None, None, None, Some(riddle), Geo(1, 2, 3), id)

      val result = isValidSpok(validSpok)
      assert(result._1 == (true))
      assert(!result._2.isDefined)
    }

    "return an error message if spok riddle is not given" in {

      val id = "randomId"
      val validSpok = Spok("riddle", Some("12345"), Some("public"), Some(0), Some("Hello This is instance text"), None,
        None, None, None, None, Geo(1, 2, 3), id)

      val result = isValidSpok(validSpok)
      assert(result._1 == (false))
      assert(result._2 == Some(List(Error(RGX_018, INVALID_RIDDLE))))
    }

    "return an error message if spok text is empty string" in {

      val id = "randomId"
      val validSpok = Spok("rawtext", Some("12345"), Some("public"), Some(0), Some("Hello This is instance text"), None,
        Some(""), None, None, None, Geo(1, 2, 3), id)

      val result = isValidSpok(validSpok)
      assert(result._1 == (false))
      assert(result._2 == Some(List(Error(RGX_019, INVALID_TEXT))))
    }

    "correctly validate respok details if all details are correct" in {

      val validSpok = InterimRespok(Some("0"), Some("Public"), Some("text"), Geo(45.00, 45.00, 45.00), None)
      val result = validateRespokDetails(validSpok)
      assert(result._1 == (true))
      assert(!result._2.isDefined)
    }

    "return an error message if respok text is too short" in {

      val invalidText = InterimRespok(Some("0"), Some("Public"), Some(""), Geo(45.00, 45.00, 45.00), None)
      val result = validateRespokDetails(invalidText)
      val expectedOutput = Some(List(Error("RGX-009", "Text is too short", None)))
      assert(result._1 == (false))
      assert(result._2 == expectedOutput)
    }

    "return an Valid Spok message if respok visibility and group is correct" in {

      val invalidText = InterimRespok(Some("123"), Some("Private"), Some("text"), Geo(45.00, 45.00, 45.00), None)
      val result = validateRespokDetails(invalidText)
      assert(result._1 == (true))
      assert(!result._2.isDefined)
    }

    "return an error message if respok geo latitude is wrong" in {

      val invalidText = InterimRespok(Some("0"), Some("Public"), Some("text"), Geo(91.00, 45.00, 45.00), None)
      val result = validateRespokDetails(invalidText)
      val expectedOutput = Some(List(Error("GEO-001", "Invalid Latitude", None)))
      assert(result._1 == (false))
      assert(result._2 == expectedOutput)
    }

    "return an error message if respok geo longitude is wrong" in {

      val invalidText = InterimRespok(Some("0"), Some("Public"), Some("text"), Geo(89.00, 245.00, 45.00), None)
      val result = validateRespokDetails(invalidText)
      val expectedOutput = Some(List(Error("GEO-002", "Invalid longitude", None)))
      assert(result._1 == (false))
      assert(result._2 == expectedOutput)
    }

    "return an error message if respok geo elevation is wrong" in {

      val invalidText = InterimRespok(Some("0"), Some("Public"), Some("text"), Geo(89.00, 45.00, 245.00), None)
      val result = validateRespokDetails(invalidText)
      val expectedOutput = Some(List(Error("GEO-003", "Invalid elevation", None)))
      assert(result._1 == (false))
      assert(result._2 == expectedOutput)
    }

    "correctly validate respok details if all details are correct and there is no text" in {

      val validSpok = InterimRespok(Some("0"), Some("Public"), None, Geo(45.00, 45.00, 45.00), None)
      val result = validateRespokDetails(validSpok)
      assert(result._1 == (true))
      assert(!result._2.isDefined)
    }

    "return an error message if comment text is too short" in {
      val text = ""
      val geo = Geo(45.00, 45.00, 45.00)
      val result = isValidComment(text, geo)
      val expectedOutput = Some(List(Error("RGX-009", "Text is too short", None)))
      assert(result === expectedOutput)
    }

    "return an error message if comment geo latitude is wrong" in {

      val text = "text"
      val geo = Geo(95.00, 45.00, 45.00)
      val result = isValidComment(text, geo)
      val expectedOutput = Some(List(Error("GEO-001", "Invalid Latitude", None)))
      assert(result === expectedOutput)
    }

    "return an error message if comment geo longitude is wrong" in {

      val text = "text"
      val geo = Geo(89.00, 2245.00, 45.00)
      val result = isValidComment(text, geo)
      val expectedOutput = Some(List(Error("GEO-002", "Invalid longitude", None)))
      assert(result === expectedOutput)
    }

    "return an error message if comment geo elevation is wrong" in {
      val text = "text"
      val geo = Geo(89.00, 45.00, 435.00)
      val result = isValidComment(text, geo)
      val expectedOutput = Some(List(Error("GEO-003", "Invalid elevation", None)))
      assert(result === expectedOutput)
    }

  }
}
