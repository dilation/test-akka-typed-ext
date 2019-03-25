
lazy val `test-akka-typed-ext` = (project in file("."))
  .settings(commonSettings: _*)

lazy val commonSettings = Seq(

  // Scala:
  scalaVersion := "2.12.6",
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
    //"-Xlog-implicits",
    //"-Ywarn-value-discard",
    "-Ywarn-unused-import"
  ),

  addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.5" cross CrossVersion.binary),

  // Dependencies:
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor-typed" % "2.5.21",
    "com.typesafe.akka" %% "akka-cluster-typed" % "2.5.21",
    "com.typesafe.akka" %% "akka-cluster-sharding-typed" % "2.5.21",
    "com.typesafe.akka" %% "akka-persistence-typed" % "2.5.21",
    "org.scalatest" %% "scalatest" % "3.0.2" % Test
  )
)
