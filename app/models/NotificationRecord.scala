/*
 * Copyright 2022 HM Revenue & Customs
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
import org.bson.types.ObjectId
import uk.gov.hmrc.mongo.play.json.formats.{MongoFormats, MongoJavatimeFormats, MongoJodaFormats}
import org.joda.time.DateTime

case class NotificationRecord (amlsRegistrationNumber: String,
                               safeId: String,
                               name: String,
                               email: String,
                               status: Option[Status],
                               contactType: Option[ContactType],
                               contactNumber: Option[String],
                               variation: Boolean,
                               receivedAt: DateTime,
                               isRead: Boolean,
                               templatePackageVersion: Option[String],
                                 _id:ObjectId = ObjectId.get()
                              )
object NotificationRecord {
 val reads: Reads[NotificationRecord] =
    (
      (JsPath \ "amlsRegistrationNumber").read[String] and
      (JsPath \ "safeId").read[String] and
        (JsPath \ "name").read[String] and
        (JsPath \ "email").read[String] and
        (JsPath \ "status").readNullable[Status] and
        (JsPath \ "contactType").readNullable[ContactType] and
        (JsPath \ "contactNumber").readNullable[String] and
        (JsPath \ "variation").read[Boolean] and
        (JsPath \ "receivedAt").read[DateTime](MongoJodaFormats.dateTimeFormat) and
        (JsPath \ "isRead").read[Boolean] and
        (JsPath \ "templatePackageVersion").readNullable[String] and
        (JsPath \ "_id").read[ObjectId](MongoFormats.objectIdFormat)
      )(NotificationRecord.apply _)

 val writes : OWrites[NotificationRecord] =
    (
      (JsPath \ "amlsRegistrationNumber").write[String] and
        (JsPath \ "safeId").write[String] and
        (JsPath \ "name").write[String] and
        (JsPath \ "email").write[String] and
        (JsPath \ "status").writeNullable[Status] and
        (JsPath \ "contactType").writeNullable[ContactType] and
        (JsPath \ "contactNumber").writeNullable[String] and
        (JsPath \ "variation").write[Boolean] and
        (JsPath \ "receivedAt").write[DateTime] (MongoJodaFormats.dateTimeFormat)  and
        (JsPath \ "isRead").write[Boolean] and
        (JsPath \ "templatePackageVersion").writeNullable[String] and
          (JsPath \ "_id").write[ObjectId] (MongoFormats.objectIdFormat)
      )(unlift(NotificationRecord.unapply))

  implicit val format: OFormat[NotificationRecord] = OFormat(reads, writes)
}
