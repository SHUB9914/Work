

package util

import java.io.InputStreamReader

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{ GetObjectRequest, S3Object }
import com.sun.jersey.api.client.ClientResponse
import com.typesafe.config.ConfigFactory
import org.scalatest.FunSuite
import org.specs2.mock.Mockito

import scala.xml.Elem
import scala.xml.XML._

class CommonUtilTest extends FunSuite {

  val conf = ConfigFactory.load()
  val accessKey = conf.getString("access-key")
  val secretKey = conf.getString("secret-key")

  val credentials = new BasicAWSCredentials(accessKey, secretKey)
  val s3client = new AmazonS3Client(credentials)
  val commonUtil = CommonUtil
  val configUtil = ConfigUtil
  val constant = Constant
  val projectPath = "codesquadtest" + constant.DELIMITER + "project-bi6" + constant.DELIMITER + "main"
  val version = "scala-2.11"
  val recipient = "abhishek@knoldus.com"
  val subject = constant.SUBJECT
  val message = "Test Email"
  val s3object: S3Object = s3client.getObject(new GetObjectRequest(
    "codesquadtest",
    "project-bi6" + constant.DELIMITER + "main/" + constant.TARGET + version + constant.SCAPEGOATPATH.substring(6)
  ))
  val elem: Elem = load(new InputStreamReader(s3object.getObjectContent))

  test("One should be able to get Scapegoat warning") {
    val result = commonUtil.findScapegoatWarnings(elem)
    assert(result === "213")
  }

  test("One should be able to get Scapegoat errors") {
    val result = commonUtil.findScapegoatError(elem)
    assert(result === "178")
  }

  test("One should be able to get Scapegoat infos") {
    val result = commonUtil.findScapegoatInfos(elem)
    assert(result === "261")
  }

  test("one should able to receive email notification") {
    val sendEmail = commonUtil.sendEmail(recipient, subject, message)
    val result = sendEmail.fold("failure")(response => "success")
    assert(result === "success")
  }

}

