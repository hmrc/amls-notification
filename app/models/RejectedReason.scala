package models

import play.api.data.validation.ValidationError
import play.api.libs.json._

sealed trait RejectedReason extends StatusReason

object RejectedReason {

  case object NonCompliant extends RejectedReason

  case object FailedToRespond extends RejectedReason

  case object FailedToPayCharges extends RejectedReason

  case object FitAndProperFailure extends RejectedReason

  case object OtherFailed extends RejectedReason

  case object OtherRefused extends RejectedReason

  import utils.MappingUtils.Implicits._

  implicit val jsonReads: Reads[RejectedReason] = {
    import play.api.libs.json._

    (__ \ "status_reason").read[String].flatMap[RejectedReason] {
      case "01" => NonCompliant
      case "02" => FailedToRespond
      case "03" => FailedToPayCharges
      case "04" => FitAndProperFailure
      case "98" => OtherFailed
      case "99" => OtherRefused
      case _ => ValidationError("error.invalid")
    }
  }

  implicit val jsonWrites = Writes[RejectedReason] {
    case NonCompliant => Json.obj("status_reason" -> "01")
    case FailedToRespond => Json.obj("status_reason" -> "02")
    case FailedToPayCharges => Json.obj("status_reason" -> "03")
    case FitAndProperFailure => Json.obj("status_reason" -> "04")
    case OtherFailed => Json.obj("status_reason" -> "98")
    case OtherRefused => Json.obj("status_reason" -> "99")
  }
}
