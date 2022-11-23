import sbt._
import play.sbt.PlayImport._
import play.core.PlayVersion

private object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28" %  "0.70.0",
    ws,
    "uk.gov.hmrc" %% "bootstrap-backend-play-28" % "5.10.0",
    "uk.gov.hmrc" %% "domain" % "6.2.0-play-28",
    "com.github.ghik" % "silencer-lib" % "1.7.5" % Provided cross CrossVersion.full,
    compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.5" cross CrossVersion.full)
  )

  private val scalatestPlusPlayVersion = "5.1.0"
  private val scalaTestVersion = "3.2.9"
  private val pegdownVersion = "1.6.0"

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "org.scalatest" %% "scalatest" % scalaTestVersion % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalatestPlusPlayVersion,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.mockito" % "mockito-core" % "3.11.2" % scope,
        "org.scalacheck" %% "scalacheck" % "1.15.4" % scope,
        "org.scalatestplus" %% "mockito-3-4" % "3.2.9.0" % scope,
        "com.vladsch.flexmark" %  "flexmark-all" % "0.36.8" % scope
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
        "org.mockito" % "mockito-core" % "3.11.2" % scope
      )
    }.test
  }

  def apply() = compile ++ Test() ++ IntegrationTest()
}
