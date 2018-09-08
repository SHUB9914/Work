package com.spok.persistence.cassandra

import com.datastax.driver.core._
import com.spok.persistence.dse.DseConnectionUri._
import org.slf4j.LoggerFactory

trait CassandraProvider {

  val logger = LoggerFactory.getLogger(getClass.getName)
  val defaultConsistencyLevel = ConsistencyLevel.valueOf(CassandraConnectionUri.writeConsistency)
  val cassandraConn: Session = {
    val cluster = new Cluster.Builder().withClusterName("Test Cluster").
      addContactPoints(CassandraConnectionUri.hosts.toArray: _*).
      withPort(CassandraConnectionUri.port).
      withQueryOptions(new QueryOptions().setConsistencyLevel(defaultConsistencyLevel)).build
    val session = cluster.connect
    session.execute(s"CREATE KEYSPACE IF NOT EXISTS  ${cassandraKeyspace} WITH REPLICATION = " +
      s"{ 'class' : 'SimpleStrategy', 'replication_factor' : ${CassandraConnectionUri.replicationFactor} }")
    session.execute(s"USE ${cassandraKeyspace}")
    createTables(session)
    session
  }

  def createTables(session: Session): ResultSet = {
    session.execute(s"CREATE TABLE IF NOT EXISTS $spok " +
      s"(spokId text, loggedtime bigint, data text, geoLatitude double, " +
      s"geoLongitude double, geoElevation double, spokerId text," +
      s" eventname text , PRIMARY KEY (spokid, loggedtime))   ")

    session.execute(s"CREATE TABLE IF NOT EXISTS $spoker " +
      s"(spokerId text, loggedtime bigint, data text, geoLatitude double, " +
      s"geoLongitude double, geoElevation double, " +
      s" eventname text , PRIMARY KEY (spokerid, loggedtime))   ")

    session.execute(s"CREATE TABLE IF NOT EXISTS $hashtagTable " +
      s"(hashtag text, PRIMARY KEY (hashtag))")

    session.execute(s"CREATE TABLE IF NOT EXISTS $trendySpok " +
      s"(spokId text, loggedtime bigint, data text, PRIMARY KEY (spokId))")

    session.execute(s"CREATE TABLE IF NOT EXISTS $LaunchSearchTable " +
      s"(userId text, spokId text, spokDetails Text , hashtag Text, " +
      s"geo_lat Double, geo_long Double, launchedTime bigint, contentType Text ," +
      s"primary Key((userId ,spokId ) ,LaunchedTime))  ")

    session.execute(s"CREATE TABLE IF NOT EXISTS $lastSpok " +
      s"(spokId text, loggedtime bigint, data text, PRIMARY KEY (spokId))")

    session.execute(s"CREATE TABLE IF NOT EXISTS $popularSpoker " +
      s"(spokerId text, loggedtime bigint, data text, PRIMARY KEY (spokerId))")

    session.execute(s"CREATE TABLE IF NOT EXISTS $spokerDetails " +
      s"(spokerId text, spokerDetails text, PRIMARY KEY (spokerId))")

    session.execute(s"CREATE TABLE IF NOT EXISTS $subscriberDetails " +
      s"(spokId text, userIds set<text>, PRIMARY KEY (spokId))")

    session.execute(s"CREATE TABLE IF NOT EXISTS $archiveDetails " +
      s"(spokerId text, loggedtime bigint, data text, mobileno text, " +
      s" PRIMARY KEY (spokerId, loggedtime))   ")
  }

}

/**
 * Creates single copy of session to be used across the application.
 */
object CassandraProvider extends CassandraProvider {

  val timeout = 40000
  // Session for Eventuate keyspace
  val session = createSessionAndInitKeyspace(CassandraConnectionUri.keyspace, defaultConsistencyLevel)

  // Shutdown hook clears up connections in case of app shutdown.
  sys addShutdownHook {
    logger.info("Shutdown hook caught.Closing Cassandra session and cluster")
    if (session.isDefined) {
      val cluster = session.get.getCluster
      session.get.close()
      cluster.close()
    }
    logger.info("Shutdown hook executed successfully")
  }

  /**
   * This function connects to the cluster and return the session connecting to the specific keyspace.
   * If keyspace is missing it will create.
   *
   * @param keySpace
   * @param defaultConsistencyLevel
   * @return
   */
  private def createSessionAndInitKeyspace(keySpace: String, defaultConsistencyLevel: ConsistencyLevel = ConsistencyLevel.QUORUM): Option[Session] = {
    val cluster = new Cluster.Builder().withClusterName("Test Cluster").
      addContactPoints(CassandraConnectionUri.hosts.toArray: _*).
      withPort(CassandraConnectionUri.port).
      withQueryOptions(new QueryOptions().setConsistencyLevel(defaultConsistencyLevel)).build
    val session = connectToKeyspace(keySpace, cluster)
    Some(session)
  }

  def connectToKeyspace(keySpace: String, cluster: Cluster): Session = {
    cluster.getConfiguration().getSocketOptions().setReadTimeoutMillis(timeout)
    val session = cluster.connect
    if (CassandraConnectionUri.removeKeyspace) {
      logger.warn("Removing keyspace ....." + keySpace)
      session.execute("DROP KEYSPACE " + keySpace + ";")
    }
    session.execute(s"CREATE KEYSPACE IF NOT EXISTS  ${keySpace} WITH REPLICATION = " +
      s"{ 'class' : 'SimpleStrategy', 'replication_factor' : ${CassandraConnectionUri.replicationFactor} }")
    session.execute(s"USE ${keySpace}")
    session
  }

}

/**
 * Creates single copy of session to be used across search application.
 */
object CassandraSearchProvider extends CassandraProvider

/**
 * Creates single copy of session to be used across messaging application.
 */
object CassandraMessageProvider {

  val timeout = 40000
  val logger = LoggerFactory.getLogger(getClass.getName)
  val defaultConsistencyLevel = ConsistencyLevel.valueOf(CassandraConnectionUri.writeConsistency)
  val cassandraMessageConn: Session = {
    val cluster = new Cluster.Builder().withClusterName("Test Cluster").
      addContactPoints(CassandraConnectionUri.hosts.toArray: _*).
      withPort(CassandraConnectionUri.port).
      withQueryOptions(new QueryOptions().setConsistencyLevel(defaultConsistencyLevel)).build
    cluster.getConfiguration().getSocketOptions().setReadTimeoutMillis(timeout)
    val session = cluster.connect
    session.execute(s"CREATE KEYSPACE IF NOT EXISTS  ${cassandraMessagingKeyspace} WITH REPLICATION = " +
      s"{ 'class' : 'SimpleStrategy', 'replication_factor' : ${CassandraConnectionUri.replicationFactor} }")
    session.execute(s"USE ${cassandraMessagingKeyspace}")
    createTables(session)
    logger.info("session using messaging keyspace" + cassandraMessagingKeyspace)
    session
  }

  def createTables(session: Session): ResultSet = {

    session.execute(s"CREATE TABLE IF NOT EXISTS $spokerDetails " +
      s"(spokerId text, nickname text, gender text, picture text, PRIMARY KEY (spokerId))")

    session.execute(s"CREATE TABLE IF NOT EXISTS $messagingDetails " +
      s"(user1Id Text, user2Id Text , messageId timeUUID, launchedtime bigInt, message Text, read bigInt, senderId Text, Primary Key((user1Id,user2Id),messageId))" +
      s" WITH CLUSTERING ORDER BY (messageId DESC) ")

    session.execute(s"CREATE TABLE IF NOT EXISTS $talksDetails " +
      s"(user1Id Text , user2Id Text , lastMsgTs bigInt, lastMsg Text, senderId text,user2Nickname text,  PRIMARY KEY((user1Id, user2Id), lastMsgTs))" +
      s" WITH CLUSTERING ORDER BY (lastMsgTs DESC)")

  }

}
