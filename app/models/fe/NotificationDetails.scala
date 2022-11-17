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

package models.fe

import models.{ContactType, Status}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.JsPath.\
import play.api.libs.json.{Format, JsValue, Json, Reads, Writes}
import play.api.libs.json._
import uk.gov.hmrc.mongo.play.json.formats.{MongoJavatimeFormats, MongoJodaFormats}
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats.dateTimeWrites

import java.time.LocalDateTime

case class NotificationDetails(contactType : Option[ContactType],
                               status : Option[Status],
                               messageText : Option[String],
                               variation : Boolean,
                               receivedAt: LocalDateTime
                              )

object NotificationDetails {

//  final val dateTimeReads: Reads[DateTime] =
//    Reads.at[String](__ \ "$date" )
//      .map(dateTime => new DateTime(dateTime.toLong, DateTimeZone.UTC))


  implicit val dateFormat: Format[LocalDateTime] = MongoJavatimeFormats.localDateTimeFormat
  //implicit val dateFormat: Format[DateTime] =  MongoJodaFormats.dateTimeFormat

  implicit val writes: Writes[NotificationDetails] = Json.writes[NotificationDetails]
}
