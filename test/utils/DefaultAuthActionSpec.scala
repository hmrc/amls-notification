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

package utils

import org.scalatest.matchers.must.Matchers
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.{AuthConnector, MissingBearerToken}
import uk.gov.hmrc.http.HeaderCarrier
import ch.qos.logback.classic.Level
import play.api.Logger
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class DefaultAuthActionSpec
    extends PlaySpec
    with MockitoSugar
    with ScalaFutures
    with Matchers
    with IntegrationPatience
    with GuiceOneAppPerSuite
    with LogCapturing {

  val mockAuthConnector                   = mock[AuthConnector]
  implicit val headCarrier: HeaderCarrier = mock[HeaderCarrier]
  val mockCC: ControllerComponents        = app.injector.instanceOf[ControllerComponents]

  class Harness(authAction: DefaultAuthAction) extends BaseController {
    def onPageLoad(): Action[AnyContent] = authAction { _ =>
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
        val result     = controller.onPageLoad()(fakeRequest)
        status(result) mustBe UNAUTHORIZED
      }
    }

    "authentication succeded" must {
      "be OK" in {
        val authAction = new DefaultAuthAction(new FakeAuthConnector(None), mockCC)
        val controller = new Harness(authAction)
        val result     = controller.onPageLoad()(fakeRequest)
        status(result) mustBe OK
      }
    }
  }

  private val authLogger: Logger = Logger(classOf[DefaultAuthAction])

  "Default Auth Action logging" must {

    "log a warning with the reason and type when authorisation fails" in {
      val controller = new Harness(new DefaultAuthAction(new FakeAuthConnector(Some(new MissingBearerToken)), mockCC))

      withCaptureOfLoggingFrom(authLogger) { logs =>
        status(controller.onPageLoad()(fakeRequest)) mustBe UNAUTHORIZED

        val event =
          logs.find(_.getFormattedMessage.contains("[DefaultAuthAction][filter] - Authorisation failed - reason:"))
        event.map(_.getLevel) mustBe Some(Level.WARN)
        event.get.getFormattedMessage must include("type: MissingBearerToken")
      }
    }

    "log a warning with the exception attached and rethrow when the auth service call fails" in {
      val boom       = new RuntimeException("auth service down")
      val controller = new Harness(new DefaultAuthAction(new FakeAuthConnector(Some(boom)), mockCC))

      withCaptureOfLoggingFrom(authLogger) { logs =>
        controller.onPageLoad()(fakeRequest).failed.futureValue mustBe boom

        val event = logs.find(
          _.getFormattedMessage.contains("[DefaultAuthAction][filter] - Unexpected exception calling auth service")
        )
        event.map(_.getLevel) mustBe Some(Level.WARN)
        Option(event.get.getThrowableProxy).map(_.getMessage) mustBe Some("auth service down")
      }
    }

    "not log any warning when authorisation succeeds" in {
      val controller = new Harness(new DefaultAuthAction(new FakeAuthConnector(None), mockCC))

      withCaptureOfLoggingFrom(authLogger) { logs =>
        status(controller.onPageLoad()(fakeRequest)) mustBe OK
        logs.exists(_.getLevel == Level.WARN) mustBe false
      }
    }
  }
}

class FakeAuthConnector(exceptionToReturn: Option[Throwable]) extends AuthConnector {
  val serviceUrl: String = "amls-notification"
  def success: Any       = ()

  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[A] =
    exceptionToReturn.fold(Future.successful(success.asInstanceOf[A]))(Future.failed(_))

}
