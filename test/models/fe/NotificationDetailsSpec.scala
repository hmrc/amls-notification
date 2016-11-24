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

package models.fe

import models._
import org.scalatest.WordSpec
import org.scalatest.MustMatchers
import play.api.libs.json.Json

class NotificationDetailsSpec extends WordSpec with MustMatchers {

  "NotificationDetails serialisation" must {
    "Serialise the object correctly" in {
      val result = NotificationDetails.writes.writes(
        NotificationDetails(Some(ContactType.NoLongerMindedToRevoke), Some(StatusType.Approved), Some(RejectedReason.FailedToRespond),  Some("THIS IS THE TEST TEXT"))
      )
      result must be (Json.obj(
        "contactType" -> "NMRV",
        "status" -> "04",
        "statusReason" -> "02",
        "messageText" -> "THIS IS THE TEST TEXT"
      ))
    }

    "serialise the object correctly when data is missing" in {
      val result = NotificationDetails.writes.writes(
        NotificationDetails(None, None, None,  Some("THIS IS THE TEST TEXT"))
      )
      result must be (Json.obj(
        "messageText" -> "THIS IS THE TEST TEXT"
      ))
    }
  }
}
