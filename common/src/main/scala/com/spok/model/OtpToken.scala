package com.spok.model

import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime

import scala.util.Random

trait OtpToken {
  val otp: String
  val phoneNumber: String
  val expirationTime: DateTime

  def isExpired: Boolean = expirationTime.isBeforeNow
}

case class OtpAuthToken(otp: String, phoneNumber: String, expirationTime: DateTime, retryCount: Int = 0) extends OtpToken

object OtpAuthToken {

  val expirationTimeOtpToken = ConfigFactory.load.getInt("app.otp.AUTH_OTP_TOKEN_EXPIRATION")

  def apply(phoneNumber: String): OtpAuthToken = {
    val nine = 9
    val otp: String = (for (i <- 0 to 3) yield Random.nextInt(nine)).mkString("")
    OtpAuthToken(otp, phoneNumber, new DateTime().plusSeconds(expirationTimeOtpToken))
  }
}
