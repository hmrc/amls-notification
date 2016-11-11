package models

import play.api.data.validation.ValidationError
import play.api.libs.json._

trait StatusReason

case object IgnoreThis extends StatusReason

object StatusReason {

  import utils.MappingUtils.Implicits._

  implicit val jsonReads: Reads[StatusReason] = {
    import play.api.libs.json._

    (__ \ "status").read[String].flatMap[StatusReason] {
      case "04" => IgnoreThis
      case "06" => __.read[RejectedReason].map (identity[StatusReason])
      case "08" => __.read[RevokedReason].map (identity[StatusReason])
      case "10" => __.read[DeregisteredReason].map (identity[StatusReason])
      case "11" => IgnoreThis
      case _ => ValidationError("error.invalid")
    }
  }

  implicit val jsonWrites: Writes[StatusReason] = {
    import play.api.libs.json._
    Writes[StatusReason] {
      case a: RejectedReason =>
           __.write[RejectedReason].writes(a)
      case a: RevokedReason =>
        __.write[RevokedReason].writes(a)
      case a: DeregisteredReason =>
        __.write[DeregisteredReason].writes(a)
    }
  }
}