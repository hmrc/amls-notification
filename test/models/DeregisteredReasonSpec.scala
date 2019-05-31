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

import models.DeregisteredReason._
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._

class DeregisteredReasonSpec extends PlaySpec {

  "DeregisteredReason model" must {
    "return reason for the string" in {
      DeregisteredReason.jsonReads.reads(Json.obj("status_reason" ->"01")) must be(JsSuccess(CeasedTrading))
      DeregisteredReason.jsonReads.reads(Json.obj("status_reason" ->"02")) must be(JsSuccess(HVDNoCashPayment))
      DeregisteredReason.jsonReads.reads(Json.obj("status_reason" ->"03")) must be(JsSuccess(OutOfScope))
      DeregisteredReason.jsonReads.reads(Json.obj("status_reason" ->"04")) must be(JsSuccess(NotTrading))
      DeregisteredReason.jsonReads.reads(Json.obj("status_reason" ->"05")) must be(JsSuccess(UnderAnotherSupervisor))
      DeregisteredReason.jsonReads.reads(Json.obj("status_reason" ->"06")) must be(JsSuccess(ChangeOfLegalEntity))
      DeregisteredReason.jsonReads.reads(Json.obj("status_reason" ->"99")) must be(JsSuccess(Other))
    }

    "fail validation on invalid data" in {
      DeregisteredReason.jsonReads.reads(Json.obj("status_reason" ->"100")) must be(JsError(List((JsPath \ "status_reason",
        List(JsonValidationError(List("error.invalid")))))))
    }

    "write data successfully" in {
      DeregisteredReason.jsonWrites.writes(CeasedTrading) must be(JsString("01"))
      DeregisteredReason.jsonWrites.writes(HVDNoCashPayment) must be(JsString("02"))
      DeregisteredReason.jsonWrites.writes(OutOfScope) must be(JsString("03"))
      DeregisteredReason.jsonWrites.writes(NotTrading) must be(JsString("04"))
      DeregisteredReason.jsonWrites.writes(UnderAnotherSupervisor) must be(JsString("05"))
      DeregisteredReason.jsonWrites.writes(ChangeOfLegalEntity) must be(JsString("06"))
      DeregisteredReason.jsonWrites.writes(Other) must be(JsString("99"))
    }
  }
}
