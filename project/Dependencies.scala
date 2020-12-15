import sbt._

object Dependencies {
  object V {
    val cats = "2.2.0"
    val console4cats = "0.8.1"
    val catsEffect = "2.2.0"
    val catsMeowMtl = "0.4.1"
    val fs2 = "2.4.5"
    val http4s = "1.0.0-M5"
    val http4sJwtAuth = "0.0.5"
    val log4cats = "1.1.1"
    val newtype = "0.4.3"
    val refined = "0.9.17"
    val betterMonadicFor = "0.3.1"
    val kindProjector = "0.11.0"
    val ciris = "1.2.1"
    val circe = "0.13.0"
    val circieOptics = "0.11.0"
    val logback = "1.2.3"
    val logging = "3.9.2"
    val specs2 = "4.10.5"
    val scalacheck_shapeless = "1.2.3"
    val scallop = "3.5.1"
    val javaxCrypto = "1.0.1"
    val redis4cats = "0.10.3"
    // val squant = "1.7.0-SNAPSHOT"
    val skunk = "0.0.21"
    val squant = "1.7.0+"
  }

  def circe(artifact: String): ModuleID =
    "io.circe" %% artifact % V.circe
  def ciris(artifact: String): ModuleID =
    "is.cir" %% artifact % V.ciris withSources () withJavadoc ()
  def http4s(artifact: String): ModuleID =
    "org.http4s" %% artifact % V.http4s withSources () withJavadoc ()

  val api =
    Seq(
      http4s("http4s-dsl"),
      http4s("http4s-blaze-server"),
      http4s("http4s-circe")
    )

  val core =
    Seq(
      http4s("http4s-blaze-client"),
      "org.tpolecat" %% "skunk-core" % V.skunk,
      "org.tpolecat" %% "skunk-circe" % V.skunk,
      "dev.profunktor" %% "redis4cats-effects" % V.redis4cats,
      "dev.profunktor" %% "redis4cats-log4cats" % V.redis4cats,
      "dev.profunktor" %% "console4cats" % V.console4cats,
      "org.typelevel" %% "squants" % V.squant,
      "dev.profunktor" %% "http4s-jwt-auth" % V.http4sJwtAuth,
      "javax.xml.crypto" % "jsr105-api" % V.javaxCrypto,
      "eu.timepit" %% "refined" % V.refined,
      "eu.timepit" %% "refined-cats" % V.refined,
      "org.typelevel" %% "cats-effect" % V.cats,
      "org.typelevel" %% "cats-core" % V.cats,
      "com.olegpy" %% "meow-mtl-core" % V.catsMeowMtl,
      "co.fs2" %% "fs2-core" % V.fs2,
      "eu.timepit" %% "refined" % V.refined,
      "eu.timepit" %% "refined-cats" % V.refined,
      "io.chrisdavenport" %% "log4cats-slf4j" % V.log4cats,
      "org.rogach" %% "scallop" % V.scallop,
      "io.estatico" %% "newtype" % V.newtype,
      "com.olegpy" %% "better-monadic-for" % V.betterMonadicFor,
      "com.olegpy" %% "better-monadic-for" % V.betterMonadicFor,
      "ch.qos.logback" % "logback-classic" % V.logback,
      http4s("http4s-circe"),
      circe("circe-core"),
      circe("circe-generic"),
      circe("circe-parser"),
      circe("circe-refined"),
      circe("circe-optics"),
      ciris("ciris"),
      ciris("ciris-enumeratum"),
      ciris("ciris-refined")
    )

  val test =
    Seq(
      "com.lihaoyi" %% "ammonite-ops" % "2.1.4" % "test",
      "com.github.alexarchambault" %% "scalacheck-shapeless_1.14" % V.scalacheck_shapeless % "test",
      "org.specs2" %% "specs2-core" % V.specs2 % "test",
      "org.specs2" %% "specs2-scalacheck" % V.specs2 % "test"
    )

  // compilerPlugins = Seq(
  val betterMonadicFor =
    "com.olegpy" %% "better-monadic-for" % V.betterMonadicFor
  val kindProject = "org.typelevel" % "kind-projector" % V.kindProjector

}
