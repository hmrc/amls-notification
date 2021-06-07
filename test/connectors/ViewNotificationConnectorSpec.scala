/*
 * Copyright 2021 HM Revenue & Customs
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
import org.mockito.ArgumentMatchers.{any, eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ViewNotificationConnectorSpec extends PlaySpec with MockitoSugar with ScalaFutures
                                    with IntegrationPatience with GuiceOneAppPerSuite with BeforeAndAfterAll {

  override def beforeAll {
    DateTimeUtils.setCurrentMillisFixed(1000)
  }

  override def afterAll: Unit = {
    DateTimeUtils.setCurrentMillisSystem()
  }

  trait Fixture {

    val mockAppConfig = mock[ApplicationConfig]
    val mockHttpClient = mock[HttpClient]
    val mockAuditConnector = mock[AuditConnector]
    val mockMetrics = mock[Metrics]

    val viewNotificationConnector = new ViewNotificationConnector(mockAppConfig, mockHttpClient, mockAuditConnector, mockMetrics) {
      override private[connectors] val audit = mock[MockAudit]
    }

    val successModel = NotificationResponse(LocalDateTime.now(), "Approved")

    val mockTimer = mock[Timer.Context]

    val amlsRegistrationNumber = "test"
    val contactNumber = "contactNumber"

    val url = s"${viewNotificationConnector.baseUrl}/anti-money-laundering/secure-comms/reg-number/$amlsRegistrationNumber/contact-number/$contactNumber"
    var dataEvent: DataEvent = null

    when {
      viewNotificationConnector.metrics.timer(eqTo(API11))
    } thenReturn mockTimer
  }

  "ViewNotificationConnector" must {

    "return a successful future containing the Notification response" in new Fixture {

      val response = HttpResponse(
        responseStatus = OK,
        responseHeaders = Map.empty,
        responseJson = Some(Json.toJson(successModel))
      )

      when {
        viewNotificationConnector.audit.sendDataEvent
      } thenReturn ((f: DataEvent) => dataEvent = f)

      when {
        viewNotificationConnector.http.GET[HttpResponse](any(), any(), any())(any(), any(), any())
      } thenReturn Future.successful(response)

      whenReady(viewNotificationConnector.getNotification(amlsRegistrationNumber, contactNumber)) {
        _ mustEqual successModel
      }

      dataEvent.auditSource mustEqual "amls-notification"
      dataEvent.auditType mustEqual "OutboundCall"
    }

    "return a failed future when the response contains a BAD_REQUEST and no response body" in new Fixture {

      val response = HttpResponse(
        responseStatus = BAD_REQUEST,
        responseHeaders = Map.empty
      )
      when {
        viewNotificationConnector.http.GET[HttpResponse](any(), any(), any())(any(), any(), any())
      } thenReturn Future.successful(response)

      when {
        viewNotificationConnector.audit.sendDataEvent
      } thenReturn ((f: DataEvent) => dataEvent = f)

      whenReady(viewNotificationConnector.getNotification(amlsRegistrationNumber, contactNumber).failed) {
        case HttpStatusException(status, body) =>
          status mustEqual BAD_REQUEST
          body mustEqual None
          dataEvent.auditSource mustEqual "amls-notification"
          dataEvent.auditType mustEqual "viewNotificationEventFailed"
      }
    }

    "return a failed future containing json validation message" in new Fixture {

      val response = HttpResponse(
        responseStatus = OK,
        responseHeaders = Map.empty,
        responseString = Some("message")
      )

      when {
        viewNotificationConnector.http.GET[HttpResponse](any(), any(), any())(any(), any(), any())
      } thenReturn Future.successful(response)

      when {
        viewNotificationConnector.audit.sendDataEvent
      } thenReturn ((f: DataEvent) => dataEvent = f)

      whenReady(viewNotificationConnector.getNotification(amlsRegistrationNumber, contactNumber).failed) {
        case HttpStatusException(status, body) =>
          status mustEqual OK
          body mustEqual Some("message")
          dataEvent.auditSource mustEqual "amls-notification"
          dataEvent.auditType mustEqual "viewNotificationEventFailed"
      }
    }

    "return a failed future containing an exception message" in new Fixture {

      when {
        viewNotificationConnector.http.GET[HttpResponse](any(), any(), any())(any(), any(), any())
      } thenReturn Future.failed(new Exception("message"))

      whenReady(viewNotificationConnector.getNotification(amlsRegistrationNumber, contactNumber).failed) {
        case HttpStatusException(status, body) =>
          status mustEqual INTERNAL_SERVER_ERROR
          body mustEqual Some("message")
      }
    }
  }
}
