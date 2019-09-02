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

import com.google.inject.Singleton
import javax.inject.Inject
import play.api.{Configuration, Environment}

@Singleton
class ApplicationConfig @Inject()(config: Configuration, environment: Environment) {

  private def baseUrl(serviceName: String) = {
    val protocol = config.getOptional[String](s"microservice.services.protocol").getOrElse("https")
    val host = config.get[String](s"microservice.services.$serviceName.host")
    val port = config.get[String](s"microservice.services.$serviceName.port")
    s"$protocol://$host:$port"
  }

  val desUrl = baseUrl("des")

  lazy val desToken = config.get[String]("microservice.services.des.auth-token")

  lazy val desEnv = {
    val env = config.get[String]("microservice.services.des.env")
    s"Env $env"
  }

  lazy val emailUrl = baseUrl("email")

  def currentTemplatePackageVersion = {
    val currentTemplate = config.get[String]("microservice.services.current-template-package-version")
    currentTemplate
  }

  def defaultTemplatePackageVersion = {
    val defaultTemplate = config.get[String]("microservice.services.default-template-package-version" )
    defaultTemplate
  }
}
