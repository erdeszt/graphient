import sbt._

object Dependencies {

  lazy val sangria = Seq(
    "org.sangria-graphql" %% "sangria" % "2.1.6",
    "org.sangria-graphql" %% "sangria-circe" % "1.3.2"
  )

  lazy val cats = Seq(
    "org.typelevel" %% "cats-core" % "2.6.1",
    "org.typelevel" %% "cats-effect" % "2.5.4"
  )

  lazy val sttp = Seq(
    "com.softwaremill.sttp.client3" %% "core" % "3.3.18"
  )

  lazy val scalaTest = Seq(
    "org.scalatest" %% "scalatest" % "3.1.1",
    "org.scalatestplus" %% "scalatestplus-scalacheck" % "3.1.0.0-RC2",
    "org.scalacheck" %% "scalacheck" % "1.14.3"
  )

  private lazy val circeVersion = "0.14.1"
  lazy val circe = Seq(
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion
  )

  lazy val spray = Seq(
    "io.spray" %% "spray-json" % "1.3.5"
  )

}
