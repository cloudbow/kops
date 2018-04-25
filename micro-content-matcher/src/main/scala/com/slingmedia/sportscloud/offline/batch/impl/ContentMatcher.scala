package com.slingmedia.sportscloud.offline.batch.impl

import com.slingmedia.sportscloud.offline.batch.Muncher

import org.apache.spark.sql.{ SQLContext, SparkSession, DataFrame, Row, Column }
import org.apache.spark.sql.functions.{ concat_ws, concat, lit, coalesce, max, min, udf, col, explode, from_json, collect_list }
import org.apache.spark.sql.types.{ StructType, StructField, StringType, IntegerType, LongType, ArrayType }

import sys.process._
import scala.language.postfixOps
import scala.collection.mutable.{ WrappedArray , ListBuffer }
import scala.util.{ Try, Success, Failure }


import java.time.{ ZonedDateTime,LocalDateTime, OffsetDateTime, ZoneOffset, Instant, ZoneId }
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

import com.databricks.spark.csv

import org.apache.spark.SparkFiles


import java.io.File
import java.nio.file.{ Paths, Files }

import org.slf4j.LoggerFactory


object CMHolder extends Serializable {
  val serialVersionUID = 1L
  @transient lazy val log = LoggerFactory.getLogger("ContentMatcher")
}

object ContentMatcher extends Serializable {
  def main(args: Array[String]) {
    CMHolder.log.debug("Args is $args")
    //"content_match", "game_schedule"
    new ContentMatcher().munch(args(0), args(1))
      //new ContentMatcher().test(args(0), args(1))
  }
}

class ContentMatcher extends Serializable with Muncher {


  override def munch(inputKafkaTopic: String, outputCollName: String): Unit = {
    val spark = SparkSession.builder().getOrCreate()
    try {
      //fetch thuuz games
      fetchThuuzGames(System.getenv("ARTIFACT_SERVER_EP"))

      //fetch sports channels
      val summaryJson6 = fetchSportsChannels(System.getenv("ARTIFACT_SERVER_EP"))

      //fetch program schedules
      fetchProgramSchedules(summaryJson6, System.getenv("ARTIFACT_SERVER_EP"))

      //Fetch MLB schedule
      val mlbScheduleDF32 = fetchMLBSchedule(inputKafkaTopic)

      //get the matched data
      val programsJoin3 = contentMatch(mlbScheduleDF32, inputKafkaTopic)

      //Write delta back to mongo

      writeData(outputCollName, programsJoin3)
    } finally {
      spark.close
    }

  }

  //All UDFs are here which actually does multiple functions on a single column in a Ros
  val array_ = udf(() => Array.empty[String])

  val getAnonsTitle: (String, String, String) => String = (homeTeamName: String, awayTeamName: String, stadium: String) => {
    var anonsTitle = awayTeamName.concat(" take on ").concat(homeTeamName)
    if (stadium != null) {
      anonsTitle = anonsTitle.concat(" at ").concat(stadium)
    }
    anonsTitle
  }
  val getAnonsTitleUDF = udf(getAnonsTitle(_: String, _: String, _: String))

  val timeISO8601ToEpochFunc: (String => Long) = (timeStr: String) => {
    if (timeStr == null) 0L else Instant.parse(timeStr).getEpochSecond()
  }
  val timeISO8601ToEpochUDF = udf(timeISO8601ToEpochFunc(_: String))
  
  val timeSchToEpochFunc: (String => Long) = (timeStr: String) => {
    if (timeStr == null || timeStr == "" ) 0L else LocalDateTime.parse(timeStr.concat("T00:00"), DateTimeFormatter.ofPattern ("yyyyMMdd'T'HH:mm")).toInstant(ZoneOffset.UTC).getEpochSecond()
  }
  val timeSchToEpochUDF = udf(timeSchToEpochFunc(_: String))

  val addDeduplicateLogic: (String, String) => String = (programTitle: String, genre: String) => {
    if (programTitle == null) programTitle else if (programTitle.startsWith(genre)) programTitle else genre.toUpperCase.concat("  ").concat(programTitle)
  }
  val partialWithMLB = udf(addDeduplicateLogic(_: String, "MLB:"))

  val makeImgUrlFunc: (String, String) => String = (teamExternalId: String, league: String) => {
    if (teamExternalId == null) {
      "http://qahsports.slingbox.com/sport_teams/baseball/mlb/gid1.png"
    } else {
      league match {
        case "NCAAF" =>
          "http://qahsports.slingbox.com/sport_teams/football/ncaaf/gid".concat(teamExternalId).concat(".png")
        case "NFL" =>
          "http://qahsports.slingbox.com/sport_teams/football/nfl/gid".concat(teamExternalId).concat(".png")
        case _ =>
          "http://qahsports.slingbox.com/sport_teams/baseball/mlb/gid".concat(teamExternalId).concat(".png")
      }

    }
  }
  val makeImgUrl = udf(makeImgUrlFunc(_:String,_:String))

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
  
  
  val getSelfTimeUDFFunc: (String => String) = (url: String) => {
    if (url != null) {
      var chUrl = url.slice(url.lastIndexOf("/"),url.length)
      chUrl = chUrl.substring(chUrl.lastIndexOf("/") + 1)
      chUrl
    } else {
      ""
    }
  }
  val selfTimeUDF = udf(getSelfTimeUDFFunc(_: String))


  //UDF definitions ends here

  val fetchMLBSchedule: (String => DataFrame) = (inputKafkaTopic: String) => {
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._
    val ds1 = spark.read.format("kafka").option("kafka.bootstrap.servers", System.getenv("KAFKA_BROKER_EP")).option("subscribe", inputKafkaTopic).load()
    val ds2 = ds1.selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)").as[(String, String)]

    val scoreSchema = StructType(StructField("score", StringType, true) :: Nil)

    val nameSchema = StructType(StructField("name", StringType, true)
      :: StructField("player-code", StringType, true)
      :: Nil)
    val playerDataSchema = StructType(StructField("player-data", nameSchema, true) :: Nil)

    val teamSchema = StructType(StructField("team-name", StringType, true)
      :: StructField("team-alias", StringType, true)
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
      :: StructField("gameId", StringType, true)
      :: StructField("gameCode", StringType, true)
      :: StructField("status", StringType, true)
      :: StructField("statusId", IntegerType, true)
      :: StructField("gameType", StringType, true)
      :: StructField("league", StringType, true)
      :: StructField("sport", StringType, true)
      :: Nil)
    val gameShceduleSchema = StructType(StructField("game-schedule", gameScheduleItemSchema, true) :: Nil)
    //only pickup MLB games for now
    val ds3 = ds2.where("key like '%_SCHEDULE.XML%'")

    val ds4 = ds3.select(from_json($"key", StructType(StructField("payload", StringType, true) :: Nil)) as "fileName", from_json($"value", StructType(StructField("payload", StringType, true) :: Nil)) as "payloadStruct")
    val ds5 = ds4.select($"fileName", from_json($"payloadStruct.payload", gameShceduleSchema) as "gameScheduleStruct")

    val ds6 = ds5.filter($"gameScheduleStruct.game-schedule".isNotNull).select($"gameScheduleStruct.game-schedule")

    val mlbScheduleDF3 = ds6.select($"game-schedule.visiting-team.team-city" as "awayTeamCity",
      $"game-schedule.visiting-team.team-name" as "awayTeamName",
      $"game-schedule.visiting-team.team-alias" as "awayTeamAlias",
      $"game-schedule.visiting-team.team-code" as "awayTeamExternalId",
      $"game-schedule.home-team.team-city" as "homeTeamCity",
      $"game-schedule.home-team.team-code" as "homeTeamExternalId",
      $"game-schedule.home-team.team-name" as "homeTeamName",
      $"game-schedule.home-team.team-alias" as "homeTeamAlias",
      $"game-schedule.home-team-score.score" as "homeTeamScore",
      $"game-schedule.visiting-team-score.score" as "awayTeamScore",
      $"game-schedule.away-starting-pitcher.player-data.name" as "awayTeamPitcherName",
      $"game-schedule.home-starting-pitcher.player-data.name" as "homeTeamPitcherName",
      $"game-schedule.home-starting-pitcher.player-data.player-code" as "homePlayerExtId",
      $"game-schedule.away-starting-pitcher.player-data.player-code" as "awayPlayerExtId",
      $"game-schedule.gameId" as "gameId",
      $"game-schedule.gameCode" as "gameCode",
      $"game-schedule.status" as "status",
      $"game-schedule.statusId" as "statusId",
      $"game-schedule.gameType" as "gameType",
      $"game-schedule.league" as "league",
      $"game-schedule.sport" as "sport",
      $"game-schedule.stadium.name" as "stadiumName",
      concat(col("game-schedule.date.year"), lit("-"),
        lit(zeroPadDateTimeUDF($"game-schedule.date.month")), lit("-"),
        lit(zeroPadDateTimeUDF($"game-schedule.date.date")), lit("T"),
        lit(zeroPadDateTimeUDF($"game-schedule.time.hour")), lit(":"),
        lit(zeroPadDateTimeUDF($"game-schedule.time.minute")), lit(":00.00"),
        lit(zeroPadTimeOffsetUDF($"game-schedule.time.utc-hour",$"game-schedule.time.utc-minute"))) as "date")


    //val leagueCheck = mlbScheduleDF3.first();
    // get the league inorder to process the images
    //val league =  mlbScheduleDF3.select($"league").first.getString(0)

    val mlbScheduleDF31 = mlbScheduleDF3.
      withColumn("game_date_epoch", timeStrToEpochUDF($"date")).
      withColumn("gameDate", timeEpochtoStrUDF($"game_date_epoch"))
    val mlbScheduleDF311 = mlbScheduleDF31.
      withColumn("anonsTitle", getAnonsTitleUDF($"homeTeamName", $"awayTeamName", $"stadiumName")).
      withColumn("homeTeamImg", makeImgUrl($"homeTeamExternalId", $"league")).
      withColumn("awayTeamImg", makeImgUrl($"awayTeamExternalId", $"league"))

    val mlbScheduleDF32 = mlbScheduleDF311.distinct.toDF.
      na.fill(0L, Seq("homeTeamScore")).
      na.fill(0L, Seq("awayTeamScore")).
      na.fill("TBA", Seq("homeTeamName")).
      na.fill("TBA", Seq("awayTeamName"))

    mlbScheduleDF32

  }

  val intersectWithThuuz: DataFrame => DataFrame = (mlbScheduleDF32: DataFrame) => {
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._
    //join with thuuz to get gex score
    val thuuzGamesDF2 = spark.sql("select * from thuuzGames").toDF

    CMHolder.log.trace("Joining thuuzgames with stats data")
    val mlbScheduleDF33 = mlbScheduleDF32.join(thuuzGamesDF2, mlbScheduleDF32("gameId") === thuuzGamesDF2("statsGameId"), "left").drop("statsGameId")
    val mlbScheduleDF34 = mlbScheduleDF33.na.fill(0L, Seq("gexPredict"))
    mlbScheduleDF34
  }

  val regexpLookupMap = Map(
    "homeTeamName"-> Map("awayTeamName" -> "^.*?\\s*#homeTeamName#\\s*(vs\\.|at)\\s*#awayTeamName#.*$",
      "awayTeamCity" ->
        Map("awayTeamName" -> "^.*?\\s*#homeTeamName#\\s*(vs\\.|at)\\s*#awayTeamCity#.*#awayTeamName#.*$"),
      "homeTeamCity" ->
        Map("awayTeamName" ->
          Map("awayTeamCity" -> "^.*?\\s*#homeTeamName#.*#homeTeamCity#\\s*(vs\\.|at)\\s*#awayTeamName#.*#awayTeamCity#.*$" ))
    ),
    "homeTeamCity"-> Map("homeTeamName" ->
      Map("awayTeamCity" ->
        Map("awayTeamName" -> "^.*?\\s*#homeTeamCity#.*#homeTeamName#\\s*(vs\\.|at)\\s*#awayTeamCity#.*#awayTeamName#.*$" ),
        "awayTeamName" -> "^.*?\\s*#homeTeamCity#.*#homeTeamName#\\s*(vs\\.|at)\\s*#awayTeamName#.*$"),
      "awayTeamCity" -> "^.*?\\s*#homeTeamCity#\\s*(vs\\.|at)\\s*#awayTeamCity#.*$"
    ),
    "awayTeamName"-> Map("homeTeamName" ->  "^.*?\\s*#awayTeamName#\\s*(vs\\.|at)\\s*#homeTeamName#.*$",
      "homeTeamCity" ->
        Map("homeTeamName" -> "^.*?\\s*#awayTeamName#\\s*(vs\\.|at)\\s*#homeTeamCity#.*#homeTeamName#.*$"),
      "awayTeamCity" ->
        Map("homeTeamName" ->
          Map("homeTeamCity" -> "^.*?\\s*#awayTeamName#.*#awayTeamCity#\\s*(vs\\.|at)\\s*#homeTeamName#.*#homeTeamCity#.*$" ))
    ),
    "awayTeamCity"-> Map("awayTeamName" ->
      Map("homeTeamName" -> "^.*?\\s*#awayTeamCity#.*#awayTeamName#\\s*(vs\\.|at)\\s*#homeTeamName#.*$",
        "homeTeamCity" ->
          Map("homeTeamName" -> "^.*?\\s*#awayTeamCity#.*#awayTeamName#\\s*(vs\\.|at)\\s*#homeTeamCity#.*#homeTeamName#.*$")),
      "homeTeamCity" -> "^.*?\\s*#awayTeamCity#\\s*(vs\\.|at)\\s*#homeTeamCity#.*$"
    )
  )




  val regexpMapTransformer:((ListBuffer[(Seq[String],String)] ,Map[String,Object] ) => ( ListBuffer[(Seq[String],String)]))  = (acc: ListBuffer[(Seq[String],String)] ,inputMap: Map[String,Object] ) => {

    var level=0
    var allColsFlattened: ListBuffer[String] = ListBuffer()
    def decomposeMap(acc: ListBuffer[(Seq[String],String)] ,inputMap: Map[String,Object]): ListBuffer[(Seq[String],String)] = {
      level+=1;
      for((name,value) <- inputMap  ) yield {
        allColsFlattened = (allColsFlattened take level-1) ++ ListBuffer(name)
        value match {
          case y:String =>
            acc += ((allColsFlattened.toSeq ,y))
          case y:Object => decomposeMap(acc,y.asInstanceOf[Map[String,Object]])
        }
      }
      level-=1
      allColsFlattened = allColsFlattened take level
      acc
    }

    decomposeMap(acc,inputMap)


  }


  val flattenedRegexpMap = regexpMapTransformer(ListBuffer(),regexpLookupMap)
  val allPossibleTotalRegexps = flattenedRegexpMap.size

  val fetchMaxTotalColumns:(List[(Seq[String],String)]=>Int)  =  (inputList: List[(Seq[String],String)]) => {

    def fetchMax(inputList: List[(Seq[String],String)],acc:Int):Int = {
      inputList match {
        case List() => acc
        case x:List[(Seq[String],String)] => {
          val currSize = x.take(1)(0)._1.size
          val max = if(currSize>acc) currSize else acc
          fetchMax(x.drop(1),max)
        }
      }
    }
    fetchMax(inputList,0)
  }

  val maxTotalColumns = fetchMaxTotalColumns(flattenedRegexpMap.toList)


  val notMatchingRegexp =  "(?!.*)"
  val getMapValueRecursively:(List[String],Map[String,Object])=>(String) = (colNames:List[String],inputMap:Map[String,Object]) => {
    colNames match {
      case Nil =>
        notMatchingRegexp //return empty regexp value if key is not found
      case x :: xs =>
        try {
          val regexpLookBack = inputMap(x)
          regexpLookBack match {
            case y:Object => if(y.isInstanceOf[String]) y.asInstanceOf[String] else  getMapValueRecursively(xs, y.asInstanceOf[Map[String,Object]])
          }
        } catch {
          case e:java.util.NoSuchElementException =>  notMatchingRegexp
        }
    }
  }


  val replaceTemplate: ((List[String],List[String],String)=>String)  = (columnNames:List[String],columns:List[String], regexpTemplate:String) => {
    var regexp=regexpTemplate
    columnNames zip columns map {
      it =>
        it match {
          case (colName,colValue) => {
            regexp = regexp.replaceAll("#"+colName+"#",colValue)
          }
        }
    }
    regexp
  }

  def lookupAndCreateColumn(allColNames:List[String],column4:String,column3:String,column2:String,column1:String):String = {
    val columns =List(column1, column2,column3,column4) filter { _ != "column_not_defined" }
    val allColumnEntries:ListBuffer[String] = ListBuffer()
    var i=0
    allColNames map {
      colName =>
        val col = columns(i)
        allColumnEntries += (if(isEmpty(col)) "" else colName)
        i +=1
    }
    val regexp = getMapValueRecursively(allColumnEntries.toList,regexpLookupMap withDefaultValue "(?!.*)" )
    replaceTemplate(allColNames.toList,columns,regexp)
  }

  val addColumnsToDF: (DataFrame,ListBuffer[(Seq[String],String)])=>DataFrame = (inputDF:DataFrame,allColNames:ListBuffer[(Seq[String],String)]) => {
    var j=0
    var finalDF = inputDF
    flattenedRegexpMap map {
      it =>
        j += 1
        val allColNames = it._1
        val regexp = it._2
        val allCols = allColNames map { col => if(isEmpty(col)) lit("") else inputDF(col)  }
        val totalCols = allCols size
        val colsNotDefined = maxTotalColumns - totalCols
        var lookupAndCreateColUDF = null:org.apache.spark.sql.expressions.UserDefinedFunction
        colsNotDefined match {
          case 0 => lookupAndCreateColUDF = udf(lookupAndCreateColumn(allColNames.toList,_:String,_:String,_:String,_:String))
          case 1 => lookupAndCreateColUDF = udf(lookupAndCreateColumn(allColNames.toList,"not_defined",_:String,_:String,_:String))
          case 2 => lookupAndCreateColUDF = udf(lookupAndCreateColumn(allColNames.toList,"not_defined","not_defined",_:String,_:String))
          case 3 => lookupAndCreateColUDF = udf(lookupAndCreateColumn(allColNames.toList,"not_defined","not_defined","not_defined",_:String))
          case _ => throw new IllegalArgumentException("Cannot be this number")
        }
        finalDF=finalDF.withColumn("regexp"+j,lookupAndCreateColUDF(allCols reverse :_*))
    }

    finalDF
  }

  def contentMatch(gameScheduleDF: DataFrame, inputKafkaTopic: String, domain: String = "slingtv"): DataFrame = {
    //Fetch from thuuz and update 
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._

    val mlbScheduleDF47 = addColumnsToDF(gameScheduleDF,flattenedRegexpMap)
    val mlbScheduleDF5 = mlbScheduleDF47.coalesce(4)






    //val gameScheduleDF=mlbScheduleDF5
    //val targetProgramsToMatch = programScheduleChannelJoin
    //val domain = "slingtv"

    val programScheduleChannelJoin2 = spark.sql("select * from programSchedules").toDF
    CMHolder.log.trace("Joining with target programs using a cross join")
    val statsTargetProgramsJoin = mlbScheduleDF5.crossJoin(programScheduleChannelJoin2)
    val rlikExpr= new StringBuilder()
    for(i <- 1 to allPossibleTotalRegexps) rlikExpr.
      append("(program_title rlike ").
      append("regexp"+i).
      append(")").
      append(if(i!=allPossibleTotalRegexps) " OR " else "" )
    val statsTargetProgramsJoin1 = statsTargetProgramsJoin.where(
      s"""( startTimeEpoch < game_date_epoch + 3600 AND startTimeEpoch > game_date_epoch - 3600 )
      AND
      (${rlikExpr.toString()}) """.stripMargin.replaceAll("\n", " "))
    CMHolder.log.trace("Matched with stats data");
    val dropCols =  ListBuffer[String]()
    for(i <- 1 to allPossibleTotalRegexps) dropCols += ("regexp"+i)
    val statsTargetProgramsJoin2 = statsTargetProgramsJoin1.
      drop(dropCols.toList:_*).
      drop(
        "subpack_int_id",
        "date")

    val gameScheduleOrig = fetchMLBSchedule(inputKafkaTopic).coalesce(4)
    val allStatsNullFills = Map(
      "program_guid" -> "0",
      "channel_guid" -> "0",
      "schedule_guid" -> "0",
      "callsign" -> "-",
      "asset_guid" -> "-",
      "program_id" -> 0,
      "channel_no" -> 0,
      "startTimeEpoch" -> 0,
      "stopTimeEpoch" -> 0,
      "selfTimeEpoch" -> 0,
      "selfTime" -> "0",
      "type" -> "0")
    val allStatsArrayColumns = Seq("subpackage_guids", "subpack_titles","ratings")
    val allSelectedColumns = gameScheduleOrig.columns.map(gameScheduleOrig(_)) ++ (allStatsNullFills.keys ++ allStatsArrayColumns).map(statsTargetProgramsJoin2(_))
    //join with all stats data to fetch the full schedules for programs not availabe in slingtv
    //deselect duplicate columns by selecting the columns from 
    val statsTargetProgramsJoin4 = gameScheduleOrig.coalesce(4).
      join(statsTargetProgramsJoin2, gameScheduleOrig("gameId").equalTo(statsTargetProgramsJoin2("gameId")), "left_outer").
      select(allSelectedColumns: _*).
      na.fill(allStatsNullFills)
    //fill array columns with empty array
    //A limitation of na.fill
    val statsTargetProgramsJoin5 = statsTargetProgramsJoin4.
      withColumn("subpackage_guids", coalesce($"subpackage_guids", array_())).
      withColumn("ratings", coalesce($"ratings", array_())).
      withColumn("subpack_titles", coalesce($"subpack_titles", array_()))

    //intersect the data with thuuz
    val statsTargetProgramsJoin6 = intersectWithThuuz(statsTargetProgramsJoin5)

    val columns = statsTargetProgramsJoin6.columns
    scala.util.Sorting.quickSort(columns)
    val programsJoin1 = statsTargetProgramsJoin6.select(columns.head, columns.tail: _*)
    val programsJoin2 = programsJoin1.coalesce(4)
    programsJoin2
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

  val fetchThuuzGames: (String) => Unit = (artifactUrl: String) => {
    //Fetch from thuuz and update 
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._
    val thuuzGamesDF = getDFFromHttp("http://api.thuuz.com/2.2/games?auth_code=6adf97f8142118ba&type=normal&status=5&days=5&sport_leagues=baseball.mlb,basketball.nba,basketball.ncaa,football.nfl,football.ncaa,hockey.nhl,golf.pga,soccer.mwc,soccer.chlg,soccer.epl,soccer.seri,soccer.liga,soccer.bund,soccer.fran,soccer.mls,soccer.wwc,soccer.ligamx,soccer.ered,soccer.ch-uefa2,soccer.eng2,soccer.prt1,soccer.sco1,soccer.tur1,soccer.rus1,soccer.bel1,soccer.euro&limit=999")
    val thuuzGamesDF1 = thuuzGamesDF.withColumn("gamesExp", explode(thuuzGamesDF.col("ratings"))).drop("ratings")
    val thuuzGamesDF11 = thuuzGamesDF1.select($"gamesExp.gex_predict" as "gexPredict", $"gamesExp.pre_game_teaser" as "preGameTeaser", $"gamesExp.external_ids.stats.game" as "statsGameId")
    val thuuzGamesDF20 = thuuzGamesDF11.withColumn("gameIdTmp", thuuzGamesDF11("statsGameId").cast(LongType)).drop("statsGameId").withColumnRenamed("gameIdTmp", "statsGameId")
    thuuzGamesDF20.createOrReplaceTempView("thuuzGames")

  }

  val fetchSportsChannels: (String) => DataFrame = (artifactUrl: String) => {
    //fetch summary json
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._
    val summaryJson = getDFFromHttp(s"http://$artifactUrl/artifacts/slingtv/sports-cloud/summary.json")
    val subPackIds = summaryJson.select($"subscriptionpacks")
    val subPackIds2 = subPackIds.withColumn("subpacksExploded", explode($"subscriptionpacks")).drop("spIdsExploded");

    val subPackIds21 = subPackIds2.select(children("subpacksExploded", subPackIds2): _*).withColumnRenamed("title", "subpack_title").withColumnRenamed("subpack_id", "subpackage_guid")

    val channelsSummaryJsonDF = summaryJson.select($"channels");
    val channelsSummaryJsonDF1 = channelsSummaryJsonDF.withColumn("channels", explode(channelsSummaryJsonDF.col("channels")));
    //filter for only offered channels . avoid dummy channels
    val channelsSummaryJsonDF2 = channelsSummaryJsonDF1.select(children("channels", channelsSummaryJsonDF1): _*).
    filter("offered = true");
    val channelsSummaryJsonDF3 = channelsSummaryJsonDF2.select($"channel_guid", $"channel_number" as "channel_no", $"subscriptionpack_ids", $"metadata.genre" as "genre", $"metadata.call_sign" as "callsign")
    val channelsSummaryJsonDF4 = channelsSummaryJsonDF3.withColumn("subPackExploded", explode($"subscriptionpack_ids")).drop("subscriptionpack_ids").withColumnRenamed("subPackExploded", "subpack_int_id")
    //Total Channels == 9210
    val channelsSummaryJsonDF5 = channelsSummaryJsonDF4.withColumn("genreExploded", explode($"genre")).drop("genre")
    //Total Sports channels== 1357 (14%)
    val summaryJson6 = channelsSummaryJsonDF5.join(
      subPackIds21, channelsSummaryJsonDF5("subpack_int_id") === subPackIds21("id"), "inner"
    ).
      drop("id").
      drop("genreExploded")
    val summaryJson7 = summaryJson6.groupBy($"channel_guid", $"channel_no", $"callsign").agg(Map(
      "subpack_int_id" -> "collect_list",
      "subpackage_guid" -> "collect_list",
      "subpack_title" -> "collect_list")).
      withColumnRenamed("collect_list(subpack_int_id)", "subpack_int_ids").
      withColumnRenamed("collect_list(subpackage_guid)", "subpackage_guids").
      withColumnRenamed("collect_list(subpack_title)", "subpack_titles")
    summaryJson7
  }

  val fetchProgramSchedules: (DataFrame, String) => Unit = (summaryJson6: DataFrame, artifactUrl:String) => {
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._
    val linearFeedDF = getDFFromHttp(s"http://$artifactUrl/artifacts/slingtv/sports-cloud/schedules_plus_3")

    val programsDF1 = linearFeedDF.select($"_self", $"programs")
    val programsDF2 = programsDF1.withColumn("programsExploded", explode(programsDF1.col("programs"))).drop("programs");
    //Dont enable this . will eat up cpu in prod
    //programsDF2.groupBy("programsExploded.name").count.count

    val programsDF21 = programsDF2.withColumn("genres", listToStrUDF($"programsExploded.genres"))
    val programsDF3 = programsDF21.select($"_self", $"programsExploded.id" as "program_id", $"programsExploded.guid" as "program_guid", $"programsExploded.name" as "program_title", $"genres", $"programsExploded.ratings" as "ratings", $"programsExploded.type" as "type").
    withColumn("channel_guid_extr", channelGuidUDF($"_self")).
    withColumn("selfTime", selfTimeUDF($"_self")).
    withColumn("selfTimeEpoch",timeSchToEpochUDF($"selfTime")).
    withColumn("program_channel_guid_key_left",concat_ws("_",$"channel_guid_extr",$"program_id")).
    drop("_self")

    val programsDF31 = programsDF3.distinct.toDF

    val schedulesDF = linearFeedDF.select($"_self",$"schedule")
    val schedulesDF1 = schedulesDF.withColumn("schedulesExp", explode(schedulesDF.col("schedule"))).drop("schedule");
    val schedulesDF2 = schedulesDF1.select($"_self",$"schedulesExp.program_id" as "s_program_id", $"schedulesExp.asset_guid", $"schedulesExp.start" as "startTime",$"schedulesExp.stop" as "stopTime", $"schedulesExp.guid" as "schedule_guid")
    val schedulesDF3 = schedulesDF2.withColumn("startTimeEpoch", timeISO8601ToEpochUDF($"startTime")).
                                   withColumn("stopTimeEpoch", timeISO8601ToEpochUDF($"stopTime")).
                                   withColumn("channel_guid_extr", channelGuidUDF($"_self")).
                                   withColumn("program_channel_guid_key_right",concat_ws("_",$"channel_guid_extr",$"s_program_id")).
                                   drop("channel_guid_extr").
                                   drop("_self")



    val programScheduleJoinDF = programsDF31.join(schedulesDF3, programsDF31("program_channel_guid_key_left") === schedulesDF3("program_channel_guid_key_right"), "inner").
    drop("s_program_id").
    drop("program_channel_guid_key_left").
    drop("program_channel_guid_key_right")


    val programScheduleJoinDF1 = programScheduleJoinDF.distinct.toDF
    //fetch the channel_guid from self 
    val programScheduleJoinDF4 = programScheduleJoinDF1.coalesce(4)
    CMHolder.log.trace("Joining summary linear channels with program schedule")
    val programScheduleChannelJoin = summaryJson6.join(programScheduleJoinDF4, summaryJson6("channel_guid") === programScheduleJoinDF4("channel_guid_extr"), "inner").drop("channel_guid_extr")
    val programScheduleChannelJoin0 = programScheduleChannelJoin.coalesce(4)

    programScheduleChannelJoin0.createOrReplaceTempView("programSchedules")
  }


  val writeData: (String, DataFrame) => Unit = (outputCollName: String, programsJoin3: DataFrame) => {
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._
    val batchTimeStamp = Instant.now().getEpochSecond
    val programsJoin4 = programsJoin3.withColumn("batchTime", lit(batchTimeStamp))
    programsJoin4.createOrReplaceTempView("programsJoin4")
    val programsJoin5 = spark.sql("select concat(channel_guid,'_',program_id,'_',gameId) as id , * from programsJoin4")
    val programsJoin6 = programsJoin5.orderBy($"channel_guid",$"program_id",$"selfTimeEpoch").repartition($"gameId").coalesce(4)

    indexResults("sc-game-schedule", outputCollName, programsJoin6)

  }

}
