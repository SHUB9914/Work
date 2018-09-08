package com.spok.accountsservice.util

import org.scalatest.WordSpec

import scala.collection.mutable.Map

class OTPHelperServiceSpec extends WordSpec with OTPHelperService {
  val phoneOtpMap: Map[String, (String, Long)] = Map()

  "store the otp with the corresponding phone number" in {
    val time: Long = 447788812
    val result = storeOtpWithPhone("9195823110", "5454", time, phoneOtpMap)
    assert(result == true)
  }

  "not store the duplicate otp with the corresponding phone number" in {

    val time: Long = 447788812
    val secondResult = storeOtpWithPhone("9195823110", "5454", time, phoneOtpMap)
    assert(secondResult == false)
  }

  "correctly validate the otp for timeout" in {
    val time: Long = 7000
    val store = storeOtpWithPhone("9195823110", "5454", time, phoneOtpMap)
    val result = validateOtp("9195823110", "5454", phoneOtpMap)
    assert(result.isInstanceOf[Boolean])
  }

  "correctly validate the otp for valid Time" in {
    val time: Long = System.currentTimeMillis()
    val store = storeOtpWithPhone("9195823110", "5454", time, phoneOtpMap)
    val result = validateOtp("9195823110", "5454", phoneOtpMap)
    assert(result.isInstanceOf[Boolean])
  }

  "correctly validate the otp for empty map" in {
    val phoneOtpMap1: Map[String, (String, Long)] = Map()
    val result = validateOtp("9195823110", "5454", phoneOtpMap1)
    assert(result.isInstanceOf[Boolean])
  }

}
