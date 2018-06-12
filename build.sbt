import Dependencies._

lazy val client = (project in file("client")).
  settings(
    scalaVersion := "2.12.6",
    version := "0.1.0",
    name := "graphient",
    scalacOptions ++= Seq(
      "-Ypartial-unification",
      "-explaintypes",
      "-language:higherKinds",
      "-Xlint"
    ),
    resolvers += Resolver.sonatypeRepo("releases"),
    libraryDependencies ++= sangria,
    libraryDependencies ++= circe,
    libraryDependencies += cats,
    libraryDependencies += catsEffect,
    libraryDependencies += scalaTest % Test,
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.7")
  )

lazy val graphient = (project in file(".")).aggregate(client)
