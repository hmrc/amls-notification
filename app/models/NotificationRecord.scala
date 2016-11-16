package models

import play.api.libs.json.Json

case class NotificationRecord (amlsRegistrationNumber: String,
                               name: String,
                               email: String,
                               status: Option[Status],
                               contactType: Option[ContactType],
                               contactNumber: Option[String],
                               variation:Boolean)

object NotificationRecord {
  implicit val format = Json.format[NotificationRecord]
}