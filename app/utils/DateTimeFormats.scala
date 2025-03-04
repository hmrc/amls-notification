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

package utils

import org.joda.time.{DateTime, DateTimeZone, LocalDate, LocalDateTime}
import play.api.libs.json._

trait DateTimeFormats {
  outer =>

  final val dateTimeReads: Reads[DateTime] =
    Reads
      .at[String](__ \ "$date" \ "$numberLong")
      .map(dateTime => new DateTime(dateTime.toLong, DateTimeZone.UTC))

  final val dateTimeWrites: Writes[DateTime] =
    Writes
      .at[String](__ \ "$date" \ "$numberLong")
      .contramap[DateTime](_.getMillis.toString)

  final val dateTimeFormat: Format[DateTime] =
    Format(dateTimeReads, dateTimeWrites)

  trait Implicits {
    implicit val jotDateTimeFormat: Format[DateTime] = outer.dateTimeFormat
  }

  object Implicits extends Implicits
}

object DateTimeFormats extends DateTimeFormats
