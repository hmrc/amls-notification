/*
 * Copyright 2017 HM Revenue & Customs
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

import play.api.mvc.EssentialFilter
import play.api.{Application, Configuration}
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.microservice.bootstrap.DefaultMicroserviceGlobal

object AmlsGlobal extends DefaultMicroserviceGlobal with RunMode {
  override lazy  val auditConnector = MicroserviceAuditConnector

  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] = app.configuration.getConfig("microservice.metrics")

  override lazy  val loggingFilter = MicroserviceLoggingFilter

  override lazy  val microserviceAuditFilter = MicroserviceAuditFilter

  override def authFilter: Option[EssentialFilter]  = Some(MicroserviceAuthFilter)
}
