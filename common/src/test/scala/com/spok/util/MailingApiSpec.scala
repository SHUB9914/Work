package com.spok.util

import org.scalatest.{ Pending, WordSpec }
import com.spok.util.Constant._

class MailingApiSpec extends WordSpec with MailingApi {

  "Mailing Api" should {

    pending
    "return a positive value if the mail is sent successfully" in {
      val result = sendMail(SUPPORT, "Where is the Respok button ?", "roger", "userId")
      assert(result.isDefined)
    }
  }
}
