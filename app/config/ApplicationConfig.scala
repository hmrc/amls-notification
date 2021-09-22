/*
 * Copyright 2021 HM Revenue & Customs
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

import com.google.inject.Singleton
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.Inject

@Singleton
class ApplicationConfig @Inject()(servicesConfig: ServicesConfig) {

  private def getBaseUrl(service: String) = servicesConfig.baseUrl(service)

  private def getConfString(key: String): String =
    servicesConfig.getConfString(key, throw new RuntimeException(s"config $key not found"))

  val desUrl: String = getBaseUrl("des")

  val desToken: String = getConfString("des.auth-token")

  val desEnv: String = getConfString("des.env")

  val emailUrl: String = getBaseUrl("email")

  val currentTemplatePackageVersion: String = getConfString("current-template-package-version")

  val defaultTemplatePackageVersion: String = getConfString("default-template-package-version")
}
