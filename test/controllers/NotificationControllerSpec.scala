/*
 * Copyright 2019 HM Revenue & Customs
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
import models._
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsNull, JsValue, Json}
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import reactivemongo.api.commands.{WriteError, WriteResult}
import repositories.NotificationMongoRepository
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import utils.SuccessfulAuthAction
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class NotificationControllerSpec extends UnitSpec with MockitoSugar with ScalaFutures with GuiceOneAppPerSuite with BeforeAndAfter {

  val mockCC: ControllerComponents = app.injector.instanceOf[ControllerComponents]
  val mockEmailConnector: EmailConnector = mock[EmailConnector]
  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val mockConfig = app.injector.instanceOf[ApplicationConfig]
  val mockNotificationRepository = mock[NotificationMongoRepository]

  val notificationController = new NotificationController(mockEmailConnector, mockConfig, mockAuditConnector, mockCC, mockNotificationRepository)

  before {
    reset(notificationController.notificationRepository)
  }

  val body = NotificationPushRequest("AA1234567891234", "name", "hh@test.com",
    Some(Status(StatusType.DeRegistered, Some(DeregisteredReason.CeasedTrading))), Some(ContactType.ApplicationApproval), None, false)

  val json = Json.obj("safeId" -> "AA1234567891234",
    "name" -> "test",
    "email" -> "test@gg.com",
    "status" -> Json.obj("status_type" -> "06",
      "status_reason" -> "100"),
    "contact_type" -> "REJR",
    "contact_number" -> "112345678251212",
    "variation" -> false)

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
    val safeId = "XA8743294823094"

    "save the input notificationPushRequest into mongo repo successfully" in {

      val writeResult = mock[WriteResult]
      when(writeResult.ok) thenReturn true

      when {
        notificationController.notificationRepository.insertRecord(any())
      } thenReturn Future.successful(writeResult)

      when {
        notificationController.audit.sendEvent(any())(any(), any())
      } thenReturn Future.successful(mock[AuditResult])

      val result = notificationController.saveNotification(amlsRegistrationNumber)(postRequest)
      status(result) should be(NO_CONTENT)
      contentAsString(result) shouldBe ""

      val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      verify(notificationController.audit).sendExtendedEvent(captor.capture())(any(), any())

      captor.getValue.auditType shouldBe "ServiceRequestReceived"
    }

    "fail validation when json parse throws error" in {

      val result = notificationController.saveNotification(amlsRegistrationNumber)(postRequestWithError)
      status(result) should be(BAD_REQUEST)
      contentAsJson(result) should be(Json.obj("errors" -> Json.arr(Json.obj("path" -> "obj.status.status_reason", "error" -> "error.invalid"))))
    }

    "return BadRequest, if input request fails validation" in {
      val result = notificationController.saveNotification("hhhh")(postRequest)
      val failure = Json.obj("errors" -> Seq("Invalid AMLS Registration Number"))

      status(result) should be(BAD_REQUEST)
      contentAsJson(result) should be(failure)
    }

    "return an invalid response when mongo insertion fails" in {

      when {
        notificationController.notificationRepository.insertRecord(any())
      } thenReturn Future.failed(HttpStatusException(INTERNAL_SERVER_ERROR, Some("message")))

      whenReady(notificationController.saveNotification(amlsRegistrationNumber)(postRequest).failed) {
        case HttpStatusException(status, b) =>
          status shouldBe INTERNAL_SERVER_ERROR
          b shouldBe Some("message")
      }
    }

    "return an invalid response when mongo returns bad write result" in {
      val writeResult = mock[WriteResult]
      when(writeResult.ok) thenReturn false
      when(writeResult.writeErrors) thenReturn Seq(WriteError(0, 1, "Some error"))

      when {
        notificationController.notificationRepository.insertRecord(any())
      } thenReturn Future.successful(writeResult)

      whenReady(notificationController.saveNotification(amlsRegistrationNumber)(postRequest)) { r =>
        r.header.status shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "return a `BadRequest` response when the json fails to parse" in {
      val request = FakeRequest()
        .withHeaders(CONTENT_TYPE -> "application/json")
        .withBody[JsValue](JsNull)

      val response = Json.obj(
        "errors" -> Seq(
          Json.obj(
            "path" -> "obj.safeId",
            "error" -> "error.path.missing"
          ),
          Json.obj(
            "path" -> "obj.variation",
            "error" -> "error.path.missing"
          ),
          Json.obj(
            "path" -> "obj.email",
            "error" -> "error.path.missing"
          ),
          Json.obj(
            "path" -> "obj.name",
            "error" -> "error.path.missing"
          )
        )
      )
      val result = notificationController.saveNotification(amlsRegistrationNumber)(request)

      status(result) shouldEqual BAD_REQUEST
      contentAsJson(result) shouldEqual response
    }

    "return BadRequest, if input request fails validation of mongo fetch" in {
      val result = notificationController.fetchNotifications("accountType", "ref", "hhhh")(getRequest)
      val failure = Json.obj("errors" -> Seq("Invalid AMLS Registration Number"))

      status(result) should be(BAD_REQUEST)
      contentAsJson(result) should be(failure)
    }


    "return an invalid response when fetch query fails" in {

      when {
        notificationController.notificationRepository.findByAmlsReference(any())
      } thenReturn Future.failed(new HttpStatusException(INTERNAL_SERVER_ERROR, Some("message")))

      whenReady(notificationController.fetchNotifications("accountType", "ref", amlsRegistrationNumber)(getRequest).failed) {
        case HttpStatusException(status, body) =>
          status shouldEqual INTERNAL_SERVER_ERROR
          body shouldEqual Some("message")
      }
    }

    "return all the matching notifications form repository" when {

      val notificationRecord = NotificationRow(
        status = Some(Status(StatusType.Revoked, Some(RevokedReason.RevokedCeasedTrading))),
        contactType = Some(ContactType.MindedToRevoke),
        contactNumber = None,
        variation = false,
        receivedAt = DateTime.now(DateTimeZone.UTC),
        isRead = false,
        amlsRegistrationNumber = amlsRegistrationNumber,
        templatePackageVersion = Some("1"),
        _id = new IDType("5832e38e01000001005ca3ff"))

      val notificationRows = Seq(notificationRecord)

      val notificationRecordWithoutVersion = NotificationRow(
        status = Some(Status(StatusType.Revoked, Some(RevokedReason.RevokedCeasedTrading))),
        contactType = Some(ContactType.MindedToRevoke),
        contactNumber = None,
        variation = false,
        receivedAt = DateTime.now(DateTimeZone.UTC),
        isRead = false,
        amlsRegistrationNumber = amlsRegistrationNumber,
        templatePackageVersion = None,
        _id = new IDType("5832e38e01000001005ca3ff"))

      val notificationRowsWithoutVersion = Seq(notificationRecordWithoutVersion)

      "valid amlsRegistration number is passed and a version number" in {
        when(notificationController.notificationRepository.findByAmlsReference(any())).thenReturn(Future.successful(notificationRows))

        val result = notificationController.fetchNotifications("accountType", "ref", amlsRegistrationNumber)(getRequest)
        status(result) should be(OK)
        contentAsJson(result) should be(Json.toJson(notificationRows))

        verify(notificationController.notificationRepository).findByAmlsReference(amlsRegistrationNumber)
      }

      "valid amlsRegistration number is passed and no version number" in {
        when(notificationController.notificationRepository.findByAmlsReference(any())).thenReturn(Future.successful(notificationRowsWithoutVersion))

        val result = notificationController.fetchNotifications("accountType", "ref", amlsRegistrationNumber)(getRequest)
        status(result) should be(OK)
        contentAsJson(result) should be(Json.toJson(Seq(notificationRecordWithoutVersion.copy(templatePackageVersion = Some("v1m0")))))

        verify(notificationController.notificationRepository).findByAmlsReference(amlsRegistrationNumber)
      }

      "a valid safeId is passed and a version number" in {
        when {
          notificationController.notificationRepository.findBySafeId(eqTo(safeId))
        } thenReturn Future.successful(notificationRows)

        val result = notificationController.fetchNotificationsBySafeId("accountType", "ref", safeId)(getRequest)
        status(result) shouldBe OK
        contentAsJson(result) shouldBe Json.toJson(notificationRows)

        verify(notificationController.notificationRepository).findBySafeId(safeId)
      }

      "a valid safeId is passed and no version number" in {
        when {
          mockNotificationRepository.findBySafeId(eqTo(safeId))
        } thenReturn Future.successful(notificationRowsWithoutVersion)

        val result = notificationController.fetchNotificationsBySafeId("accountType", "ref", safeId)(getRequest)
        status(result) shouldBe OK

        contentAsJson(result) shouldBe Json.toJson(Seq(notificationRecordWithoutVersion.copy(templatePackageVersion = Some("v1m0"))))
        verify(mockNotificationRepository).findBySafeId(safeId)
      }

    }

    "return a bad request" when {
      "an invalid safeId is passed" in {
        val result = notificationController.fetchNotificationsBySafeId("accountType", "ref", "an invalid safe ID")(getRequest)

        status(result) shouldBe BAD_REQUEST
      }
    }
  }
}
