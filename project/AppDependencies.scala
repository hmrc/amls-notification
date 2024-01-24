import sbt._
import play.sbt.PlayImport._
import play.core.PlayVersion

private object AppDependencies {

  val bootstrapVersion = "8.4.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28" %  "1.7.0",
    ws,
    "uk.gov.hmrc" %% "bootstrap-backend-play-28" % bootstrapVersion,
    "uk.gov.hmrc" %% "domain" % "8.3.0-play-28"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply(): Seq[sbt.ModuleID] = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "bootstrap-test-play-28" % bootstrapVersion % scope,
        "org.scalacheck" %% "scalacheck" % "1.17.0" % scope,
        "org.scalatestplus" %% "mockito-3-4" % "3.2.10.0" % scope,
        "com.vladsch.flexmark" %  "flexmark-all" % "0.62.2" % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test()
}
