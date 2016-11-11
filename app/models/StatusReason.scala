package models

import play.api.data.validation.ValidationError
import play.api.libs.json.Reads

trait StatusReason

object StatusReason {

  implicit val jsonReads: Reads[StatusReason] = {
    import play.api.libs.json._

    (__ \ "status").read[String].flatMap[StatusReason] {
      case "04" => {}
      case "06" => __.read[RejectedReason].flatMap(x => x)
      case "08" => Revoked
      case "10" => DeRegistered
      case "11" => Expired
      case _ => ValidationError("error.invalid")
    }
  }

}