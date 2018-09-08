import awscala.dynamodbv2._

object AwsExample extends App {

  implicit val dynamoDB = DynamoDB.local()

  val tableName = "Companies_1516768334254"
  val res: Table = dynamoDB.table(tableName).get

  val res1: Option[Item] = res.get("2")

 // println("========res1==="+res1.get.attributes)

  val names: Seq[String] = res1.get.attributes.map(_.name)
  val values: Seq[Option[String]] = res1.get.attributes.map(_.value.s)

  println("======names==="+names)
  println("======values==="+values)







  def createTable(): Unit ={
    val tableName = s"Companies_${System.currentTimeMillis}"
    val   createdTableMeta: TableMeta = dynamoDB.createTable(
      name = tableName,
      hashPK = "Id" -> AttributeType.String)
    println(s"Created Table: ${createdTableMeta}")
    println(s" Table: ${createdTableMeta.table}")

    dynamoDB.describe(createdTableMeta.table).map { (meta) =>
      println(">>>>>>meta1>>>>>>>>>" + meta.table)
      println(">>>>>>meta2>>>>>>>>>" + meta.attributes)
      println(">>>>>>meta3>>>>>>>>>" + meta.keySchema)
      println(">>>>>>meta4>>>>>>>>>" + meta.itemCount)
      println(">>>>>>meta5>>>>>>>>>" + meta.name)
    }
  }

  def insertDataIntoTables(): Unit ={
    val tableName = "Companies_1516768334254"
    val res: Table = dynamoDB.table(tableName).get

  res.put("1" , "name" -> "shubham")
  res.put("2" , "name" -> "rahul" , "age" -> "23")
  res.put("3")
    //  res.put(10)  -> give exception if we will give primary key with wrong type

  }

}
