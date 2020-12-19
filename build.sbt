import NativePackagerHelper._
import com.typesafe.sbt.packager.docker._
import TodoListPlugin._

ThisBuild / scalaVersion := "2.13.3"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "q2io"
ThisBuild / organizationName := "q2io"
lazy val supportedScalaVersions = List("2.12.12", "2.13.3")

lazy val projectSettings = Seq(
  bloopAggregateSourceDependencies in Global := true,
  bloopExportJarClassifiers in Global := Some(Set("sources")),
  scalafmtOnCompile := true,
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots"),
    "jitpack" at "https://jitpack.io"
  ),
  libraryDependencies ++= Seq(
    compilerPlugin(Dependencies.kindProject cross CrossVersion.full),
    compilerPlugin(Dependencies.betterMonadicFor)
  ),
  crossScalaVersions := supportedScalaVersions,
  Test / fork := true,
  parallelExecution := false,
  Test / testForkedParallel := false
)

lazy val dockerSettings = Seq(
  version in Docker := s"""${version.value}-${scala.sys.props
    .getOrElse(" build_number", "local-build")}""",
  dockerRepository := Some(organization.value),
  dockerUpdateLatest := true
)

lazy val commonSettings = projectSettings ++ CompilerSettings.options

compileWithTodolistSettings

lazy val root = (project in file("."))
  .aggregate(domain, core, cli, http, checkout, modules)
  .settings(
    publish / skip := true
  )

lazy val domain = (project in file("01-domain"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++=
      Dependencies.core ++
        Dependencies.test
  )
  .settings(dockerSettings: _*)
  .enablePlugins(JavaAppPackaging)

lazy val core = (project in file("02-core"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++=
      Dependencies.core ++
        Dependencies.test
  )
  .settings(dockerSettings: _*)
  .enablePlugins(JavaAppPackaging)
  .dependsOn(domain % "compile->compile;test->test")

lazy val checkout = (project in file("03-checkout"))
  .settings(commonSettings: _*)
  .settings(dockerSettings: _*)
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)
  .settings(
    libraryDependencies ++= Dependencies.core ++ Dependencies.api ++ Dependencies.test
  )
  .dependsOn(
    domain % "compile->compile;test->test",
    core % "compile->compile;test->test"
  )

lazy val cli = (project in file("04-cli"))
  .settings(commonSettings: _*)
  .settings(dockerSettings: _*)
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)
  .settings(libraryDependencies ++= Dependencies.core ++ Dependencies.test)
  .dependsOn(
    domain % "compile->compile;test->test",
    core % "compile->compile;test->test",
    checkout % "compile->compile;test->test"
  )

lazy val http = (project in file("04-http"))
  .settings(commonSettings: _*)
  .settings(dockerSettings: _*)
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)
  .settings(
    libraryDependencies ++= Dependencies.core ++ Dependencies.api ++ Dependencies.test
  )
  .dependsOn(
    domain % "compile->compile;test->test",
    core % "compile->compile;test->test",
    checkout % "compile->compile;test->test"
  )

lazy val modules = (project in file("05-modules"))
  .settings(commonSettings: _*)
  .settings(dockerSettings: _*)
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)
  .settings(
    libraryDependencies ++= Dependencies.core ++ Dependencies.api ++ Dependencies.test
  )
  .dependsOn(
    domain % "compile->compile;test->test",
    core % "compile->compile;test->test",
    checkout % "compile->compile;test->test",
    http % "compile->compile;test->test"
  )

enablePlugins(MicrositesPlugin)
