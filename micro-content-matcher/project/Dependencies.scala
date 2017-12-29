import sbt._

object Dependencies {
  lazy val spark = "org.apache.spark" % "spark-sql_2.11"  % "2.1.0" % "provided"
  lazy val sparkStreaming = "org.apache.spark" % "spark-streaming_2.11" % "2.1.0" % "provided"
  lazy val scalaLogger = "com.typesafe.scala-logging" % "scala-logging_2.11" % "3.7.2" 
  lazy val kafkaSql = "org.apache.spark" % "spark-sql-kafka-0-10_2.11" % "2.1.1"
  lazy val kafkaSparkStreaming = "org.apache.spark" % "spark-streaming-kafka-0-10_2.11" % "2.1.1"
  lazy val kafkaSparkSql = "org.apache.spark" % "spark-sql-kafka-0-10_2.11" % "2.1.1"
  lazy val databricksCSV = "com.databricks" % "spark-csv_2.10" % "1.5.0"
  lazy val logback = "ch.qos.logback" % "logback-classic" % "1.1.7"
  lazy val elastic = "org.elasticsearch" % "elasticsearch-spark-20_2.11" % "6.0.0"

}
