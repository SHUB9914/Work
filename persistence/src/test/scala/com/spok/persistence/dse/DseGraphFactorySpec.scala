package com.spok.persistence.dse

import com.datastax.driver.dse.DseCluster
import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.scalatest.{ FlatSpec, Matchers }
import com.spok.persistence.dse.DseConnectionUri._

class DseGraphFactorySpec extends FlatSpec with Matchers {

  behavior of "DseGraphFactory "

  it should "be able to connect to DSE graph database using OLTP traversal" in {
    val graphCluster: DseCluster = DseGraphFactory.dseConn.getCluster()
    val oltpSource = graphCluster.getConfiguration.getGraphOptions.getGraphSource
    assert(Option(graphCluster.toString).isDefined)
    assert(oltpSource.equals("g"))
  }

  it should "be able to connect to DSE graph database using OLAP traversal" in {
    val graphCluster: DseCluster = DseGraphFactory.dseOlapConn.getCluster()
    val olapSource = graphCluster.getConfiguration.getGraphOptions.getGraphSource
    assert(Option(graphCluster.toString).isDefined)
    assert(olapSource.equals("a"))
  }

  it should "be able to connect to DSE Solr using SolrJ API" in {
    val solrConn: HttpSolrClient = DseGraphFactory.dseSolrConn("spok_history", "launchsearch")
    val urlString = s"http://$solrHostname:$solrPort/solr/$solrKeyspace.$solrTable"
    assert(solrConn.getBaseURL.equals(urlString))
  }

}
