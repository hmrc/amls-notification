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

import models.RejectedReason._
import org.scalatestplus.play.PlaySpec
import play.api.data.validation.ValidationError
import play.api.libs.json._

class RejectedReasonSpec extends PlaySpec {

  "RejectedReason model" must {
    "return reason for the string" in {
      RejectedReason.jsonReads.reads(Json.obj("status_reason" ->"01")) must be(JsSuccess(NonCompliant))
      RejectedReason.jsonReads.reads(Json.obj("status_reason" ->"02")) must be(JsSuccess(FailedToRespond))
      RejectedReason.jsonReads.reads(Json.obj("status_reason" ->"03")) must be(JsSuccess(FailedToPayCharges))
      RejectedReason.jsonReads.reads(Json.obj("status_reason" ->"04")) must be(JsSuccess(FitAndProperFailure))
      RejectedReason.jsonReads.reads(Json.obj("status_reason" ->"98")) must be(JsSuccess(OtherFailed))
      RejectedReason.jsonReads.reads(Json.obj("status_reason" ->"99")) must be(JsSuccess(OtherRefused))
    }

    "fail validation on invalid data" in {
      RejectedReason.jsonReads.reads(Json.obj("status_reason" ->"100")) must be(JsError(List((JsPath \ "status_reason",
        List(ValidationError(List("error.invalid")))))))
    }

    "write data successfully" in {
      RejectedReason.jsonWrites.writes(NonCompliant) must be(JsString("01"))
      RejectedReason.jsonWrites.writes(FailedToRespond) must be(JsString("02"))
      RejectedReason.jsonWrites.writes(FailedToPayCharges) must be(JsString("03"))
      RejectedReason.jsonWrites.writes(FitAndProperFailure) must be(JsString("04"))
      RejectedReason.jsonWrites.writes(OtherFailed) must be(JsString("98"))
      RejectedReason.jsonWrites.writes(OtherRefused) must be(JsString("99"))
    }
  }
}
