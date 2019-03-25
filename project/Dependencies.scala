import sbt._

object Dependencies {

  lazy val sangria = Seq(
    "org.sangria-graphql" %% "sangria" % "1.4.2",
    "org.sangria-graphql" %% "sangria-circe" % "1.2.1"
  )

  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.5"

  private lazy val circeVersion = "0.9.3"
  lazy val circe = Seq(
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion,
    "io.circe" %% "circe-generic-extras" % "0.10.0-M2", // TODO: ?
  )

  lazy val cats       = "org.typelevel" %% "cats-core" % "1.0.1"
  lazy val catsEffect = "org.typelevel" %% "cats-effect" % "1.0.0-RC2"

  lazy val sttp = Seq(
    "com.softwaremill.sttp" %% "core" % "1.5.11",
    "com.softwaremill.sttp" %% "akka-http-backend" % "1.5.11",
    "com.typesafe.akka" %% "akka-stream" % "2.5.19",
    "com.softwaremill.sttp" %% "async-http-client-backend-cats" % "1.5.11" % Test,
  )

  val http4sVersion = "0.20.0-M5"
  lazy val http4s = Seq(
    "org.http4s" %% "http4s-dsl" % http4sVersion,
    "org.http4s" %% "http4s-blaze-server" % http4sVersion,
    "org.http4s" %% "http4s-circe" % http4sVersion,
    "io.circe" %% "circe-optics" % circeVersion,
    "org.sangria-graphql" %% "sangria-circe" % "1.2.1"
  )

}
