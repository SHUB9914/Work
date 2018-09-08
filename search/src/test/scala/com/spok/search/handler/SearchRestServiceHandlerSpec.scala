package com.spok.search.handler

import java.util.{ Date, UUID }

import akka.actor.{ Actor, ActorSystem }
import akka.stream.ActorMaterializer
import akka.testkit.TestActorRef
import com.spok.model.Account.MyDetails
import com.spok.model.SpokModel._
import com.spok.search.service._
import com.spok.util.Constant._
import org.scalatest.{ BeforeAndAfterAll, WordSpec }

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class SearchRestServiceHandlerSpec extends WordSpec with SearchRestServiceHandler with BeforeAndAfterAll {
  implicit val system = ActorSystem("search")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  override def afterAll() {
    system.terminate()
  }

  "SearchRestServiceHandlerSpec" should {

    "return list of nickname" in {
      val successOutput = """{"resource":"searchNickname","status":"success","errors":[],"data":[{"nickname":"Cyril","id":"userId"}]}"""
      val message = List("Cyril")
      val view = TestActorRef(new Actor {
        def receive = {
          case GetNicknames(nickname: String) ⇒ {
            sender ! GetNicknamesSuccess(List(Nickname("Cyril", "userId")))
          }
        }
      })
      val result = Await.result(getByNickname(view, "Cyril"), 5 second)
      assert(result === successOutput)
    }

    "not return list of nickname due to generic error" in {
      val errorOutput = """{"resource":"searchNickname","status":"failed","errors":[{"id":"SRH-106","message":"Unable searching nicknames (generic error)."}],"data":{}}"""
      val view = TestActorRef(new Actor {
        def receive = {
          case GetNicknames(nickname: String) ⇒ {
            sender ! GetNicknamesFailure(new Exception(UNABLE_SEARCHING_NICKNAME), SRH_106)
          }
        }
      })
      val result = Await.result(getByNickname(view, "Cyril"), 5 second)
      assert(result === errorOutput)
    }

    "return list of hastags" in {
      val successOutput = """{"resource":"searchHashtag","status":"success","errors":[],"data":{"hashtags":["awesome"]}}"""
      val message = List("awesome")
      val view = TestActorRef(new Actor {
        def receive = {
          case GetHashtags(hashtag: String) ⇒ {
            sender ! GetHashtagsSuccess(message)
          }
        }
      })
      val result = Await.result(getByHashtag(view, "awe"), 5 second)
      assert(result === successOutput)
    }

    "not return list of hastags due to generic error" in {
      val errorOutput = """{"resource":"searchHashtag","status":"failed","errors":[{"id":"SRH-107","message":"Unable searching hashtags (generic error)."}],"data":{}}"""
      val view = TestActorRef(new Actor {
        def receive = {
          case GetHashtags(hashtag: String) ⇒ {
            sender ! GetHashtagsFailure(new Exception(UNABLE_SEARCHING_HASHTAG), SRH_107)
          }
        }
      })
      val result = Await.result(getByHashtag(view, "awe"), 5 second)
      assert(result === errorOutput)
    }

    "return list of popular spoker" in {
      val successOutput =
        """{"resource":"searchPopularSpoker","status":"success","errors":[],"data":{"previous":"0","next":"2","spokers":[{"id":"ff11fa02-b64d-4b13-928c-b7919abb691e","nickname":"Cyril","gender":"male","picture":"testuser.jpg","cover":"","nbFollowers":4,"nbFollowing":0,"nbSpoks":0}]}}"""
      val message = PopularSpokerRes("0", "2", List(MyDetails("ff11fa02-b64d-4b13-928c-b7919abb691e", "Cyril", "male", "testuser.jpg", "", 4, 0, 0)))
      val view = TestActorRef(new Actor {
        def receive = {
          case GetPopularSpokers(pos: String) ⇒ {
            sender ! GetPopularSpokersSuccess(message)
          }
        }
      })
      val result = Await.result(getPopularSpoker(view, "1"), 5 second)
      assert(result === successOutput)
    }

    "not return list of  popular spoker due to generic error" in {
      val errorOutput = """{"resource":"searchPopularSpoker","status":"failed","errors":[{"id":"SRH_108","message":"Unable loading popular spokers list (generic error)."}],"data":{}}"""
      val view = TestActorRef(new Actor {
        def receive = {
          case GetPopularSpokers(pos: String) ⇒ {
            sender ! GetPopularSpokersFailure(new Exception(UNABLE_LOADING_SPOKER), SRH_108)
          }
        }
      })
      val result = Await.result(getPopularSpoker(view, "1"), 5 second)
      assert(result === errorOutput)
    }

    "return last 10 spoks" in {
      val spokId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val id1 = UUID.randomUUID().toString
      val author = Spoker(id, "ramesh", "male", "picture")
      val counters = Counters(1, 1, 1, 0.0)
      val content = Content(picturePreview = Some("picturePreview"), pictureFull = Some("pictureFull"))
      val spoksResponse = SpoksResponse("1", "2", List(LastSpoks(spokId, "text", 1000, new Date,
        "hi i am kias", author, "public", counters, content)))
      val spoksResJson = write(spoksResponse)
      val successOutput = s"""{"resource":"loadLastSpoks","status":"success","errors":[],"data":$spoksResJson}"""
      val view = TestActorRef(new Actor {
        def receive = {
          case GetLastSpoks(userId: String, pos: String) ⇒ {
            sender ! GetLastSpoksSuccess(spoksResponse)
          }
        }
      })
      val result = Await.result(getLastSpoks(view, id, "1"), 5 second)
      assert(result === successOutput)
    }

    "not return last 10 spoks due to generic error" in {
      val userId = getUUID()
      val errorOutput = s"""{"resource":"loadLastSpoks","status":"failed","errors":[{"id":"SRH-104","message":"$UNABLE_LOADING_LAST_SPOKS"}],"data":{}}"""
      val view = TestActorRef(new Actor {
        def receive = {
          case GetLastSpoks(userId: String, pos: String) ⇒ {
            sender ! GetLastSpoksFailure(new Exception(UNABLE_LOADING_LAST_SPOKS), SRH_104)
          }
        }
      })
      val result = Await.result(getLastSpoks(view, userId, "1"), 5 second)
      assert(result === errorOutput)
    }

    "return last 10 spoks of my friend" in {
      val spokId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val id1 = UUID.randomUUID().toString
      val author = Spoker(id, "ramesh", "male", "picture")
      val counters = Counters(1, 1, 1, 0.0)
      val content = Content(picturePreview = Some("picturePreview"), pictureFull = Some("pictureFull"))
      val spoksResponse = SpoksResponse("1", "2", List(LastSpoks(spokId, "text", 1000, new Date,
        "hi i am kias", author, "public", counters, content)))
      val spoksResJson = write(spoksResponse)
      val successOutput = s"""{"resource":"loadFriendSpoks","status":"success","errors":[],"data":$spoksResJson}"""
      val view = TestActorRef(new Actor {
        def receive = {
          case GetFriendSpoks(userId: String, pos: String) ⇒ {
            sender ! GetFriendSpoksSuccess(spoksResponse)
          }
        }
      })
      val result = Await.result(getFriendSpoks(view, id, "1"), 5 second)
      assert(result === successOutput)
    }

    "not return last 10 spoks of my friend due to generic error" in {
      val userId = getUUID()
      val errorOutput = s"""{"resource":"loadFriendSpoks","status":"failed","errors":[{"id":"SRH-103","message":"$UNABLE_LOADING_FRIEND_SPOKS"}],"data":{}}"""
      val view = TestActorRef(new Actor {
        def receive = {
          case GetFriendSpoks(userId: String, pos: String) ⇒ {
            sender ! GetFriendSpoksFailure(new Exception(UNABLE_LOADING_FRIEND_SPOKS), SRH_103)
          }
        }
      })
      val result = Await.result(getFriendSpoks(view, userId, "1"), 5 second)
      assert(result === errorOutput)
    }

    "return last 10 trendy spoks" in {
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val id1 = UUID.randomUUID().toString
      val author = Spoker(id, "ramesh", "male", "picture")
      val counters = Counters(1, 1, 1, 0.0)
      val content = Content(picturePreview = Some("picturePreview"), pictureFull = Some("pictureFull"))
      val spoksResponse = SpoksResponse("1", "2", List(LastSpoks(spokId, "text", 1000, new Date,
        "hi i am kias", author, "public", counters, content)))
      val spoksResJson = write(spoksResponse)
      val successOutput = s"""{"resource":"loadTrendySpoks","status":"success","errors":[],"data":$spoksResJson}"""
      val view = TestActorRef(new Actor {
        def receive = {
          case GetTrendySpoks(pos: String, userId: String) ⇒ {
            sender ! GetTrendySpoksSuccess(spoksResponse)
          }
        }
      })
      val result = Await.result(getTrendySpoks(view, "1", userId), 5 second)
      assert(result === successOutput)
    }

    "not return last 10 trendy spoks due to generic error" in {
      val userId = getUUID()
      val errorOutput = s"""{"resource":"loadTrendySpoks","status":"failed","errors":[{"id":"SRH-109","message":"$UNABLE_LOADING_TRENDY_SPOKS"}],"data":{}}"""
      val view = TestActorRef(new Actor {
        def receive = {
          case GetTrendySpoks(pos: String, userId: String) ⇒ {
            sender ! GetTrendySpoksFailure(new Exception(UNABLE_LOADING_TRENDY_SPOKS), SRH_109)
          }
        }
      })
      val result = Await.result(getTrendySpoks(view, "1", userId), 5 second)
      assert(result === errorOutput)
    }

    "return launch search spoks" in {
      val spokId = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val id = UUID.randomUUID().toString
      val id1 = UUID.randomUUID().toString
      val author = Spoker(id, "ramesh", "male", "picture")
      val counters = Counters(1, 1, 1, 0.0)
      val content = Content(picturePreview = Some("picturePreview"), pictureFull = Some("pictureFull"))
      val spoksResponse = SpoksResponse("1", "2", List(LastSpoks(spokId, "text", 1000, new Date,
        "hi i am kias", author, "public", counters, content)))
      val spoksResJson = write(spoksResponse)
      val successOutput = s"""{"resource":"launchSearch","status":"success","errors":[],"data":$spoksResJson}"""
      val view = TestActorRef(new Actor {
        def receive = {
          case GetLaunchSearch(pos: String, nickname: List[String], hashtags: List[String], latitude: String, longitude: String,
            start: String, end: String, contentTypes: List[String], userId: String) ⇒ {
            sender ! GetLaunchSearchSuccess(spoksResponse)
          }
        }
      })
      val result = Await.result(getlaunchSearch(view, "1", Some("cyril"), Some("fam"), "22.22", "23.33",
        "123", "456", Some("picture"), userId), 5 second)
      assert(result === successOutput)
    }

    "not return launch search spoks" in {
      val userId = getUUID()
      val errorOutput = s"""{"resource":"launchSearch","status":"failed","errors":[{"id":"SRH-105","message":"$UNABLE_SEARCHING_SPOKS"}],"data":{}}"""
      val view = TestActorRef(new Actor {
        def receive = {
          case GetLaunchSearch(pos: String, nickname: List[String], hashtags: List[String], latitude: String, longitude: String,
            start: String, end: String, contentTypes: List[String], userId: String) ⇒ {
            sender ! GetLaunchSearchFailure(new Exception(UNABLE_SEARCHING_SPOKS), SRH_105)
          }
        }
      })
      val result = Await.result(getlaunchSearch(view, "1", Some("cyril"), Some("fam"), "22.22", "23.33",
        "123", "456", Some("picture"), userId), 5 second)
      assert(result === errorOutput)
    }

  }
}
