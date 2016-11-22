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

package controllers

import exceptions.HttpStatusException
import models.{NotificationPushRequest, NotificationRecord}
import org.joda.time.{DateTimeZone, DateTime}
import play.api.Logger
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.mvc.{Action, Result}
import repositories.NotificationRepository
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait NotificationController extends BaseController {

  val amlsRegNoRegex = "^X[A-Z]ML00000[0-9]{6}$".r
  val prefix = "[NotificationController][post]"

  private[controllers] def notificationRepository: NotificationRepository

  private def toError(errors: Seq[(JsPath, Seq[ValidationError])]): JsObject =
    Json.obj(
      "errors" -> (errors map {
        case (path, error) =>
          Json.obj(
            "path" -> path.toJsonString,
            "error" -> error.head.message
          )
      })
    )

  private def toError(message: String): JsObject =
    Json.obj(
      "errors" -> Seq(message)
    )

  def saveNotification(amlsRegistrationNumber: String) =
    Action.async(parse.json) {
      implicit request =>
        Logger.debug(s"$prefix - amlsRegNo: $amlsRegistrationNumber")
        amlsRegNoRegex.findFirstIn(amlsRegistrationNumber) match {
          case Some(_) =>
            Json.fromJson[NotificationPushRequest](request.body) match {
              case JsSuccess(body, _) => {
                val record = NotificationRecord(amlsRegistrationNumber,
                  body.name,
                  body.email,
                  body.status,
                  body.contactType,
                  body.contactNumber,
                  body.variation,
                  DateTime.now(DateTimeZone.UTC)
                )
                notificationRepository.insertRecord(record) map {
                  response =>
                    Ok(Json.toJson(response))
                } recoverWith {
                  case e@HttpStatusException(status, Some(body)) =>
                    Logger.warn(s"$prefix - Status: ${status}, Message: $body")
                    Future.failed(e)
                }
              }
              case JsError(errors) =>
                Future.successful(BadRequest(toError(errors)))
            }
          case _ =>
            Future.successful {
              BadRequest(toError("Invalid AMLS Registration Number"))
            }
        }
    }

  def fetchNotifications(amlsRegistrationNumber: String) =
    Action.async {
      implicit request =>
        Logger.debug(s"$prefix - amlsRegNo: $amlsRegistrationNumber")
        amlsRegNoRegex.findFirstIn(amlsRegistrationNumber) match {
          case Some(_) =>
            notificationRepository.findByAmlsReference(amlsRegistrationNumber) map {
              response =>
                Logger.debug(s"$prefix - Response: ${Json.toJson(response)}")
                Ok(Json.toJson(response))
            } recoverWith {
              case e@HttpStatusException(status, Some(body)) =>
                Logger.warn(s"$prefix - Status: ${status}, Message: $body")
                Future.failed(e)
            }
          case _ =>
            Future.successful {
              BadRequest(toError("Invalid AMLS Registration Number"))
            }
        }
    }
}

object NotificationController extends NotificationController {
  // $COVERAGE-OFF$
  override private[controllers] val notificationRepository = NotificationRepository()
}
