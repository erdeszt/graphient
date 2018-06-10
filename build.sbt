import Dependencies._

lazy val client = (project in file("client")).
  settings(
    scalaVersion := "2.12.6",
    version := "0.1.0",
    name := "graphient",
    scalacOptions += "-Ypartial-unification",
    libraryDependencies ++= sangria,
    libraryDependencies ++= circe,
    libraryDependencies += sttp,
    libraryDependencies += cats,
    libraryDependencies += catsEffect,
    libraryDependencies ++= http4s,
    libraryDependencies += scalaTest % Test
  )

lazy val graphient = (project in file(".")).aggregate(client)
