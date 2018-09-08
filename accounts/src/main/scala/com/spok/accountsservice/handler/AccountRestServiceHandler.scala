package com.spok.accountsservice.handler

import java.io
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.model.ws.TextMessage.Strict
import akka.http.scaladsl.model.ws.{ Message, TextMessage }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import com.spok.accountsservice.service._
import com.spok.model.Account._
import com.spok.model.SpokModel.{ Error, Geo }
import com.spok.model.{ Location, OtpAuthToken }
import com.spok.persistence.dse.DseGraphFactory
import com.spok.persistence.factory.DSEGraphPersistenceFactoryApi
import com.spok.persistence.factory.messaging.MessagingApi
import com.spok.persistence.factory.spokgraph.DSESpokApi
import com.spok.persistence.redis.RedisFactory
import com.spok.util.Constant._
import com.spok.util._
import net.liftweb.json.JsonAST.JNothing

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import com.spok.accountsservice.service.AccountActorCommand._
import com.spok.accountsservice.service.AccountManagerCommands._
import com.spok.accountsservice.service.AccountActorSuccessReplies._
import com.spok.accountsservice.service.AccountActorFailureReplies._
import com.spok.accountsservice.service.AccountAlreadyRegisters._
import com.spok.accountsservice.service.AccountViewCommands._
import com.spok.accountsservice.service.AccountSuccessViewReplies._
import com.spok.accountsservice.service.AccountViewFailureReplies._

/**
 * Service handler for Accounts Service
 */

case class TransientUser(nickname: String, birthdate: String, location: Location, gender: String, contacts: List[String], phoneNumber: String, picture: Option[String], geoText: String, level: Option[String])

trait AccountRestServiceHandler extends JsonHelper with ValidationUtil with RandomUtil with LoggerUtil with ResponseUtil with HttpUtil {

  implicit val system: ActorSystem

  implicit val materializer: ActorMaterializer
  implicit val timeout = Timeout(40 seconds)

  import akka.pattern.ask
  import system.dispatcher

  val jwtTokenHelper: JWTTokenHelper = JWTTokenHelper
  val dseGraphPersistenceFactoryApi: DSEGraphPersistenceFactoryApi = DSEGraphPersistenceFactoryApi
  val fileUploadUtility: FileUploadUtility = FileUploadUtility
  val redisFactory: RedisFactory = RedisFactory
  val dseMessagingApi: MessagingApi = MessagingApi

  def registeredDetailsMessage(userRegistrationResponse: UserRegistrationResponse): TextMessage = {
    val commonResponse = generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(userRegistrationResponse), Some(DETAILS))
    TextMessage(write(commonResponse))
  }
  def authenticateDetailsMessage(userAuthenticationResponse: UserAuthenticationResponse): TextMessage = {
    val commonResponse = generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(userAuthenticationResponse), Some(DETAILS))
    TextMessage(write(commonResponse))
  }

  /**
   * This method is for detecting request
   * and perform spok operation accordingily
   */
  def detectRegistrationRequestAndPerform(command: ActorRef, query: ActorRef): Flow[Message, Message, _] = {
    logger.info(s"Account service is connected for registration action!!")
    Flow[Message].mapAsync(Runtime.getRuntime.availableProcessors()) {
      case TextMessage.Strict(txt) => handleRegistrationRequest(command, query, txt)
      case TextMessage.Streamed(stream) => {
        stream
          .limit(Int.MaxValue) // Max frames we are willing to wait for
          .completionTimeout(50 seconds) // Max time until last frame
          .runFold("")(_ + _) // Merges the frames
          .flatMap { txt =>
            logger.info("Getting streamed message ")
            handleRegistrationRequest(command, query, txt)
          }
      }
      case _ => {
        logger.info(s"Huhh: What the hell is this?")
        Future(TextMessage(write(sendFormattedError(ACT_101, INVALID_ACTION))))
      }
    }
  }

  private def handleRegistrationRequest(command: ActorRef, query: ActorRef, txt: String): Future[TextMessage] = {
    val actionOpt = try {
      (parse(txt) \ (ACTION)).extractOpt[String]
    } catch {
      case ex: Exception => Some(INVALID_JSON)
    }
    actionOpt match {
      case Some(REGISTER) => accountRegisterHandler(command, txt)
      case Some(CODE) => accountOtpHandler(query, command, txt, false)
      case Some(DETAILS) => registerUserDetails(query, command, txt)
      case Some(INVALID_JSON) => Future(TextMessage(write(sendFormattedError(SYST_503, SERVICE_UNAVAILABLE))))
      case _ => handleAuthenicateRequest(command, query, txt)
    }
  }

  private def handleAuthenicateRequest(command: ActorRef, query: ActorRef, txt: String): Future[TextMessage] = {
    val actionOpt = (parse(txt) \ (ACTION)).extractOpt[String]
    actionOpt match {
      case Some(AUTHENTICATE) => accountAuthenticationHandler(command, txt)
      case Some(AUTH_CODE) => accountOtpHandler(query, command, txt, true)
      case _ => handleInvalidRegistrationRequest(command, query, txt, actionOpt)
    }
  }

  private def handleInvalidRegistrationRequest(command: ActorRef, query: ActorRef, txt: String, actionOpt: Option[String]): Future[TextMessage] = {
    actionOpt match {
      case Some(someAction) => Future(TextMessage(write(sendFormattedError(SYST_401, UNABLE_AUTHENTICATING_USER, Some(ACTION)))))
      case None => Future(TextMessage(write(sendFormattedError(ACT_101, MISSING_ACTION))))
    }
  }

  /**
   * This method is for detecting request
   * and perform spok operation accordingily
   */
  def detectRequestAndPerform(command: ActorRef, query: ActorRef, userId: String, phoneNumber: String): Flow[Message, Message, _] = {
    logger.info(s"Account service is connected for ${phoneNumber}. Now ready to perform account action!!")
    Flow[Message].mapAsync(Runtime.getRuntime.availableProcessors()) {
      case TextMessage.Strict(txt) => handleUserRequests(command, query, userId, phoneNumber, txt)
      case TextMessage.Streamed(stream) => {
        stream
          .limit(Int.MaxValue) // Max frames we are willing to wait for
          .completionTimeout(50 seconds) // Max time until last frame
          .runFold("")(_ + _) // Merges the frames
          .flatMap { txt =>
            logger.info("Getting streamed message " + txt)
            handleUserRequests(command, query, userId, phoneNumber, txt)
          }
      }
      case _ => {
        logger.info(s"${phoneNumber}: What the hell is this?")
        Future(TextMessage(write(sendFormattedError(ACT_101, INVALID_ACTION))))
      }
    }
  }

  private def handleUserRequests(command: ActorRef, query: ActorRef, userId: String, phoneNumber: String, txt: String): Future[TextMessage] = {
    logger.info(s"${phoneNumber}:  in handleUserRequests!")
    val actionOpt = try {
      (parse(txt) \ (ACTION)).extractOpt[String]
    } catch {
      case ex: Exception => Some(INVALID_JSON)
    }
    logger.info(s"${phoneNumber}:  is performing <${actionOpt}>!")
    actionOpt match {
      case Some(FOLLOW_UNFOLLOW) => fetch(command, query, userId, txt, phoneNumber)
      case Some(INVALID_JSON) => Future(TextMessage(write(sendFormattedError(SYST_503, SERVICE_UNAVAILABLE))))
      case _ => handleUserSettingsRelatedRequests(command, query, userId, phoneNumber, txt, actionOpt)
    }
  }

  private def handleUserSettingsRelatedRequests(command: ActorRef, query: ActorRef, userId: String,
    phoneNumber: String, txt: String, actionOpt: Option[String]) = {
    actionOpt match {
      case Some(FOLLOW_SETTINGS) => updateSettingsHandler(command, userId, phoneNumber, txt)
      case Some(HELP_SETTINGS) => updateHelpSettingsHandler(command, userId, phoneNumber, txt)
      case Some(SUPPORT) => handleSupportRequests(command, userId, txt, phoneNumber)
      case _ => handleUserGroupRelatedRequests(command, query, userId, phoneNumber, txt, actionOpt)
    }
  }

  private def handleUserGroupRelatedRequests(command: ActorRef, query: ActorRef, userId: String,
    phoneNumber: String, txt: String, actionOpt: Option[String]) = {
    actionOpt match {
      case Some(CREATE_GROUP) => groupCreationHandler(command, phoneNumber, userId, txt)
      case Some(UPDATE_GROUP) => groupUpdationHandler(query, command, phoneNumber, userId, txt)
      case Some(REMOVE_GROUP) => removeGroupHandler(command, query, userId, phoneNumber, txt)
      case _ => handleAddRemoveFollowersInGroupRequests(command, query, userId, phoneNumber, txt, actionOpt)
    }
  }

  private def handleAddRemoveFollowersInGroupRequests(command: ActorRef, query: ActorRef, userId: String,
    phoneNumber: String, txt: String, actionOpt: Option[String]) = {
    actionOpt match {
      case Some(ADD_FOLLOWER_GROUP) => addFollowersInGroupHandler(command, userId, phoneNumber, txt)
      case Some(REMOVE_FOLLOWER_GROUP) => removeFollowersFromGroupHandler(command, userId, phoneNumber, txt)
      case _ => handleUpdatePhoneNumberUserRequests(command, query, userId, phoneNumber, txt, actionOpt)
    }
  }

  private def handleUpdatePhoneNumberUserRequests(command: ActorRef, query: ActorRef, userId: String,
    phoneNumber: String, txt: String, actionOpt: Option[String]) = {

    actionOpt match {
      case Some(UPDATE_PHONE_STEP_ONE) => sendOtpToNewNumber(command, userId, phoneNumber, txt)
      case Some(UPDATE_PHONE_STEP_TWO) => accountOtpHandlerForUpdatedNumber(command, query, txt, true, phoneNumber, userId)
      case _ => handlePromoteUser(command, query, userId, phoneNumber, txt, actionOpt)
    }
  }

  private def handlePromoteUser(command: ActorRef, query: ActorRef, userId: String,
    phoneNumber: String, txt: String, actionOpt: Option[String]) = {

    actionOpt match {
      case Some(UPDATE_LEVEL) => promoteUser(command, userId, phoneNumber, txt)
      case _ => handleSuspendUserRequests(command, query, userId, phoneNumber, txt, actionOpt)
    }
  }

  private def handleSuspendUserRequests(command: ActorRef, query: ActorRef, userId: String,
    phoneNumber: String, txt: String, actionOpt: Option[String]): Future[TextMessage] = {

    actionOpt match {
      case Some(SUSPEND_SPOKER) => suspendUser(command, userId, phoneNumber, txt)
      case Some(REACTIVATE_SPOKER) => reactivateUser(command, userId, phoneNumber, txt)
      case _ => handleDisableUserRequests(query, userId, phoneNumber, txt, actionOpt)
    }
  }

  private def handleDisableUserRequests(query: ActorRef, userId: String,
    phoneNumber: String, txt: String, actionOpt: Option[String]): Future[TextMessage] = {

    actionOpt match {
      case Some(DISABLE_USER) => disableUser(query, userId, phoneNumber, txt)
      case Some(MY_ACCOUNT_DISABLE) => disableAccountOfUserByHimeSelf(query, userId)
      case Some(someAction) => Future(TextMessage(write(sendFormattedError(ACT_101, MISSING_ACTION))))
      case None => Future(TextMessage(write(sendFormattedError(ACT_101, MISSING_ACTION))))
    }
  }

  /**
   *
   * @param command
   * @return correct or incorrect number message for register step1
   */
  def accountRegisterHandler(command: ActorRef, txt: String): Future[TextMessage] = {
    val phoneNumberCombination = extractPhoneNumber(txt)
    phoneNumberCombination match {
      case (Some(countryCode), Some(number)) => {
        val resultNumber = isValidNumber(countryCode, number)
        if (resultNumber) {
          val phoneNumber = countryCode + number
          val futureResponse = ask(command, Validate(phoneNumber)).mapTo[AccountAck]
          getResponse(futureResponse)
        } else {
          val error = List(Error(RGX_001, INVALID_PHONE_NUMBER))
          val commonResponse = sendFormattedError(RGX_001, INVALID_PHONE_NUMBER, Some(REGISTER))
          Future(TextMessage(write(commonResponse)))
        }
      }
      case (None, None) => {
        val commonResponse = sendFormattedError(PRS_001, INVALID_JSON, Some(REGISTER))
        Future(TextMessage(write(commonResponse)))
      }
    }
  }

  def accountAuthenticationHandler(command: ActorRef, txt: String): Future[TextMessage] = {
    val phoneNumberCombination = extractPhoneNumber(txt)
    phoneNumberCombination match {
      case (Some(countryCode), Some(number)) => {
        val resultNumber = isValidNumber(countryCode, number)
        if (resultNumber) {
          val phoneNumber = countryCode + number
          val futureResponse = ask(command, Authenticate(phoneNumber)).mapTo[AccountAck]
          getResponse(futureResponse)
        } else {
          val error = List(Error(RGX_001, INVALID_PHONE_NUMBER))
          val commonResponse = sendFormattedError(RGX_001, INVALID_PHONE_NUMBER, Some(AUTHENTICATE))
          Future(TextMessage(write(commonResponse)))
        }
      }
      case (None, None) => {
        val commonResponse = sendFormattedError(PRS_001, INVALID_JSON, Some(AUTHENTICATE))
        Future(TextMessage(write(commonResponse)))
      }
    }
  }

  private def getResponse(futureResponse: Future[AccountAck]): Future[TextMessage] = {
    futureResponse.map { response =>
      response match {
        case validateUserSuccess: ValidateUserSuccess => {
          val commonResponse = generateCommonResponseForCaseClass(SUCCESS, None, Some(Message(validateUserSuccess.message)), Some(REGISTER))
          TextMessage(write(commonResponse))
        }
        case validateUserFailure: ValidateUserFailure => {
          val commonResponse = sendFormattedError(IDT_102, validateUserFailure.message, Some(REGISTER))
          TextMessage(write(commonResponse))
        }
        case alreadyRegistered: AllReadyRegisteredUser => {
          val commonResponse = sendFormattedError(IDT_001, alreadyRegistered.message, Some(REGISTER))
          TextMessage(write(commonResponse))
        }
        case userNotRegitered: UserNotRegitered => {
          val commonResponse = sendFormattedError(IDT_001, userNotRegitered.message, Some(AUTHENTICATE))
          TextMessage(write(commonResponse))
        }
        case authenticateUserSuccess: AuthenticateUserSuccess => {
          val commonResponse = generateCommonResponseForCaseClass(SUCCESS, None, Some(Message(authenticateUserSuccess.message)), Some(AUTHENTICATE))
          TextMessage(write(commonResponse))
        }
        case authenticateUserFailure: AuthenticateUserFailure => {
          val commonResponse = sendFormattedError(IDT_102, authenticateUserFailure.message, Some(AUTHENTICATE))
          TextMessage(write(commonResponse))
        }

      }
    }
  }

  /**
   *
   * @param query The query ActorRef
   * @return confirm message if OTP and phone number get validate otherwise incorrect OTP message for register step2
   */
  def accountOtpHandler(query: ActorRef, command: ActorRef, txt: String, isAuthenticate: Boolean): Future[TextMessage] = {
    val phoneOtpCombination = extractOTPNumber(txt)
    phoneOtpCombination match {
      case (Some(phoneNumber), Some(otpCode)) => {
        val validOtp = isValidOtp(otpCode)
        validOtp match {
          case true => {
            val futureResponse = ask(query, GetOTPToken(phoneNumber))
            futureResponse.flatMap {
              response => handleOtpResponse(response, phoneNumber, otpCode, command, false, "", "", isAuthenticate)
            }
          }
          case false => Future(TextMessage(write(sendFormattedError(RGX_002, INVALID_OTP, Some(CODE)))))
        }
      }
      case (None, None) => Future(TextMessage(write(sendJsonErrorWithEmptyData(Some(CODE)))))
    }
  }

  def accountOtpHandlerForUpdatedNumber(command: ActorRef, query: ActorRef, json: String, isUpdateNumber: Boolean = false, number: String = "",
    userId: String = ""): Future[TextMessage] = {
    val phoneOtpCombination = extractOTPNumber(json)
    phoneOtpCombination match {
      case (Some(phoneNumber), Some(otpCode)) =>
        {
          val validOtp = isValidOtp(otpCode)
          validOtp match {
            case true => {
              val futureResponse = ask(query, GetOTPToken(phoneNumber))
              futureResponse.flatMap {
                response => handleOtpResponse(response, phoneNumber, otpCode, command, isUpdateNumber, number, userId, false)
              }
            }
            case false => Future(TextMessage(write(sendFormattedError(RGX_002, INVALID_OTP, Some(UPDATE_PHONE_STEP_TWO)))))

          }
        }
      case (None, None) => Future(TextMessage(write(sendJsonErrorWithEmptyData(Some(UPDATE_PHONE_STEP_TWO)))))
    }
  }

  private def handleOtpResponse(response: Any, phoneNumber: String, otpCode: String, command: ActorRef,
    isUpdateNumber: Boolean, number: String, userId: String, isAuthenticate: Boolean) = {

    response match {
      case validatedOTP: FindOtpTokenSuccess =>
        val otpAuthToken: OtpAuthToken = validatedOTP.otpToken
        val retryCount: Int = verifyOtpTokenForMaxTries(otpAuthToken, phoneNumber, otpCode, command)
        verifyOTPMessage(otpAuthToken, phoneNumber, otpCode, retryCount, command, isUpdateNumber, number, userId, isAuthenticate)
      case failure: FindOtpTokenFailure => {
        failure.message match {
          case OTP_EXPIRED => Future(TextMessage(write(sendFormattedError(IDT_111, OTP_EXPIRED, Some(CODE)))))
          case GENERIC_OTP_ERROR_MESSAGE => Future(TextMessage(write(sendFormattedError(IDT_109, GENERIC_OTP_ERROR_MESSAGE, Some(CODE)))))
        }
      }
    }
  }

  private def verifyOTPMessage(otpAuthToken: OtpAuthToken, phoneNumber: String, code: String, retryCount: Int,
    command: ActorRef, isUpdateNumber: Boolean, number: String, userId: String, isAuthenticate: Boolean): Future[TextMessage] = {
    if (retryCount <= MAX_RETRIES) {
      if (retryCount == otpAuthToken.retryCount) {
        clearOtp(command, isUpdateNumber, number, userId, phoneNumber, isAuthenticate)
      } else Future(TextMessage(write(sendFormattedError(IDT_005, WRONG_OTP, Some(CODE)))))
    } else {
      command ! ClearOtpToken(phoneNumber)
      Future(TextMessage(write(sendFormattedError(IDT_110, MAX_RETRIES_MESSAGE, Some(CODE)))))
    }
  }

  private def clearOtp(command: ActorRef, isUpdateNumber: Boolean, number: String, userId: String, phoneNumber: String, isAuthenticate: Boolean) = {
    if (isUpdateNumber) {
      eventuateUpdatePhoneNumber(command, userId, number, phoneNumber)
    } else {
      command ! AllowUser(phoneNumber)
      if (isAuthenticate) {
        try {
          val validUserId: String = dseGraphPersistenceFactoryApi.getUserIdByMobileNumber(phoneNumber)
          val commonResponse = authenticateDetailsMessage(UserAuthenticationResponse(jwtTokenHelper.createJwtTokenWithRole(
            validUserId, phoneNumber, USER_ROLE
          ), validUserId))
          Future(commonResponse)
        } catch {
          case ex: Exception => Future(TextMessage(write(sendFormattedError(IDT_109, GENERIC_ERROR_MESSAGE_AUTHENTICATE, Some(AUTH_CODE)))))
        }

      } else {
        val commonResponse = generateCommonResponse(SUCCESS, None, Some(phoneNumber), Some(CODE))
        Future(TextMessage(write(commonResponse)))
      }
    }
  }

  private def eventuateUpdatePhoneNumber(command: ActorRef, userId: String, phoneNumber: String, newNumber: String): Future[TextMessage] = {
    val numberUpdateResponse = ask(command, UpdateNumber(userId, phoneNumber, newNumber)).mapTo[AccountAck]
    numberUpdateResponse.map { response =>
      response match {
        case UpdatePhoneNumberSuccess(msg) =>
          TextMessage(write(generateCommonResponse(SUCCESS, Some(List()),
            Some(jwtTokenHelper.createJwtTokenWithRole(userId, newNumber, USER_ROLE)), Some(UPDATE_PHONE_STEP_TWO))))
        case updatePhoneNumberFailure: UpdatePhoneNumberFailure =>
          TextMessage(write(sendFormattedError(
            updatePhoneNumberFailure.errorCode,
            updatePhoneNumberFailure.cause.getMessage, Some(UPDATE_PHONE_STEP_TWO)
          )))
      }
    }
  }

  private def verifyOtpTokenForMaxTries(otpAuthToken: OtpAuthToken, phoneNumber: String, code: String, command: ActorRef): Int = {
    if (otpAuthToken.otp.equals(code)) {
      otpAuthToken.retryCount
    } else {
      val newOtpToken: OtpAuthToken = otpAuthToken.copy(retryCount = otpAuthToken.retryCount + 1)
      command ! UpdateOtpToken(newOtpToken)
      newOtpToken.retryCount
    }
  }

  /**
   * @return details registered message if user details are successfully validated in register step3 otherwise
   *         incorrect message
   */
  private def registerUserDetails(query: ActorRef, command: ActorRef, txt: String): Future[TextMessage] = {
    val transientUser = extractUserDetails(txt)
    info("User registration (Step 3):: converted into transientUser " + transientUser)
    transientUser match {
      case Some(someTransientUser) => {
        handleTransientUser(query, command, someTransientUser)
      }
      case None => Future(TextMessage(write(sendJsonErrorWithEmptyData(Some(DETAILS)))))
    }
  }

  private def handleTransientUser(query: ActorRef, command: ActorRef, transientUser: TransientUser): Future[TextMessage] = {
    val message = isValidUser(transientUser.nickname, transientUser.birthdate, transientUser.location.status, transientUser.gender)
    message match {
      case (isValid, errorList) => if (isValid) {
        val futureResponse = ask(query, GetValidUser(transientUser.phoneNumber)).mapTo[Boolean]
        futureResponse flatMap {
          case true => handleValidUser(command, transientUser, errorList)
          case false => Future(TextMessage(write(sendFormattedError(IDT_121, VERIFY_NUMBER, Some(DETAILS)))))
        }
      } else {
        Future(TextMessage(write(generateCommonResponseForError(FAILED, errorList, None, Some(DETAILS)))))
      }
    }
  }

  private def handleValidUser(command: ActorRef, transientUser: TransientUser, msg: Option[List[Error]]): Future[TextMessage] = {
    val format = new SimpleDateFormat(DATEFORMAT)
    val date = format.parse(transientUser.birthdate)
    val level = transientUser.level.getOrElse("user")
    if (level.equals("user")) {
      val user = User(transientUser.nickname, date, transientUser.location, transientUser.gender,
        transientUser.contacts, transientUser.phoneNumber, getUUID(), transientUser.picture, None, transientUser.geoText, (transientUser.level.getOrElse("user")).toLowerCase())
      if (dseGraphPersistenceFactoryApi.isUniqueNickname(user.nickname)) {
        Future(TextMessage(write(sendFormattedError(IDT_122, NICKNAME_ALREADY_IN_USE, Some(DETAILS)))))
      } else {
        val futureResponse = ask(command, Create(user)).mapTo[AccountAck]
        futureResponse.map { response =>
          response match {
            case accountCreateSuccess: AccountCreateSuccess => accountCreateSuccessAction(user, command)
            case accountCreateFailure: AccountCreateFailure => TextMessage(write(sendFormattedError(
              IDT_104,
              accountCreateFailure.cause.getMessage, Some(DETAILS)
            )))
          }
        }
      }
    } else Future(TextMessage(write(sendFormattedError(IDT_123, NOT_VALID_LEVEL, Some(DETAILS)))))
  }

  /* Successfully create new user account and make the new user automatically follow previously registered users
    who have this user in their contact list */
  private def accountCreateSuccessAction(user: User, command: ActorRef) = {

    //TODO : Make Dse get call through accountview
    val alreadyRegisteredUser = dseGraphPersistenceFactoryApi.getRegisteredUsers(user.userNumber.substring(1))
    val alreadyRegisteredUserIds = alreadyRegisteredUser.map(node => node.toString.replaceAll(QUOTES, NO_SPACE))
    Future {
      info("User registration (Step 3):: Registerted user ids of user's contact list " + alreadyRegisteredUserIds)
      alreadyRegisteredUserIds.map(registeredUserId => {
        val followUnfollow = FollowUnfollow(user.userNumber, user.userId, registeredUserId)
        ask(command, FollowUnfollowAction(followUnfollow)).mapTo[AccountAck]
      })
    }
    registeredDetailsMessage(UserRegistrationResponse(jwtTokenHelper.createJwtTokenWithRole(
      user.userId, user.userNumber, USER_ROLE
    ), user.userId, alreadyRegisteredUserIds))
  }

  def extractPhoneNumber(numberJson: String): (Option[String], Option[String]) = {

    try {
      val countryCode = (parse(numberJson) \ COUNTRYCODE).extract[String]
      val number = (parse(numberJson) \ USERNUMBER).extract[String]
      (Some(countryCode), Some(number))
    } catch {
      case ex: Exception => (None, None)
    }
  }

  def extractOTPNumber(numberJson: String): (Option[String], Option[String]) = {

    try {
      val number = (parse(numberJson) \ USERNUMBER).extract[String]
      val updatedNumber = if (number.substring(0, 1) == "+") number else "+" + number
      val code = (parse(numberJson) \ OTPCODE).extract[String]
      (Some(updatedNumber), Some(code))
    } catch {
      case ex: Exception => (None, None)
    }
  }

  def extractUserDetails(userDetailsJson: String): Option[TransientUser] = {

    try {
      val nickname = (parse(userDetailsJson) \ NICKNAME).extract[String]
      val birthdate = (parse(userDetailsJson) \ BIRTHDATE).extract[String]
      val location = try {
        (parse(userDetailsJson) \ LOCATION).extract[Location]
      } catch {
        case ex: Exception => (parse(LOCATIONEXCEPTIONSTRING) \ LOCATION).extract[Location]
      }
      val gender = (parse(userDetailsJson) \ GENDER).extract[String]
      val contacts = (parse(userDetailsJson) \ CONTACTS).extract[List[String]]
      val userNumber = (parse(userDetailsJson) \ USERNUMBER).extract[String]
      val picture = (parse(userDetailsJson) \ PICTURE).extractOpt[String]
      val geoText = (parse(userDetailsJson) \ GEOTEXT).extract[String]
      val level = (parse(userDetailsJson) \ LEVEL).extractOpt[String]

      val transientUser = TransientUser(nickname, birthdate, location, gender, contacts, userNumber, picture, geoText, level)
      Some(transientUser)
    } catch {
      case ex: Exception => None
    }
  }

  /**
   * This function will validate old number and send OTP to new number
   *
   * @param command     The command actor ref.
   * @param phoneNumber old and new numbers
   * @return success message if OTP send successfully else error message.
   */
  def sendOtpToNewNumber(command: ActorRef, userId: String, phoneNumber: String, json: String): Future[TextMessage] = {
    val phoneNumberDetails = extractPhoneNumbersWhileUpdating(json)
    phoneNumberDetails match {
      case Some(phoneNumbers) => {
        val isValidPhoneNumbers = validPhoneNumbers(phoneNumbers)
        if (isValidPhoneNumbers) eventuateSendOtpToNewNumber(command, phoneNumbers, phoneNumber, userId)
        else Future(TextMessage(write(sendFormattedError(RGX_001, INVALID_PHONE_NUMBERS, Some(UPDATE_PHONE_STEP_ONE)))))
      }
      case None => Future(TextMessage(write(sendJsonErrorWithEmptyData(Some(UPDATE_PHONE_STEP_ONE)))))
    }
  }

  private def eventuateSendOtpToNewNumber(command: ActorRef, phoneNumbers: PhoneNumbers, phoneNumber: String, userId: String): Future[TextMessage] = {
    val oldNumber = if (phoneNumbers.oldCountryCode.substring(0, 1) == "+") phoneNumbers.oldCountryCode + phoneNumbers.oldNumber
    else "+" + phoneNumbers.oldCountryCode + phoneNumbers.oldNumber

    val newNumber = if (phoneNumbers.newCountryCode.substring(0, 1) == "+") phoneNumbers.newCountryCode + phoneNumbers.newNumber
    else "+" + phoneNumbers.newCountryCode + phoneNumbers.newNumber
    val sendOtp = ask(command, ValidateNumber(phoneNumber, oldNumber, newNumber, userId)).mapTo[AccountAck]
    sendOtp.map { response =>
      response match {
        case validatePhoneNumberSuccess: ValidatePhoneNumberSuccess =>
          TextMessage(write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(Message(validatePhoneNumberSuccess.message)), Some(UPDATE_PHONE_STEP_ONE))))
        case validatePhoneNumberFailure: ValidatePhoneNumberFailure =>
          TextMessage(write(sendFormattedError(validatePhoneNumberFailure.errorCode, validatePhoneNumberFailure.cause.getMessage, Some(UPDATE_PHONE_STEP_ONE))))
      }
    }
  }

  private def extractPhoneNumbersWhileUpdating(numberJson: String): Option[PhoneNumbers] =
    parse(numberJson).extractOpt[PhoneNumbers]

  /*
  Gives a follow or unfollow response if a user is followed or unfollowed or else gives a generic error
   */
  def fetch(command: ActorRef, query: ActorRef, userId: String, json: String, phoneNumber: String): Future[TextMessage] = {

    val isUserSuspended: Future[IsUserSuspendedAsk] = ask(query, IsUserSuspended(userId)).mapTo[IsUserSuspendedAsk]

    val response = isUserSuspended.map { response =>
      response.status match {
        case SPOKER_SUSPENDED => Future(TextMessage(write(sendFormattedError(SPK_014, ACTION_IMPOSSIBLE, Some(FOLLOW_UNFOLLOW)))))
        case PROPERTY_NOT_FOUND => Future(TextMessage(write(sendFormattedError(FLW_101, FOLLOW_UNFOLLOW_ERROR, Some(FOLLOW_UNFOLLOW)))))
        case SPOKER_NOT_SUSPENDED =>

          val followingIdOpt = (parse(json) \ (FOLLOWING_ID)).extractOpt[String]
          followingIdOpt match {
            case Some(followingId) => {
              if (followingId.equals(userId)) {
                Future(TextMessage(write(sendFormattedError(FLW_104, NOT_FOLLOW_ITSELF, Some(FOLLOW_UNFOLLOW)))))
              } else {
                val followUnfollow: FollowUnfollow = FollowUnfollow(phoneNumber, userId, followingId)
                val follows = ask(command, FollowUnfollowAction(followUnfollow)).mapTo[AccountAck]
                follows flatMap {
                  response => sendFollowUnfollowResponse(response, followUnfollow)
                }
              }
            }
            case None => Future(TextMessage(write(sendFormattedError(FLW_103, FOLLOW_ID_ERROR, Some(FOLLOW_UNFOLLOW)))))
          }
      }
    }
    response.flatMap(identity)
  }

  private def sendFollowUnfollowResponse(response: AccountAck, followUnfollow: FollowUnfollow) = {

    response match {
      case followUnfollowSuccess: FollowUnfollowSuccess => sendFollowUnfollowSuccessResponse(followUnfollowSuccess, followUnfollow)
      case followUnfollowFailure: FollowUnfollowFailure => Future(TextMessage(write(sendFormattedError(FLW_101, FOLLOW_UNFOLLOW_ERROR, Some(FOLLOW_UNFOLLOW)))))
    }
  }

  private def sendFollowUnfollowSuccessResponse(followUnfollowSuccess: FollowUnfollowSuccess, followUnfollow: FollowUnfollow) = {
    followUnfollowSuccess.followUnfollows match {
      case FOLLOWS => {
        dseGraphPersistenceFactoryApi.isFollowingExists(followUnfollow.followingId, followUnfollow.followerId) match {
          case true => Future(TextMessage(write(generateCommonResponseForCaseClass(SUCCESS, Some(List()),
            Some(AccountResponse(followResponse = Some(followUnfollow), isFriend = Some(true))), Some(FOLLOW_UNFOLLOW)))))
          case false => Future(TextMessage(write(generateCommonResponseForCaseClass(SUCCESS, Some(List()),
            Some(AccountResponse(followResponse = Some(followUnfollow))), Some(FOLLOW_UNFOLLOW)))))
        }
      }
      case UNFOLLOWS =>
        removeTalkIdFromRedis(followUnfollow.followerId, followUnfollow.followingId)
        Future(TextMessage(write(generateCommonResponseForCaseClass(SUCCESS, Some(List()),
          Some(AccountResponse(unFollowResponse = Some(followUnfollow))), Some(FOLLOW_UNFOLLOW)))))
    }
  }

  /**
   *
   * @param command to remove the group
   * @param query
   * @param userId
   * @param phoneNumber
   * @return appropriate message if the group was found and removed or not
   */
  def removeGroupHandler(command: ActorRef, query: ActorRef, userId: String, phoneNumber: String, json: String): Future[TextMessage] = {
    logger.info(s"${phoneNumber}:  Got Json. Now removing group ${json} ")
    val groupIdOpt = (parse(json) \ (GROUP_ID)).extractOpt[String]
    groupIdOpt match {
      case Some(groupId) => {
        if (!(groupId equals "0")) {
          handleValidRemoveGroupRequest(query, command, groupId, userId, phoneNumber)
        } else {
          Future(TextMessage(write(sendFormattedError(GRP_002, GROUP_CANNOT_BE_DELETED, Some(REMOVE_GROUP)))))
        }
      }
      case None => Future(TextMessage(write(sendFormattedError(GRP_001, s"Group Id not found", Some(REMOVE_GROUP)))))
    }
  }

  private def handleValidRemoveGroupRequest(query: ActorRef, command: ActorRef, groupId: String, userId: String, phoneNumber: String) = {
    val validGroupResponse = ask(query, ValidateGroup(groupId, userId)).mapTo[IsValidGroupAck]
    validGroupResponse flatMap {
      response =>
        response.groupExistStatus match {
          case true => performRemoveGroup(command, groupId, userId, phoneNumber)
          case false => Future(TextMessage(write(sendFormattedError(GRP_001, s"Group $groupId not found", Some(REMOVE_GROUP)))))
        }
    }
  }

  private def performRemoveGroup(command: ActorRef, groupId: String, userId: String, phoneNumber: String): Future[TextMessage] = {

    val groupRemove = ask(command, RemoveGroup(groupId, userId, phoneNumber)).mapTo[AccountAck]
    groupRemove map { reply =>
      reply match {
        case groupRemovedSuccess: GroupRemovedSuccess =>
          logger.info(s"${phoneNumber}:  Group is removed ${groupRemovedSuccess.groupId} ")
          TextMessage(write(generateCommonResponseForCaseClass(SUCCESS, Some(List()),
            Some(GroupId(groupRemovedSuccess.groupId)), Some(REMOVE_GROUP))))
        case groupRemovedFailure: GroupRemovedFailure =>
          logger.error(s"${phoneNumber}: Failed to removed group ${groupRemovedFailure} ")
          TextMessage(write(sendFormattedError(GRP_103, GROUP_REMOVAL_ERROR + groupId + " (generic error)", Some(REMOVE_GROUP))))
      }
    }
  }

  /**
   * This function will create a group
   *
   * @param command
   * @param phoneNumber
   * @return groupId on success
   */
  def groupCreationHandler(command: ActorRef, phoneNumber: String, userId: String, json: String): Future[TextMessage] = {
    logger.info(s"${phoneNumber}:  Got Json. Now creating group ${json} ")
    val groupTitleDetails = extractGroupTitle(json)
    groupTitleDetails match {
      case Some(title) => {
        val validTitle = isValidTitle(title)
        validTitle match {
          case Some(error) => {
            logger.error(s"${phoneNumber}:  Invalid title ${error} ")
            Future(TextMessage(write(sendFormattedError(error.id, error.message, Some(CREATE_GROUP)))))
          }
          case None => createValidGroup(title, command, phoneNumber, userId)
        }
      }
      case None => Future(TextMessage(write(sendJsonErrorWithEmptyData(Some(CREATE_GROUP)))))
    }
  }

  private def createValidGroup(title: String, command: ActorRef, phoneNumber: String, userId: String): Future[TextMessage] = {
    val group = Group(getUUID(), title)
    val futureResponse = ask(command, CreateUserGroup(group, phoneNumber, userId)).mapTo[AccountAck]
    futureResponse.map {
      response =>
        response match {
          case groupCreateSuccess: GroupCreateSuccess => {
            logger.info(s"${phoneNumber}:  Group is created ${groupCreateSuccess.group.id} ")
            TextMessage(write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(GroupId(groupCreateSuccess.group.id)), Some(CREATE_GROUP))))
          }
          case groupCreateFailure: GroupCreateFailure => {
            logger.error(s"${phoneNumber}: Failed to create group ${groupCreateFailure} ")
            TextMessage(write(sendFormattedError(GRP_101, GROUP_CREATION_ERROR, Some(CREATE_GROUP))))
          }
        }
    }
  }

  /**
   *
   * @param query       to fetch if the group exists or not
   * @param command     to update the group name if all the validations pass
   * @param phoneNumber user's phonenumber
   * @param userId
   * @return appropriate message as to whether the group was updated or not
   */
  def groupUpdationHandler(query: ActorRef, command: ActorRef, phoneNumber: String,
    userId: String, json: String): Future[TextMessage] = {
    logger.info(s"${phoneNumber}:  Got Json. Now updating group ${json} ")
    val groupIdOpt = (parse(json) \ (GROUP_ID)).extractOpt[String]
    groupIdOpt match {
      case Some(groupId) => {
        val groupCombination = extractGroupTitle(json)
        groupCombination match {
          case Some(title) => handleValidUpdateGroupRequest(query, command, title, phoneNumber, userId, groupId)
          case None => Future(TextMessage(write(sendJsonErrorWithEmptyData(Some(UPDATE_GROUP)))))
        }
      }
      case None => Future(TextMessage(write(sendFormattedError(GRP_001, s"Group Id not found", Some(UPDATE_GROUP)))))
    }
  }

  private def handleValidUpdateGroupRequest(query: ActorRef, command: ActorRef, title: String, phoneNumber: String,
    userId: String, groupId: String) = {
    val validTitle = isValidTitle(title)
    validTitle match {
      case Some(error) => Future(TextMessage(write(sendFormattedError(error.id, error.message, Some(UPDATE_GROUP)))))
      case None => eventuateUpdateGroupHandler(query, command, title, phoneNumber, userId, groupId)
    }
  }

  private def eventuateUpdateGroupHandler(query: ActorRef, command: ActorRef, title: String, phoneNumber: String,
    userId: String, groupId: String): Future[TextMessage] = {

    val validGroupResponse = ask(query, ValidateGroup(groupId, userId)).mapTo[IsValidGroupAck]
    val updateGroupResult = validGroupResponse map {
      response =>
        response.groupExistStatus match {
          case true => updateValidGroup(title, command, phoneNumber, userId, groupId) // update group
          case false => Future(TextMessage(write(sendFormattedError(GRP_001, s"Group $groupId not found", Some(UPDATE_GROUP)))))
        }
    }
    updateGroupResult.flatMap(identity)
  }

  private def updateValidGroup(title: String, command: ActorRef, phoneNumber: String, userId: String,
    groupId: String): Future[TextMessage] = {

    val group = Group(groupId, title)
    val futureResponse = ask(command, UpdateUserGroup(group, phoneNumber, userId)).mapTo[AccountAck]
    futureResponse.map {
      response =>
        response match {
          case groupUpdateSuccess: GroupUpdateSuccess =>
            logger.info(s"${phoneNumber}:  Group is updated ${groupUpdateSuccess.group.id} ")
            TextMessage(write(generateCommonResponseForCaseClass(SUCCESS, Some(List()),
              Some(GroupId(groupUpdateSuccess.group.id)), Some(UPDATE_GROUP))))
          case groupUpdateFailure: GroupUpdateFailure =>
            logger.error(s"${phoneNumber}: Failed to update group ${groupUpdateFailure} ")
            TextMessage(write(sendFormattedError(GRP_102, GROUP_UPDATE_ERROR + groupId + " (generic error)", Some(UPDATE_GROUP))))
        }
    }
  }

  private def extractGroupTitle(groupJson: String): (Option[String]) = (parse(groupJson) \ TITLE).extractOpt[String]

  /**
   * This function will add followers or contact in a group
   *
   * @param command
   * @param phoneNumber
   * @param userId
   * @return success or failure message
   */
  def addFollowersInGroupHandler(command: ActorRef, userId: String, phoneNumber: String, json: String): Future[TextMessage] = {
    logger.info(s"${phoneNumber}:  Adding followers/contacts in group  ${phoneNumber} ")
    val groupIdOpt = (parse(json) \ (GROUP_ID)).extractOpt[String]
    groupIdOpt match {
      case Some(groupId) => {
        val userGroup: Option[UserGroup] = extractUserGroupDetails(json, groupId)
        userGroup match {
          case (Some(someUserGroup)) => handleValidAddFollowersInGroupHandleRequest(command, phoneNumber, userId, someUserGroup)
          case None => Future(TextMessage(write(sendJsonErrorWithEmptyData(Some(ADD_FOLLOWER_GROUP)))))
        }
      }
      case None => Future(TextMessage(write(sendFormattedError(GRP_001, s"Group Id not found", Some(ADD_FOLLOWER_GROUP)))))
    }
  }

  private def handleValidAddFollowersInGroupHandleRequest(command: ActorRef, phoneNumber: String, userId: String, userGroup: UserGroup) = {
    if (userGroup.userIds.isEmpty && userGroup.contacts.isEmpty) {
      Future(TextMessage(write(sendFormattedError(GRP_108, s"Contacts and UserIds are Empty"))))
    } else {
      val futureResponse: Future[AccountAck] = ask(command, AddFollowers(userGroup, phoneNumber, userId)).mapTo[AccountAck]
      addFollowersInGroup(futureResponse)
    }
  }

  private def addFollowersInGroup(futureResponse: Future[AccountAck]) = {
    futureResponse.map { response =>
      response match {
        case addFollowerInGroupSuccess: AddFollowerInGroupSuccess =>
          logger.info(s"Added followers/contacts in group ")
          TextMessage(write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(addFollowerInGroupSuccess), Some(ADD_FOLLOWER_GROUP))))
        case addFollowerInGroupFailure: AddFollowerInGroupFailure => {
          logger.error(s" Failed to add followers/contacts in group ${addFollowerInGroupFailure} ")
          val contacts = addFollowerInGroupFailure.userGroup.contacts.map(x => x.phone)
          val message = addFollowerInGroupFailure.cause.getMessage + "   invalidContact: " + contacts +
            "  invalidUserIds: " + addFollowerInGroupFailure.userGroup.userIds
          TextMessage(write(sendFormattedError(addFollowerInGroupFailure.errorCode, message, Some(ADD_FOLLOWER_GROUP))))
        }
      }
    }
  }

  private def extractUserGroupDetails(userGroupDetailsJson: String, groupId: String): Option[UserGroup] = {
    try {
      val validateKey = parse(userGroupDetailsJson)
      if (((validateKey \ "userIds") equals JNothing) || ((validateKey \ "contacts") equals JNothing)) None
      else {
        val userGroup = (parse(userGroupDetailsJson)).extract[UserGroupDetails]
        Some(UserGroup(groupId, userGroup.userIds, userGroup.contacts))
      }
    } catch {
      case ex: Exception => None
    }
  }

  /**
   * This function will remove followers or contact in a group
   *
   * @param command
   * @param phoneNumber
   * @param userId
   * @return success or failure message
   */
  def removeFollowersFromGroupHandler(command: ActorRef, userId: String, phoneNumber: String, json: String): Future[TextMessage] = {
    logger.info(s"${phoneNumber}:  Removing followers/contacts in group  ${phoneNumber} ")
    val groupIdOpt = (parse(json) \ (GROUP_ID)).extractOpt[String]
    groupIdOpt match {
      case Some(groupId) => {
        val removeUserGroup = extractRemoveUserGroupDetails(json, groupId)
        removeUserGroup match {
          case (Some(someRemoveUserGroup)) => handleValidRemoveFollowersFromGroupRequest(command, phoneNumber, userId, groupId, someRemoveUserGroup)
          case None => Future(TextMessage(write(sendJsonErrorWithEmptyData(Some(REMOVE_FOLLOWER_GROUP)))))
        }
      }
      case None => Future(TextMessage(write(sendFormattedError(GRP_001, s"Group Id not found", Some(REMOVE_FOLLOWER_GROUP)))))
    }
  }

  private def handleValidRemoveFollowersFromGroupRequest(command: ActorRef, phoneNumber: String, userId: String,
    groupId: String, removeUserGroup: RemoveUserGroup) = {
    if (!(removeUserGroup.groupId equals ZERO)) {
      val futureResponse: Future[AccountAck] = ask(command, RemoveFollowers(removeUserGroup, phoneNumber, userId)).mapTo[AccountAck]
      removeFollowersFromGroup(futureResponse, groupId)
    } else Future(TextMessage(write(sendFormattedError(
      GRP_106,
      s"Unable removing user(s) or contact(s) from group $groupId (generic error).", Some(REMOVE_FOLLOWER_GROUP)
    ))))
  }

  private def removeFollowersFromGroup(futureResponse: Future[AccountAck], groupId: String) = {
    futureResponse.map {
      case removeFollowerInGroupSuccess: RemoveFollowerInGroupSuccess =>
        logger.info(s"Removed followers/contacts in group ")
        TextMessage(write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(removeFollowerInGroupSuccess), Some(REMOVE_FOLLOWER_GROUP))))
      case removeFollowerInGroupFailure: RemoveFollowerInGroupFailure =>
        logger.error(s" Failed to remove followers/contacts in group ${removeFollowerInGroupFailure} ")
        TextMessage(write(sendFormattedError(
          GRP_106,
          removeFollowerInGroupFailure.cause.getMessage, Some(REMOVE_FOLLOWER_GROUP)
        )))

    }
  }

  private def extractRemoveUserGroupDetails(removeUserGroupDetailsJson: String, groupId: String): Option[RemoveUserGroup] = {
    try {
      val validateKey = parse(removeUserGroupDetailsJson)
      if (((validateKey \ "userIds") equals JNothing) || ((validateKey \ "phones") equals JNothing)) None
      else {
        val removeUserGroup = (parse(removeUserGroupDetailsJson)).extract[RemoveUserGroupDetails]
        Some(RemoveUserGroup(groupId, removeUserGroup.userIds, removeUserGroup.phones))
      }
    } catch {
      case ex: Exception => None
    }
  }

  /**
   * This function will get user profile with Minimal details.
   *
   * @param query        spok view actorRef
   * @param targetUserId targetuser's id
   * @param userId       user's id
   * @return Minimal view of user if success otherwise error message.
   */
  def viewShortDetail(query: ActorRef, targetUserId: String, userId: String): Future[String] = {
    RedisFactory.storeVisitiedUsers(targetUserId, userId)
    val userProfileResponse = ask(query, ViewUserMinimalDetails(targetUserId))
    userProfileResponse.map {
      case viewUserMinimalDetailsSuccess: ViewUserMinimalDetailsSuccessResponse =>
        write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(viewUserMinimalDetailsSuccess.userMinimalDetails), Some(VIEW_MINIMAL_DETAILS)))
      case viewUserMinimalDetailsFailure: ViewUserMinimalDetailsFailureResponse =>
        write(sendFormattedError(viewUserMinimalDetailsFailure.errorCode, viewUserMinimalDetailsFailure.cause.getMessage, Some(VIEW_MINIMAL_DETAILS)))
    }
  }

  /**
   * This function will get user profile with full details.
   *
   * @param query        account view actor ref.
   * @param targetUserId targetuser's id
   * @param userId       user's id
   * @return full view of user if success otherwise error message.
   */
  def viewFullDetail(query: ActorRef, targetUserId: String, userId: String): Future[String] = {
    RedisFactory.storeVisitiedUsers(targetUserId, userId)
    val userProfileResponse = ask(query, GetUserProfileFullDetails(targetUserId, userId))
    userProfileResponse.map {
      case userProfileFullDetailsSuccess: UserProfileFullDetailsSuccess =>
        write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(userProfileFullDetailsSuccess.userProfileFullDetails), Some(VIEW_FULL_DETAILS)))
      case userProfileFullDetailsFailure: UserProfileFullDetailsFailure =>
        write(sendFormattedError(userProfileFullDetailsFailure.errorCode, userProfileFullDetailsFailure.cause.getMessage, Some(VIEW_FULL_DETAILS)))
    }
  }

  /**
   * This function will be used to remove user from cache.
   *
   * @param query        spok view actorRef
   * @param targetUserId targetuser's id
   * @param userId       user's id
   * @return full view of user if success otherwise error message.
   */
  def removeUserFromCache(query: ActorRef, targetUserId: String, userId: String): Future[String] = {
    RedisFactory.remove(targetUserId)
    Future.successful("Removed from cache " + targetUserId)
  }

  def disableUser(query: ActorRef, userId: String, phoneNumber: String, json: String): Future[TextMessage] = {
    val targetId = (parse(json) \ (TARGETUSERID)).extractOpt[String]
    targetId match {
      case Some(targetId) => disableAccountOfUser(query, userId, targetId)
      case None => Future(TextMessage(write(sendFormattedError(USR_001, s"User Id not found", Some(DISABLE_USER)))))
    }
  }

  def promoteUser(command: ActorRef, userId: String, phoneNumber: String, json: String): Future[TextMessage] = {
    val level = (parse(json) \ (LEVEL)).extractOpt[String].getOrElse("admin")
    val spokerId = (parse(json) \ (SPOKER_ID)).extractOpt[String]
    if (level == "admin" || level == "superadmin" || level == "user") {
      spokerId match {
        case None => Future(TextMessage(write(sendFormattedError(USR_001, s"Spoker not found", Some(DISABLE_USER)))))
        case Some(spokerId) => prmoteAccountOfUser(command, userId, level, spokerId)
      }
    } else {
      Future(TextMessage(write(sendFormattedError(ADM_003, s"Invalid level", Some(UPDATE_LEVEL)))))
    }
  }

  def suspendUser(command: ActorRef, userId: String, phoneNumber: String, json: String): Future[TextMessage] = {
    val targetId = (parse(json) \ (SPOKER_ID)).extractOpt[String]
    targetId match {
      case Some(targetId) => suspendAccountOfUser(command, userId, targetId, phoneNumber)
      case None => Future(TextMessage(write(sendFormattedError(USR_001, s"User Id not found", Some(SUSPEND_SPOKER)))))
    }
  }

  def reactivateUser(command: ActorRef, userId: String, phoneNumber: String, json: String): Future[TextMessage] = {
    val targetId = (parse(json) \ (SPOKER_ID)).extractOpt[String]
    targetId match {
      case Some(targetId) => reactivateAccountOfUser(command, userId, targetId, phoneNumber)
      case None => Future(TextMessage(write(sendFormattedError(USR_001, s"User Id not found", Some(SUSPEND_SPOKER)))))
    }
  }

  def suspendAccountOfUser(command: ActorRef, userId: String, targetId: String, phoneNUmber: String): Future[TextMessage] = {

    val disableResponse: Future[Any] = ask(command, Suspend(userId, targetId, phoneNUmber))
    disableResponse.map { res =>
      res match {
        case suspendResponseSuccess: SuspendResponseSuccess =>
          TextMessage(write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(Message(suspendResponseSuccess.message)), Some(SUSPEND_SPOKER))))

        case suspendResponseFailure: SuspendResponseFailure =>
          TextMessage(write(sendFormattedError(suspendResponseFailure.errorCode, suspendResponseFailure.cause.getMessage, Some(SUSPEND_SPOKER))))
      }
    }
  }

  def reactivateAccountOfUser(command: ActorRef, userId: String, targetId: String, phoneNUmber: String): Future[TextMessage] = {

    val reactiveResponse: Future[Any] = ask(command, Recativate(userId, targetId, phoneNUmber))
    reactiveResponse.map { res =>
      res match {
        case reactivateResponseSuccess: ReactivatedResponseSuccess =>
          TextMessage(write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(Message(reactivateResponseSuccess.message)), Some(REACTIVATE_SPOKER))))

        case suspendResponseFailure: ReactivateResponseFailure =>
          TextMessage(write(sendFormattedError(suspendResponseFailure.errorCode, suspendResponseFailure.cause.getMessage, Some(REACTIVATE_SPOKER))))
      }
    }
  }

  def disableAccountOfUser(query: ActorRef, userId: String, targetId: String): Future[TextMessage] = {

    val disableResponse: Future[Any] = ask(query, Disable(userId, targetId))
    disableResponse.map { res =>
      res match {
        case disableResponseSuccess: DisableResponseSuccess =>
          TextMessage(write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(Message(disableResponseSuccess.message)), Some(DISABLE_USER))))

        case disableResponseFailure: DisableResponseFailure =>
          TextMessage(write(sendFormattedError(disableResponseFailure.errorCode, disableResponseFailure.cause.getMessage, Some(DISABLE_USER))))
      }
    }

  }

  def prmoteAccountOfUser(command: ActorRef, userId: String, userLevel: String, spokerId: String): Future[TextMessage] = {
    val disableResponse: Future[Any] = ask(command, promotUser(userId, userLevel, spokerId))
    disableResponse.map { res =>
      res match {
        case promotResponseSuccess: PromotUserAccountSuccess =>
          TextMessage(write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(Message(promotResponseSuccess.message)), Some(UPDATE_LEVEL))))

        case promotResponseFailure: PromotUserAccountFailure =>
          TextMessage(write(sendFormattedError(promotResponseFailure.errorCode, promotResponseFailure.cause.getMessage, Some(UPDATE_LEVEL))))
      }
    }
  }

  def disableAccountOfUserByHimeSelf(query: ActorRef, userId: String): Future[TextMessage] = {
    val disableResponse = ask(query, DisableUser(userId))
    disableResponse.map { res =>
      res match {
        case disableResponseSuccess: DisableResponseSuccess =>
          TextMessage(write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(Message(disableResponseSuccess.message)), Some(DISABLE_USER))))
        case disableResponseFailure: DisableResponseFailure =>
          TextMessage(write(sendFormattedError(disableResponseFailure.errorCode, disableResponseFailure.cause.getMessage, Some(DISABLE_USER))))

      }
    }
  }

  /**
   * This function will update user profile.
   *
   * @param command     The command actor ref.
   * @param phoneNumber user's phone number
   * @param userId      user's id
   * @return success message if user profile updated
   *         successfully else error message.
   */

  def updateUserProfileHandler(command: ActorRef, query: ActorRef, userId: String, phoneNumber: String, nickName: Option[String],
    birthDate: Option[String], gender: Option[String], geoLat: Option[Double], getLong: Option[Double], geoElev: Option[Double],
    geoText: Option[String], coverFile: Option[File], pictureFile: Option[File]): Future[String] = {

    val isUserSuspended: Future[IsUserSuspendedAsk] = ask(query, IsUserSuspended(userId)).mapTo[IsUserSuspendedAsk]
    val response: Future[Future[String]] = isUserSuspended.map { response =>
      response.status match {
        case SPOKER_SUSPENDED => Future(write(sendFormattedError(SPK_014, ACTION_IMPOSSIBLE, Some(UPDATE_USER_PROFILE))))
        case PROPERTY_NOT_FOUND => Future(write(sendFormattedError(MYA_101, USER_PROFILE_UPDATE_GENERIC_ERROR, Some(UPDATE_USER_PROFILE))))
        case SPOKER_NOT_SUSPENDED =>
          val userV = DSESpokApi.fetchUserV(userId)
          val geoV = DSESpokApi.fetchUserGeoV(userId)
          val userNickName = if (nickName.isDefined) nickName.get else userV.getProperty("nickname").getValue.asString()
          val userBirthDate = if (birthDate.isDefined) birthDate.get else userV.getProperty("birthDate").getValue.asString()
          val userGender = if (gender.isDefined) gender.get else userV.getProperty("gender").getValue.asString()
          val userGeoText = if (geoText.isDefined) geoText.get else userV.getProperty("geoText").getValue.asString()
          val latitude = if (geoLat.isDefined) geoLat.get else geoV.getProperty("latitude").getValue.asString().toDouble
          val longitude = if (getLong.isDefined) getLong.get else geoV.getProperty("longitude").getValue.asString().toDouble
          val elevation = if (geoElev.isDefined) geoElev.get else geoV.getProperty("elevation").getValue.asString().toDouble
          val correctBirthdate = if ((userBirthDate).length > 15) {
            val parseFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy")
            val date = parseFormat.parse(userBirthDate)
            val format = new SimpleDateFormat("yyyy-MM-dd")
            format.format(date)
          } else {
            userBirthDate
          }
          val validUserProfileDetails: Option[UserProfileJson] = Some(UserProfileJson(userNickName, correctBirthdate, userGender, None, None, Geo(latitude, longitude, elevation), userGeoText))
          validUserProfileDetails match {
            case Some(userProfileDetails) => {
              val validUserProfile: Option[List[Error]] = isValidUserProfile(userProfileDetails)
              handleValidUserProfileRequest(validUserProfile, userProfileDetails, command, userId,
                phoneNumber, coverFile, pictureFile)
            }
            case None => Future(write(sendJsonErrorWithEmptyData(Some(UPDATE_USER_PROFILE))))
          }
      }
    }
    response.flatMap(identity)
  }

  private def handleValidUserProfileRequest(validUserProfile: Option[List[Error]], userProfileDetails: UserProfileJson,
    command: ActorRef, userId: String, phoneNumber: String, coverFile: Option[File], pictureFile: Option[File]) = {
    validUserProfile match {
      case Some(errorList) => Future(write(generateCommonResponseForError(FAILED, Some(errorList), None, Some(UPDATE_USER_PROFILE))))
      case None => {
        if (dseGraphPersistenceFactoryApi.isUniqueUserNickname(userProfileDetails.nickname, userId)) {
          Future(write(sendFormattedError(IDT_122, NICKNAME_ALREADY_IN_USE, Some(UPDATE_USER_PROFILE))))
        } else handleValidUpdateUserProfileRequest(command, phoneNumber, userId, userProfileDetails, coverFile, pictureFile)
      }
    }
  }

  private def handleValidUpdateUserProfileRequest(command: ActorRef, phoneNumber: String,
    userId: String, userProfileDetails: UserProfileJson, coverFile: Option[File], pictureFile: Option[File]) = {
    val (uploadCoverFile, uploadCoverError) = if (coverFile.isDefined) {
      fileUploadUtility.uploadProfilePicture(coverFile.get)
    } else {
      (None, None)
    }
    val (uploadPictureFile, uploadPictureError) = if (pictureFile.isDefined) {
      fileUploadUtility.uploadProfilePicture(pictureFile.get)
    } else {
      (None, None)
    }
    val errorList: ListBuffer[Error] = ListBuffer()
    uploadCoverError match {
      case Some(someUploadCoverError) => errorList ++= someUploadCoverError
      case None => // Do Nothing
    }
    uploadPictureError match {
      case Some(someUploadPictureError) => errorList ++= someUploadPictureError
      case None => // Do Nothing
    }
    handleUpdateUserProfileRequest(command, phoneNumber, userId, errorList, uploadCoverFile, uploadPictureFile, userProfileDetails)
  }

  private def handleUpdateUserProfileRequest(command: ActorRef, phoneNumber: String, userId: String,
    errorList: ListBuffer[Error], uploadCoverFile: Option[String], uploadPictureFile: Option[String], userProfileDetails: UserProfileJson) = {
    if (errorList.nonEmpty) {
      Future(write(generateCommonResponseForError(FAILED, Some(errorList.toList), None, Some(UPDATE_USER_PROFILE))))
    } else {
      val newUserProfileDetails = userProfileDetails.copy(picture = uploadPictureFile, cover = uploadCoverFile)

      val date = if ((newUserProfileDetails.birthDate).length > 15) {
        val parseFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy")
        val date = parseFormat.parse(newUserProfileDetails.birthDate)
        val format = new SimpleDateFormat("yyyy-MM-dd")
        format.format(date)
      } else {
        newUserProfileDetails.birthDate
      }
      val format = new SimpleDateFormat(DATEFORMAT)
      val birthDate: Date = format.parse(date)
      val userProfile = UserProfile(newUserProfileDetails.nickname, birthDate, newUserProfileDetails.gender,
        newUserProfileDetails.picture, newUserProfileDetails.cover, newUserProfileDetails.geo, newUserProfileDetails.geoText)
      eventuateUpdateUserProfileHandler(command, phoneNumber, userId, userProfile)
    }
  }

  private def eventuateUpdateUserProfileHandler(command: ActorRef, phoneNumber: String, userId: String, userProfile: UserProfile): Future[String] = {
    val userProfileUpdate = ask(command, UpdateProfile(phoneNumber, userId, userProfile)).mapTo[AccountAck]
    userProfileUpdate.map { response =>
      response match {
        case UserProfileUpdateSuccess(msg) =>
          write(generateCommonResponseForCaseClass(SUCCESS, Some(List()),
            Some(Message(msg)), Some(UPDATE_USER_PROFILE)))
        case userProfileUpdateFailure: UserProfileUpdateFailure => write(sendFormattedError(
          userProfileUpdateFailure.errorCode,
          userProfileUpdateFailure.cause.getMessage, Some(UPDATE_USER_PROFILE)
        ))
      }
    }
  }

  private def extractUserProfile(userProfileJson: String): Option[UserProfileJson] = parse(userProfileJson).extractOpt[UserProfileJson]

  /**
   * This function will get list of followers of an user.
   *
   * @param query        account view actor ref.
   * @param targetUserId targetuser's id
   * @param userId       user's id
   * @param pos          Pagination position identifier.
   * @return list of followers if success otherwise error message.
   */
  def getUserFollowers(query: ActorRef, targetUserId: String, userId: String, pos: String): Future[String] = {
    val followersResponse = ask(query, GetFollowers(userId, targetUserId, pos))
    followersResponse.map { res =>
      res match {
        case followersResponseSuccess: FollowersResponseSuccess =>
          write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(followersResponseSuccess.userFollowers), Some(GET_FOLLOWERS)))
        case followersResponseFailure: FollowersResponseFailure =>
          write(sendFormattedError(followersResponseFailure.errorCode, followersResponseFailure.cause.getMessage, Some(GET_FOLLOWERS)))
      }
    }
  }

  /**
   * This function will get list of followings of an user.
   *
   * @param query        account view actor ref.
   * @param targetUserId targetuser's id
   * @param userId       user's id
   * @param pos          Pagination position identifier.
   * @return list of followings if success otherwise error message.
   */
  def getUserFollowings(query: ActorRef, targetUserId: String, userId: String, pos: String): Future[String] = {
    val followingsResponse = ask(query, GetFollowings(userId, targetUserId, pos))
    followingsResponse.map { res =>
      res match {
        case followingsResponseSuccess: FollowingsResponseSuccess =>
          write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(followingsResponseSuccess.userFollowings), Some(GET_FOLLOWINGS)))
        case followingsResponseFailure: FollowingsResponseFailure =>
          write(sendFormattedError(followingsResponseFailure.errorCode, followingsResponseFailure.cause.getMessage, Some(GET_FOLLOWINGS)))
      }
    }
  }

  /**
   * Handler to fetch details of all groups of a user
   *
   * @param query
   * @param userId
   * @return
   */
  def getDetailsOfGroupsForUser(query: ActorRef, userId: String, pos: Option[String]): Future[String] = {
    val validPos: String = pos match {
      case Some(pos) => pos
      case None => "1"
    }
    val followingsResponse = ask(query, GetGroupDetailsForUser(userId, validPos))
    followingsResponse.map { res =>
      res match {
        case getGroupDetailsForSuccess: GetGroupDetailsForSuccess =>
          logger.info(s"${userId}: Got group details: ")
          write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(getGroupDetailsForSuccess.response), Some(GET_GROUPS)))
        case getGroupDetailsForFailure: GetGroupDetailsForFailure =>
          write(sendFormattedError(GRP_104, LOAD_GROUP_DETAILS_GENERIC_ERROR, Some(GET_GROUPS)))
      }
    }
  }

  /**
   * handles updates of user settings
   *
   * @param command
   * @param userId
   * @return
   */
  def updateSettingsHandler(command: ActorRef, userId: String, phoneNumber: String, json: String): Future[TextMessage] = {
    val userSettings = extractUserSettingsDetails(json)
    userSettings match {
      case Some(userSetting) => {
        val followingsResponse = ask(command, UpdateUserSettings(userSetting, userId)).mapTo[AccountAck]
        followingsResponse.map {
          case followSettingUpdateSuccess: FollowSettingUpdateSuccess =>
            TextMessage(write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(Message(followSettingUpdateSuccess.message)), Some(FOLLOW_SETTINGS))))
          case followSettingUpdateFailure: FollowSettingUpdateFailure =>
            TextMessage(write(sendFormattedError(followSettingUpdateFailure.errorCode, followSettingUpdateFailure.cause.getMessage, Some(FOLLOW_SETTINGS))))
        }
      }
      case None => Future(TextMessage(write(sendJsonErrorWithEmptyData(Some(FOLLOW_SETTINGS)))))
    }
  }

  /**
   * Updates User Help Settings
   *
   * @param command
   * @param userId
   * @return
   */

  def updateHelpSettingsHandler(command: ActorRef, userId: String, phoneNumber: String, json: String): Future[TextMessage] = {
    val updatedResult = ask(command, UpdateUserHelpSettings(userId)).mapTo[AccountAck]
    updatedResult map {
      case HelpSettingUpdateSuccess(txt) =>
        TextMessage(write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(Message(txt)), Some(HELP_SETTINGS))))
      case helpSettingUpdateFailure: HelpSettingUpdateFailure =>
        TextMessage(write(sendFormattedError(helpSettingUpdateFailure.errorCode, helpSettingUpdateFailure.cause.getMessage, Some(HELP_SETTINGS))))
    }
  }

  private def extractUserSettingsDetails(userSettingsDetailsJson: String): Option[UserSetting] =
    parse(userSettingsDetailsJson).extractOpt[UserSetting]

  /**
   * This function will get my details.
   *
   * @param query  account view actor ref.
   * @param userId user's id
   * @return get my details if success otherwise error message
   */
  def viewMyDetail(query: ActorRef, userId: String): Future[String] = {
    val myDetailsResponse = ask(query, GetMyDetails(userId))
    myDetailsResponse.map { res =>
      res match {
        case myDetailsSuccess: MyDetailsSuccess =>
          write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(myDetailsSuccess.myDetails), Some(VIEW_MY_DETAILS)))
        case myDetailsFailure: MyDetailsFailure =>
          write(sendFormattedError(myDetailsFailure.errorCode, myDetailsFailure.cause.getMessage, Some(VIEW_MY_DETAILS)))
      }
    }
  }

  /**
   * This function will get loggedin users details.
   *
   * @param query  account view actor ref.
   * @param userId user's id
   * @return get my details if success otherwise error message
   */
  def viewDetail(query: ActorRef, userId: String): Future[String] = {
    val detailsResponse = ask(query, GetDetails(userId, None))
    detailsResponse.map { res =>
      res match {
        case detailsSuccess: DetailsSuccess =>
          write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(detailsSuccess.loggedUsersDetails), Some(GET_MY_PROFILE)))
        case detailsFailure: DetailsFailure =>
          write(sendFormattedError(detailsFailure.errorCode, detailsFailure.cause, Some(GET_MY_PROFILE)))
      }
    }
  }

  def viewDetailByAdmin(query: ActorRef, userId: String, targetId: String): Future[String] = {
    val detailsResponse = ask(query, GetDetails(userId, Some(targetId)))
    detailsResponse.map { res =>
      res match {
        case detailsSuccess: DetailsByAdminSuccess =>
          write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(detailsSuccess.spoker), Some(GET_USER_DETAILS_BY_ADMIN)))
        case detailsFailure: DetailsByAdminFailure =>
          write(sendFormattedError(detailsFailure.errorCode, detailsFailure.cause, Some(GET_USER_DETAILS_BY_ADMIN)))
      }
    }
  }

  /**
   *
   * @param command
   * @param userId of the user raising the support request
   * @param json   containing the message that the user has sent for the support team
   * @param phoneNumber
   * @return
   */
  def handleSupportRequests(command: ActorRef, userId: String, json: String, phoneNumber: String): Future[TextMessage] = {
    val messageOpt = (parse(json) \ MESSAGE).extractOpt[String]
    messageOpt match {
      case Some(message) => {
        val validMessage = isValidMessage(message)
        if (validMessage) sendEmailToSupportTeamHandler(command, userId, phoneNumber, message)
        else Future(TextMessage(write(sendFormattedError(IDT_008, INVALID_SUPPORT_MESSAGE, Some(SUPPORT)))))
      }
      case None => Future(TextMessage(write(sendJsonErrorWithEmptyData(Some(SUPPORT)))))
    }
  }

  private def sendEmailToSupportTeamHandler(command: ActorRef, userId: String, phoneNumber: String, message: String) = {
    val follows = ask(command, AskSupport(userId, phoneNumber, message)).mapTo[AccountAck]
    follows map {
      case supportProvidedSuccess: SupportProvidedSuccess => TextMessage(write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(Message(message)), Some(SUPPORT))))
      case supportProvidedFailure: SupportProvidedFailure => TextMessage(write(sendFormattedError(
        supportProvidedFailure.errorCode, supportProvidedFailure.cause.getMessage, Some(SUPPORT)
      )))
    }
  }

  /**
   *
   * @param query
   * @param userId   who is wanting to fetch the details of its group
   * @param groupId  of the specific group
   * @param position of the details to be fetched
   * @return
   */
  def viewOneGroup(query: ActorRef, userId: String, groupId: String, position: Option[String]): Future[String] = {
    val pos = position match {
      case Some(somePos) => somePos
      case None => "1"
    }
    val singleGroupDetails = ask(query, GetSingleGroupDetails(userId, groupId, pos))
    singleGroupDetails.map {
      case getSingleGroupDetailsSuccess: GetSingleGroupDetailsSuccess =>
        write(generateCommonResponseForCaseClass(SUCCESS, Some(List()), Some(getSingleGroupDetailsSuccess.singleGroupDetails), Some(SPECIFIC_GROUP)))
      case getSingleGroupDetailsFailure: GetSingleGroupDetailsFailure =>
        write(sendFormattedError(getSingleGroupDetailsFailure.errorId, getSingleGroupDetailsFailure.errorMessage, Some(SPECIFIC_GROUP)))

    }
  }

  /**
   * This method is used to delete talk id from redis on success of unfollow
   *
   * @param senderId sender id
   * @param targetId receiver id
   */
  private def removeTalkIdFromRedis(senderId: String, targetId: String): Future[Long] = {
    val isTalkExist = redisFactory.isTalkExist(senderId, targetId)
    isTalkExist flatMap { talk =>
      talk match {
        case true => redisFactory.removeTalkId(senderId, targetId)
        case false => Future(0L)
      }
    }
  }

}

