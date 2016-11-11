package controllers

import models.NotificationPushRequest
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._

class NotificationControllerSpec extends PlaySpec with MockitoSugar {

  object NotificationController extends NotificationController {

  }

  val request = FakeRequest()
    .withHeaders(CONTENT_TYPE -> "application/json")

  "NotificationController" must {

    "save the api12 input push request into mongo repo successfully" in {

    }

    "return BadRequest, if input request fails validation" in {

      val body = NotificationPushRequest (
        "name",
        "test@gmail.com",
        "status",
        "contantType",
        "contactNumber"
      )
      val result = NotificationController.save("test", "test", "test")(request)(body)
      val failure = Json.obj("errors" -> Seq("Invalid SafeId"))

      status(result) must be(BAD_REQUEST)
      contentAsJson(result) must be(failure)

    }
  }

}
