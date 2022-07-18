/*
 * Copyright 2022 HM Revenue & Customs
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
import com.google.inject.Singleton
import config.ApplicationConfig
import connectors.EmailConnector
import exceptions.HttpStatusException

import javax.inject.Inject
import models.{NotificationPushRequest, NotificationRecord}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logging
import play.api.libs.json._
import play.api.mvc.ControllerComponents
import repositories.NotificationMongoRepository
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class NotificationController @Inject()(private[controllers] val emailConnector: EmailConnector,
                                       amlsConfig: ApplicationConfig,
                                       private[controllers] val auditConnector: AuditConnector,
                                       private[controllers] val cc: ControllerComponents,
                                       private[controllers] val notificationRepository: NotificationMongoRepository) extends BackendController(cc) with Logging {

  val amlsRegNoRegex = "^X[A-Z]ML00000[0-9]{6}$".r
  val safeIdRegex = "^[A-Za-z0-9]{15}$".r
  val prefix = "[NotificationController]"

  private def toError(errors: Seq[(JsPath, Seq[JsonValidationError])]): JsObject =
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
    Action.async(parse.json) {
      implicit request =>
        logger.debug(s"$prefix [saveNotification] - amlsRegNo: $amlsRegistrationNumber, body: ${request.body.toString}")
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
                  logger.warn(s"$prefix [saveNotification] - $amlsRegistrationNumber - malformed API 12 message received")
                  // $COVERAGE-ON$
                }

                notificationRepository.insertRecord(record) map {
                  case result if result.ok =>
                    emailConnector.sendNotificationReceivedTemplatedEmail(List(body.email))
                    auditConnector.sendExtendedEvent(NotificationReceivedEvent(amlsRegistrationNumber, body))
                    NoContent
                  case result =>
                    logger.error(s"$prefix [saveNotification] - Could not save notification results")

                    auditConnector.sendExtendedEvent(NotificationFailedEvent(
                      amlsRegistrationNumber,
                      body,
                      result.writeErrors map { e => s"${e.code}: ${e.errmsg}" }
                    ))

                    InternalServerError
                } recoverWith {
                  case e@HttpStatusException(status, Some(exceptionBody)) =>
                    logger.warn(s"$prefix [saveNotification] - Status: $status, Message: $exceptionBody")

                    auditConnector.sendExtendedEvent(NotificationFailedEvent(
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
    Action.async {
        logger.debug(s"$prefix [fetchNotifications] - amlsRegNo: $amlsRegistrationNumber")
        amlsRegNoRegex.findFirstIn(amlsRegistrationNumber) match {
          case Some(_) =>
            notificationRepository.findByAmlsReference(amlsRegistrationNumber) map {
              response =>
                val newResponse = response.map(x => x.copy(templatePackageVersion = x.templatePackageVersion orElse Some(amlsConfig.defaultTemplatePackageVersion)) )
                logger.debug(s"$prefix [fetchNotifications] - Response: ${Json.toJson(newResponse)}")
                Ok(Json.toJson(newResponse))
            } recoverWith {
              case e@HttpStatusException(status, Some(body)) =>
                logger.warn(s"$prefix [fetchNotifications] - Status: ${status}, Message: $body")
                Future.failed(e)
            }
          case _ =>
            Future.successful {
              BadRequest(toError("Invalid AMLS Registration Number"))
            }
        }
    }

  def fetchNotificationsBySafeId(accountType: String, ref: String, safeId: String) = Action.async {
        logger.debug(s"$prefix [fetchNotificationsBySafeId] - safeId: $safeId")
        safeIdRegex.findFirstIn(safeId) match {
          case Some(_) =>
            notificationRepository.findBySafeId(safeId) map {
              response =>
                println(" inside response::"+response)
              val newResponse = response.map(x => x.copy(templatePackageVersion = x.templatePackageVersion orElse Some(amlsConfig.defaultTemplatePackageVersion)) )
              logger.debug(s"$prefix [fetchNotificationsBySafeId] - Response: ${Json.toJson(newResponse)}")
              Ok(Json.toJson(newResponse))
            } recoverWith {
              case e@HttpStatusException(status, Some(body)) =>
                println(" inside HttpStatusException::")
                // $COVERAGE-OFF$
                logger.warn(s"$prefix [fetchNotificationsBySafeId] - Status: ${status}, Message: $body")
                Future.failed(e)
              // $COVERAGE-ON$
            }
          case _ =>
            println(" inside case _ notification::")
            Future.successful {
              BadRequest(toError("Invalid SafeID"))
            }
        }
    }
}

