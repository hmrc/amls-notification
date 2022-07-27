/*
 * Copyright 2022 HM Revenue & Customs
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

  implicit val jsonReads: Reads[DeregisteredReason] = {
    import play.api.libs.json._

    (__ \ "status_reason").read[String].flatMap[DeregisteredReason] {
      case "01" => Reads(_ => JsSuccess(CeasedTrading))
      case "02" => Reads(_ => JsSuccess(HVDNoCashPayment))
      case "03" => Reads(_ => JsSuccess(OutOfScope))
      case "04" => Reads(_ => JsSuccess(NotTrading))
      case "05" => Reads(_ => JsSuccess(UnderAnotherSupervisor))
      case "06" => Reads(_ => JsSuccess(ChangeOfLegalEntity))
      case "99" => Reads(_ => JsSuccess(Other))
      case _ => Reads(_ => JsError(JsPath \ "status_reason" -> JsonValidationError("error.invalid")))
    }
  }

  implicit val jsonWrites = Writes[DeregisteredReason] {
    case CeasedTrading => JsString("01")
    case HVDNoCashPayment => JsString("02")
    case OutOfScope => JsString("03")
    case NotTrading => JsString("04")
    case UnderAnotherSupervisor => JsString("05")
    case ChangeOfLegalEntity => JsString("06")
    case Other => JsString("99")
  }
}
