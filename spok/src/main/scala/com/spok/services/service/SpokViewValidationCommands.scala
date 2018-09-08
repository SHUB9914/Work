package com.spok.services.service

/**
 * Contains all view commands of validation for spok
 */
object SpokViewValidationCommands {

  case class IsValidSpok(userId: String, spokId: String)

  case class IsValidSpokAndSendStatus(userId: String, spokId: String)

  case class IsValidPollQuestion(questionId: String)

  case class IsValidAbsoluteSpok(spokId: String)

  case class IsValidSpokWithEnabledFlag(spokId: String)

  case class IsUnspoked(spokId: String)

  case class IsValidSpokById(spokId: String)

  case class IsUserSuspended(spokerId: String)
}
