package com.spok.util

import com.spok.util.Constant._

import scala.util.Random

/**
 * Generating the OTP for mobile number verification
 */

trait OtpGenerationUtil {

  def generateOtp: (String, Long) = {

    val otpToken: String = (for (i <- 0 to 3) yield Random.nextInt(NINE)).mkString("")
    val time = System.currentTimeMillis()

    (otpToken, time)
  }
}
