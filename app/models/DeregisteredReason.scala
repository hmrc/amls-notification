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

sealed trait DeregisteredReason extends StatusReason

object DeregisteredReason {


  case object CeasedTrading extends DeregisteredReason

  case object HVDNoCashPayment extends DeregisteredReason

  case object OutOfScope extends DeregisteredReason

  case object NotTrading extends DeregisteredReason

  case object UnderAnotherSupervisor extends DeregisteredReason

  case object ChangeOfLegalEntity extends DeregisteredReason

  case object Other extends DeregisteredReason

  import utils.MappingUtils.Implicits._

  implicit val jsonReads: Reads[DeregisteredReason] = {
    import play.api.libs.json._

    (__ \ "status_reason").read[String].flatMap[DeregisteredReason] {
      case "01" => CeasedTrading
      case "02" => HVDNoCashPayment
      case "03" => OutOfScope
      case "04" => NotTrading
      case "05" => UnderAnotherSupervisor
      case "06" => ChangeOfLegalEntity
      case "99" => Other
      case _ => ValidationError("error.invalid")
    }
  }

  implicit val jsonWrites = Writes[DeregisteredReason] {
    case CeasedTrading => Json.obj("status_reason" -> "01")
    case HVDNoCashPayment => Json.obj("status_reason" -> "02")
    case OutOfScope => Json.obj("status_reason" -> "03")
    case NotTrading => Json.obj("status_reason" -> "04")
    case UnderAnotherSupervisor => Json.obj("status_reason" -> "05")
    case ChangeOfLegalEntity => Json.obj("status_reason" -> "06")
    case Other => Json.obj("status_reason" -> "99")
  }
}
