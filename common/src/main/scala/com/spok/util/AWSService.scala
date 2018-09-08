package com.spok.util

import java.io.File

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{ CannedAccessControlList, PutObjectRequest }

trait AWSService {

  val credentials: BasicAWSCredentials
  val amazonS3Client: AmazonS3Client

  /**
   * This method will take file as input and upload it to S3 and will return the key
   *
   * @param file
   * @return
   */
  def uploadFile(file: File): String = {
    LoggerUtil.info("In upload file, File name is = " + file.getName)
    val obj: PutObjectRequest = new PutObjectRequest(ConfigUtil.awsBucket, file.getName, file)
    obj.setCannedAcl(CannedAccessControlList.PublicRead)
    amazonS3Client.putObject(obj)
    amazonS3Client.getResourceUrl(ConfigUtil.awsBucket, obj.getKey)

  }
}

object AWSService extends AWSService {
  val credentials: BasicAWSCredentials = new BasicAWSCredentials(ConfigUtil.awsAccessKey, ConfigUtil.awsSecretKey)
  val amazonS3Client: AmazonS3Client = new AmazonS3Client(credentials)
}
