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

import models.RejectedReason.FailedToRespond
import models.StatusType.{Approved, DeRegistered, Rejected}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._

class NotificationPushRequestSpec extends PlaySpec {

  "NotificationPushRequest" must {

    "read json successfully" when {
      "status and status reason is Rejected" in {

        val json = Json.obj("safeId" -> "AA1234567891234",
          "name" -> "test",
          "email" -> "test@gg.com",
          "status" -> Json.obj("status_type" -> "06",
            "status_reason" -> "03"),
          "contact_type" -> "REJR",
          "contact_number" -> "112345678251212",
          "variation" -> false)

        NotificationPushRequest.jsonReads.reads(json) must be(JsSuccess(NotificationPushRequest(
          "AA1234567891234",
          "test",
          "test@gg.com",
          Some(Status(StatusType.Rejected, Some(RejectedReason.FailedToPayCharges))),
          Some(ContactType.RejectionReasons),
          Some("112345678251212"),
          variation = false)))
      }

      "fail when length of name exceed maxLength" in {

        val json = Json.obj("safeId" -> "AA1234567891234",
          "name" -> "test" * 140,
          "email" -> "test@gg.com",
          "status" -> Json.obj("status_type" -> "06",
            "status_reason" -> "03"),
          "contact_type" -> "REJR",
          "contact_number" -> "112345678251212",
          "variation" -> false)

        NotificationPushRequest.jsonReads.reads(json) must be(JsError(List((JsPath \ "name", List(JsonValidationError(List("error.pattern")))))))
      }

      "allow special characters in the name" in {
        val json = Json.obj("safeId" -> "AA1234567891234",
          "name" -> "test (£*$(03094",
          "email" -> "test@gg.com",
          "status" -> Json.obj("status_type" -> "06",
            "status_reason" -> "03"),
          "contact_type" -> "REJR",
          "contact_number" -> "112345678251212",
          "variation" -> false)

        NotificationPushRequest.jsonReads.reads(json).asEither match {
          case Right(request) => request.name mustBe "test (£*$(03094"
          case Left(errors) => fail(errors.toString())
        }
      }

      "fail when length of email exceed maxLength and status type is invalid" in {
        val maxEmail = 100

        val json = Json.obj("safeId" -> "AA1234567891234",
          "name" -> "test" * 140,
          "email" -> "test@gg.com" * 100,
          "status" -> Json.obj("status_type" -> "03",
            "status_reason" -> "03"),
          "contact_type" -> "REJR",
          "contact_number" -> "112345678251212",
          "variation" -> false)

        NotificationPushRequest.jsonReads.reads(json) must be(JsError(List((JsPath \ "email", List(JsonValidationError(List("error.maxLength"), maxEmail))),
          (JsPath \ "status" \ "status_type", List(JsonValidationError(List("error.invalid")))),
          (JsPath \ "name", List(JsonValidationError(List("error.pattern")))))))
      }

      "fail when length of safeId exceed maxLength" in {

        val json = Json.obj("safeId" -> "AA1234567891234" * 10,
          "name" -> "test",
          "email" -> "test@gg.com",
          "status" -> Json.obj("status_type" -> "06",
            "status_reason" -> "03"),
          "contact_type" -> "REJR",
          "contact_number" -> "112345678251212",
          "variation" -> false)

        NotificationPushRequest.jsonReads.reads(json) must be(JsError(List((JsPath \ "safeId", List(JsonValidationError(List("error.pattern")))))))
      }

      "fail when safeId has invalid character" in {

        val json = Json.obj("safeId" -> "A1234567*123456",
          "name" -> "test",
          "email" -> "test@gg.com",
          "status" -> Json.obj("status_type" -> "06",
            "status_reason" -> "03"),
          "contact_type" -> "REJR",
          "contact_number" -> "112345678251212",
          "variation" -> false)

        NotificationPushRequest.jsonReads.reads(json) must be(JsError(List((JsPath \ "safeId", List(JsonValidationError(List("error.pattern")))))))
      }

      "fail when status type is valid Rejected type and status reason is invalid rejected reason" in {

        val json = Json.obj(
          "safeId" -> "AA1234567891234",
          "name" -> "test",
          "email" -> "test@gg.com",
          "status" -> Json.obj("status_type" -> "06",
            "status_reason" -> "100"),
          "contact_type" -> "REJR",
          "contact_number" -> "112345678251212",
          "variation" -> false)

        NotificationPushRequest.jsonReads.reads(json) must be(JsError(List(
          (JsPath \ "status" \ "status_reason", List(JsonValidationError(List("error.invalid")))))))
      }

      "fail when status type is valid Deregistered type and status reason is invalid Deregistered reason" in {

        val json = Json.obj("safeId" -> "AA1234567891234",
          "name" -> "test",
          "email" -> "test@gg.com",
          "status" -> Json.obj("status_type" -> "08",
            "status_reason" -> "100"),
          "contact_type" -> "REJR",
          "contact_number" -> "112345678251212",
          "variation" -> false)

        NotificationPushRequest.jsonReads.reads(json) must be(JsError(List(
          (JsPath \ "status" \ "status_reason", List(JsonValidationError(List("error.invalid")))))))
      }

      "fail when status type is valid Revoked type and status reason is invalid Revoked reason" in {

        val json = Json.obj("safeId" -> "AA1234567891234",
          "name" -> "test",
          "email" -> "test@gg.com",
          "status" -> Json.obj("status_type" -> "10",
            "status_reason" -> "100"),
          "contact_type" -> "REJR",
          "contact_number" -> "112345678251212",
          "variation" -> false)

        NotificationPushRequest.jsonReads.reads(json) must be(JsError(List(
          (JsPath \ "status" \ "status_reason", List(JsonValidationError(List("error.invalid")))))))
      }

      "fail when status type is invalid and status reason is invalid" in {

        val json = Json.obj("safeId" -> "AA1234567891234",
          "name" -> "test",
          "email" -> "test@gg.com",
          "status" -> Json.obj("status_type" -> "100",
            "status_reason" -> "100"),
          "contact_type" -> "REJR",
          "contact_number" -> "112345678251212",
          "variation" -> false)

        NotificationPushRequest.jsonReads.reads(json) must be(JsError(List(
          (JsPath \ "status" \ "status_type", List(JsonValidationError(List("error.invalid")))))))
      }

      "status and status reason is Revoked" in {

        val json = Json.obj("safeId" -> "AA1234567891234",
          "name" -> "test",
          "email" -> "test@gg.com",
          "status" -> Json.obj("status_type" -> "08",
            "status_reason" -> "02"),
          "contact_type" -> "REJR",
          "contact_number" -> "112345678251212",
          "variation" -> false)

        NotificationPushRequest.jsonReads.reads(json) must be(JsSuccess(NotificationPushRequest("AA1234567891234", "test", "test@gg.com",
          Some(Status(StatusType.Revoked, Some(RevokedReason.RevokedCeasedTrading))), Some(ContactType.RejectionReasons), Some("112345678251212"), variation = false)))
      }

      "status and status reason is DeRegistered" in {

        val json = Json.obj("safeId" -> "AA1234567891234",
          "name" -> "test",
          "email" -> "test@gg.com",
          "status" -> Json.obj("status_type" -> "10",
            "status_reason" -> "01"),
          "contact_type" -> "REJR",
          "contact_number" -> "112345678251212",
          "variation" -> false)

        NotificationPushRequest.jsonReads.reads(json) must be(JsSuccess(NotificationPushRequest("AA1234567891234", "test", "test@gg.com",
          Some(Status(StatusType.DeRegistered,
            Some(DeregisteredReason.CeasedTrading))), Some(ContactType.RejectionReasons), Some("112345678251212"), variation = false)))
      }

      "status and status reason is Expired" in {

        val json = Json.obj("safeId" -> "AA1234567891234",
          "name" -> "test",
          "email" -> "test@gg.com",
          "status" -> Json.obj("status_type" -> "11",
            "status_reason" -> "03"),
          "contact_type" -> "REJR",
          "contact_number" -> "112345678251212",
          "variation" -> false)

        NotificationPushRequest.jsonReads.reads(json) must be(JsSuccess(NotificationPushRequest("AA1234567891234", "test", "test@gg.com",
          Some(Status(StatusType.Expired, None)), Some(ContactType.RejectionReasons), Some("112345678251212"), variation = false)))
      }

      "status and status reason is Approved" in {

        val json = Json.obj("safeId" -> "AA1234567891234",
          "name" -> "test",
          "email" -> "test@gg.com",
          "status" -> Json.obj("status_type" -> "04",
            "status_reason" -> "03"),
          "contact_type" -> "REJR",
          "contact_number" -> "112345678251212",
          "variation" -> false)

        NotificationPushRequest.jsonReads.reads(json) must be(JsSuccess(NotificationPushRequest("AA1234567891234", "test", "test@gg.com",
          Some(Status(StatusType.Approved, None)), Some(ContactType.RejectionReasons), Some("112345678251212"), variation = false)))
      }
    }

    "serialise successfully" when {
      "path 'status' is missing in json" in {
        val json = Json.obj("safeId" -> "AA1234567891234",
          "name" -> "test",
          "email" -> "test@gg.com",
          "status_reason" -> "03",
          "contact_type" -> "REJR",
          "contact_number" -> "112345678251212",
          "variation" -> false)

        NotificationPushRequest.jsonReads.reads(json) must be(JsSuccess(NotificationPushRequest("AA1234567891234", "test", "test@gg.com",
          None, Some(ContactType.RejectionReasons), Some("112345678251212"), variation = false)))
      }
    }

    "fail serialization" when {
      "status value passed incorrectly" in {
        val json = Json.obj("safeId" -> "AA1234567891234",
          "name" -> "test",
          "email" -> "test@gg.com",
          "status" -> Json.obj("status_type" -> "7774",
            "status_reason" -> "03"),
          "contact_type" -> "REJR",
          "contact_number" -> "112345678251212",
          "variation" -> false)

        NotificationPushRequest.jsonReads.reads(json) must be(JsError(List((JsPath \ "status" \ "status_type", List(JsonValidationError(List("error.invalid")))))))
      }
    }

    "write json successfully" when {
      "status and status reason is Rejected" in {

        val model = NotificationPushRequest("AA1234567891234", "test", "test@gg.com", Some(Status(StatusType.Rejected,
          Some(RejectedReason.FailedToPayCharges))), Some(ContactType.RejectionReasons), Some("112345678251212"), variation = false)

        val json = Json.obj("safeId" -> "AA1234567891234",
          "name" -> "test",
          "email" -> "test@gg.com",
          "status" -> Json.obj("status_type" -> "06",
            "status_reason" -> "03"),
          "contact_type" -> "REJR",
          "contact_number" -> "112345678251212",
          "variation" -> false)

        NotificationPushRequest.jsonWrites.writes(model) must be(json)

      }

      "status and status reason is Revoked" in {

        val model = NotificationPushRequest("AA1234567891234", "test", "test@gg.com", Some(Status(StatusType.Revoked,
          Some(RevokedReason.RevokedCeasedTrading))), Some(ContactType.RejectionReasons), Some("112345678251212"), variation = false)

        val json = Json.obj("safeId" -> "AA1234567891234",
          "name" -> "test",
          "email" -> "test@gg.com",
          "status" -> Json.obj("status_type" -> "08",
            "status_reason" -> "02"),
          "contact_type" -> "REJR",
          "contact_number" -> "112345678251212",
          "variation" -> false)

        NotificationPushRequest.jsonWrites.writes(model) must be(json)
      }

      "status and status reason is DeRegistered" in {

        val model = NotificationPushRequest("AA1234567891234", "test", "test@gg.com", Some(Status(StatusType.DeRegistered,
          Some(DeregisteredReason.CeasedTrading))), Some(ContactType.RejectionReasons), Some("112345678251212"), variation = false)

        val json = Json.obj("safeId" -> "AA1234567891234",
          "name" -> "test",
          "email" -> "test@gg.com",
          "status" -> Json.obj("status_type" -> "10",
            "status_reason" -> "01"),
          "contact_type" -> "REJR",
          "contact_number" -> "112345678251212",
          "variation" -> false)

        NotificationPushRequest.jsonWrites.writes(model) must be(json)
      }

      "status and status reason is Expired" in {
        val model = NotificationPushRequest("AA1234567891234", "test", "test@gg.com", Some(Status(StatusType.Expired, None)),
          Some(ContactType.RejectionReasons), Some("112345678251212"), variation = false)

        val json = Json.obj("safeId" -> "AA1234567891234",
          "name" -> "test",
          "email" -> "test@gg.com",
          "status" -> Json.obj("status_type" -> "11"),
          "contact_type" -> "REJR",
          "contact_number" -> "112345678251212",
          "variation" -> false)

        NotificationPushRequest.jsonWrites.writes(model) must be(json)
      }

      "status and status reason is Approved" in {

        val model = NotificationPushRequest("AA1234567891234", "test", "test@gg.com", Some(Status(StatusType.Approved, None)),
          Some(ContactType.RejectionReasons), Some("112345678251212"), variation = false)
        val json = Json.obj(
          "safeId" -> "AA1234567891234",
          "name" -> "test",
          "email" -> "test@gg.com",
          "status" -> Json.obj("status_type" -> "04"),
          "contact_type" -> "REJR",
          "contact_number" -> "112345678251212",
          "variation" -> false)

        NotificationPushRequest.jsonWrites.writes(model) must be(json)
      }
    }

    "validate NotificationPushRequest format method" in {

      val model = NotificationPushRequest("AA1234567891234", "test", "test@gg.com", Some(Status(StatusType.Expired, None)),
        Some(ContactType.RejectionReasons), Some("112345678251212"), variation = false)

      NotificationPushRequest.formats.reads(NotificationPushRequest.formats.writes(model)) must be(JsSuccess(model))
    }

    "returns 'true' for 'isSane' when there is no contact type and" when {
      "status is 'DeRegistered'" in {
        val model = NotificationPushRequest(
          "AA123456789",
          "test",
          "test@test.com",
          Some(Status(DeRegistered, None)),
          None,
          None,
          variation = false
        )

        model.isSane mustBe true
      }

      "status is not DeRegistered and there is a status reason" in {
        val model = NotificationPushRequest(
          "AA123456789",
          "test",
          "test@test.com",
          Some(Status(Rejected, Some(FailedToRespond))),
          None,
          None,
          variation = false
        )

        model.isSane mustBe true
      }
    }

    "returns 'false' for 'isSane' when there is no Contact Type and" when {
      "there is no status" in {
        val model = NotificationPushRequest(
          "AA123456789",
          "test",
          "test@test.com",
          None,
          None,
          None,
          variation = false
        )

        model.isSane mustBe false
      }

      "status is not deregistered and there is no status reason" in {
        val model = NotificationPushRequest(
          "AA123456789",
          "test",
          "test@test.com",
          Some(Status(Approved, None)),
          None,
          None,
          variation = false
        )

        model.isSane mustBe false
      }
    }
  }
}
