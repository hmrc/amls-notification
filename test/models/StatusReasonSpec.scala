package models

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{Json, JsSuccess}

class StatusReasonSpec extends PlaySpec {

  "StatusReason model" must {
    "must serialise and de serialise data successfully" in {

    //  val jssVal = StatusReason.jsonWrites.writes(RejectedReason.FailedToPayCharges)
      val json = Json.obj("status" -> "04",
      "status_reason" -> "03")
      StatusReason.jsonReads.reads(json) must be(JsSuccess(RejectedReason.FailedToPayCharges))

    }
  }



}
