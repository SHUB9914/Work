import de.johoop.cpd4sbt.CopyPasteDetector
import sbt.Keys._
import sbt._
import sbtassembly.AssemblyKeys._
import sbtassembly.{MergeStrategy, PathList}
import scoverage.ScoverageKeys


object CommonSettings {

  lazy val projectSettings = Seq(
    organization := "spok",
    scalaVersion := Dependencies.scala,
    resolvers += "OJO Snapshots" at "https://oss.jfrog.org/oss-snapshot-local",
    fork in Test := true,
    parallelExecution in Test := false,
    checksums in update := Nil,
    ScoverageKeys.coverageExcludedFiles := ".*SpokHttpServer.*; .*ApiHttpServer.*; .*NotificationHttpServer.*;" +
      ".*AccountsHttpServer.*; .*MailingApi.*; .*SearchHttpServer.*; .*GenerateData.*; .*TrendySpokActor.*; .*BootStrap.*; "+
    ".*Configuration.*; .*Scheduler.*; .*SearchService.*; .*LastSpokActor.*; .*PopularSpokerActor.*; .*AWSService.*; .*HttpUtil.*; .*MessagingHttpServer.*; ",
    test in assembly := {},
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", xs@_*) =>
        (xs map {
          _.toLowerCase
        }) match {
          case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) => MergeStrategy.discard
          case _ => MergeStrategy.first
        }
      case "application.conf" => MergeStrategy.concat
      case "reference.conf" => MergeStrategy.concat
      case _ => MergeStrategy.first
    }
  )


  def baseProject(name: String): Project = (
    Project(name, file(name))
      settings (projectSettings: _*)
    ).enablePlugins(CopyPasteDetector)

}
