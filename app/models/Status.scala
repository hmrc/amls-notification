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

sealed trait Status

object Status {

  case object Approved extends Status

  case object Rejected extends Status

  case object Revoked extends Status

  case object DeRegistered extends Status

  case object Expired extends Status

  import utils.MappingUtils.Implicits._

  implicit val jsonReads: Reads[Status] = {
    import play.api.libs.json._

    (__ \ "status").read[String].flatMap[Status] {
      case "04" => Approved
      case "06" => Rejected
      case "08" => Revoked
      case "10" => DeRegistered
      case "11" => Expired
      case _ => ValidationError("error.invalid")
    }
  }

  implicit val jsonWrites = Writes[Status] {
    case Approved => Json.obj("status" -> "04")
    case Rejected => Json.obj("status" -> "06")
    case Revoked => Json.obj("status" -> "08")
    case DeRegistered => Json.obj("status" -> "10")
    case Expired => Json.obj("status" -> "11")
  }
}
