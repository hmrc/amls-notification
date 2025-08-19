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

import javax.inject.Inject
import play.api.Logging
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.{ExecutionContext, Future}

case class SendTemplatedEmailRequest(to: List[String], templateId: String, parameters: Map[String, String])

object SendTemplatedEmailRequest {
  implicit val format: OFormat[SendTemplatedEmailRequest] = Json.format[SendTemplatedEmailRequest]
}

class EmailConnector @Inject() (val config: ApplicationConfig, val httpClientV2: HttpClientV2)(implicit
  ec: ExecutionContext
) extends Logging {
  lazy val serviceURL: String = config.emailUrl
  val sendEmailURI            = "/hmrc/email"

  def sendNotificationReceivedTemplatedEmail(to: List[String])(implicit hc: HeaderCarrier): Future[Boolean] = {
    val request = SendTemplatedEmailRequest(to, "amls_notification_received_template", Map())
    sendEmail(request)
  }

  private def sendEmail(request: SendTemplatedEmailRequest)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val postUrl = s"$serviceURL$sendEmailURI"

    logger.debug(s"[EmailConnector] Sending email to ${request.to.mkString(", ")}")

    httpClientV2
      .post(url"$postUrl")
      .withBody(Json.toJson(request))
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case 202 => logger.debug(s"[EmailConnector] Email sent: ${response.body}"); true
          case _   => logger.error(s"[EmailConnector] Email not sent: ${response.body}"); false
        }
      }
  }
}
