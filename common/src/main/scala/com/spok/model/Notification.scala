package com.spok.model

import org.joda.time.DateTime

case class Notification(
  notificationId: String,
  notificationType: String,
  relatedTo: String,
  time: DateTime,
  emitter: Emitter
)

case class Emitter(
  emitterId: String,
  nickname: String,
  gender: String,
  picture: String
)

// Websocket Notification
case class UserNotification(
  userId: String,
  notification: Notification
)

// Get Notifications
case class NotifyUser(
  previous: String,
  next: String,
  notification: Notification
)

// Store Notification
case class NotificationDetail(
  userIds: List[String],
  notificationId: String,
  notificationType: String,
  relatedTo: String,
  emitterId: String
)

/**
 *
 * @param id Notification's identifier.
 * @param notificationType Notification's type.
 * @param relatedTo Notification's object identifier related to (depends on type, often a spok instance).
 * @param timestamp Notification's timestamp.
 * @param emitter Notification's emitter details.
 */
case class Notifications(
  id: String,
  notificationType: String,
  relatedTo: String,
  timestamp: String,
  emitter: Emitter
)
/**
 *
 * @param previous previous page index
 * @param next next page index
 * @param notifications List of Notifications
 */
case class NotificationsResponse(
  previous: String,
  next: String,
  notifications: List[Notifications]
) extends SpokDataResponse
