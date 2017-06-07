name := "downloader-akka"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.15",
  "org.apache.httpcomponents" % "httpclient" % "4.5.2",
  "com.github.scopt" %% "scopt" % "3.6.0"
)

mainClass in assembly := Some("ru.chicker.downloader.Main")
test in assembly := {}

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}
