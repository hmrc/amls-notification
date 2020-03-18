/*
 * Copyright 2020 HM Revenue & Customs
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

case class Status(status: StatusType, statusReason: Option[StatusReason])

object Status {

  implicit val jsonReads: Reads[Status] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._

    val reads = (
      (__ \ "status_type").read[StatusType] and
        (__ \ "status_reason").readNullable[String]
      ).tupled

    reads flatMap {
      case (statusType, Some(_)) => StatusReason.jsonReads(statusType) map {
        case EmptyReason => Status(statusType, None)
        case reason => Status(statusType, Some(reason))
      }
      case (statusType, _) => Reads(_ => JsSuccess(Status(statusType, None)))
    }
  }

  implicit val jsonWrites: Writes[Status] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    (
      (__ \ "status_type").write[StatusType] and
        (__ \ "status_reason").writeNullable[StatusReason]
      ) (unlift(Status.unapply))
  }
}
