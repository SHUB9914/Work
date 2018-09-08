package com.spok.util

import org.scalatest.WordSpec

class JsonHelperSpec extends WordSpec with JsonHelper {

  "JsonHelper" should {
    "able to test write method of JsonHelper" in {

      val jsonInput = """{"spokId":"SpokId","ttl":90}"""
      val jValue = parse(jsonInput)
      val result = write(jValue)
      assert(result === jsonInput)
    }
  }
  "able to test parse method " in {
    val jsonInput =
      """{
        "spokId":"SpokId",
        "ttl": 90
        }
      """
    val result = parse(jsonInput)
    assert((result \ "ttl").extract[Int] === 90)
  }

  "able to test extractOrEmptyString method in Positive" in {
    val jsonInput = """{"spokId":"SpokId","ttl":90}"""
    val writeJsonInput = write(jsonInput)
    val parseJson = parse(writeJsonInput)
    val result = extractOrEmptyString(parseJson)
    assert(result === """{"spokId":"SpokId","ttl":90}""")
  }

  "able to test extractOrEmptyString method in Negative" in {
    val parseEmptyString = parse("")
    val result = extractOrEmptyString(parseEmptyString)
    assert(result === "")
  }
}