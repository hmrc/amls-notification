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

package metrics

import com.codahale.metrics.Timer.Context
import com.codahale.metrics.{Counter, MetricRegistry, Timer}
import com.google.inject.Inject

class Metrics @Inject() (metrics: com.codahale.metrics.MetricRegistry) {
  // $COVERAGE-OFF$
  val registry = new MetricRegistry
  val timers   = Map[APITypes, Timer](
    API11 -> registry.timer(s"${API11.key}-timer")
  )

  val successCounters = Map[APITypes, Counter](
    API11 -> registry.counter(s"${API11.key}-success")
  )

  val failedCounters = Map[APITypes, Counter](
    API11 -> registry.counter(s"${API11.key}-failure")
  )

  def timer(api: APITypes): Context = timers(api).time()
  def success(api: APITypes): Unit  = successCounters(api).inc()
  def failed(api: APITypes): Unit   = failedCounters(api).inc()
}
