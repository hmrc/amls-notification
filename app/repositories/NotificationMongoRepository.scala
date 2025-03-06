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

package repositories

import com.google.inject.{Inject, Singleton}
import models.{IDType, NotificationRecord, NotificationRow}
import org.bson.types.ObjectId
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model._
import play.api.Logging
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NotificationMongoRepository @Inject() (mongo: MongoComponent)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[NotificationRecord](
      mongoComponent = mongo,
      collectionName = "notification",
      domainFormat = NotificationRecord.format,
      indexes = Seq(
        IndexModel(
          ascending("receivedAt"),
          IndexOptions().name("receivedAt_1")
        ),
        IndexModel(
          keys = Indexes.ascending("amlsRegistrationNumber"),
          indexOptions = IndexOptions().name("amlsRegistrationNumber_index").unique(false)
        ),
        IndexModel(
          keys = Indexes.ascending("safeId"),
          indexOptions = IndexOptions().name("safeId_index").unique(false)
        )
      )
    )
    with Logging {

  def insertRecord(notificationRequest: NotificationRecord): Future[Boolean] =
    collection
      .insertOne(notificationRequest)
      .toFuture()
      .map(_.wasAcknowledged)

  def markAsRead(id: String): Future[Boolean] = {
    import models.NotificationRecord.objectIdFormat
    val query = Filters.eq("_id", Codecs.toBson(new ObjectId(id)))
    collection
      .updateOne(
        filter = query,
        update = Updates.set("isRead", true)
      )
      .toFuture()
      .map(_.wasAcknowledged())
  }

  def findById(idString: String): Future[Option[NotificationRecord]] = {
    import models.NotificationRecord.objectIdFormat
    val query = Filters.eq("_id", Codecs.toBson(new ObjectId(idString)))
    collection
      .findOneAndUpdate(
        filter = query,
        update = Updates.set("isRead", true),
        options = FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
      )
      .toFutureOption()
  }

  def findByAmlsReference(amlsReferenceNumber: String): Future[Seq[NotificationRow]] =
    collection
      .find(Filters.eq("amlsRegistrationNumber", amlsReferenceNumber))
      .sort(Sorts.descending("receivedAt"))
      .toFuture()
      .map(
        _.map(result =>
          NotificationRow(
            result.status,
            result.contactType,
            result.contactNumber,
            result.variation,
            result.receivedAt,
            result.isRead,
            result.amlsRegistrationNumber,
            result.templatePackageVersion,
            IDType(result._id.toString)
          )
        )
      )

  def findBySafeId(safeId: String): Future[Seq[NotificationRow]] =
    collection
      .find(Filters.eq("safeId", safeId))
      .sort(Sorts.descending("receivedAt"))
      .toFuture()
      .map(
        _.map(result =>
          NotificationRow(
            result.status,
            result.contactType,
            result.contactNumber,
            result.variation,
            result.receivedAt,
            result.isRead,
            result.amlsRegistrationNumber,
            result.templatePackageVersion,
            IDType(result._id.toString)
          )
        )
      )
}
