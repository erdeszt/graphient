import Dependencies._

lazy val commonSettings = Seq(
  scalaVersion := "2.12.6",
  version := "0.1.1",
  scalacOptions ++= Seq(
    "-Ypartial-unification",
    "-explaintypes",
    "-language:higherKinds",
    "-Xlint"
  ),
  resolvers += Resolver.sonatypeRepo("releases"),
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.7")
)

lazy val graphient = (project in file("graphient")).
  settings(
    name := "graphient",
    commonSettings,
    libraryDependencies ++= sangria,
    libraryDependencies ++= circe,
    libraryDependencies += cats,
    libraryDependencies += catsEffect,
    libraryDependencies += scalaTest % Test,
  )

lazy val graphienttp = (project in file("graphienttp")).
  settings(
    name := "graphienttp",
    commonSettings,
    libraryDependencies ++= sangria,
    libraryDependencies ++= circe,
    libraryDependencies += cats,
    libraryDependencies += catsEffect,
    libraryDependencies += scalaTest % Test,
    libraryDependencies ++= sttp
  )

lazy val root = (project in file(".")).aggregate(graphient, graphienttp)