import sbt._
import sbt.Keys._

object CompilerSettings {

  lazy val options = Seq(
    scalacOptions ++= {
      Seq(
        "-encoding",
        "UTF-8",
        "-language:_",
        "-feature",
        // "-Xfatal-warnings",
        "-deprecation",
        "-Xlint:infer-any",
        "-Xlint:adapted-args",
        "-Xlint:delayedinit-select",
        "-Xlint:-unused",
        "-unchecked",
        // "-Ypartial-unification",
        "-Ymacro-annotations",
        "-Ywarn-dead-code",
        "-Ywarn-numeric-widen",
        "-Ywarn-macros:after",
        "-Ywarn-unused:implicits",
        "-Ywarn-unused:imports",
        "-Ywarn-unused:locals",
        "-Ywarn-unused:patvars",
        "-Ywarn-unused:privates"
      )
    }
  )
}
