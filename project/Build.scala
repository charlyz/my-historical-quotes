import sbt.Keys._
import sbt._
import play.sbt._
import Play.autoImport._

object ApplicationBuild extends Build {
  val appDependencies = Seq(
    "com.typesafe.play" %% "play" % "2.5.12",
    "com.typesafe.slick" %% "slick" % "3.1.1",
    "com.github.tototoshi" %% "slick-joda-mapper" % "2.2.0",
    "com.h2database" % "h2" % "1.4.193",
    ws
  )

  val main = Project("my-historical-quotes", file("."))
    .enablePlugins(PlayScala)
    .settings(
      organization := "com.appnexus.api",
      libraryDependencies ++= appDependencies,
      scalaVersion := "2.11.8"
    )
}

