scalaVersion := "2.13.1"
version := "0.1.0-SNAPSHOT"
organization := "io.timmers"
organizationName := "timmers.io"

addCommandAlias("check", "fixCheck; fmtCheck")
addCommandAlias("fix", "all compile:scalafix test:scalafix")
addCommandAlias(
  "fixCheck",
  "compile:scalafix --check; test:scalafix --check"
)
addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias(
  "fmtCheck",
  "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck"
)

scalafixDependencies in ThisBuild += "com.nequissimus" %% "sort-imports" % "0.3.1"

val zioVersion = "1.0.0-RC20"
val zioLoggingVersion = "0.3.1"
val zioCatsVersion = "2.1.3.0-RC15"
val http4sVersion = "0.21.4"
val circeVersion = "0.13.0"
val scalaTestVersion = "3.1.2"

lazy val root = (project in file("."))
  .settings(
    name := "thunder",
    scalacOptions := Seq(
      "-Ywarn-unused:_"
    ),
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-logging" % zioLoggingVersion,
      "dev.zio" %% "zio-test" % zioVersion % Test,
      "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
      "dev.zio" %% "zio-interop-cats" % zioCatsVersion,
      "org.http4s" %% "http4s-blaze-client" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-generic-extras" % circeVersion,
      "io.circe" %% "circe-literal" % circeVersion,
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
      compilerPlugin(
        ("org.typelevel" % "kind-projector" % "0.11.0").cross(CrossVersion.full)
      ),
      compilerPlugin(scalafixSemanticdb)
    ),
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
  )
