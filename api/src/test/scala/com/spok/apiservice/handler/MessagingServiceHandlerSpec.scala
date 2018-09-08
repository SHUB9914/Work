package com.spok.apiservice.handler

import org.scalatest.{ Matchers, WordSpec }

class MessagingServiceHandlerSpec extends WordSpec with Matchers with MessagingServiceHandler {

  "MessagingServiceHandler" should {

    "be able to check response and take action" in {

      val data = """{"resource": "talk","status": "success","errors": [],"data": {"sender": {"id": "a26c57d5-a9d9-4b23-b47d-866fa1071c7c","nickName": "user1","gender": "male", "picture": "1.jpg"},"receiver": { "id": "fc23255b-1e92-44f2-beeb-52cd3dab7e7a","nickName": "user2", "gender": "male","picture": "2.jpg" },"message": { "id": "313ddae9-79c2-4b83-bafb-3aee9c35b37d","text": "first message"}}}"""
      val result = checkMessagingResponseAndTakeAction(data, "a26c57d5-a9d9-4b23-b47d-866fa1071c7c")
      assert(result)

    }

    "be able to check response and take action when response contains user typing " in {

      val data = """{"resource": "typing","status": "success","errors": [],"data": {"userId": "a26c57d5-a9d9-4b23-b47d-866fa1071c7c","targetUserId": "a26c57d5-a9d9-4b23-b47d-866fa1071c7c"}}"""
      val result = checkMessagingResponseAndTakeAction(data, "a26c57d5-a9d9-4b23-b47d-866fa1071c7c")
      assert(result)

    }

  }

}
