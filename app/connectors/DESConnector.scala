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
import metrics.Metrics
import play.api.Logging
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.Audit
import utils.{AuditHelper, HttpResponseHelper}

trait DESConnector extends HttpResponseHelper with Logging {

  val requestUrl = "anti-money-laundering/secure-comms"

  protected val amlsConfig: ApplicationConfig
  protected val http: HttpClient
  protected val auditConnector: AuditConnector
  protected val metrics: Metrics

  private[connectors] lazy val baseUrl: String = amlsConfig.desUrl
  private[connectors] lazy val token: String = s"Bearer ${amlsConfig.desToken}"
  private[connectors] lazy val env: String = amlsConfig.desEnv
  private[connectors] val audit: Audit = new Audit(AuditHelper.appName, auditConnector)
  private[connectors] val fullUrl: String = s"$baseUrl/$requestUrl"

  protected implicit val hc: HeaderCarrier = HeaderCarrier()
}