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

package connectors

import config.{AmlsConfig, WSHttp}
import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class SendTemplatedEmailRequest(to: List[String], templateId: String, parameters: Map[String, String])

object SendTemplatedEmailRequest {
  implicit val format = Json.format[SendTemplatedEmailRequest]
}

trait EmailConnector extends ServicesConfig {

  private[connectors] def httpPost: HttpPost

  private[connectors] def url: String

  def sendNotificationReceivedTemplatedEmail(to: List[String])(implicit hc: HeaderCarrier): Future[Boolean] = {
    val request = SendTemplatedEmailRequest(to, "amls_notification_received_template", Map())
    sendEmail(request)
  }

  private def sendEmail(request: SendTemplatedEmailRequest)(implicit hc: HeaderCarrier): Future[Boolean] = {

    Logger.debug(s"[EmailConnector] Sending email to ${request.to.mkString(", ")}")

    httpPost.POST[SendTemplatedEmailRequest, HttpResponse](url, request, Seq(("Content-Type", "application/json"))) map {
      response =>
        response.status match {
          case 202 => Logger.debug(s"[EmailConnector] Email sent: ${response.body}"); true
          case _ => Logger.error(s"[EmailConnector] Email not sent: ${response.body}"); false
        }
    }
  }
}

object EmailConnector extends EmailConnector {
  // $COVERAGE-OFF$
  override def httpPost: HttpPost = WSHttp
  override def url = s"${AmlsConfig.emailUrl}/send-templated-email"
}