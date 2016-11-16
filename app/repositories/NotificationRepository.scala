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

package repositories

import models.NotificationRecord
import play.api.Logger
import play.modules.reactivemongo.MongoDbConnection
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.api.DefaultDB
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.mongo.{ReactiveRepository, Repository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait NotificationRepository extends Repository[NotificationRecord, BSONObjectID] {

  def insertRecord(notificationRequest: NotificationRecord):Future[Boolean]

}

class NotificationMongoRepository()(implicit mongo: () => DefaultDB)
  extends ReactiveRepository[NotificationRecord, BSONObjectID]("notification", mongo, NotificationRecord.format)
  with NotificationRepository{

  collection.indexesManager.ensure(Index(Seq("amlsRegistrationNumber" -> IndexType.Ascending), name = Some("amlsRegistrationNumber"), unique = true))

  override def insertRecord(notificationRequest: NotificationRecord):Future[Boolean] = {
    collection.insert(notificationRequest) map { lastError =>
      Logger.debug(s"[NotificationMongoRepository][insert] : { NotificationRequest : $notificationRequest" +
        s" , result: ${lastError.ok}, errors: ${lastError.errmsg} }")
      lastError.ok
    }
  }
}

object NotificationRepository extends MongoDbConnection {

  private lazy val notificationRepository = new NotificationMongoRepository

  def apply(): NotificationMongoRepository = notificationRepository
}

