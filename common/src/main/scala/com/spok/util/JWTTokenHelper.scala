package com.spok.util

import io.igl.jwt._
import play.api.libs.json.{ JsString, JsValue }

trait JWTTokenHelper {

  case class Role(value: String) extends ClaimValue {
    override val field: ClaimField = Role
    override val jsValue: JsValue = JsString(value)
  }

  object Role extends ClaimField {
    override def attemptApply(value: JsValue): Option[ClaimValue] =
      value.asOpt[String].map(apply)

    override val name = Constant.ROLE
  }

  case class PhoneNumber(value: String) extends ClaimValue {
    override val field: ClaimField = PhoneNumber
    override val jsValue: JsValue = JsString(value)
  }

  object PhoneNumber extends ClaimField {
    override def attemptApply(value: JsValue): Option[ClaimValue] =
      value.asOpt[String].map(apply)

    override val name = Constant.PHONE_NUMBER
  }

  /**
   * This method returns JWT token
   *
   * @param userId
   * @param role
   * @return
   */
  def createJwtTokenWithRole(userId: String, phoneNumber: String, role: String): String = {
    val jwt = new DecodedJwt(Seq(Alg(Algorithm.HS256), Typ(Constant.JWT_TOKEN_TYPE)), Seq(
      Iss(userId),
      PhoneNumber(phoneNumber.substring(1)), Role(role), Iat(System.currentTimeMillis())
    ))
    jwt.encodedAndSigned(Constant.JWT_SECRET_KEY)
  }

  /**
   * isVerify validates JWt token and returns userId and phoneNumber.
   *
   * @param jwtString
   * @return
   */
  def getInfoFromJwtToken(jwtString: String): Option[(String, String)] = {
    val jwt = DecodedJwt.validateEncodedJwt(jwtString, Constant.JWT_SECRET_KEY, Algorithm.HS256, Set(Typ), Set(Iss, PhoneNumber, Role, Iat))
    jwt.toOption match {
      case Some(jwtToken) => Some((jwtToken.getClaim[Iss].get.value, jwtToken.getClaim[PhoneNumber].get.value))
      case None => None
    }
  }

  def isValid(jwtString: String): Boolean = {
    val jwt = DecodedJwt.validateEncodedJwt(jwtString, Constant.JWT_SECRET_KEY, Algorithm.HS256, Set(Typ), Set(Iss, PhoneNumber, Role, Iat))
    jwt.toOption match {
      case Some(jwtToken) => true
      case None => false
    }
  }

}

object JWTTokenHelper extends JWTTokenHelper
