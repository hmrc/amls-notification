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

import java.util.NoSuchElementException

import connectors.ViewNotificationConnector
import exceptions.HttpStatusException
import models._
import models.fe.NotificationDetails
import org.joda.time.LocalDateTime
import org.scalatest.mock.MockitoSugar
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import models.des.NotificationResponse
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import repositories.NotificationRepository
import reactivemongo.bson.BSONObjectID
import repositories.NotificationRepository

import scala.concurrent.Future
import org.joda.time.DateTime
import scala.concurrent.ExecutionContext.Implicits.global
import utils.DataGen._
import java.util.NoSuchElementException

import scala.concurrent.duration.Duration
import scala.util.Try


class ViewNotificationControllerSpec extends PlaySpec with GeneratorDrivenPropertyChecks
  with ScalaFutures
  with MockitoSugar {

  trait Fixture {
    val TestController = new ViewNotificationController {
      override val connector = mock[ViewNotificationConnector]
      override private[controllers] val notificationRepository = mock[NotificationRepository]
    }
  }

  val request = FakeRequest()
    .withHeaders(CONTENT_TYPE -> "application/json")

  "ViewNotificationController" must {
    val amlsRegistrationNumber = amlsRegNumberGen.sample.get
    val contactNumber = "11111"

    "return a `BadRequest` response when the amls registration number is invalid" in new Fixture {
      val result = TestController.viewNotification("accountType", "ref", "test", "test")(request)
      val failure = Json.obj("errors" -> Seq("Invalid AMLS Registration Number"))

      status(result) must be(BAD_REQUEST)
      contentAsJson(result) must be(failure)
    }

    "return a valid response when the amls registration number is valid" in new Fixture {
      val record = notificationRecordGen.sample.map(_.copy(
        contactNumber = Some(contactNumber),
        amlsRegistrationNumber = amlsRegistrationNumber))

      val expectedDetails = NotificationDetails(
        record flatMap {
          _.contactType
        },
        record flatMap {
          _.status flatMap {
            _.status
          }
        },
        record flatMap {
          _.status flatMap {
            _.statusReason
          }
        },
        Some("secure-comms text"))

      when {
        TestController.notificationRepository.findById("NOTIFICATIONID")
      } thenReturn Future.successful(record)

      when {
        TestController.connector.getNotification(eqTo(amlsRegistrationNumber), eqTo(contactNumber))(any(), any())
      } thenReturn Future.successful(Des.notificationResponseGen.sample.map(_.copy(secureCommText = "secure-comms text")).get)

      val result = TestController.viewNotification("accountType", "ref", amlsRegistrationNumber, "NOTIFICATIONID")(request)

      status(result) must be(OK)
      contentAsJson(result) must be(Json.toJson(expectedDetails))
    }

    "update the isRead flag successfully" in new Fixture {

      val mockBSONObjectID = mock[BSONObjectID]
      when(TestController.notificationRepository.markAsRead(any())).thenReturn(Future.successful(true))

      val result = TestController.markNotificationAsRead(mockBSONObjectID)(request)
      status(result) must be(OK)
      contentAsJson(result) must be(Json.toJson(true))

    }

    "return an invalid response when the service fails" in new Fixture {
      private val maybeRecord = notificationRecordGen.sample.map(_.copy(
        amlsRegistrationNumber = amlsRegistrationNumber,
        contactNumber = Some("CONTACTNUMBER")))

      when {
        TestController.notificationRepository.findById("NOTIFICATIONID")
      } thenReturn Future.successful(maybeRecord)

      when {
        TestController.connector.getNotification(any(), any())(any(), any())
      } thenReturn Future.failed(new HttpStatusException(INTERNAL_SERVER_ERROR, Some("message")))

      private val future = TestController.viewNotification("accountType", "ref", amlsRegistrationNumber, "NOTIFICATIONID")(request)

      whenReady(future.failed) {
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
            "return message details containing message type from the repo and text from the connector" in new Fixture {
              val regNo = amlsRegNumberGen.sample.get

              when {
                TestController.notificationRepository.findById("NOTIFICATIONID1")
              }.thenReturn(Future.successful(notificationRecordGen.sample.map(_.copy(
                amlsRegistrationNumber = regNo,
                contactNumber = Some("CONTACTNUMBER1"),
                contactType = Some(ContactType.ReminderToPayForManualCharges),
                status = Some(Status(Some(StatusType.Approved), Some(RejectedReason.FailedToRespond)))
              ))))

              when {
                TestController.connector.getNotification(regNo, "CONTACTNUMBER1")
              }.thenReturn(Future.successful(Des.notificationResponseGen.sample.map(_.copy(secureCommText = "THIS IS THE MESSAGE TEXT 00001")).get))

              val result = TestController.viewNotification("accountType", "ref", regNo, "NOTIFICATIONID1")(request)

              status(result) must be(OK)
              contentAsJson(result) must be(Json.obj(
                "contactType" -> "RPM1",
                "status" -> "04",
                "statusReason" -> "02",
                "messageText" -> "THIS IS THE MESSAGE TEXT 00001"
              ))
            }
          }

          "the requested notification does not contain a contact number" must {
            "return message details containing message type from the repo and no message text" in new Fixture {
              val regNo = amlsRegNumberGen.sample.get
              when {
                TestController.notificationRepository.findById("NOTIFICATIONID2")
              }.thenReturn(Future.successful(notificationRecordGen.sample.map(_.copy(
                amlsRegistrationNumber = regNo,
                contactNumber = None,
                contactType = Some(ContactType.ReminderToPayForManualCharges),
                status = Some(Status(Some(StatusType.Approved), Some(RejectedReason.FailedToRespond)))
              ))))

              val result = TestController.viewNotification("accountType", "ref", regNo, "NOTIFICATIONID2")(request)

              status(result) must be(OK)
              contentAsJson(result) must be(Json.obj(
                "contactType" -> "RPM1",
                "status" -> "04",
                "statusReason" -> "02"
              ))
            }
          }
        }

        "the requested notification does not belong to the AMLSRegistration" must {
          "return a Not Found" in new Fixture {
            when {
              TestController.notificationRepository.findById("NOTIFICATIONID1")
            }.thenReturn(Future.successful(notificationRecordGen.sample.map(_.copy(
              contactNumber = Some("CONTACTNUMBER1"),
              amlsRegistrationNumber = "NOT A REAL AMLS REG NUMBER"
            ))))

            val regNo = amlsRegNumberGen.sample.get

            val result = TestController.viewNotification("accountType", "ref", regNo, "NOTIFICATIONID1")(request)

            status(result) must be(NOT_FOUND)
          }
        }
      }

      "the requested notification does not exist in the repo" must {
        "return a not found " in new Fixture {
          when {
            TestController.notificationRepository.findById(any())
          }.thenReturn(Future.successful(None))

          val result = TestController.viewNotification("accountType", "ref", amlsRegNumberGen.sample.get, "NOTIFICATIONID")(request)
          status(result) must be(NOT_FOUND)
        }
      }
    }
}
