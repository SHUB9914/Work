package com.spok.accountsservice.service

import com.spok.accountsservice.service.AccountManagerCommands.promotUser
import com.spok.model.Account.{ Group, UserProfile, UserSetting }

/**
 * Contains all the update commands for an account
 */
object AccountActorUpdateCommands {

  case class UpdateGroup(group: Group, userId: String)

  case class UpdateUserProfile(userId: String, userProfile: UserProfile)

  case class UpdateSettings(userSetting: UserSetting, userId: String)

  case class UpdateHelpSettings(userId: String)

  case class UpdatePhoneNumber(userId: String, newNumber: String)

  case class UpdateAllGroupsWhenUserIsUnfollowed(userId: String)

  case class promotUserAccount(userId: String, level: String, spokerId: String)

}
