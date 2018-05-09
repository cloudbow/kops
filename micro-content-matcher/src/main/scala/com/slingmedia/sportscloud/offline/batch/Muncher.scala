package com.slingmedia.sportscloud.offline.batch

import org.apache.spark.sql.{ DataFrame, Column }
import org.apache.spark.sql.types.{ StructField, StructType };
import org.apache.spark.sql.functions.{ col, udf }
import org.apache.spark.sql.{ SparkSession, DataFrame, Row, Column }

import org.slf4j.LoggerFactory;

import scala.util.{ Try, Success, Failure }

import java.time.{ ZonedDateTime, OffsetDateTime, ZoneOffset, Instant, ZoneId }
import java.time.format.DateTimeFormatter
import java.net.{URL, HttpURLConnection}

import org.elasticsearch.spark.rdd.EsSpark                        


object MLogHolder extends Serializable {
  val serialVersionUID = 1L;
  @transient lazy val log = LoggerFactory.getLogger("Muncher")
}

trait Muncher {
  def munch(inputKafkaTopic: String, outputCollName: String): Unit = {}
  def stream(inputKafkaTopic: String, outputCollName: String): Unit = {}
  def munch(inputKafkaTopic: String, outputCollName: String, artifactUrl: String): Unit = {}
  def munch(inputKafkaTopic: String, outputCollName: String, schema: StructType, filterCond: String): Unit = {}
  def munch(batchTime: Long, index:String, inputKafkaTopic: String, outputCollName: String, schema: StructType, imgRequired: Boolean, idColumn: Column, filterCond: String, testColumn: Column): Unit = {}
  val children: (String, DataFrame) => Array[Column] = (colname: String, df: DataFrame) => {
    val parent = df.schema.fields.filter(_.name == colname).head
    val fields = parent.dataType match {
      case x: StructType => x.fields
      case _             => Array.empty[StructField]
    }
    fields.map(x => col(s"$colname.${x.name}"))
  }

  val normalizeLeague: (String => String) = (league: String) => {
    league match {
      case "CBK" =>  "NCAAB"
      case "CFB" => "NCAAF"
      case _ => league
    }
  }
  val normalizeLeagueUDF = udf(normalizeLeague(_: String))

  val timeStrToEpoch: (String => Long) = (timeStr: String) => {
    if (timeStr == null) 0L else OffsetDateTime.parse(timeStr).toEpochSecond()
  }
  val timeStrToEpochUDF = udf(timeStrToEpoch(_: String))

  val isEmpty: (String => Boolean) = (x: String) => {
    x == null || x.trim.isEmpty
  }


  val zeroPadNum: (Int => String) = (num: Int) => {
    if (num < 10) {
      "0".concat(num.toString)
    } else {
      num.toString()
    }
  }

  val getZeroPadTimeOffsetFunc: (String, String) => String = (offHour: String, offMinute: String) => {
    var defaultOffset="+00:00"
    if (isEmpty(offHour) && isEmpty(offMinute)) {
      defaultOffset
    } else {
      val offsetHourInt =if(isEmpty(offHour)) 0 else offHour.toInt
      val offsetMinInt = if(isEmpty(offMinute)) 0 else offMinute.toInt
      val offsetHourAbs  = Math.abs(offsetHourInt)
      if (offsetHourInt < 0) {
        defaultOffset="-"
      }  else {
        defaultOffset="+"
      }
      defaultOffset.concat(zeroPadNum(offsetHourAbs)).concat(":").concat(zeroPadNum(offsetMinInt))
    }
  }
  val zeroPadTimeOffsetUDF = udf(getZeroPadTimeOffsetFunc(_: String,_:String))


  val getZeroPadDateTimeFunc: (String => String) = (timeStr: String) => {
    if (isEmpty(timeStr)) {
      "00"
    } else {
      val timeInt = if(isEmpty(timeStr)) 0 else timeStr.toInt
      zeroPadNum(timeInt)
    }
  }

  val zeroPadDateTimeUDF = udf(getZeroPadDateTimeFunc(_: String))

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


  val indexResults: (String, String,DataFrame) => Unit = ( index:String, outputCollName: String, input: DataFrame) => {
    val inputConverted = input.toJSON
    EsSpark.saveJsonToEs(inputConverted.rdd,s"$index/$outputCollName", Map("es.mapping.id" -> "id"))
  }

  val getReorderedStatusId: (Int => Int) = (statusId: Int) => {
    if (statusId == 23) 2 else statusId
  }
  val getReorderedStatusIdUDF = udf(getReorderedStatusId(_: Int))

  def get(url: String,
          connectTimeout: Int = 60000,
          readTimeout: Int = 50000,
          requestMethod: String = "GET") =
  {
    val connection = (new URL(url)).openConnection.asInstanceOf[HttpURLConnection]
    connection.setConnectTimeout(connectTimeout)
    connection.setReadTimeout(readTimeout)
    connection.setRequestMethod(requestMethod)
    val inputStream = connection.getInputStream
    val content = scala.io.Source.fromInputStream(inputStream).mkString
    if (inputStream != null) inputStream.close
    content
  }

  val getDFFromHttp:(String => DataFrame) = (url: String) => {
    import scala.collection.mutable.ListBuffer
    val content = get(url)
    var responseArrList = ListBuffer.empty[String].toList
    if(content!=null) {
      responseArrList = content.split("\n").toList.filter(_ != "")
    }
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._
    val jsonRDD = spark.sparkContext.parallelize(responseArrList)
    val jsonDF = spark.read.json(jsonRDD)
    jsonDF
  }


}