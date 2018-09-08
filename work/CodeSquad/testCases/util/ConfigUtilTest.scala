
package util

import org.scalatest.FunSuite

/**
 * Created by deepak on 25/7/16.
 */
class ConfigUtilTest extends FunSuite {

  val configUtil = ConfigUtil

  test("One should be able to get the scheduler interval time") {

    val result = configUtil.findSchedulerIntervalFromConfig
    assert(result === 4)
  }

}

