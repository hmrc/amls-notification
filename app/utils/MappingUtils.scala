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

package utils

import play.api.data.validation.ValidationError
import play.api.libs.json._

trait MappingUtils {

  object Implicits {
    import play.api.libs.json.{Reads, JsSuccess, JsError}

    implicit def toReadsSuccess[A, B <: A](b: B): Reads[A] =
      Reads { _ => JsSuccess(b) }

    implicit def toReadsFailure[A](f: ValidationError): Reads[A] =
      Reads { _ => JsError(f) }

    implicit class PathAdditions(path: JsPath) {

      def readNullable1[T](implicit r: Reads[T]): Reads[Option[T]] = readNullable1(path)(r)

      private def readNullable1[A](path: JsPath)(implicit reads: Reads[A]) = Reads[Option[A]] { json =>
        path.applyTillLast(json).fold(
          jsError => JsSuccess(None),
          jsResult => jsResult.fold(
            _ => JsSuccess(None),
            a => a match {
              case JsNull => JsSuccess(None)
              case js => reads.reads(js).repath(path).map(Some(_))
            }
          )
        )
      }
    }
  }
}

object MappingUtils extends MappingUtils
