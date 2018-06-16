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

      "io.vertx" %% "vertx-lang-scala"       % "3.5.2",
      "io.vertx" %% "vertx-web-client-scala" % "3.5.2",

      scalaTest % Test
    )
  )

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case PathList("META-INF", xs @ _*) => MergeStrategy.last
  case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.last
  case PathList("codegen.json") => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
