import sbt._

object Dependencies {

  lazy val sangria = Seq(
    "org.sangria-graphql" %% "sangria" % "1.4.1",
    "org.sangria-graphql" %% "sangria-circe" % "1.2.1"
  )

  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.5"

  private lazy val circeVersion = "0.9.3"
  lazy val circe = Seq(
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion
  )

  lazy val sttp = "com.softwaremill.sttp" %% "core" % "1.2.0-RC6"

  lazy val cats       = "org.typelevel" %% "cats-core" % "1.0.1"
  lazy val catsEffect = "org.typelevel" %% "cats-effect" % "1.0.0-RC2"

  private lazy val http4sVersion = "0.18.12"
  lazy val http4s = Seq(
    "org.http4s" %% "http4s-dsl" % http4sVersion,
    "org.http4s" %% "http4s-blaze-server" % http4sVersion
  )
}
