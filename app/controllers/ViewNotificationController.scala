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
  private[controllers] def repo: NotificationRepository


  val amlsRegNoRegex = "^X[A-Z]ML00000[0-9]{6}$".r
  val prefix = "[ViewNotificationController]"

  private def toError(message: String): JsObject =
    Json.obj(
      "errors" -> Seq(message)
    )

  def viewNotification(amlsRegistrationNumber: String, notificationId: String) =
    Action.async {
      implicit request =>
        Logger.debug(s"$prefix[viewNotification] - amlsRegNo: $amlsRegistrationNumber - notificationId: $notificationId")

        repo.findById(notificationId) flatMap {
          case Some(record) => record.contactNumber.fold (???) {contactNumber =>
            connector.getNotification(amlsRegistrationNumber, contactNumber) map { detail =>
              Ok(Json.toJson(NotificationDetails(
                                record.contactType,
                                record.status flatMap {_.status},
                                record.status flatMap {_.statusReason},
                                detail.secureCommText)))
            }
          }
          case None => Future.successful(NotFound)
        }
    }
}

object ViewNotificationController extends ViewNotificationController{
  // $COVERAGE-OFF$
  override private[controllers] val connector = DESConnector
  override private[controllers] val repo = NotificationRepository()
}
