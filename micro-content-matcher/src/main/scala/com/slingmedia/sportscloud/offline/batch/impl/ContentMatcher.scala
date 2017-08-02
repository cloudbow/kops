package com.slingmedia.sportscloud.offline.batch

import org.apache.spark.sql.{ SQLContext, SparkSession, DataFrame, Row, Column }
import org.apache.spark.sql.functions.{ concat, lit, coalesce, max, min, udf, col, explode, from_json, collect_list }
import org.apache.spark.sql.types.{ StructType, StructField, StringType, IntegerType, LongType, ArrayType };

import sys.process._

import java.time.{ ZonedDateTime, OffsetDateTime, ZoneOffset, Instant, ZoneId }
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit;

import com.databricks.spark.csv

import java.io.File
import java.nio.file.{ Paths, Files }

import scala.collection.mutable.WrappedArray

import org.slf4j.LoggerFactory;

import com.typesafe.scalalogging.slf4j.LazyLogging
import com.typesafe.scalalogging.slf4j.Logger

import com.lucidworks.spark.util.{ SolrSupport, SolrQuerySupport, ConfigurationConstants }

object CMHolder extends Serializable {
  val serialVersionUID = 1L;
  @transient lazy val log = LoggerFactory.getLogger("ContentMatcher")
}

case class GameEvent(id: String, anonsTitle: String, assetGuid: String, awayPlayerExtId: String, awayTeamCity: String, awayTeamExternalId: String, awayTeamImg: String, awayTeamName: String, awayTeamPitcherName: String, awayTeamScore: String, callsign: String, channelGuid: String, channelNo: Long, gameCode: String, gameDate: String, gameDateEpoch: Long, genres: String, gexPredict: Long, homePlayerExtId: String, homeTeamCity: String, homeTeamExternalId: String, homeTeamImg: String, homeTeamName: String, homeTeamPitcherName: String, homeTeamScore: String, preGameTeaser: String, programGuid: String, programId: Long, programTitle: String, stadiumName: String, subpackTitle: String, subpackageGuid: String)
case class GameEvents(id: String, batchTime: Long, gameEvents: Array[GameEvent])

object ContentMatcher extends Serializable {
  def main(args: Array[String]) {
    CMHolder.log.debug("Args is $args")
    new ContentMatcher().munch("content_match", "game_schedule", "localhost:9983")
  }
}

class ContentMatcher extends Serializable with Muncher {

  override def munch(inputKafkaTopic: String, outputCollName: String, zkHost: String): Unit = {

    //fetch thuuz games
    fetchThuuzGames()

    //fetch sports channels
    val summaryJson6 = fetchSportsChannels()

    //fetch program schedules
    fetchProgramSchedules(summaryJson6)

    //Fetch MLB schedule
    val mlbScheduleDF32 = fetchMLBSchedule()

    //join with thuuz to get gex score
    val mlbScheduleDF34 = intersectWithThuuz(mlbScheduleDF32)

    //get the matched data
    val programsJoin3 = contentMatch(mlbScheduleDF34)

    //Write delta back to mongo
    writeData( outputCollName, zkHost, programsJoin3)

  }

  //All UDFs are here which actually does multiple functions on a single column in a Ros

  val getAnonsTitle: (String, String, String) => String = (homeTeamName: String, awayTeamName: String, stadium: String) => {
    var anonsTitle = homeTeamName.concat(" take on ").concat(awayTeamName)
    if (stadium != null) {
      anonsTitle.concat(" at ").concat(stadium)
    }
    anonsTitle
  }
  val getAnonsTitleUDF = udf(getAnonsTitle(_: String, _: String, _: String))

  val getZeroPaddedFunc: (String => String) = (timeStr: String) => {
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
  val getZeroPaddedUDF = udf(getZeroPaddedFunc(_: String))

  val timeStrToEpoch: (String => Long) = (timeStr: String) => {
    if (timeStr == null) 0L else OffsetDateTime.parse(timeStr).toEpochSecond()
  }
  val timeStrToEpochUDF = udf(timeStrToEpoch(_: String))

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

  val timeISO8601ToEpochFunc: (String => Long) = (timeStr: String) => {
    if (timeStr == null) 0L else Instant.parse(timeStr).getEpochSecond()
  }
  val timeISO8601ToEpochUDF = udf(timeISO8601ToEpochFunc(_: String))

  val addDeduplicateLogic: (String, String) => String = (programTitle: String, genre: String) => {
    if (programTitle == null) programTitle else if (programTitle.startsWith(genre)) programTitle else genre.toUpperCase.concat("  ").concat(programTitle)
  }
  val partialWithMLB = udf(addDeduplicateLogic(_: String, "MLB:"))

  val makeImgUrlFunc: (String => String) = (teamExternalId: String) => { if (teamExternalId == null) "http://qahsports.slingbox.com/sport_teams/baseball/mlb/gid1.png" else "http://qahsports.slingbox.com/sport_teams/baseball/mlb/gid".concat(teamExternalId).concat(".png") }
  val makeImgUrl = udf(makeImgUrlFunc)

  val listToStrFunc: (WrappedArray[String] => String) = (genres: WrappedArray[String]) => {
    if (genres == null) "" else genres.mkString("::")
  }
  val listToStrUDF = udf(listToStrFunc(_: WrappedArray[String]))

  val getChannelGuidFromSelfFunc: (String => String) = (url: String) => {
    if (url != null) {
      var chUrl = url.slice(0, 0 + url.lastIndexOf("/"))
      chUrl = chUrl.substring(chUrl.lastIndexOf("/") + 1)
      chUrl
    } else {
      ""
    }
  }
  val channelGuidUDF = udf(getChannelGuidFromSelfFunc(_: String))

  val regexpCreator = (awayTeamCity: String, awayTeamName: String, homeTeamCity: String, homeTeamName: String, matchType: Int) => {
    val interMediaryExpr = "(vs\\.|at)"
    val startExpr = ".*?"
    var regexp: String = null
    matchType match {
      case 0 =>
        regexp = ("^" + startExpr + "\\s*" + awayTeamCity + ".*" + awayTeamName + "\\s*" + interMediaryExpr + "\\s*" + homeTeamCity + "\\s*" + homeTeamName + "$")
      case 1 =>
        regexp = ("^" + startExpr + "\\s*" + awayTeamName + "\\s*" + interMediaryExpr + "\\s*" + homeTeamName + "$")
      case 2 =>
        regexp = ("^" + startExpr + "\\s*" + awayTeamName + "\\s*" + interMediaryExpr + "\\s*" + homeTeamCity + "\\s*" + homeTeamName + "$")
      case 3 =>
        regexp = ("^" + startExpr + "\\s*" + awayTeamCity + "\\s*" + awayTeamName + "\\s*" + interMediaryExpr + "\\s*" + homeTeamName + "$")
      case _ =>
        regexp = "!"
    }
    regexp
  }

  val fullMatch = udf(regexpCreator(_: String, _: String, _: String, _: String, 0))
  val partialWithAnHn = udf(regexpCreator(_: String, _: String, _: String, _: String, 1))
  val partialWithAtHcHn = udf(regexpCreator(_: String, _: String, _: String, _: String, 2))
  val partialWithAcAnHn = udf(regexpCreator(_: String, _: String, _: String, _: String, 2))

  //UDF definitions ends here

  def contentMatch(gameScheduleDF: DataFrame, domain: String = "slingtv"): DataFrame = {
    //Fetch from thuuz and update 
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._
    val mlbScheduleDF41 = gameScheduleDF.withColumn("regexp1", fullMatch($"awayTeamCity", $"awayTeamName", $"homeTeamCity", $"homeTeamName"))
    val mlbScheduleDF42 = mlbScheduleDF41.withColumn("regexp2", partialWithAnHn($"awayTeamCity", $"awayTeamName", $"homeTeamCity", $"homeTeamName"))
    val mlbScheduleDF43 = mlbScheduleDF42.withColumn("regexp3", partialWithAtHcHn($"awayTeamCity", $"awayTeamName", $"homeTeamCity", $"homeTeamName"))
    val mlbScheduleDF44 = mlbScheduleDF43.withColumn("regexp4", partialWithAcAnHn($"awayTeamCity", $"awayTeamName", $"homeTeamCity", $"homeTeamName"))
    val mlbScheduleDF5 = mlbScheduleDF44.coalesce(3).cache

    //val gameScheduleDF=mlbScheduleDF5
    //val targetProgramsToMatch = programScheduleChannelJoin
    //val domain = "slingtv"

    val programScheduleChannelJoin2 = spark.sql("select * from programSchedules").toDF
    CMHolder.log.trace("Joining with target programs using a cross join")
    val statsTargetProgramsJoin = mlbScheduleDF5.crossJoin(programScheduleChannelJoin2)
    val statsTargetProgramsJoin1 = statsTargetProgramsJoin.where("( start_time_epoch < game_date_epoch + 3600 AND start_time_epoch > game_date_epoch - 3600 ) AND ((program_title rlike regexp1) OR (program_title rlike regexp2) OR (program_title rlike regexp3) OR (program_title rlike regexp4))")
    CMHolder.log.trace("Matched with stats data");
    val programsJoin1 = statsTargetProgramsJoin1.drop("regexp1", "regexp2", "regexp3", "regexp4", "subpack_int_id", "start_time", "start_time_epoch", "date")
    val programsJoin2 = programsJoin1.withColumn("homeTeamImg", makeImgUrl($"homeTeamExternalId")).withColumn("awayTeamImg", makeImgUrl($"awayTeamExternalId"))
    val columns = programsJoin2.columns
    scala.util.Sorting.quickSort(columns)
    val programsJoin3 = programsJoin2.select(columns.head, columns.tail: _*)
    val programsJoin31 = programsJoin3.coalesce(3)
    programsJoin31
  }

  def getStats(statsTargetProgramsJoin1: DataFrame, statsTargetProgramsJoin: DataFrame, domain: String = "slingtv"): (Long, Long, Float) = {
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._
    val statsTargetProgramsJoin2 = statsTargetProgramsJoin1.select($"program_title").withColumn("program_title", partialWithMLB($"program_title")).distinct
    val statsTargetProgramsJoin3 = statsTargetProgramsJoin.where("(program_title rlike regexp1) OR (program_title rlike regexp2) OR (program_title rlike regexp3) OR (program_title rlike regexp4)").select($"program_title").withColumn("program_title", partialWithMLB($"program_title")).distinct
    val sportsProgramsDir = s"/tmp/sports-cloud/all-mlb-$domain-sports-programs.csv"
    s"rm -rf $sportsProgramsDir" !!;
    statsTargetProgramsJoin3.coalesce(1).write.format("com.databricks.spark.csv").option("header", "true").save(s"$sportsProgramsDir")
    val matchedGamesDir = s"/tmp/sports-cloud/all-mlb-$domain-matched-games.csv"
    s"rm -rf $matchedGamesDir" !!;
    statsTargetProgramsJoin2.coalesce(1).write.format("com.databricks.spark.csv").option("header", "true").save(s"$matchedGamesDir")
    val totalMatchedCount = statsTargetProgramsJoin2.count
    val totalGamesCount = statsTargetProgramsJoin3.count
    (totalMatchedCount, totalGamesCount, totalMatchedCount.toFloat / totalGamesCount.toFloat * 100)
  }

  val fetchThuuzGames: () => Unit = () => {
    //Fetch from thuuz and update 
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._
    val thuuzGamesDF = spark.read.json("/data/feeds/thuuz.json")
    val thuuzGamesDF1 = thuuzGamesDF.withColumn("gamesExp", explode(thuuzGamesDF.col("ratings"))).drop("ratings")
    val thuuzGamesDF11 = thuuzGamesDF1.select($"gamesExp.gex_predict" as "gexPredict", $"gamesExp.pre_game_teaser" as "preGameTeaser", $"gamesExp.external_ids.stats.game" as "statsGameCode");
    val thuuzGamesDF20 = thuuzGamesDF11.withColumn("gameCodeTmp", thuuzGamesDF11("statsGameCode").cast(LongType)).drop("statsGameCode").withColumnRenamed("gameCodeTmp", "statsGameCode")
    thuuzGamesDF20.createOrReplaceTempView("thuuzGames")

  }

  val fetchSportsChannels: () => DataFrame = () => {
    //fetch summary json
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._
    val summaryJson = spark.read.json("/data/feeds/summary.json")
    val subPackIds = summaryJson.select($"subscriptionpacks")
    val subPackIds2 = subPackIds.withColumn("subpacksExploded", explode($"subscriptionpacks")).drop("spIdsExploded");
    val subPackIds21 = subPackIds2.select(children("subpacksExploded", subPackIds2): _*).withColumnRenamed("title", "subpack_title").withColumnRenamed("subpack_id", "subpackage_guid").where("subpack_title != 'Ops Test' OR subpack_title != 'Ops Test - D'");

    val channelsSummaryJsonDF = summaryJson.select($"channels");
    val channelsSummaryJsonDF1 = channelsSummaryJsonDF.withColumn("channels", explode(channelsSummaryJsonDF.col("channels")));
    val channelsSummaryJsonDF2 = channelsSummaryJsonDF1.select(children("channels", channelsSummaryJsonDF1): _*)
    val channelsSummaryJsonDF3 = channelsSummaryJsonDF2.select($"channel_guid", $"channel_number" as "channel_no", $"subscriptionpack_ids", $"metadata.genre" as "genre", $"metadata.call_sign" as "callsign")
    val channelsSummaryJsonDF4 = channelsSummaryJsonDF3.withColumn("subPackExploded", explode($"subscriptionpack_ids")).drop("subscriptionpack_ids").withColumnRenamed("subPackExploded", "subpack_int_id")
    //Total Channels == 9210
    val channelsSummaryJsonDF5 = channelsSummaryJsonDF4.withColumn("genreExploded", explode($"genre")).drop("genre")
    //Total Sports channels== 1357 (14%)
    // Filter only sports channels
    val channelsSummaryJsonDF6 = channelsSummaryJsonDF5.filter("genreExploded='Sports'").drop("genreExploded")
    val summaryJson6 = channelsSummaryJsonDF6.join(subPackIds21, channelsSummaryJsonDF6("subpack_int_id") === subPackIds21("id"), "inner").drop("id")
    summaryJson6
  }

  val fetchProgramSchedules: (DataFrame) => Unit = (summaryJson6: DataFrame) => {
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._
    val linearFeedDF = spark.read.json("/data/feeds/schedules_plus_3")
    val programsDF1 = linearFeedDF.select($"_self", $"programs")
    val programsDF2 = programsDF1.withColumn("programsExploded", explode(programsDF1.col("programs"))).drop("programs");
    programsDF2.groupBy("programsExploded.name").count.count

    val programsDF21 = programsDF2.withColumn("genres", listToStrUDF($"programsExploded.genres"))
    val programsDF3 = programsDF21.select($"_self", $"programsExploded.id" as "program_id", $"programsExploded.guid" as "program_guid", $"programsExploded.name" as "program_title", $"genres")
    val programsDF31 = programsDF3.distinct.toDF

    val schedulesDF = linearFeedDF.select($"schedule")
    val schedulesDF1 = schedulesDF.withColumn("schedulesExp", explode(schedulesDF.col("schedule"))).drop("schedule");
    val schedulesDF2 = schedulesDF1.select($"schedulesExp.program_id" as "s_program_id", $"schedulesExp.asset_guid", $"schedulesExp.start" as "start_time")
    val schedulesDF3 = schedulesDF2.withColumn("start_time_epoch", timeISO8601ToEpochUDF($"start_time"))

    val programScheduleJoinDF = programsDF31.join(schedulesDF3, programsDF31("program_id") === schedulesDF3("s_program_id"), "inner").drop("s_program_id")
    val programScheduleJoinDF1 = programScheduleJoinDF.distinct.toDF

    val programScheduleJoinDF3 = programScheduleJoinDF1.withColumn("channel_guid_extr", channelGuidUDF($"_self")).drop("_self")
    val programScheduleJoinDF4 = programScheduleJoinDF3.coalesce(3)
    CMHolder.log.trace("Joining summary linear channels with program schedule")
    val programScheduleChannelJoin = summaryJson6.join(programScheduleJoinDF4, summaryJson6("channel_guid") === programScheduleJoinDF4("channel_guid_extr"), "inner").drop("channel_guid_extr")
    val programScheduleChannelJoin0 = programScheduleChannelJoin.coalesce(3)

    programScheduleChannelJoin0.createOrReplaceTempView("programSchedules")
  }

  val fetchMLBSchedule: () => DataFrame = () => {
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._
    val ds1 = spark.read.format("kafka").option("kafka.bootstrap.servers", "localhost:9092").option("subscribe", "content_match").load()
    val ds2 = ds1.selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)").as[(String, String)]

    val scoreSchema = StructType(StructField("score", StringType, true) :: Nil)

    val nameSchema = StructType(StructField("name", StringType, true) 
        :: StructField("player-code", StringType, true) 
        :: Nil)
    val playerDataSchema = StructType(StructField("player-data", nameSchema, true) :: Nil)

    val teamSchema = StructType(StructField("team-name", StringType, true)
        :: StructField("team-city", StringType, true) 
        :: StructField("team-code", StringType, true) 
        :: Nil)
    val dateSchema = StructType(StructField("month", StringType, true) 
        :: StructField("date", StringType, true)
        :: StructField("day", StringType, true) 
        :: StructField("year", StringType, true)
        :: Nil)
    val timeSchema = StructType(StructField("hour", StringType, true) 
        :: StructField("minute", StringType, true) 
        :: StructField("utc-hour", StringType, true) 
        :: StructField("utc-minute", StringType, true) 
        :: Nil)

    val gameScheduleItemSchema = StructType(StructField("stadium", nameSchema, true)
        :: StructField("visiting-team-score", scoreSchema, true) 
        :: StructField("home-team-score", scoreSchema, true)
        :: StructField("away-starting-pitcher", playerDataSchema, true) 
        :: StructField("home-starting-pitcher", playerDataSchema, true)
        :: StructField("home-team", teamSchema, true) 
        :: StructField("visiting-team", teamSchema, true) 
        :: StructField("date", dateSchema, true) 
        :: StructField("time", timeSchema, true)
        :: StructField("gamecode", StringType, true) 
        :: StructField("league", StringType, true) 
        :: StructField("sport", StringType, true) 
        :: Nil)
    val gameShceduleSchema = StructType(StructField("game-schedule", gameScheduleItemSchema, true) :: Nil)
    //only pickup MLB games for now
    val ds3 = ds2.where("key like '%MLB_SCHEDULE.XML%'")
    val ds4 = ds3.select(from_json($"key", StructType(StructField("payload", StringType, true) :: Nil)) as "fileName", from_json($"value", StructType(StructField("payload", StringType, true) :: Nil)) as "payloadStruct")
    val ds5 = ds4.select($"fileName", from_json($"payloadStruct.payload", gameShceduleSchema) as "gameScheduleStruct")

    val ds6 = ds5.filter($"gameScheduleStruct.game-schedule".isNotNull).select($"gameScheduleStruct.game-schedule")

    val mlbScheduleDF3 = ds6.select($"game-schedule.visiting-team.team-city" as "awayTeamCity",
      $"game-schedule.visiting-team.team-name" as "awayTeamName",
      $"game-schedule.visiting-team.team-code" as "awayTeamExternalId",
      $"game-schedule.home-team.team-city" as "homeTeamCity",
      $"game-schedule.home-team.team-code" as "homeTeamExternalId",
      $"game-schedule.home-team.team-name" as "homeTeamName",
      $"game-schedule.home-team-score.score" as "homeTeamScore",
      $"game-schedule.visiting-team-score.score" as "awayTeamScore",
      $"game-schedule.away-starting-pitcher.player-data.name" as "awayTeamPitcherName",
      $"game-schedule.home-starting-pitcher.player-data.name" as "homeTeamPitcherName",
      $"game-schedule.home-starting-pitcher.player-data.player-code" as "homePlayerExtId",
      $"game-schedule.away-starting-pitcher.player-data.player-code" as "awayPlayerExtId",
      $"game-schedule.gamecode" as "gameCode",
      $"game-schedule.league" as "league",
      $"game-schedule.sport" as "sport",
      $"game-schedule.stadium.name" as "stadiumName",
      concat(col("game-schedule.date.year"), lit("-"), lit(getZeroPaddedUDF($"game-schedule.date.month")), lit("-"), lit(getZeroPaddedUDF($"game-schedule.date.date")), lit("T"), col("game-schedule.time.hour"), lit(":"), col("game-schedule.time.minute"), lit(":00.00"), lit(getZeroPaddedUDF($"game-schedule.time.utc-hour")), lit(":"), col("game-schedule.time.utc-minute")) as "date")

    val mlbScheduleDF31 = mlbScheduleDF3.withColumn("game_date_epoch", timeStrToEpochUDF($"date")).withColumn("gameDate", timeEpochtoStrUDF($"game_date_epoch"))
    val mlbScheduleDF311 = mlbScheduleDF31.withColumn("anonsTitle", getAnonsTitleUDF($"homeTeamName", $"awayTeamName", $"stadiumName"))
    val mlbScheduleDF32 = mlbScheduleDF311.distinct.toDF.na.fill(0L, Seq("homeTeamScore")).na.fill(0L, Seq("awayTeamScore"))
    mlbScheduleDF32

  }

  val intersectWithThuuz: DataFrame => DataFrame = (mlbScheduleDF32: DataFrame) => {
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._
    //join with thuuz to get gex score
    val thuuzGamesDF2 = spark.sql("select * from thuuzGames").toDF
    CMHolder.log.trace("Joining thuuzgames with stats data")
    val mlbScheduleDF33 = mlbScheduleDF32.join(thuuzGamesDF2, mlbScheduleDF32("gameCode") === thuuzGamesDF2("statsGameCode"), "left").drop("statsGameCode")
    val mlbScheduleDF34 = mlbScheduleDF33.na.fill(0L, Seq("gexPredict"))
    mlbScheduleDF34
  }

  val writeData: ( String, String, DataFrame) => Unit = ( outputCollName: String, zkHost: String, programsJoin3: DataFrame) => {
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._
    val batchTimeStamp = Instant.now().getEpochSecond
    val programsJoin4 = programsJoin3.withColumn("batchTime", lit(batchTimeStamp))
    programsJoin4.createOrReplaceTempView("programsJoin4")
    val programsJoin5 = spark.sql("select concat(channel_guid,'_',program_id) as id , * from programsJoin4")
    if (programsJoin5.count > 0) {
      val solrOpts = Map("zkhost" -> zkHost, "collection" -> outputCollName, ConfigurationConstants.GENERATE_UNIQUE_KEY -> "false")
      programsJoin5.write.format("solr").options(solrOpts).mode(org.apache.spark.sql.SaveMode.Overwrite).save()
      val solrCloudClient = SolrSupport.getCachedCloudClient(zkHost)
      solrCloudClient.commit(outputCollName, true, true)
    }

  }

}