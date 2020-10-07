import sbt._

object Dependencies {

  lazy val sangria = Seq(
    "org.sangria-graphql" %% "sangria" % "2.0.1",
    "org.sangria-graphql" %% "sangria-circe" % "1.3.0"
  )

  lazy val cats = Seq(
    "org.typelevel" %% "cats-core" % "2.0.0",
    "org.typelevel" %% "cats-effect" % "2.0.0"
  )

  lazy val sttp = Seq(
    "com.softwaremill.sttp.client" %% "core" % "2.2.4"
  )

  lazy val scalaTest = Seq(
    "org.scalatest" %% "scalatest" % "3.1.1",
    "org.scalatestplus" %% "scalatestplus-scalacheck" % "3.1.0.0-RC2",
    "org.scalacheck" %% "scalacheck" % "1.14.3"
  )

  private lazy val circeVersion = "0.12.3"
  lazy val circe = Seq(
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion
  )

  lazy val spray = Seq(
    "io.spray" %% "spray-json" % "1.3.5"
  )

}
