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

package utils

import javax.inject.Inject
import play.api.Logger
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class DefaultAuthAction @Inject()(
                                   val authConnector: AuthConnector
                                 )(implicit ec: ExecutionContext) extends AuthAction with AuthorisedFunctions {

  override protected def filter[A](request: Request[A]): Future[Option[Result]] = {
    // $COVERAGE-OFF$
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers)

    Logger.debug(s"DefaultAuthAction calling authorised(ConfidenceLevel.L50)")

    authorised(ConfidenceLevel.L50) {
      Logger.debug(s"DefaultAuthAction calling authorised(ConfidenceLevel.L50) - success")
      Future.successful(None)
    }.recover[Option[Result]] {
      case e: AuthorisationException => {
        Logger.debug(s"DefaultAuthAction calling authorised(ConfidenceLevel.L50 - fail: " + e)
        Some(Results.Unauthorized)
      }
    }
    // $COVERAGE-ON$
  }
}

@com.google.inject.ImplementedBy(classOf[DefaultAuthAction])
trait AuthAction extends ActionFilter[Request] with ActionBuilder[Request]
