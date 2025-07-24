ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "declarative-smart-contract",
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-language:existentials",
      "-language:implicitConversions"
    ),
    javaOptions ++= Seq(
      "-Djava.library.path=."
    ),
    fork := true
  )

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-parser-combinators" % "2.3.0",
  "org.scalatest" %% "scalatest" % "3.2.15" % Test
)

Compile / unmanagedJars += {
  baseDirectory.value / "unmanaged" / "com.microsoft.z3.jar"
}