package com.slingmedia.sportscloud.offline.batch.impl.leagues

import com.slingmedia.sportscloud.offline.batch.Muncher
import com.slingmedia.sportscloud.offline.batch.impl.ContentMatcher


import java.time.{ ZonedDateTime , Instant, ZoneId}
import java.time.format.DateTimeFormatter

import org.apache.spark.sql.{ SQLContext, SparkSession, DataFrame, Row, Column }
import org.apache.spark.sql.functions.{ concat_ws, concat, lit, coalesce, max, min, udf, col, explode, from_json, collect_list }
import org.apache.spark.sql.types.{ StructType, StructField, StringType, IntegerType, LongType, ArrayType }

import sys.process._
import scala.language.postfixOps
import scala.collection.mutable.WrappedArray
import scala.util.{ Try, Success, Failure }

import java.time.{ ZonedDateTime,LocalDateTime, OffsetDateTime, ZoneOffset, Instant, ZoneId }
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

import com.databricks.spark.csv

import org.apache.spark.SparkFiles


import java.io.File
import java.nio.file.{ Paths, Files }

import org.slf4j.LoggerFactory


object SoccerCMHolder extends Serializable {
  val serialVersionUID = 1L
  @transient lazy val log = LoggerFactory.getLogger("SoccerContentMatcher")
}

object SoccerContentMatcher extends Serializable {
  def main(args: Array[String]) {
    SoccerCMHolder.log.debug("Args is $args")
    //"content_match", "game_schedule"
    new SoccerContentMatcher().munch(args(0), args(1))
      //new ContentMatcher().test(args(0), args(1))
  }
}

class SoccerContentMatcher extends ContentMatcher {

  val getExternalIDFunc: (String => String) = (externalId: String) => {
    if (isEmpty(externalId)) {
      "0"
    } else {
      externalId.split("-")(1)
    }
  }

  val externalIDUDF = udf(getExternalIDFunc(_: String))

  override val getReorderedStatusId: (Int => Int) = (statusId: Int) => {
    if (statusId == 10)  {
      4
    }  else if (statusId == 40) {
      2
    } else if (statusId == 80) {
      1
    }
    else {
      statusId
    }
  }

  def nagraUtcStrToEpochFunc(utcStr: String): Long = {
    val zdt = ZonedDateTime.parse(utcStr,DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss Z"));
    val epoch = zdt.toEpochSecond()
    epoch
  }

  val nagraUtcStrToEpochUDF = udf(nagraUtcStrToEpochFunc(_: String))

  override def munch(inputKafkaTopic: String, outputCollName: String): Unit = {
    super.munch(inputKafkaTopic,outputCollName)
  }
  override val fetchMLBSchedule: (String => DataFrame) = (inputKafkaTopic: String) => {

    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._

    val pattern = "yyyy-dd-MM+Z"

    val startOff = Instant.now.minus(1, ChronoUnit.DAYS)
    val startUtc = startOff.atZone(ZoneId.of("UTC-07:00"))
    val startTime = startUtc.format(DateTimeFormatter.ofPattern(pattern))

    val endOff = Instant.now.plus(5, ChronoUnit.DAYS)
    val endUtc = endOff.atZone(ZoneId.of("UTC-07:00"))
    val endTime = endUtc.format(DateTimeFormatter.ofPattern(pattern))

    val nagraGameScheduleDF1 = getDFFromHttp(s"http://gwserv-mobileprod.echodata.tv/Gamefinder/api/game/search?startDate=$startTime&endDate=$endTime&page.size=300")
    val nagraGameScheduleDF2 = nagraGameScheduleDF1.withColumn("content", explode(nagraGameScheduleDF1.col("content")));
    val nagraGameScheduleDF3 = nagraGameScheduleDF2.select(children("content", nagraGameScheduleDF2): _*)

    val allNagraFills = Map(
      "homeTeamCity" -> "",
      "awayTeamCity" -> "",
      "homeTeamScore" -> 0,
      "awayTeamScore" -> 0)

    val nagraGameScheduleDF4  = nagraGameScheduleDF3.filter("sport='soccer'").select($"awayTeam.city".alias("awayTeamCity"),
      $"awayTeam.name".alias("awayTeamName"),
      $"awayTeam.alias".alias("awayTeamAlias"),
      $"awayTeam.teamExternalId".alias("atExternalId"),
      $"homeTeam.city".alias("homeTeamCity"),
      $"homeTeam.name".alias("homeTeamName"),
      $"homeTeam.alias".alias("homeTeamAlias"),
      $"homeTeam.teamExternalId".alias("htExternalId"),
      $"homeScore".alias("homeTeamScore"),
      $"awayScore".alias("awayTeamScore"),
      $"id".alias("gameId"),
      $"id".alias("gameCode"),
      $"gameStatus".alias("status"),
      $"statusId".alias("statusId"),
      $"gameType".alias("gameType"),
      $"league".alias("league"),
      $"sport".alias("sport"),
      $"venue".alias("stadiumName"),
      $"scheduledDate".alias("scheduledDate")).
      withColumn("game_date_epoch",nagraUtcStrToEpochUDF(col("scheduledDate"))).
      withColumn("gameDate", timeEpochtoStrUDF($"game_date_epoch")).
      withColumn("awayTeamExternalId",externalIDUDF(col("aTExternalId"))).
      withColumn("homeTeamExternalId",externalIDUDF(col("hTExternalId")))
      .drop("aTExternalId")
      .drop("hTExternalId")
      .na.fill(allNagraFills)

    val nagraGameScheduleDF41 = nagraGameScheduleDF4.
      withColumn("anonsTitle", getAnonsTitleUDF($"homeTeamName", $"awayTeamName", $"stadiumName")).
      withColumn("statusId", getReorderedStatusIdUDF($"statusId")).
      withColumn("homeTeamImg", makeImgUrl($"homeTeamExternalId", $"league")).
      withColumn("awayTeamImg", makeImgUrl($"awayTeamExternalId", $"league"))

    val nagraGameScheduleDF42 = nagraGameScheduleDF41.distinct.toDF
    nagraGameScheduleDF42

  }


}
