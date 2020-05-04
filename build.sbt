import Dependencies._

lazy val Scalac213Options = Seq(
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
  "-Xlint",
  "-Ywarn-extra-implicit",
  "-Ywarn-numeric-widen",
  "-Ywarn-unused:implicits",
  "-Ywarn-unused:imports",
  "-Ywarn-unused:locals",
  "-Ywarn-unused:patvars",
  "-Ywarn-unused:privates",
  "-Ywarn-value-discard",
) ++ {
  if (scala.sys.env.getOrElse("CI", "") == "true") {
    Seq("-Xfatal-warnings")
  } else { Seq() }
}
lazy val ScalacOptions = scalaVersion.map {
  case v if v.startsWith("2.12") =>
    Scalac213Options ++ Seq(
      "-Ypartial-unification",
      "-Ywarn-inaccessible",
      "-Yno-adapted-args",
      "-Ywarn-infer-any",
      "-Ywarn-nullary-override",
      "-Ywarn-nullary-unit",
      "-Xfuture",
    )
  case _ => Scalac213Options
}

lazy val graphientCore = (project in file("graphient"))
  .settings(
    name := "graphient",
    scalaVersion := "2.12.11",
    crossScalaVersions := List("2.12.11", "2.13.2"),
    version := "6.1.0",
    scalacOptions ++= ScalacOptions.value,
    resolvers += Resolver.sonatypeRepo("releases"),
    bintrayRepository := "io.github.erdeszt",
    organization := "io.github.erdeszt",
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3"),
    libraryDependencies ++= sangria,
    libraryDependencies ++= cats,
    libraryDependencies ++= sttp,
    libraryDependencies ++= scalaTest.map(_ % Test),
  )

lazy val graphientCirce = (project in file("graphient-circe"))
  .settings(
    name := "graphient-circe",
    scalaVersion := "2.12.11",
    crossScalaVersions := List("2.12.11", "2.13.2"),
    version := "2.1.0",
    scalacOptions ++= ScalacOptions.value,
    resolvers += Resolver.sonatypeRepo("releases"),
    organization := "io.github.erdeszt",
    bintrayRepository := "io.github.erdeszt",
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    libraryDependencies ++= circe,
    libraryDependencies ++= scalaTest.map(_ % Test),
  ).dependsOn(graphientCore % "compile->compile;test->test")

lazy val graphientSpray = (project in file("graphient-spray"))
  .settings(
    name := "graphient-spray",
    scalaVersion := "2.12.11",
    crossScalaVersions := List("2.12.11", "2.13.2"),
    version := "2.1.0",
    scalacOptions ++= ScalacOptions.value,
    resolvers += Resolver.sonatypeRepo("releases"),
    organization := "io.github.erdeszt",
    bintrayRepository := "io.github.erdeszt",
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    libraryDependencies ++= spray,
    libraryDependencies ++= scalaTest.map(_ % Test),
  ).dependsOn(graphientCore % "compile->compile;test->test")

lazy val graphientProject = (project in file(".")).aggregate(graphientCore, graphientCirce, graphientSpray)
