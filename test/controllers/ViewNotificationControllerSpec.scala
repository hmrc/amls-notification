/*
 * Copyright 2024 HM Revenue & Customs
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

import java.time.Instant
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.NotificationMongoRepository
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import utils.DataGen._
import utils.SuccessfulAuthAction
import ch.qos.logback.classic.Level
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.Logger
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ViewNotificationControllerSpec
    extends PlaySpec
    with ScalaFutures
    with MockitoSugar
    with GuiceOneAppPerSuite
    with LogCapturing
    with Eventually {

  trait Fixture {

    val mockViewNotificationConnector   = mock[ViewNotificationConnector]
    val mockAuditConnector              = mock[AuditConnector]
    val mockCC: ControllerComponents    = app.injector.instanceOf[ControllerComponents]
    val mockNotificationMongoRepository = mock[NotificationMongoRepository]

    when(mockAuditConnector.sendEvent(any())(any(), any()))
      .thenReturn(Future.successful(AuditResult.Success))
    when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
      .thenReturn(Future.successful(AuditResult.Success))

    val viewNotificationController = new ViewNotificationController(
      mockViewNotificationConnector,
      mockAuditConnector,
      mockCC,
      SuccessfulAuthAction,
      mockNotificationMongoRepository
    )
  }

  val request = FakeRequest() withHeaders CONTENT_TYPE -> "application/json"

  val dateTime: Instant = Instant.ofEpochMilli(1479730062573L)

  "ViewNotificationController" must {
    val amlsRegistrationNumber = amlsRegNumberGen.sample.get
    val contactNumber          = "11111"

    "return a `BadRequest` response when the amls registration number is invalid" in new Fixture {
      val result  = viewNotificationController.viewNotification("accountType", "ref", "test", "test")(request)
      val failure = Json.obj("errors" -> Seq("Invalid AMLS Registration Number"))

      status(result)        must be(BAD_REQUEST)
      contentAsJson(result) must be(failure)
    }

    "return a valid response when the amls registration number is valid" in new Fixture {
      val record = notificationRecordGen.sample.map(
        _.copy(contactNumber = Some(contactNumber), amlsRegistrationNumber = amlsRegistrationNumber)
      )

      val expectedDetails = NotificationDetails(
        record flatMap { _.contactType },
        record flatMap { _.status },
        Some("secure-comms text"),
        record.fold(false)(_.variation),
        record.fold(Instant.now())(_.receivedAt)
      )

      when {
        viewNotificationController.notificationRepository.findById("NOTIFICATIONID")
      } thenReturn Future.successful(record)

      when {
        viewNotificationController.connector
          .getNotification(eqTo(amlsRegistrationNumber), eqTo(contactNumber))(any())
      } thenReturn Future.successful(
        Des.notificationResponseGen.sample.map(_.copy(secureCommText = "secure-comms text")).get
      )

      when {
        viewNotificationController.notificationRepository.markAsRead("NOTIFICATIONID")
      } thenReturn Future.successful(true)

      val result =
        viewNotificationController.viewNotification("accountType", "ref", amlsRegistrationNumber, "NOTIFICATIONID")(
          request
        )

      status(result)        must be(OK)
      contentAsJson(result) must be(Json.toJson(expectedDetails))

      val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

      verify(viewNotificationController.audit).sendExtendedEvent(captor.capture())(any(), any())
      captor.getValue.auditType mustBe "OutboundCall"
    }

    "update the isRead flag" in new Fixture {
      when(viewNotificationController.notificationRepository.findById(any()))
        .thenReturn(Future.successful(notificationRecordGen.sample.map {
          _.copy(amlsRegistrationNumber = amlsRegistrationNumber)
        }))

      when(viewNotificationController.connector.getNotification(any(), any())(any()))
        .thenReturn(Future.successful(Des.notificationResponseGen.sample.get))

      when(viewNotificationController.notificationRepository.markAsRead("NOTIFICATIONID"))
        .thenReturn(Future.successful(true))

      val result =
        viewNotificationController.viewNotification("accountType", "ref", amlsRegistrationNumber, "NOTIFICATIONID")(
          request
        )

      status(result) must be(OK)
      verify(viewNotificationController.notificationRepository)
        .markAsRead("NOTIFICATIONID")
    }

    "still succeed when the notification cannot be marked as read" in new Fixture {
      when(viewNotificationController.notificationRepository.findById(any()))
        .thenReturn(Future.successful(notificationRecordGen.sample.map {
          _.copy(amlsRegistrationNumber = amlsRegistrationNumber)
        }))

      when(viewNotificationController.connector.getNotification(any(), any())(any()))
        .thenReturn(Future.successful(Des.notificationResponseGen.sample.get))

      when(viewNotificationController.notificationRepository.markAsRead(any()))
        .thenReturn(Future.failed(new HttpStatusException(500, None)))

      val result =
        viewNotificationController.viewNotification("accountType", "ref", amlsRegistrationNumber, "NOTIFICATIONID")(
          request
        )

      status(result) must be(OK)
    }

    "return an invalid response when the service fails" in new Fixture {
      private val maybeRecord = notificationRecordGen.sample.map(
        _.copy(amlsRegistrationNumber = amlsRegistrationNumber, contactNumber = Some("CONTACTNUMBER"))
      )

      when {
        viewNotificationController.notificationRepository.findById("NOTIFICATIONID")
      } thenReturn Future.successful(maybeRecord)

      when {
        viewNotificationController.connector.getNotification(any(), any())(any())
      } thenReturn Future.failed(new HttpStatusException(INTERNAL_SERVER_ERROR, Some("message")))

      private val future =
        viewNotificationController.viewNotification("accountType", "ref", amlsRegistrationNumber, "NOTIFICATIONID")(
          request
        )

      whenReady(future.failed) { case HttpStatusException(status, body) =>
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
              viewNotificationController.notificationRepository.findById("NOTIFICATIONID1")
            }.thenReturn(
              Future.successful(
                notificationRecordGen.sample.map(
                  _.copy(
                    amlsRegistrationNumber = regNo,
                    contactNumber = Some("CONTACTNUMBER1"),
                    contactType = Some(ContactType.ReminderToPayForManualCharges),
                    status = Some(Status(StatusType.Approved, Some(RejectedReason.FailedToRespond))),
                    variation = true,
                    receivedAt = dateTime
                  )
                )
              )
            )

            when {
              viewNotificationController.connector.getNotification(regNo, "CONTACTNUMBER1")
            }.thenReturn(
              Future.successful(
                Des.notificationResponseGen.sample.map(_.copy(secureCommText = "THIS IS THE MESSAGE TEXT 00001")).get
              )
            )

            when {
              viewNotificationController.notificationRepository.markAsRead("NOTIFICATIONID1")
            }.thenReturn(Future.successful(true))

            val result =
              viewNotificationController.viewNotification("accountType", "ref", regNo, "NOTIFICATIONID1")(request)

            status(result)        must be(OK)
            contentAsJson(result) must be(
              Json.obj(
                "contactType" -> "RPM1",
                "status"      -> Json.obj("status_type" -> "04", "status_reason" -> "02"),
                "messageText" -> "THIS IS THE MESSAGE TEXT 00001",
                "variation"   -> true,
                "receivedAt"  -> Json.parse("""{"$date":{"$numberLong":"1479730062573"}}""")
              )
            )
          }
        }

        "the requested notification does not contain a contact number" must {
          "return message details containing message type from the repo and no message text" in new Fixture {
            val regNo = amlsRegNumberGen.sample.get
            when {
              viewNotificationController.notificationRepository.findById("NOTIFICATIONID2")
            }.thenReturn(
              Future.successful(
                notificationRecordGen.sample.map(
                  _.copy(
                    amlsRegistrationNumber = regNo,
                    contactNumber = None,
                    contactType = Some(ContactType.ReminderToPayForManualCharges),
                    status = Some(Status(StatusType.Approved, Some(RejectedReason.FailedToRespond))),
                    variation = true,
                    receivedAt = dateTime
                  )
                )
              )
            )

            when {
              viewNotificationController.notificationRepository.markAsRead("NOTIFICATIONID2")
            }.thenReturn(Future.successful(true))

            val result =
              viewNotificationController.viewNotification("accountType", "ref", regNo, "NOTIFICATIONID2")(request)

            status(result)        must be(OK)
            contentAsJson(result) must be(
              Json.obj(
                "contactType" -> "RPM1",
                "status"      -> Json.obj("status_type" -> "04", "status_reason" -> "02"),
                "variation"   -> true,
                "receivedAt"  -> Json.parse("""{"$date":{"$numberLong":"1479730062573"}}""")
              )
            )
          }
        }
      }

      "the requested notification does not belong to the AMLSRegistration" must {
        "return a Not Found" in new Fixture {
          when {
            viewNotificationController.notificationRepository.findById("NOTIFICATIONID1")
          }.thenReturn(
            Future.successful(
              notificationRecordGen.sample.map(
                _.copy(
                  contactNumber = Some("CONTACTNUMBER1"),
                  amlsRegistrationNumber = "NOT A REAL AMLS REG NUMBER"
                )
              )
            )
          )

          val regNo = amlsRegNumberGen.sample.get

          val result =
            viewNotificationController.viewNotification("accountType", "ref", regNo, "NOTIFICATIONID1")(request)

          status(result) must be(NOT_FOUND)
        }
      }
    }

    "the requested notification does not exist in the repo" must {
      "return a not found " in new Fixture {
        when {
          viewNotificationController.notificationRepository.findById(any())
        }.thenReturn(Future.successful(None))

        val result = viewNotificationController.viewNotification(
          "accountType",
          "ref",
          amlsRegNumberGen.sample.get,
          "NOTIFICATIONID"
        )(request)
        status(result) must be(NOT_FOUND)
      }
    }
  }

  private val controllerLogger: Logger = Logger(classOf[ViewNotificationController])
  private val backgroundPatience       = timeout(Span(5, Seconds))
  private val pollInterval             = interval(Span(50, Millis))

  "ViewNotificationController logging" must {
    val regNo = amlsRegNumberGen.sample.get

    "log a warning when the AMLS registration number is invalid" in new Fixture {
      withCaptureOfLoggingFrom(controllerLogger) { logs =>
        status(
          viewNotificationController.viewNotification("accountType", "ref", "invalid", "ID")(request)
        ) mustBe BAD_REQUEST

        logs.exists(e =>
          e.getLevel == Level.WARN &&
            e.getFormattedMessage.contains(
              "[ViewNotificationController][viewNotification] - Invalid AMLS registration number format"
            )
        ) mustBe true
      }
    }

    "log a warning when the notification belongs to a different AMLS registration number" in new Fixture {
      val otherRecord = notificationRecordGen.sample.map(_.copy(amlsRegistrationNumber = "XBML00000999999"))
      when(viewNotificationController.notificationRepository.findById("NOTIFICATIONID")) thenReturn Future.successful(
        otherRecord
      )

      withCaptureOfLoggingFrom(controllerLogger) { logs =>
        status(
          viewNotificationController.viewNotification("accountType", "ref", regNo, "NOTIFICATIONID")(request)
        ) mustBe NOT_FOUND

        logs.exists(e =>
          e.getLevel == Level.WARN &&
            e.getFormattedMessage.contains(
              "[ViewNotificationController][viewNotification] - Notification does not belong to amlsRegNo"
            ) &&
            e.getFormattedMessage.contains("notificationId: NOTIFICATIONID")
        ) mustBe true
      }
    }

    "log a warning when the notification does not exist" in new Fixture {
      when(viewNotificationController.notificationRepository.findById("NOTIFICATIONID")) thenReturn Future.successful(
        None
      )

      withCaptureOfLoggingFrom(controllerLogger) { logs =>
        status(
          viewNotificationController.viewNotification("accountType", "ref", regNo, "NOTIFICATIONID")(request)
        ) mustBe NOT_FOUND

        logs.exists(e =>
          e.getLevel == Level.WARN &&
            e.getFormattedMessage.contains(
              "[ViewNotificationController][viewNotification] - Notification not found - notificationId: NOTIFICATIONID"
            )
        ) mustBe true
      }
    }

    "still succeed and log an error with the status when marking as read fails with an HttpStatusException" in new Fixture {
      val record = notificationRecordGen.sample.map(_.copy(amlsRegistrationNumber = regNo, contactNumber = None))
      when(viewNotificationController.notificationRepository.findById("NOTIFICATIONID")) thenReturn Future.successful(
        record
      )
      when(viewNotificationController.notificationRepository.markAsRead("NOTIFICATIONID")) thenReturn
        Future.failed(HttpStatusException(INTERNAL_SERVER_ERROR, None))

      withCaptureOfLoggingFrom(controllerLogger) { logs =>
        status(
          viewNotificationController.viewNotification("accountType", "ref", regNo, "NOTIFICATIONID")(request)
        ) mustBe OK

        eventually(backgroundPatience, pollInterval) {
          logs.exists(e =>
            e.getLevel == Level.ERROR &&
              e.getFormattedMessage.contains(
                "[ViewNotificationController][markNotificationAsRead] - HttpStatusException marking notification as read - status: 500, notificationId: NOTIFICATIONID"
              )
          ) mustBe true
        }
      }
    }

    "still succeed and log an error with the exception attached when marking as read fails unexpectedly" in new Fixture {
      val record = notificationRecordGen.sample.map(_.copy(amlsRegistrationNumber = regNo, contactNumber = None))
      when(viewNotificationController.notificationRepository.findById("NOTIFICATIONID")) thenReturn Future.successful(
        record
      )
      when(viewNotificationController.notificationRepository.markAsRead("NOTIFICATIONID")) thenReturn
        Future.failed(new RuntimeException("mongo down"))

      withCaptureOfLoggingFrom(controllerLogger) { logs =>
        status(
          viewNotificationController.viewNotification("accountType", "ref", regNo, "NOTIFICATIONID")(request)
        ) mustBe OK

        eventually(backgroundPatience, pollInterval) {
          val event = logs.find(
            _.getFormattedMessage.contains(
              "[ViewNotificationController][markNotificationAsRead] - Unexpected exception marking notification as read"
            )
          )
          event.map(_.getLevel) mustBe Some(Level.ERROR)
          Option(event.get.getThrowableProxy).map(_.getMessage) mustBe Some("mongo down")
        }
      }
    }
  }
}
