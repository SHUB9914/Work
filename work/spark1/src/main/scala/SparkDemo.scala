import org.apache.spark.{SparkConf, SparkContext}
import org.apache.log4j.Logger
import org.apache.log4j.Level
import org.apache.spark
import org.apache.spark.sql.catalyst.encoders.RowEncoder
import org.apache.spark.sql.{Column, DataFrame, Row, SparkSession}
import org.apache.spark.sql.functions._



object SparkDemo extends App{
  Logger.getLogger("org").setLevel(Level.OFF)
  Logger.getLogger("akka").setLevel(Level.OFF)


  // Create the case classes for our domain
  case class Department(id: String, name: String)
  case class Employee(firstName: String, lastName: String, email: String, salary: Int)
  case class DepartmentWithEmployees(department: Department, employees: Seq[Employee])

  // Create the Departments
  val department1 =  Department("123456", "Computer Science")
  val department2 =  Department("789012", "Mechanical Engineering")
  val department3 =  Department("345678", "Theater and Drama")
  val department4 =  Department("901234", "Indoor Recreation")

  // Create the Employees
  val employee1 =  Employee("michael", "armbrust", "no-reply@berkeley.edu", 100000)
  val employee2 =  Employee("xiangrui", "meng", "no-reply@stanford.edu", 120000)
  val employee3 =  Employee("matei", null, "no-reply@waterloo.edu", 140000)
  val employee4 =  Employee(null, "wendell", "no-reply@princeton.edu", 160000)

  // Create the DepartmentWithEmployees instances from Departments and Employees
  val departmentWithEmployees1 =  DepartmentWithEmployees(department1, Seq(employee1, employee2))
  val departmentWithEmployees2 =  DepartmentWithEmployees(department2, Seq(employee3, employee4))
  val departmentWithEmployees3 =  DepartmentWithEmployees(department3, Seq(employee1, employee4))
  val departmentWithEmployees4 =  DepartmentWithEmployees(department4, Seq(employee2, employee3))




  val ssc = SparkSession.builder()
    .appName("SparkSessionZipsExample").master("local[*]")
    .config("spark.some.config.option", "some-value")
    .getOrCreate()

  import ssc.implicits._

  val sc = ssc.sparkContext
  val rdd = sc.textFile("/home/shubham/Music/sparkExample/spark-assignment-2-master/src/main/resources/ratings.dat")

  val ratingRdd = rdd.map(_.split("::")).map(arr =>(arr(0) , arr(1) , arr(2).toInt , arr(3)))
    .toDF("userID" , "movieID" , "rating" , "timestamp")


  val movieRdd = sc.textFile("/home/shubham/Music/sparkExample/spark-assignment-2-master/src/main/resources/movies.dat")

  val movieDf: DataFrame = movieRdd.map(_.split("::")).
    map(arr => (arr(0) , arr(1) , arr(2))).
    toDF("MovieID" , "Title" , "Genres")



  def getHigestRatedOfMovieByUser(userId : String , df : DataFrame)={

    df.filter($"userID" === userId).groupBy("movieID").count().sort(desc("count")).show(5)
    df.filter($"userID" === userId).groupBy("movieID").max("")
  }


  def joinExample(ratingDf : DataFrame , moviesDf : DataFrame)={

   /* moviesDf.groupByKey{
      arr => arr(0)
    }.count().show(3)*/

    val res = ratingDf.withColumn("newCol" , when($"MovieId"=== "6" , 1).otherwise(0))

    val data = moviesDf.filter($"Genres" contains "Action")

//    res.show(10)
    ratingDf.join(data , "MovieID").show(2)



//    ratingDf.join(data , "MovieID").groupBy("movieID").avg("rating").sort(desc("avg(rating)")).show(5)
  }




//  getHigestRatedOfMovieByUser("346" , df)

/*  ratingRdd.show(3)
  movieDf.show(5)
  joinExample(ratingRdd , movieDf)*/


/*
  val data = df.map {x =>
   val a =  x.getAs[String]("userID")
   val b =  x.getAs[String]("movieID")
   val c =  x.getAs[String]("rating").toInt
   val d =  x.getAs[String]("timestamp")
    (a,b,c,d)
  }.toDF("userID" , "movieID" , "rating" , "timestamp")

  import org.apache.spark.sql.functions._



  data.groupBy("userID").avg("rating").sort(desc("avg(rating)")).show(3)*/

  val departmentsWithEmployeesSeq1 = Seq(departmentWithEmployees1, departmentWithEmployees2)
  val df1 = departmentsWithEmployeesSeq1.toDF()


  val departmentsWithEmployeesSeq2 = Seq(departmentWithEmployees3, departmentWithEmployees4)
  val df2 = departmentsWithEmployeesSeq2.toDF()

  df1.show()

  Seq(employee1 ,  employee2).toDF().show()

  val explodeDF = df1.explode($"employees") {
    case Row(employee: Seq[Row]) => employee.map{ employee =>
      val firstName = employee(0).asInstanceOf[String]
      val lastName = employee(1).asInstanceOf[String]
      val email = employee(2).asInstanceOf[String]
      val salary = employee(3).asInstanceOf[Int]
      Employee(firstName, lastName, email, salary)
    }
  }.cache()

//  explodeDF.where($"firstName"==="michael").show() // where clause

//  (explodeDF.na).fill("--").show()

  val changeNameValue : Column => Column = (x) => {
    trim(x)


  }

  val udff = (x:String , y:String) => x + y

  val shubhamUdf = udf(udff)







  explodeDF.show()

  explodeDF.withColumn("firstName" , concat(col("firstName"),lit("Shubham"))).show()

  explodeDF.groupBy("firstName").sum("salary").show()
  explodeDF.groupBy("firstName").agg(sum("salary") , avg("salary")).show()


  explodeDF.withColumn("firstName" , shubhamUdf(col("firstName"),col("firstName"))).show()

  explodeDF.toJSON.collect().foreach(println)

//  explodeDF.filter($"firstName" === "" || $"lastName" === "").show()

 /* import org.apache.spark.sql.functions._
  val countDistinctDF = explodeDF.select($"firstName", $"lastName")
  .groupBy($"firstName", $"lastName")
  .agg(countDistinct($"firstName") as "distinct_first_names")

    countDistinctDF.show()

  explodeDF.registerTempTable("databricks_df_example")


  ssc.sql("""
SELECT firstName, lastName, count(*) as distinct_first_names
FROM databricks_df_example
GROUP BY firstName, lastName
""").show()*/


  // Sum up all the salaries
//  val salarySumDF = explodeDF.agg("salary" -> "sum")
  val salarySumDF = explodeDF.agg("salary" -> "sum")

  salarySumDF.show()











}
