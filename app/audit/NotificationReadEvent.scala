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

package audit

import models.fe.NotificationDetails
import models.{ContactType, Status}
import play.api.libs.json.{JsObject, Json, Writes}
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.http.HeaderCarrier

object NotificationReadEvent {

  def apply(amlsRegNo: String, request: NotificationDetails)
           (implicit hc: HeaderCarrier, contactW: Writes[ContactType], statusW: Writes[Status]) = {

    val data = Json.toJson(hc.toAuditDetails()).as[JsObject] ++ Json.obj(
      "registrationNumber" -> amlsRegNo,
      "contactType" -> request.contactType,
      "messageContent" -> request.messageText
    )

    ExtendedDataEvent(
      auditSource = AppName.appName,
      auditType = "notificationRead",
      tags = hc.toAuditTags("Read Notification", "N/A"),
      detail = data
    )
  }
}