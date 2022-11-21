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
import play.api.libs.json._
import org.bson.types.ObjectId
import uk.gov.hmrc.mongo.play.json.formats.{MongoFormats, MongoJodaFormats}
import org.joda.time.{DateTime}




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

  implicit val objectIdFormat: Format[ObjectId] = MongoFormats.objectIdFormat
  implicit val dtf: Format[DateTime] = MongoJodaFormats.dateTimeFormat
  implicit val format: Format[NotificationRecord] = Json.format[NotificationRecord]
}