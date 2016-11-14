package models

import org.scalatestplus.play.PlaySpec
import play.api.libs.json._

class NotificationPushRequestSpec extends PlaySpec {

  "NotificationPushRequest" must {
    "serialise successfully" in {

      val json =  Json.obj("name" -> "test",
      "email" -> "test@gg.com",
      "status" -> "06",
      "status_reason" -> "03",
      "contact_type" -> "REJR",
      "contact_number" -> "112345678251212",
        "variation" -> false)

      NotificationPushRequest.jsonReads.reads(json) must be()

    }
  }
}
