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

import models.{NotificationRecord, ContactType, NotificationPushRequest}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.NotificationRepository

import scala.concurrent.Future

class NotificationControllerSpec extends PlaySpec with MockitoSugar with ScalaFutures  with OneServerPerSuite{

  object TestNotificationController extends NotificationController {
    override private[controllers] val notificationRepository = NotificationRepository()

  }
  val body = NotificationPushRequest ("name", "hh@test.com", None, Some(ContactType.ApplicationApproval), None, false)
  val request = FakeRequest("POST", "/")
    .withHeaders(CONTENT_TYPE -> "application/json")
      .withBody[JsValue](Json.toJson(body))


  "NotificationController" must {

    val amlsRegistrationNumber = "XAML00000567890"

    "save the api12 input push request into mongo repo successfully" in {
     // when(TestNotificationController.notificationRepository.insertRecord(any())).thenReturn(Future.successful(true))

      TestNotificationController.notificationRepository.insertRecord(NotificationRecord(amlsRegistrationNumber,
        "name",
        "gg@gmail.com",
        None, Some(ContactType.ApplicationApproval), None, false))

      whenReady(TestNotificationController.saveNotification(amlsRegistrationNumber)(request)) {
        result =>
          result mustEqual true
      }
    }

    "return BadRequest, if input request fails validation" in {
      val result = TestNotificationController.saveNotification("hhhh")(request)
      val failure = Json.obj("errors" -> Seq("Invalid SafeId"))
      val gg = result

      status(gg) must be(BAD_REQUEST)
      println(contentAsString(gg))
      contentAsJson(gg) must be(failure)

    }
  }

}
