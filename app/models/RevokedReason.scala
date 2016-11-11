package models

import play.api.data.validation.ValidationError
import play.api.libs.json._

sealed trait RevokedReason extends StatusReason

case object RevokedMissingTrader extends RevokedReason

case object RevokedCeasedTrading extends RevokedReason

case object RevokedNonCompliant extends RevokedReason

case object RevokedFitAndProperFailure extends RevokedReason

case object RevokedFailedToPayCharges extends RevokedReason

case object RevokedFailedToRespond extends RevokedReason

case object RevokedOther extends RevokedReason

object RevokedReason {

  import utils.MappingUtils.Implicits._

  implicit val jsonReads: Reads[RevokedReason] = {
    import play.api.libs.json._

    (__ \ "status_reason").read[String].flatMap[RevokedReason] {
      case "01" => RevokedMissingTrader
      case "02" => RevokedCeasedTrading
      case "03" => RevokedNonCompliant
      case "04" => RevokedFitAndProperFailure
      case "05" => RevokedFailedToPayCharges
      case "06" => RevokedFailedToRespond
      case "99" => RevokedOther
      case _ => ValidationError("error.invalid")
    }
  }

  implicit val jsonWrites = Writes[RevokedReason] {
    case RevokedMissingTrader => Json.obj("status_reason" -> "01")
    case RevokedCeasedTrading => Json.obj("status_reason" -> "02")
    case RevokedNonCompliant => Json.obj("status_reason" -> "03")
    case RevokedFitAndProperFailure => Json.obj("status_reason" -> "04")
    case RevokedFailedToPayCharges => Json.obj("status_reason" -> "05")
    case RevokedFailedToRespond => Json.obj("status_reason" -> "06")
    case RevokedOther => Json.obj("status_reason" -> "99")
  }
}
