import Dependencies._

lazy val ScalacOptions = Seq(
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
  "-Xfatal-warnings",
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
)

lazy val graphient = (project in file("graphient"))
  .settings(
    name := "graphient",
    scalaVersion := "2.12.6",
    version := "4.0.1",
    scalacOptions ++= ScalacOptions,
    resolvers += Resolver.sonatypeRepo("releases"),
    bintrayRepository := "io.github.erdeszt",
    organization := "io.github.erdeszt",
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.7"),
    libraryDependencies ++= sangria,
    libraryDependencies ++= circe,
    libraryDependencies ++= cats,
    libraryDependencies ++= sttp,
    libraryDependencies ++= scalaTest.map(_ % Test),
    libraryDependencies ++= circeTest,
    libraryDependencies ++= http4s.map(_ % Test)
  )

lazy val graphientSpray = (project in file("graphient-spray"))
  .settings(
    name := "graphient-spray",
    scalaVersion := "2.12.6",
    version := "1.0.0",
    scalacOptions ++= ScalacOptions,
    resolvers += Resolver.sonatypeRepo("releases"),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.7"),
    libraryDependencies ++= spray,
    libraryDependencies ++= scalaTest.map(_ % Test),
    libraryDependencies += "com.softwaremill.sttp" %% "async-http-client-backend-cats" % "1.6.7" % Test,
  ).dependsOn(graphient)

lazy val graphientProject = (project in file(".")).aggregate(graphient, graphientSpray)
