package models

import org.scalatestplus.play.PlaySpec
import play.api.libs.json._

class NotificationPushRequestSpec extends PlaySpec {

  "NotificationPushRequest" must {

    "read json successfully" when {
      "status and status reason is Rejected" in {

        val json =  Json.obj("name" -> "test",
          "email" -> "test@gg.com",
          "status" -> Json.obj("status_type" -> "06",
            "status_reason" -> "03"),
          "contact_type" -> "REJR",
          "contact_number" -> "112345678251212",
          "variation" -> false)

        NotificationPushRequest.jsonReads.reads(json) must be(JsSuccess(NotificationPushRequest("test","test@gg.com",Some(Status(StatusType.Rejected, Some(RejectedReason.FailedToPayCharges))),Some("REJR"),Some("112345678251212"),false)))
      }

      "status and status reason is Revoked" in {

        val json =  Json.obj("name" -> "test",
          "email" -> "test@gg.com",
          "status" -> Json.obj("status_type" -> "08",
            "status_reason" -> "02"),
          "contact_type" -> "REJR",
          "contact_number" -> "112345678251212",
          "variation" -> false)

        NotificationPushRequest.jsonReads.reads(json) must be(JsSuccess(NotificationPushRequest("test","test@gg.com",Some(Status(StatusType.Revoked, Some(RevokedReason.RevokedCeasedTrading))),Some("REJR"),Some("112345678251212"),false)))
      }

      "status and status reason is DeRegistered" in {

        val json =  Json.obj("name" -> "test",
          "email" -> "test@gg.com",
          "status" -> Json.obj("status_type" -> "10",
            "status_reason" -> "01"),
          "contact_type" -> "REJR",
          "contact_number" -> "112345678251212",
          "variation" -> false)

        NotificationPushRequest.jsonReads.reads(json) must be(JsSuccess(NotificationPushRequest("test","test@gg.com",Some(Status(StatusType.DeRegistered, Some(DeregisteredReason.CeasedTrading))),Some("REJR"),Some("112345678251212"),false)))
      }

      "status and status reason is Expired" in {

        val json =  Json.obj("name" -> "test",
          "email" -> "test@gg.com",
          "status" -> Json.obj("status_type" -> "11",
            "status_reason" -> "03"),
          "contact_type" -> "REJR",
          "contact_number" -> "112345678251212",
          "variation" -> false)

        NotificationPushRequest.jsonReads.reads(json) must be(JsSuccess(NotificationPushRequest("test","test@gg.com",Some(Status(StatusType.Expired, None)),Some("REJR"),Some("112345678251212"),false)))
      }

      "status and status reason is Approved" in {

        val json =  Json.obj("name" -> "test",
          "email" -> "test@gg.com",
          "status" -> Json.obj("status_type" -> "04",
            "status_reason" -> "03"),
          "contact_type" -> "REJR",
          "contact_number" -> "112345678251212",
          "variation" -> false)

        NotificationPushRequest.jsonReads.reads(json) must be(JsSuccess(NotificationPushRequest("test","test@gg.com",Some(Status(StatusType.Approved, None)), Some("REJR"),Some("112345678251212"),false)))
      }

    }

    "serialise successfully" when {
      "path 'status' is missing in json" in {
        {
           val json =  Json.obj("name" -> "test",
            "email" -> "test@gg.com",
            "status_reason" -> "03",
            "contact_type" -> "REJR",
            "contact_number" -> "112345678251212",
            "variation" -> false)

          NotificationPushRequest.jsonReads.reads(json) must be(JsSuccess(NotificationPushRequest("test","test@gg.com", None,Some("REJR"),Some("112345678251212"),false)))

        }
      }
    }

    "write json successfully" when {
      "status and status reason is Rejected" in {

        val model = NotificationPushRequest("test","test@gg.com", Some(Status(StatusType.Rejected,
          Some(RejectedReason.FailedToPayCharges))), Some("REJR"), Some("112345678251212"), false)

        val json = Json.obj("name" -> "test",
          "email" -> "test@gg.com",
          "status" -> Json.obj("status_type" -> "06"),
          "contact_type" -> "REJR",
          "contact_number" -> "112345678251212",
          "variation" -> false)

        NotificationPushRequest.jsonWrites.writes(model) must be(json)

      }

      "status and status reason is Revoked" in {

        val model = NotificationPushRequest("test","test@gg.com",Some(Status(StatusType.Revoked, Some(RevokedReason.RevokedCeasedTrading))),Some("REJR"),Some("112345678251212"),false)

        val json =  Json.obj("name" -> "test",
          "email" -> "test@gg.com",
          "status" -> Json.obj("status_type" -> "08",
            "status_reason" -> "02"),
          "contact_type" -> "REJR",
          "contact_number" -> "112345678251212",
          "variation" -> false)

        NotificationPushRequest.jsonWrites.writes(model) must be(json)
      }

      "status and status reason is DeRegistered" in {

        val model = NotificationPushRequest("test","test@gg.com",Some(Status(StatusType.DeRegistered, Some(DeregisteredReason.CeasedTrading))),Some("REJR"),Some("112345678251212"),false)

        val json =  Json.obj("name" -> "test",
          "email" -> "test@gg.com",
          "status" -> Json.obj("status_type" -> "10",
            "status_reason" -> "01"),
          "contact_type" -> "REJR",
          "contact_number" -> "112345678251212",
          "variation" -> false)

        NotificationPushRequest.jsonWrites.writes(model) must be(json)
      }

      "status and status reason is Expired" in {
        val model = NotificationPushRequest("test","test@gg.com",Some(Status(StatusType.Expired, None)),Some("REJR"),Some("112345678251212"),false)

        val json =  Json.obj("name" -> "test",
          "email" -> "test@gg.com",
          "status" -> Json.obj("status_type" -> "11"),
          "contact_type" -> "REJR",
          "contact_number" -> "112345678251212",
          "variation" -> false)

        NotificationPushRequest.jsonWrites.writes(model) must be(json)
      }

      "status and status reason is Approved" in {

        val model = NotificationPushRequest("test","test@gg.com",Some(Status(StatusType.Approved, None)), Some("REJR"),Some("112345678251212"),false)
        val json =  Json.obj("name" -> "test",
          "email" -> "test@gg.com",
          "status" -> Json.obj("status_type" -> "04"),
          "contact_type" -> "REJR",
          "contact_number" -> "112345678251212",
          "variation" -> false)

        NotificationPushRequest.jsonWrites.writes(model) must be(json)
      }

    }

  }
}
