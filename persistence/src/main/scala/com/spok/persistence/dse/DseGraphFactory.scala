package com.spok.persistence.dse

import com.datastax.driver.dse.graph.{ GraphOptions, SimpleGraphStatement }
import com.datastax.driver.dse.{ DseCluster, DseSession }
import com.spok.persistence.dse.DseConnectionUri._
import com.spok.util.LoggerUtil
import org.apache.solr.client.solrj.impl.HttpSolrClient

object DseGraphFactory {

  val olaptraversal = "a"
  val timeout = 120000

  val dseConn: DseSession = {

    LoggerUtil.info("Connecting with DSE Cluster....")
    val dseCluster: DseCluster = DseCluster.builder()
      .addContactPoint(hostName)
      .withGraphOptions(new GraphOptions().setGraphName(keyspace))
      .build();

    getDseSession(dseCluster)
  }

  val dseOlapConn: DseSession = {

    LoggerUtil.info("Connecting with DSE Cluster....")
    val dseCluster: DseCluster = DseCluster.builder()
      .addContactPoint(hostName)
      .withGraphOptions(new GraphOptions().setGraphName(keyspace).setGraphSource(olaptraversal))
      .build();

    getDseSession(dseCluster)
  }

  private def getDseSession(dseCluster: DseCluster): DseSession = {

    dseCluster.getConfiguration().getSocketOptions().setReadTimeoutMillis(timeout)
    val dseConnSession: DseSession = dseCluster.connect()
    dseConnSession.executeGraph(new SimpleGraphStatement("system.graph('" + keyspace + "').ifNotExists().create()").setSystemQuery())
    dseConnSession.execute("USE " + keyspace)
    val dseConnection: DseSession = dseCluster.connect(keyspace)
    dseConnection
  }

  val dseSolrConn = (solrKeyspace: String, solrTable: String) => {
    val urlString = s"http://$solrHostname:$solrPort/solr/$solrKeyspace.$solrTable"
    new HttpSolrClient.Builder(urlString).build()
  }

}
