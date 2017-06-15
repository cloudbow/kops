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


val hiveContext = new HiveContext(sc)
val sqlContext = new SQLContext(sc)

//parse linear feed and get mapping from subpackage_id -> channel_guid -> [echostar_content_ids]


//get all linear feeds
"curl -o /tmp/channels.json http://7dd67fc2.cms.movetv.com/cms/api/linear_feed/4" !!
import org.apache.spark.sql.Row
import org.apache.spark.sql.functions.{ concat, lit, coalesce, max }
import org.apache.spark.sql.types.{ StructType, StructField, StringType, IntegerType };
import org.apache.spark.sql.DataFrame


//load the linear feed to dataframe
val channelsJson = sqlContext.read.json("/tmp/channels.json")
def children(colname: String, df: DataFrame) = {
  val parent = df.schema.fields.filter(_.name == colname).head
  val fields = parent.dataType match {
    case x: StructType => x.fields
    case _ => Array.empty[StructField]
  }
  fields.map(x => col(s"$colname.${x.name}"))
}


//select only required fields and explode channels & subpacks since they are arrays
val channelsDF = channelsJson.select($"channels")
val channelsDF1 = channelsDF.withColumn("chExploded", explode(channelsDF.col("channels"))).drop($"channels")
val channelsDF2 = channelsDF1.select(children("chExploded", channelsDF1) : _* )


val subPacks = channelsJson.select($"subscription_packs")
val subPacks2 = subPacks.withColumn("sbkExploded", explode(subPacks.col("subscription_packs"))).drop($"subscription_packs")
val subPacks3 = subPacks2.select(children("sbkExploded", subPacks2) : _* )
val subPacks4 = subPacks3.withColumn("sbExploded", explode(subPacks3.col("channels"))).drop($"channels").withColumnRenamed("sbExploded","channel_no").withColumnRenamed("guid","subpackage_guid").withColumnRenamed("id","subpack_int_id").withColumnRenamed("title","subpack_title")

//create join between channels and subpacks based on collection
val subPackChannelsDF = subPacks4.join(channelsDF2, subPacks4("channel_no") === channelsDF2("id"), "inner").withColumnRenamed("guid", "channel_guid")
val subPackChannelsDF1 = subPackChannelsDF.select($"subpackage_guid",$"subpack_int_id",$"subpack_title",$"title",$"channel_guid",$"channel_no",$"_today",$"call_sign",$"genre")
val subPackChannelsDF2  = subPackChannelsDF1.filter("genre='Sports'")
val subPackChannelsDF3 = subPackChannelsDF2.groupBy($"channel_guid").agg(first($"_today") as "_today")

var urls = ""
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

// fetch _today urls for programs for next 3 days
"cat /dev/null" #> new File("/tmp/schedules_plus_3")
subPackChannelsDF3.select($"_today").collect.distinct.foreach{   it => 
	val url = it.getString(0)
	if(url!=null) { 
		for( i <- 0 to 3){
			val epochTimeOffset = Instant.now().plus(i, ChronoUnit.DAYS);      				  
			val utc = epochTimeOffset.atZone(ZoneId.of("Z"));        				  
			def pattern = "yyyyMMdd";
			val formattedDate = utc.format(DateTimeFormatter.ofPattern(pattern)); 
			val fullUrl = url.slice(0, 0+url.lastIndexOf("/")) + formattedDate
			Thread sleep 1000
			(s"curl  $url" #>> new File("/tmp/schedules_plus_3")).!
			(s"echo "  #>> new File("/tmp/schedules_plus_3")).!
		}
       }
}




val schedulesDF= spark.read.json("/tmp/schedules_plus_3")


//load franchise df with all required information
val franchisesDF = schedulesDF.select($"franchises")
val franchisesDF1 = franchisesDF.withColumn("franchisesExp",explode($"franchises")).drop("franchises")
val franchisesDF2 = franchisesDF1.select($"franchisesExp.id",$"franchisesExp.lookups")
val franchisesDF3 = franchisesDF2.withColumn("lookupsExploded",explode($"lookups")).drop("lookups")
val franchisesDF4 = franchisesDF3.select(Array(col("id")) ++ children("lookupsExploded", franchisesDF3) : _* ).withColumnRenamed("key", "fr_id_key").withColumnRenamed("value","fr_id_val")
val franchisesDF5 = franchisesDF4.filter("fr_id_key='echostar_id' or fr_id_key='rovi_series_id'")

//get all content_ids from dish_live_v2 for series_id
"cat /dev/null" #> new File("/tmp/series_content_ids")
franchisesDF5.select($"fr_id_val").collect.distinct.foreach{   it => 
	val seriesId = it.getString(0)
	if(seriesId!=null) { 
			val url = s"http://egi.slingbox.com/solr/dish_live_v2/select/?q=series_id:$seriesId&fl=content_id,series_id,callsign&wt=json"
			Thread sleep 1000
			(s"curl  $url" #>> new File("/tmp/series_content_ids")).!
			(s"echo "  #>> new File("/tmp/series_content_ids")).!
	
       }
}
franchisesDF5.select($"fr_id_val").collect.distinct.foreach{   it => 
	val seriesId = it.getString(0)
	if(seriesId!=null) { 
			val url = s"http://egi.slingbox.com/solr/dish_live_v2/select/?q=content_id:$seriesId&fl=content_id,series_id,callsign&wt=json"
			Thread sleep 1000
			(s"curl  $url" #>> new File("/tmp/series_content_ids")).!
			(s"echo "  #>> new File("/tmp/series_content_ids")).!
	
       }
}


val seriesCidMappingDF= spark.read.json("/tmp/series_content_ids")
val seriesCidMappingDF1= seriesCidMappingDF.select($"response.docs")
val seriesCidMappingDF2= seriesCidMappingDF1.withColumn("docsExp",explode($"docs")).drop("docs")
val seriesCidMappingDF3 = seriesCidMappingDF2.select(children("docsExp", seriesCidMappingDF2) : _* )
val seriesCidMappingDF31 = seriesCidMappingDF3.distinct.toDF
//join franchise with seriesId
val franchiseSeriesJoinDF = franchisesDF5.join(seriesCidMappingDF31, franchisesDF5("fr_id_val") === seriesCidMappingDF31("series_id"), "inner").drop("id").drop("fr_id_key").drop("fr_id_val")





val programsDF1 = schedulesDF.select($"_self",$"programs")
val programsDF2 = programsDF1.withColumn("programsExploded", explode(programsDF1.col("programs"))).drop("programs");
import scala.collection.mutable.WrappedArray
def listToStrFunc(genres: WrappedArray[String]): String = {	
	if(genres==null) "" else genres.mkString(" ")	
}
val listToStrUDF = udf(listToStrFunc(_: WrappedArray[String]))


val programsDF21 = programsDF2.withColumn("genresStr",listToStrUDF($"programsExploded.genres"))
val programsDF22 = programsDF21.filter($"genresStr".contains("sport")).drop("genresStr")
val programsDF3 = programsDF22.select($"_self",$"programsExploded.id",$"programsExploded.guid", $"programsExploded.lookups",$"programsExploded.franchise_id",$"programsExploded.name" as "program_title")
val programsDF31 = programsDF3.distinct.toDF




def getChannelGuidFromSelfFunc(url: String): String = {
	var chUrl= url.slice(0, 0+url.lastIndexOf("/"))
	chUrl = chUrl.substring(chUrl.lastIndexOf("/")+1)
	chUrl
}
val channelGuidUDF = udf(getChannelGuidFromSelfFunc(_: String))

val programsDF4 = programsDF31.withColumn("channel_guid_extr", channelGuidUDF($"_self")).drop("_self").withColumnRenamed("guid","program_guid").withColumnRenamed("id","program_id")
val programsDF5 = programsDF4.withColumn("lookupsExploded", explode(programsDF4.col("lookups"))).drop("lookups");
val programsDF6 = programsDF5.select(Array(col("program_id"), col("program_guid"),col("channel_guid_extr"),col("franchise_id"),col("program_title")) ++ children("lookupsExploded", programsDF5) : _* )
val programsDF7 = programsDF6.withColumnRenamed("key", "id_key").withColumnRenamed("value","id_val")
val programsDF8 = programsDF7.filter("id_key='echostar_id' or id_key='rovi_series_id'")
val programsDF81 = programsDF8.distinct.toDF
val finalContentIdsList = franchiseSeriesJoinDF.join(programsDF81, franchiseSeriesJoinDF("series_id") === programsDF81("id_val"), "inner").drop("id").drop("franchise_id").drop("id_key").drop("id_val")



//join with channel_guid after gettind data for todays urls
val programsDF9 = subPackChannelsDF2.join(finalContentIdsList, subPackChannelsDF2("channel_guid") === finalContentIdsList("channel_guid_extr"), "inner").drop("channel_guid_extr")
val programsDF91 = programsDF9.distinct.toDF






//parse mongo db data and get the mapping from echostar_content_id -> gameId -> teamId
case class ExternalId(id: String, provider: String)
case class Team(uid:String, externalIds:Array[ExternalId])
case class EventTeam(uid:String, teamId:String)

val readConfigEventTeam = ReadConfig(Map("partitionKey"->"_id","numberOfPartitions"->"20","collection" -> "eventTeam", "readPreference.name" -> "primaryPreferred","partitioner"->"MongoPaginateByCountPartitioner"), Some(ReadConfig(spark)))
val eventTeamDF = MongoSpark.load[EventTeam](spark, readConfigEventTeam)	



val readConfigTeam = ReadConfig(Map("partitionKey"->"_id","numberOfPartitions"->"20","collection" -> "team", "readPreference.name" -> "primaryPreferred","partitioner"->"MongoPaginateByCountPartitioner"), Some(ReadConfig(spark)))
val teamDF = MongoSpark.load[Team](spark, readConfigTeam)




val readConfigEvent = ReadConfig(Map("partitionKey"->"_id","numberOfPartitions"->"20","collection" -> "event", "readPreference.name" -> "primaryPreferred","partitioner"->"MongoPaginateByCountPartitioner"), Some(ReadConfig(spark)))
case class Event(contentIDs:String, externalIds: Array[ExternalId], eventTeams: Array[String] )
val eventDF=  MongoSpark.load[Event](spark,readConfigEvent)



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



val eventDF10 = eventDF.withColumn("contentIdsExtr", contentIdArrayUDF($"contentIDs")).drop("contentIDs");
val eventDF1 = eventDF10.withColumn("contentId", explode(eventDF10.col("contentIdsExtr"))).drop("contentIdsExtr")

val eventDF2 = eventDF1.withColumn("externalIdsExploded", explode(eventDF.col("externalIds"))).drop("externalIds");
val eventDF22 = eventDF2.select( Array(col("contentId"), col("eventTeams")) ++ children("externalIdsExploded", eventDF2) : _* ).withColumnRenamed("id","gameId")
val eventDF3 = eventDF22.withColumn("eventTeamId", explode(eventDF.col("eventTeams"))).drop("eventTeams");
val eventDF4 = eventDF3.filter("provider='nagra'").filter("contentId!='null'").filter("contentId!=''")
//join events with eventTeam to get home and away team
val eventDF5 = eventDF4.join(eventTeamDF, eventDF4("eventTeamId") === eventTeamDF("uid"), "inner").drop("uid").drop("eventTeamId").drop("provider")
//join with team to get teamid from nagra
val eventDF6 = eventDF5.join(teamDF, eventDF5("teamId") === teamDF("uid"), "inner").drop("uid").drop("teamId")
val eventDF7 =  eventDF6.withColumn("externalIdsExploded", explode(eventDF6.col("externalIds"))).drop("externalIds");
val eventDF8 = eventDF7.select( Array(col("contentId"), col("gameId")) ++ children("externalIdsExploded", eventDF7) : _* ).withColumnRenamed("id","teamId")
val eventDF9 = eventDF8.filter("provider='nagra'").drop("provider")
	
//join between slingtv and our mongo data
val joinedContentDF = eventDF9.join(programsDF91, eventDF9("contentId") === programsDF91("content_id"), "inner").drop("content_id").drop("series_id")

//convert to a form with json key as  channel_guid_content_id
case class GameEvent(contentId:String, gameId:String, teamId:String,seriesId:String, channelGuid:String, programTitle:String, programId:String, programGuid:String,subpackageGuid:String,subpackTitle:String,title:String,callsign:String)
val mongoChGPgmIdCallsignDF2 = joinedContentDF.map {
         case Row(contentId: String,gameId: String,teamId: String,series_id:String, channel_guid: String, program_title:String,program_id: String,program_guid: String,subpackage_guid:String,subpack_title:String,title:String,callsign:String) =>
           ((channel_guid.concat("_").concat(program_id).cocnat("_").concat("callsign")), GameEvent(contentId, gameId,teamId,series_id,channel_guid,program_title,program_id,program_guid,subpackage_guid,subpack_title,title,callsign))
}
val mongoChGPgmIdCallsignDF3 = mongoChGPgmIdCallsignDF2.withColumnRenamed("_1","id").withColumnRenamed("_2","gameEvent")
val mongoChGPgmIdCallsignDF4 = mongoChGPgmIdCallsignDF3.groupBy("id").agg(collect_list("gameEvent")).withColumnRenamed("collect_list(gameEvent)","gameEvents")


val mongoChGPgmIdDF2 = mongoChGPgmIdDF.map {
         case Row(contentId: String,gameId: String,teamId: String,series_id:String, channel_guid: String, program_title:String,program_id: String,program_guid: String,subpackage_guid:String,subpack_title:String,title:String,callsign:String) =>
           ((channel_guid.concat("_").concat(program_id).concat("_").concat("callsign")), GameEvent(contentId, gameId,teamId,series_id,channel_guid,program_title,program_id,program_guid,subpackage_guid,subpack_title,title,callsign))
}
val mongoChGPgmIdDF3 = mongoChGPgmIdDF2.withColumnRenamed("_1","id").withColumnRenamed("_2","gameEvent")
val mongoChGPgmIdDF4 = mongoChGPgmIdDF3.groupBy("id").agg(collect_list("gameEvent")).withColumnRenamed("collect_list(gameEvent)","gameEvents")



//write to mongo
val writeConfig = WriteConfig(Map("collection" -> "slingtv_schguid_program_id_callsign_cont_nagra_mapping", "writeConcern.w" -> "majority"), Some(WriteConfig(sc)))
mongoChGPgmIdCallsignDF4.write.option("collection", "slingtv_schguid_program_id_callsign_cont_nagra_mapping").mode("overwrite").mongo()
mongoChGPgmIdDF4.write.option("collection", "slingtv_schguid_program_id_cont_nagra_mapping").mode("overwrite").mongo()



