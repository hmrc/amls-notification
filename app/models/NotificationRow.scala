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

package models


import play.api.libs.functional.syntax._
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json._
import org.joda.time.DateTime
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats



case class NotificationRow (
                             status: Option[Status],
                             contactType: Option[ContactType],
                             contactNumber: Option[String],
                             variation: Boolean,
                             receivedAt: DateTime,
                             isRead: Boolean,
                             amlsRegistrationNumber: String,
                             templatePackageVersion: Option[String],
                             _id: IDType
                           )

object NotificationRow {
  val reads: Reads[NotificationRow] =
    (
      (JsPath \ "status").readNullable[Status] and
        (JsPath \ "contactType").readNullable[ContactType] and
        (JsPath \ "contactNumber").readNullable[String] and
        (JsPath \ "variation").read[Boolean] and
        (JsPath \ "receivedAt").read[DateTime](MongoJodaFormats.dateTimeReads)and
        (JsPath \ "isRead").read[Boolean] and
        (JsPath \ "amlsRegistrationNumber").read[String] and
        (JsPath \ "templatePackageVersion").readNullable[String] and
        (JsPath \ "_id").read[IDType]
      )(NotificationRow.apply _)

  val writes : OWrites[NotificationRow] =
    (
      (JsPath \ "status").writeNullable[Status] and
        (JsPath \ "contactType").writeNullable[ContactType] and
        (JsPath \ "contactNumber").writeNullable[String] and
        (JsPath \ "variation").write[Boolean] and
        (JsPath \ "receivedAt").write[DateTime](MongoJodaFormats.dateTimeWrites) and
        (JsPath \ "isRead").write[Boolean] and
        (JsPath \ "amlsRegistrationNumber").write[String] and
        (JsPath \ "templatePackageVersion").writeNullable[String] and
        (JsPath \ "_id").write[IDType]
      )(unlift(NotificationRow.unapply))

  implicit val format: OFormat[NotificationRow] = OFormat(reads, writes)
}

case class IDType(id: String)

object IDType {
  implicit val bsonRead: Reads[IDType] =
    (__ \ "$oid").read[String].map { bsonId =>
      new IDType(bsonId)
    }


  implicit val bsonReadWrite: Writes[IDType] = new Writes[IDType] {
    def writes(bsonId: IDType): JsValue = Json.obj(
      "$oid" -> bsonId.id
    )
  }
}