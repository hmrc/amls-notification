package repositories

import models.NotificationPushRequest
import play.api.Logger
import play.api.libs.json.Json
import play.modules.reactivemongo.MongoDbConnection
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.api.DefaultDB
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.mongo.{ReactiveRepository, Repository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait NotificationRepository extends Repository[NotificationPushRequest, BSONObjectID] {

  def insert(notificationRequest: NotificationPushRequest):Future[Boolean]

}

class NotificationMongoRepository()(implicit mongo: () => DefaultDB)
  extends ReactiveRepository[NotificationPushRequest, BSONObjectID]("notification", mongo, NotificationPushRequest.formats)
  with NotificationRepository{

  collection.indexesManager.ensure(Index(Seq("registrationNumber" -> IndexType.Ascending), name = Some("registrationNumber"), unique = true))

/*  override def insert(notificationRequest: NotificationPushRequest):Future[Boolean] = {
    collection.update(selector = Json.obj("registrationNumber" -> ""),
      update = Json.obj("$set" -> Json.toJson(notificationRequest)),
      upsert = true).map {
      lastError =>
        Logger.debug(s"[NotificationMongoRepository][insertByRegistrationNumber] : { statusNotification: $notificationRequest, result: ${lastError.ok}, errors: ${lastError.errmsg} }")
        lastError.ok
    }
  }*/

  override def insert(feeResponse: NotificationPushRequest):Future[Boolean] = {
    collection.insert(feeResponse) map { lastError =>
      Logger.debug(s"[FeeResponseMongoRepository][insert] : { feeResponse : $feeResponse , result: ${lastError.ok}, errors: ${lastError.errmsg} }")
      lastError.ok
    }
  }
}

object NotificationRepository extends MongoDbConnection {

  private lazy val notificationRepository = new NotificationMongoRepository

  def apply(): NotificationMongoRepository = notificationRepository
}
