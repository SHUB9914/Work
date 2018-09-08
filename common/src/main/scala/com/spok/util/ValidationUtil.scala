package com.spok.util

import java.text.{ ParseException, SimpleDateFormat }
import java.util.{ Calendar, Date }

import com.spok.model.Account.{ PhoneNumbers, UserProfileJson }
import com.spok.model.SpokModel.{ Error, Geo }
import com.spok.util.Constant._

import scala.collection.mutable.ListBuffer

/**
 * Used for validation purpose
 */
trait ValidationUtil extends LoggerUtil with ResponseUtil {

  /**
   *
   * @param countryCode
   * @param number
   * @return true if the user phone number is according to the set validations otherwise return false
   */
  def isValidNumber(countryCode: String, number: String): Boolean = {
    val fullNumber = countryCode + number
    val countryCodeResult = countryCode matches COUNTRYCODEPATTERRN
    val phoneNumberResult = number matches PHONENUMBERPATTERN
    val result = fullNumber.length > 9 && fullNumber.length < 16 && phoneNumberResult && countryCodeResult
    result
  }

  def isValidOtp(otpCode: String): Boolean = (otpCode.length == 4)

  /**
   *
   * @param nickname
   * @param birthdate
   * @param locationStatus
   * @param gender
   * @return true and ok message if the details entered in step3 are correct else return false and the apt error message
   */

  def isValidUser(nickname: String, birthdate: String, locationStatus: String, gender: String): (Boolean, Option[List[Error]]) = {

    val checkNickname = validateNickname(nickname)
    val checkBirthdate = isValidDate(birthdate)

    val checkGender = validateGender(gender)
    val checkLocation = validateLocation(locationStatus)

    if (checkNickname && checkBirthdate && checkGender && checkLocation) (true, None)
    else sendInvalidJSONResponse(checkNickname, checkBirthdate, checkGender, checkLocation)
  }

  private def sendInvalidJSONResponse(checkNickname: Boolean, checkBirthdate: Boolean, checkGender: Boolean,
    checkLocation: Boolean): (Boolean, Option[List[Error]]) = {

    val errorList: ListBuffer[Error] = ListBuffer()

    if (!checkNickname) errorList += Error("RGX-003", INVALID_NICKNAME)
    if (!checkBirthdate) errorList += Error("TIME-008", INVALID_BIRTHDATE)
    if (!checkGender) errorList += Error("RGX-004", INVALID_GENDER)
    if (!checkLocation) errorList += Error("RGX-005", "Invalid Location")
    (false, Some(errorList.toList))
  }

  private def isValidDate(birthdate: String): Boolean = {
    try {
      val format = new SimpleDateFormat(DATEFORMAT)
      format.setLenient(false)
      val date = format.parse(birthdate)
      validateDate(date)
    } catch {
      case ex: ParseException => {
        error("Error::", ex)
        false
      }
    }
  }

  /**
   *
   * @param birthdate
   * @return validates users birthdate and returns true if correct else false
   */
  private def validateDate(birthdate: Date): Boolean = {

    val birthDay = Calendar.getInstance
    birthDay.setTime(birthdate)
    val birthYear = birthDay.get(Calendar.YEAR)
    val birthMonth = birthDay.get(Calendar.MONTH)
    val day = birthDay.get(Calendar.DAY_OF_MONTH)

    val today = Calendar.getInstance
    val currYear = today.get(Calendar.YEAR)
    val currMonth = today.get(Calendar.MONTH)
    val currDay = today.get(Calendar.DAY_OF_MONTH)

    val age = if (currMonth < birthMonth || (birthMonth == currMonth && currDay < day)) {
      currYear - birthYear - 1
    } else (currYear - birthYear)

    !(age < 13 || age > 99)
  }

  /**
   *
   * @param nickname
   * @return returns true if users nickname correct else false
   */
  def validateNickname(nickname: String): Boolean = nickname matches NICKNAMEREGEX

  private def validateGender(gender: String): Boolean = gender.equalsIgnoreCase(MALE) || gender.equalsIgnoreCase(FEMALE)

  private def validateLocation(locationStatus: String): Boolean = locationStatus.equals(OK)

  /**
   *
   * @param title
   * @return true if group title is valid
   */
  def isValidTitle(title: String): Option[Error] = {

    val titleLength = title.length
    if (titleLength < 1) Some(Error("RGX-014", GROUP_TITLE_SHORT))
    else if (titleLength > 256) Some(Error("RGX-015", GROUP_TITLE_LONG))
    else None
  }

  /**
   *
   * @param geoLocation
   * @return true if valid geo else false and the corresponding error message
   */
  def validateGeoLocation(geoLocation: Geo): (Boolean, String) = {

    val validLati = isValidLatitude(geoLocation.latitude)
    val validLongi = isValidLongitude(geoLocation.longitude)
    val validElev = isValidElevation(geoLocation.elevation)

    val geoList = List(validLati, validLongi, validElev)

    if (!(geoList contains false)) (true, VALID_SPOK)
    else if (!validLati) (false, (INVALID_LATITUDE))
    else if (!validLongi) (false, (INVALID_LONGITUDE))
    else (false, (INVALID_ELEVATION))
  }

  def isValidLatitude(latitude: Double): Boolean = latitude.toString matches LATITUDEREGEX

  def isValidLongitude(longitude: Double): Boolean = longitude.toString matches LONGITUDEREGEX

  def isValidElevation(elevation: Double): Boolean = elevation.toString matches ELEVATIONREGEX

  /**
   *
   *
   * @param userProfile
   * @return true if valid user profile details else false and the corresponding error message
   */
  def isValidUserProfile(userProfile: UserProfileJson): Option[List[Error]] = {

    val checkNickName: Boolean = validateNickname(userProfile.nickname)
    val checkBirthDate: Boolean = isValidDate(userProfile.birthDate)
    val checkGender: Boolean = validateGender(userProfile.gender)
    val (checkGeo, geoErrorList) = validateGeoLocationForAll(userProfile.geo)
    if (checkNickName && checkBirthDate && checkGender && checkGeo) None
    else sendInvalidUserProfileJSONResponse(checkNickName, checkBirthDate, checkGender, geoErrorList)
  }

  def validateGeoLocationForAll(geoLocation: Geo): (Boolean, Option[List[Error]]) = {

    val validLati = isValidLatitude(geoLocation.latitude)
    val validLongi = isValidLongitude(geoLocation.longitude)
    val validElev = isValidElevation(geoLocation.elevation)
    val geoList = List(validLati, validLongi, validElev)

    if (!(geoList contains false)) (true, None)
    else (false, sendInvalidGeoResponse(validLati, validLongi, validElev))
  }

  def sendInvalidGeoResponse(validLati: Boolean, validLongi: Boolean, validElev: Boolean): Some[List[Error]] = {

    val errorList: ListBuffer[Error] = ListBuffer()
    if (!validLati) errorList += Error("GEO-001", INVALID_LATITUDE)
    if (!validLongi) errorList += Error("GEO-002", INVALID_LONGITUDE)
    if (!validElev) errorList += Error("GEO-003", INVALID_ELEVATION)
    Some(errorList.toList)
  }

  private def sendInvalidUserProfileJSONResponse(checkNickName: Boolean, checkBirthDate: Boolean,
    checkGender: Boolean, geoErrorList: Option[List[Error]]) = {

    val errorList: ListBuffer[Error] = ListBuffer()
    if (!checkNickName) errorList += Error("RGX-003", INVALID_NICKNAME)
    if (!checkBirthDate) errorList += Error("TIME-008", INVALID_BIRTHDATE)
    if (!checkGender) errorList += Error("RGX-004", INVALID_GENDER)
    makeGeoErrorList(errorList, geoErrorList)
    Some(errorList.toList)
  }

  private def makeGeoErrorList(errorList: ListBuffer[Error], geoErrorList: Option[List[Error]]) = {
    geoErrorList match {
      case Some(list) => errorList ++= list
      case None => // Do Nothing
    }
    errorList
  }

  /**
   * Validate phone numbers while updating number.
   *
   * @param phoneNumbers old and new numbers
   * @return true if both number are valid else false
   */
  def validPhoneNumbers(phoneNumbers: PhoneNumbers): Boolean = {
    val oldcounteryCode: String = if (phoneNumbers.oldCountryCode.substring(0, 1) == "+") phoneNumbers.oldCountryCode
    else "+" + phoneNumbers.oldCountryCode
    val newcounteryCode: String = if (phoneNumbers.newCountryCode.substring(0, 1) == "+") phoneNumbers.newCountryCode
    else "+" + phoneNumbers.newCountryCode

    val oldNumber = oldcounteryCode + phoneNumbers.oldNumber
    val isOldCountryCode = oldcounteryCode matches COUNTRYCODEPATTERRN
    val isOldNumber = phoneNumbers.oldNumber matches PHONENUMBERPATTERN
    val newNumber = newcounteryCode + phoneNumbers.newNumber
    val isNewCountryCode = newcounteryCode matches COUNTRYCODEPATTERRN
    val isNewNumber = phoneNumbers.newNumber matches PHONENUMBERPATTERN
    val oldNumberResult = validateOldNumber(oldNumber, isOldCountryCode, isOldNumber)
    val newNumberResult = validateNewNumber(newNumber, isNewCountryCode, isNewNumber)

    oldNumberResult && newNumberResult
  }

  private def validateOldNumber(oldNumber: String, isOldCountryCode: Boolean, isOldNumber: Boolean) =
    oldNumber.length > 9 && oldNumber.length < 16 && isOldCountryCode && isOldNumber

  private def validateNewNumber(newNumber: String, isNewCountryCode: Boolean, isNewNumber: Boolean) =
    newNumber.length > 9 && newNumber.length < 16 && isNewCountryCode && isNewNumber

  /**
   *
   * @param message the message that has to be validated
   * @return true if the message is greater than equal to 20 chars or else false
   */
  def isValidMessage(message: String): Boolean = if (message.length >= 20) true else false

}

