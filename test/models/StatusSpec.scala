package models

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsSuccess, Json}

class StatusSpec extends PlaySpec {

  "Status model" must {
    "must serialise and de serialise data successfully" in {

      val json = Json.obj("status" -> "04")
      Status.jsonReads.reads(json) must be(JsSuccess(StatusType.Approved))

    }
  }


}
