import sbt._

object Dependencies {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.1"
  lazy val scalaparser = "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4"
  lazy val kafkalib = "org.apache.kafka" % "connect-api" % "0.10.2.0" % "provided"
  lazy val ftpKafka = "com.eneco" % "kafka-connect-ftp" % "0.0.0-unspecified"
  lazy val scalaXML = "org.scala-lang.modules" %% "scala-xml" % "1.0.2"
}
