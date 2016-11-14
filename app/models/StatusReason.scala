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
      case a: RejectedReason => println("i am here==================================")
        RejectedReason.jsonWrites.writes(a)
      case a: RevokedReason =>
        RevokedReason.jsonWrites.writes(a)
      case a: DeregisteredReason =>
        DeregisteredReason.jsonWrites.writes(a)
    }
  }
}
