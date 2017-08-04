package com.slingmedia.sportscloud.schedulers

import java.io.{File,PrintWriter}
import java.util.{TimeZone,EnumSet}
import java.time.{ ZonedDateTime , OffsetDateTime , ZoneOffset , Instant, ZoneId}
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

import sys.process._
import scala.io.Source
import scala.language.postfixOps
import collection.JavaConverters._

import com.jayway.jsonpath.{ Configuration,JsonPath, Option, ReadContext ,Criteria , Filter , TypeRef}
import com.jayway.jsonpath.spi.json. { JsonProvider , GsonJsonProvider }
import com.jayway.jsonpath.spi.mapper.{ MappingProvider, JacksonMappingProvider }

import com.google.gson.{JsonArray,JsonElement}

import org.quartz.{ Job , JobExecutionContext }

import org.slf4j.LoggerFactory;


object Holder extends Serializable {
  val serialVersionUID = 1L;
  @transient lazy val log = LoggerFactory.getLogger("SportsCloudSchedulers")
}


object SportsCloudSchedulers  {

	private var quartzSchedulerWrapper:QuartzSchedulerWrapper = null 

	def main(args: Array[String]) {
	    quartzSchedulerWrapper =  QuartzSchedulerWrapper()
	    Holder.log.debug("Args is $args")	
	    var allBatchJobs:List[ScheduledJob] = List()
	    
	    val downloadSummaryJob:ScheduledJob = ScheduledJob("downloadSummaryJob","sportscloud-batch-schedules",classOf[DownloadSummaryJob].asInstanceOf[Class[Any]],"0 0 7 ? * *",ScheduleType.CRON_MISFIRE_DO_NOTHING)
	    allBatchJobs = downloadSummaryJob :: allBatchJobs
	    Holder.log.debug("Adding downloadSummaryJob")
	    
	    val  downloadSchedulesJob:ScheduledJob = ScheduledJob("downloadShedulesJob","sportscloud-batch-schedules",classOf[DownloadSchedulesJob].asInstanceOf[Class[Any]],"0 15 7 ? * *",ScheduleType.CRON_MISFIRE_NOW)
	    allBatchJobs = downloadSchedulesJob :: allBatchJobs
		  Holder.log.debug("Adding downloadSchedulesJob")
		  
		  val  contentMatchJob:ScheduledJob = ScheduledJob("contentMatchJob","sportscloud-batch-schedules",classOf[ContentMatchJob].asInstanceOf[Class[Any]],"0 0 8 ? * *",ScheduleType.CRON_MISFIRE_NOW)
	    allBatchJobs = contentMatchJob :: allBatchJobs
	    Holder.log.debug("Adding contentMatchJob")
	    
	    val  teamStandingsMetaDataBatchJob:ScheduledJob = ScheduledJob("teamStandingsMetaDataBatchJob","sportscloud-batch-schedules",classOf[TeamStandingsMetaDataBatchJob].asInstanceOf[Class[Any]],"0 30 8 ? * *",ScheduleType.CRON_MISFIRE_DO_NOTHING)
	    allBatchJobs = teamStandingsMetaDataBatchJob :: allBatchJobs
	    Holder.log.debug("Adding metaDataBatchJob")
	    
	    val  playerStatsMetaDataBatchJob:ScheduledJob = ScheduledJob("playerStatsMetaDataBatchJob","sportscloud-batch-schedules",classOf[PlayerStatsMetaDataBatchJob].asInstanceOf[Class[Any]],"0 45 8 ? * *",ScheduleType.CRON_MISFIRE_DO_NOTHING)
	    allBatchJobs = playerStatsMetaDataBatchJob :: allBatchJobs
	    Holder.log.debug("Adding metaDataBatchJob")
	    
	    val  batchScoreJob:ScheduledJob = ScheduledJob("batchScoreJob","sportscloud-batch-schedules",classOf[BatchScoreJob].asInstanceOf[Class[Any]],null,ScheduleType.FIRE_ONCE)
	    allBatchJobs = batchScoreJob :: allBatchJobs
	    Holder.log.debug("Adding batchScoreJob")

	    val  thuuzJob:ScheduledJob = ScheduledJob("thuuzJob","sportscloud-batch-schedules",classOf[ThuuzJob].asInstanceOf[Class[Any]],"0 0 7 ? * *",ScheduleType.CRON_MISFIRE_NOW)
	    allBatchJobs = thuuzJob :: allBatchJobs
	    Holder.log.debug("Adding thuuzJob")
	   	
	    Holder.log.debug("Publishing all batch jobs")
	    quartzSchedulerWrapper.publishJobs(allBatchJobs)
	    
	    var allRestartableJobs:List[ScheduledJob] = List()
	    
	    val  liveStreamJob:ScheduledJob = ScheduledJob("liveStreamJob","sportscloud-restartable-schedules",classOf[LiveStreamJob].asInstanceOf[Class[Any]],"0 * * ? * *",ScheduleType.CRON_MISFIRE_DO_NOTHING)
	    allRestartableJobs = liveStreamJob :: allRestartableJobs
	    Holder.log.debug("Adding liveStreamJob")
	    
	    val  kakfkConnCM:ScheduledJob = ScheduledJob("kafkaConnContentMatch","sportscloud-restartable-schedules",classOf[KafkaConnectContentMatchJob].asInstanceOf[Class[Any]],"0 * * ? * *",ScheduleType.CRON_MISFIRE_DO_NOTHING)
	    allRestartableJobs = kakfkConnCM :: allRestartableJobs
	    Holder.log.debug("Adding kafkaConnContentMatch")
	    
	    val  kakfkConnLI:ScheduledJob = ScheduledJob("kafkaConnLiveInfo","sportscloud-restartable-schedules",classOf[KafkaConnectLiveInfoJob].asInstanceOf[Class[Any]],"0 * * ? * *",ScheduleType.CRON_MISFIRE_DO_NOTHING)
	    allRestartableJobs = kakfkConnLI :: allRestartableJobs
	    Holder.log.debug("Adding kafkaConnLiveInfo")
	    
	    val  kakfkConnMB:ScheduledJob = ScheduledJob("kafkaConnMetaBatch","sportscloud-restartable-schedules",classOf[KafkaConnectMetaBatchJob].asInstanceOf[Class[Any]],"0 * * ? * *",ScheduleType.CRON_MISFIRE_DO_NOTHING)
	    allRestartableJobs = kakfkConnMB :: allRestartableJobs
	    Holder.log.debug("Adding kafkaConnMetaBatch")
	    
	    Holder.log.debug("Publishing all streaming jobs")
	    quartzSchedulerWrapper.publishJobs(allRestartableJobs)

	}

}


class KafkaConnectContentMatchJob extends Job {
  private val log = LoggerFactory.getLogger("KafkaConnectContentMatchJob")
	override def execute(context:JobExecutionContext) {
    log.trace("Executing task : KafkaConnectContentMatchJob")	  
    Seq("/project/sports-cloud-schedulers/src/main/resources/scripts/allenv/launch_kafka_connect_jobs.sh",
        "/project/sports-cloud-parsers/src/main/resources/kafka-standalone/cs-content-match.properties",
        "/project/sports-cloud-parsers/src/main/resources/kafka-connect/ftp-connect-content-match.properties",
        "/var/log/sports-cloud-kafka-jobs/cs-content-match-kafka-connect.log" ) !      
	}
}


class KafkaConnectMetaBatchJob extends Job {
  private val log = LoggerFactory.getLogger("KafkaConnectMetaBatchJob")
	override def execute(context:JobExecutionContext) {
    log.trace("Executing task : KafkaConnectMetaBatchJob")	  
    Seq("/project/sports-cloud-schedulers/src/main/resources/scripts/allenv/launch_kafka_connect_jobs.sh",
    "/project/sports-cloud-parsers/src/main/resources/kafka-standalone/cs-meta-batch.properties",
    "/project/sports-cloud-parsers/src/main/resources/kafka-connect/ftp-meta-batch.properties",
    "/var/log/sports-cloud-kafka-jobs/cs-meta-batch-kafka-connect.log") !
    
	}
}

class KafkaConnectLiveInfoJob extends Job {
  private val log = LoggerFactory.getLogger("KafkaConnectLiveInfoJob")
	override def execute(context:JobExecutionContext) {
    log.trace("Executing task : KafkaConnectLiveInfoJob")	  
    Seq("/project/sports-cloud-schedulers/src/main/resources/scripts/allenv/launch_kafka_connect_jobs.sh",
    "/project/sports-cloud-parsers/src/main/resources/kafka-standalone/cs-live-info.properties",
    "/project/sports-cloud-parsers/src/main/resources/kafka-connect/ftp-live-scores.properties",
    "/var/log/sports-cloud-kafka-jobs/cs-live-info-kafka-connect.log" ) !  
	}
}


class ThuuzJob extends Job {
  private val log = LoggerFactory.getLogger("ThuuzJob")
	override def execute(context:JobExecutionContext) {
    log.trace("Executing task : ThuuzJob")	  
		"curl http://api.thuuz.com/2.2/games?auth_code=6adf97f8142118ba&type=normal&status=5&days=3&sport_leagues=baseball.mlb,basketball.nba,basketball.ncaa,football.nfl,football.ncaa,hockey.nhl,golf.pga,soccer.mwc,soccer.chlg,soccer.epl,soccer.seri,soccer.liga,soccer.bund,soccer.fran,soccer.mls,soccer.wwc,soccer.ligamx,soccer.ered,soccer.ch-uefa2,soccer.eng2,soccer.prt1,soccer.sco1,soccer.tur1,soccer.rus1,soccer.bel1,soccer.euro&limit=999" #> new File("/data/feeds/thuuz.json") !
	}
}



abstract class SparkSubmitJob extends Job {
  protected val sportsCloudBatchJarLoc = System.getProperty("sportsCloudBatchJarLoc")
  protected val sparkHomeLoc  = System.getProperty("sparkHomeLoc")
  protected val sparkExtraJars = System.getProperty("sparkExtraJars")
  protected val sparkPackages = Seq("--packages", "org.apache.spark:spark-sql-kafka-0-10_2.11:2.1.1")
  protected var mainClass:String = null
  protected var name:String =null
  
  def buildSparkCommand(name:String, mainClass:String, extraJars:String, jarName:String, args:String):Seq[String] = {  
    val sparkCommand:Seq[String] = Seq(s"$sparkHomeLoc/bin/spark-submit",
        "--name",name,
        "--class", mainClass ,
        "--master","local[8]", 
        "--driver-java-options", "-Dlog4j.configuration=file:/spark-log4j-config/log4j-driver.properties", 
        "--driver-memory", "7G", 
        "--executor-memory", "7G", 
        "--total-executor-cores", "4", 
        "--conf", "spark.default.parallelism=4",
        "--conf", "spark.executor.extraJavaOptions=-Dlog4j.configuration=file:/spark-log4j-config/log4j-executor.properties",
        "--conf", "spark.serializer=org.apache.spark.serializer.KryoSerializer") ++
      sparkPackages ++
      Seq("--jars",sparkExtraJars) ++
      Seq(jarName) ++
      args.split(" ").toSeq.asInstanceOf[Seq[String]]      
    sparkCommand  
  }
  
}


class ContentMatchJob extends SparkSubmitJob {
  private val log = LoggerFactory.getLogger("ContentMatchJob")
  
  override def execute(context:JobExecutionContext) {
    mainClass = "com.slingmedia.sportscloud.offline.batch.impl.ContentMatcher"
    name = "SportsCloudContentMatch"
    log.trace("Executing task : SparkSubmitJob")
    val sparkSumbitCommand = buildSparkCommand(name,mainClass,sparkExtraJars,sportsCloudBatchJarLoc,"content_match game_schedule localhost:9983")
    log.trace(s"Executing command $sparkSumbitCommand")
    sparkSumbitCommand #>> new File("/var/log/sports-cloud-schedulers/sc-batch-job.log") !

	}
}

class TeamStandingsMetaDataBatchJob extends SparkSubmitJob {
  private val log = LoggerFactory.getLogger("TeamStandingsMetaDataBatchJob")
  
  override def execute(context:JobExecutionContext) {
    mainClass = "com.slingmedia.sportscloud.offline.batch.impl.MetaDataMuncher"
    name = "TeamStandingsMetaDataMuncher"
    log.trace("Executing task : TeamStandingsMetaDataBatchJob")
   
    val sparkSumbitCommand = buildSparkCommand(name,mainClass,sparkExtraJars,sportsCloudBatchJarLoc,"TEAMSTANDINGS meta_batch team_standings localhost:9983")
    log.trace(s"Executing command $sparkSumbitCommand")
    sparkSumbitCommand #>> new File("/var/log/sports-cloud-schedulers/sc-batch-job.log") !

	}
}

class PlayerStatsMetaDataBatchJob extends SparkSubmitJob {
  private val log = LoggerFactory.getLogger("PlayerStatsMetaDataBatchJob")
  
  override def execute(context:JobExecutionContext) {
    mainClass = "com.slingmedia.sportscloud.offline.batch.impl.MetaDataMuncher"
    name = "PlayerStatsDataMuncher"
    log.trace("Executing task : PlayerStatsMetaDataBatchJob")
   
    val sparkSumbitCommand = buildSparkCommand(name,mainClass,sparkExtraJars,sportsCloudBatchJarLoc,"PLAYERSTATS meta_batch player_stats localhost:9983")
    log.trace(s"Executing command $sparkSumbitCommand")
    sparkSumbitCommand #>> new File("/var/log/sports-cloud-schedulers/sc-batch-job.log") !

	}
}

class BatchScoreJob extends SparkSubmitJob {
  private val log = LoggerFactory.getLogger("BatchScoreJob")
  
  override def execute(context:JobExecutionContext) {
    mainClass = "com.slingmedia.sportscloud.offline.batch.impl.MetaDataMuncher"
    name = "BatchScoreJob"
    log.trace("Executing task : BatchScoreJob")
   
    val sparkSumbitCommand = buildSparkCommand(name,mainClass,sparkExtraJars,sportsCloudBatchJarLoc,"LIVEINFO live_info live_info localhost:9983")
    log.trace(s"Executing command $sparkSumbitCommand")
    sparkSumbitCommand #>> new File("/var/log/sports-cloud-schedulers/sc-stream-job.log") !

	}
}

class LiveStreamJob extends SparkSubmitJob {
  private val log = LoggerFactory.getLogger("LiveStreamJob")
  
  override def execute(context:JobExecutionContext) {
    //This needs to be a long running job and hence for reliability we use a shell script
    "/project/sports-cloud-schedulers/src/main/resources/scripts/allenv/launch_live_info_streaming.sh"  !
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
		val cmsSummaryUrl = System.getProperty("cmsSummaryUrl")
		s"curl http://$cmsHost.cdn.cms.movetv.com/$cmsSummaryUrl" #> new File("/data/feeds/summary.json") !
	}

}

