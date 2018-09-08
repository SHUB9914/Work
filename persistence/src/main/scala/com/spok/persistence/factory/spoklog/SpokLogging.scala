package com.spok.persistence.factory.spoklog

import com.datastax.driver.core.ResultSet
import com.spok.persistence.cassandra.CassandraMessageProvider._
import com.spok.persistence.cassandra.CassandraProvider._
import com.spok.persistence.dse.DseGraphFactory
import com.spok.util.JsonHelper
import com.spok.persistence.dse.DseConnectionUri.{ solrMessagingKeyspace, _ }
import org.apache.solr.client.solrj.impl.HttpSolrClient

trait SpokLogging extends JsonHelper {

  /**
   * This function will insert details for spok and spoker events in database.
   *
   * @param json
   * @param tableName
   * @return
   */
  def insertHistory(json: String, tableName: String): ResultSet = {
    cassandraConn.execute(s"INSERT INTO $tableName JSON '$json'")
  }

  def insertHistoryByBinding(json: String, tableName: String): ResultSet = {
    val prepStmt = cassandraConn.prepare(s"INSERT INTO $tableName JSON  ? ")
    cassandraConn.execute(prepStmt.bind(s"$json"))
  }

  def deleteSpokes(tableName: String, spokId: String) = {
    cassandraConn.execute(s"delete from $tableName where spokid in ($spokId) ")
  }
  def deletePopularSpokers(tableName: String, spokerId: String) = {
    cassandraConn.execute(s"delete from $tableName where spokerid = '$spokerId' ")
  }

  def deleteFromSearchTalkers(userId: String): Unit = {
    val solrClient: HttpSolrClient = DseGraphFactory.dseSolrConn(solrMessagingKeyspace, talksDetails)
    solrClient.deleteByQuery(s"user2id:$userId")
    solrClient.commit()
  }

  /**
   * This function will store hashtag in database.
   *
   * @param hashtag
   * @return
   */
  def insertHashTag(hashtag: String, tableName: String): ResultSet = {
    cassandraConn.execute(s"INSERT INTO $tableName (hashtag) VALUES ('${hashtag.toLowerCase}')")
  }

  /**
   * This function will insert details for spoker and messaging details in database.
   *
   * @param json
   * @param tableName
   * @return
   */
  def insertMesssagingDetails(json: String, tableName: String): ResultSet = {
    cassandraMessageConn.execute(s"INSERT INTO $tableName JSON '$json'")
  }

  /**
   * This function will insert and update subscriber details.
   *
   * @param spokId
   * @param userId
   * @param tableName
   * @return
   */
  def upsertSubscriber(spokId: String, userId: String, tableName: String): ResultSet =
    cassandraConn.execute(s"UPDATE $tableName SET userIds = userIds + {'$userId'} WHERE spokId = '$spokId';")

  /**
   * This function will remove subscriber details.
   *
   * @param spokId
   * @param userId
   * @param tableName
   * @return
   */
  def removeSubscriber(spokId: String, userId: String, tableName: String): ResultSet =
    cassandraConn.execute(s"UPDATE $tableName SET userIds = userIds - {'$userId'} WHERE spokId = '$spokId';")

  /**
   * This function will get subscriber details.
   *
   * @param tableName
   * @return
   */
  def fetchDataFromTable(tableName: String): ResultSet =
    cassandraConn.execute(s"SELECT * from $tableName;")

  /**
   *  This function will get the subscriber userid's of a spok
   *
   * @param spokId
   * @param tableName
   * @return
   */
  def isSubscriber(spokId: String, tableName: String) =
    cassandraConn.execute(s"SELECT * from $tableName where spokId = '$spokId';")

}

object SpokLogging extends SpokLogging

