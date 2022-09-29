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

package repositories

import com.google.inject.{Inject, Singleton}
import com.mongodb.ErrorCategory
import models.{NotificationRecord, NotificationRow}
import org.mongodb.scala.MongoWriteException
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{Filters, FindOneAndUpdateOptions, IndexModel, IndexOptions, ReturnDocument, Sorts, Updates}
import play.api.Logging
import play.api.libs.json.{JsObject, JsValue, Json, Writes}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class NotificationMongoRepository @Inject()(mongo: MongoComponent)
  extends PlayMongoRepository[NotificationRecord](
    mongoComponent = mongo,
    collectionName ="notification",
    domainFormat =NotificationRecord.format,
    indexes = Seq(IndexModel(ascending("receivedAt"), IndexOptions()
      .name("receivedAt")
    ),
  ))with Logging
{

   def insertRecord(notificationRequest: NotificationRecord): Future[Boolean] = {
    collection
      .insertOne(notificationRequest)
      .toFuture()
      .map(_.wasAcknowledged)
      .recoverWith {
        case e: MongoWriteException if e.getError.getCategory == ErrorCategory.DUPLICATE_KEY =>
          Future.failed(new IllegalArgumentException("NINO and UTR must both be unique"))
      }
  }

  def markAsRead(id: String): Future[Boolean] = {
    collection
      .findOneAndUpdate(
        filter = Filters.eq("_id",id),
        update = Updates.set("isRead", true),
        options = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
      ).toFuture().map{ result =>
      if(result.isRead == true){
        true
      }else false
    }
  }

  def findById(idString: String): Future[Option[NotificationRecord]] = {
    collection.find(Filters.eq("objId",idString)).toFuture().map(_.headOption)
  }

   def findByAmlsReference(amlsReferenceNumber: String) = {

     collection.find(Filters.eq("amlsRegistrationNumber",amlsReferenceNumber)).sort(Sorts.descending("receivedAt"))
       .collect()
       .toFuture()
       .map(result => result)
  }

  def findBySafeId(safeId: String) = collection.find (Filters.eq("safeId",safeId)).sort(Sorts.descending("receivedAt"))
    .collect()
    .toFuture()
    .map(result => result)
}
