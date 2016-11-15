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

package controllers

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
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

           /*val result = NotificationController.save("test", "test", "test")(request)(body)
      val failure = Json.obj("errors" -> Seq("Invalid SafeId"))

      status(result) must be(BAD_REQUEST)
      contentAsJson(result) must be(failure)*/

    }
  }

}
