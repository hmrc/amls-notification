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

package config

import play.api.Mode.Mode
import play.api.mvc.EssentialFilter
import play.api.{Application, Configuration, Play}
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.microservice.bootstrap.DefaultMicroserviceGlobal

//TODO: Remove this file when upgrading to Play 2.6 - see https://www.playframework.com/documentation/2.7.x/GlobalSettings

object AmlsGlobal extends DefaultMicroserviceGlobal with RunMode {

  private lazy val msAuthConnector = new MicroserviceAuthConnector(Play.current, auditConnector)

  private lazy val controllerConfiguration = new ControllerConfiguration(Play.current)

  private lazy val msAuthFilter = new MicroserviceAuthFilter(
    new AuthParamsControllerConfiguration(controllerConfiguration),
    controllerConfiguration,
    msAuthConnector
  )

  override lazy val auditConnector = new MicroserviceAuditConnector(Play.current)

  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] =
    Play.current.configuration.getConfig("microservice.metrics")

  override lazy val loggingFilter = new MicroserviceLoggingFilter(controllerConfiguration)

  override lazy val microserviceAuditFilter = new MicroserviceAuditFilter(
    Play.current,
    controllerConfiguration,
    auditConnector
  )

  override def authFilter: Option[EssentialFilter]  = Some(msAuthFilter)

  override protected def mode: Mode = Play.current.mode

  override protected def runModeConfiguration: Configuration = Play.current.configuration
}