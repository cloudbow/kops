package com.slingmedia.sportscloud.offline.batch

import org.apache.spark.sql.{SQLContext , SparkSession , DataFrame }
import org.apache.spark.{ SparkConf, SparkContext }
import org.apache.spark.sql.{ Row , Column }
import org.apache.spark.sql.types.{ StructType, StructField, StringType, IntegerType }
import org.apache.spark.sql.DataFrame
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.sql.functions.{ concat, lit, coalesce, max , min , udf , col , explode , from_json , collect_list}
import org.apache.spark.sql.types.{ StructType, StructField, StringType, IntegerType,LongType,ArrayType };

import com.mongodb.spark.config._
import com.mongodb.spark.MongoSpark;
import com.mongodb.spark._ 
import com.mongodb.spark.config.ReadConfig  
import com.mongodb.spark.sql._  

import org.bson.Document;

import sys.process._

import java.time.{ ZonedDateTime , OffsetDateTime , ZoneOffset , Instant, ZoneId }
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit;

import com.databricks.spark.csv

import java.io.File
import java.nio.file.{Paths, Files}

import scala.collection.mutable.WrappedArray

import org.slf4j.LoggerFactory;

import com.typesafe.scalalogging.slf4j.LazyLogging  
import com.typesafe.scalalogging.slf4j.Logger

object Holder extends Serializable {
  val serialVersionUID = 1L;
  @transient lazy val log = LoggerFactory.getLogger("SyntheticInputGenerator")
}

case class GameEvent(id:String,assetGuid:String,awayTeamCity:String,awayTeamExternalId:String,awayTeamImg:String,awayTeamName:String,awayTeamPitcherName:String,awayTeamScore:String,callsign:String,channelGuid:String,channelNo:Long,gameCode:String,gameDateEpoch:Long,genres:String,gexPredict:Long,homeTeamCity:String,homeTeamExternalId:String,homeTeamImg:String,homeTeamName:String,homeTeamPitcherName:String,homeTeamScore:String,programGuid:String,programId:Long,programTitle:String,subpackTitle:String,subpackageGuid:String)
case class GameEvents(id:String, gameEvents:Array[GameEvent])


object ContentMatcher extends Serializable {
	//parse linear feed and get mapping from subpackage_id -> channel_guid -> [echostar_content_ids]
	val children:(String,DataFrame)=>Array[Column] = (colname: String, df: DataFrame) => {
	  val parent = df.schema.fields.filter(_.name == colname).head
	  val fields = parent.dataType match {
	    case x: StructType => x.fields
	    case _ => Array.empty[StructField]
	  }
	  fields.map(x => col(s"$colname.${x.name}"))
	}

	def getZeroPaddedFunc:(String=>String) = (timeStr: String) => {
		val timeInInt=timeStr.toInt
		if (timeInInt < 0) {   
			val absTime = Math.abs(timeInInt)
			"-".concat(getZeroPaddedFunc(absTime.toString))
		}  else if(timeInInt < 10 ) {
			"0".concat(timeStr) 
		}  else  {
			timeStr
		}
	}
	val getZeroPaddedUDF = udf(getZeroPaddedFunc(_: String))

	def timeStrToEpoch:(String=>Long) = (timeStr:String) => {
		if(timeStr==null) 0L else OffsetDateTime.parse(timeStr).toEpochSecond() 
	}

	val timeStrToEpochUDF = udf(timeStrToEpoch(_: String))

	def timeISO8601ToEpochFunc:(String=>Long) = (timeStr:String) => {
		if(timeStr==null) 0L else Instant.parse(timeStr).getEpochSecond()
	}
	val timeISO8601ToEpochUDF = udf(timeISO8601ToEpochFunc(_: String))


	def main(args: Array[String]) {
    Holder.log.debug("Args is $args")
		val spark = SparkSession.builder().getOrCreate()
		val sc = spark.sparkContext
		import spark.implicits._

		//Fetch from thuuz and update 
		val thuuzGamesDF = spark.read.json("/tmp/thuuz.json")
		val thuuzGamesDF1 = thuuzGamesDF.withColumn("gamesExp", explode(thuuzGamesDF.col("ratings"))).drop("ratings")
		val thuuzGamesDF11 = thuuzGamesDF1.select($"gamesExp.gex_predict" as "gexPredict",$"gamesExp.external_ids.stats.game" as "statsGameCode");
		val thuuzGamesDF20 = thuuzGamesDF11.withColumn("gameCodeTmp", thuuzGamesDF11("statsGameCode").cast(LongType)).drop("statsGameCode").withColumnRenamed("gameCodeTmp", "statsGameCode")
		thuuzGamesDF20.createOrReplaceTempView("thuuzGames")

		//fetch summary json
		val summaryJson = spark.read.json("/tmp/summary.json")
		val subPackIds = summaryJson.select($"subscriptionpacks")
		val subPackIds2 = subPackIds.withColumn("subpacksExploded", explode($"subscriptionpacks")).drop("spIdsExploded") ;
		val subPackIds21 = subPackIds2.select(children("subpacksExploded", subPackIds2) : _* ).withColumnRenamed("title","subpack_title").withColumnRenamed("subpack_id","subpackage_guid").where("subpack_title != 'Ops Test' OR subpack_title != 'Ops Test - D'") ;

		val channelsSummaryJsonDF = summaryJson.select($"channels");
		val channelsSummaryJsonDF1 = channelsSummaryJsonDF.withColumn("channels", explode(channelsSummaryJsonDF.col("channels")));
		val channelsSummaryJsonDF2 = channelsSummaryJsonDF1.select(children("channels", channelsSummaryJsonDF1) : _* )
		val channelsSummaryJsonDF3 = channelsSummaryJsonDF2.select($"channel_guid",$"channel_number" as "channel_no",$"subscriptionpack_ids",$"metadata.genre" as "genre",$"metadata.call_sign" as "callsign")
		val channelsSummaryJsonDF4 = channelsSummaryJsonDF3.withColumn("subPackExploded", explode($"subscriptionpack_ids")).drop("subscriptionpack_ids").withColumnRenamed("subPackExploded","subpack_int_id")
		//Total Channels == 9210
		val channelsSummaryJsonDF5 = channelsSummaryJsonDF4.withColumn("genreExploded", explode($"genre")).drop("genre")
		//Total Sports channels== 1357 (14%)
		// Filter only sports channels
		val channelsSummaryJsonDF6 = channelsSummaryJsonDF5.filter("genreExploded='Sports'").drop("genreExploded")

		val summaryJson6 = channelsSummaryJsonDF6.join(subPackIds21, channelsSummaryJsonDF6("subpack_int_id") === subPackIds21("id"), "inner").drop("id")

		//fetch schedules
		val linearFeedDF= spark.read.json("/tmp/schedules_plus_3")
		val programsDF1 = linearFeedDF.select($"_self",$"programs")
		val programsDF2 = programsDF1.withColumn("programsExploded", explode(programsDF1.col("programs"))).drop("programs");
		programsDF2.groupBy("programsExploded.name").count.count
		def listToStrFunc(genres: WrappedArray[String]): String = {	
			if(genres==null) "" else genres.mkString("::")	
		}
		val listToStrUDF = udf(listToStrFunc(_: WrappedArray[String]))

		val programsDF21 = programsDF2.withColumn("genres",listToStrUDF($"programsExploded.genres"))
		val programsDF3 = programsDF21.select($"_self",$"programsExploded.id" as "program_id",$"programsExploded.guid" as "program_guid",$"programsExploded.name" as "program_title",$"genres")
		val programsDF31 = programsDF3.distinct.toDF

		val schedulesDF = linearFeedDF.select($"schedule")
		val schedulesDF1 = schedulesDF.withColumn("schedulesExp", explode(schedulesDF.col("schedule"))).drop("schedule");
		val schedulesDF2 = schedulesDF1.select($"schedulesExp.program_id" as "s_program_id",$"schedulesExp.asset_guid",$"schedulesExp.start" as "start_time")
		val schedulesDF3 = schedulesDF2.withColumn("start_time_epoch",timeISO8601ToEpochUDF($"start_time"))

		val programScheduleJoinDF = programsDF31.join(schedulesDF3, programsDF31("program_id") === schedulesDF3("s_program_id"), "inner").drop("s_program_id")
		val programScheduleJoinDF1 = programScheduleJoinDF.distinct.toDF


		def getChannelGuidFromSelfFunc(url: String): String = {
			if(url!=null) {
				var chUrl= url.slice(0, 0+url.lastIndexOf("/"))
				chUrl = chUrl.substring(chUrl.lastIndexOf("/")+1)
				chUrl
			} else {
				""
			}			
		}
		val channelGuidUDF = udf(getChannelGuidFromSelfFunc(_: String))

		val programScheduleJoinDF3 = programScheduleJoinDF1.withColumn("channel_guid_extr", channelGuidUDF($"_self")).drop("_self")
		val programScheduleJoinDF4 =  programScheduleJoinDF3.coalesce(3)

		val programScheduleChannelJoin = summaryJson6.join(programScheduleJoinDF4, summaryJson6("channel_guid") === programScheduleJoinDF4("channel_guid_extr"), "inner").drop("channel_guid_extr")
		val programScheduleChannelJoin0 = programScheduleChannelJoin.coalesce(3)

		programScheduleChannelJoin0.createOrReplaceTempView("programSchedules")

		// end of join 

		val ds1 = spark .read .format("kafka") .option("kafka.bootstrap.servers", "localhost:9092") .option("subscribe", "mlb_meta") .load()
		val ds2 = ds1.selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)") .as[(String, String)]

		val scoreSchema = StructType( StructField("score", StringType, true) :: Nil)

		val nameSchema = StructType( StructField("name", StringType, true) :: Nil)
		val playerDataSchema = StructType( StructField("player-data", nameSchema, true) :: Nil)


		val teamSchema =  StructType( StructField("team-name", StringType, true) ::  StructField("team-city", StringType, true) ::  StructField("team-code", StringType, true) :: Nil)
		val dateSchema = StructType( StructField("month", StringType, true) ::  StructField("date", StringType, true) :: StructField("day", StringType, true) :: StructField("year", StringType, true) :: Nil)
		val timeSchema = StructType( StructField("hour", StringType, true) ::  StructField("minute", StringType, true) :: StructField("utc-hour", StringType, true) :: StructField("utc-minute", StringType, true) :: Nil)

		val gameScheduleItemSchema = StructType( StructField("visiting-team-score", scoreSchema, true) :: StructField("home-team-score", scoreSchema, true) ::  StructField("away-starting-pitcher", playerDataSchema, true) :: StructField("home-starting-pitcher", playerDataSchema, true) :: StructField("home-team", teamSchema, true) :: StructField("visiting-team", teamSchema, true) :: StructField("date", dateSchema, true) :: StructField("time", timeSchema, true) :: StructField("gamecode", StringType, true) :: Nil)
		val gameShceduleSchema = StructType( StructField("game-schedule",gameScheduleItemSchema,true) :: Nil )
		//only pickup MLB games for now
		val ds3 = ds2.where("key like '%MLB_SCHEDULE.XML%'")
		val ds4 = ds3.select(from_json($"key",StructType( StructField("payload", StringType, true) :: Nil)) as "fileName", from_json($"value",StructType( StructField("payload", StringType, true) :: Nil)) as "payloadStruct")
		val ds5 = ds4.select($"fileName", from_json($"payloadStruct.payload",gameShceduleSchema) as "gameScheduleStruct")



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
							$"game-schedule.gamecode" as "gameCode",
							concat(col("game-schedule.date.year"),lit("-"),lit(getZeroPaddedUDF($"game-schedule.date.month")),lit("-"),lit(getZeroPaddedUDF($"game-schedule.date.date")),lit("T"),col("game-schedule.time.hour"),lit(":"),col("game-schedule.time.minute"),lit(":00.00"),lit(getZeroPaddedUDF($"game-schedule.time.utc-hour")),lit(":"),col("game-schedule.time.utc-minute")) as "date")

		val mlbScheduleDF31 = mlbScheduleDF3.withColumn("game_date_epoch",timeStrToEpochUDF($"date"))
		val mlbScheduleDF32 = mlbScheduleDF31.distinct.toDF

		//join with thuuz to get gex score
		val thuuzGamesDF2 = spark.sql("select * from thuuzGames").toDF
		val mlbScheduleDF33 = mlbScheduleDF32.join(thuuzGamesDF2, mlbScheduleDF32("gameCode") === thuuzGamesDF2("statsGameCode"), "left").drop("statsGameCode")
		val mlbScheduleDF34 = mlbScheduleDF33.na.fill(0L, Seq("gexPredict"))
		val mlbScheduleDF35 = mlbScheduleDF34.na.fill(0L, Seq("homeTeamScore"))
		val mlbScheduleDF4 = mlbScheduleDF35.na.fill(0L, Seq("awayTeamScore"))

		//mlbScheduleDF4.where("game_date_epoch < CAST(current_timestamp() AS long)+86400 AND  game_date_epoch > CAST(current_timestamp() AS long)").count
		//15
		//mlbScheduleDF31.where("homeTeamName='Cubs' AND awayTeamName='Nationals'").show(false)
		//mlbScheduleDF31.where("homeTeamName='Nationals' AND awayTeamName='Cubs'").show(false)

		def addDeduplicateLogic( programTitle:String,genre:String): String = {
			if(programTitle==null) programTitle else if(programTitle.startsWith(genre)) programTitle  else genre.toUpperCase.concat("  ").concat(programTitle)
		}
		val partialWithMLB = udf(addDeduplicateLogic(_: String,"MLB:"))

		val regexpCreator = (awayTeamCity:String,awayTeamName:String, homeTeamCity:String,homeTeamName:String,matchType:Int) => {
			val interMediaryExpr = "(vs\\.|at)"
			val startExpr = ".*?"
			var regexp:String = null
			matchType match {
		      case 0 => 
		        regexp = ("^"+startExpr+"\\s*"+awayTeamCity+ ".*" + awayTeamName + "\\s*" + interMediaryExpr + "\\s*"+ homeTeamCity + "\\s*" + homeTeamName+"$")
		      case 1 => 
		        regexp = ("^"+startExpr+"\\s*"+awayTeamName +"\\s*"+interMediaryExpr + "\\s*" + homeTeamName +"$")
		      case 2 =>
		        regexp = ("^"+startExpr + "\\s*" + awayTeamName + "\\s*" + interMediaryExpr + "\\s*" + homeTeamCity + "\\s*" + homeTeamName +"$")
		      case 3 =>
		        regexp = ("^"+startExpr + "\\s*" + awayTeamCity + "\\s*" + awayTeamName + "\\s*" + interMediaryExpr + "\\s*" + homeTeamName +"$")
		      case _ =>
		      	regexp = "!"
		    } 
		    regexp   
		}

		val fullMatch = udf(regexpCreator(_: String,_: String,_: String,_: String, 0))
		val partialWithAnHn = udf(regexpCreator(_: String,_: String,_: String,_: String, 1))
		val partialWithAtHcHn = udf(regexpCreator(_: String,_: String,_: String,_: String, 2))
		val partialWithAcAnHn = udf(regexpCreator(_: String,_: String,_: String,_: String, 2))

		val mlbScheduleDF41 = mlbScheduleDF4.withColumn("regexp1", fullMatch($"awayTeamCity",$"awayTeamName",$"homeTeamCity",$"homeTeamName"))
		val mlbScheduleDF42 = mlbScheduleDF41.withColumn("regexp2", partialWithAnHn($"awayTeamCity",$"awayTeamName",$"homeTeamCity",$"homeTeamName"))
		val mlbScheduleDF43 = mlbScheduleDF42.withColumn("regexp3", partialWithAtHcHn($"awayTeamCity",$"awayTeamName",$"homeTeamCity",$"homeTeamName"))
		val mlbScheduleDF44 = mlbScheduleDF43.withColumn("regexp4", partialWithAcAnHn($"awayTeamCity",$"awayTeamName",$"homeTeamCity",$"homeTeamName"))
		val mlbScheduleDF5 = mlbScheduleDF44.coalesce(3).cache

		//val gameScheduleDF=mlbScheduleDF5
		//val targetProgramsToMatch = programScheduleChannelJoin
		//val domain = "slingtv"

		def getMatchedCount(gameScheduleDF:DataFrame , targetProgramsToMatch:DataFrame , domain:String="slingtv"): DataFrame = {

			val statsTargetProgramsJoin = gameScheduleDF.crossJoin(targetProgramsToMatch)
			val statsTargetProgramsJoin1 = statsTargetProgramsJoin.where("( start_time_epoch < game_date_epoch + 3600 AND start_time_epoch > game_date_epoch - 3600 ) AND ((program_title rlike regexp1) OR (program_title rlike regexp2) OR (program_title rlike regexp3) OR (program_title rlike regexp4))")
			statsTargetProgramsJoin1
		}

		def getStats(statsTargetProgramsJoin1:DataFrame,statsTargetProgramsJoin:DataFrame , domain:String="slingtv" ):(Long, Long,Float) =  {
			val statsTargetProgramsJoin2 = statsTargetProgramsJoin1.select($"program_title").withColumn("program_title",partialWithMLB($"program_title")).distinct
			val statsTargetProgramsJoin3  = statsTargetProgramsJoin.where("(program_title rlike regexp1) OR (program_title rlike regexp2) OR (program_title rlike regexp3) OR (program_title rlike regexp4)").select($"program_title").withColumn("program_title",partialWithMLB($"program_title")).distinct
			val sportsProgramsDir = s"/tmp/sports-cloud/all-mlb-$domain-sports-programs.csv"
			s"rm -rf $sportsProgramsDir" !! ; 
			statsTargetProgramsJoin3.coalesce(1).write.format("com.databricks.spark.csv").option("header", "true").save(s"$sportsProgramsDir")
			val matchedGamesDir = s"/tmp/sports-cloud/all-mlb-$domain-matched-games.csv"
			s"rm -rf $matchedGamesDir" !! ;
			statsTargetProgramsJoin2.coalesce(1).write.format("com.databricks.spark.csv").option("header", "true").save(s"$matchedGamesDir")
			val totalMatchedCount = statsTargetProgramsJoin2.count
			val totalGamesCount = statsTargetProgramsJoin3.count
			(totalMatchedCount,totalGamesCount, totalMatchedCount.toFloat/totalGamesCount.toFloat*100)
		}

		val makeImgUrlFunc: (String => String) = (teamExternalId: String) => {if (teamExternalId == null ) "http://gwserv-mobileprod.echodata.tv/Gamefinder/logos/MEDIUM/gid1.png" else "http://gwserv-mobileprod.echodata.tv/Gamefinder/logos/MEDIUM/gid".concat(teamExternalId).concat(".png")}
		val makeImgUrl = udf(makeImgUrlFunc)

		val programScheduleChannelJoin2 = spark.sql("select * from programSchedules").toDF
		val programsJoin = getMatchedCount(mlbScheduleDF5,programScheduleChannelJoin2)
		Holder.log.trace("Matched with stats data");
		val programsJoin1 = programsJoin.drop("regexp1","regexp2","regexp3","regexp4","subpack_int_id","start_time","start_time_epoch","date")
		val programsJoin2 = programsJoin1.withColumn("homeTeamImg",makeImgUrl($"homeTeamExternalId")).withColumn("awayTeamImg",makeImgUrl($"awayTeamExternalId"))
		val columns = programsJoin2.columns
		scala.util.Sorting.quickSort(columns)
		val programsJoin3 = programsJoin2.select(columns.head, columns.tail:_*)
		val mongoChGPgmIdDF2 = programsJoin3.rdd.map { row =>
				val asset_guid = row.getString(0)
				val awayTeamCity = row.getString(1)
				val awayTeamExternalId = row.getString(2)
				val awayTeamImg = row.getString(3)
				val awayTeamName = row.getString(4)
				val awayTeamPitcherName = row.getString(5)
				val awayTeamScore = row.getString(6)
				val callsign = row.getString(7)
				val channel_guid = row.getString(8)
				val channel_no = row.getLong(9)
				val gameCode = row.getString(10)
				val game_date_epoch = row.getLong(11)
				val genres = row.getString(12)
				val gexPredict = row.getLong(13)
				val homeTeamCity = row.getString(14)
				val homeTeamExternalId = row.getString(15)
				val homeTeamImg = row.getString(16)
				val homeTeamName = row.getString(17)
				val homeTeamPitcherName = row.getString(18)
				val homeTeamScore = row.getString(19)
				val program_guid = row.getString(20)
				val program_id = row.getLong(21)
				val program_title = row.getString(22)
				val subpack_title = row.getString(23)
				val subpackage_guid = row.getString(24)
		        ((channel_guid.concat("_").concat(program_id.toString)),GameEvent(channel_guid.concat("_").concat(program_id.toString),asset_guid,awayTeamCity,awayTeamExternalId,awayTeamImg,awayTeamName,awayTeamPitcherName,awayTeamScore,callsign,channel_guid,channel_no,gameCode,game_date_epoch,genres,gexPredict,homeTeamCity,homeTeamExternalId,homeTeamImg,homeTeamName,homeTeamPitcherName,homeTeamScore,program_guid,program_id,program_title,subpack_title,subpackage_guid))
		}.toDF

		val mongoChGPgmIdDF3 = mongoChGPgmIdDF2.withColumnRenamed("_1","id").withColumnRenamed("_2","gameEvent")
		val mongoChGPgmIdDF40 = mongoChGPgmIdDF3.groupBy("id").agg(collect_list("gameEvent") as "gameEvents").cache
		//This schema conversion if to make sure that long fields are not nullable so that its comparable to mongo db
		import org.apache.spark.sql.catalyst.ScalaReflection.schemaFor
	  val gameEventSchema =  schemaFor[GameEvent].dataType.asInstanceOf[StructType]
		val gameEventsSchema = StructType( StructField("id", StringType, true) :: StructField("gameEvents", ArrayType(gameEventSchema), true) :: Nil )
		val mongoChGPgmIdDF4 = spark.createDataFrame(mongoChGPgmIdDF40.rdd, gameEventsSchema)

		val readConfigchPgmDF = ReadConfig(Map("partitionKey"->"_id","numberOfPartitions"->"5","collection" -> "slingtv_sports_events", "readPreference.name" -> "primaryPreferred","partitioner"->"MongoPaginateByCountPartitioner"), Some(ReadConfig(spark)))
		val chPgmDF = MongoSpark.load[GameEvents](spark, readConfigchPgmDF)
		val writeConfig = WriteConfig(Map("collection" -> "slingtv_sports_events", "writeConcern.w" -> "majority"), Some(WriteConfig(sc)))
		import scala.util.{ Try , Success, Failure}



		val count = Try { chPgmDF.count }
		count match {
			case Success(v) => 
			  Holder.log.trace("getting delta data from mongo");
				val originDF = chPgmDF.select($"id",$"gameEvents")
				val finalchPgmDFtoWrite = originDF.except(mongoChGPgmIdDF4)
				//write to mongo
				if(finalchPgmDFtoWrite.count>0) {
				  Holder.log.trace("writing delta data from mongo");
					finalchPgmDFtoWrite.write.option("collection", "slingtv_sports_events").mode("append").mongo()
				}
			case Failure(e) =>
				mongoChGPgmIdDF4.write.option("collection", "slingtv_sports_events").mode("overwrite").mongo()
		}

	}

}