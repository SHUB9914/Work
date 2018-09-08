package com.spok.util

import org.scalatest.WordSpec

class CalcutateDistanceSpec extends WordSpec with CalculateDistance {

  "CalcutateDistance" should {
    "be able to calculate distance between two geo points " in {
      val result = haversineDistance((22.1234, 12.3244), (11.54455, -3.54664))
      assert(result == 2055492.374)
    }
  }
}
