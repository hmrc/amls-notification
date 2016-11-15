/*
 * Copyright 2016 HM Revenue & Customs
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

import play.api.libs.json.{Writes, Reads}

case class NotificationPushRequest (name: String,
                                   email: String,
                                   status: Option[Status],
                                   contactType: Option[String],
                                   contactNumber: Option[String],
                                   variation:Boolean
                                  )

object NotificationPushRequest {

  implicit val jsonReads: Reads[NotificationPushRequest] = {

    import play.api.libs.functional.syntax._
    import play.api.libs.json.Reads._
    import play.api.libs.json._

      ((__ \ "name").read[String] and
        (__ \ "email").read[String] and
        (__ \ "status").readNullable[Status] and
        (__ \ "contact_type").readNullable[String] and
        (__ \ "contact_number").readNullable[String] and
        (__ \ "variation").read[Boolean]
        )(NotificationPushRequest.apply _)
  }

  implicit val jsonWrites: Writes[NotificationPushRequest] = {

    import play.api.libs.functional.syntax._
    import play.api.libs.json.Writes._
    import play.api.libs.json._

      (
        (__ \ "name").write[String] and
          (__ \ "email").write[String] and
          (__ \ "status").writeNullable[Status] and
          (__ \ "contact_type").writeNullable[String] and
          (__ \ "contact_number").writeNullable[String] and
          (__ \ "variation").write[Boolean]
        ) (unlift(NotificationPushRequest.unapply))
    }
}
