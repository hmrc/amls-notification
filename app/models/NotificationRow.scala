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

import org.joda.time.DateTime
import play.api.libs.json._
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

case class NotificationRow (
                             status: Option[Status],
                             contactType: Option[ContactType],
                             contactNumber: Option[String],
                             variation: Boolean,
                             receivedAt: DateTime,
                             _id: IDType
                           )

object NotificationRow {

  implicit val dateFormat = ReactiveMongoFormats.dateTimeFormats
  implicit val format = Json.format[NotificationRow]
}

case class IDType(id: String)

object IDType {
  implicit val bsonRead: Reads[IDType] =
    (__ \ "$oid").read[String].map { dateTime =>
      new IDType(dateTime)
    }


  implicit val bsonReadWrite: Writes[IDType] = new Writes[IDType] {
    def writes(dateTime: IDType): JsValue = Json.obj(
      "$oid" -> dateTime.id
    )
  }
}