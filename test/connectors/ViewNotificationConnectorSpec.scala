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

package connectors

import audit.MockAudit
import com.codahale.metrics.Timer
import config.{AmlsConfig, MicroserviceAuditConnector, WSHttp}
import exceptions.HttpStatusException
import metrics.{API11, Metrics}
import models.des.NotificationResponse
import org.joda.time.{DateTimeUtils, LocalDateTime}
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.audit.HandlerResult
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.model.{Audit, DataEvent}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ViewNotificationConnectorSpec extends PlaySpec
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

    object testConnector extends ViewNotificationConnector(
      app.injector.instanceOf(classOf[AmlsConfig]),
      app.injector.instanceOf(classOf[WSHttp]),
      app.injector.instanceOf(classOf[MicroserviceAuditConnector])) {

      lazy override private[connectors] val baseUrl: String = "baseUrl"
      lazy override private[connectors] val env: String = "ist0"
      override private[connectors] val http = mock[WSHttp]
      override private[connectors] val metrics: Metrics = mock[Metrics]
      override private[connectors] val audit = mock[MockAudit]
      override private[connectors] val fullUrl: String = s"$baseUrl/$requestUrl"
    }

    val successModel = NotificationResponse(LocalDateTime.now(), "Approved")

    val mockTimer = mock[Timer.Context]

    val amlsRegistrationNumber = "test"
    val contactNumber = "contactNumber"

    val url = s"${testConnector.baseUrl}/anti-money-laundering/secure-comms/reg-number/$amlsRegistrationNumber/contact-number/$contactNumber"
    var dataEvent: DataEvent = null

    when {
      testConnector.metrics.timer(eqTo(API11))
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
        testConnector.audit.sendDataEvent
      } thenReturn ((f: DataEvent) => dataEvent = f)

      when {
        testConnector.http.GET[HttpResponse](eqTo(url))(any(), any(), any())
      } thenReturn Future.successful(response)

      whenReady(testConnector.getNotification(amlsRegistrationNumber, contactNumber)) {
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
        testConnector.http.GET[HttpResponse](eqTo(url))(any(), any(), any())
      } thenReturn Future.successful(response)

      when {
        testConnector.audit.sendDataEvent
      } thenReturn ((f: DataEvent) => dataEvent = f)

      whenReady(testConnector.getNotification(amlsRegistrationNumber, contactNumber).failed) {
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
        testConnector.http.GET[HttpResponse](any())(any(), any(), any())
      } thenReturn Future.successful(response)

      when {
        testConnector.audit.sendDataEvent
      } thenReturn ((f: DataEvent) => dataEvent = f)

      whenReady(testConnector.getNotification(amlsRegistrationNumber, contactNumber).failed) {
        case HttpStatusException(status, body) =>
          status mustEqual OK
          body mustEqual Some("message")
          dataEvent.auditSource mustEqual "amls-notification"
          dataEvent.auditType mustEqual "viewNotificationEventFailed"
      }
    }

    "return a failed future containing an exception message" in new Fixture {

      when {
        testConnector.http.GET[HttpResponse](eqTo(url))(any(), any(), any())
      } thenReturn Future.failed(new Exception("message"))

      whenReady(testConnector.getNotification(amlsRegistrationNumber, contactNumber).failed) {
        case HttpStatusException(status, body) =>
          status mustEqual INTERNAL_SERVER_ERROR
          body mustEqual Some("message")
      }
    }
  }

}
