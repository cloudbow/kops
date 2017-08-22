import sbt._

object Dependencies {
  lazy val scalaLogger = "com.typesafe.scala-logging" % "scala-logging_2.11" % "3.7.2" 
  lazy val logback = "ch.qos.logback" % "logback-classic" % "1.1.7"
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.1"
  lazy val gson = "com.google.code.gson" % "gson" % "2.8.1"
  lazy val fasterXML = "com.fasterxml.jackson.core" % "jackson-databind" % "2.4.5"
  lazy val jaywayJson = "com.jayway.jsonpath" % "json-path" % "2.4.0"
  lazy val httpcomponents = "org.apache.httpcomponents" % "httpclient" % "4.4.1"
  lazy val scalaparser = "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4"
  lazy val quartzScheduler = "org.quartz-scheduler" % "quartz" % "2.3.0"
}
