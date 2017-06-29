import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.sql.{SQLContext , SparkSession , DataFrame }
import org.apache.spark.{ SparkConf, SparkContext }
import org.apache.spark.sql.{ Row , Column }
import org.apache.spark.sql.functions.{ concat, lit, coalesce, max }
import org.apache.spark.sql.types.{ StructType, StructField, StringType, IntegerType }
import org.apache.spark.sql.DataFrame
import com.mongodb.spark.config._
import sys.process._
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import com.mongodb.spark.MongoSpark;
import org.bson.Document;
import org.apache.spark.sql.functions.{ concat, lit, coalesce, max , min }
import org.apache.spark.sql.types.{ StructType, StructField, StringType, IntegerType,ArrayType };
import com.mongodb.spark._  
import com.mongodb.spark.config.ReadConfig  
import com.mongodb.spark.sql._  
import com.typesafe.scalalogging.slf4j.LazyLogging  
import org.bson.Document
import java.time.{ ZonedDateTime , OffsetDateTime , ZoneOffset , Instant, ZoneId}
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit;
import com.databricks.spark.csv
import java.io.File
import scala.collection.mutable.WrappedArray


val hiveContext = new HiveContext(sc)
val sqlContext = new SQLContext(sc)

//parse linear feed and get mapping from subpackage_id -> channel_guid -> [echostar_content_ids]
def children(colname: String, df: DataFrame) = {
  val parent = df.schema.fields.filter(_.name == colname).head
  val fields = parent.dataType match {
    case x: StructType => x.fields
    case _ => Array.empty[StructField]
  }
  fields.map(x => col(s"$colname.${x.name}"))
}
"cat /dev/null" #> new File("/tmp/summary.json") !
"curl -o /tmp/summary.json http://cbd46b77.cdn.cms.movetv.com/cms/publish3/domain/summary/1.json" !
val summaryJson = spark.read.json("/tmp/summary.json")
val subPackIds = summaryJson.select($"subscriptionpacks")
val subPackIds2 = subPackIds.withColumn("subpacksExploded", explode($"subscriptionpacks")).drop("spIdsExploded")
val subPackIds21 = subPackIds2.select(children("subpacksExploded", subPackIds2) : _* ).withColumnRenamed("title","subpack_title").withColumnRenamed("subpack_id","subpackage_guid")

val channelsSummaryJsonDF = summaryJson.select($"channels");
val channelsSummaryJsonDF1 = channelsSummaryJsonDF.withColumn("channels", explode(channelsSummaryJsonDF.col("channels")));
val channelsSummaryJsonDF2 = channelsSummaryJsonDF1.select(children("channels", channelsSummaryJsonDF1) : _* )
val channelsSummaryJsonDF3 = channelsSummaryJsonDF2.select($"channel_guid",$"channel_number" as "channel_no",$"subscriptionpack_ids",$"metadata.genre" as "genre",$"metadata.call_sign" as "callsign")
val channelsSummaryJsonDF4 = channelsSummaryJsonDF3.withColumn("subPackExploded", explode($"subscriptionpack_ids")).drop("subscriptionpack_ids").withColumnRenamed("subPackExploded","subpack_int_id")
//Total Channels == 9210
val channelsSummaryJsonDF5 = channelsSummaryJsonDF4.withColumn("genreExploded", explode($"genre")).drop("genre")
//Total Sports channels== 1357 (14%)
val channelsSummaryJsonDF6 = channelsSummaryJsonDF5.filter("genreExploded='Sports'").drop("genreExploded")

val summaryJson6 = channelsSummaryJsonDF6.join(subPackIds21, channelsSummaryJsonDF6("subpack_int_id") === subPackIds21("id"), "inner").drop("id")




"mv /tmp/schedules_plus_3 /tmp/schedules_plus_3.`date +%F%T`" !
"cat /dev/null" #> new File("/tmp/schedules_plus_3") !
summaryJson6.select($"channel_guid").collect.distinct.foreach{   it => 
	val channel_guid = it.getString(0)
	if(channel_guid!=null) { 
		for( i <- 0 to 3){
			val epochTimeOffset = Instant.now().plus(i, ChronoUnit.DAYS);      				  
			val utc = epochTimeOffset.atZone(ZoneId.of("Z"));        				  
			def pattern = "yyyyMMdd";
			val formattedDate = utc.format(DateTimeFormatter.ofPattern(pattern)); 
			val fullUrl = s"http://cbd46b77.cdn.cms.movetv.com/cms/api/linear_feed/channels/v1/$channel_guid/" + formattedDate
			Thread sleep 3000
			(s"curl $fullUrl" #>> new File("/tmp/schedules_plus_3")).!
			(s"echo "  #>> new File("/tmp/schedules_plus_3")).!
		}
       }
}





val teamNameAttribSchema = StructType( StructField("_name", StringType, true) :: Nil)
val teamCityAttribSchema = StructType( StructField("_city", StringType, true) :: Nil)
val teamSchema =  StructType( StructField("team-name", teamNameAttribSchema, true) ::  StructField("team-city", teamCityAttribSchema, true) :: Nil)
val dateSchema = StructType( StructField("_month", StringType, true) ::  StructField("_date", StringType, true) :: StructField("_day", StringType, true) :: StructField("_year", StringType, true) :: Nil)
val timeSchema = StructType( StructField("_hour", StringType, true) ::  StructField("_minute", StringType, true) :: StructField("_utc-hour", StringType, true) :: StructField("_utc-minute", StringType, true) :: Nil)

val gameScheduleItemSchema = StructType( StructField("home-team", teamSchema, true) :: StructField("visiting-team", teamSchema, true) :: StructField("date", dateSchema, true) :: StructField("time", timeSchema, true) :: Nil)
val gameScheduleSchema =  StructType( StructField("game-schedule", ArrayType(gameScheduleItemSchema), true) :: Nil)
val baseballMlbScheduleSchema = StructType(StructField("baseball-mlb-schedule", gameScheduleSchema, true) :: Nil)
val sportsScheduleSchema = StructType(StructField("sports-schedule", baseballMlbScheduleSchema, true) :: Nil)

sportsScheduleSchema.printTreeString

def getZeroPaddedFunc(timeStr: String): String = {
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

def timeStrToEpoch(timeStr:String): Long = {
	OffsetDateTime.parse(timeStr).toEpochSecond()
}

val timeStrToEpochUDF = udf(timeStrToEpoch(_: String))


val mlbScheduleDF = spark.read.format("com.databricks.spark.xml").option("rowTag", "sports-statistics").schema(sportsScheduleSchema).load("/tmp/stats-ftp-data/MLB_SCHEDULE.XML")
val mlbScheduleDF1 = mlbScheduleDF.select($"sports-schedule.baseball-mlb-schedule.game-schedule")
val mlbScheduleDF2 = mlbScheduleDF1.withColumn("game-schedule",explode(mlbScheduleDF1.col("game-schedule")))
val mlbScheduleDF3 = mlbScheduleDF2.select($"game-schedule.visiting-team.team-city._city" as "awayTeamCity",
					$"game-schedule.visiting-team.team-name._name" as "awayTeamName",
					$"game-schedule.home-team.team-city._city" as "homeTeamCity",
					$"game-schedule.home-team.team-name._name" as "homeTeamName",
					concat(col("game-schedule.date._year"),lit("-"),lit(getZeroPaddedUDF($"game-schedule.date._month")),lit("-"),lit(getZeroPaddedUDF($"game-schedule.date._date")),lit("T"),col("game-schedule.time._hour"),lit(":"),col("game-schedule.time._minute"),lit(":00.00"),lit(getZeroPaddedUDF($"game-schedule.time._utc-hour")),lit(":"),col("game-schedule.time._utc-minute")) as "date")

val mlbScheduleDF4 = mlbScheduleDF3.withColumn("game_date_epoch",timeStrToEpochUDF($"date"))
val mlbScheduleDF31 = mlbScheduleDF4.distinct.toDF
mlbScheduleDF4.where("game_date_epoch < CAST(current_timestamp() AS long)+86400 AND  game_date_epoch > CAST(current_timestamp() AS long)").count
//15
//mlbScheduleDF31.where("homeTeamName='Cubs' AND awayTeamName='Nationals'").show(false)
//mlbScheduleDF31.where("homeTeamName='Nationals' AND awayTeamName='Cubs'").show(false)



val linearFeedDF= spark.read.json("/tmp/schedules_plus_3")
val programsDF1 = linearFeedDF.select($"_self",$"programs")
val programsDF2 = programsDF1.withColumn("programsExploded", explode(programsDF1.col("programs"))).drop("programs");
programsDF2.groupBy("programsExploded.name").count.count
def listToStrFunc(genres: WrappedArray[String]): String = {	
	if(genres==null) "" else genres.mkString("::")	
}
val listToStrUDF = udf(listToStrFunc(_: WrappedArray[String]))

val programsDF21 = programsDF2.withColumn("genres",listToStrUDF($"programsExploded.genres"))
val programsDF3 = programsDF21.select($"programsExploded.name" as "program_title",$"genres")
val programsDF31 = programsDF3.distinct.toDF
programsDF31.groupBy("program_title").count.count
//programsDF31.groupBy("program_title").count.agg(sum("count")).show === 5989 unique programs


def mutableSum[String](tuples: Array[(String, Int)])  = {
  val m = scala.collection.mutable.Map.empty[String, Int].withDefault(_ => 0)
  for ((k, v) <- tuples) m += (k -> (v + m(k)))
  m.toArray
}



def getMatchedCount(gameScheduleDF:DataFrame , targetProgramsToMatch:DataFrame , startExpr:String = "",domain:String="slingtv"): (Long, Long, Long, Long, Float, Float) = {
	val statsGamesSchedule = gameScheduleDF.collect
	val gamesTitle = targetProgramsToMatch.collect
	var totalCount=0
	val interMediaryExpr = "(\\vs.|at)"
	val matchedUnmatched1 = statsGamesSchedule.map {  it =>
		val titleRegExp = ("^"+startExpr+"\\s*"+it.getString(0)+ ".*" + it.getString(1) + "\\s*" + interMediaryExpr + "\\s*"+ it.getString(2) + "\\s*" + it.getString(3)+"$").r	 
		val x = gamesTitle.map { it1 =>
			val stringToBeMatched = it1.getString(0)
			stringToBeMatched match {
	  			case titleRegExp(_*) => (stringToBeMatched , 1)
	  			case _ => (stringToBeMatched,0)
			}
		}
		x
	}.flatten

	val matchedUnmatched2 = statsGamesSchedule.map {  it =>
		val titleRegExp = ("^"+startExpr+"\\s*"+it.getString(1) +"\\s*"+interMediaryExpr + "\\s*" + it.getString(3) +"$").r
		val x = gamesTitle.map { it1 =>
			val stringToBeMatched = it1.getString(0)
			stringToBeMatched match {
	  			case titleRegExp(_*) =>  (stringToBeMatched , 1)
	  			case _ => (stringToBeMatched,0)
			}			
		}
		x
	}.flatten

	val matchedUnmatched3 = statsGamesSchedule.map {  it =>
		val titleRegExp = ("^"+startExpr + "\\s*" + it.getString(1) + "\\s*" + interMediaryExpr + "\\s*" + it.getString(2) + "\\s*" + it.getString(3) +"$").r
		val x = gamesTitle.map { it1 =>
			val stringToBeMatched = it1.getString(0)
			stringToBeMatched match {
	  			case titleRegExp(_*) =>  (stringToBeMatched , 1)
	  			case _ => (stringToBeMatched,0)
			}			
		}
		x
	}.flatten

	val matchedUnmatched4 = statsGamesSchedule.map {  it =>
		val titleRegExp = ("^"+startExpr + "\\s*" + it.getString(0) + "\\s*" + it.getString(1) + "\\s*" + interMediaryExpr + "\\s*" + it.getString(3) +"$").r
		val x = gamesTitle.map { it1 =>
			val stringToBeMatched = it1.getString(0)
			stringToBeMatched match {
	  			case titleRegExp(_*) =>  (stringToBeMatched , 1)
	  			case _ => (stringToBeMatched,0)
			}			
		}
		x
	}.flatten



	val matchedUnmatched0 = matchedUnmatched1 ++ matchedUnmatched2 ++ matchedUnmatched3 ++ matchedUnmatched4
	val matchedUnmatched = mutableSum(matchedUnmatched0)
	val matchedUnmatchedDF = sc.parallelize(matchedUnmatched).toDF
	val groupedDF = matchedUnmatchedDF.groupBy("_1").agg(sum("_2")).withColumnRenamed("sum(_2)","count").withColumnRenamed("_1","program_title")
	val totalProgramsCount = groupedDF.count 
	val totalDetectedGames = targetProgramsToMatch.where(s"program_title rlike '$interMediaryExpr'").groupBy("program_title").count
	val possibleGamesDir =s"/tmp/sports-cloud/all-mlb-$domain-possible-games.csv"
	s"rm -rf $possibleGamesDir" !! ; 
	totalDetectedGames.coalesce(1).write.format("com.databricks.spark.csv").option("header", "true").save(s"$possibleGamesDir")
	val totalDetectedGamesCount = totalDetectedGames.count
	val sportsProgramsDir = s"/tmp/sports-cloud/all-mlb-$domain-sports-programs.csv"
	s"rm -rf $sportsProgramsDir" !! ; 
	groupedDF.where("1=1").groupBy("program_title").count.coalesce(1).write.format("com.databricks.spark.csv").option("header", "true").save(s"$sportsProgramsDir")
	val allMatched = groupedDF.where("count > 0")
	val matchedGamesDir = s"/tmp/sports-cloud/all-mlb-$domain-matched-games.csv"
	s"rm -rf $matchedGamesDir" !! ;
	allMatched.coalesce(1).write.format("com.databricks.spark.csv").option("header", "true").save(s"$matchedGamesDir")
	val totalMatchedCount = allMatched.count
	val unmatched = groupedDF.where("count==0")
	val todaysGameCount = gameScheduleDF.where("game_date_epoch < CAST(current_timestamp() AS long)+86400 AND  game_date_epoch > CAST(current_timestamp() AS long)").count
	(totalProgramsCount,todaysGameCount,totalDetectedGamesCount,totalMatchedCount, totalMatchedCount.toFloat/totalProgramsCount.toFloat*100 ,totalMatchedCount.toFloat/totalDetectedGamesCount.toFloat*100)
}
val totalJoinBetweenSlingTVAndStats = getMatchedCount(mlbScheduleDF31,programsDF31,".*")
totalJoinBetweenSlingTVAndStats

"cat /dev/null" #> new File("/tmp/dish_live.json") !
var a = 0;
var start = 0;
var end =1000;
for( a <- 1 until 10){		
	s"curl http://egi.slingbox.com/solr/dish_live_v2/select/?q=content_type:(2+4)+AND+program_title:MLB*&wt=json&start=$start&rows=$end"  #>> new File("/tmp/dish_live.json") !! ;
    start = end
    end = start + 1000
}

val solrArray = spark.read.json("/tmp/dish_live.json")
val solrArray1 = solrArray.select($"response.docs")
solrArray1.registerTempTable("solrArray1")
val solrArray2 = solrArray1.withColumn("docs", explode(solrArray1.col("docs")))
solrArray2.registerTempTable("solrArray2")
val solrArray3  = solrArray2.select($"docs.program_title").distinct.toDF

val totalJoinBetweenDANYAndStats = getMatchedCount(mlbScheduleDF31,solrArray3,"MLB\\:","dany")
totalJoinBetweenDANYAndStats



