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

package utils

import org.scalatest.MustMatchers
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.{BaseController, ControllerComponents}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.{AuthConnector, MissingBearerToken}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class DefaultAuthActionSpec extends PlaySpec with MockitoSugar with ScalaFutures with MustMatchers with IntegrationPatience with GuiceOneAppPerSuite {

  val mockAuthConnector = mock[AuthConnector]
  implicit val headCarrier = mock[HeaderCarrier]
  val mockCC: ControllerComponents = app.injector.instanceOf[ControllerComponents]

  class Harness(authAction: DefaultAuthAction) extends BaseController {
    def onPageLoad() = authAction { _ =>
      Ok
    }

    override protected def controllerComponents: ControllerComponents = mockCC
  }

  def fakeRequest = FakeRequest("", "")

  "Default Auth Action" when {
    "authentication failed" must {
      "be UNAUTHORIZED" in {
        val authAction = new DefaultAuthAction(new FakeAuthConnector(Some(new MissingBearerToken)), mockCC)
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe UNAUTHORIZED
      }
    }

    "authentication succeded" must {
      "be OK" in {
        val authAction = new DefaultAuthAction(new FakeAuthConnector(None), mockCC)
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe OK
      }
    }
  }
}

class FakeAuthConnector(exceptionToReturn: Option[Throwable]) extends AuthConnector {
  val serviceUrl: String = "amls-notification"
  def success: Any = ()

  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
    exceptionToReturn.fold(Future.successful(success.asInstanceOf[A]))(Future.failed(_))

}