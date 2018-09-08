package com.spok.util

import java.text.SimpleDateFormat
import java.util.Calendar

import com.spok.model.SpokModel.Error
import org.scalatest.WordSpec

/**
 * Testing the Validation Util
 */
class ValidationUtilSpec extends WordSpec with ValidationUtil {

  val format = new SimpleDateFormat("yyyy-MM-dd")
  val correctLocation = "OK"

  "Validation util" should {

    "return true when the phone number is correct" in {

      val countryCode = "+91"
      val phoneNumber = "95823110"
      assert(isValidNumber(countryCode, phoneNumber))
    }

    "return true when the OTP length is correct" in {
      assert(isValidOtp("1234"))
    }

    "return true when the phone number is correct and exactly 10 digits" in {

      val countryCode = "+91"
      val phoneNumber = "9582311"
      assert(isValidNumber(countryCode, phoneNumber))
    }

    "return true when the phone number is correct and exactly 15 digits" in {

      val countryCode = "+91"
      val phoneNumber = "958231155777"
      assert(isValidNumber(countryCode, phoneNumber))
    }

    "return false when the phone number does not have +" in {

      val countryCode = "91"
      val phoneNumber = "95823110"
      assert(!isValidNumber(countryCode, phoneNumber))
    }

    "return false when the phone number has space" in {

      val countryCode = "+91"
      val phoneNumber = "95823 110"
      assert(!isValidNumber(countryCode, phoneNumber))
    }

    "return false when the phone number is greater than 15 in length" in {

      val countryCode = "+91"
      val phoneNumber = "9582311000000"
      assert(!isValidNumber(countryCode, phoneNumber))
    }

    "return false when the phone number is less than 10 in length" in {

      val countryCode = "+91"
      val phoneNumber = "110999"
      assert(!isValidNumber(countryCode, phoneNumber))
    }

    "return true when the country code starts with 0" in {

      val countryCode = "+01"
      val phoneNumber = "95823110"
      assert(isValidNumber(countryCode, phoneNumber))
    }

    "return true when the phone number starts with 0" in {

      val countryCode = "+91"
      val phoneNumber = "05823110"
      assert(isValidNumber(countryCode, phoneNumber))
    }

    "return false when the phone number starts has dot" in {

      val countryCode = "+9.1"
      val phoneNumber = "0582.3110"
      assert(!isValidNumber(countryCode, phoneNumber))
    }

    "return true when the nickname is correct" in {
      val nickname = "sonu"
      val birthdate = "1992-10-25"
      val location = correctLocation
      val gender = "male"
      val result = isValidUser(nickname, birthdate, location, gender)
      assert(result == (true, None))
    }

    "return true when the nickname has space in between" in {
      val nickname = "sonu mehrotra"
      val birthdate = "1992-10-25"
      val location = correctLocation
      val gender = "male"
      val result = isValidUser(nickname, birthdate, location, gender)
      assert(result == (true, None))
    }

    "return true when the nickname has dash in between" in {
      val nickname = "sonu-mehrotra"
      val birthdate = "1992-10-25"
      val location = correctLocation
      val gender = "male"
      val result = isValidUser(nickname, birthdate, location, gender)
      assert(result == (true, None))
    }

    "return true when the nickname has dot in between" in {
      val nickname = "sonu.mehrotra"
      val birthdate = "1992-10-25"
      val location = correctLocation
      val gender = "male"
      val result = isValidUser(nickname, birthdate, location, gender)
      assert(result == (true, None))
    }

    "return true when the nickname has double quotes in between" in {
      val nickname = """sonu "great" mehrotra"""
      val birthdate = "1992-10-25"
      val location = correctLocation
      val gender = "male"
      val result = isValidUser(nickname, birthdate, location, gender)
      assert(result == (true, None))
    }

    "return true when the nickname has apostrophy in between" in {
      val nickname = "sonu'mehrotra"
      val birthdate = "1992-10-25"
      val location = correctLocation
      val gender = "male"
      val result = isValidUser(nickname, birthdate, location, gender)
      assert(result == (true, None))
    }

    "return false when the nickname is starting with a dash" in {
      val nickname = "-sonumehrotra"
      val birthdate = "1992-10-25"
      val location = correctLocation
      val gender = "male"
      val result = isValidUser(nickname, birthdate, location, gender)
      assert(result == (false, Some(List(Error("RGX-003", "Invalid nickname")))))
    }

    "return false when the nickname is starting with a number" in {
      val nickname = "5sonu"
      val birthdate = "1992-10-25"
      val location = correctLocation
      val gender = "male"
      val result = isValidUser(nickname, birthdate, location, gender)
      assert(result == (false, Some(List(Error("RGX-003", "Invalid nickname")))))
    }

    "return false when the nickname is starting with a special char" in {
      val nickname = "@sonu"
      val birthdate = "1992-10-25"
      val location = correctLocation
      val gender = "male"
      val result = isValidUser(nickname, birthdate, location, gender)
      assert(result == (false, Some(List(Error("RGX-003", "Invalid nickname")))))
    }

    "return false when the nickname only has spaces" in {
      val nickname = "     "
      val birthdate = "1992-10-25"
      val location = correctLocation
      val gender = "male"
      val result = isValidUser(nickname, birthdate, location, gender)
      assert(result == (false, Some(List(Error("RGX-003", "Invalid nickname")))))
    }

    "return false when the nickname only has less than 3 chars" in {
      val nickname = "so"
      val birthdate = "1992-10-25"
      val location = correctLocation
      val gender = "male"
      val result = isValidUser(nickname, birthdate, location, gender)
      assert(result == (false, Some(List(Error("RGX-003", "Invalid nickname")))))
    }

    "return true when the nickname only has 3 chars" in {
      val nickname = "son"
      val birthdate = "1992-10-25"
      val location = correctLocation
      val gender = "male"
      val result = isValidUser(nickname, birthdate, location, gender)
      assert(result == (true, None))
    }

    "return true when age is between 13 and 99" in {
      val date = "1992-10-25"
      val birthdate = format.parse(date)
      val nickname = "sonu"
      val location = correctLocation
      val gender = "male"
      val result = isValidUser(nickname, date, location, gender)
      assert(result == (true, None))
    }

    "return false when age is less than 13" in {
      val today = Calendar.getInstance
      val date = "2008-" + (today.get(Calendar.MONTH) + 1) + "-" + (today.get(Calendar.DAY_OF_MONTH) + 2)
      val nickname = "sonu"
      val location = correctLocation
      val gender = "male"
      val result = isValidUser(nickname, date, location, gender)
      assert(result == (false, Some(List(Error("TIME-008", "Invalid date", None)))))
    }

    "return false when age is more than 99" in {
      val date = "1901-10-25"
      val birthdate = format.parse(date)
      val nickname = "sonu"
      val location = correctLocation
      val gender = "male"
      val result = isValidUser(nickname, date, location, gender)
      assert(result == (false, Some(List(Error("TIME-008", "Invalid date", None)))))
    }

    "return false when date is incorrect" in {
      val date = "190110-25"
      val nickname = "sonu"
      val location = correctLocation
      val gender = "male"
      val result = isValidUser(nickname, date, location, gender)
      assert(result == (false, Some(List(Error("TIME-008", "Invalid date", None)))))
    }

    "return true when all details are correct and gender is female" in {
      val birthdate = "1992-10-25"
      val nickname = "stuti"
      val location = correctLocation
      val gender = "female"
      val result = isValidUser(nickname, birthdate, location, gender)
      assert(result == (true, None))
    }

    "return false when gender is wrong" in {
      val birthdate = "1992-10-25"
      val nickname = "stuti"
      val location = correctLocation
      val gender = "gmale"
      val result = isValidUser(nickname, birthdate, location, gender)
      assert(result == (false, Some(List(Error("RGX-004", "Invalid gender", None)))))
    }

    "return true when location is correct" in {
      val birthdate = "1992-10-25"
      val nickname = "stuti"
      val location = correctLocation
      val gender = "female"
      val result = isValidUser(nickname, birthdate, location, gender)
      assert(result == (true, None))
    }

    "return false when location is not correct" in {
      val birthdate = "1992-10-25"
      val nickname = "stuti"
      val location = "jfsaljfal"
      val gender = "female"
      val result = isValidUser(nickname, birthdate, location, gender)
      assert(result == (false, Some(List(Error("RGX-005", "Invalid Location", None)))))
    }

    "return no error when group title is correct" in {
      val title = "test"
      val result = isValidTitle(title)
      assert(result == (None))
    }

    "return error when group title is too long" in {
      val title = "Title is too long Title is too long Title is too long Title is too long Title is too long Title is too long Title is too long Title is too long Title is too long Title is too long Title is too long Title is too long Title is too long Title is too long Title is too long Title is too long Title is too long Title is too long Title is too long Title is too long Title is too long Title is too long Title is too long Title is too long Title is too long Title is too long Title is too long Title is too long"
      val result = isValidTitle(title)
      assert(result == Some(Error("RGX-015", "Title is too long", None)))
    }

    "return false when group title is too short" in {
      val title = ""
      val result = isValidTitle(title)
      assert(result == Some(Error("RGX-014", "Title is too short", None)))
    }

  }

}
