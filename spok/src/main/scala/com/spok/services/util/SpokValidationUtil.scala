package com.spok.services.util

import com.spok.model.SpokModel.{ Error, _ }
import com.spok.util.Constant._
import com.spok.util.ValidationUtil

import scala.collection.mutable.ListBuffer

/**
 * Spok Validation Util
 */
trait SpokValidationUtil extends ValidationUtil {

  /**
   *
   * @param userSpok
   * @return true if all the validations pass else returns false and the error message which corresponds to
   *         the validation that may have failed
   */
  def isValidSpok(userSpok: Spok): (Boolean, Option[List[Error]]) = {

    val (isValidContent, validContent) = validateSpokContentAndVisibility(
      userSpok.contentType,
      userSpok.visibility.getOrElse(PUBLIC), userSpok.groupId.getOrElse("0"), userSpok.file.getOrElse("")
    )
    val (isValidGeo, validGeo) = validateGeoLocationForAll(userSpok.geo)
    val (isValidSpok, validSpok) = validateSpokContent(userSpok)
    if (isValidContent && isValidGeo && isValidSpok) (true, None)
    else {
      makeSpokErrorList(validContent, validGeo, validSpok)
    }
  }

  private def validateSpokContent(userSpok: Spok) = {
    userSpok.contentType match {
      case RAW_TEXT => validateTextSpok(userSpok)
      case URL => validateUrlSpok(userSpok)
      case _ => validatePollOrRiddleSpok(userSpok)
    }
  }

  private def validatePollOrRiddleSpok(userSpok: Spok) = {
    userSpok.contentType match {
      case POLL => validatePollSpok(userSpok)
      case RIDDLE => validateRiddleSpok(userSpok)
      case _ => (true, None)
    }
  }

  private def validateTextSpok(userSpok: Spok) =
    if (!userSpok.text.isEmpty && !userSpok.text.get.equals("")) { (true, None) }
    else { (false, Some(List(Error(RGX_019, INVALID_TEXT)))) }

  private def validateUrlSpok(userSpok: Spok) =
    if (userSpok.url.isEmpty) { (false, Some(List(Error(RGX_008, INVALID_URL)))) }
    else { isValidUrlSpok(userSpok.url.get) }

  private def validatePollSpok(userSpok: Spok) =
    if (userSpok.poll.isEmpty) { (false, Some(List(Error(RGX_017, INVALID_POLL)))) }
    else { isValidPollSpok(userSpok.poll) }

  private def validateRiddleSpok(userSpok: Spok) =
    if (userSpok.riddle.isEmpty) { (false, Some(List(Error(RGX_018, INVALID_RIDDLE)))) }
    else { (true, None) }

  private def makeSpokErrorList(validContent: List[Error], validGeo: Option[List[Error]], validSpok: Option[List[Error]]) = {
    val errorList: ListBuffer[Error] = ListBuffer()
    errorList ++= validContent
    validGeo match {
      case Some(geoError) => errorList ++= geoError
      case None => // do nothing
    }
    validSpok match {
      case Some(spokError) => errorList ++= spokError
      case None => // do noting
    }
    (false, Some(errorList.toList))
  }

  /**
   *
   * @param userUrl
   * @return true if valid url else false and the invalid url message
   */
  private def isValidUrlSpok(userUrl: Url): (Boolean, Option[List[Error]]) = if (userUrl.address matches URLREGEX) {
    validateUrlContent(userUrl.urlType)
  } else (false, Some(List(Error(RGX_008, INVALID_URL))))

  private def isValidPollSpok(poll: Option[Poll]): (Boolean, Option[List[Error]]) = {
    poll match {
      case Some(userPoll) => createPollValidationErrorList(userPoll)
      case None => (true, None)
    }
  }

  private def createPollValidationErrorList(userPoll: Poll) = {
    val (isValidQues, questionErrorList) = validatePollQuestion(userPoll)
    val (isValidAns, answerErrorList) = validatePollAnswer(userPoll)
    val (areQuestionRankValid, questionRankError) = validatePollQuestionRanks(userPoll)
    val (areAnswerRankValid, answerRankError) = validatePollAnswerRanks(userPoll)
    createErrorListForPoll(isValidQues, questionErrorList, isValidAns, answerErrorList,
      areQuestionRankValid, questionRankError, areAnswerRankValid, answerRankError)
  }

  private def validatePollQuestion(userPoll: Poll): (Boolean, Option[Error]) = {
    if (userPoll.questions.length > 20 || userPoll.questions.length < 1) {
      (false, Some(Error(SPK_012, INVALID_QUESTION)))
    } else (true, None)
  }

  private def validatePollAnswer(userPoll: Poll): (Boolean, Option[Error]) = {
    if (checkAnswerLength(userPoll)) {
      (true, None)
    } else (false, Some(Error(SPK_013, INVALID_ANSWER)))
  }

  private def validatePollQuestionRanks(userPoll: Poll): (Boolean, Option[Error]) = {
    val listOfQuestionRank = userPoll.questions.map(quest => quest.rank)
    if ((listOfQuestionRank.size == listOfQuestionRank.toSet.size) && (listOfQuestionRank.max == listOfQuestionRank.size)) {
      (true, None)
    } else (false, Some(Error(RGX_021, "Invalid ranks for poll questions")))
  }

  private def validatePollAnswerRanks(userPoll: Poll): (Boolean, Option[Error]) = {
    val listOfAnswerRank = userPoll.questions.map(quest => {
      val answerRankList = quest.answers.map(_.rank)
      if ((answerRankList.size == answerRankList.toSet.size) && (answerRankList.max == answerRankList.size)) true else false
    })
    if (listOfAnswerRank contains false) (false, Some(Error(RGX_020, "Invalid ranks for poll answers"))) else (true, None)
  }

  private def createErrorListForPoll(isValidQues: Boolean, questionErrorList: Option[Error],
    isValidAns: Boolean, answerErrorList: Option[Error], areQuestionRankValid: Boolean, questionRankError: Option[Error],
    areAnswerRankValid: Boolean, answerRankError: Option[Error]): (Boolean, Option[List[Error]]) = {
    if (isValidQues && isValidAns && areQuestionRankValid && areAnswerRankValid) (true, None) else {
      (false, Some(makePollErrorList(questionErrorList, answerErrorList, questionRankError, answerRankError)))
    }
  }

  private def makePollErrorList(questionErrorList: Option[Error], answerErrorList: Option[Error],
    questionRankError: Option[Error], answerRankError: Option[Error]): List[Error] = {
    val errorList: ListBuffer[Error] = ListBuffer()
    questionErrorList match {
      case Some(questionErrors) => errorList += questionErrors
      case None => // Do Nothing
    }
    answerErrorList match {
      case Some(answerErrors) => errorList += answerErrors
      case None => // Do Nothing
    }
    questionRankError match {
      case Some(questRankErr) => errorList += questRankErr
      case None => // Do Nothing
    }
    answerRankError match {
      case Some(ansRankErr) => errorList += ansRankErr
      case None => // Do Nothing
    }
    errorList.toList
  }

  private def checkAnswerLength(userPoll: Poll) = {
    val answerLengthList = userPoll.questions map (question => if (question.answers.length < 2 || question.answers.length > 10) false else true)
    if (answerLengthList.contains(false)) false else true
  }

  private def validateSpokContentAndVisibility(contentType: String, visibility: String, groupId: String, file: String): (Boolean, List[Error]) = {

    val errorList: ListBuffer[Error] = ListBuffer()
    val contentList = List(PICTURE, ANIMATED_GIF, VIDEO, SOUND, URL, RAW_TEXT, HTML_TEXT, POLL, RIDDLE)
    val mediaContentList = List(PICTURE, ANIMATED_GIF, VIDEO, SOUND)
    if (contentList contains contentType) {
      if ((mediaContentList contains contentType) && (file.length == 0)) errorList += Error(SPK_003, INVALID_MEDIA_FILE)
    } else errorList += Error(RGX_006, INVALID_CONTENT)
    if (!visibility.equalsIgnoreCase(PUBLIC) && (!visibility.equalsIgnoreCase(PRIVATE))) errorList += Error(SPK_133, INVALID_VISIBILITY)
    if (errorList.isEmpty) (true, Nil)
    else (false, errorList.toList)
  }

  private def validateUrlContent(urlType: Option[String]): (Boolean, Option[List[Error]]) = {
    val urlContentList = List(PICTURE, ANIMATED_GIF, VIDEO, SOUND, TEXT)
    urlType match {
      case Some(value) => if (urlContentList contains value) (true, None) else (false, Some(List(Error(RGX_012, INVALID_URL_CONTENT))))
      case None => (true, None)
    }

  }

  /**
   * Validates respok's text and visibility
   *
   * @param interimRespok
   * @return true if validation true else corresponding error message
   */
  private def validateRespokText(interimRespok: InterimRespok): (Boolean, Option[List[Error]]) = {

    interimRespok.text match {
      case Some(textOpt) => isTextValidated(textOpt)
      case None => (true, None)
    }
  }

  private def isTextValidated(text: String): (Boolean, Option[List[Error]]) = {

    if (text.length > 0 && text.length < 65536) (true, None)
    else {
      val errorList: ListBuffer[Error] = ListBuffer()
      if (text.length < 1) errorList += Error(RGX_009, TEXT_SHORT_ERROR)
      if (text.length > 65536) errorList += Error(RGX_010, TEXT_LONG_ERROR)
      (false, Some(errorList.toList))
    }
  }

  /**
   * Validates Respok content
   *
   * @param respokContent
   * @return true if all contents valid else false and the corresponding error message
   */
  def validateRespokDetails(respokContent: InterimRespok): (Boolean, Option[List[Error]]) = {

    val (isValidText, validTextError): (Boolean, Option[List[Error]]) = validateRespokText(respokContent)
    val (isValidGeo, validGeoError): (Boolean, Option[List[Error]]) = validateGeoLocationForAll(respokContent.geo)
    val (isValidVisibility, validVisibilityError) = validateRespokVisibility(respokContent)
    if (isValidText && isValidGeo && isValidVisibility) (true, None)
    else (false, Some(makeRespokErrorList(validTextError, validGeoError, validVisibilityError)))
  }

  private def validateRespokVisibility(interimRespok: InterimRespok): (Boolean, Option[List[Error]]) = {

    val visibilityErrorList = interimRespok.visibility match {
      case Some(optVisibility) => {
        if (!optVisibility.equalsIgnoreCase(PUBLIC) && (!optVisibility.equalsIgnoreCase(PRIVATE))) (false, Some(List(Error(SPK_133, INVALID_VISIBILITY))))
        else (true, None)
      }
      case None => (true, None)
    }
    visibilityErrorList
  }

  private def makeRespokErrorList(validTextError: Option[List[Error]], validGeoError: Option[List[Error]], validVisibilityError: Option[List[Error]]) = {
    val errorList: ListBuffer[Error] = ListBuffer()
    validTextError match {
      case Some(textError) => errorList ++= textError
      case None => // Do Nothing
    }
    validGeoError match {
      case Some(geoError) => errorList ++= geoError
      case None => // Do Nothing
    }
    validVisibilityError match {
      case Some(visibilityError) => errorList ++= visibilityError
      case None => // Do Nothing
    }
    errorList.toList
  }

  /**
   * Validated the unspok content
   *
   * @param unspokContent
   * @return true if the unspok content is valid else false and the corresponding error message
   */
  def validateUnspokDetails(unspokContent: InterimUnspok): (Boolean, Option[List[Error]]) = {

    val validGeo: (Boolean, Option[List[Error]]) = validateGeoLocationForAll(unspokContent.geo)
    validGeo match {
      case (true, message) => (true, None)
      case (false, message) => (false, message)
    }
  }

  def isValidComment(text: String, geo: Geo): Option[List[Error]] = {

    val errorList: ListBuffer[Error] = ListBuffer()
    if (text.length < 1) errorList += Error(RGX_009, TEXT_SHORT_ERROR)
    if (text.length > 65536) errorList += Error(RGX_010, TEXT_LONG_ERROR)
    validateGeoLocationForAll(geo) match {
      case (true, None) => //
      case (false, Some(geoErrorList)) => errorList ++= geoErrorList
    }
    Some(errorList.toList)
  }
}
