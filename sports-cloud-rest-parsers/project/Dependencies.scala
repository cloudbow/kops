import sbt._

object Dependencies {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.1"
  lazy val logback = "ch.qos.logback" % "logback-classic" % "1.1.7"
  lazy val scalaLogger = "com.typesafe.scala-logging" % "scala-logging-slf4j_2.11" % "2.1.2"
  lazy val scalaparser = "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4"
  lazy val playJson = "com.typesafe.play" %% "play-json" % "2.3.0"
}