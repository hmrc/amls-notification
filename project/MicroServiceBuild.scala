import sbt._

object MicroServiceBuild extends Build with MicroService {

  val appName = "amls-notification"

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {
  import play.sbt.PlayImport._
  import play.core.PlayVersion

  private val microserviceBootstrapVersion = "4.12.0"
  private val microserviceBootstrapVersion2 = "0.39.0"
  private val domainVersion = "5.6.0-play-25"
  private val domainVersion2 = "5.6.0-play-26"
  private val hmrcTestVersion = "3.8.0-play-25"
  private val hmrcTestVersion2 = "3.8.0-play-26"
  private val scalaTestVersion = "3.0.5"
  private val pegdownVersion = "1.6.0"
  private val playUiVersion = "7.4.0"

  private val playReactivemongoVersion = "7.19.0-play-26"
  private val scalatestPlusPlayVersion = "2.0.1"
  private val scalatestPlusPlayVersion2 = "3.1.2"

//  "uk.gov.hmrc" %% "simple-reactivemongo" % "7.9.0-play-26",

  val compile = Seq(
    "uk.gov.hmrc" %% "simple-reactivemongo" % playReactivemongoVersion,
    ws,
    "uk.gov.hmrc" %% "bootstrap-play-26" % microserviceBootstrapVersion2,
    "uk.gov.hmrc" %% "domain" % domainVersion2
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion2 % scope,
        "org.scalatest" %% "scalatest" % scalaTestVersion % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalatestPlusPlayVersion,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.mockito" % "mockito-core" % "2.1.0" % scope,
        "org.scalacheck" %% "scalacheck" % "1.12.5" % scope
      )
    }.test
  }

  object IntegrationTest {
    def apply() = new TestDependencies {

      override lazy val scope: String = "it"

      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.scalatest" %% "scalatest" % scalaTestVersion % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalatestPlusPlayVersion,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.mockito" % "mockito-core" % "2.1.0" % scope
      )
    }.test
  }

  def apply() = compile ++ Test() ++ IntegrationTest()
}
