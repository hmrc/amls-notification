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

package connectors

import audit.MockAudit
import com.codahale.metrics.Timer
import config.ApplicationConfig
import exceptions.HttpStatusException
import metrics.{API11, Metrics}
import models.des.NotificationResponse
import org.joda.time.{DateTimeUtils, LocalDateTime}
import org.mockito.ArgumentMatchers.{any, argThat, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ViewNotificationConnectorSpec
    extends PlaySpec
    with MockitoSugar
    with ScalaFutures
    with IntegrationPatience
    with GuiceOneAppPerSuite
    with BeforeAndAfterAll {

  override def beforeAll(): Unit =
    DateTimeUtils.setCurrentMillisFixed(1000)

  override def afterAll(): Unit =
    DateTimeUtils.setCurrentMillisSystem()

  trait Fixture {

    val mockAppConfig      = mock[ApplicationConfig]
    val mockHttpClientV2   = mock[HttpClientV2]
    val mockRequestBuilder = mock[RequestBuilder]
    val mockAuditConnector = mock[AuditConnector]
    val mockMetrics        = mock[Metrics]

    when(mockAppConfig.desUrl).thenReturn("http://localhost:8080")
    when(mockAppConfig.desToken).thenReturn("test-token")
    when(mockAppConfig.desEnv).thenReturn("test-env")

    val viewNotificationConnector =
      new ViewNotificationConnector(mockAppConfig, mockHttpClientV2, mockAuditConnector, mockMetrics) {
        override private[connectors] val audit = mock[MockAudit]
      }

    val successModel = NotificationResponse(LocalDateTime.now(), "Approved")

    val mockTimer = mock[Timer.Context]

    val amlsRegistrationNumber = "test"
    val contactNumber          = "contactNumber"

    val url                  =
      s"${viewNotificationConnector.baseUrl}/anti-money-laundering/secure-comms/reg-number/$amlsRegistrationNumber/contact-number/$contactNumber"
    var dataEvent: DataEvent = null

    when {
      viewNotificationConnector.metrics.timer(eqTo(API11))
    } thenReturn mockTimer
  }

  "ViewNotificationConnector" must {

    "return a successful future containing the Notification response" in new Fixture {

      val response = HttpResponse(
        status = OK,
        json = Json.toJson(successModel),
        headers = Map.empty
      )

      when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any(): _*)).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any())).thenReturn(Future.successful(response))

      whenReady(viewNotificationConnector.getNotification(amlsRegistrationNumber, contactNumber)) {
        _ mustEqual successModel
      }

      verify(viewNotificationConnector.audit).sendDataEvent(argThat { dataEvent: DataEvent =>
        dataEvent.auditSource.equals("amls-notification") &&
        dataEvent.auditType.equals("OutboundCall")
      })(any())
    }

    "return a failed future when the response contains a BAD_REQUEST and no response body" in new Fixture {

      val response = HttpResponse(
        status = BAD_REQUEST,
        headers = Map.empty,
        body = null
      )

      when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any(): _*)).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any())).thenReturn(Future.successful(response))

      whenReady(viewNotificationConnector.getNotification(amlsRegistrationNumber, contactNumber).failed) {
        case HttpStatusException(status, body) =>
          status mustEqual BAD_REQUEST
          body mustEqual None
      }

      verify(viewNotificationConnector.audit, times(2)).sendDataEvent(argThat { dataEvent: DataEvent =>
        dataEvent.auditSource.equals("amls-notification") &&
        dataEvent.auditType.equals("viewNotificationEventFailed")
      })(any())
    }

    "return a failed future containing json validation message" in new Fixture {

      val response = HttpResponse(
        status = OK,
        headers = Map.empty,
        body = "{\"message\": \"none\"}"
      )

      when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any(): _*)).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any())).thenReturn(Future.successful(response))

      whenReady(viewNotificationConnector.getNotification(amlsRegistrationNumber, contactNumber).failed) {
        case HttpStatusException(status, body) =>
          status mustEqual OK
          body mustEqual Some("{\"message\": \"none\"}")
      }

      verify(viewNotificationConnector.audit, times(2)).sendDataEvent(argThat { dataEvent: DataEvent =>
        dataEvent.auditSource.equals("amls-notification") &&
        dataEvent.auditType.equals("viewNotificationEventFailed")
      })(any())
    }

    "return a failed future containing an exception message" in new Fixture {

      when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any(): _*)).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any())).thenReturn(Future.failed(new Exception("message")))

      whenReady(viewNotificationConnector.getNotification(amlsRegistrationNumber, contactNumber).failed) {
        case HttpStatusException(status, body) =>
          status mustEqual INTERNAL_SERVER_ERROR
          body mustEqual Some("message")
      }
    }
  }
}
