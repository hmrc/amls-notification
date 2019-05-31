/*
 * Copyright 2019 HM Revenue & Customs
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

import models.StatusType._
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._

class StatusSpec extends PlaySpec {

  "Status model" must {
    "must serialise and de serialise data successfully" in {
      val data = Status(Approved,None)
      Status.jsonReads.reads(Status.jsonWrites.writes(data)) must be(JsSuccess(data))
    }

    "must serialise and de serialise data successfully for the status Rejected" in {
      val data = Status(Rejected,Some(RejectedReason.NonCompliant))
      Status.jsonReads.reads(Status.jsonWrites.writes(data)) must be(JsSuccess(data))
    }

    "must serialise and de serialise data successfully for the status Revoked" in {
      val data = Status(Revoked, Some(RevokedReason.RevokedFitAndProperFailure))
      Status.jsonReads.reads(Status.jsonWrites.writes(data)) must be(JsSuccess(data))
    }

    "must serialise and de serialise data successfully for the status DeRegistered" in {
      val data = Status(DeRegistered, Some(DeregisteredReason.HVDNoCashPayment))
      Status.jsonReads.reads(Status.jsonWrites.writes(data)) must be(JsSuccess(data))
    }


    "must serialise and de serialise data successfully for the status Expired" in {
      val data = Status(Expired, None)
      Status.jsonReads.reads(Status.jsonWrites.writes(data)) must be(JsSuccess(data))
    }

    "fail with error when status value is passed incorrectly" in {
      StatusType.jsonReads.reads(JsString("12")) must be(JsError(List((JsPath ,List(JsonValidationError(List("error.invalid")))))))

    }
  }
}
