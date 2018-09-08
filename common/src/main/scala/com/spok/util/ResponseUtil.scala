package com.spok.util

import com.spok.model.SpokModel._
import com.spok.model.SpokDataResponse
import com.spok.util.Constant._
import com.typesafe.config.ConfigFactory

/**
 * Utility for handling the response formats
 */

trait ResponseUtil {

  val resourceName = ConfigFactory.load.getString("resource")
  def generateCommonResponse(status: String, error: Option[List[Error]], data: Option[String] = None,
    resource: Option[String] = Some(resourceName)): StandardResponseForString = {
    StandardResponseForString(resource, status, error, data)
  }

  def generateCommonResponseForCaseClass(status: String, error: Option[List[Error]], data: Option[SpokDataResponse] = None,
    resource: Option[String] = Some(resourceName)): StandardResponseForCaseClass = {
    StandardResponseForCaseClass(resource, status, error, data)
  }

  def generateCommonResponseForListCaseClass(status: String, error: Option[List[Error]], data: Option[List[SpokDataResponse]] = None,
    resource: Option[String] = Some(resourceName)): StandardResponseForListCaseClass = {
    StandardResponseForListCaseClass(resource, status, error, data)
  }

  def sendFormattedError(errorCode: String, errorMessage: String, resource: Option[String] = Some(resourceName)): StandardResponseForStringError = {
    val error = List(Error(errorCode, errorMessage))
    generateCommonResponseForError(FAILED, Some(error), None, resource = resource)
  }

  def generateCommonResponseForError(status: String, error: Option[List[Error]], data: Option[String] = None,
    resource: Option[String] = Some(resourceName)): StandardResponseForStringError = {
    StandardResponseForStringError(resource, status, error, Some(EmptyData(None)))
  }

  def sendJsonErrorWithEmptyData(resource: Option[String] = Some(resourceName)): StandardResponseForStringError = {
    val error = List(Error(PRS_001, INVALID_JSON))
    generateCommonResponseForError(FAILED, Some(error), None, resource)
  }

}

object ResponseUtil extends ResponseUtil
