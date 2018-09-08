package com.spok.util

import io.igl.jwt._
import org.scalatest.WordSpec

class JWTTokenHelperSpec extends WordSpec with JWTTokenHelper with RandomUtil {

  "JWTTokenHelperSpec" should {

    "be able to create JWT token with role" in {
      val result = createJwtTokenWithRole(getUUID(), "+912345456545", Constant.ADMIN_ROLE)
      val jwt = DecodedJwt.validateEncodedJwt(result, Constant.JWT_SECRET_KEY, Algorithm.HS256, Set(Typ), Set(Iss, PhoneNumber, Role, Iat)).get
      assert(jwt.getClaim[Role].get.value == Constant.ADMIN_ROLE)
      assert(jwt.getClaim[PhoneNumber].get.value == "912345456545")
    }

    "be able to get phone number and userId from JWT token " in {
      val jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo"
      val result = getInfoFromJwtToken(jwt)
      assert(result.get._1 == "5ad25ab8-e44f-4590-8e82-8bf0c974991e")
      assert(result.get._2 == "+33660760376")
    }

    "not be able to get phone number and userId from invalid JWT token " in {
      val jwt = "1234566666"
      val result = getInfoFromJwtToken(jwt)
      assert(!result.isDefined)
    }

    "not be able to get phone number and userId from JWT token  " in {
      val jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJjeXJpbCIsInJvbGUiOiJhZG1pbiIsImV4cCI6MTQ2ODU3NTg1Njc5MSwiaWF0IjoxNDY4NTc1ODQ2Nzk2fQ.aO_aeFAx2xyefDUwh_3RPe5a2HFYHU-_3Hm5VaR_VgQ"
      val result = getInfoFromJwtToken(jwt)
      assert(!result.isDefined)
    }

    "be able to validate JWT token " in {
      val jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI1YWQyNWFiOC1lNDRmLTQ1OTAtOGU4Mi04YmYwYzk3NDk5MWUiLCJwaG9uZV9udW1iZXIiOiIrMzM2NjA3NjAzNzYiLCJyb2xlIjoidXNlciIsImlhdCI6MTQ2ODU3MjE2MDIxNn0._lsHenZoIzKoZHtKGPoNl8nasuY0FksXkNJvUBJIRYo"
      val result = isValid(jwt)
      assert(result)
    }

    "not be able validate JWT token  " in {
      val jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJjeXJpbCIsInJvbGUiOiJhZG1pbiIsImV4cCI6MTQ2ODU3NTg1Njc5MSwiaWF0IjoxNDY4NTc1ODQ2Nzk2fQ.aO_aeFAx2xyefDUwh_3RPe5a2HFYHU-_3Hm5VaR_VgQ"
      val result = isValid(jwt)
      assert(!result)
    }

  }

}
