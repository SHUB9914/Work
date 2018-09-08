import CommonSettings._
import Dependencies._
import scoverage.ScoverageKeys

name := "spok-api"

version := "1.0"

scalaVersion := scala

def compile(deps: Seq[ModuleID]): Seq[ModuleID] = deps map (_ % "compile")
def provided(deps: Seq[ModuleID]): Seq[ModuleID] = deps map (_ % "provided")
def test(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "test")

lazy val root = (
  project.in(file("."))
    aggregate(common, api, spok, persistence, notification, accounts, search, scheduler, messaging)
  )

lazy val api = (
  baseProject("api")
    settings(libraryDependencies ++= compile(apiDependencies) ++ test(scalaTest, akkaHttpTest, mock),
    ScoverageKeys.coverageMinimum := 85,
    ScoverageKeys.coverageFailOnMinimum := true
    )
  ) dependsOn (common, persistence)

lazy val common = (
  baseProject("common")
    settings(libraryDependencies ++= compile(commonModuleDependencies) ++ test(scalaTest, akkaHttpTest, mock),
    ScoverageKeys.coverageMinimum := 90,
    ScoverageKeys.coverageFailOnMinimum := true
    )
  )

lazy val persistence = (
  baseProject("persistence")
    settings(libraryDependencies ++= compile(persistenceDependencies) ++ test(scalaTest, akkaHttpTest, mock),
    ScoverageKeys.coverageMinimum := 80,
    ScoverageKeys.coverageFailOnMinimum := true
    )
  ) dependsOn (common)


lazy val spok = (
  baseProject("spok")
    settings(libraryDependencies ++= compile(spokDependencies) ++ test(scalaTest, akkaHttpTest, mock),
    ScoverageKeys.coverageMinimum := 90,
    ScoverageKeys.coverageFailOnMinimum := true
    )
  ) dependsOn(common, persistence)

lazy val notification = (
  baseProject("notification")
    settings(libraryDependencies ++= compile(notificationDependencies) ++ test(scalaTest, akkaHttpTest, mock),
    ScoverageKeys.coverageMinimum := 90,
    ScoverageKeys.coverageFailOnMinimum := true
    )
  ) dependsOn(common, persistence)

lazy val accounts = (
  baseProject("accounts")
    settings(libraryDependencies ++= compile(accountsDependencies) ++ test(scalaTest, akkaHttpTest, mock),
    ScoverageKeys.coverageMinimum := 85,
    ScoverageKeys.coverageFailOnMinimum := true
    )
  ) dependsOn(common, persistence)

lazy val search = (
  baseProject("search")
    settings(libraryDependencies ++= compile(akkaDependencies) ++ test(scalaTest, akkaHttpTest, mock),
    ScoverageKeys.coverageMinimum := 90,
    ScoverageKeys.coverageFailOnMinimum := true
    )
  ) dependsOn(common, persistence)

lazy val scheduler = (
  baseProject("scheduler")
    settings(libraryDependencies ++= compile(schedulerDependencies) ++ test(scalaTest, akkaHttpTest, mock),
    ScoverageKeys.coverageMinimum := 90,
    ScoverageKeys.coverageFailOnMinimum := true
    )
  ) dependsOn(common, persistence)

lazy val messaging = (
  baseProject("messaging")
    settings(libraryDependencies ++= compile(akkaDependencies) ++ test(scalaTest, akkaHttpTest, mock),
    ScoverageKeys.coverageMinimum := 90,
    ScoverageKeys.coverageFailOnMinimum := true
    )
  ) dependsOn(common, persistence)

