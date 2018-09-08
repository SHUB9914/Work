import org.mongodb.scala._

import scala.util._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Projections._
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._
import org.bson.codecs.configuration.CodecRegistries.fromCodecs
import org.bson.codecs.configuration.CodecRegistries.{fromCodecs, fromProviders, fromRegistries}
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.bson.types.ObjectId


case class Users(name : String)


object A extends App{

  val mongoClient: MongoClient = MongoClient()


  val codecRegistry = fromRegistries(fromProviders(classOf[Users]), DEFAULT_CODEC_REGISTRY )

  val database: MongoDatabase = mongoClient.getDatabase("users").withCodecRegistry(codecRegistry)




  val coll: MongoCollection[Users] = database.getCollection("students")

  val res = coll.find(equal("name" , "shubham")).projection(fields(include("name"),excludeId())).toFuture()

  val idRes = coll.find(equal("_id" , new ObjectId(""))).projection(fields(include("name"),excludeId())).toFuture()






  res.onComplete{
    case Success(value) =>println(">>>>>>scuccess>>>>"+value)
    case Failure(value) => println(">>>>>>>>failed>>>"+value)
  }



  println(">>>>>>1111>>>>>")

  Thread.sleep(1000)
}
