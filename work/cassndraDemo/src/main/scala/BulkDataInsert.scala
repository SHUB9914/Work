import java.nio.ByteBuffer

import com.datastax.driver.core.{Cluster, Session}
import com.typesafe.config.{ConfigFactory, ConfigList}
import org.slf4j.LoggerFactory

import scala.util.Random



object BulkDataInsert extends CassandraProvider with App {

  val log = LoggerFactory.getLogger(this.getClass)
  val query = "create table IF NOT EXISTS users(id text , name text , primary key(id))"
  val satement1 = "insert into users(id , name) values('e001','shubham')"
  val satement2 = "insert into users(id , name) values('e002','rahul')"

  /* cassandraConn.execute(query)
   for(i<- 100000 to 1000000)
   cassandraConn.executeAsync(s"insert into users(id , name) values('${i.toString}','shubham')")*/

  val res = cassandraConn.execute(s"SELECT * FROM mykeyspace.users where id='2'")
  val iterate = res.iterator()
  while (iterate.hasNext) {

    println(iterate.next())

  }

  log.info("completed the inserts")
  cassandraConn.close()
}

trait CassandraProvider {
  val logger = LoggerFactory.getLogger(getClass.getName)
  val config = ConfigFactory.load()
  val cassandraKeyspace = config.getString("cassandra.keyspace.rawFiles")
  val cassandraHostname = config.getString("cassandra.contact.contact-points-str")

  val cassandraConn: Session = {
    println("===cassandraHostname====="+cassandraHostname)
    val cluster = new Cluster.Builder().withClusterName("Test Cluster").
      addContactPoints(cassandraHostname).build
    val session = cluster.connect
    session.execute(s"CREATE KEYSPACE IF NOT EXISTS  ${cassandraKeyspace} WITH REPLICATION = " +
      s"{ 'class' : 'SimpleStrategy', 'replication_factor' : 2 }")
    session.execute(s"USE ${cassandraKeyspace}")
    // Optional method that can be implemented if table creation scripts are required
    //createTables(session)
    session
  }
}