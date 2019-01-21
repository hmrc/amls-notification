/*
 * Copyright 2019 HM Revenue & Customs
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

package config

import play.api.{Configuration, Play}
import play.api.Mode.Mode
import uk.gov.hmrc.play.config.ServicesConfig

object AmlsConfig extends ServicesConfig {

  override protected def mode: Mode = Play.current.mode

  override protected def runModeConfiguration: Configuration = Play.current.configuration

  private def loadConfig(key: String) =
    getConfString(key, throw new Exception(s"Config missing key: $key"))

  lazy val desUrl = baseUrl("des")
  lazy val desToken = loadConfig("des.auth-token")
  lazy val desEnv = loadConfig("des.env")

  lazy val emailUrl = baseUrl("email")
  lazy val currentTemplatePackageVersion = loadConfig("current-template-package-version")
  lazy val defaultTemplatePackageVersion = loadConfig("default-template-package-version")


}
