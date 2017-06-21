import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.sql.SQLContext
import org.apache.spark.{ SparkConf, SparkContext }
import sys.process._
import java.time.ZonedDateTime
import org.apache.spark.sql.Row
import java.time.format.DateTimeFormatter
import org.apache.spark.sql.functions.{ concat, lit, coalesce, max }
import org.apache.spark.sql.types.{ StructType, StructField, StringType, IntegerType };
import org.apache.spark.sql.DataFrame
import com.mongodb.spark.config._
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import com.mongodb.spark.MongoSpark;
import org.bson.Document;
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.Row
import org.apache.spark.sql.functions.{ concat, lit, coalesce, max }
import org.apache.spark.sql.types.{ StructType, StructField, StringType, IntegerType };
import org.apache.spark.sql.DataFrame
import com.mongodb.spark._  
import com.mongodb.spark.config.ReadConfig  
import com.mongodb.spark.sql._  
import com.typesafe.scalalogging.slf4j.LazyLogging  
import org.apache.spark.sql.SparkSession  
import org.apache.spark.sql.functions.{max, min}  
import org.bson.Document
import java.io.File
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.Instant;
import java.time.ZoneId;
import com.databricks.spark.csv

import java.io.File

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

"curl -o /tmp/summary.json http://93a256a7.cms.movetv.com/cms/publish3/domain/summary/4.json" !
val summaryJson = sqlContext.read.json("/tmp/summary.json")
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
			val fullUrl = s"http://93a256a7.cdn.cms.movetv.com/cms/api/linear_feed/channels/v1/$channel_guid/" + formattedDate
			Thread sleep 3000
			(s"curl $fullUrl" #>> new File("/tmp/schedules_plus_3")).!
			(s"echo "  #>> new File("/tmp/schedules_plus_3")).!
		}
       }
}






val linearFeedDF= spark.read.json("/tmp/schedules_plus_3")


//load franchise df with all required information
val franchisesDF = linearFeedDF.select($"franchises")
val franchisesDF1 = franchisesDF.withColumn("franchisesExp",explode($"franchises")).drop("franchises")
val franchisesDF2 = franchisesDF1.select($"franchisesExp.id",$"franchisesExp.lookups")
val franchisesDF3 = franchisesDF2.withColumn("lookupsExploded",explode($"lookups")).drop("lookups")
val franchisesDF4 = franchisesDF3.select(Array(col("id")) ++ children("lookupsExploded", franchisesDF3) : _* ).withColumnRenamed("key", "fr_id_key").withColumnRenamed("value","fr_id_val")
val franchisesDF5 = franchisesDF4.filter("fr_id_key='echostar_id' or fr_id_key='rovi_series_id'").drop("fr_id_key")
//Total franchises : 8418
//get all content_ids from dish_live_v2 for series_id
"echo "  #>> new File("/tmp/series_content_ids") !!
"curl http://egi.slingbox.com/solr/dish_live_v2/select/?q=content_type:2&start=0&rows=14000&fl=content_id,series_id&wt=json" #>> new File("/tmp/series_content_ids") !!
"echo "  #>> new File("/tmp/series_content_ids") !!


val seriesCidMappingDF= spark.read.json("/tmp/series_content_ids")
val seriesCidMappingDF1= seriesCidMappingDF.select($"response.docs")
val seriesCidMappingDF2= seriesCidMappingDF1.withColumn("docsExp",explode($"docs")).drop("docs")
val seriesCidMappingDF3 = seriesCidMappingDF2.select(children("docsExp", seriesCidMappingDF2) : _* )
val seriesCidMappingDF31 = seriesCidMappingDF3.distinct.toDF
//Total content_ids of type sports in echo id space(derived from DANY) - 40515



val programsDF1 = linearFeedDF.select($"_self",$"programs")
val programsDF2 = programsDF1.withColumn("programsExploded", explode(programsDF1.col("programs"))).drop("programs");
//programsDF2.groupBy("programsExploded.name").count.agg(sum("count")).show === 5989 unique programs
import scala.collection.mutable.WrappedArray
def listToStrFunc(genres: WrappedArray[String]): String = {	
	if(genres==null) "" else genres.mkString("::")	
}
val listToStrUDF = udf(listToStrFunc(_: WrappedArray[String]))

val programsDF21 = programsDF2.withColumn("genres",listToStrUDF($"programsExploded.genres"))
val programsDF3 = programsDF21.select($"_self",$"programsExploded.id",$"programsExploded.guid", $"programsExploded.lookups",$"programsExploded.franchise_id",$"programsExploded.name" as "program_title",$"genres")
val programsDF31 = programsDF3.distinct.toDF
//programsDF31.groupBy("program_title").count.agg(sum("count")).show === 5989 unique programs




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

val programsDF4 = programsDF31.withColumn("channel_guid_extr", channelGuidUDF($"_self")).drop("_self").withColumnRenamed("guid","program_guid").withColumnRenamed("id","program_id")
val programsDF5 = programsDF4.withColumn("lookupsExploded", explode(programsDF4.col("lookups"))).drop("lookups");
val programsDF6 = programsDF5.select(Array(col("program_id"), col("program_guid"),col("channel_guid_extr"),col("franchise_id"),col("program_title"), col("genres")) ++ children("lookupsExploded", programsDF5) : _* )
val programsDF7 = programsDF6.withColumnRenamed("key", "id_key").withColumnRenamed("value","id_val")
val programsDF8 = programsDF7.filter("id_key='echostar_id'")
//programsDF8.groupBy("program_title").count.agg(sum("count")).show
//Filter only contents with echostar_id : 3297
val programsDF81 = programsDF8.distinct.toDF
val programsFranchiseJoinDF = programsDF81.join(franchisesDF5, programsDF81("franchise_id") === franchisesDF5("id"), "inner").drop("id").drop("series_id").drop("id_key").drop("id_val").drop("franchise_id")

val finalContentIdsList0 = programsFranchiseJoinDF.join(seriesCidMappingDF31, programsFranchiseJoinDF("fr_id_val") === seriesCidMappingDF31("content_id"), "inner").drop("id").drop("series_id").drop("id_key").drop("id_val").drop("fr_id_val").drop("franchise_id").drop("series_id")

val finalContentIdsList1 = programsDF81.join(seriesCidMappingDF31, programsDF81("id_val") === seriesCidMappingDF31("content_id"), "inner").drop("id").drop("id_val").drop("id_key").drop("id_val").drop("franchise_id").drop("series_id")

val finalContentIdsList2 = finalContentIdsList0 union finalContentIdsList1
//finalContentIdsList2.groupBy("program_title").count.agg(sum("count")).show
//Total Joined programs between slingtv and echo id space : 12203

val schedulesDF = linearFeedDF.select($"schedule")
val schedulesDF1 = schedulesDF.withColumn("schedulesExp", explode(schedulesDF.col("schedule"))).drop("schedule");
val schedulesDF2 = schedulesDF1.select($"schedulesExp.program_id",$"schedulesExp.asset_guid").withColumnRenamed("program_id","s_program_id")

val finalContentIdsList3 = finalContentIdsList2.join(schedulesDF2, finalContentIdsList2("program_id") === schedulesDF2("s_program_id"), "inner").drop("s_program_id")
val finalContentIdsList = finalContentIdsList3.distinct.toDF

//join with channel_guid after gettind data for todays urls
val programsDF9 = summaryJson6.join(finalContentIdsList, summaryJson6("channel_guid") === finalContentIdsList("channel_guid_extr"), "inner").drop("channel_guid_extr")
val programsDF91 = programsDF9.distinct.toDF
//Total joined programs which has a valid channel_guid : 11924




//parse mongo db data and get the mapping from echostar_content_id -> gameId -> teamId
case class ExternalId(id: String, provider: String)
case class Team(uid:String, externalIds:Array[ExternalId])
case class EventTeam(uid:String, teamId:String)

val readConfigEventTeam = ReadConfig(Map("partitionKey"->"_id","numberOfPartitions"->"20","collection" -> "eventTeam", "readPreference.name" -> "primaryPreferred","partitioner"->"MongoPaginateByCountPartitioner"), Some(ReadConfig(spark)))
val eventTeamDF = MongoSpark.load[EventTeam](spark, readConfigEventTeam)	



val readConfigTeam = ReadConfig(Map("partitionKey"->"_id","numberOfPartitions"->"20","collection" -> "team", "readPreference.name" -> "primaryPreferred","partitioner"->"MongoPaginateByCountPartitioner"), Some(ReadConfig(spark)))
val teamDF = MongoSpark.load[Team](spark, readConfigTeam)




val readConfigEvent = ReadConfig(Map("partitionKey"->"_id","numberOfPartitions"->"20","collection" -> "event", "readPreference.name" -> "primaryPreferred","partitioner"->"MongoPaginateByCountPartitioner"), Some(ReadConfig(spark)))
case class Event(contentIDs:String, externalIds: Array[ExternalId], eventTeams: Array[String],title:String )
val eventDF=  MongoSpark.load[Event](spark,readConfigEvent)
val eventDF0 = eventDF.withColumnRenamed("title","event_title")


def children(colname: String, df: DataFrame) = {
  val parent = df.schema.fields.filter(_.name == colname).head
  val fields = parent.dataType match {
    case x: StructType => x.fields
    case _ => Array.empty[StructField]
  }
  fields.map(x => col(s"$colname.${x.name}"))
}


def contentIdArrayFun(contentIds: String): Array[String] = {
	var contentIdsMutated = contentIds	
	var finalArray = Array[String]() 
	if(contentIdsMutated != null ) {
	    contentIdsMutated = contentIdsMutated.replace("]","").replace("[","").replace("\"","")	
	    finalArray=contentIdsMutated.split(",").map(_.toString)
	}
	finalArray
}
val contentIdArrayUDF = udf(contentIdArrayFun(_: String))



val eventDF10 = eventDF0.withColumn("contentIdsExtr", contentIdArrayUDF($"contentIDs")).drop("contentIDs");
val eventDF1 = eventDF10.withColumn("contentId", explode(eventDF10.col("contentIdsExtr"))).drop("contentIdsExtr")

val eventDF2 = eventDF1.withColumn("externalIdsExploded", explode(eventDF.col("externalIds"))).drop("externalIds");
val eventDF22 = eventDF2.select( Array(col("contentId"), col("eventTeams") , col("event_title")) ++ children("externalIdsExploded", eventDF2) : _* ).withColumnRenamed("id","gameId")
val eventDF3 = eventDF22.withColumn("eventTeamId", explode(eventDF.col("eventTeams"))).drop("eventTeams");
val eventDF4 = eventDF3.filter("provider='nagra'").filter("contentId!='null'").filter("contentId!=''")
//join events with eventTeam to get home and away team
val eventDF5 = eventDF4.join(eventTeamDF, eventDF4("eventTeamId") === eventTeamDF("uid"), "inner").drop("uid").drop("eventTeamId").drop("provider")
//join with team to get teamid from nagra
val eventDF6 = eventDF5.join(teamDF, eventDF5("teamId") === teamDF("uid"), "inner").drop("uid").drop("teamId")
val eventDF7 =  eventDF6.withColumn("externalIdsExploded", explode(eventDF6.col("externalIds"))).drop("externalIds");
val eventDF8 = eventDF7.select( Array(col("contentId"), col("gameId"), col("event_title")) ++ children("externalIdsExploded", eventDF7) : _* ).withColumnRenamed("id","teamId")
val eventDF9 = eventDF8.filter("provider='nagra'").drop("provider")
	
//Total programs in nagra as of today: 8622

//join between slingtv and our mongo data
val joinedContentDF = eventDF9.join(programsDF91, eventDF9("contentId") === programsDF91("content_id"), "inner").drop("content_id").drop("series_id")
joinedContentDF.groupBy("program_title").count.show(300,false)
//Total joined content : 124

//convert to a form with json key as  channel_guid_content_id
case class GameEvent(contentId:String, gameId:String, event_title:String, teamId:String, channel_guid:String, channel_no:Long,callsign:String, subpackIntId:Long, subpackageGuid:String, subpacTitle:String, programId:Long, programGuid:String, programTitle:String,genres:String,assetGuid:String)
val mongoChGPgmIdCallsignDF2 = joinedContentDF.map { row =>
		 val contentId= row.getString(0)
		 val gameId= row.getString(1)
		 val event_title= row.getString(2)
		 val teamId= row.getString(3)
		 val channel_guid= row.getString(4)
		 val channel_no= row.getLong(5)
		 val callsign = row.getString(6)
		 val subpack_int_id= row.getLong(7)
		 val subpackage_guid= row.getString(8)
		 val subpack_title= row.getString(9)
		 val program_id= row.getLong(10)
		 val program_guid= row.getString(11)
		 val program_title= row.getString(12)
		 val genres= row.getString(13)
		 val asset_guid= row.getString(14)
         ((channel_guid.concat("_").concat(program_id.toString).concat("_").concat(callsign)),GameEvent(contentId, gameId, event_title, teamId, channel_guid, channel_no, callsign,subpack_int_id, subpackage_guid, subpack_title, program_id, program_guid, program_title,genres,asset_guid))
}
val mongoChGPgmIdCallsignDF3 = mongoChGPgmIdCallsignDF2.withColumnRenamed("_1","id").withColumnRenamed("_2","gameEvent")
val mongoChGPgmIdCallsignDF4 = mongoChGPgmIdCallsignDF3.groupBy("id").agg(collect_list("gameEvent")).withColumnRenamed("collect_list(gameEvent)","gameEvents")

//write to mongo
val writeConfig = WriteConfig(Map("collection" -> "slingtv_schguid_program_id_callsign_cont_nagra_mapping", "writeConcern.w" -> "majority"), Some(WriteConfig(sc)))
mongoChGPgmIdCallsignDF4.write.option("collection", "slingtv_schguid_program_id_callsign_cont_nagra_mapping").mode("overwrite").mongo()


val mongoChGPgmIdDF2 = joinedContentDF.map { row =>
		 val contentId= row.getString(0)
		 val gameId= row.getString(1)
		 val event_title= row.getString(2)
		 val teamId= row.getString(3)
		 val channel_guid= row.getString(4)
		 val channel_no= row.getLong(5)
		 val callsign = row.getString(6)
		 val subpack_int_id= row.getLong(7)
		 val subpackage_guid= row.getString(8)
		 val subpack_title= row.getString(9)
		 val program_id= row.getLong(10)
		 val program_guid= row.getString(11)
		 val program_title= row.getString(12)
		 val genres= row.getString(13)
		 val asset_guid= row.getString(14)
         ((channel_guid.concat("_").concat(program_id.toString)),GameEvent(contentId, gameId, event_title, teamId, channel_guid, channel_no,callsign, subpack_int_id, subpackage_guid, subpack_title,  program_id, program_guid, program_title,genres,asset_guid))
}
val mongoChGPgmIdDF3 = mongoChGPgmIdDF2.withColumnRenamed("_1","id").withColumnRenamed("_2","gameEvent")
val mongoChGPgmIdDF4 = mongoChGPgmIdDF3.groupBy("id").agg(collect_list("gameEvent")).withColumnRenamed("collect_list(gameEvent)","gameEvents")




val writeConfig = WriteConfig(Map("collection" -> "slingtv_schguid_program_id_cont_nagra_mapping", "writeConcern.w" -> "majority"), Some(WriteConfig(sc)))
mongoChGPgmIdDF4.write.option("collection", "slingtv_schguid_program_id_cont_nagra_mapping").mode("overwrite").mongo()



