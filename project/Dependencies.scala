import sbt._

object Dependencies {

  lazy val sangria = Seq(
    "org.sangria-graphql" %% "sangria" % "1.4.2",
    "org.sangria-graphql" %% "sangria-circe" % "1.2.1"
  )

  private lazy val circeVersion = "0.12.1"
  lazy val circe = Seq(
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion
  )
  lazy val circeTest = Seq(
    "io.circe" %% "circe-optics" % "0.12.0"
  ).map(_ % Test)

  lazy val cats = Seq(
    "org.typelevel" %% "cats-core" % "2.0.0",
    "org.typelevel" %% "cats-effect" % "2.0.0"
  )

  lazy val sttp = Seq(
    "com.softwaremill.sttp" %% "core" % "1.6.7"
  )

  lazy val scalaTest = Seq(
    "org.scalatest" %% "scalatest" % "3.0.5"
  )

  private lazy val http4sVersion = "0.20.10"
  lazy val http4s = Seq(
    "org.http4s" %% "http4s-dsl" % http4sVersion,
    "org.http4s" %% "http4s-blaze-server" % http4sVersion,
    "org.http4s" %% "http4s-circe" % http4sVersion,
    "com.softwaremill.sttp" %% "async-http-client-backend-cats" % "1.6.7"
  )

}
