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


import audit.{ViewNotificationEvent, ViewNotificationEventFailed}
import config.ApplicationConfig
import exceptions.HttpStatusException
import javax.inject.Inject
import metrics.{API11, Metrics}
import models.des.NotificationResponse
import play.api.http.Status._
import play.api.libs.json.{JsSuccess, Json, Writes}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}


class ViewNotificationConnector @Inject()(val amlsConfig: ApplicationConfig,
                                          val http: HttpClient,
                                          val auditConnector: AuditConnector,
                                          val metrics: Metrics) extends DESConnector {

  def getNotification(amlsRegistrationNumber: String, contactNumber: String)
    (implicit ec: ExecutionContext, wr: Writes[NotificationResponse]): Future[NotificationResponse] = {

    val prefix = "[DESConnector][getNotification]"
    val bodyParser = JsonParsed[NotificationResponse]
    val timer = metrics.timer(API11)
    val notificationUrl = s"$fullUrl/reg-number/$amlsRegistrationNumber/contact-number/$contactNumber"
    logger.debug(s"$prefix - reg no: $amlsRegistrationNumber - contactNumber: $contactNumber")

    http.GET[HttpResponse](notificationUrl, headers = Seq("Environment" -> env,
      HeaderNames.ACCEPT -> "application/json",
      "Authorization" -> token
    )
    )(implicitly, hc, implicitly) map {
      response =>
        timer.stop()
        logger.debug(s"$prefix - Base Response: ${response.status}")
        logger.debug(s"$prefix - Response Body: ${response.body}")
        response
    } flatMap {
      case _@status(OK) & bodyParser(JsSuccess(body: NotificationResponse, _)) =>
        metrics.success(API11)
        audit.sendDataEvent(ViewNotificationEvent(amlsRegistrationNumber, contactNumber, body))
        logger.debug(s"$prefix - Success response")
        logger.debug(s"$prefix - Response body: ${Json.toJson(body)}")
        Future.successful(body)
      case r@status(s) =>
        metrics.failed(API11)
        logger.warn(s"$prefix - Failure response: $s")
        val httpEx: HttpStatusException = HttpStatusException(s, Option(r.body))
        audit.sendDataEvent(ViewNotificationEventFailed(amlsRegistrationNumber, contactNumber, httpEx))
        Future.failed(HttpStatusException(s, Option(r.body)))
    } recoverWith {
      case e: HttpStatusException =>
        logger.warn(s"$prefix - Failure: Exception", e)
        audit.sendDataEvent(ViewNotificationEventFailed(amlsRegistrationNumber, contactNumber, e))
        Future.failed(e)
      case e =>
        timer.stop()
        metrics.failed(API11)
        logger.warn(s"$prefix - Failure: Exception", e)
        Future.failed(HttpStatusException(INTERNAL_SERVER_ERROR, Some(e.getMessage)))

    }
  }
}
