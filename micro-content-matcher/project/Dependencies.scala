import sbt._

object Dependencies {
  lazy val spark = "org.apache.spark" % "spark-sql_2.11"  % "2.1.0"
  lazy val scalaLogger = "com.typesafe.scala-logging" % "scala-logging-slf4j_2.11" % "2.1.2"
  lazy val kafka = "org.apache.spark" % "spark-sql-kafka-0-10_2.11" % "2.1.1"
  lazy val databricksCSV = "com.databricks" % "spark-csv_2.10" % "1.5.0"
  lazy val mongoSpark = "org.mongodb.spark" % "mongo-spark-connector_2.11" % "2.0.0"
  lazy val logback = "ch.qos.logback" % "logback-classic" % "1.1.7"

}
