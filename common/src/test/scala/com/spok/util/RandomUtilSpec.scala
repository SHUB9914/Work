package com.spok.util

import org.scalatest.WordSpec

class RandomUtilSpec extends WordSpec with RandomUtil {
  "RandomUtil" should {
    "able to get random number" in {
      assert(getUUID().length > 0)
    }
  }
}
