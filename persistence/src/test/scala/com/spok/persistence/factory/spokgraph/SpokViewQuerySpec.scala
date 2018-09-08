package com.spok.persistence.factory.spokgraph

import com.spok.util.Constant._
import com.spok.util.RandomUtil
import org.scalatest.{ FlatSpec, Matchers }

class SpokViewQuerySpec extends FlatSpec with Matchers with SpokViewQuery with RandomUtil {

  behavior of "SpokViewQuerySpec "

  it should "be able to get spok stats query" in {
    val spokId1 = getUUID()
    val result = getSpokStatsQuery(spokId1)
    val expectedOutput = s"""g.V().hasLabel('spok').has('spokId','$spokId1').outE('hasStats').inV()"""
    assert(result == expectedOutput)
  }

  it should "be able to get get the 10 comments of a spok query" in {
    val spokId1 = getUUID()
    val fromPosNo = 1
    val toPosNo = 2
    val result = getCommentsQuery(spokId1, fromPosNo, toPosNo)
    val expectedOutput = s"""g.V().hasLabel('spok').has('spokId','$spokId1').outE('hasAComment').order()
                             |.by('launched',decr).inV().range($fromPosNo,$toPosNo)""".stripMargin
    assert(result == expectedOutput)
  }

  it should "be able to get the total comments of a spok query" in {
    val spokId1 = getUUID()
    val result = getTotalCommentsQuery(spokId1)
    val expectedOutput = s"""g.V().hasLabel('spok').has('spokId','$spokId1').outE('hasAComment').count()"""
    assert(result == expectedOutput)
  }

  it should "be able to get get the time stamp of a comment query" in {
    val spokId1 = getUUID()
    val commentId1 = getUUID()
    val commentId2 = getUUID()
    val result1 = getCommentsTimeStamp(spokId1, commentId1)
    val result2 = getCommentsTimeStamp(spokId1, commentId2)
    val expectedOutput1 = s"""g.V().has('$COMMENT','$COMMENT_ID','$commentId1').values('$TIMESTAMP')"""
    val expectedOutput2 = s"""g.V().has('$COMMENT','$COMMENT_ID','$commentId2').values('$TIMESTAMP')"""
    assert(result1 == expectedOutput1)
    assert(result2 == expectedOutput2)
  }

  it should "be able to get get 10 respokes of a spok query" in {
    val spokId1 = getUUID()
    val fromPosNo = 1
    val toPosNo = 2
    val result = getReSpokersQuery(spokId1, fromPosNo, toPosNo)
    val expectedOutput =
      s"""g.V().hasLabel('spok').has('spokId','$spokId1').has('enabled','true').inE('isAssociatedWith').has('status','respoked')
         |.order().by('launched',decr).outV().range($fromPosNo,$toPosNo)""".stripMargin

    assert(result == expectedOutput)
  }

  it should "be able to get get 10 scoped user of a spok query" in {
    val spokId1 = getUUID()
    val fromPosNo = 1
    val toPosNo = 2
    val result = getScopedUsersQuery(spokId1, fromPosNo, toPosNo)
    val expectedOutput =
      s"""g.V().hasLabel('spok').has('spokId','$spokId1').has('enabled','true').inE('isAssociatedWith').has('status','pending')
         |.order().by('launched',decr).outV().hasLabel('users').range($fromPosNo,$toPosNo)""".stripMargin
    assert(result == expectedOutput)
  }

  it should "be able to get get 10 spoks of a user query" in {
    val userId = getUUID()
    val fromPosNo = 1
    val toPosNo = 2
    val result = getMySpokQuery(userId, fromPosNo, toPosNo)
    val expectedOutput =
      s"""g.V().hasLabel('spok').has('enabled','true').has('author','$userId').order().by('launched',decr).range($fromPosNo,$toPosNo)""".stripMargin
    assert(result == expectedOutput)
  }

}
