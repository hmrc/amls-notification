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

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.JsSuccess

import java.time.Instant
import java.time.temporal.ChronoUnit

class NotificationRecordSpec extends PlaySpec {

  "NotificationRecord" must {
    "read and write json successfully" in {

      val model = NotificationRecord(
        "amlsNumber",
        "safeId",
        "name",
        "hh@test.com",
        Some(
          Status(StatusType.Revoked, Some(RevokedReason.RevokedCeasedTrading))
        ),
        Some(ContactType.MindedToRevoke),
        None,
        false,
        Instant.now().truncatedTo(ChronoUnit.MILLIS),
        false,
        Some("1")
      )

      NotificationRecord.format.reads(NotificationRecord.format.writes(model)) must be(JsSuccess(model))
    }

  }

}
