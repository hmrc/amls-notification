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

import connectors.{DESConnector, ViewNotificationConnector}
import exceptions.HttpStatusException
import models.{NotificationRecord, StatusType}
import models.fe.NotificationDetails
import play.api.Logger
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Action
import repositories.NotificationRepository
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ViewNotificationController extends BaseController {

  private[controllers] def connector: ViewNotificationConnector  // $COVERAGE-OFF$

  private[controllers] def notificationRepository: NotificationRepository


  val amlsRegNoRegex = "^X[A-Z]ML00000[0-9]{6}$".r
  val prefix = "[ViewNotificationController]"

  private def toError(message: String): JsObject =
    Json.obj(
      "errors" -> Seq(message)
    )


  def viewNotification(accountType:String, ref:String, amlsRegistrationNumber: String, notificationId: String) =
    Action.async {
      implicit request =>
        Logger.debug(s"$prefix[viewNotification] - amlsRegNo: $amlsRegistrationNumber - notificationId: $notificationId")

        amlsRegNoRegex.findFirstIn(amlsRegistrationNumber) match {
          case Some(_) => notificationRepository.findById(notificationId) flatMap {
            case Some(record@NotificationRecord(`amlsRegistrationNumber`, _,_,_,_,_,_,_,_,_)) => {
                record.contactNumber.fold (
                  Future.successful(Ok(Json.toJson(NotificationDetails(
                    record.contactType,
                    record.status.fold[Option[StatusType]](None)(x => Some(x.status)),
                    record.status flatMap {_.statusReason},
                    None))))
                ) {contactNumber =>
                  connector.getNotification(amlsRegistrationNumber, contactNumber) map { detail =>
                    Ok(Json.toJson(NotificationDetails(
                      record.contactType,
                      record.status.fold[Option[StatusType]](None)(x => Some(x.status)),
                      record.status flatMap {_.statusReason},
                      Some(detail.secureCommText))))
                  }
                }
              }
            case _ => Future.successful(NotFound)
          }
          case None => Future.successful(BadRequest(toError("Invalid AMLS Registration Number")))
        }
    }

  def markNotificationAsRead(id: String) = {
    Action.async {
      implicit request =>
        notificationRepository.markAsRead(id) map {
          response =>
            Ok(Json.toJson(response))
        } recoverWith {
          case e@HttpStatusException(status, Some(body)) =>
            Logger.warn(s"$prefix - Status: ${status}, Message: $body")
            Future.failed(e)

        }
    }
  }
}

object ViewNotificationController extends ViewNotificationController{
  // $COVERAGE-OFF$
  override private[controllers] val connector = DESConnector
  override private[controllers] val notificationRepository = NotificationRepository()
}
