package com.slingmedia.sportscloud.offline.batch

import org.apache.spark.sql.{ DataFrame, Column }
import org.apache.spark.sql.types.{ StructField, StructType };
import org.apache.spark.sql.functions.{ col, udf }
import org.apache.spark.sql.{ SparkSession, DataFrame, Row, Column }

import org.slf4j.LoggerFactory;

import scala.util.{ Try, Success, Failure }

import java.time.{ ZonedDateTime, OffsetDateTime, ZoneOffset, Instant, ZoneId }
import java.time.format.DateTimeFormatter

import org.elasticsearch.spark.rdd.EsSpark                        


object MLogHolder extends Serializable {
  val serialVersionUID = 1L;
  @transient lazy val log = LoggerFactory.getLogger("Muncher")
}

trait Muncher {
  def munch(inputKafkaTopic: String, outputCollName: String): Unit = {}
  def stream(inputKafkaTopic: String, outputCollName: String): Unit = {}
  def munch(inputKafkaTopic: String, outputCollName: String, schema: StructType, filterCond: String): Unit = {}
  def munch(batchTime: Long, inputKafkaTopic: String, outputCollName: String, schema: StructType, imgRequired: Boolean, idColumn: Column, filterCond: String, testColumn: Column): Unit = {}
  val children: (String, DataFrame) => Array[Column] = (colname: String, df: DataFrame) => {
    val parent = df.schema.fields.filter(_.name == colname).head
    val fields = parent.dataType match {
      case x: StructType => x.fields
      case _             => Array.empty[StructField]
    }
    fields.map(x => col(s"$colname.${x.name}"))
  }

  val timeStrToEpoch: (String => Long) = (timeStr: String) => {
    if (timeStr == null) 0L else OffsetDateTime.parse(timeStr).toEpochSecond()
  }
  val timeStrToEpochUDF = udf(timeStrToEpoch(_: String))

  val getZeroPaddedFunc: (String => String) = (timeStr: String) => {
    if (timeStr == null) {
      "0"
    } else {
      val timeInInt = timeStr.toInt
      if (timeInInt < 0) {
        val absTime = Math.abs(timeInInt)
        "-".concat(getZeroPaddedFunc(absTime.toString))
      } else if (timeInInt < 10) {
        "0".concat(timeStr)
      } else {
        timeStr
      }
    }
  }
  
  val getZeroPaddedUDF = udf(getZeroPaddedFunc(_: String))

    val timeEpochToStr: (Long => String) = (timeEpoch: Long) => {
    if (timeEpoch == 0) {
      "1972-05-20T17:33:18Z"
    } else {
      val epochTime: Instant = Instant.ofEpochSecond(timeEpoch);
      val utc: ZonedDateTime = epochTime.atZone(ZoneId.of("Z"));
      val pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'";
      val utcTime = utc.format(DateTimeFormatter.ofPattern(pattern));
      utcTime
    }

  }
  val timeEpochtoStrUDF = udf(timeEpochToStr(_: Long))
  
  
  val indexResults: (String,DataFrame) => Unit = ( outputCollName: String, input: DataFrame) => {
    EsSpark.saveToEs(input.rdd,s"sports-cloud/$outputCollName", Map("es.mapping.id" -> "id"))
  }

}