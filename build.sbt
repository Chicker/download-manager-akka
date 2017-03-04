name := "downloader-akka"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.15",
  "org.apache.httpcomponents" % "httpclient" % "4.5.2"
)
    