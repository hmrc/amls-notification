/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models

import play.api.libs.json._

case class NotificationPushRequest (name: String,
                                   email: String,
                                   status: Option[Status],
                                   contactType: Option[ContactType],
                                   contactNumber: Option[String],
                                   variation:Boolean
                                  )

object NotificationPushRequest {

  implicit val jsonReads: Reads[NotificationPushRequest] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Reads._
    import play.api.libs.json._
    val namePattern = "^[A-Za-z0-9 ]{1,140}$".r
    val maxEmailLength = 100

      ((__ \ "name").read[String] (pattern(namePattern)) and
        (__ \ "email").read[String] (minLength[String](1)keepAnd maxLength[String](maxEmailLength)) and
        (__ \ "status").readNullable[Status] and
        (__ \ "contact_type").readNullable[ContactType] and
        (__ \ "contact_number").readNullable[String] and
        (__ \ "variation").read[Boolean]
        )(NotificationPushRequest.apply _)
  }

  implicit val jsonWrites: OWrites[NotificationPushRequest] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Writes._
    import play.api.libs.json._

      (
        (__ \ "name").write[String] and
          (__ \ "email").write[String] and
          (__ \ "status").writeNullable[Status] and
          (__ \ "contact_type").writeNullable[ContactType] and
          (__ \ "contact_number").writeNullable[String] and
          (__ \ "variation").write[Boolean]
        ) (unlift(NotificationPushRequest.unapply))
    }

  implicit val formats: OFormat[NotificationPushRequest] =
  OFormat(jsonReads, jsonWrites)
}
