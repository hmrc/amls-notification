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

trait StatusReason

case object IgnoreThis extends StatusReason

object StatusReason {

  implicit val jsonReads: Reads[Option[StatusReason]] = {
    import play.api.libs.json._

    (__ \ "status_type").read[String].flatMap[Option[StatusReason]] {
      case "04" => Reads(_ => JsSuccess(None))
      case "06" => __.read(Reads.optionNoError[RejectedReason]).map (identity[Option[StatusReason]])
      case "08" => __.read(Reads.optionNoError[RevokedReason]).map (identity[Option[StatusReason]])
      case "10" => __.read(Reads.optionNoError[DeregisteredReason]).map (identity[Option[StatusReason]])
      case "11" => Reads(_ => JsSuccess(None))
      case _ => Reads(_ => JsSuccess(None))
    }
  }

  implicit val jsonWrites: Writes[StatusReason] = {
    import play.api.libs.json._
    Writes[StatusReason] {
      case a: RejectedReason =>
        RejectedReason.jsonWrites.writes(a)
      case a: RevokedReason =>
        RevokedReason.jsonWrites.writes(a)
      case a: DeregisteredReason =>
        DeregisteredReason.jsonWrites.writes(a)
    }
  }
}
