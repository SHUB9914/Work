package com.spok.accountsservice.util

import scala.collection.mutable.Map

trait OTPHelperService {

  def storeOtpWithPhone(phoneNumber: String, otp: String, time: Long, phoneOtpMap: Map[String, (String, Long)]): Boolean = {

    val check = phoneOtpMap.get(phoneNumber)
    check match {
      case Some(value) => false
      case None => {
        phoneOtpMap += (phoneNumber -> (otp, time))
        true
      }
    }
  }

  def validateOtp(phoneNumber: String, otp: String, phoneOtpMap: Map[String, (String, Long)]): Boolean = {

    val storedOtp = phoneOtpMap get (phoneNumber)
    storedOtp match {
      case Some(value) => {
        val currentTime = System.currentTimeMillis()
        val (sOtp, storedTime) = value
        val differenceTime = ((currentTime - storedTime) / (1000 * 60))
        if ((sOtp equals (otp)) && (differenceTime < 5)) {
          true
        } else {
          phoneOtpMap -= phoneNumber
          false
        }
      }
      case None => false
    }

  }
}
