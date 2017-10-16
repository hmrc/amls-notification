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

import connectors.EmailConnector
import exceptions.HttpStatusException
import models._
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.{JsNull, JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.NotificationRepository
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.concurrent.Future

class NotificationControllerSpec extends PlaySpec with MockitoSugar with ScalaFutures with OneServerPerSuite with BeforeAndAfter {

  object TestNotificationController extends NotificationController {
    override private[controllers] val notificationRepository = mock[NotificationRepository]
    override private[controllers] val emailConnector = mock[EmailConnector]
    override private[controllers] val audit = mock[AuditConnector]
  }

  before {
    reset(TestNotificationController.notificationRepository)
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

      when(TestNotificationController.notificationRepository.insertRecord(any())).thenReturn(Future.successful(true))

      when {
        TestNotificationController.audit.sendEvent(any())(any(), any())
      } thenReturn Future.successful(mock[AuditResult])

      val result = TestNotificationController.saveNotification(amlsRegistrationNumber)(postRequest)
      status(result) must be(NO_CONTENT)
      contentAsString(result) mustBe ""

      val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      verify(TestNotificationController.audit).sendExtendedEvent(captor.capture())(any(), any())

      captor.getValue.auditType mustBe "notificationReceived"
    }

    "fail validation when json parse throws error" in {

      val result = TestNotificationController.saveNotification(amlsRegistrationNumber)(postRequestWithError)
      status(result) must be(BAD_REQUEST)
      contentAsJson(result) must be(Json.obj("errors" -> Json.arr(Json.obj("path" -> "obj.status.status_reason", "error" -> "error.invalid"))))
    }

    "return BadRequest, if input request fails validation" in {
      val result = TestNotificationController.saveNotification("hhhh")(postRequest)
      val failure = Json.obj("errors" -> Seq("Invalid AMLS Registration Number"))

      status(result) must be(BAD_REQUEST)
      contentAsJson(result) must be(failure)

    }

    "return an invalid response when mongo insertion fails" in {

      when {
        TestNotificationController.notificationRepository.insertRecord(any())
      } thenReturn Future.failed(new HttpStatusException(INTERNAL_SERVER_ERROR, Some("message")))

      whenReady(TestNotificationController.saveNotification(amlsRegistrationNumber)(postRequest).failed) {
        case HttpStatusException(status, body) =>
          status mustBe INTERNAL_SERVER_ERROR
          body mustBe Some("message")
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
      val result = TestNotificationController.saveNotification(amlsRegistrationNumber)(request)

      status(result) mustEqual BAD_REQUEST
      contentAsJson(result) mustEqual response
    }

    "return BadRequest, if input request fails validation of mongo fetch" in {
      val result = TestNotificationController.fetchNotifications("accountType", "ref", "hhhh")(getRequest)
      val failure = Json.obj("errors" -> Seq("Invalid AMLS Registration Number"))

      status(result) must be(BAD_REQUEST)
      contentAsJson(result) must be(failure)

    }


    "return an invalid response when fetch query fails" in {

      when {
        TestNotificationController.notificationRepository.findByAmlsReference(any())
      } thenReturn Future.failed(new HttpStatusException(INTERNAL_SERVER_ERROR, Some("message")))

      whenReady(TestNotificationController.fetchNotifications("accountType", "ref", amlsRegistrationNumber)(getRequest).failed) {
        case HttpStatusException(status, body) =>
          status mustEqual INTERNAL_SERVER_ERROR
          body mustEqual Some("message")
      }
    }

    "return all the matching notifications form repository" when {

      val notificationRecord = NotificationRow(
        Some(Status(StatusType.Revoked, Some(RevokedReason.RevokedCeasedTrading))),
        Some(ContactType.MindedToRevoke),
        None,
        false,
        DateTime.now(DateTimeZone.UTC),
        false,
        amlsRegistrationNumber,
        new IDType("5832e38e01000001005ca3ff"))

      val notificationRows = Seq(notificationRecord)

      "valid amlsRegistration number is passed" in {
        when(TestNotificationController.notificationRepository.findByAmlsReference(any())).thenReturn(Future.successful(notificationRows))

        val result = TestNotificationController.fetchNotifications("accountType", "ref", amlsRegistrationNumber)(getRequest)
        status(result) must be(OK)
        contentAsJson(result) must be(Json.toJson(notificationRows))

        verify(TestNotificationController.notificationRepository).findByAmlsReference(amlsRegistrationNumber)
      }

      "a valid safeId is passed" in {
        when {
          TestNotificationController.notificationRepository.findBySafeId(eqTo(safeId))
        } thenReturn Future.successful(notificationRows)

        val result = TestNotificationController.fetchNotificationsBySafeId("accountType", "ref", safeId)(getRequest)
        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(notificationRows)

        verify(TestNotificationController.notificationRepository).findBySafeId(safeId)
      }

    }

    "return a bad request" when {
      "an invalid safeId is passed" in {
        val result = TestNotificationController.fetchNotificationsBySafeId("accountType", "ref", "an invalid safe ID")(getRequest)

        status(result) mustBe BAD_REQUEST
      }
    }
  }
}
