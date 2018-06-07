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
      "com.softwaremill.sttp" %% "core"   % "1.1.14",
      "com.softwaremill.sttp" %% "json4s" % "1.1.14",

      scalaTest % Test
    )
  )

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}
