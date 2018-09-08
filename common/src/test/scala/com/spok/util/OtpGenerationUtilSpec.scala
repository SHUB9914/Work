package com.spok.util

import org.scalatest.WordSpec

/**
 * Testing the Otp generation spec.
 */
class OtpGenerationUtilSpec extends WordSpec with OtpGenerationUtil {

  "Otp generation util" should {

    "generate a four digit OTP" in {

      val result = generateOtp
      assert(result._1.length == 4)
    }

    "generate a unique four digit OTP" in {

      val firstOtp = generateOtp
      val secondOtp = generateOtp
      assert((firstOtp equals (secondOtp)) == false)
    }
  }

}
