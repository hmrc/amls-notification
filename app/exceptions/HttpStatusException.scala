package exceptions

case class HttpStatusException(status: Int, body: Option[String]) extends Throwable
