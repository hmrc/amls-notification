/*
 * Copyright 2017 HM Revenue & Customs
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
import com.codahale.metrics.{MetricRegistry, Counter, Timer}
import uk.gov.hmrc.play.graphite.MicroserviceMetrics

trait Metrics {
  def timer(api: APITypes): Timer.Context
  def success(api: APITypes): Unit
  def failed(api: APITypes): Unit
}

object Metrics extends Metrics with MicroserviceMetrics {
  // $COVERAGE-OFF$
  val registry: MetricRegistry = metrics.defaultRegistry
  val timers = Map[APITypes, Timer](
    API11 -> registry.timer(s"${API11.key}-timer")
  )

  val successCounters = Map[APITypes, Counter](
    API11 -> registry.counter(s"${API11.key}-success")
  )

  val failedCounters = Map[APITypes, Counter](
    API11 -> registry.counter(s"${API11.key}-failure")
  )

  override def timer(api: APITypes): Context = timers(api).time()
  override def success(api: APITypes): Unit = successCounters(api).inc()
  override def failed(api: APITypes): Unit = failedCounters(api).inc()
}
