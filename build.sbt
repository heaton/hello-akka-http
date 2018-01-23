name := "hello-akka-http"

version := "0.1"

scalaVersion := "2.12.4"

val akkaHttpVersion = "10.0.11"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.7",
  "com.typesafe.akka" %% "akka-stream" % "2.5.7",

  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,

  "org.scalatest" %% "scalatest" % "3.0.4" % Test,
  "org.scalamock" %% "scalamock" % "4.0.0" % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test
)