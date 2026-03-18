import sbt._
import play.sbt.PlayImport._
import play.core.PlayVersion

private object AppDependencies {

  private val playVersion = "play-30"
  private val bootstrapVersion = "10.7.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-$playVersion"        %  "2.11.0",
    "uk.gov.hmrc"       %% s"bootstrap-backend-$playVersion" % bootstrapVersion,
    "uk.gov.hmrc"       %% s"domain-$playVersion"            % "10.0.0",
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-$playVersion"        %  "2.12.0",
    "uk.gov.hmrc"       %% s"bootstrap-backend-$playVersion" % bootstrapVersion exclude("org.apache.commons", "commons-lang3"),
    "uk.gov.hmrc"       %% "domain"                          % "8.3.0-play-28"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"         %% s"bootstrap-test-$playVersion" % bootstrapVersion,
    "org.scalacheck"      %% "scalacheck"                   % "1.18.1",
    "org.scalatestplus"   %% "mockito-3-4"                  % "3.2.10.0"
  ).map(_ % "test")

  def apply(): Seq[ModuleID] = compile ++ test
}