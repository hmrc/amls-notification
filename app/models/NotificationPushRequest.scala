package models

import play.api.libs.json.Json

case class NotificationPushRequest (name: String,
                                   email: String,
                                   status: Option[String],
                                   statusReason: Option[StatusReason],
                                   contactType: Option[String],
                                   contactNumber: Option[String],
                                   variation:Boolean
                                  )

object NotificationPushRequest {

  implicit val format = Json.format[NotificationPushRequest]

}



