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

package utils

import models._
import org.joda.time.{LocalDateTime, DateTime}
import org.scalacheck.Gen

object DataGen {
  val amlsRegNumberGen = for {
    a <- Gen.alphaUpperChar
    b <- Gen.listOfN(6, Gen.numChar).map(_.mkString)
  } yield s"X${a}ML00000${b}"

  val statusTypeGen = Gen.oneOf(StatusType.Approved,
                                StatusType.Rejected,
                                StatusType.Revoked,
                                StatusType.DeRegistered,
                                StatusType.Expired)

  val statusReasonGen = Gen.oneOf(RejectedReason.NonCompliant, RejectedReason.FailedToRespond, RejectedReason.FailedToPayCharges,
    RejectedReason.FitAndProperFailure, RejectedReason.OtherFailed, RejectedReason.OtherRefused,
    RevokedReason.RevokedMissingTrader, RevokedReason.RevokedCeasedTrading, RevokedReason.RevokedNonCompliant,
    RevokedReason.RevokedFitAndProperFailure, RevokedReason.RevokedFailedToPayCharges,
    RevokedReason.RevokedFailedToRespond, RevokedReason.RevokedOther,
    DeregisteredReason.CeasedTrading, DeregisteredReason.HVDNoCashPayment, DeregisteredReason.OutOfScope,
    DeregisteredReason.NotTrading, DeregisteredReason.UnderAnotherSupervisor, DeregisteredReason.ChangeOfLegalEntity,
    DeregisteredReason.Other)

  val statusGen = for {
    statusType <- statusTypeGen
    statusReason <- Gen.option(statusReasonGen)
  } yield Status(statusType, statusReason)

  val contactTypeGen = Gen.oneOf(ContactType.RejectionReasons,
    ContactType.RevocationReasons,
    ContactType.MindedToReject,
    ContactType.NoLongerMindedToReject,
    ContactType.MindedToRevoke,
    ContactType.NoLongerMindedToRevoke,
    ContactType.Others,
    ContactType.ApplicationApproval,
    ContactType.RenewalApproval,
    ContactType.AutoExpiryOfRegistration,
    ContactType.RenewalReminder,
    ContactType.ReminderToPayForApplication,
    ContactType.ReminderToPayForRenewal,
    ContactType.ReminderToPayForVariation,
    ContactType.ReminderToPayForManualCharges)

  val daysInMonth: Int => Int = {
    case 2 => 28
    case x if Seq(9, 4, 6, 11).contains(x) => 30
    case _ => 31
  }

  val dateTimeGen = for {
    month <- Gen.choose(1, 12)
    day <- Gen.choose(1, daysInMonth(month))
    year <- Gen.choose(1967, 2020)
  } yield new DateTime(year, month, day, 0, 0)

  val localDateTimeGen = for {
    month <- Gen.choose(1, 12)
    day <- Gen.choose(1, daysInMonth(month))
    year <- Gen.choose(1967, 2020)
  } yield new LocalDateTime(year, month, day, 0, 0)

  val notificationRecordGen = for {
    a <- amlsRegNumberGen
    b <- Gen.alphaStr
    c <- Gen.alphaStr
    d <- Gen.alphaStr
    e <- Gen.option(statusGen)
    f <- Gen.option(contactTypeGen)
    g <- Gen.option(Gen.alphaStr)
    h <- Gen.oneOf(true, false)
    i <- dateTimeGen
    j <- Gen.oneOf(true,false)
  } yield NotificationRecord(a, b, c, d, e, f, g, h, i, j, Some("1"))

  object Des {
    import models.des._

    val notificationResponseGen = for {
      a <- localDateTimeGen
      b <- Gen.alphaStr
    } yield NotificationResponse(a, b)
  }
}
