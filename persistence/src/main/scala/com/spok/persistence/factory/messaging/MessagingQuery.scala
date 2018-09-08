package com.spok.persistence.factory.messaging

import com.spok.model.Messaging.{ TalkDetails, UserMessage }
import com.spok.persistence.dse.DseConnectionUri._
import com.spok.util.JsonHelper
import org.apache.solr.client.solrj.SolrQuery

trait MessagingQuery extends JsonHelper {

  def insertMessages(sender: UserMessage, receiver: UserMessage): String = {
    val queryBuilder = new StringBuilder("BEGIN BATCH ")
    queryBuilder.append(messsagingQuery(write(sender), messagingDetails))
    queryBuilder.append(messsagingQuery(write(receiver), messagingDetails))
    queryBuilder.append(" APPLY BATCH")
    queryBuilder.toString()
  }

  def insertTalks(senderTalkDetail: TalkDetails, receiverTalkDetail: TalkDetails): String = {
    val queryBuilder = new StringBuilder("BEGIN BATCH ")
    queryBuilder.append(messsagingQuery(write(senderTalkDetail), talksDetails))
    queryBuilder.append(messsagingQuery(write(receiverTalkDetail), talksDetails))
    queryBuilder.append(" APPLY BATCH")
    queryBuilder.toString()
  }

  def deleteFromTalks(senderId: String, receiverId: String): String = {
    s""" DELETE FROM $talksDetails where user1Id in ('$senderId' , '$receiverId') and user2Id in ('$senderId' , '$receiverId');"""
  }

  def messsagingQuery(json: String, tableName: String): String = {
    s"INSERT INTO $tableName JSON '$json'"
  }

  def getSpokerDetails(senderId: String, receiverId: String): String = {
    s"select * from $spokerDetails where spokerid in ('$senderId','$receiverId');"
  }

  def getSpokerDetails(userId: String): String = {
    s"select * from $spokerDetails where spokerid = '$userId';"
  }

  def fetchTalks(userId: String, start: Int, rows: Int): SolrQuery = {
    val solrQuery = new SolrQuery
    solrQuery.set("q", s"user1id:$userId")
    solrQuery.set("sort", "lastmsgts desc")
    solrQuery.set("start", s"$start")
    solrQuery.set("rows", s"$rows")
    solrQuery
  }

  def fetchMessagesForUser(messageId: Option[String], user1Id: String, user2Id: String, order: String): String = {
    messageId match {
      case Some(id) => {
        order match {
          case "desc" => s"""select * from $messagingDetails where user1Id ='$user1Id' and user2Id='$user2Id' and messageid < $id LIMIT $messagingSearchLimit"""
          case "asc" => s"""select * from $messagingDetails where user1Id ='$user1Id' and user2Id='$user2Id' and messageid > $id order by messageid ASC LIMIT $messagingSearchLimit"""
        }
      }
      case None => s""" select * from $messagingDetails where user1Id ='$user1Id' and user2Id='$user2Id' order by messageid $order LIMIT $messagingSearchLimit"""
    }
  }

  def removeMessage(user1Id: String, user2Id: String, messageId: String): String = {
    s"delete from $messagingDetails where user1Id ='$user1Id' AND user2Id='$user2Id' AND messageid = $messageId;"
  }

  def fetchAvailableLatestMsg(user1Id: String, user2Id: String) = {
    s"""select * from $messagingDetails where user1id='$user1Id' and user2id='$user2Id' LIMIT 1"""
  }

  def deleteUserTalk(user1Id: String, user2Id: String): String = {
    val queryBuilder = new StringBuilder("BEGIN BATCH ")
    queryBuilder.append(deleteQuery(user1Id, user2Id, messagingDetails))
    queryBuilder.append(deleteQuery(user1Id, user2Id, talksDetails))
    queryBuilder.append(" APPLY BATCH")
    queryBuilder.toString()
  }

  def deleteQuery(user1Id: String, user2Id: String, tablename: String): String = {
    s"delete from $tablename where user1id ='$user1Id' AND user2id='$user2Id';"
  }

  def isMessageIdExistQuery(user1Id: String, user2Id: String, messageId: String): String = {
    s"select messageid from $messagingDetails where user1Id='$user1Id' and user2Id='$user2Id' and messageid=$messageId LIMIT 1;"
  }

  def getSearchQuery(userid: String, message: String, rows: Int): SolrQuery = {
    val solrQuery = new SolrQuery
    solrQuery.set("q", s"senderid:$userid OR user1id:$userid AND message:*$message*")
    solrQuery.set("sort", "launchedtime desc")
    solrQuery.set("df", s"message")
    solrQuery.set("rows", s"$rows")
    solrQuery
  }

  def getTalkerList(username: String, rows: Int): SolrQuery = {
    val solrQuery = new SolrQuery
    solrQuery.set("q", s"user2nickname:$username*")
    solrQuery.set("sort", "lastmsgts desc")
    solrQuery.set("fl", "user2nickname")
    solrQuery.set("df", "user2nickname")
    solrQuery.set("rows", s"$rows")
    solrQuery
  }

  def getSpokerDetailsWithUserName(userName: String): String = {
    s"select * from $spokerDetails where nickname = '$userName' ALLOW FILTERING;"
  }

  def updateReadMessageInMessageTable(userId: String, targetUserId: String, messageId: String, messageReadTime: Long): String = {
    s" update $messagingDetails set read = $messageReadTime where user1id ='$userId' and user2id ='$targetUserId' and messageid =$messageId ;"
  }

  def isTargetIdExistQuery(user1Id: String, user2Id: String): String = {
    s"select user2id from $messagingDetails where user1Id='$user1Id' and user2Id='$user2Id' limit 1";
  }

}
