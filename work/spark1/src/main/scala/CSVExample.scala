import SparkDemo.{Department, _}
import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.{DataFrame, SparkSession}


object CSVExample extends App {


  Logger.getLogger("org").setLevel(Level.OFF)
  Logger.getLogger("akka").setLevel(Level.OFF)
  // Create the case classes for our domain
  case class Department(id: String, name: String)
  case class Employee(firstName: String, lastName: String, email: String, salary: Int)
  case class DepartmentWithEmployees(department: Department, employees: Seq[Employee])

  val department1 =  Department("123456", "Computer Science")
  val employee1 =  Employee("michael", "armbrust", "no-reply@berkeley.edu", 100000)
  val employee2 =  Employee("xiangrui", "meng", "no-reply@stanford.edu", 120000)

  val departmentWithEmployees1 =  DepartmentWithEmployees(department1, Seq(employee1, employee2))

  val sc = SparkSession.builder()
    .appName("CSVExample")
    .master("local[*]").getOrCreate()

  import sc.implicits._

  def getCsv: DataFrame = sc.read
    .option("header", "true")
    .option("inferSchema", "true")
    .csv("/home/shubham/Documents/mycsv.csv")

  def storeCsv(df : DataFrame): Unit ={
    df.write.option("header","true").csv("/home/shubham/Documents/aaaaaa.csv")
//    df.write.format("csv").save("/home/shubham/Documents/department.csv")

//    df.write.format("com.databricks.spark.csv").save("/home/shubham/Documents/mycsv.csv")

  }


  Seq(employee1).toDF.show()

  storeCsv(Seq(employee1).toDF)

}
