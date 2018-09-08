package baile.stream.extension

import java.io.ByteArrayInputStream

import akka.stream.scaladsl.Flow
import akka.util.ByteString
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model._

import scala.collection.JavaConverters._
import scala.concurrent.{ ExecutionContext, Future }

object StreamingService {

  def uploadStreamAsFile(
    client: AmazonS3,
    bucket: String,
    key: String,
    chunkUploadConcurrency: Int = 1
  )(implicit executionContext: ExecutionContext): Flow[ByteString, CompleteMultipartUploadResult, akka.NotUsed] = {
    val request = new InitiateMultipartUploadRequest(bucket, key)
    uploadStreamAsFile(request, chunkUploadConcurrency, client)
  }

  def uploadStreamAsFile(
    intiate: InitiateMultipartUploadRequest,
    chunkUploadConcurrency: Int,
    client: AmazonS3
  )(implicit executionContext: ExecutionContext): Flow[ByteString, CompleteMultipartUploadResult, akka.NotUsed] = {

    val uploadChunkSize = 8 * 1024 * 1024 // recommended by AWS

    def initiateUpload: Future[String] = initiateMultipartUpload(intiate, client).map(_.getUploadId)

    Flow[ByteString]
      .via(FlowExt.rechunkByteStringBySize(uploadChunkSize))
      .via(FlowExt.zipWithConstantLazyAsync(initiateUpload))
      .via(FlowExt.zipWithIndex)
      .mapAsyncUnordered(chunkUploadConcurrency) { case ((bytes, uploadId), partNumber) =>
        println("Uploading part>>>>>>>>>>>>>>>>>" + partNumber)
        val uploadRequest = new UploadPartRequest()
          .withBucketName(intiate.getBucketName)
          .withKey(intiate.getKey)
          .withPartNumber((partNumber + 1).toInt)
          .withUploadId(uploadId)
          .withInputStream(new ByteArrayInputStream(bytes.toArray))
          .withPartSize(bytes.length.toLong)

        uploadPart(uploadRequest,client).map(r => (r.getPartETag, uploadId)).recoverWith {
          case e: Exception =>
            abortMultipartUpload(
              new AbortMultipartUploadRequest(intiate.getBucketName, intiate.getKey, uploadId),
              client
            )
            Future.failed(e)
        }
      }
      .via(FlowExt.fold(Vector.empty[(PartETag, String)])(_ :+ _))
      .mapAsync(1) { etags =>
        etags.headOption match {
          case Some((_, uploadId)) =>
            val compRequest = new CompleteMultipartUploadRequest(
              intiate.getBucketName, intiate.getKey, uploadId, etags.map(_._1).asJava
            )

            val futResult = completeMultipartUpload(compRequest, client).map(Option.apply).recoverWith {
              case e: Exception =>
                abortMultipartUpload(
                  new AbortMultipartUploadRequest(intiate.getBucketName, intiate.getKey, uploadId),
                  client
                )
                Future.failed(e)
            }
            futResult

          case None => Future.successful(None)
        }
      }
      .mapConcat(_.to[scala.collection.immutable.Seq])
  }

  def uploadPart(
    req: UploadPartRequest,
    client: AmazonS3
  )(implicit executionContext: ExecutionContext): Future[UploadPartResult] = {
    println("Uploading part>>>>>>>>>>>>>>>>>" + req )
    Future(client.uploadPart(req))
  }

  def initiateMultipartUpload(
    initiateMultipartUploadRequest: InitiateMultipartUploadRequest,
    client: AmazonS3
  )(implicit executionContext: ExecutionContext): Future[InitiateMultipartUploadResult] =
    Future(client.initiateMultipartUpload(initiateMultipartUploadRequest))

  def abortMultipartUpload(
    req: AbortMultipartUploadRequest,
    client: AmazonS3
  )(implicit executionContext: ExecutionContext): Future[Unit] =
    Future(client.abortMultipartUpload(req))

  def completeMultipartUpload(
    completeMultipartUploadRequest: CompleteMultipartUploadRequest,
    client: AmazonS3
  )(implicit executionContext: ExecutionContext): Future[CompleteMultipartUploadResult] =
    Future(client.completeMultipartUpload(completeMultipartUploadRequest))

}
