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

package connectors

import audit.MockAudit
import com.codahale.metrics.Timer
import exceptions.HttpStatusException
import metrics.{API11, Metrics}
import org.joda.time.{DateTimeUtils, LocalDateTime}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.play.http.{HttpResponse, HttpGet, HttpPost}
import models.des.NotificationResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ViewNotificationConnectorSpec
  extends PlaySpec
    with MockitoSugar
    with ScalaFutures
    with IntegrationPatience with OneServerPerSuite with BeforeAndAfterAll {

  override def beforeAll {
    DateTimeUtils.setCurrentMillisFixed(1000)
  }

  override def afterAll: Unit = {
    DateTimeUtils.setCurrentMillisSystem()
  }

  trait Fixture {

    object testConnector extends ViewNotificationConnector {
      override private[connectors] val baseUrl: String = "baseUrl"
      override private[connectors] val token: String = "token"
      override private[connectors] val env: String = "ist0"
      override private[connectors] val httpGet: HttpGet = mock[HttpGet]
      override private[connectors] val httpPost: HttpPost = mock[HttpPost]
      override private[connectors] val metrics: Metrics = mock[Metrics]
      override private[connectors] val audit = MockAudit
      override private[connectors] val fullUrl: String = s"$baseUrl/$requestUrl"
    }

    val successModel = NotificationResponse(LocalDateTime.now(), "Approved")

    val mockTimer = mock[Timer.Context]

    val amlsRegistrationNumber = "test"
    val contactNumber = "contactNumber"

    val url = s"${testConnector.baseUrl}/anti-money-laundering/secure-comms/reg-number/$amlsRegistrationNumber/contact-number/$contactNumber"

    when {
      testConnector.metrics.timer(eqTo(API11))
    } thenReturn mockTimer
  }

  "ViewNotificationConnector" must {

    "return a succesful future containing the Notification response" in new Fixture {

      val response = HttpResponse(
        responseStatus = OK,
        responseHeaders = Map.empty,
        responseJson = Some(Json.toJson(successModel))
      )

      when {
        testConnector.httpGet.GET[HttpResponse](eqTo(url))(any(), any())
      } thenReturn Future.successful(response)

      whenReady(testConnector.getNotification(amlsRegistrationNumber, contactNumber)) {
        _ mustEqual successModel
      }
    }

    "return a failed future when the response contains a BAD_REQUEST and no response body" in new Fixture {

      val response = HttpResponse(
        responseStatus = BAD_REQUEST,
        responseHeaders = Map.empty
      )
      when {
        testConnector.httpGet.GET[HttpResponse](eqTo(url))(any(), any())
      } thenReturn Future.successful(response)

      whenReady(testConnector.getNotification(amlsRegistrationNumber, contactNumber).failed) {
        case HttpStatusException(status, body) =>
          status mustEqual BAD_REQUEST
          body mustEqual None
      }
    }

    "return a failed future containing json validation message" in new Fixture {

      val response = HttpResponse(
        responseStatus = OK,
        responseHeaders = Map.empty,
        responseString = Some("message")
      )

      when {
        testConnector.httpGet.GET[HttpResponse](any())(any(), any())
      } thenReturn Future.successful(response)

      whenReady(testConnector.getNotification(amlsRegistrationNumber, contactNumber).failed) {
        case HttpStatusException(status, body) =>
          status mustEqual OK
          body mustEqual Some("message")
      }
    }

    "return a failed future containing an exception message" in new Fixture {

      when {
        testConnector.httpGet.GET[HttpResponse](eqTo(url))(any(), any())
      } thenReturn Future.failed(new Exception("message"))

      whenReady(testConnector.getNotification(amlsRegistrationNumber, contactNumber).failed) {
        case HttpStatusException(status, body) =>
          status mustEqual INTERNAL_SERVER_ERROR
          body mustEqual Some("message")
      }
    }
  }

}
