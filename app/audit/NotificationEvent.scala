package audit

import models.des.SubscriptionView
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.http.HeaderCarrier

object NotificationEvent {
  def apply
  (amlsRegistrationNumber: String)
  (implicit
   hc: HeaderCarrier
  ): DataEvent =
    DataEvent(
      auditSource = AppName.appName,
      auditType = "OutboundCall",
      tags = hc.toAuditTags("Subscription View", "N/A"),
      detail = hc.toAuditDetails() ++ Map(
        "amlsRegNo" -> amlsRegistrationNumber
      )
    )
}
