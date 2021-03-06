import sbt._

private object AppDependencies {
  import play.sbt.PlayImport._
  import play.core.PlayVersion

  private val bootstrapVersion = "2.24.0"
  private val domainVersion = "5.9.0-play-26"
  private val pegdownVersion = "1.6.0"
  private val authVersion = "3.0.0-play-26"
  private val simpleReactivemongoVersion = "7.30.0-play-26"

  val compile = Seq(
    "uk.gov.hmrc" %% "simple-reactivemongo" % simpleReactivemongoVersion,
    ws,
    "uk.gov.hmrc" %% "bootstrap-backend-play-26" % bootstrapVersion,
    "uk.gov.hmrc" %% "domain" % domainVersion,
    "uk.gov.hmrc" %% "auth-client" % authVersion
  )

  private val scalatestPlusPlayVersion = "3.1.2"
  private val scalaTestVersion = "3.0.9"

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "org.scalatest" %% "scalatest" % scalaTestVersion % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalatestPlusPlayVersion,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.mockito" % "mockito-core" % "3.0.0" % scope,
        "org.scalacheck" %% "scalacheck" % "1.14.0" % scope
      )
    }.test
  }

  object IntegrationTest {
    def apply() = new TestDependencies {

      override lazy val scope: String = "it"

      override lazy val test = Seq(
        "org.scalatest" %% "scalatest" % scalaTestVersion % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalatestPlusPlayVersion,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.mockito" % "mockito-core" % "3.0.0" % scope
      )
    }.test
  }

  def apply() = compile ++ Test() ++ IntegrationTest()
}
