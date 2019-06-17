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

package controllers

import audit.{NotificationFailedEvent, NotificationReceivedEvent}
import config.{AmlsConfig, MicroserviceAuditConnector}
import connectors.EmailConnector
import exceptions.HttpStatusException
import javax.inject.Inject
import models.{NotificationPushRequest, NotificationRecord}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logger
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.mvc.Action
import repositories.NotificationRepository
import uk.gov.hmrc.play.microservice.controller.BaseController
import utils.AuthAction

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class NotificationController @Inject()(
  emailConnector: EmailConnector,
  amlsConfig: AmlsConfig,
  msAuditConnector: MicroserviceAuditConnector,
  authAction: AuthAction
) extends BaseController {

  val amlsRegNoRegex = "^X[A-Z]ML00000[0-9]{6}$".r
  val safeIdRegex = "^[A-Za-z0-9]{15}$".r
  val prefix = "[NotificationController]"

  private[controllers] val notificationRepository = NotificationRepository()
  private[controllers] val audit = msAuditConnector

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

  //noinspection ScalaStyle
  def saveNotification(amlsRegistrationNumber: String) =
    authAction.async(parse.json) {
      implicit request =>
        Logger.debug(s"$prefix [saveNotification] - amlsRegNo: $amlsRegistrationNumber, body: ${request.body.toString}")
        amlsRegNoRegex.findFirstIn(amlsRegistrationNumber) match {
          case Some(_) =>
            Json.fromJson[NotificationPushRequest](request.body) match {
              case JsSuccess(body, _) =>

                val record = NotificationRecord(amlsRegistrationNumber,
                  body.safeId,
                  body.name,
                  body.email,
                  body.status,
                  body.contactType,
                  body.contactNumber,
                  body.variation,
                  DateTime.now(DateTimeZone.UTC),
                  isRead = false,
                  Some(amlsConfig.currentTemplatePackageVersion)
                )

                if (!body.isSane) {
                  // $COVERAGE-OFF$
                  Logger.warn(s"$prefix [saveNotification] - $amlsRegistrationNumber - malformed API 12 message received")
                  // $COVERAGE-ON$
                }

                notificationRepository.insertRecord(record) map {
                  case result if result.ok =>
                    emailConnector.sendNotificationReceivedTemplatedEmail(List(body.email))
                    audit.sendExtendedEvent(NotificationReceivedEvent(amlsRegistrationNumber, body))
                    NoContent
                  case result =>
                    Logger.error(s"$prefix [saveNotification] - Could not save notification results")

                    audit.sendExtendedEvent(NotificationFailedEvent(
                      amlsRegistrationNumber,
                      body,
                      result.writeErrors map { e => s"${e.code}: ${e.errmsg}" }
                    ))

                    InternalServerError
                } recoverWith {
                  case e@HttpStatusException(status, Some(exceptionBody)) =>
                    Logger.warn(s"$prefix [saveNotification] - Status: $status, Message: $exceptionBody")

                    audit.sendExtendedEvent(NotificationFailedEvent(
                      amlsRegistrationNumber,
                      body,
                      Seq(e.getMessage)
                    ))

                    Future.failed(e)
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

  def fetchNotifications(accountType: String, ref: String, amlsRegistrationNumber: String) =
    authAction.async {
      implicit request =>
        Logger.debug(s"$prefix [fetchNotifications] - amlsRegNo: $amlsRegistrationNumber")
        amlsRegNoRegex.findFirstIn(amlsRegistrationNumber) match {
          case Some(_) =>
            notificationRepository.findByAmlsReference(amlsRegistrationNumber) map {
              response =>
                val newResponse = response.map(x => x.copy(templatePackageVersion = x.templatePackageVersion orElse Some(amlsConfig.defaultTemplatePackageVersion)) )
                Logger.debug(s"$prefix [fetchNotifications] - Response: ${Json.toJson(newResponse)}")
                Ok(Json.toJson(newResponse))
            } recoverWith {
              case e@HttpStatusException(status, Some(body)) =>
                Logger.warn(s"$prefix [fetchNotifications] - Status: ${status}, Message: $body")
                Future.failed(e)
            }
          case _ =>
            Future.successful {
              BadRequest(toError("Invalid AMLS Registration Number"))
            }
        }
    }

  def fetchNotificationsBySafeId(accountType: String, ref: String, safeId: String) =
    authAction.async {
      implicit request =>
        Logger.debug(s"$prefix [fetchNotificationsBySafeId] - safeId: $safeId")
        safeIdRegex.findFirstIn(safeId) match {
          case Some(_) =>
            notificationRepository.findBySafeId(safeId) map {
              response =>
              val newResponse = response.map(x => x.copy(templatePackageVersion = x.templatePackageVersion orElse Some(amlsConfig.defaultTemplatePackageVersion)) )
              Logger.debug(s"$prefix [fetchNotificationsBySafeId] - Response: ${Json.toJson(newResponse)}")
              Ok(Json.toJson(newResponse))
            } recoverWith {
              case e@HttpStatusException(status, Some(body)) =>
                // $COVERAGE-OFF$
                Logger.warn(s"$prefix [fetchNotificationsBySafeId] - Status: ${status}, Message: $body")
                Future.failed(e)
              // $COVERAGE-ON$
            }
          case _ =>
            Future.successful {
              BadRequest(toError("Invalid SafeID"))
            }
        }
    }
}

