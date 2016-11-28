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

import org.joda.time.{DateTimeZone, DateTime}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.JsSuccess

class NotificationRecordSpec extends PlaySpec {

  "NotificationRecord" must {
    "read and write json successfully"  in {

      val model = NotificationRecord("amlsNumber", "name", "hh@test.com",
        Some(Status(Some(StatusType.Revoked),
          Some(RevokedReason.RevokedCeasedTrading))),
        Some(ContactType.MindedToRevoke), None, false, DateTime.now(DateTimeZone.UTC), false)

      NotificationRecord.format.reads(NotificationRecord.format.writes(model)) must be(JsSuccess(model))
    }

  }

}
