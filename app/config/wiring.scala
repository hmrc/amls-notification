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

import akka.actor.ActorSystem
import com.typesafe.config.Config
import javax.inject.Inject
import play.api.Mode.Mode
import play.api.{Application, Configuration}
import uk.gov.hmrc.http.hooks.HttpHooks
import uk.gov.hmrc.http.{HttpDelete, HttpGet, HttpPost, HttpPut}
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.auth.controllers.AuthParamsControllerConfig
import uk.gov.hmrc.play.auth.microservice.connectors.AuthConnector
import uk.gov.hmrc.play.auth.microservice.filters.AuthorisationFilter
import uk.gov.hmrc.play.config.{AppName, ControllerConfig, RunMode, ServicesConfig}
import uk.gov.hmrc.play.http.ws._
import uk.gov.hmrc.play.microservice.config.LoadAuditingConfig
import uk.gov.hmrc.play.microservice.filters.{AuditFilter, LoggingFilter, MicroserviceFilterSupport}

trait Hooks extends HttpHooks with HttpAuditing {
  override val hooks = Seq(AuditingHook)
}

class WSHttp @Inject()(application: Application, msAuditConnector: MicroserviceAuditConnector)
  extends HttpGet
  with WSGet
  with HttpPut
  with WSPut
  with HttpPost
  with WSPost
  with HttpDelete
  with WSDelete
  with Hooks
  with AppName {

  // TODO: Determine whether we need auditing here

  override val hooks = Seq.empty

  protected def actorSystem: ActorSystem = application.actorSystem

  override protected def configuration: Option[Config] = Some(application.configuration.underlying)

  override protected def appNameConfiguration: Configuration = application.configuration

  override def auditConnector: AuditConnector = msAuditConnector
}

class MicroserviceAuditConnector @Inject()(application: Application) extends AuditConnector with RunMode {
  override lazy val auditingConfig = LoadAuditingConfig("auditing")

  override protected def mode: Mode = application.mode

  override protected def runModeConfiguration: Configuration = application.configuration
}

class MicroserviceAuthConnector @Inject()(application: Application, msAuditConnector: MicroserviceAuditConnector)
  extends WSHttp(application, msAuditConnector) with AuthConnector with ServicesConfig {

  override val authBaseUrl = baseUrl("auth")

  override protected def appNameConfiguration: Configuration = application.configuration

  override protected def mode: Mode = application.mode

  override protected def runModeConfiguration: Configuration = application.configuration

  override protected def configuration: Option[Config] = Some(application.configuration.underlying)

  override protected def actorSystem: ActorSystem = application.actorSystem
}

class ControllerConfiguration @Inject()(application: Application) extends ControllerConfig {
  override lazy val controllerConfigs: Config = application.configuration.underlying.getConfig("controllers")
}

class AuthParamsControllerConfiguration @Inject()(controllerConfig: ControllerConfiguration)
  extends AuthParamsControllerConfig {

  lazy val controllerConfigs = controllerConfig.controllerConfigs
}

class MicroserviceAuditFilter @Inject()(
  application: Application,
  controllerConfig: ControllerConfiguration,
  microserviceAuditConnector: MicroserviceAuditConnector) extends AuditFilter with AppName with MicroserviceFilterSupport {

  override val auditConnector = microserviceAuditConnector

  override def controllerNeedsAuditing(controllerName: String) =
    controllerConfig.paramsForController(controllerName).needsAuditing

  override protected def appNameConfiguration: Configuration = application.configuration
}

class  MicroserviceLoggingFilter @Inject()(controllerConfig: ControllerConfiguration)
  extends LoggingFilter with MicroserviceFilterSupport{

  override def controllerNeedsLogging(controllerName: String) =
    controllerConfig.paramsForController(controllerName).needsLogging
}

class MicroserviceAuthFilter @Inject()(
  authConfig: AuthParamsControllerConfiguration,
  controllerConfig: ControllerConfiguration,
  microserviceAuthConnector: MicroserviceAuthConnector) extends AuthorisationFilter with MicroserviceFilterSupport {

  override val authParamsConfig = authConfig
  override val authConnector = microserviceAuthConnector
  override def controllerNeedsAuth(controllerName: String): Boolean =
    controllerConfig.paramsForController(controllerName).needsAuth
}
