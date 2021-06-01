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

import org.scalatestplus.play.PlaySpec
import play.api.libs.json._

class ContactTypeSpec extends PlaySpec {

  "ContactType model" must {
    "read/write Json successfully" in {
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.RejectionReasons)) must be(JsSuccess(ContactType.RejectionReasons))
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.RevocationReasons)) must be(JsSuccess(ContactType.RevocationReasons))
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.MindedToReject)) must be(JsSuccess(ContactType.MindedToReject))
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.NoLongerMindedToReject)) must be(JsSuccess(ContactType.NoLongerMindedToReject))
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.MindedToRevoke)) must be(JsSuccess(ContactType.MindedToRevoke))
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.NoLongerMindedToRevoke)) must be(JsSuccess(ContactType.NoLongerMindedToRevoke))
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.Others)) must be(JsSuccess(ContactType.Others))
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.ApplicationApproval)) must be(JsSuccess(ContactType.ApplicationApproval))
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.RenewalApproval)) must be(JsSuccess(ContactType.RenewalApproval))
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.AutoExpiryOfRegistration)) must be(JsSuccess(ContactType.AutoExpiryOfRegistration))
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.RenewalReminder)) must be(JsSuccess(ContactType.RenewalReminder))
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.ReminderToPayForApplication)) must be(JsSuccess(ContactType.ReminderToPayForApplication))
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.ReminderToPayForRenewal)) must be(JsSuccess(ContactType.ReminderToPayForRenewal))
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.ReminderToPayForVariation)) must be(JsSuccess(ContactType.ReminderToPayForVariation))
      ContactType.jsonReads.reads(ContactType.jsonWrites.writes(ContactType.ReminderToPayForManualCharges)) must be(JsSuccess(ContactType.ReminderToPayForManualCharges))
    }


    "fail with error when status value is passed incorrectly" in {
      ContactType.jsonReads.reads(JsString("RPM1RPM1")) must be(JsError(List((JsPath  \"contact_type",List(JsonValidationError(List("error.invalid")))))))

    }
  }

}
