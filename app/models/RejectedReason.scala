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

sealed trait RejectedReason extends StatusReason

object RejectedReason {

  case object NonCompliant extends RejectedReason

  case object FailedToRespond extends RejectedReason

  case object FailedToPayCharges extends RejectedReason

  case object FitAndProperFailure extends RejectedReason

  case object OtherFailed extends RejectedReason

  case object OtherRefused extends RejectedReason

  implicit val jsonReads: Reads[RejectedReason] = {
    import play.api.libs.json._
    (__ \ "status_reason").read[String].flatMap[RejectedReason] {
      case "01" => Reads(_ => JsSuccess(NonCompliant))
      case "02" => Reads(_ => JsSuccess(FailedToRespond))
      case "03" => Reads(_ => JsSuccess(FailedToPayCharges))
      case "04" => Reads(_ => JsSuccess(FitAndProperFailure))
      case "98" => Reads(_ => JsSuccess(OtherFailed))
      case "99" => Reads(_ => JsSuccess(OtherRefused))
      case _ => Reads(_ => JsError(JsPath \ "status_reason" -> ValidationError("error.invalid")))
    }
  }

  implicit val jsonWrites = Writes[RejectedReason] {
    case NonCompliant => JsString("01")
    case FailedToRespond => JsString("02")
    case FailedToPayCharges => JsString("03")
    case FitAndProperFailure => JsString("04")
    case OtherFailed => JsString("98")
    case OtherRefused => JsString("99")
  }
}
