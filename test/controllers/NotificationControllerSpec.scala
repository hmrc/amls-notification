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

import config.ApplicationConfig
import connectors.EmailConnector
import exceptions.HttpStatusException
import models.ContactType.{NewRenewalReminder, RenewalReminder}
import models._

import java.time.{Instant, ZoneOffset}
import java.time.{ZoneId, ZonedDateTime}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsNull, JsValue, Json}
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.NotificationMongoRepository
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import ch.qos.logback.classic.Level
import play.api.Logger
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing

import scala.concurrent.{ExecutionContext, Future}

class NotificationControllerSpec
    extends PlaySpec
    with MockitoSugar
    with ScalaFutures
    with GuiceOneAppPerSuite
    with BeforeAndAfter
    with LogCapturing {

  val mockCC                     = app.injector.instanceOf[ControllerComponents]
  val mockEmailConnector         = mock[EmailConnector]
  val mockAuditConnector         = mock[AuditConnector]
  val mockConfig                 = app.injector.instanceOf[ApplicationConfig]
  val mockNotificationRepository = mock[NotificationMongoRepository]
  val mockExecutionContext       = app.injector.instanceOf[ExecutionContext]

  val notificationController =
    new NotificationController(mockEmailConnector, mockConfig, mockAuditConnector, mockCC, mockNotificationRepository)(
      mockExecutionContext
    )

  before {
    reset(notificationController.notificationRepository)
  }

  val body = NotificationPushRequest(
    "AA1234567891234",
    "name",
    "hh@test.com",
    Some(Status(StatusType.DeRegistered, Some(DeregisteredReason.CeasedTrading))),
    Some(ContactType.ApplicationApproval),
    None,
    false
  )

  val json = Json.obj(
    "safeId"         -> "AA1234567891234",
    "name"           -> "test",
    "email"          -> "test@gg.com",
    "status"         -> Json.obj("status_type" -> "06", "status_reason" -> "100"),
    "contact_type"   -> "REJR",
    "contact_number" -> "112345678251212",
    "variation"      -> false
  )

  val postRequest = FakeRequest("POST", "/")
    .withHeaders(CONTENT_TYPE -> "application/json")
    .withBody[JsValue](Json.toJson(body))

  val postRequestWithError = FakeRequest("POST", "/")
    .withHeaders(CONTENT_TYPE -> "application/json")
    .withBody[JsValue](json)

  val getRequest = FakeRequest()
    .withHeaders(CONTENT_TYPE -> "application/json")

  "NotificationController" must {

    val amlsRegistrationNumber = "XAML00000567890"
    val safeId                 = "XA8743294823094"

    "save the input notificationPushRequest into mongo repo successfully" in {
      when {
        notificationController.notificationRepository.insertRecord(any())
      } thenReturn Future.successful(true)

      when {
        notificationController.auditConnector.sendEvent(any())(any(), any())
      } thenReturn Future.successful(mock[AuditResult])

      val result = notificationController.saveNotification(amlsRegistrationNumber)(postRequest)
      status(result) must be(NO_CONTENT)
      contentAsString(result) mustBe ""

      val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      verify(notificationController.auditConnector).sendExtendedEvent(captor.capture())(any(), any())

      captor.getValue.auditType must be("ServiceRequestReceived")
    }

    "fail validation when json parse throws error" in {

      val result = notificationController.saveNotification(amlsRegistrationNumber)(postRequestWithError)
      status(result)        must be(BAD_REQUEST)
      contentAsJson(result) must be(
        Json.obj("errors" -> Json.arr(Json.obj("path" -> "obj.status.status_reason", "error" -> "error.invalid")))
      )
    }

    "return BadRequest, if input request fails validation" in {
      val result  = notificationController.saveNotification("hhhh")(postRequest)
      val failure = Json.obj("errors" -> Seq("Invalid AMLS Registration Number"))

      status(result)        must be(BAD_REQUEST)
      contentAsJson(result) must be(failure)
    }

    "return an invalid response when mongo insertion fails" in {

      when {
        notificationController.notificationRepository.insertRecord(any())
      } thenReturn Future.failed(HttpStatusException(INTERNAL_SERVER_ERROR, Some("message")))

      whenReady(notificationController.saveNotification(amlsRegistrationNumber)(postRequest).failed) {
        case HttpStatusException(status, b) =>
          status mustBe INTERNAL_SERVER_ERROR
          b mustBe Some("message")
      }
    }

    "return a `BadRequest` response when the json fails to parse" in {
      val request = FakeRequest()
        .withHeaders(CONTENT_TYPE -> "application/json")
        .withBody[JsValue](JsNull)

      val response = Json.obj(
        "errors" -> Seq(
          Json.obj(
            "path"  -> "obj.safeId",
            "error" -> "error.path.missing"
          ),
          Json.obj(
            "path"  -> "obj.email",
            "error" -> "error.path.missing"
          ),
          Json.obj(
            "path"  -> "obj.name",
            "error" -> "error.path.missing"
          ),
          Json.obj(
            "path"  -> "obj.variation",
            "error" -> "error.path.missing"
          )
        )
      )
      val result   = notificationController.saveNotification(amlsRegistrationNumber)(request)

      status(result) mustEqual BAD_REQUEST
      contentAsJson(result) mustEqual response
    }

    "return BadRequest, if input request fails validation of mongo fetch" in {
      val result  = notificationController.fetchNotifications("accountType", "ref", "hhhh")(getRequest)
      val failure = Json.obj("errors" -> Seq("Invalid AMLS Registration Number"))

      status(result)        must be(BAD_REQUEST)
      contentAsJson(result) must be(failure)
    }

    "return an invalid response when fetch query fails" in {

      when {
        notificationController.notificationRepository.findByAmlsReference(any())
      } thenReturn Future.failed(new HttpStatusException(INTERNAL_SERVER_ERROR, Some("message")))

      whenReady(
        notificationController.fetchNotifications("accountType", "ref", amlsRegistrationNumber)(getRequest).failed
      ) { case HttpStatusException(status, body) =>
        status mustEqual INTERNAL_SERVER_ERROR
        body mustEqual Some("message")
      }
    }

    "return all the matching notifications form repository" when {

      val notificationRecord = NotificationRow(
        status = Some(Status(StatusType.Revoked, Some(RevokedReason.RevokedCeasedTrading))),
        contactType = Some(ContactType.MindedToRevoke),
        contactNumber = None,
        variation = false,
        receivedAt = ZonedDateTime.now(ZoneOffset.UTC).toInstant,
        isRead = false,
        amlsRegistrationNumber = amlsRegistrationNumber,
        templatePackageVersion = Some("1"),
        _id = new IDType("5832e38e01000001005ca3ff")
      )

      val notificationRows = Seq(notificationRecord)

      val notificationRecordWithoutVersion = NotificationRow(
        status = Some(Status(StatusType.Revoked, Some(RevokedReason.RevokedCeasedTrading))),
        contactType = Some(ContactType.MindedToRevoke),
        contactNumber = None,
        variation = false,
        receivedAt = ZonedDateTime.now(ZoneOffset.UTC).toInstant,
        isRead = false,
        amlsRegistrationNumber = amlsRegistrationNumber,
        templatePackageVersion = None,
        _id = new IDType("5832e38e01000001005ca3ff")
      )

      val notificationRowsWithoutVersion = Seq(notificationRecordWithoutVersion)

      "valid amlsRegistration number is passed and a version number" in {
        when(notificationController.notificationRepository.findByAmlsReference(any()))
          .thenReturn(Future.successful(notificationRows))

        val result = notificationController.fetchNotifications("accountType", "ref", amlsRegistrationNumber)(getRequest)
        status(result)        must be(OK)
        contentAsJson(result) must be(Json.toJson(notificationRows))

        verify(notificationController.notificationRepository).findByAmlsReference(amlsRegistrationNumber)
      }

      "valid amlsRegistration number is passed and no version number" in {
        when(notificationController.notificationRepository.findByAmlsReference(any()))
          .thenReturn(Future.successful(notificationRowsWithoutVersion))

        val result = notificationController.fetchNotifications("accountType", "ref", amlsRegistrationNumber)(getRequest)
        status(result)        must be(OK)
        contentAsJson(result) must be(
          Json.toJson(Seq(notificationRecordWithoutVersion.copy(templatePackageVersion = Some("v1m0"))))
        )

        verify(notificationController.notificationRepository).findByAmlsReference(amlsRegistrationNumber)
      }

      "a valid safeId is passed and a version number" in {
        when {
          notificationController.notificationRepository.findBySafeId(eqTo(safeId))
        } thenReturn Future.successful(notificationRows)

        val result = notificationController.fetchNotificationsBySafeId("accountType", "ref", safeId)(getRequest)
        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(notificationRows)

        verify(notificationController.notificationRepository).findBySafeId(safeId)
      }

      "a valid safeId is passed and no version number" in {
        when {
          mockNotificationRepository.findBySafeId(eqTo(safeId))
        } thenReturn Future.successful(notificationRowsWithoutVersion)

        val result = notificationController.fetchNotificationsBySafeId("accountType", "ref", safeId)(getRequest)
        status(result) mustBe OK

        contentAsJson(result) mustBe Json.toJson(
          Seq(notificationRecordWithoutVersion.copy(templatePackageVersion = Some("v1m0")))
        )
        verify(mockNotificationRepository).findBySafeId(safeId)
      }

    }

    "return correct Content Type" when {

      "contact Type is RenewalReminder and currentTemplatePackageVersion in config is v7m0" when {
        "28 days " in {
          val result = notificationController.getContactType(
            Some(RenewalReminder),
            ZonedDateTime.ofInstant(Instant.parse("2022-07-03T10:49:17.727Z"), ZoneId.of("UTC")),
            "v7m0"
          )
          result must be(Some(RenewalReminder))
        }

        "14 days" in {
          val result = notificationController.getContactType(
            Some(RenewalReminder),
            ZonedDateTime.ofInstant(Instant.parse("2022-07-16T10:49:17.727Z"), ZoneId.of("UTC")),
            "v7m0"
          )
          result must be(Some(NewRenewalReminder))
        }

        "7 days" in {
          val result = notificationController.getContactType(
            Some(RenewalReminder),
            ZonedDateTime.ofInstant(Instant.parse("2022-07-28T10:49:17.727Z"), ZoneId.of("UTC")),
            "v7m0"
          )
          result must be(Some(NewRenewalReminder))
        }

      }

      "contact Type is RenewalReminder and currentTemplatePackageVersion in config is v4m0" when {
        "14 days" in {
          val result = notificationController.getContactType(
            Some(RenewalReminder),
            ZonedDateTime.ofInstant(Instant.parse("2022-07-16T10:49:17.727Z"), ZoneId.of("UTC")),
            "v4m0"
          )
          result must be(Some(RenewalReminder))
        }

      }

      "contact Type is not renewal Reminder but new template needed" in {
        List(ContactType.AutoExpiryOfRegistration, ContactType.ReminderToPayForRenewal).foreach { cTYpe =>
          withClue(s"For Contact Type [$cTYpe]") {
            val result = notificationController
              .getContactType(
                Some(cTYpe),
                ZonedDateTime.ofInstant(Instant.parse("2022-07-22T10:49:17.727Z"), ZoneId.of("UTC")),
                "v7m0"
              )

            result must be(Some(cTYpe))
          }
        }
      }

      "contact Type is not renewal Reminder but old template needed" in {
        List(
          ContactType.RejectionReasons,
          ContactType.RevocationReasons,
          ContactType.MindedToReject,
          ContactType.NoLongerMindedToReject,
          ContactType.MindedToRevoke,
          ContactType.NoLongerMindedToRevoke,
          ContactType.Others,
          ContactType.ApplicationApproval,
          ContactType.RenewalApproval,
          ContactType.ReminderToPayForApplication,
          ContactType.ReminderToPayForVariation,
          ContactType.ReminderToPayForManualCharges
        ).foreach { cTYpe =>
          withClue(s"For Contact Type [$cTYpe]") {
            val result = notificationController
              .getContactType(
                Some(cTYpe),
                ZonedDateTime.ofInstant(Instant.parse("2022-07-22T10:49:17.727Z"), ZoneId.of("UTC")),
                "v7m0"
              )
            result must be(Some(cTYpe))
          }
        }
      }

    }

    "return a bad request" when {
      "an invalid safeId is passed" in {
        val result =
          notificationController.fetchNotificationsBySafeId("accountType", "ref", "an invalid safe ID")(getRequest)

        status(result) mustBe BAD_REQUEST
      }
    }
  }

  private val controllerLogger: Logger = Logger(classOf[NotificationController])
  private val validRegNo               = "XAML00000567890"
  private val validSafeId              = "XA8743294823094"

  "NotificationController logging" must {

    "return 500 and log an error when the notification insert is not acknowledged" in {
      when(notificationController.notificationRepository.insertRecord(any())) thenReturn Future.successful(false)

      withCaptureOfLoggingFrom(controllerLogger) { logs =>
        status(notificationController.saveNotification(validRegNo)(postRequest)) mustBe INTERNAL_SERVER_ERROR

        val event = logs.find(
          _.getFormattedMessage.contains(
            "[NotificationController][saveNotification] - Notification insert not acknowledged"
          )
        )
        event.map(_.getLevel) mustBe Some(Level.ERROR)
        event.get.getFormattedMessage must include(validRegNo)
      }
    }

    "log a warning with the status when saving fails with an HttpStatusException" in {
      when(notificationController.notificationRepository.insertRecord(any())) thenReturn
        Future.failed(HttpStatusException(INTERNAL_SERVER_ERROR, Some("message")))

      withCaptureOfLoggingFrom(controllerLogger) { logs =>
        notificationController
          .saveNotification(validRegNo)(postRequest)
          .failed
          .futureValue mustBe a[HttpStatusException]

        logs.exists(e =>
          e.getLevel == Level.WARN &&
            e.getFormattedMessage.contains(
              "[NotificationController][saveNotification] - HttpStatusException saving notification - status: 500"
            )
        ) mustBe true
      }
    }

    "log a warning with the exception attached and rethrow when saving fails unexpectedly" in {
      val boom = new RuntimeException("mongo down")
      when(notificationController.notificationRepository.insertRecord(any())) thenReturn Future.failed(boom)

      withCaptureOfLoggingFrom(controllerLogger) { logs =>
        notificationController.saveNotification(validRegNo)(postRequest).failed.futureValue mustBe boom

        val event = logs.find(
          _.getFormattedMessage.contains(
            "[NotificationController][saveNotification] - Unexpected exception saving notification"
          )
        )
        event.map(_.getLevel) mustBe Some(Level.WARN)
        Option(event.get.getThrowableProxy).map(_.getMessage) mustBe Some("mongo down")
      }
    }

    "log the invalid json paths, and no email address at warn level, when the payload is invalid" in {
      val invalidRequest = FakeRequest("POST", "/")
        .withHeaders(CONTENT_TYPE -> "application/json")
        .withBody[JsValue](Json.obj("email" -> "secret@test.com"))

      withCaptureOfLoggingFrom(controllerLogger) { logs =>
        status(notificationController.saveNotification(validRegNo)(invalidRequest)) mustBe BAD_REQUEST

        val event = logs.find(
          _.getFormattedMessage.contains("[NotificationController][saveNotification] - Invalid notification payload")
        )
        event.map(_.getLevel) mustBe Some(Level.WARN)
        event.get.getFormattedMessage must include("invalid paths:")
        logs
          .filter(_.getLevel.isGreaterOrEqual(Level.INFO))
          .exists(_.getFormattedMessage.contains("secret@test.com")) mustBe false
      }
    }

    "log a warning when saving with an invalid AMLS registration number" in {
      withCaptureOfLoggingFrom(controllerLogger) { logs =>
        status(notificationController.saveNotification("invalid")(postRequest)) mustBe BAD_REQUEST

        logs.exists(e =>
          e.getLevel == Level.WARN &&
            e.getFormattedMessage.contains(
              "[NotificationController][saveNotification] - Invalid AMLS registration number format"
            )
        ) mustBe true
      }
    }

    "log a warning when fetching with an invalid AMLS registration number" in {
      withCaptureOfLoggingFrom(controllerLogger) { logs =>
        status(
          notificationController.fetchNotifications("accountType", "ref", "invalid")(getRequest)
        ) mustBe BAD_REQUEST

        logs.exists(e =>
          e.getLevel == Level.WARN &&
            e.getFormattedMessage.contains(
              "[NotificationController][fetchNotifications] - Invalid AMLS registration number format"
            )
        ) mustBe true
      }
    }

    "log a warning with the exception attached and rethrow when fetching fails unexpectedly" in {
      val boom = new RuntimeException("mongo down")
      when(notificationController.notificationRepository.findByAmlsReference(any())) thenReturn Future.failed(boom)

      withCaptureOfLoggingFrom(controllerLogger) { logs =>
        notificationController
          .fetchNotifications("accountType", "ref", validRegNo)(getRequest)
          .failed
          .futureValue mustBe boom

        val event = logs.find(
          _.getFormattedMessage.contains(
            "[NotificationController][fetchNotifications] - Unexpected exception fetching notifications"
          )
        )
        event.map(_.getLevel) mustBe Some(Level.WARN)
        Option(event.get.getThrowableProxy).map(_.getMessage) mustBe Some("mongo down")
      }
    }

    "log a warning when fetching with an invalid safeId" in {
      withCaptureOfLoggingFrom(controllerLogger) { logs =>
        status(
          notificationController.fetchNotificationsBySafeId("accountType", "ref", "invalid")(getRequest)
        ) mustBe BAD_REQUEST

        logs.exists(e =>
          e.getLevel == Level.WARN &&
            e.getFormattedMessage.contains(
              "[NotificationController][fetchNotificationsBySafeId] - Invalid SafeID format"
            )
        ) mustBe true
      }
    }

    "log a warning with the exception attached and rethrow when fetching by safeId fails unexpectedly" in {
      val boom = new RuntimeException("mongo down")
      when(notificationController.notificationRepository.findBySafeId(any())) thenReturn Future.failed(boom)

      withCaptureOfLoggingFrom(controllerLogger) { logs =>
        notificationController
          .fetchNotificationsBySafeId("accountType", "ref", validSafeId)(getRequest)
          .failed
          .futureValue mustBe boom

        val event = logs.find(
          _.getFormattedMessage.contains(
            "[NotificationController][fetchNotificationsBySafeId] - Unexpected exception fetching notifications"
          )
        )
        event.map(_.getLevel) mustBe Some(Level.WARN)
        Option(event.get.getThrowableProxy).map(_.getMessage) mustBe Some("mongo down")
      }
    }
  }
}
