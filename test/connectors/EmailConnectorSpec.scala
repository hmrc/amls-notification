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

import config.ApplicationConfig
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import ch.qos.logback.classic.Level
import play.api.Logger
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing

import scala.concurrent.{ExecutionContext, Future}

class EmailConnectorSpec
    extends PlaySpec
    with MockitoSugar
    with ScalaFutures
    with IntegrationPatience
    with GuiceOneAppPerSuite
    with BeforeAndAfterAll
    with LogCapturing {

  trait Fixture {
    implicit val hc: HeaderCarrier    = HeaderCarrier()
    implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

    val sendTo                             = "e@mail.com"
    val mockHttpClientV2: HttpClientV2     = mock[HttpClientV2]
    val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
    val config: ApplicationConfig          = app.injector.instanceOf[ApplicationConfig]

    val emailConnector = new EmailConnector(config, mockHttpClientV2)

    def stubHttp(response: Future[HttpResponse]): Unit = {
      when(mockHttpClientV2.post(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any[HttpReads[HttpResponse]], any[ExecutionContext]))
        .thenReturn(response)
    }
  }

  "SendTemplatedEmailRequest" must {
    "serialise and deserialise correctly" in {
      val req  = SendTemplatedEmailRequest(List("a@b.com"), "some_template", Map("key" -> "value"))
      val json = Json.toJson(req)
      json.as[SendTemplatedEmailRequest] mustBe req
    }
  }

  "The Email connector" must {
    "send a details of the email template and content and report a positive response" in new Fixture {
      stubHttp(Future.successful(HttpResponse(202, "")))
      await(emailConnector.sendNotificationReceivedTemplatedEmail(List(sendTo))) must be(true)
    }

    "send a details of the email template and content and report a negative response" in new Fixture {
      stubHttp(Future.successful(HttpResponse(400, "")))
      await(emailConnector.sendNotificationReceivedTemplatedEmail(List(sendTo))) must be(false)
    }

    "return false when the email service responds with 500" in new Fixture {
      stubHttp(Future.successful(HttpResponse(500, "")))
      await(emailConnector.sendNotificationReceivedTemplatedEmail(List(sendTo))) must be(false)
    }

    "propagate a failed Future when the HTTP call throws" in new Fixture {
      stubHttp(Future.failed(new RuntimeException("connection refused")))
      whenReady(emailConnector.sendNotificationReceivedTemplatedEmail(List(sendTo)).failed) { ex =>
        ex mustBe a[RuntimeException]
        ex.getMessage mustBe "connection refused"
      }
    }

    "handle multiple recipients" in new Fixture {
      stubHttp(Future.successful(HttpResponse(202, "")))
      await(emailConnector.sendNotificationReceivedTemplatedEmail(List("a@a.com", "b@b.com"))) must be(true)
    }
  }

  private val emailLogger: Logger = Logger(classOf[EmailConnector])

  "The Email connector logging" must {

    "log an error with the status and template when the email service rejects the request" in new Fixture {
      stubHttp(Future.successful(HttpResponse(400, "bad request")))

      withCaptureOfLoggingFrom(emailLogger) { logs =>
        await(emailConnector.sendNotificationReceivedTemplatedEmail(List(sendTo))) must be(false)

        val event =
          logs.find(_.getFormattedMessage.contains("[EmailConnector][sendEmail] - Email service rejected request"))
        event.map(_.getLevel) mustBe Some(Level.ERROR)
        event.get.getFormattedMessage must include("status: 400")
        event.get.getFormattedMessage must include("template: amls_notification_received_template")
      }
    }

    "log a warning with the exception attached when the HTTP call throws" in new Fixture {
      val boom = new RuntimeException("connection refused")
      stubHttp(Future.failed(boom))

      withCaptureOfLoggingFrom(emailLogger) { logs =>
        emailConnector.sendNotificationReceivedTemplatedEmail(List(sendTo)).failed.futureValue mustBe boom

        val event =
          logs.find(_.getFormattedMessage.contains("[EmailConnector][sendEmail] - Exception calling email service"))
        event.map(_.getLevel) mustBe Some(Level.WARN)
        Option(event.get.getThrowableProxy).map(_.getMessage) mustBe Some("connection refused")
      }
    }

    "never log the recipient email address" in new Fixture {
      stubHttp(Future.successful(HttpResponse(400, "")))

      withCaptureOfLoggingFrom(emailLogger) { logs =>
        await(emailConnector.sendNotificationReceivedTemplatedEmail(List(sendTo)))
        logs.exists(_.getFormattedMessage.contains(sendTo)) mustBe false
      }
    }
  }
}
