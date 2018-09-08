package com.spok.util

import java.awt.image.BufferedImage
import java.io.{ File, FileOutputStream }
import java.util.Base64
import javax.imageio.ImageIO

import com.spok.model.SpokModel.Error
import com.spok.util.Constant._

import scala.collection.mutable.ListBuffer

trait FileUploadUtility extends RandomUtil with LoggerUtil {

  val awsService: AWSService

  /**
   * This is used for pictrue file validation.
   *
   * @param file
   * @return
   */
  private def pictureValidation(file: File): Option[List[Error]] = {
    val path = file.getPath
    val fileSize = file.length
    val bimg: BufferedImage = ImageIO.read(file)
    val width = bimg.getWidth()
    val height = bimg.getHeight()
    val validPicture = ((path.contains('.')) && (List(".jpg", ".png", ".gif", ".jpeg") contains path.substring(path.lastIndexOf('.'), path.length)))
    val isValidFileSize = (fileSize <= (1024 * 1024 * 10))
    val validPictureDimension = (width >= 800 && height >= 300)
    val errorList: ListBuffer[Error] = ListBuffer()
    if (!validPicture) errorList += Error(PIC_001, INVALID_PICTURE_FORMAT)
    if (!isValidFileSize) errorList += Error(PIC_003, INVALID_PICTURE_SIZE)
    if (!validPictureDimension) errorList += Error(PIC_002, INVALID_PICTURE_DIMENSION)
    if (errorList.nonEmpty) Some(errorList.toList)
    else None
  }

  private def fileValidation(file: File): Option[List[Error]] = {
    val fileSize = file.length
    val isValidFileSize = (fileSize <= (1024 * 1024 * 100))
    val errorList: ListBuffer[Error] = ListBuffer()
    if (!isValidFileSize) errorList += Error(SPK_129, INVALID_File_SIZE)
    if (errorList.nonEmpty) Some(errorList.toList)
    else None

  }

  /**
   * This method will upload media file on S3 server
   *
   * @param file
   * @return
   */
  def mediaUpload(file: File): (Option[String], Option[List[Error]]) = {
    fileValidation(file) match {
      case None =>
        val url = awsService.uploadFile(file)
        file.deleteOnExit()
        (Some(url), None)
      case Some(message) =>
        file.deleteOnExit()
        (None, Some(message))
    }
  }
  // $COVERAGE-OFF$
  def uploadProfilePicture(file: File): (Option[String], Option[List[Error]]) = {
    try {
      pictureValidation(file) match {
        case None =>
          val url = awsService.uploadFile(file)
          file.deleteOnExit()
          (Some(url), None)
        case Some(message) =>
          file.deleteOnExit()
          (None, Some(message))
      }
    } catch {
      case ex: Exception =>
        info("Exception while uploading user profile " + ex)
        (None, Some(List(Error(ACT_103, INVALID_BASE64_ENCODED_FILE))))
    }
  }
  // $COVERAGE-ON$
}

object FileUploadUtility extends FileUploadUtility {
  val awsService = AWSService
}
