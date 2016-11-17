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
import models._
import org.joda.time.LocalDateTime
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import models.des.NotificationResponse
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import repositories.NotificationRepository
import scala.concurrent.Future
import org.joda.time.DateTime
import scala.concurrent.ExecutionContext.Implicits.global

class ViewNotificationControllerSpec extends PlaySpec
  with ScalaFutures
  with MockitoSugar {

  trait Fixture {
   val TestController = new ViewNotificationController {
      override val connector = mock[ViewNotificationConnector]
      override private[controllers] val repo = mock[NotificationRepository]
    }
  }

  val request = FakeRequest()
    .withHeaders(CONTENT_TYPE -> "application/json")

  "ViewNotificationController" must {

      val amlsRegistrationNumber = "XAML00000567890"
      val contactNumber = "11111"

      "return a `BadRequest` response when the amls registration number is invalid" in new Fixture {

        val result = TestController.viewNotification("test", "test")(request)
        val failure = Json.obj("errors" -> Seq("Invalid AMLS Registration Number"))

        status(result) must be(BAD_REQUEST)
        contentAsJson(result) must be(failure)
      }

      "return a valid response when the amls registration number is valid" in new Fixture {

        val response = NotificationResponse(LocalDateTime.now(), "secure-comms text")

        when {
          TestController.connector.getNotification(eqTo(amlsRegistrationNumber), eqTo(contactNumber))(any(), any())
        } thenReturn Future.successful(response)

        val result = TestController.viewNotification(amlsRegistrationNumber, contactNumber)(request)

        status(result) must be(OK)
        contentAsJson(result) must be(Json.toJson(response))
      }

      "return an invalid response when the service fails" in new Fixture {

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

  it when {
    "the requested notification exists" when {
      "the requested notification belongs to the AMLSRegistration" when {
        "the requested notification contains a contact number" must {
          "return message details containing message type from the repo and text from the connector" in new Fixture{
            when {
              TestController.repo.findById("NOTIFICATIONID1")
            }.thenReturn(Future.successful(Some(NotificationRecord(
                                                  "AMLSREGISTRATIONNO1",
                                                  "NAME1",
                                                  "EMAIL1",
                                                  Some(Status(Some(StatusType.Approved), Option(RejectedReason.FailedToRespond))),
                                                  Some(ContactType.ReminderToPayForManualCharges),
                                                  Some("CONTACTNUMBER1"),
                                                  false,
                                                  new DateTime("2015-1-28")))))

            when {
              TestController.connector.getNotification("AMLSREGISTRATIONNO1", "CONTACTNUMBER1")
            }.thenReturn(Future.successful(NotificationResponse(new LocalDateTime("1923-5-27"), "THIS IS THE MESSAGE TEXT 00001")))

            val result = TestController.viewNotification("AMLSREGISTRATIONNO1", "NOTIFICATIONID1")(request)
            status(result) must be (OK)
            contentAsJson(result) must be (Json.obj(
                "contactType" -> "RPM1",
                "status" -> "04",
                "statusReason" -> "02",
                "messageText" -> "THIS IS THE MESSAGE TEXT 00001"
              )
            )
          }
        }

        "the requested notification does not contain a contact number" must {
          "return message details containing message type from the repo and no message text" in {
            1 must be (2)
          }
        }
      }

      "the requested notification does not belong to the AMLSRegistration" must {
        "return a Not Found" in {
          1 must be(2)
        }
      }
    }

    "the requested notification does not exist" must {
      "return a not found " in new Fixture {
        when {
            TestController.repo.findById("NOTIFICATIONID")
        }.thenReturn(Future.successful(None))

        val result = TestController.viewNotification("AMLSREGISTRATIONNO2", "NOTIFICATIONID")(request)
        status(result) must be (NOT_FOUND)
      }
    }
  }

}
