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

import akka.stream.Materializer
import connectors.ViewNotificationConnector
import models.{ContactType, NotificationPushRequest}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.NotificationRepository

class NotificationControllerSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  object TestNotificationController extends NotificationController {
    override private[controllers] val notificationRepository = mock[NotificationRepository]

  }

  val request = FakeRequest()
    .withHeaders(CONTENT_TYPE -> "application/json")


  "NotificationController" must {

    val amlsRegistrationNumber = "XAML00000567890"
    val body = NotificationPushRequest ("name", "hh@test.com", None, Some(ContactType.ApplicationApproval), None, false)


    "save the api12 input push request into mongo repo successfully" in {

    }

    "return BadRequest, if input request fails validation" in {
      val mtrlzr = mock[Materializer]
      val result = TestNotificationController.saveNotification("")(request)
      val failure = Json.obj("errors" -> Seq("Invalid SafeId"))
      val gg = result.run()(mtrlzr)

      status(gg) must be(BAD_REQUEST)
      contentAsJson(gg) must be(failure)

    }
  }

}
