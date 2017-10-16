/*
 * Copyright 2017 HM Revenue & Customs
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
import models.fe.NotificationDetails
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.ArgumentCaptor
import org.scalatest.mock.MockitoSugar
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import repositories.NotificationRepository
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import utils.DataGen._


class ViewNotificationControllerSpec extends PlaySpec with GeneratorDrivenPropertyChecks
  with ScalaFutures
  with MockitoSugar
  with OneAppPerSuite {

  trait Fixture {
    val TestController = new ViewNotificationController {
      override val connector = mock[ViewNotificationConnector]
      override private[controllers] val audit = mock[AuditConnector]
      override private[controllers] val notificationRepository = mock[NotificationRepository]
    }
  }

  val request = FakeRequest() withHeaders CONTENT_TYPE -> "application/json"

  val dateTime = new DateTime(1479730062573L, DateTimeZone.UTC)

  "ViewNotificationController" must {
    val amlsRegistrationNumber = amlsRegNumberGen.sample.get
    val contactNumber = "11111"

    "return a `BadRequest` response when the amls registration number is invalid" in new Fixture {
      val result = TestController.viewNotification("accountType", "ref", "test", "test")(request)
      val failure = Json.obj("errors" -> Seq("Invalid AMLS Registration Number"))

      when {
        TestController.audit.sendEvent(any())(any(), any())
      } thenReturn Future.successful(mock[AuditResult])

      status(result) must be(BAD_REQUEST)
      contentAsJson(result) must be(failure)
    }

    "return a valid response when the amls registration number is valid" in new Fixture {
      val record = notificationRecordGen.sample.map(_.copy(
        contactNumber = Some(contactNumber),
        amlsRegistrationNumber = amlsRegistrationNumber))

      val expectedDetails = NotificationDetails(
        record flatMap {_.contactType},
        record flatMap {_.status},
        Some("secure-comms text"),
        record.fold(false) {_.variation},
        record.fold(new DateTime()){_.receivedAt}
      )

      when {
        TestController.audit.sendEvent(any())(any(), any())
      } thenReturn Future.successful(mock[AuditResult])

      when {
        TestController.notificationRepository.findById("NOTIFICATIONID")
      } thenReturn Future.successful(record)

      when {
        TestController.connector.getNotification(eqTo(amlsRegistrationNumber), eqTo(contactNumber))(any(), any())
      } thenReturn Future.successful(Des.notificationResponseGen.sample.map(_.copy(secureCommText = "secure-comms text")).get)

      val result = TestController.viewNotification("accountType", "ref", amlsRegistrationNumber, "NOTIFICATIONID")(request)

      status(result) must be(OK)
      contentAsJson(result) must be(Json.toJson(expectedDetails))

      val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

      verify(TestController.audit).sendExtendedEvent(captor.capture())(any(), any())
      captor.getValue.auditType mustBe "OutboundCall"
    }

    "update the isRead flag" in new Fixture {
      when(TestController.notificationRepository.findById(any()))
        .thenReturn(Future.successful(notificationRecordGen.sample.map {
          _.copy(amlsRegistrationNumber = amlsRegistrationNumber)
        }))

      when(TestController.connector.getNotification(any(), any())(any(), any()))
        .thenReturn(Future.successful(Des.notificationResponseGen.sample.get))

      when(TestController.notificationRepository.markAsRead("NOTIFICATIONID"))
        .thenReturn(Future.successful(true))


      val result = TestController.viewNotification("accountType", "ref", amlsRegistrationNumber, "NOTIFICATIONID")(request)

      status(result) must be(OK)
      verify(TestController.notificationRepository)
        .markAsRead("NOTIFICATIONID")
    }

    "still succeed when the notification cannot be marked as read" in new Fixture {
      when(TestController.notificationRepository.findById(any()))
        .thenReturn(Future.successful(notificationRecordGen.sample.map {
          _.copy(amlsRegistrationNumber = amlsRegistrationNumber)
        }))

      when(TestController.connector.getNotification(any(), any())(any(), any()))
        .thenReturn(Future.successful(Des.notificationResponseGen.sample.get))

      when(TestController.notificationRepository.markAsRead(any()))
        .thenReturn(Future.failed(new HttpStatusException(500, None)))

      val result = TestController.viewNotification("accountType", "ref", amlsRegistrationNumber, "NOTIFICATIONID")(request)

      status(result) must be(OK)
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
                status = Some(Status(StatusType.Approved, Some(RejectedReason.FailedToRespond))),
                variation = true,
                receivedAt = dateTime
              ))))

              when {
                TestController.connector.getNotification(regNo, "CONTACTNUMBER1")
              }.thenReturn(Future.successful(Des.notificationResponseGen.sample.map(_.copy(
                secureCommText = "THIS IS THE MESSAGE TEXT 00001")).get))

              val result = TestController.viewNotification("accountType", "ref", regNo, "NOTIFICATIONID1")(request)

              status(result) must be(OK)
              contentAsJson(result) must be(Json.obj(
                "contactType" -> "RPM1",
                "status" -> Json.obj("status_type"->"04", "status_reason" -> "02"),
                "messageText" -> "THIS IS THE MESSAGE TEXT 00001",
                "variation" -> true,
                "receivedAt" -> Json.parse("""{"$date":1479730062573}""")
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
                status = Some(Status(StatusType.Approved, Some(RejectedReason.FailedToRespond))),
                variation = true,
                receivedAt = dateTime
              ))))

              val result = TestController.viewNotification("accountType", "ref", regNo, "NOTIFICATIONID2")(request)

              status(result) must be(OK)
              contentAsJson(result) must be(Json.obj(
                "contactType" -> "RPM1",
                "status" -> Json.obj("status_type" -> "04", "status_reason" -> "02"),
                "variation" -> true,
                "receivedAt" -> Json.parse("""{"$date":1479730062573}""")
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
