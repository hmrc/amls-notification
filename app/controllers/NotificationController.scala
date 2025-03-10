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

package controllers

import audit.{NotificationFailedEvent, NotificationReceivedEvent}
import com.google.inject.Singleton
import config.ApplicationConfig
import connectors.EmailConnector
import exceptions.HttpStatusException
import models.ContactType.{NewRenewalReminder, RenewalReminder}
import models.{ContactType, NotificationPushRequest, NotificationRecord}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logging
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import repositories.NotificationMongoRepository
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

@Singleton
class NotificationController @Inject() (
  private[controllers] val emailConnector: EmailConnector,
  amlsConfig: ApplicationConfig,
  private[controllers] val auditConnector: AuditConnector,
  private[controllers] val cc: ControllerComponents,
  private[controllers] val notificationRepository: NotificationMongoRepository
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  val amlsRegNoRegex: Regex = "^X[A-Z]ML00000[0-9]{6}$".r
  val safeIdRegex: Regex    = "^[A-Za-z0-9]{15}$".r
  val prefix                = "[NotificationController]"

  private def toError(errors: Seq[(JsPath, Seq[JsonValidationError])]): JsObject =
    Json.obj(
      "errors" -> (errors map { case (path, error) =>
        Json.obj(
          "path"  -> path.toJsonString,
          "error" -> error.head.message
        )
      })
    )

  private def toError(message: String): JsObject =
    Json.obj(
      "errors" -> Seq(message)
    )

  // noinspection ScalaStyle
  def saveNotification(amlsRegistrationNumber: String): Action[JsValue] =
    Action.async(parse.json) { implicit request =>
      logger.debug(s"$prefix [saveNotification] - amlsRegNo: $amlsRegistrationNumber, body: ${request.body.toString}")
      amlsRegNoRegex.findFirstIn(amlsRegistrationNumber) match {
        case Some(_) =>
          Json.fromJson[NotificationPushRequest](request.body) match {
            case JsSuccess(body, _)                                                           =>
              val contactType = getContactType(
                body.contactType,
                DateTime.now(DateTimeZone.UTC),
                amlsConfig.currentTemplatePackageVersion
              )
              val record      = NotificationRecord(
                amlsRegistrationNumber,
                body.safeId,
                body.name,
                body.email,
                body.status,
                contactType,
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
                case result if result =>
                  emailConnector.sendNotificationReceivedTemplatedEmail(List(body.email))
                  auditConnector.sendExtendedEvent(NotificationReceivedEvent(amlsRegistrationNumber, body))
                  NoContent
              } recoverWith { case e @ HttpStatusException(status, Some(exceptionBody)) =>
                logger.warn(s"$prefix [saveNotification] - Status: $status, Message: $exceptionBody")

                auditConnector.sendExtendedEvent(
                  NotificationFailedEvent(
                    amlsRegistrationNumber,
                    body,
                    Seq(e.getMessage)
                  )
                )

                Future.failed(e)
              }
            case JsError(errors: immutable.Seq[(JsPath, immutable.Seq[JsonValidationError])]) =>
              Future.successful(BadRequest(toError(errors)))
          }
        case _       =>
          Future.successful {
            BadRequest(toError("Invalid AMLS Registration Number"))
          }
      }
    }

  def fetchNotifications(accountType: String, ref: String, amlsRegistrationNumber: String): Action[AnyContent] =
    Action.async {
      logger.debug(s"$prefix [fetchNotifications] - amlsRegNo: $amlsRegistrationNumber")
      amlsRegNoRegex.findFirstIn(amlsRegistrationNumber) match {
        case Some(_) =>
          notificationRepository.findByAmlsReference(amlsRegistrationNumber) map { response =>
            val newResponse = response.map(x =>
              x.copy(
                templatePackageVersion = x.templatePackageVersion orElse Some(amlsConfig.defaultTemplatePackageVersion)
              )
            )
            logger.debug(s"$prefix [fetchNotifications] - Response: ${Json.toJson(newResponse)}")
            Ok(Json.toJson(newResponse))
          } recoverWith { case e @ HttpStatusException(status, Some(body)) =>
            logger.warn(s"$prefix [fetchNotifications] - Status: $status, Message: $body")
            Future.failed(e)
          }
        case _       =>
          Future.successful {
            BadRequest(toError("Invalid AMLS Registration Number"))
          }
      }
    }

  def fetchNotificationsBySafeId(accountType: String, ref: String, safeId: String): Action[AnyContent] = Action.async {
    logger.debug(s"$prefix [fetchNotificationsBySafeId] - safeId: $safeId")
    safeIdRegex.findFirstIn(safeId) match {
      case Some(_) =>
        notificationRepository.findBySafeId(safeId) map { response =>
          val newResponse = response.map(x =>
            x.copy(templatePackageVersion =
              x.templatePackageVersion orElse Some(amlsConfig.defaultTemplatePackageVersion)
            )
          )
          logger.debug(s"$prefix [fetchNotificationsBySafeId] - Response: ${Json.toJson(newResponse)}")
          Ok(Json.toJson(newResponse))
        } recoverWith { case e @ HttpStatusException(status, Some(body)) =>
          // $COVERAGE-OFF$
          logger.warn(s"$prefix [fetchNotificationsBySafeId] - Status: $status, Message: $body")
          Future.failed(e)
        // $COVERAGE-ON$
        }
      case _       =>
        Future.successful {
          BadRequest(toError("Invalid SafeID"))
        }
    }
  }

  def getContactType(contactType: Option[ContactType], date: DateTime, templateVersion: String): Option[ContactType] = {
    val boundaryDay = date.dayOfMonth().getMaximumValue - (28 + 14) / 2
    templateVersion match {
      case "v6m0" =>
        contactType match {
          case Some(RenewalReminder) if date.getDayOfMonth >= boundaryDay =>
            Some(NewRenewalReminder)
          case _                                                          => contactType
        }
      case _      => contactType
    }

  }
}
