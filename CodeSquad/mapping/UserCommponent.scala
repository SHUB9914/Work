/*
package dbServiceUtil.mapping

import dbServiceUtil._
import model.CodeSquadModel.UserInfomation

import scala.concurrent.Future


trait UserCommponent extends UserMapping {
  this : DBComponent =>
  import driver.api._

  def insertUser(userInfomation: UserInfomation): Future[Int] = {
  db.run(userInfo+=userInfomation)
  }
}

object UserCommponent extends UserCommponent with DBFactory
*/
