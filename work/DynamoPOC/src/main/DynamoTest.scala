/*

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement
import com.amazonaws.services.dynamodbv2.model.KeyType
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType
import java.util

object DynamoTest extends App{

  //val client: AmazonDynamoDB = AmazonDynamoDBClientBuilder.standard.build

  val client: AmazonDynamoDBClient = new AmazonDynamoDBClient().withEndpoint("http://localhost:8000")
  val dynamoDB = new DynamoDB(client)
  val tableName = "Products"

  try {
    System.out.println("Creating the table, wait...")

    val keySchema: util.List[KeySchemaElement] = util.Arrays.asList(
      new KeySchemaElement("ID", KeyType.HASH), // the partition key
      new KeySchemaElement("Nomenclature", KeyType.RANGE))

    val attributeDef = util.Arrays.asList(new AttributeDefinition("ID", ScalarAttributeType.N), new AttributeDefinition("Nomenclature", ScalarAttributeType.S))
    val table = dynamoDB.createTable(tableName,keySchema, attributeDef, new ProvisionedThroughput(10L, 10L))
    table.waitForActive
    println("Table created successfully.  Status: " + table.getDescription.getTableStatus)
  } catch {
    case e: Exception =>
      System.err.println("Cannot create the table: ")
      System.err.println(e.getMessage)
  }

}
*/
