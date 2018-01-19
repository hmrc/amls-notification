/*
 * Copyright 2018 HM Revenue & Customs
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

import models.{NotificationRecord, NotificationRow}
import play.api.Logger
import play.api.libs.json.{Json, OWrites}
import play.modules.reactivemongo.MongoDbConnection
import reactivemongo.api.DefaultDB
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.mongo.{ReactiveRepository, Repository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

trait NotificationRepository extends Repository[NotificationRecord, BSONObjectID] {

  def insertRecord(notificationRequest: NotificationRecord):Future[Boolean]
  def markAsRead(id: String):Future[Boolean]

  def findById(idString : String ) : Future[Option[NotificationRecord]]
  def findByAmlsReference(amlsReferenceNumber: String): Future[Seq[NotificationRow]]
  def findBySafeId(safeId: String): Future[Seq[NotificationRow]]
}

class NotificationMongoRepository()(implicit mongo: () => DefaultDB)
  extends ReactiveRepository[NotificationRecord, BSONObjectID]("notification", mongo, NotificationRecord.format)
  with NotificationRepository{

  override def indexes: Seq[Index] = {
    Seq(Index(Seq("receivedAt" -> IndexType.Ascending)))
  }

  override def insertRecord(notificationRequest: NotificationRecord):Future[Boolean] = {
    collection.insert(notificationRequest) map { lastError =>
      Logger.debug(s"[NotificationMongoRepository][insert] : { NotificationRequest : $notificationRequest" +
        s" , result: ${lastError.ok}, errors: ${WriteResult.lastError(lastError)} }")
      lastError.ok
    }
  }

  override def markAsRead(id: String):Future[Boolean] = {

    val modifier = Json.obj("$set" -> Json.obj("isRead" -> true))

    collection.update(Json.obj("_id" -> Json.toJsFieldJsValueWrapper(BSONObjectID(id))(idFormatImplicit)), modifier).
      map { lastError =>
        Logger.debug(s"[NotificationMongoRepository][update] : { ID : $id" +
          s" , result: ${lastError.ok}, errors: ${lastError.errmsg} }")
        lastError.ok
      }

  }

  def findById(idString : String) : Future[Option[NotificationRecord]] = {
    Try {
      BSONObjectID(idString)
    } .map { id: BSONObjectID => findById(id) }
      .recover { case _: IllegalArgumentException => Future.successful(None) }
      .get
  }

  override def findByAmlsReference(amlsReferenceNumber: String) = {
    collection.find(Json.obj("amlsRegistrationNumber" -> amlsReferenceNumber)).
      sort(Json.obj("receivedAt" -> -1)).cursor[NotificationRow]().collect[Seq]()
  }

  override def findBySafeId(safeId: String) = collection.find {
    Json.obj("safeId" -> safeId)
  }.sort(Json.obj("receivedAt" -> -1)).cursor[NotificationRow]().collect[Seq]()
}

object NotificationRepository extends MongoDbConnection {

  private lazy val notificationRepository = new NotificationMongoRepository

  def apply(): NotificationMongoRepository = notificationRepository
}
