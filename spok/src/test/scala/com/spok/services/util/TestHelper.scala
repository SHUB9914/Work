package com.spok.services.util

import com.spok.persistence.cassandra.CassandraProvider

trait TestHelper {

  val session = CassandraProvider.session
}
