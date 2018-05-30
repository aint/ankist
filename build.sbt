import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.github.aint",
      scalaVersion := "2.12.5",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "Ankist",

    libraryDependencies ++= Seq(
      "com.softwaremill.sttp" %% "core" % "1.1.14",
      scalaTest % Test
    )
  )
