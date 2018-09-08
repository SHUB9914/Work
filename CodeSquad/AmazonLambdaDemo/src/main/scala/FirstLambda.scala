package example

import scala.collection.JavaConverters._
import java.net.URLDecoder

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.S3Event




object FirstLambda extends App{

  def myHandler(name : String , context : Context): Unit = {

    println("hello :"+name)
  }
}

class MyDecoder {

}
