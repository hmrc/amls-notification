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

import config.ApplicationConfig

import javax.inject.Inject
import play.api.Logging
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class SendTemplatedEmailRequest(to: List[String], templateId: String, parameters: Map[String, String])

object SendTemplatedEmailRequest {
  implicit val format: OFormat[SendTemplatedEmailRequest] = Json.format[SendTemplatedEmailRequest]
}

class EmailConnector @Inject()(val config: ApplicationConfig, val http: DefaultHttpClient) extends Logging {
  lazy val serviceURL: String = config.emailUrl
  val sendEmailURI = "/hmrc/email"

  def sendNotificationReceivedTemplatedEmail(to: List[String])(implicit hc: HeaderCarrier): Future[Boolean] = {
    val request = SendTemplatedEmailRequest(to, "amls_notification_received_template", Map())
    sendEmail(request)
  }

  private def sendEmail(request: SendTemplatedEmailRequest)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val postUrl = s"""$serviceURL$sendEmailURI"""

    logger.debug(s"[EmailConnector] Sending email to ${request.to.mkString(", ")}")

    http.POST(postUrl, request) map {
      response =>
        response.status match {
          case 202 => logger.debug(s"[EmailConnector] Email sent: ${response.body}"); true
          case _ => logger.error(s"[EmailConnector] Email not sent: ${response.body}"); false
        }
    }
  }
}