package metrics

sealed trait APITypes {
  def key: String
}

case object API12 extends APITypes {
  override val key: String = "etmp-amls-registration-view"
}
