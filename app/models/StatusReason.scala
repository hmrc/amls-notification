/*
 * Copyright 2024 HM Revenue & Customs
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

import models.StatusType.{DeRegistered, Rejected, Revoked}
import play.api.libs.json._

trait StatusReason

case object EmptyReason extends StatusReason

object StatusReason {

  def jsonReads(statusType: StatusType) = new Reads[StatusReason] {
    override def reads(json: JsValue) = statusType match {
      case Rejected => RejectedReason.jsonReads.reads(json) map identity[StatusReason]
      case Revoked => RevokedReason.jsonReads.reads(json) map identity[StatusReason]
      case DeRegistered => DeregisteredReason.jsonReads.reads(json) map identity[StatusReason]
      case _ => JsSuccess(EmptyReason)
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
