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

package repositories

import com.google.inject.{Inject, Singleton}
import models.{NotificationRecord, NotificationRow}
import play.api.Logger
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.Cursor
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class NotificationMongoRepository @Inject()(component: ReactiveMongoComponent)
  extends ReactiveRepository[NotificationRecord, BSONObjectID]("notification", component.mongoConnector.db, NotificationRecord.format) {

  override def indexes: Seq[Index] = {
    Seq(Index(Seq("receivedAt" -> IndexType.Ascending)))
  }

   def insertRecord(notificationRequest: NotificationRecord): Future[WriteResult] = {
    collection.insert(notificationRequest) map { writeResult =>
      Logger.debug(s"[NotificationMongoRepository][insert] : { NotificationRequest : $notificationRequest" +
        s" , result: ${writeResult.ok}, errors: ${WriteResult.lastError(writeResult)} }")
      writeResult
    }
  }

   def markAsRead(id: String): Future[Boolean] = {

    val modifier = Json.obj("$set" -> Json.obj("isRead" -> true))

     BSONObjectID.parse(id).map { objId: BSONObjectID =>
       collection.update(Json.obj("_id" -> Json.toJsFieldJsValueWrapper(objId)(idFormatImplicit)), modifier).
         map { lastError =>
           Logger.debug(s"[NotificationMongoRepository][update] : { ID : $id" +
             s" , result: ${lastError.ok}, errors: ${lastError.errmsg} }")
           lastError.ok
         }
     } .recover {
       case _: IllegalArgumentException => Future.successful(false)
     } .get
  }

  def findById(idString: String): Future[Option[NotificationRecord]] = {

    BSONObjectID.parse(idString).map { objId: BSONObjectID =>
      findById(objId)
    } .recover {
      case _: IllegalArgumentException => Future.successful(None)
    } .get

  }

   def findByAmlsReference(amlsReferenceNumber: String) = {
     val query = Json.obj("amlsRegistrationNumber" -> amlsReferenceNumber)

    collection.find(query).
      sort(Json.obj("receivedAt" -> -1)).cursor[NotificationRow]().collect[Seq](100, Cursor.FailOnError())
  }

   def findBySafeId(safeId: String) = collection.find {
    Json.obj("safeId" -> safeId)
  }.sort(Json.obj("receivedAt" -> -1)).cursor[NotificationRow]().collect[Seq](100, Cursor.FailOnError())
}
