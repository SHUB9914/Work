package com.spok.persistence.factory.messaging

import com.datastax.driver.core.ResultSet
import com.spok.model.Messaging._
import com.spok.model.SpokModel.Error
import com.spok.persistence.cassandra.CassandraMessageProvider._
import com.spok.persistence.dse.DseConnectionUri._
import com.spok.persistence.dse.DseGraphFactory
import com.spok.persistence.factory.dsequery.DSEUserQuery
import com.spok.persistence.factory.spoklog.SpokLogging
import com.spok.util.Constant._
import com.spok.util.{ JsonHelper, LoggerUtil, RandomUtil }
import org.apache.solr.common.SolrDocument

import scala.collection.JavaConverters._

trait MessagingApi extends MessagingQuery with DSEUserQuery with JsonHelper with LoggerUtil with RandomUtil with SpokLogging {

  val messageLogging: SpokLogging = SpokLogging

  /**
   * This function will store message details in batch.
   *
   * @param messageDetails
   * @return
   */
  def insertMessagesDetailsPerUser(messageDetails: Message): Option[MessageResponse] = {
    val finalMessageData = messageDetails.copy(text = getValidContent(messageDetails.text))
    val sender: UserMessage = UserMessage(finalMessageData.senderId, finalMessageData.receiverId, finalMessageData.messageId, finalMessageData.text,
      finalMessageData.time, finalMessageData.senderId, finalMessageData.time)
    val receiver = UserMessage(finalMessageData.receiverId, finalMessageData.senderId, finalMessageData.messageId, finalMessageData.text,
      finalMessageData.time, finalMessageData.senderId)
    val (user1Nickname, user2Nickname) = try {
      (
        cassandraMessageConn.execute(getSpokerDetails(finalMessageData.senderId)).one().getString("nickname"),
        cassandraMessageConn.execute(getSpokerDetails(finalMessageData.receiverId)).one().getString("nickname")
      )
    } catch {
      case ex: Exception => ("", "")
    }
    val senderTalkDetail = TalkDetails(finalMessageData.senderId, finalMessageData.receiverId, finalMessageData.time, finalMessageData.text,
      finalMessageData.senderId, user2Nickname)
    val receiverTalkDetail = TalkDetails(finalMessageData.receiverId, finalMessageData.senderId, finalMessageData.time, finalMessageData.text,
      finalMessageData.senderId, user1Nickname)
    insertMessagingDetails(insertMessages(sender, receiver), senderTalkDetail, receiverTalkDetail, messageDetails)
  }

  private def getValidContent(content: String): String = content.replaceAll("'", "''")

  /**
   * This method insert messaging details using batch insert
   *
   * @param messageInsertQuery
   * @return
   */
  private def insertMessagingDetails(messageInsertQuery: String, senderTalkDetail: TalkDetails, receiverTalkDetail: TalkDetails, message: Message): Option[MessageResponse] = {
    try {
      val messageRes: ResultSet = cassandraMessageConn.execute(messageInsertQuery)
      val talksDeleted: ResultSet = cassandraMessageConn.execute(deleteFromTalks(message.senderId, message.receiverId))
      val talkRes: ResultSet = cassandraMessageConn.execute(insertTalks(senderTalkDetail, receiverTalkDetail))
      getInsertedMessageResponse(message)
    } catch {
      case ex: Exception =>
        info(s"Exception while inserting message ${ex.getMessage}")
        None
    }
  }

  private def getInsertedMessageResponse(message: Message): Option[MessageResponse] = {
    try {
      val (sender: User, receiver: User) = getSenderReceiverDetails(message.senderId, message.receiverId)
      Some(MessageResponse(sender, receiver, MessageDetail(message.messageId.toString, message.text)))
    } catch {
      case ex: Exception =>
        info(s"Exception while getting sender and receiver details")
        None
    }
  }

  private def getSenderReceiverDetails(senderId: String, receiverId: String): (User, User) = {
    val rows = cassandraMessageConn.execute(getSpokerDetails(senderId, receiverId)).all
    val (user1, user2) = (
      User(rows.get(0).getString("spokerid"), rows.get(0).getString("nickname"),
        rows.get(0).getString("gender"), rows.get(0).getString("picture")),
        User(rows.get(1).getString("spokerid"), rows.get(1).getString("nickname"),
          rows.get(1).getString("gender"), rows.get(1).getString("picture"))
    )
    val (sender, receiver) = if (user1.id.equals(senderId)) {
      (user1, user2)
    } else {
      (user2, user1)
    }
    (sender, receiver)
  }

  private def getPaginationNumber(pos: Int, listSize: Int): (String, String) = {
    val limit = searchLimit
    if (listSize > limit) {
      (pos.toString, (pos + 1).toString)
    } else if (pos - 1 < 1) {
      ((pos).toString, "")
    } else {
      ((pos - 1).toString, "")
    }
  }

  /**
   * This function is used to get talks for an user.
   *
   * @param pos
   * @param userId
   * @return
   */
  def getTalkLists(pos: String, userId: String): (Option[TalksResponse], Option[Error]) = {
    try {
      val validPos = if ((pos.toInt - 1) < 1) 1 else pos.toInt
      val start = (validPos - 1) * searchLimit
      val limit = searchLimit + 1
      val docResponse: List[SolrDocument] = DseGraphFactory.dseSolrConn(solrMessagingKeyspace, talksDetails)
        .query(fetchTalks(userId, start, limit)).getResults.asScala.toList
      val (previous, next) = getPaginationNumber(validPos, docResponse.size)
      val talkList = if (docResponse.size > searchLimit) {
        docResponse.dropRight(1)
      } else {
        docResponse
      }
      val talkDetails = talkList.map { talk =>
        val userRow = cassandraMessageConn.execute(getSpokerDetails(talk.getFieldValues(s"user2id").toArray().mkString)).one()
        val user = if (userRow == null) {
          try {
            val (userId, nickName, gender, picture) = insertUserDetailsInSpokerDetailsTable(talk.getFieldValues(s"user2id").toArray().mkString)
            User(userId, nickName, gender, picture)
          } catch {
            case ex: Exception =>
              LoggerUtil.logger.info("Error In Inserting Spoker Details for Id : " + talk.getFieldValues(s"user2id").toArray().mkString)
              User(talk.getFieldValues(s"user2id").toArray().mkString, "", "", "")
          }
        } else {
          User(userRow.getString("spokerid"), userRow.getString("nickname"), userRow.getString("gender"), userRow.getString("picture"))
        }

        Talks(user, LastMessage(
          talk.getFieldValues(s"senderid").toArray().mkString,
          millisToDate(talk.getFieldValues(s"lastmsgts").toArray().mkString.toLong),
          talk.getFieldValues(s"lastmsg").toArray().mkString
        ))
      }
      (Some(TalksResponse(previous, next, talkDetails)), None)
    } catch {
      case ex: NullPointerException => (None, Some(Error(MSG_109, RETRY_AFTER_SOMETIME)))
      case ex: Exception => {
        info(s"Exception while getting talks details: " + ex.getMessage())
        (None, Some(Error(MSG_101, UNABLE_LOAD_TALK_LIST)))
      }
    }
  }

  private def insertUserDetailsInSpokerDetailsTable(userId: String): (String, String, String, String) = {
    val userVertex = DseGraphFactory.dseConn.executeGraph(getUser(userId)).one().asVertex()
    val logJson = SpokerDetails(userId, userVertex.getProperty("nickname").getValue.asString(), userVertex.getProperty("gender").getValue.asString(), userVertex.getProperty("picture").getValue.asString())
    insertMesssagingDetails(write(logJson), spokerDetails)
    (userId, userVertex.getProperty("nickname").getValue.asString(), userVertex.getProperty("gender").getValue.asString(), userVertex.getProperty("picture").getValue.asString())
  }

  /**
   * Function to get user talk with other user
   *
   * @param messageId
   * @param user1Id
   * @param user2Id
   * @param order
   * @return
   */
  def getUserTalk(messageId: Option[String], user1Id: String, user2Id: String, order: String): (Option[TalkResponse], Option[Error]) = {
    try {
      val userTalkList = cassandraMessageConn.execute(fetchMessagesForUser(messageId, user1Id, user2Id, order)).all().asScala.toList
      val messageDetails = userTalkList.map { msg =>
        UserMessageDetail(msg.getUUID("messageid").toString, msg.getString("message"),
          millisToDate(msg.getLong("launchedtime")), msg.getString("senderid"))
      }

      val (sender: User, receiver: User) = getSenderReceiverDetails(user1Id, user2Id)
      (Some(TalkResponse(sender, receiver, messageDetails)), None)
    } catch {
      case ex: Exception =>
        info(s"Exception while getting Messages for user id: " + user1Id + " and " + user2Id + "::" + ex.getMessage())
        (None, Some(Error(MSG_102, UNABLE_LOAD_TALK)))
    }
  }

  /**
   * Function to remove message
   *
   * @param user1Id
   * @param user2Id
   * @param messageId
   * @return
   */
  def removeMessageById(user1Id: String, user2Id: String, messageId: String): (Option[String], Option[Error]) = {
    try {
      val isExist = cassandraMessageConn.execute(isMessageIdExistQuery(user1Id, user2Id, messageId)).all().asScala.toList
      if (isExist.isEmpty) {
        (None, Some(Error(MSG_003, s"Message $messageId not found.")))
      } else {
        removeMessageFromMessageTable(user1Id, user2Id, messageId)
      }
    } catch {
      case ex: Exception =>
        info(s"Exception while removing message for message id" + messageId + "::: " + ex.getMessage())
        (None, Some(Error(MSG_105, UNABLE_DELETE_MESSAGE)))
    }
  }

  private def removeMessageFromMessageTable(user1Id: String, user2Id: String, messageId: String) = {
    cassandraMessageConn.execute(removeMessage(user1Id, user2Id, messageId))
    val nextAvailableLatestMsg = cassandraMessageConn.execute(fetchAvailableLatestMsg(user1Id, user2Id)).all()
    if (!nextAvailableLatestMsg.isEmpty) {
      val talksDeleted: ResultSet = cassandraMessageConn.execute(deleteQuery(user1Id, user2Id, talksDetails))
      val user2Nickname = try {
        cassandraMessageConn.execute(getSpokerDetails(user2Id)).one().getString("nickname")
      } catch {
        case ex: Exception => ""
      }
      val senderTalkDetail = TalkDetails(user1Id, user2Id, nextAvailableLatestMsg.get(0).getLong("launchedtime"), getValidContent(nextAvailableLatestMsg.get(0).getString("message")),
        nextAvailableLatestMsg.get(0).getString("senderid"), user2Nickname)

      val talkRes: ResultSet = cassandraMessageConn.execute(messsagingQuery(write(senderTalkDetail), talksDetails))
    } else {
      cassandraMessageConn.execute(deleteQuery(user1Id, user2Id, talksDetails))
    }
    (Some("Message deleted successfully"), None)
  }

  /**
   * Function to delete full user talk
   *
   * @param user1Id
   * @param user2Id
   * @return
   */
  def removeTalkDetails(user1Id: String, user2Id: String): (Option[String], Option[Error]) = {
    try {
      cassandraMessageConn.execute(deleteUserTalk(user1Id, user2Id))
      (Some("Talk deleted successfully"), None)
    } catch {
      case ex: Exception =>
        info(s"Exception while removing talk of user" + user1Id + " AND " + user2Id + "::: " + ex.getMessage())
        (None, Some(Error(MSG_104, UNABLE_DELETE_TALK)))
    }
  }

  /**
   * This Function is used to search full text in messgaes .
   *
   * @param userId
   * @param message
   */
  def fullTextMessageSearch(userId: String, message: String): (Option[List[SearchMessageResponse]], Option[Error]) = {
    try {
      val rows = messagingFulltextSearchLimit
      val docResponse: List[SolrDocument] = DseGraphFactory.dseSolrConn(solrMessagingKeyspace, messagingDetails)
        .query(getSearchQuery(userId, message, rows)).getResults.asScala.toList
      val searchMessagesDetails: List[SearchMessageResponse] = docResponse.map { res =>
        val userRow = cassandraMessageConn.execute(getSpokerDetails(res.getFieldValues(s"user2id").toArray().mkString)).one()
        SearchMessageResponse(res.getFieldValues(s"messageid").toArray().mkString, res.getFieldValues(s"message").toArray().mkString,
          millisToDate(res.getFieldValues(s"launchedtime").toArray().mkString.toLong), userRow.getString("spokerid"), userRow.getString("nickname"),
          userRow.getString("gender"), userRow.getString("picture"), res.getFieldValues(s"senderid").toArray().mkString)
      }
      (Some(searchMessagesDetails), None)
    } catch {
      case ex: Exception =>
        info(ex.getMessage())
        (None, Some(Error(MSG_106, UNABLE_SEARCHING_MESSAGES)))
    }
  }

  /**
   * Function to search talkers
   *
   * @param username
   * @return
   */
  def searchTalker(username: String): (Option[List[User]], Option[Error]) = {
    try {
      val rows = messagingTalkerSearchLimit
      val docResponse = DseGraphFactory.dseSolrConn(solrMessagingKeyspace, talksDetails)
        .query(getTalkerList(username, rows)).getResults.asScala.toSet
      val talkersDetails = docResponse.map { res =>
        val userRow = cassandraMessageConn.execute(getSpokerDetailsWithUserName(res.getFieldValues(s"user2nickname").toArray().mkString)).one()
        User(userRow.getString("spokerid"), userRow.getString("nickname"), userRow.getString("gender"), userRow.getString("picture"))
      }
      (Some(talkersDetails.toList), None)
    } catch {
      case ex: Exception =>
        info(ex.getMessage())
        (None, Some(Error(MSG_106, UNABLE_SEARCHING_TALKERS)))
    }
  }

  def readMessageUpdate(userId: String, targetUserId: String, messageId: String): (Option[String], Option[Error]) = {
    try {
      val isExists = cassandraMessageConn.execute(isTargetIdExistQuery(userId, targetUserId)).all().asScala.toList
      if (isExists.nonEmpty) {
        val isMessageIdExist = cassandraMessageConn.execute(isMessageIdExistQuery(userId, targetUserId, messageId)).all().asScala.toList
        if (isMessageIdExist.isEmpty) {
          (None, Some(Error(MSG_002, s"Talk $targetUserId not found.")))
        } else {
          cassandraMessageConn.execute(updateReadMessageInMessageTable(userId, targetUserId, messageId, RandomUtil.timeStamp))
          (Some("Saved Successfully"), None)
        }
      } else {
        (None, Some(Error(MSG_002, s"Talk $targetUserId not found.")))
      }
    } catch {
      case ex: Exception =>
        info(s"Exception while removing message for message id" + messageId + "::: " + ex.getMessage())
        (None, Some(Error(MSG_107, s"Unable to flag message $messageId as read (generic error).")))
    }
  }

}

object MessagingApi extends MessagingApi
