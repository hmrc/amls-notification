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

import models._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import java.time.Instant

class NotificationDetailsSpec extends AnyWordSpec with Matchers {

  "NotificationDetails serialisation" must {

    val dateTime = Instant.ofEpochMilli(1479730062573L)

    "Serialise the object correctly" in {
      NotificationDetails.writes.writes(
        NotificationDetails(
          Some(ContactType.NoLongerMindedToRevoke),
          Some(Status(StatusType.Approved, Some(RejectedReason.FailedToRespond))),
          Some("THIS IS THE TEST TEXT"),
          false,
          dateTime
        )
      ) must be(
        Json.obj(
          "contactType" -> "NMRV",
          "status"      -> Json.obj("status_type" -> "04", "status_reason" -> "02"),
          "messageText" -> "THIS IS THE TEST TEXT",
          "variation"   -> false,
          "receivedAt"  -> Json.parse("""{"$date":{"$numberLong":"1479730062573"}}""")
        )
      )
    }

    "serialise the object correctly when data is missing" in {
      NotificationDetails.writes.writes(
        NotificationDetails(None, None, Some("THIS IS THE TEST TEXT"), false, dateTime)
      ) must be(
        Json.obj(
          "messageText" -> "THIS IS THE TEST TEXT",
          "variation"   -> false,
          "receivedAt"  -> Json.parse("""{"$date":{"$numberLong":"1479730062573"}}""")
        )
      )
    }
  }
}
