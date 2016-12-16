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

import config.WSHttp
import org.mockito.ArgumentCaptor
import play.api.test.Helpers._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost, HttpResponse}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.Matchers.{eq => eqTo, _}
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.Future

class EmailConnectorSpec extends PlaySpec with MockitoSugar with ScalaFutures with IntegrationPatience with OneServerPerSuite with BeforeAndAfterAll {

  trait Fixture {

    implicit val hc = HeaderCarrier()

    val mockHttp = mock[WSHttp]
    val sendTo = "e@mail.com"

    object TestEmailConnector extends EmailConnector{
      override def httpPost: HttpPost = mockHttp
      override def url = "test-email-url"
    }

  }

  "The Email connector" must {
    "send a details of the email template and content and report a positive response" in new Fixture {

//      val requestCaptor = ArgumentCaptor.forClass(classOf[SendTemplatedEmailRequest])

      when(mockHttp.POST[SendTemplatedEmailRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(202)))

      val result = await(TestEmailConnector.sendNotificationReceivedTemplatedEmail(List(sendTo)))

      result must be(true)

//      verify(httpRequest)
//
//      requestCaptor.getValue.to must contain(sendTo)
//      requestCaptor.getValue.templateId must be("amls_notification_received_template")

    }
    "send a details of the email template and content and report a negative response" in new Fixture {

      val requestCaptor = ArgumentCaptor.forClass(classOf[SendTemplatedEmailRequest])

      when(mockHttp.POST[SendTemplatedEmailRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(400)))

      val result = await(TestEmailConnector.sendNotificationReceivedTemplatedEmail(List(sendTo)))

      result must be(false)

    }

  }

}
