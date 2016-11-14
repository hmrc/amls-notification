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

package models

import play.api.data.validation.ValidationError
import play.api.libs.json._

sealed trait RevokedReason extends StatusReason

object RevokedReason {

  case object RevokedMissingTrader extends RevokedReason

  case object RevokedCeasedTrading extends RevokedReason

  case object RevokedNonCompliant extends RevokedReason

  case object RevokedFitAndProperFailure extends RevokedReason

  case object RevokedFailedToPayCharges extends RevokedReason

  case object RevokedFailedToRespond extends RevokedReason

  case object RevokedOther extends RevokedReason

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
