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

import connectors.ViewNotificationConnector
import exceptions.HttpStatusException
import org.joda.time.LocalDateTime
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import models._
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Future

class ViewNotificationControllerSpec extends PlaySpec
  with ScalaFutures
  with MockitoSugar {

  object TestController extends ViewNotificationController {
    override val connector = mock[ViewNotificationConnector]
  }

  val request = FakeRequest()
    .withHeaders(CONTENT_TYPE -> "application/json")

  "ViewNotificationController" must {

      val amlsRegistrationNumber = "XAML00000567890"
      val contactNumber = "11111"

      "return a `BadRequest` response when the amls registration number is invalid" in {

        val result = TestController.viewNotification("test", "test")(request)
        val failure = Json.obj("errors" -> Seq("Invalid AMLS Registration Number"))

        status(result) must be(BAD_REQUEST)
        contentAsJson(result) must be(failure)
      }

      "return a valid response when the amls registration number is valid" in {

        val response = NotificationResponse(LocalDateTime.now(), "secure-comms text")

        when {
          TestController.connector.getNotification(eqTo(amlsRegistrationNumber), eqTo(contactNumber))(any(), any())
        } thenReturn Future.successful(response)

        val result = TestController.viewNotification(amlsRegistrationNumber, contactNumber)(request)

        status(result) must be(OK)
        contentAsJson(result) must be(Json.toJson(response))
      }

      "return an invalid response when the service fails" in {

        when {
          TestController.connector.getNotification(eqTo(amlsRegistrationNumber), eqTo(contactNumber))(any(), any())
        } thenReturn Future.failed(new HttpStatusException(INTERNAL_SERVER_ERROR, Some("message")))

        whenReady (TestController.viewNotification(amlsRegistrationNumber, contactNumber)(request).failed) {
          case HttpStatusException(status, body) =>
            status mustEqual INTERNAL_SERVER_ERROR
            body mustEqual Some("message")
        }

    }


  }

}
