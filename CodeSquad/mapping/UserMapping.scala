/*
package dbServiceUtil.mapping

import model.CodeSquadModel.UserInfomation
import slick.jdbc.MySQLProfile.api._


trait UserMapping {

  class UserMapping(tag : Tag) extends Table[UserInfomation](tag , ""){

    val username = column[String]("username" ,  O.PrimaryKey)

    val password = column[String]("password")

    val email_id = column[String]("email_id")

    def * = (username, password, email_id) <> (UserInfomation.tupled, UserInfomation.unapply)

  }
  val userInfo = TableQuery[UserMapping]

}

*/
