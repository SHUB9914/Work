package com.spok.model

object Search {

  case class BatchSpok(
    spokId: String,
    loggedTime: Long,
    data: String
  )

  case class PopularSpoker(
    spokerId: String,
    loggedTime: Long,
    data: String
  )
}
