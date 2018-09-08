package com.spok.search.service

import java.util.{ Date, UUID }

import akka.actor.{ ActorSystem, Props }
import akka.testkit.{ ImplicitSender, TestKit }
import com.spok.model.Account.MyDetails
import com.spok.model.SpokModel._
import com.spok.persistence.cassandra.CassandraProvider
import com.spok.persistence.factory.search.SearchApi
import com.spok.util.Constant._
import com.spok.util.RandomUtil
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ BeforeAndAfterAll, MustMatchers, WordSpecLike }

class SearchViewSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with WordSpecLike with MustMatchers with MockitoSugar
    with BeforeAndAfterAll with RandomUtil {

  def this() = this(ActorSystem("SearchActorSystem"))
  val session = CassandraProvider.session

  val mockedSearchApi: SearchApi = mock[SearchApi]

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
    session.get.close()
  }

  "A SearchView" must {

    "able to get list of nicknames" in {
      val actorRef = system.actorOf(Props(new SearchView {
        override val dseSearchApi: SearchApi = mockedSearchApi
      }))
      when(mockedSearchApi.getByNickname("Cyr")) thenReturn ((List(Nickname("Cyril", "userId")), true))
      actorRef ! GetNicknames("Cyr")
      expectMsgPF() { case GetNicknamesSuccess(nicknames) => nicknames mustBe List(Nickname("Cyril", "userId")) }
    }

    "not able to get list of nicknames" in {
      val actorRef = system.actorOf(Props(new SearchView {
        override val dseSearchApi: SearchApi = mockedSearchApi
      }))
      when(mockedSearchApi.getByNickname("Cyr")) thenReturn ((Nil, false))
      actorRef ! GetNicknames("Cyr")
      expectMsgPF() { case GetNicknamesFailure(error, errorCode) => error.getMessage mustBe UNABLE_SEARCHING_NICKNAME }
    }

    "able to get list of hashtags" in {
      val actorRef = system.actorOf(Props(new SearchView {
        override val dseSearchApi: SearchApi = mockedSearchApi
      }))
      when(mockedSearchApi.getByHashtag("Awe")) thenReturn ((List("Awesome"), true))
      actorRef ! GetHashtags("Awe")
      expectMsgPF() { case GetHashtagsSuccess(hashtag) => hashtag mustBe List("Awesome") }
    }

    "not able to get list of hashtags" in {
      val actorRef = system.actorOf(Props(new SearchView {
        override val dseSearchApi: SearchApi = mockedSearchApi
      }))
      when(mockedSearchApi.getByHashtag("Awe")) thenReturn ((Nil, false))
      actorRef ! GetHashtags("Awe")
      expectMsgPF() { case GetHashtagsFailure(error, errorCode) => error.getMessage mustBe UNABLE_SEARCHING_HASHTAG }
    }

    "able to get list of popular spoker" in {
      val actorRef = system.actorOf(Props(new SearchView {
        override val dseSearchApi: SearchApi = mockedSearchApi
      }))

      val result = PopularSpokerRes("0", "2", List(MyDetails("ff11fa02-b64d-4b13-928c-b7919abb691e", "Cyril", "male", "testuser.jpg", "", 4, 0, 0)))

      when(mockedSearchApi.getPopularSpokers("1")) thenReturn (Some(result))
      actorRef ! GetPopularSpokers("1")
      expectMsgPF() {
        case GetPopularSpokersSuccess(result) => result mustBe PopularSpokerRes("0", "2",
          List(MyDetails("ff11fa02-b64d-4b13-928c-b7919abb691e", "Cyril", "male", "testuser.jpg", "", 4, 0, 0)))
      }
    }

    "not able to get list of popular spoker" in {
      val actorRef = system.actorOf(Props(new SearchView {
        override val dseSearchApi: SearchApi = mockedSearchApi
      }))

      when(mockedSearchApi.getPopularSpokers("1")) thenReturn (None)
      actorRef ! GetPopularSpokers("1")
      expectMsgPF() {
        case GetPopularSpokersFailure(error, errorCode) => error.getMessage mustBe UNABLE_LOADING_SPOKER
      }
    }

    "able to get last 10 spoks" in {
      val userId = getUUID()
      val id = UUID.randomUUID().toString
      val id1 = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new SearchView {
        override val dseSearchApi: SearchApi = mockedSearchApi
      }))
      val spokId = UUID.randomUUID().toString
      val author = Spoker(id, "ramesh", "male", "picture")
      val counters = Counters(1, 1, 1, 0.0)
      val content = Content(picturePreview = Some("picturePreview"), pictureFull = Some("pictureFull"))
      val spoksResponse = Some(SpoksResponse("1", "2", List(LastSpoks(spokId, "text", 1000, new Date,
        "hi i am kias", author, "Public", counters, content))))
      when(mockedSearchApi.getLastSpoks(userId, "2")) thenReturn (spoksResponse)
      actorRef ! GetLastSpoks(userId, "2")
      expectMsgPF() { case GetLastSpoksSuccess(spoks) => spoks.previous mustBe "1" }
    }

    "not able to get last 10 spoks generic error" in {
      val userId = getUUID()
      val actorRef = system.actorOf(Props(new SearchView {
        override val dseSearchApi: SearchApi = mockedSearchApi
      }))
      when(mockedSearchApi.getLastSpoks(userId, "1")) thenReturn None
      actorRef ! GetLastSpoks(userId, "1")
      expectMsgPF() { case GetLastSpoksFailure(error, errorCode) => error.getMessage mustBe UNABLE_LOADING_LAST_SPOKS }
    }

    "able to get last 10 trendy spoks" in {
      val id = UUID.randomUUID().toString
      val userId = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new SearchView {
        override val dseSearchApi: SearchApi = mockedSearchApi
      }))
      val spokId = UUID.randomUUID().toString
      val author = Spoker(id, "ramesh", "male", "picture")
      val counters = Counters(1, 1, 1, 0.0)
      val content = Content(picturePreview = Some("picturePreview"), pictureFull = Some("pictureFull"))
      val spoksResponse = Some(SpoksResponse("1", "2", List(LastSpoks(spokId, "text", 1000, new Date,
        "hi i am kias", author, "Public", counters, content))))
      when(mockedSearchApi.getTrendySpok("2", userId)) thenReturn (spoksResponse)
      actorRef ! GetTrendySpoks("2", userId)
      expectMsgPF() { case GetTrendySpoksSuccess(spoks) => spoks.previous mustBe "1" }
    }

    "not able to get last 10 trendy spoks due to generic error" in {
      val userId = getUUID()
      val actorRef = system.actorOf(Props(new SearchView {
        override val dseSearchApi: SearchApi = mockedSearchApi
      }))
      when(mockedSearchApi.getTrendySpok("1", userId)) thenReturn None
      actorRef ! GetTrendySpoks("1", userId)
      expectMsgPF() { case GetTrendySpoksFailure(error, errorCode) => error.getMessage mustBe UNABLE_LOADING_TRENDY_SPOKS }
    }

    "able to get last 10 spoks of my friend" in {
      val userId = getUUID()
      val id = UUID.randomUUID().toString
      val id1 = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new SearchView {
        override val dseSearchApi: SearchApi = mockedSearchApi
      }))
      val spokId = UUID.randomUUID().toString
      val author = Spoker(id, "ramesh", "male", "picture")
      val counters = Counters(1, 1, 1, 0.0)
      val content = Content(picturePreview = Some("picturePreview"), pictureFull = Some("pictureFull"))
      val spoksResponse = Some(SpoksResponse("1", "2", List(LastSpoks(spokId, "text", 1000, new Date,
        "hi i am kias", author, "Public", counters, content))))
      when(mockedSearchApi.getFriendSpoks(userId, "2")) thenReturn (spoksResponse)
      actorRef ! GetFriendSpoks(userId, "2")
      expectMsgPF() { case GetFriendSpoksSuccess(spoks) => spoks.previous mustBe "1" }
    }

    "not able to get last 10 spoks of my friend generic error" in {
      val userId = getUUID()
      val actorRef = system.actorOf(Props(new SearchView {
        override val dseSearchApi: SearchApi = mockedSearchApi
      }))
      when(mockedSearchApi.getFriendSpoks(userId, "1")) thenReturn None
      actorRef ! GetFriendSpoks(userId, "1")
      expectMsgPF() { case GetFriendSpoksFailure(error, errorCode) => error.getMessage mustBe UNABLE_LOADING_FRIEND_SPOKS }
    }

    "able to get launch search" in {
      val userId = getUUID()
      val id = UUID.randomUUID().toString
      val id1 = UUID.randomUUID().toString
      val actorRef = system.actorOf(Props(new SearchView {
        override val dseSearchApi: SearchApi = mockedSearchApi
      }))
      val spokId = UUID.randomUUID().toString
      val author = Spoker(id, "ramesh", "male", "picture")
      val counters = Counters(1, 1, 1, 0.0)
      val content = Content(picturePreview = Some("picturePreview"), pictureFull = Some("pictureFull"))
      val spoksResponse = Some(SpoksResponse("1", "2", List(LastSpoks(spokId, "text", 1000, new Date,
        "hi i am kias", author, "Public", counters, content))))
      when(mockedSearchApi.getlaunchSearch("2", List("cyrilids"), List("abc"),
        "11.22", "22.13", "111", "999", List("picture"), userId)) thenReturn (spoksResponse)
      actorRef ! GetLaunchSearch("2", List("cyrilids"), List("abc"),
        "11.22", "22.13", "111", "999", List("picture"), userId)
      expectMsgPF() { case GetLaunchSearchSuccess(spoks) => spoks.previous mustBe "1" }
    }

    "not able to get launch search due to generic error" in {
      val userId = getUUID()
      val actorRef = system.actorOf(Props(new SearchView {
        override val dseSearchApi: SearchApi = mockedSearchApi
      }))
      when(mockedSearchApi.getlaunchSearch("1", List("cyrilids"), List("abc"),
        "11.22", "22.13", "111", "999", List("picture"), userId)) thenReturn None
      actorRef ! GetLaunchSearch("1", List("cyrilids"), List("abc"),
        "11.22", "22.13", "111", "999", List("picture"), userId)
      expectMsgPF() { case GetLaunchSearchFailure(error, errorCode) => error.getMessage mustBe UNABLE_SEARCHING_SPOKS }
    }

  }

}
