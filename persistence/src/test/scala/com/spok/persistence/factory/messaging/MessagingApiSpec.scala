package com.spok.persistence.factory.messaging

import com.datastax.driver.core.utils.UUIDs
import com.spok.model.Messaging._
import com.spok.persistence.dse.DseConnectionUri._
import com.spok.persistence.factory.spokLog.SpokerDetails
import com.spok.persistence.factory.spoklog.SpokLogging
import com.spok.util.Constant._
import org.scalatest.{ BeforeAndAfterAll, FlatSpec, Matchers }

class MessagingApiSpec extends FlatSpec with Matchers with MessagingApi with BeforeAndAfterAll {

  val spokLogger = SpokLogging
  val time1 = timeStamp
  val time2 = timeStamp + 1000

  def intialSteps(
    senderId: String,
    receiverId1: String,
    receiverId2: String
  ) = {
    val sender = write(SpokerDetails(senderId, "piyush", "male", "picture1"))
    val receiver1 = write(SpokerDetails(receiverId1, "prashant", "male", "picture2"))
    val receiver2 = write(SpokerDetails(receiverId2, "narayan", "male", "picture3"))
    spokLogger.insertMesssagingDetails(sender, spokerDetails)
    spokLogger.insertMesssagingDetails(receiver1, spokerDetails)
    spokLogger.insertMesssagingDetails(receiver2, spokerDetails)
  }

  behavior of "MessagingApiSpec "

  it should "be able to insert user messages details and return correct response" in {
    val senderId = getUUID()
    val receiverId1 = getUUID()
    val receiverId2 = getUUID()
    val messageId1 = UUIDs.timeBased()
    intialSteps(senderId, receiverId1, receiverId2)

    val mesasgeDetails = Message(senderId, receiverId1, messageId1.toString, "hi", time1)
    val messageResponse = Some(MessageResponse(User(senderId, "piyush", "male", "picture1"), User(receiverId1, "prashant", "male", "picture2"), MessageDetail(messageId1.toString, "hi", None)))
    val res: Option[MessageResponse] = insertMessagesDetailsPerUser(mesasgeDetails)
    assert(res == messageResponse)
  }

  it should "be able to fetch talk between two users when no message id is passed" in {

    val senderId = getUUID()
    val receiverId1 = getUUID()
    val receiverId2 = getUUID()
    val messageId1 = UUIDs.timeBased()
    Thread.sleep(1000)
    val messageId2 = UUIDs.timeBased()
    intialSteps(senderId, receiverId1, receiverId2)

    val messsage1 = Message(senderId, receiverId1, messageId1.toString, "hi", time1)
    val messsage2 = Message(senderId, receiverId1, messageId2.toString, "hello", time2)

    val res: Option[MessageResponse] = insertMessagesDetailsPerUser(messsage1)
    val res1: Option[MessageResponse] = insertMessagesDetailsPerUser(messsage2)
    val (talkres, error) = getUserTalk(None, senderId, receiverId1, "desc")
    assert(talkres.get.me.id == senderId.toString)
    assert(talkres.get.user.id == receiverId1.toString)
    assert(talkres.get.messages.head.id == messageId2.toString)
    assert(error == None)
  }

  it should "be able to fetch talk between two users when when message id is passed in desc order" in {

    val senderId = getUUID()
    val receiverId1 = getUUID()
    val receiverId2 = getUUID()
    val messageId1 = UUIDs.timeBased()
    Thread.sleep(1000)
    val messageId2 = UUIDs.timeBased()
    intialSteps(senderId, receiverId1, receiverId2)

    val messsage1 = Message(senderId, receiverId1, messageId1.toString, "hi", time1)
    val messsage2 = Message(senderId, receiverId1, messageId2.toString, "hello", time2)

    val res: Option[MessageResponse] = insertMessagesDetailsPerUser(messsage1)
    val res1: Option[MessageResponse] = insertMessagesDetailsPerUser(messsage2)
    val (talkres, error) = getUserTalk(Some(messageId2.toString), senderId, receiverId1, "desc")
    assert(talkres.get.me.id == senderId.toString)
    assert(talkres.get.user.id == receiverId1.toString)
    assert(talkres.get.messages.head.id == messageId1.toString)
    assert(error == None)
  }

  it should "be able to fetch talk between two users when message id is passed in asc order" in {

    val senderId = getUUID()
    val receiverId1 = getUUID()
    val receiverId2 = getUUID()
    val messageId1 = UUIDs.timeBased()
    Thread.sleep(1000)
    val messageId2 = UUIDs.timeBased()
    Thread.sleep(1000)
    val messageId3 = UUIDs.timeBased()
    intialSteps(senderId, receiverId1, receiverId2)

    val messsage1 = Message(senderId, receiverId1, messageId1.toString, "hi", time1)
    val messsage2 = Message(senderId, receiverId1, messageId2.toString, "hello", time2)
    val messsage3 = Message(senderId, receiverId1, messageId3.toString, "how r u?", time2 + 1000)

    val res: Option[MessageResponse] = insertMessagesDetailsPerUser(messsage1)
    val res1: Option[MessageResponse] = insertMessagesDetailsPerUser(messsage2)
    val res2: Option[MessageResponse] = insertMessagesDetailsPerUser(messsage3)
    val (talkres, error) = getUserTalk(Some(messageId1.toString), senderId, receiverId1, "asc")
    assert(talkres.get.me.id == senderId.toString)
    assert(talkres.get.user.id == receiverId1.toString)
    assert(talkres.get.messages.head.id == messageId2.toString)
    assert(error == None)
  }

  it should "be able to fetch talk between two users when no message id is passed in asc order" in {

    val senderId = getUUID()
    val receiverId1 = getUUID()
    val receiverId2 = getUUID()
    val messageId1 = UUIDs.timeBased()
    Thread.sleep(1000)
    val messageId2 = UUIDs.timeBased()
    Thread.sleep(1000)
    val messageId3 = UUIDs.timeBased()
    intialSteps(senderId, receiverId1, receiverId2)

    val messsage1 = Message(senderId, receiverId1, messageId1.toString, "hi", time1)
    val messsage2 = Message(senderId, receiverId1, messageId2.toString, "hello", time2)
    val messsage3 = Message(senderId, receiverId1, messageId3.toString, "how r u?", time2 + 1000)

    val res: Option[MessageResponse] = insertMessagesDetailsPerUser(messsage1)
    val res1: Option[MessageResponse] = insertMessagesDetailsPerUser(messsage2)
    val res2: Option[MessageResponse] = insertMessagesDetailsPerUser(messsage3)
    val (talkres, error) = getUserTalk(None, senderId, receiverId1, "asc")
    assert(talkres.get.me.id == senderId.toString)
    assert(talkres.get.user.id == receiverId1.toString)
    assert(talkres.get.messages.head.id == messageId1.toString)
    assert(error == None)
  }

  it should "be able to remove talk for user" in {

    val senderId = getUUID()
    val receiverId1 = getUUID()
    val receiverId2 = getUUID()
    val messageId1 = UUIDs.timeBased()
    Thread.sleep(1000)
    val messageId2 = UUIDs.timeBased()
    Thread.sleep(1000)
    val messageId3 = UUIDs.timeBased()
    intialSteps(senderId, receiverId1, receiverId2)

    val messsage1 = Message(senderId, receiverId1, messageId1.toString, "hi", time1)
    val messsage2 = Message(senderId, receiverId1, messageId2.toString, "hello", time2)
    val messsage3 = Message(receiverId1, senderId, messageId3.toString, "how r u?", time2 + 1000)

    val res: Option[MessageResponse] = insertMessagesDetailsPerUser(messsage1)
    val res1: Option[MessageResponse] = insertMessagesDetailsPerUser(messsage2)
    val res2: Option[MessageResponse] = insertMessagesDetailsPerUser(messsage3)
    val (talkres, error) = removeTalkDetails(senderId, receiverId1)
    assert(talkres.get == "Talk deleted successfully")
    assert(error == None)

  }

  it should "be able to remove message and update the talk table" in {

    val senderId = getUUID()
    val receiverId1 = getUUID()
    val receiverId2 = getUUID()
    val messageId1 = UUIDs.timeBased()
    Thread.sleep(1000)
    val messageId2 = UUIDs.timeBased()
    Thread.sleep(1000)
    val messageId3 = UUIDs.timeBased()
    intialSteps(senderId, receiverId1, receiverId2)

    val messsage1 = Message(senderId, receiverId1, messageId1.toString, "hi", time1)
    val messsage2 = Message(senderId, receiverId1, messageId2.toString, "hello", time2)
    val messsage3 = Message(receiverId1, senderId, messageId3.toString, "how r u?", time2 + 1000)

    val res: Option[MessageResponse] = insertMessagesDetailsPerUser(messsage1)
    val res1: Option[MessageResponse] = insertMessagesDetailsPerUser(messsage2)
    val res2: Option[MessageResponse] = insertMessagesDetailsPerUser(messsage3)
    val (talkres, error) = removeMessageById(senderId, receiverId1, messageId3.toString)
    assert(talkres.get == "Message deleted successfully")
    assert(error == None)
  }

  it should "be able to remove only present message of user and update talk table " in {
    val senderId = getUUID()
    val receiverId1 = getUUID()
    val receiverId2 = getUUID()
    val messageId1 = UUIDs.timeBased()

    intialSteps(senderId, receiverId1, receiverId2)
    val messsage1 = Message(senderId, receiverId1, messageId1.toString, "hi", time1)

    val res: Option[MessageResponse] = insertMessagesDetailsPerUser(messsage1)
    val (talkres, error) = removeMessageById(senderId, receiverId1, messageId1.toString)
    assert(talkres.get == "Message deleted successfully")
    assert(error == None)

  }

  it should "be able to fetch talk list for user" in {

    val senderId = getUUID()
    val receiverId1 = getUUID()
    val receiverId2 = getUUID()
    val messageId1 = UUIDs.timeBased()
    Thread.sleep(1000)
    val messageId2 = UUIDs.timeBased()
    intialSteps(senderId, receiverId1, receiverId2)

    val messsage1 = Message(senderId, receiverId1, messageId1.toString, "hi", time1)
    val messsage2 = Message(senderId, receiverId2, messageId2.toString, "hello", time2)

    val res: Option[MessageResponse] = insertMessagesDetailsPerUser(messsage1)
    val res1: Option[MessageResponse] = insertMessagesDetailsPerUser(messsage2)
    Thread.sleep(15000)
    info("fetching talk List")
    val (talksres, error) = getTalkLists("1", senderId)
    assert(talksres.get.previous == "1")
    assert(talksres.get.next == "")
    assert(talksres.get.talks.head.user.id == receiverId2)
    assert(talksres.get.talks.head.last.text == "hello")
    assert(error == None)
  }

  it should "be able to search messages as full text search for a user" in {

    val senderId = getUUID()
    val receiverId1 = getUUID()
    val receiverId2 = getUUID()
    val messageId1 = UUIDs.timeBased()
    Thread.sleep(1000)
    val messageId2 = UUIDs.timeBased()
    intialSteps(senderId, receiverId1, receiverId2)
    val text1 = "R u online? buddy"
    val text2 = "yes I am online. how r u?"

    val messsage1 = Message(senderId, receiverId1, messageId1.toString, text1, time1)
    val messsage2 = Message(receiverId1, senderId, messageId2.toString, text2, time2)

    insertMessagesDetailsPerUser(messsage1)
    insertMessagesDetailsPerUser(messsage2)
    Thread.sleep(15000)
    info("fetching talk List")
    val (res, error) = fullTextMessageSearch(senderId, "online")
    assert(res.get.head.id == messageId2.toString)
    assert(res.get.head.senderId == receiverId1)
    assert(res.get.head.text == text2)
  }

  it should "be able to search talker provided a usernickname" in {

    val senderId = getUUID()
    val receiverId1 = getUUID()
    val receiverId2 = getUUID()
    val messageId1 = UUIDs.timeBased()
    val text1 = "R u online? buddy"
    intialSteps(senderId, receiverId1, receiverId2)
    val messsage1 = Message(senderId, receiverId1, messageId1.toString, text1, time1)
    insertMessagesDetailsPerUser(messsage1)
    Thread.sleep(15000)
    val (res, error) = searchTalker("piy")
    assert(res.get.head.gender == "male")
    assert(res.get.head.nickname == "piyush")
    assert(res.get.size >= 1)
  }

  it should "be able to update read time for message" in {

    val senderId = getUUID()
    val receiverId1 = getUUID()
    val receiverId2 = getUUID()
    val messageId1 = UUIDs.timeBased()
    val text1 = "R u online? buddy"
    intialSteps(senderId, receiverId1, receiverId2)
    val messsage1 = Message(senderId, receiverId1, messageId1.toString, text1, time1)
    insertMessagesDetailsPerUser(messsage1)
    val (res, error) = readMessageUpdate(senderId, receiverId1, messageId1.toString)
    assert(error == None)
    assert(res.get == "Saved Successfully")
  }

  it should "not be able to update read time for message when targetId is incorrect" in {

    val senderId = getUUID()
    val receiverId1 = getUUID()
    val receiverId2 = getUUID()
    val messageId1 = UUIDs.timeBased()
    val text1 = "R u online? buddy"
    intialSteps(senderId, receiverId1, receiverId2)
    val messsage1 = Message(senderId, receiverId1, messageId1.toString, text1, time1)
    insertMessagesDetailsPerUser(messsage1)

    val (res, error) = readMessageUpdate(senderId, "IncorrectTragetUserId", messageId1.toString)
    assert(error.get.id == MSG_002)
    assert(error.get.message == "Talk IncorrectTragetUserId not found.")
    assert(res == None)
  }

  it should "not be able to update read time for message when messageid is incorrect" in {

    val senderId = getUUID()
    val receiverId1 = getUUID()
    val receiverId2 = getUUID()
    val messageId1 = UUIDs.timeBased()
    val messageId2 = UUIDs.timeBased().toString
    val text1 = "R u online? buddy"
    intialSteps(senderId, receiverId1, receiverId2)
    val messsage1 = Message(senderId, receiverId1, messageId1.toString, text1, time1)
    insertMessagesDetailsPerUser(messsage1)

    val (res, error) = readMessageUpdate(senderId, receiverId1, messageId2.toString)
    assert(error.get.id == MSG_002)
    assert(error.get.message == s"Talk $receiverId1 not found.")
    assert(res == None)
  }

  it should "not be able to update read time for message due to generic error" in {

    val senderId = getUUID()
    val receiverId1 = getUUID()
    val receiverId2 = getUUID()
    val messageId1 = UUIDs.timeBased()
    val messageId2 = UUIDs.timeBased().toString
    val text1 = "R u online? buddy"
    intialSteps(senderId, receiverId1, receiverId2)
    val messsage1 = Message(senderId, receiverId1, messageId1.toString, text1, time1)
    insertMessagesDetailsPerUser(messsage1)

    val (res, error) = readMessageUpdate(senderId, receiverId1, "messageid")
    assert(error.get.id == MSG_107)
    assert(error.get.message == s"Unable to flag message messageid as read (generic error).")
    assert(res == None)
  }

}
