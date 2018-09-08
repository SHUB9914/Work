package com.spok.boot.sbt

import com.spok.persistence.cassandra.CassandraSearchProvider
import com.spok.scheduler.Scheduler
import com.spok.util.LoggerUtil

object BootStrap extends LoggerUtil {

  def main(args: Array[String]): Unit = {
    CassandraSearchProvider

    info("<<Scheduler is started>>")
    Scheduler.start()
  }

}
