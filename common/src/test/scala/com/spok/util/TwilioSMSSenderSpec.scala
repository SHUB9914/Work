package com.spok.util

import org.scalatest.WordSpec

import scala.util.Try

class TwilioSMSSenderSpec extends WordSpec with TwilioSMSSender {

  "Twilio SMS Sender" should {

    "return SID when the message is sent" in {
      val to = "+919711235181"
      val msg = "OTP - 6666"
      val result: Try[String] = sendSMS(to, msg)
      assert(result.isSuccess)
    }

  }

}
