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
) ++ {
  if (scala.sys.env.getOrElse("CI", "") == "true") {
    Seq("-Xfatal-warnings")
  } else { Seq() }
}

lazy val graphientCore = (project in file("graphient"))
  .settings(
    name := "graphient",
    scalaVersion := "2.12.6",
    version := "5.0.0-SNAPSHOT",
    scalacOptions ++= ScalacOptions,
    resolvers += Resolver.sonatypeRepo("releases"),
    bintrayRepository := "io.github.erdeszt",
    organization := "io.github.erdeszt",
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.7"),
    libraryDependencies ++= sangria,
    libraryDependencies ++= cats,
    libraryDependencies ++= sttp,
    libraryDependencies ++= scalaTest.map(_ % Test),
  )

lazy val graphientCirce = (project in file("graphient-circe"))
  .settings(
    name := "graphient-circe",
    scalaVersion := "2.12.6",
    version := "1.0.0",
    scalacOptions ++= ScalacOptions,
    resolvers += Resolver.sonatypeRepo("releases"),
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    libraryDependencies ++= circe,
    libraryDependencies ++= scalaTest.map(_ % Test),
  ).dependsOn(graphientCore % "compile->compile;test->test")

lazy val graphientSpray = (project in file("graphient-spray"))
  .settings(
    name := "graphient-spray",
    scalaVersion := "2.12.6",
    version := "1.0.0",
    scalacOptions ++= ScalacOptions,
    resolvers += Resolver.sonatypeRepo("releases"),
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    libraryDependencies ++= spray,
    libraryDependencies ++= scalaTest.map(_ % Test),
  ).dependsOn(graphientCore % "compile->compile;test->test")

lazy val graphientProject = (project in file(".")).aggregate(graphientCore, graphientCirce, graphientSpray)
