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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import scala.concurrent.Future

class EmailConnectorSpec extends PlaySpec with MockitoSugar with ScalaFutures with IntegrationPatience with GuiceOneAppPerSuite with BeforeAndAfterAll {

  trait Fixture {

    implicit val hc = HeaderCarrier()

    val sendTo = "e@mail.com"
    val mockAppConfig = mock[ServicesConfig]
    val mockHttpClient = mock[DefaultHttpClient]

    val emailConnector = new EmailConnector(mockAppConfig, mockHttpClient)
  }

  "The Email connector" must {
    "send a details of the email template and content and report a positive response" in new Fixture {
      when(emailConnector.http.POST[SendTemplatedEmailRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(202, "")))

      val result = await(emailConnector.sendNotificationReceivedTemplatedEmail(List(sendTo)))

      result must be(true)
    }

    "send a details of the email template and content and report a negative response" in new Fixture {
      when(emailConnector.http.POST[SendTemplatedEmailRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(400, "")))

      val result = await(emailConnector.sendNotificationReceivedTemplatedEmail(List(sendTo)))

      result must be(false)
    }
  }
}
