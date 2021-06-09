/*
 * Copyright 2021 HM Revenue & Customs
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

import org.joda.time.{DateTime, DateTimeZone}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{Json, JsSuccess}

class NotificationRowSpec extends PlaySpec {

  "NotificationRow" must {
    "read and write json successfully" in {
      val amlsRegistrationNumber = "XAML00000567890"

      val model = NotificationRow(
        Some(Status(StatusType.Revoked, Some(RevokedReason.RevokedCeasedTrading))),
        Some(ContactType.MindedToRevoke),
        None,
        false,
        new DateTime(1479730062573L, DateTimeZone.UTC),
        false,
        amlsRegistrationNumber,
        Some("1"),
        new IDType("5832e38e01000001005ca3ff"))

      val json = Json.parse(
        """
          | {
          |   "status": {
          |     "status_type":"08",
          |     "status_reason":"02"
          |   },
          |   "contactType":"MTRV",
          |   "variation":false,
          |   "receivedAt":{
          |     "$date":1479730062573
          |   },
          |   "isRead":false,
          |   "amlsRegistrationNumber":"XAML00000567890",
          |   "templatePackageVersion":"1",
          |   "_id":{"$oid":"5832e38e01000001005ca3ff"}
          | }
          |
        """.stripMargin)

      NotificationRow.format.reads(json) must be(JsSuccess(model))
    }
  }
}
