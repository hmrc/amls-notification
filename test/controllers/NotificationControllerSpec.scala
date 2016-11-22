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

import exceptions.HttpStatusException
import models._
import org.joda.time.{DateTimeZone, DateTime}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.{JsNull, JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import org.mockito.Mockito._
import org.mockito.Matchers.{eq => eqTo, _}
import reactivemongo.bson.BSONObjectID
import repositories.NotificationRepository

import scala.concurrent.Future

class NotificationControllerSpec extends PlaySpec with MockitoSugar with ScalaFutures with OneServerPerSuite {

  object TestNotificationController extends NotificationController {
    override private[controllers] val notificationRepository = mock[NotificationRepository]
  }

  val body = NotificationPushRequest("name", "hh@test.com",
    Some(Status(Some(StatusType.Rejected), Some(RejectedReason.FailedToPayCharges))), Some(ContactType.ApplicationApproval), None, false)
  val postRequest = FakeRequest("POST", "/")
    .withHeaders(CONTENT_TYPE -> "application/json")
    .withBody[JsValue](Json.toJson(body))

  val getRequest = FakeRequest()
    .withHeaders(CONTENT_TYPE -> "application/json")


  "NotificationController" must {

    val amlsRegistrationNumber = "XBML00000567890"

    "save the input notificationPushRequest into mongo repo successfully" in {

      when(TestNotificationController.notificationRepository.insertRecord(any())).thenReturn(Future.successful(true))

      val result = TestNotificationController.saveNotification(amlsRegistrationNumber)(postRequest)
      status(result) must be(OK)
      contentAsJson(result) must be(Json.toJson(true))
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
          status mustEqual INTERNAL_SERVER_ERROR
          body mustEqual Some("message")
      }
    }

    "return a `BadRequest` response when the json fails to parse" in {
      val request = FakeRequest()
        .withHeaders(CONTENT_TYPE -> "application/json")
        .withBody[JsValue](JsNull)

      val response = Json.obj(
        "errors" -> Seq(
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
      val result = TestNotificationController.fetchNotifications("hhhh")(getRequest)
      val failure = Json.obj("errors" -> Seq("Invalid AMLS Registration Number"))

      status(result) must be(BAD_REQUEST)
      contentAsJson(result) must be(failure)

    }


    "return an invalid response when fetch query fails" in {

      when {
        TestNotificationController.notificationRepository.findByAmlsReference(any())
      } thenReturn Future.failed(new HttpStatusException(INTERNAL_SERVER_ERROR, Some("message")))

      whenReady(TestNotificationController.fetchNotifications(amlsRegistrationNumber)(getRequest).failed) {
        case HttpStatusException(status, body) =>
          status mustEqual INTERNAL_SERVER_ERROR
          body mustEqual Some("message")
      }
    }

    "return all the matching notifications form repository" when {
      "valid amlsRegistration number is passed" in {

        val id = BSONObjectID.generate
        val notificationRecord = NotificationRow (
          Some(Status(Some(StatusType.Revoked),
            Some(RevokedReason.RevokedCeasedTrading))),
          Some(ContactType.MindedToRevoke), None, false, DateTime.now(DateTimeZone.UTC), new IDType("5832e38e01000001005ca3ff"))

        when(TestNotificationController.notificationRepository.findByAmlsReference(any())).thenReturn(Future.successful(Seq(notificationRecord)))

        val result = TestNotificationController.fetchNotifications(amlsRegistrationNumber)(getRequest)
        status(result) must be(OK)
        contentAsJson(result) must be(Json.toJson(Seq(notificationRecord)))
      }
    }
  }
}
