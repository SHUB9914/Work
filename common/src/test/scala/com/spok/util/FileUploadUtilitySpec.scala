package com.spok.util

import java.io.File
import java.nio.file.Files

import com.spok.model.SpokModel.Error
import com.spok.util.Constant._
import org.mockito.Mockito._
import org.scalatest.WordSpec
import org.scalatest.mock.MockitoSugar

class FileUploadUtilitySpec extends WordSpec with FileUploadUtility with MockitoSugar {
  override val awsService: AWSService = mock[AWSService]

  "FileUploadUtilitySpec" should {

    "be able to upload file S3 while updating user profile" in {
      val source = new File("src/test/resources/test.jpg")
      val dest = new File("/tmp/test.jpg")
      val ss = Files.copy(source.toPath(), dest.toPath())
      when(awsService.uploadFile(dest)) thenReturn ("S3://url")
      val result = uploadProfilePicture(dest)
      assert(result == (Some("S3://url"), None))
    }

    "not be able to upload file S3 while updating user profile" in {
      val source = new File("src/test/resources/invalid.jpg")
      val dest = new File("/tmp/invalid.bmp")
      val ss = Files.copy(source.toPath(), dest.toPath())
      when(awsService.uploadFile(dest)) thenReturn ("S3://url")
      val result = uploadProfilePicture(dest)
      assert(result == (None, Some(List(Error(PIC_001, INVALID_PICTURE_FORMAT, None), Error(PIC_002, INVALID_PICTURE_DIMENSION, None)))))
    }

    "be able to upload file S3 while creating media type spok" in {
      val file = File.createTempFile("file", "test")
      when(awsService.uploadFile(file)) thenReturn ("S3://url")
      val result = mediaUpload(file)
      assert(result == (Some("S3://url"), None))
    }

  }

}
