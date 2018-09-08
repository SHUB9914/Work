package com.spok.persistence.dse

import com.typesafe.config.ConfigFactory

object DseConnectionUri {

  val config = ConfigFactory.load("references.conf")

  //DSE Graph Connection
  val keyspace = config.getString("dse.keyspace")
  val pageSize = config.getInt("dse.pagination.spok")
  val groupSize = config.getInt("dse.pagination.group")
  val searchLimit = config.getInt("dse.search.limit")
  val hostName = config.getString("dse.graph.hostname")

  //DSE Cassandra Connection
  val cassandraKeyspace = config.getString("cassandra.keyspace")
  val spok = config.getString("cassandra.tablename.spok")
  val spoker = config.getString("cassandra.tablename.spoker")
  val hashtagTable = config.getString("cassandra.tablename.hashtag")
  val LaunchSearchTable = config.getString("cassandra.tablename.launchSearch")

  //DSE Cassandra Batch View for Search
  val trendySpok = config.getString("cassandra.tablename.trendyspok")
  val lastSpok = config.getString("cassandra.tablename.lastspok")
  val popularSpoker = config.getString("cassandra.tablename.popularSpoker")

  //DSE Solr Connection
  val solrHostname = config.getString("solr.hostname")
  val solrPort = config.getString("solr.port")
  val solrKeyspace = config.getString("solr.keyspace")
  val solrTable = config.getString("solr.tablename")
  val solrMessagingKeyspace = config.getString("solr.message.keyspace")

  //DSE Cassandra for Messaging
  val cassandraMessagingKeyspace = config.getString("cassandra.messaging.keyspace")
  val spokerDetails = config.getString("cassandra.tablename.spokerDetails")
  val messagingDetails = config.getString("cassandra.tablename.messages")
  val talksDetails = config.getString("cassandra.tablename.talks")
  val messagingSearchLimit = config.getInt("dse.messaging.limit")
  val messagingFulltextSearchLimit = config.getInt("dse.messagingFullText.limit")
  val messagingTalkerSearchLimit = config.getInt("dse.messagingTalker.limit")

  //DSE subscriber details
  val subscriberDetails = config.getString("cassandra.tablename.subscriberDetails")
  val archiveDetails = config.getString("cassandra.tablename.userarchivedetails")

}

