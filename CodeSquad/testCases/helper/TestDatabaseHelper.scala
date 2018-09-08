package helper

import java.util.UUID
import util.Constant._
import anorm._
import play.api.db.Database
import util.SqlQuery._

class TestDatabaseHelper(db: Database) {

  def insertDummyRecords = {
    /**
     * Adding Users
     */
    db.withConnection { implicit connection =>
      SQL(ADD_NEW_USER)
        .on("username" -> "divya", "password" -> "12345678", "email_id" -> "harsh@knoldus.com")
        .executeInsert()

      SQL(ADD_NEW_USER)
        .on("username" -> "raghav", "password" -> "12345678", "email_id" -> "raghav@knoldus.com")
        .executeInsert()

      SQL(ADD_NEW_USER)
        .on("username" -> "deepak", "password" -> "12345678", "email_id" -> "deepak@knoldus.com")
        .executeInsert()
    }

    /**
     * Adding Projects
     */
    db.withConnection { implicit connection =>
      SQL(INSERT_USER_DETAILS)
        .on(
          "p_name" -> "DQR",
          "access_key" -> UUID.randomUUID().toString,
          "secret_key" -> UUID.randomUUID().toString
        )
        .executeInsert()
    }

    /**
     * Adding relations with roles
     */
    db.withConnection { implicit connection =>
      SQL(INSERT_USER_PROJECT)
        .on(
          "username" -> "divya",
          "project" -> "DQR",
          "is_admin" -> YES
        )
        .executeInsert()

      SQL(INSERT_USER_PROJECT)
        .on(
          "username" -> "deepak",
          "project" -> "DQR",
          "is_admin" -> NO
        )
        .executeInsert()
    }
  }

  def insertDummyPasswordLink = {
    db.withConnection { implicit connection =>
      SQL(INSERT_PASSWORD_LINK)
        .on(
          "id" -> "111",
          "email" -> "harsh@knoldus.com",
          "is_expired" -> "false"
        ).executeInsert()
    }
  }
}
