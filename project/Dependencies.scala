import sbt._

object Dependencies {

  lazy val sangria = Seq(
    "org.sangria-graphql" %% "sangria" % "1.4.2",
    "org.sangria-graphql" %% "sangria-circe" % "1.2.1"
  )

  lazy val cats = Seq(
    "org.typelevel" %% "cats-core" % "2.0.0",
    "org.typelevel" %% "cats-effect" % "2.0.0"
  )

  lazy val sttp = Seq(
    "com.softwaremill.sttp" %% "core" % "1.6.7"
  )

  lazy val scalaTest = Seq(
    "org.scalatest" %% "scalatest" % "3.0.5",
    "org.scalacheck" %% "scalacheck" % "1.14.1"
  )

  private lazy val circeVersion = "0.12.1"
  lazy val circe = Seq(
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion
  )

  lazy val spray = Seq(
    "io.spray" %% "spray-json" % "1.3.2"
  )

}
