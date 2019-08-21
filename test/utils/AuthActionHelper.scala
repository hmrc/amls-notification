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

import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc._
import play.api.test.Helpers

object SuccessfulAuthAction extends AuthAction {
  override protected def filter[A](request: Request[A]): Future[Option[Result]] =
    Future.successful(None)

  override def parser: BodyParser[AnyContent] = Helpers.stubControllerComponents().parsers.defaultBodyParser

  override protected def executionContext: ExecutionContext = Helpers.stubControllerComponents().executionContext
}

object FailedAuthAction extends AuthAction {
  override protected def filter[A](request: Request[A]): Future[Option[Result]] =
    Future.successful(Some(Results.Unauthorized))

  override def parser: BodyParser[AnyContent] = Helpers.stubControllerComponents().parsers.defaultBodyParser

  override protected def executionContext: ExecutionContext = Helpers.stubControllerComponents().executionContext
}