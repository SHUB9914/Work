package com.spok.persistence.factory

import com.spok.model.Account.User
import com.spok.model._
import com.spok.persistence.dse.DseGraphFactory
import org.scalatest.{ BeforeAndAfterAll, FlatSpec, Matchers }

class DSEUserNotificationFactoryApiSpec extends FlatSpec with Matchers
    with DSEUserNotificationFactoryApi with BeforeAndAfterAll {

  override def afterAll: Unit = {
    DseGraphFactory.dseConn.executeGraph("g.V().drop()")
  }

  behavior of "DSEUserNotificationFactoryApiSpec "

  it should "be able to add notification Details in graph for list of users" in {

    val id1 = getUUID()
    val id2 = getUUID()
    val id3 = getUUID()

    val userAttributes = User("user1", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(28.6363011, 77.5025632), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+919582311059", "+919582611051"), "+919983899777", id1, Some("user1.jpg"))

    val userAttributesTest1 = User("user2", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(28.6363011, 77.5025632), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+919582311059", "+919983899777"), "+919876543210", id2, Some("user2.jpg"))

    val userAttributesTest2 = User("user3", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(28.6363011, 77.5025632), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+919983899777", "+919582611051", "+91851003849"), "+919582311059", id3, Some("user3.jpg"))

    val notificationTest: NotificationDetail = NotificationDetail(List(id1, id3), "NotificationId123456", "REGISTERED", id2, id2)

    val persistenceObj = DSEGraphPersistenceFactoryApi
    persistenceObj.insertUser(userAttributes)
    persistenceObj.insertUser(userAttributesTest1)
    persistenceObj.insertUser(userAttributesTest2)
    val notificationV = storeUsersNotifications(notificationTest)
    assert(notificationV.get.getProperty("notificationId").getValue.toString.equals(""""NotificationId123456""""))
  }

  it should "be able to get the users nickname,gender and picture based on user id" in {

    val id = getUUID()
    val notificationId = getUUID()
    val alreadyRegisteredUserId = getUUID()

    val userAttributesTest4 = User("user111", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(28.6363011, 77.5025632), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+919582311059", "+919582611051"), "+919983899777", id, Some("user111.jpg"))

    val persistenceObj = DSEGraphPersistenceFactoryApi
    persistenceObj.insertUser(userAttributesTest4)
    val notificationDetail = NotificationDetail(List(alreadyRegisteredUserId), notificationId, "register",
      id, id)
    val result = getnotificationRespone(id, notificationDetail)
    assert(result contains "nickname")
    assert(result contains "gender")
    assert(result contains "picture")

  }

  it should "be able to remove a notification for a given user if the notification exists" in {

    val userId1 = getUUID()
    val userId2 = getUUID()
    val userId3 = getUUID()
    val notificationId = getUUID()

    val userAttributes = User("Cyril", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))), " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(28.6363011, 77.5025632), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+919582311059", "+919582611051"), "+919983899777", userId1, Some("Cyril.jpg"))

    val userAttributesTest1 = User("Kais", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India", Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(28.6363011, 77.5025632), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+919582311059", "+919983899777"), "+919876543210", userId2, Some("Kais.jpg"))

    val userAttributesTest2 = User("Victor", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(28.6363011, 77.5025632), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+919983899777", "+919582611051", "+91851003849"), "+919582311059", userId3, Some("Victor.jpg"))

    val notificationTest: NotificationDetail = NotificationDetail(List(userId1, userId3), notificationId, "REGISTERED", userId2, userId2)

    val persistenceObj = DSEGraphPersistenceFactoryApi
    persistenceObj.insertUser(userAttributes)
    persistenceObj.insertUser(userAttributesTest1)
    persistenceObj.insertUser(userAttributesTest2)
    storeUsersNotifications(notificationTest)

    val result = removeNotification(notificationId, userId1)

    assert(result._1 == true)
    assert(result._2 == "Removed")
  }

  it should "be able to send not found error if the notification is not found while removing notification" in {

    val userId1 = getUUID()
    val userId2 = getUUID()
    val userId3 = getUUID()
    val notificationId = getUUID()
    val wrongNotificationid = getUUID()

    val userAttributes = User("Cyril", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))), " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(28.6363011, 77.5025632), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+919582311059", "+919582611051"), "+919983899777", userId1, Some("Cyril.jpg"))

    val userAttributesTest1 = User("Kais", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India", Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(28.6363011, 77.5025632), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+919582311059", "+919983899777"), "+919876543210", userId2, Some("Kais.jpg"))

    val userAttributesTest2 = User("Victor", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(28.6363011, 77.5025632), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+919983899777", "+919582611051", "+91851003849"), "+919582311059", userId3, Some("Victor.jpg"))

    val notificationTest: NotificationDetail = NotificationDetail(List(userId1, userId3), notificationId, "REGISTERED", userId2, userId2)

    val persistenceObj = DSEGraphPersistenceFactoryApi
    persistenceObj.insertUser(userAttributes)
    persistenceObj.insertUser(userAttributesTest1)
    persistenceObj.insertUser(userAttributesTest2)
    storeUsersNotifications(notificationTest)

    val result = removeNotification(wrongNotificationid, userId1)

    assert(result._1 == false)
    assert(result._2 == s"Notification $wrongNotificationid not found")
  }

  it should "be able to get wall notifications" in {

    val userId1 = getUUID()
    val userId2 = getUUID()
    val userId3 = getUUID()
    val notificationId = getUUID()

    val userAttributes = User("Cyril", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))), " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(28.6363011, 77.5025632), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+919582311059", "+919582611051"), "+919983899777", userId1, Some("Cyril.jpg"))

    val userAttributesTest1 = User("Kais", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India", Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(28.6363011, 77.5025632), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+919582311059", "+919983899777"), "+919876543210", userId2, Some("Kais.jpg"))

    val userAttributesTest2 = User("Victor", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(28.6363011, 77.5025632), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+919983899777", "+919582611051", "+91851003849"), "+919582311059", userId3, Some("Victor.jpg"))

    val notificationTest: NotificationDetail = NotificationDetail(List(userId1, userId3), notificationId, "REGISTERED", userId2, userId2)

    val persistenceObj = DSEGraphPersistenceFactoryApi
    persistenceObj.insertUser(userAttributes)
    persistenceObj.insertUser(userAttributesTest1)
    persistenceObj.insertUser(userAttributesTest2)
    storeUsersNotifications(notificationTest)

    val result = getnotifications(userId1, "1").get

    assert(result.notifications.isEmpty == false)
  }

  it should "not be able to get wall notifications" in {

    val userId1 = getUUID()
    val userId2 = getUUID()
    val userId3 = getUUID()
    val notificationId = getUUID()
    val userId4 = getUUID()

    val userAttributes = User("Cyril", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))), " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(28.6363011, 77.5025632), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+919582311059", "+919582611051"), "+919983899777", userId1, Some("Cyril.jpg"))

    val userAttributesTest1 = User("Kais", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India", Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(28.6363011, 77.5025632), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+919582311059", "+919983899777"), "+919876543210", userId2, Some("Kais.jpg"))

    val userAttributesTest2 = User("Victor", date, Location(List(LocationDetails(
      List(AddressComponents("Noida", "Noida", List("locality", "political"))),
      " Noida, Uttar Pradesh 201301, India",
      Geometry(Bounds(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632)), InnerLocation(28.6363011, 77.5025632), "APPROXIMATE",
        ViewPort(NorthEast(28.6363011, 77.5025632), SouthWest(28.6363011, 77.5025632))), "ChIJezVzMaTlDDkRP8B8yDDO_zc", List("locality", "political")
    )), "OK"),
      "male", List("+919983899777", "+919582611051", "+91851003849"), "+919582311059", userId3, Some("Victor.jpg"))

    val notificationTest: NotificationDetail = NotificationDetail(List(userId1, userId3), notificationId, "REGISTERED", userId2, userId2)

    val persistenceObj = DSEGraphPersistenceFactoryApi
    persistenceObj.insertUser(userAttributes)
    persistenceObj.insertUser(userAttributesTest1)
    persistenceObj.insertUser(userAttributesTest2)
    storeUsersNotifications(notificationTest)

    val result = getnotifications(userId4, "1").get

    assert(result.notifications.isEmpty == true)
  }

}
