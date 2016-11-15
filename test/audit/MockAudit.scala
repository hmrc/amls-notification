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

package audit

import uk.gov.hmrc.play.audit.model.{Audit, DataEvent}

// This only exists because of the difficulty in mocking
// the `sendDataEvent` method of the `Audit` class
// scalastyle:off null
object MockAudit extends Audit("mockApp", null) {
  override def sendDataEvent: (DataEvent) => Unit =
    _ => {}
}
