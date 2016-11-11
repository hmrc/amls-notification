package utils

import play.api.data.validation.ValidationError

trait MappingUtils {

  object Implicits {
    import play.api.libs.json.{Reads, JsSuccess, JsError}

    implicit def toReadsSuccess[A, B <: A](b: B): Reads[A] =
      Reads { _ => JsSuccess(b) }

    implicit def toReadsFailure[A](f: ValidationError): Reads[A] =
      Reads { _ => JsError(f) }
  }
}

object MappingUtils extends MappingUtils
