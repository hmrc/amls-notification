import sbt._
import play.sbt.PlayImport._
import play.core.PlayVersion

private object AppDependencies {

  private val playVersion = "play-30"
  private val bootstrapVersion = "9.11.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-$playVersion"        %  "2.6.0",
    "uk.gov.hmrc"       %% s"bootstrap-backend-$playVersion" % bootstrapVersion,
    "uk.gov.hmrc"       %% "domain"                          % "8.3.0-play-28",
    ws
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"         %% s"bootstrap-test-$playVersion" % bootstrapVersion,
    "org.scalacheck"      %% "scalacheck"                   % "1.18.1",
    "org.scalatestplus"   %% "mockito-3-4"                  % "3.2.10.0"
  ).map(_ % "test")

  def apply(): Seq[ModuleID] = compile ++ test
}