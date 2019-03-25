import Dependencies._

lazy val graphient = (project in file("graphient"))
  .settings(
    name := "graphient",
    scalaVersion := "2.12.6",
    version := "0.1.2",
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding",
      "utf-8",
      "-explaintypes",
      "-feature",
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-language:postfixOps",
      "-unchecked",
      "-Xfuture",
      "-Xlint",
      "-Yno-adapted-args",
      "-Ypartial-unification",
      "-Ywarn-extra-implicit",
      "-Ywarn-inaccessible",
      "-Ywarn-infer-any",
      "-Ywarn-nullary-override",
      "-Ywarn-nullary-unit",
      "-Ywarn-numeric-widen",
      "-Ywarn-unused:implicits",
      "-Ywarn-unused:imports",
      "-Ywarn-unused:locals",
      "-Ywarn-unused:patvars",
      "-Ywarn-unused:privates",
      "-Ywarn-value-discard",
    ),
    resolvers += Resolver.sonatypeRepo("releases"),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.7"),
    libraryDependencies ++= sangria,
    libraryDependencies ++= circe,
    libraryDependencies ++= cats,
    libraryDependencies ++= sttp,
    libraryDependencies ++= scalaTest.map(_ % Test),
    libraryDependencies ++= circeOptics.map(_ % Test),
    libraryDependencies ++= http4s.map(_ % Test)
  )

lazy val root = (project in file(".")).aggregate(graphient)
