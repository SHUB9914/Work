package com.spok.persistence.cassandra

import com.datastax.driver.core._
import org.scalatest.{ PrivateMethodTester, WordSpec }

import scala.collection.JavaConversions._

class CassandraConnectionParamsSpec extends WordSpec with PrivateMethodTester {

  "A CassandraConnectionParams" should {
    "be able to fetch params from config" in {
      assert(CassandraConnectionUri.port === 9042)
      assert(CassandraConnectionUri.hosts.contains("127.0.0.1"))
      assert(CassandraConnectionUri.keyspace === "eventuate_test")

    }
  }

  "be able to Create a keyspace" in {
    val cluster = new Cluster.Builder().withClusterName("Test Cluster").
      addContactPoints(CassandraConnectionUri.hosts.toArray: _*).
      withPort(CassandraConnectionUri.port).
      withQueryOptions(new QueryOptions().setConsistencyLevel(ConsistencyLevel.QUORUM)).build
    cluster.getConfiguration().getSocketOptions().setReadTimeoutMillis(20000);

    val testKeyspace = "test_keyspace"
    val session = CassandraProvider.connectToKeyspace(s"${testKeyspace}", cluster)
    session.execute(s"DROP KEYSPACE ${testKeyspace};")
    assert(Option(session).isDefined)
  }

  "be able to Create a History keyspace and its related tables" in {
    val session: Session = CassandraProvider.cassandraConn
    val keyspace = session.getLoggedKeyspace
    val tables: ResultSet = session.execute(s"select table_name from system_schema.tables " +
      s" where keyspace_name='" + keyspace + "'")
    val tableNames = tables.all().toList map (_.getString(0))
    assert(tableNames.size > 2)
  }

}