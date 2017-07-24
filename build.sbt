
lazy val `test-akka-typed-ext` = (project in file("."))
  .settings(commonSettings: _*)

lazy val commonSettings = Seq(

  // Scala:
  scalaVersion := "2.11.11",
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-unchecked",
    "-encoding", "UTF-8",
    "-target:jvm-1.8",
    "-Xlint:_",
    "-Xfuture",
    "-Yno-adapted-args",
    "-Ywarn-numeric-widen",
    "-Ywarn-dead-code",
    "-Ybackend:GenBCode",
    "-Ydelambdafy:method",
    "-Yopt:l:method",
    //"-Xlog-implicits",
    //"-Ywarn-value-discard",
    "-Ywarn-unused-import"
  ),

  addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.3" cross CrossVersion.binary),

  // Dependencies:
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-typed" % "2.5.3",
    "org.scalatest" %% "scalatest" % "3.0.2" % Test
  )
)
