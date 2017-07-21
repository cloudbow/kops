package com.slingmedia.sportscloud.schedulers

import java.io.{File,PrintWriter}

import java.util.TimeZone

import java.time.{ ZonedDateTime , OffsetDateTime , ZoneOffset , Instant, ZoneId}
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

import sys.process._

import scala.io.Source

import collection.JavaConverters._

import com.jayway.jsonpath.{ Configuration,JsonPath, Option, ReadContext ,Criteria , Filter , TypeRef}
import com.jayway.jsonpath.spi.json. { JsonProvider , GsonJsonProvider }
import com.jayway.jsonpath.spi.mapper.{ MappingProvider, JacksonMappingProvider }

import com.google.gson.JsonArray

import java.util.EnumSet

import org.quartz.{ Job , JobExecutionContext }

import org.slf4j.LoggerFactory;
import com.typesafe.scalalogging.slf4j.Logger
import com.google.gson.JsonElement

 

object Holder extends Serializable {
  val serialVersionUID = 1L;
  @transient lazy val log = LoggerFactory.getLogger("SportsCloudSchedulers")
}


object SportsCloudSchedulers  {

	private var quartzSchedulerWrapper:QuartzSchedulerWrapper = null 

	def main(args: Array[String]) {
	    quartzSchedulerWrapper =  QuartzSchedulerWrapper()
	    Holder.log.debug("Args is $args")	
	    var allHomeGeneousJobsList:List[ScheduledJob] = List()
	    val downloadSummaryJob:ScheduledJob = ScheduledJob("downloadSummaryJob","sportscloud-batch-schedules",classOf[DownloadSummaryJob].asInstanceOf[Class[Any]],"0 0 7 ? * *",ScheduleType.CRON_MISFIRE_DO_NOTHING)
	    allHomeGeneousJobsList = downloadSummaryJob :: allHomeGeneousJobsList
	    Holder.log.debug("Adding downloadSummaryJob")
	    val  downloadSchedulesJob:ScheduledJob = ScheduledJob("downloadShedulesJob","sportscloud-batch-schedules",classOf[DownloadSchedulesJob].asInstanceOf[Class[Any]],"0 0 8 ? * *",ScheduleType.CRON_MISFIRE_NOW)
	    allHomeGeneousJobsList = downloadSchedulesJob :: allHomeGeneousJobsList
		  Holder.log.debug("Adding downloadSchedulesJob")
		  val  sparkSubmitJob:ScheduledJob = ScheduledJob("contentMatchJob","sportscloud-batch-schedules",classOf[SparkSubmitJob].asInstanceOf[Class[Any]],"0 15 7 ? * *",ScheduleType.CRON_MISFIRE_DO_NOTHING)
	    allHomeGeneousJobsList = sparkSubmitJob :: allHomeGeneousJobsList
	    Holder.log.debug("Adding sparkSubmitJob")
	    val  thuuzJob:ScheduledJob = ScheduledJob("thuuzJob","sportscloud-batch-schedules",classOf[ThuuzJob].asInstanceOf[Class[Any]],"0 0 7 ? * *",ScheduleType.CRON_MISFIRE_NOW)
	    allHomeGeneousJobsList = thuuzJob :: allHomeGeneousJobsList
	    Holder.log.debug("Adding thuuzJob")
	   	Holder.log.debug("Publishing all jobs")
	    quartzSchedulerWrapper.publishJobs(allHomeGeneousJobsList,ScheduleType.CRON_MISFIRE_DO_NOTHING)
	}

}

class ThuuzJob extends Job {
  private val log = LoggerFactory.getLogger("ThuuzJob")
	override def execute(context:JobExecutionContext) {
    log.trace("Executing task : ThuuzJob")	  
		"curl http://api.thuuz.com/2.2/games?auth_code=6adf97f8142118ba&type=normal&status=5&days=3&sport_leagues=baseball.mlb,basketball.nba,basketball.ncaa,football.nfl,football.ncaa,hockey.nhl,golf.pga,soccer.mwc,soccer.chlg,soccer.epl,soccer.seri,soccer.liga,soccer.bund,soccer.fran,soccer.mls,soccer.wwc,soccer.ligamx,soccer.ered,soccer.ch-uefa2,soccer.eng2,soccer.prt1,soccer.sco1,soccer.tur1,soccer.rus1,soccer.bel1,soccer.euro&limit=999" #> new File("/data/feeds/thuuz.json") !!
	}
}

class SparkSubmitJob extends Job {
  private val log = LoggerFactory.getLogger("SparkSubmitJob")

	override def execute(context:JobExecutionContext) {
    log.trace("Executing task : SparkSubmitJob")
    val sportsCloudBatchJarLoc = System.getProperty("sportsCloudBatchJarLoc")
    val sparkHomeLoc  = System.getProperty("sparkHomeLoc")
		s"""$sparkHomeLoc/bin/spark-submit --name SportsCloudOfflineBatchJob --class com.slingmedia.sportscloud.offline.batch.ContentMatcher  --master local[8]  --driver-memory 7G --executor-memory 7G --total-executor-cores 4 --conf "spark.mongodb.input.uri=mongodb://sportapi2:Eo8ahsiera@cqhlsdb02.sling.com:2701/eventstorage.event?readPreference=primaryPreferred" --conf "spark.mongodb.output.uri=mongodb://sportapi2:Eo8ahsiera@cqhlsdb02.sling.com:2701/eventstorage.slingtv_schguid_cont_nagra_mapping" --packages com.databricks:spark-csv_2.10:1.5.0,joda-time:joda-time:2.9.7,org.mongodb.spark:mongo-spark-connector_2.11:2.0.0,com.databricks:spark-xml_2.10:0.4.1,org.apache.spark:spark-streaming-kafka-0-10_2.11:2.1.1,org.apache.spark:spark-sql-kafka-0-10_2.11:2.1.1,org.scala-lang.modules:scala-xml_2.11:1.0.6,org.apache.kafka:connect-api:0.10.2.0,org.quartz-scheduler:quartz:2.3.0,net.liftweb:lift-json_2.11:2.6.3,com.jayway.jsonpath:json-path:2.4.0    $sportsCloudBatchJarLoc  """ #>> new File("/var/log/sc-batch-job.log") !!
	}

}

class DownloadSchedulesJob extends Job {
  private val log = LoggerFactory.getLogger("DownloadSchedulesJob")

	override def execute(context:JobExecutionContext) {
    log.trace("Executing task : DownloadSchedulesJob")
		
		val conf :Configuration= Configuration.defaultConfiguration();
    Configuration.setDefaults(new Configuration.Defaults() {

    		val  jsonProviderObj:JsonProvider = new GsonJsonProvider();
    		val  mappingProviderObj:MappingProvider = new JacksonMappingProvider();
      
      		override def jsonProvider():JsonProvider = {
      			jsonProviderObj;
      		}

      		override def mappingProvider():MappingProvider = {
      			 mappingProviderObj;
      		}

      		override def options():java.util.Set[Option] = {
      			EnumSet.noneOf(classOf[Option])
      		}
    
		});
		"cat /dev/null" #> new File("/data/feeds/schedules_plus_3") ! ;
		getFileContents("/data/feeds/summary.json").foreach( it => {

				val json = it
				val ctx:ReadContext = JsonPath.using(conf).parse(it)
				val  sportsGenreFilter:Filter = Filter.filter(Criteria.where("metadata.genre").contains("Sports"))
				val filteredChannels:String = ctx.read("$.channels[?]", sportsGenreFilter).toString
				val jsonArray:JsonArray = JsonPath.read[com.google.gson.JsonArray](filteredChannels,"$.[*].channel_guid")
				val  iterator:Iterator[JsonElement] = jsonArray.iterator.asScala
				while(iterator.hasNext) {
				  val channel_guid=iterator.next.getAsString
					if(channel_guid!=null) { 
						for( i <- 0 to 3){
							val epochTimeOffset = Instant.now().plus(i, ChronoUnit.DAYS);      				  
							val utc = epochTimeOffset.atZone(ZoneId.of("Z"));        				  
							def pattern = "yyyyMMdd";
							val formattedDate = utc.format(DateTimeFormatter.ofPattern(pattern)); 
							val cmsHost =  System.getProperty("cmsHost")
							val fullUrl = s"http://$cmsHost.cdn.cms.movetv.com/cms/api/linear_feed/channels/v1/$channel_guid/" + formattedDate
							log.trace(s"Full url is $fullUrl")
							Thread sleep 3000
							(s"curl $fullUrl" #>> new File("/data/feeds/schedules_plus_3")).!
							(s"echo "  #>> new File("/data/feeds/schedules_plus_3")).!
						}
					}
				}

		})

	}

	val getFileContents:(String=>List[String]) = (fileName:String ) => {
		val bufferedSource = Source.fromFile(fileName)
		val lines = bufferedSource.getLines.toList
		bufferedSource.close
		lines
	}

}


class DownloadSummaryJob extends Job {
  private val log = LoggerFactory.getLogger("DownloadSummaryJob")

	override def execute(context:JobExecutionContext) {
    log.trace("Executing task : DownloadSummaryJob")
		val cmsHost =  System.getProperty("cmsHost")
		s"curl http://$cmsHost.cdn.cms.movetv.com/cms/publish3/domain/summary/1.json" #> new File("/data/feeds/summary.json") !!
	}

}

