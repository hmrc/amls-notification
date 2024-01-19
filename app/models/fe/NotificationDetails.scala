/*
 * Copyright 2024 HM Revenue & Customs
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

package models.fe

import models.{ContactType, Status}
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json._
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats

case class NotificationDetails(contactType : Option[ContactType],
                               status : Option[Status],
                               messageText : Option[String],
                               variation : Boolean,
                               receivedAt: DateTime
                              )

object NotificationDetails {
 val reads: Reads[NotificationDetails ] =
    (
      (JsPath \ "contactType").readNullable[ContactType] and
      (JsPath \ "status").readNullable[Status] and
      (JsPath \ "messageText").readNullable[String] and
      (JsPath \ "variation").read[Boolean] and
      (JsPath \ "receivedAt").read[DateTime]((MongoJodaFormats.dateTimeFormat))
    )(NotificationDetails.apply _)

   val writes : OWrites[NotificationDetails ] =
    (
      (JsPath \ "contactType").writeNullable[ContactType] and
        (JsPath \ "status").writeNullable[Status] and
        (JsPath \ "messageText").writeNullable[String] and
        (JsPath \ "variation").write[Boolean] and
        (JsPath \ "receivedAt").write[DateTime]((MongoJodaFormats.dateTimeFormat))
      )(unlift(NotificationDetails.unapply))

  implicit val format: OFormat[NotificationDetails] = OFormat(reads, writes)
}
