/*
 * Copyright 2016 HM Revenue & Customs
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

package audit

import models.des.{NotificationResponse, ReadStatusResponse, SubscriptionRequest, SubscriptionResponse}
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.http.HeaderCarrier

object GetNotificationEvent {
  def apply
  (amlsRegistrationNumber: String, contactNumber: String, response: NotificationResponse)
  (implicit
   hc: HeaderCarrier,
   reqW: Writes[SubscriptionRequest],
   resW: Writes[SubscriptionResponse]
  ): DataEvent =
    DataEvent(
      auditSource = AppName.appName,
      auditType = "OutboundCall",
      tags = hc.toAuditTags("Get Notification", "N/A"),
      detail = hc.toAuditDetails() ++ Map(
        "amlsRegNo" -> amlsRegistrationNumber,
        "contactNumber" -> contactNumber,
        "response" -> Json.toJson(response).toString
      )
    )
}