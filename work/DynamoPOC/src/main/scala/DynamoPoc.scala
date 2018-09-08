
import java.util

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document._
import com.amazonaws.services.dynamodbv2.model._
import java.util.List

import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec

import scala.collection.JavaConversions._
import com.amazonaws.services.dynamodbv2.document.utils.{NameMap, ValueMap}

object DynamoPoc extends App{

  //val client: AmazonDynamoDB = AmazonDynamoDBClientBuilder.standard.build

  val client: AmazonDynamoDBClient = new AmazonDynamoDBClient().withEndpoint("http://localhost:8000")
  val dynamoDB = new DynamoDB(client)

//  val table: Table = dynamoDB.getTable("Companies_1516768334254")

 /* val valueMap: util.HashMap[String, Object] = new util.HashMap[String, Object]()
  valueMap.put(":xxx", 122)*/


/*

  val querySpec = new QuerySpec()
    .withKeyConditionExpression("Id = :xxx")
    .withValueMap(new ValueMap().`with`(":xxx","2"))
*/




/*
  val items: ItemCollection[QueryOutcome] = table.query(querySpec)

  val iterator: scala.List[Item] = items.iterator().toList
  iterator.foreach(value => println(value.getString("name") , value.getString("Id") , value.getString("age")))

  println("======iterator===="+iterator)

*/

//createTable
  putItem


  def putItem = {
    val table = dynamoDB.getTable("users")
    println(s"table ===> ${table}")
    val item = new Item().withPrimaryKey("Id" , 102 , "Name" , "umang")
//    table.putItem(new Item().withPrimaryKey("Id", "1", "Name", "simar"))
    table.putItem(item)
    PutItemRequest

  }

  def createTable: Unit ={

    val tableName = "usersInfo"

//    try {
      println("Creating the table, wait...")

      val keySchema: List[KeySchemaElement] = util.Arrays.asList(
        new KeySchemaElement("Id", KeyType.HASH), // the partition key
        new KeySchemaElement("Name", KeyType.RANGE) ,
        new KeySchemaElement("Age", KeyType.RANGE) ,
        new KeySchemaElement("Address", KeyType.RANGE)
      )

      val attributeDef = util.Arrays.asList(
        new AttributeDefinition("Id", ScalarAttributeType.S),
        new AttributeDefinition("Name", ScalarAttributeType.S) ,
        new AttributeDefinition("Age", ScalarAttributeType.S) ,
        new AttributeDefinition("Address", ScalarAttributeType.S)
      )

      val table: Table = dynamoDB.createTable(tableName,keySchema, attributeDef, new ProvisionedThroughput(10L, 10L))
      table.waitForActive
      println("Table created successfully.  Status: " + table.getDescription.getTableStatus)
   /* } catch {
      case e: Exception =>
       println("Cannot create the table: ")
       println(e.getMessage)
    }*/
    table
  }



}
