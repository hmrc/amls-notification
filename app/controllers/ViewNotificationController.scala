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

import audit.NotificationReadEvent
import connectors.ViewNotificationConnector
import exceptions.HttpStatusException

import javax.inject.Inject
import models.NotificationRecord
import models.fe.NotificationDetails
import play.api.Logging
import play.api.libs.json.{JsObject, Json}
import utils.AuthAction
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import repositories.NotificationMongoRepository
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

class ViewNotificationController @Inject() (
  private[controllers] val connector: ViewNotificationConnector,
  private[controllers] val audit: AuditConnector,
  cc: ControllerComponents,
  authAction: AuthAction,
  private[controllers] val notificationRepository: NotificationMongoRepository
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  val amlsRegNoRegex = "^X[A-Z]ML00000[0-9]{6}$".r
  val prefix         = "[ViewNotificationController]"

  private def toError(message: String): JsObject =
    Json.obj(
      "errors" -> Seq(message)
    )

  def viewNotification(
    accountType: String,
    ref: String,
    amlsRegistrationNumber: String,
    notificationId: String
  ): Action[AnyContent] =
    authAction.async { implicit request =>
      logger.debug(s"$prefix[viewNotification] - amlsRegNo: $amlsRegistrationNumber - notificationId: $notificationId")

      amlsRegNoRegex.findFirstIn(amlsRegistrationNumber) match {
        case Some(_) =>
          notificationRepository.findById(notificationId) flatMap {
            case Some(record @ NotificationRecord(`amlsRegistrationNumber`, _, _, _, _, _, _, _, _, _, _, _)) =>
              {
                record match {
                  case record if record.contactNumber.isDefined =>
                    connector.getNotification(amlsRegistrationNumber, record.contactNumber.get) map { detail =>
                      val notificationDetails = NotificationDetails(
                        record.contactType,
                        record.status,
                        Some(detail.secureCommText),
                        record.variation,
                        record.receivedAt
                      )

                      logger.debug(s"$prefix[viewNotification] - sending: $notificationDetails")
                      audit.sendExtendedEvent(NotificationReadEvent(amlsRegistrationNumber, notificationDetails))
                      Ok(Json.toJson(notificationDetails))
                    }
                  case _                                        =>
                    Future.successful(
                      Ok(
                        Json.toJson(
                          NotificationDetails(
                            record.contactType,
                            record.status,
                            None,
                            record.variation,
                            record.receivedAt
                          )
                        )
                      )
                    )
                }
              }.andThen { case _ => markNotificationAsRead(notificationId) }
            case _                                                                                            => Future.successful(NotFound)
          }
        case None    => Future.successful(BadRequest(toError("Invalid AMLS Registration Number")))
      }
    }

  private def markNotificationAsRead(id: String): Future[Result] =
    notificationRepository.markAsRead(id) map { response =>
      Ok(Json.toJson(response))
    } recoverWith { case e @ HttpStatusException(status, _) =>
      logger.warn(s"$prefix - Failed to mark notification as read. Status: $status")
      Future.failed(e)
    }
}
