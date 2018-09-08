package com.spok.util

import com.twilio.sdk.TwilioRestClient
import com.twilio.sdk.resource.instance.Sms
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConversions._
import scala.util.Try

/**
 * Service to connect to Twilio
 */
trait TwilioSMSSender {

  val configFactory = ConfigFactory.load
  val phoneFrom = Some(configFactory.getString("app.sms.phoneNumber"))
  val sid = configFactory.getString("app.sms.id")
  val token = configFactory.getString("app.sms.token")

  def sendSMS(to: String, msg: String): Try[String] = Try {
    LoggerUtil.info(s"Sending SMS to $to with text $msg")

    val params = Map(("Body", msg), ("To", to), ("From", phoneFrom.get))
    val client = new TwilioRestClient(sid, token)
    val messageFactory = client.getAccount.getSmsFactory
    val message: Sms = messageFactory.create(params)

    message.getSid
  }

}
