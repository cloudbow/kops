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
import com.jayway.jsonpath.spi.json. { JsonSmartJsonProvider, JsonProvider , GsonJsonProvider }
import com.jayway.jsonpath.spi.mapper.{ JsonSmartMappingProvider, MappingProvider, JacksonMappingProvider }

import com.google.gson.{JsonArray,JsonElement}


import org.slf4j.LoggerFactory;


object Holder extends Serializable {
  val serialVersionUID = 1L;
  @transient lazy val log = LoggerFactory.getLogger("SportsCloudSchedulers")
}

object KafkaConnectContentMatchJob  {
  def main(args: Array[String]) {
    new KafkaConnectContentMatchJob().execute(args)
  }
}

class KafkaConnectContentMatchJob {
  private val log = LoggerFactory.getLogger("KafkaConnectContentMatchJob")
  def execute(args: Array[String]) {
    args(0) match {

      case "ncaaf" =>
        log.trace("Executing task : NCAAF KafkaConnectContentMatchJob")
        Seq("/deploy-scheduled-jobs/scripts/kafka/connect/launch_kafka_connect_jobs.sh",
          "content_match_ncaaf",
          "5",
          "15",
          "unused",
          "36000000",
          "/project/sports-cloud-parsers/src/main/resources/kafka-standalone/cs-content-match.properties",
          "/deploy-scheduled-jobs/scripts/kafka/connect/worker-config/ncaaf/ftp-content-match-ncaaf.json",
          "/var/log/sports-cloud-kafka-jobs/cs-content-match-kafka-connect-ncaaf.log",
          "ftp-content-match-ncaaf") !
      case "nfl" =>
        log.trace("Executing task : nfl KafkaConnectContentMatchJob")
        Seq("/deploy-scheduled-jobs/scripts/kafka/connect/launch_kafka_connect_jobs.sh",
          "content_match_nfl",
          "5",
          "15",
          "unused",
          "36000000",
          "/project/sports-cloud-parsers/src/main/resources/kafka-standalone/cs-content-match.properties",
          "/deploy-scheduled-jobs/scripts/kafka/connect/worker-config/nfl/ftp-content-match-nfl.json",
          "/var/log/sports-cloud-kafka-jobs/cs-content-match-kafka-connect-nfl.log",
          "ftp-content-match-nfl") !
      case "_" =>

        log.trace("Executing task : KafkaConnectContentMatchJob")
        Seq("/deploy-scheduled-jobs/scripts/kafka/connect/launch_kafka_connect_jobs.sh",
          "content_match",
          "5",
          "15",
          "unused",
          "36000000",
          "/project/sports-cloud-parsers/src/main/resources/kafka-standalone/cs-content-match.properties",
          "/deploy-scheduled-jobs/scripts/kafka/connect/worker-config/ftp-content-match.json",
          "/var/log/sports-cloud-kafka-jobs/cs-content-match-kafka-connect.log",
          "ftp-content-match") !
    }
	}
}

object KafkaConnectMetaBatchJob  {
  def main(args: Array[String]) {
    new KafkaConnectMetaBatchJob().execute(args)
  }
}

class KafkaConnectMetaBatchJob {
  private val log = LoggerFactory.getLogger("KafkaConnectMetaBatchJob")
	 def execute(args: Array[String]) {
     args(0) match {

       case "ncaaf" =>
         log.trace("Executing task NCAAF: KafkaConnectMetaBatchJob")
         Seq("/deploy-scheduled-jobs/scripts/kafka/connect/launch_kafka_connect_jobs.sh",
           "meta_batch_ncaaf",
           "5",
           "15",
           "unused",
           "36000000",
           "/project/sports-cloud-parsers/src/main/resources/kafka-standalone/cs-meta-batch.properties",
           "/deploy-scheduled-jobs/scripts/kafka/connect/worker-config/ncaaf/ftp-meta-batch-ncaaf.json",
           "/var/log/sports-cloud-kafka-jobs/cs-meta-batch-kafka-connect-ncaaf.log",
           "ftp-meta-batch-ncaaf") !
       case "nfl" =>
         log.trace("Executing task nfl: KafkaConnectMetaBatchJob")
         Seq("/deploy-scheduled-jobs/scripts/kafka/connect/launch_kafka_connect_jobs.sh",
           "meta_batch_nfl",
           "5",
           "15",
           "unused",
           "36000000",
           "/project/sports-cloud-parsers/src/main/resources/kafka-standalone/cs-meta-batch.properties",
           "/deploy-scheduled-jobs/scripts/kafka/connect/worker-config/nfl/ftp-meta-batch-nfl.json",
           "/var/log/sports-cloud-kafka-jobs/cs-meta-batch-kafka-connect-nfl.log",
           "ftp-meta-batch-nfl") !
       case "_" =>
         log.trace("Executing task : KafkaConnectMetaBatchJob")
         Seq("/deploy-scheduled-jobs/scripts/kafka/connect/launch_kafka_connect_jobs.sh",
           "meta_batch",
           "5",
           "15",
           "unused",
           "36000000",
           "/project/sports-cloud-parsers/src/main/resources/kafka-standalone/cs-meta-batch.properties",
           "/deploy-scheduled-jobs/scripts/kafka/connect/worker-config/ftp-meta-batch.json",
           "/var/log/sports-cloud-kafka-jobs/cs-meta-batch-kafka-connect.log",
           "ftp-meta-batch") !
     }
    
	}
}

object KafkaConnectLiveInfoJob  {
  def main(args: Array[String]) {
    new KafkaConnectLiveInfoJob().execute(args)
  }
}

class KafkaConnectLiveInfoJob {
  private val log = LoggerFactory.getLogger("KafkaConnectLiveInfoJob")
	def execute(args: Array[String]) {
    args(0) match {

      case "ncaaf" =>
        log.trace("Executing task: KafkaConnectLiveInfoJob")
        Seq("/deploy-scheduled-jobs/scripts/kafka/connect/launch_kafka_connect_jobs.sh",
          "live_info_ncaaf",
          "0",
          "0",
          "unused",
          "1800000",
          "/project/sports-cloud-parsers/src/main/resources/kafka-standalone/cs-live-info.properties",
          "/deploy-scheduled-jobs/scripts/kafka/connect/worker-config/ncaaf/ftp-live-info-ncaaf.json",
          "/var/log/sports-cloud-kafka-jobs/cs-live-info-kafka-connect-ncaaf.log",
          "ftp-live-info-ncaaf") !
      case "nfl" =>
        log.trace("Executing task: KafkaConnectLiveInfoJob for nfl")
        Seq("/deploy-scheduled-jobs/scripts/kafka/connect/launch_kafka_connect_jobs.sh",
          "live_info_nfl",
          "0",
          "0",
          "unused",
          "1800000",
          "/project/sports-cloud-parsers/src/main/resources/kafka-standalone/cs-live-info.properties",
          "/deploy-scheduled-jobs/scripts/kafka/connect/worker-config/nfl/ftp-live-info-nfl.json",
          "/var/log/sports-cloud-kafka-jobs/cs-live-info-kafka-connect-nfl.log",
          "ftp-live-info-nfl") !
      case "_" =>
        log.trace("Executing task: KafkaConnectLiveInfoJob")
        Seq("/deploy-scheduled-jobs/scripts/kafka/connect/launch_kafka_connect_jobs.sh",
          "live_info",
          "0",
          "0",
          "unused",
          "1800000",
          "/project/sports-cloud-parsers/src/main/resources/kafka-standalone/cs-live-info.properties",
          "/deploy-scheduled-jobs/scripts/kafka/connect/worker-config/ftp-live-info.json",
          "/var/log/sports-cloud-kafka-jobs/cs-live-info-kafka-connect.log",
          "ftp-live-scores") !
    }
	}
}





abstract class SparkSubmitJob {
  protected val sportsCloudBatchJarLoc = System.getProperty("sportsCloudBatchJarLoc")
  protected val sparkHomeLoc  = System.getProperty("sparkHomeLoc")
  protected val sparkExtraJars = System.getProperty("sparkExtraJars")
  protected val sparkPackages = Seq("--packages", "org.apache.spark:spark-sql-kafka-0-10_2.11:2.1.1")
  protected var mainClass:String = null
  protected var name:String =null
  
  def buildSparkCommand(name:String, mainClass:String, extraJars:String, jarName:String, args:String):Seq[String] = {  
    val gcPrintFlags = " -XX:+PrintFlagsFinal -XX:+PrintReferenceGC -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintAdaptiveSizePolicy -XX:+UnlockDiagnosticVMOptions -XX:+G1SummarizeConcMark "
    val g1gcOpts = "-XX:+UseG1GC -XX:InitiatingHeapOccupancyPercent=30  -XX:G1ReservePercent=15 -XX:MaxGCPauseMillis=2000"
    val sparkCommand:Seq[String] = Seq(s"$sparkHomeLoc/bin/spark-submit",
        "--name",name,
        "--class", mainClass ,
        "--master","local[8]", 
        "--driver-java-options", "-Dlog4j.configuration=file:/spark-log4j-config/log4j-driver.properties", 
        "--driver-memory", "7G", 
        "--executor-memory", "7G", 
        "--total-executor-cores", "4", 
        "--conf", "spark.es.index.auto.create=false",
        "--conf", "spark.es.resource=sports-cloud/game_schedule",
        "--conf", "spark.es.nodes=localhost",
        "--conf", "spark.es.net.http.auth.user=elastic",
        "--conf", "spark.es.net.http.auth.pass=changeme",
        "--conf", "spark.es.write.operation=upsert",
        "--conf", "spark.default.parallelism=4",
        "--conf", s"spark.executor.extraJavaOptions=$g1gcOpts $gcPrintFlags -XX:+UseCompressedOops -Dlog4j.configuration=file:/spark-log4j-config/log4j-executor.properties",
        "--conf", "spark.serializer=org.apache.spark.serializer.KryoSerializer",
        "--conf", s"spark.driver.extraJavaOptions=$g1gcOpts $gcPrintFlags -XX:+UseCompressedOops ") ++
      sparkPackages ++
      Seq("--jars",sparkExtraJars) ++
      Seq(jarName) ++
      args.split(" ").toSeq.asInstanceOf[Seq[String]]      
    sparkCommand  
  }
  def execute()
}


class ContentMatchJob extends SparkSubmitJob {
  private val log = LoggerFactory.getLogger("ContentMatchJob")
  
  override def execute() {
    mainClass = "com.slingmedia.sportscloud.offline.batch.impl.ContentMatcher"
    name = "SportsCloudContentMatch"
    log.trace("Executing task : SparkSubmitJob")
    val sparkSumbitCommand = buildSparkCommand(name,mainClass,sparkExtraJars,sportsCloudBatchJarLoc,"content_match game_schedule")
    log.trace(s"Executing command $sparkSumbitCommand")
    sparkSumbitCommand #>> new File("/var/log/sports-cloud-schedulers/sc-batch-job.log") !

	}
}

class TeamStandingsMetaDataBatchJob extends SparkSubmitJob {
  private val log = LoggerFactory.getLogger("TeamStandingsMetaDataBatchJob")
  
  override def execute() {
    mainClass = "com.slingmedia.sportscloud.offline.batch.impl.MetaDataMuncher"
    name = "TeamStandingsMetaDataMuncher"
    log.trace("Executing task : TeamStandingsMetaDataBatchJob")
   
    val sparkSumbitCommand = buildSparkCommand(name,mainClass,sparkExtraJars,sportsCloudBatchJarLoc,"TEAMSTANDINGS meta_batch team_standings")
    log.trace(s"Executing command $sparkSumbitCommand")
    sparkSumbitCommand #>> new File("/var/log/sports-cloud-schedulers/sc-batch-job.log") !

	}
}

class PlayerStatsMetaDataBatchJob extends SparkSubmitJob {
  private val log = LoggerFactory.getLogger("PlayerStatsMetaDataBatchJob")
  
  override def execute() {
    mainClass = "com.slingmedia.sportscloud.offline.batch.impl.MetaDataMuncher"
    name = "PlayerStatsDataMuncher"
    log.trace("Executing task : PlayerStatsMetaDataBatchJob")
   
    val sparkSumbitCommand = buildSparkCommand(name,mainClass,sparkExtraJars,sportsCloudBatchJarLoc,"PLAYERSTATS meta_batch player_stats")
    log.trace(s"Executing command $sparkSumbitCommand")
    sparkSumbitCommand #>> new File("/var/log/sports-cloud-schedulers/sc-batch-job.log") !

	}
}

class BatchScoreJob extends SparkSubmitJob {
  private val log = LoggerFactory.getLogger("BatchScoreJob")
  
  override def execute() {
    mainClass = "com.slingmedia.sportscloud.offline.batch.impl.MetaDataMuncher"
    name = "BatchScoreJob"
    log.trace("Executing task : BatchScoreJob")
   
    val sparkSumbitCommand = buildSparkCommand(name,mainClass,sparkExtraJars,sportsCloudBatchJarLoc,"LIVEINFO live_info live_info")
    log.trace(s"Executing command $sparkSumbitCommand")
    sparkSumbitCommand #>> new File("/var/log/sports-cloud-schedulers/sc-stream-job.log") !

	}
}

class LiveStreamJob extends SparkSubmitJob {
  private val log = LoggerFactory.getLogger("LiveStreamJob")
  
  override def execute() {
    //This needs to be a long running job and hence for reliability we use a shell script
   Seq( "/project/sports-cloud-schedulers/src/main/resources/scripts/allenv/launch_live_info_streaming.sh" ,
       "14",
       "23",
       "0",
       "6",
       "/var/log/sports-cloud-streaming-jobs/sc-live-stream-job.log") !
       
	}
}


object DownloadSchedulesJob  {
  def main(args: Array[String]) {
    new DownloadSchedulesJob().execute()
  }
}


class DownloadSchedulesJob {
  private val log = LoggerFactory.getLogger("DownloadSchedulesJob")

	 def execute() {
    log.trace("Executing task : DownloadSchedulesJob")
		
		s"curl http://artifact-server.marathon.l4lb.thisdcos.directory:9082/artifacts/slingtv/sports-cloud/summary.json" #> new File("/tmp/summary.json") ! ;

		"cat /dev/null" #> new File("/tmp/schedules_plus_3") ! ;
		getFileContents("/tmp/summary.json").foreach( it => {
		     val json = it  
		     // There is a need for switching configuration and its done here
		     val conf :Configuration= Configuration.defaultConfiguration();
         Configuration.setDefaults(new Configuration.Defaults() {
          		val  jsonProviderObj:JsonProvider = new JsonSmartJsonProvider();
          		val  mappingProviderObj:MappingProvider = new JsonSmartMappingProvider();
            
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
				val ctx:ReadContext = JsonPath.using(conf).parse(it)
				val  sportsGenreFilter:Filter = Filter.filter(Criteria.where("metadata.genre").contains("Sports"))
				val filteredChannels:String = ctx.read("$.channels[?]", sportsGenreFilter).toString
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
				val jsonArray:JsonArray = JsonPath.read[com.google.gson.JsonArray](filteredChannels,"$.[*].channel_guid")
        var randomNum = scala.util.Random
        var higher = 300
        var lower = 1

				val  iterator:Iterator[JsonElement] = jsonArray.iterator.asScala
				while(iterator.hasNext) {
				  val channel_guid=iterator.next.getAsString
					if(channel_guid!=null) { 
						for( i <- 0 to 5){
							val epochTimeOffset = Instant.now().plus(i, ChronoUnit.DAYS);      				  
							val utc = epochTimeOffset.atZone(ZoneId.of("Z"));        				  
							def pattern = "yyyyMMdd";
							val formattedDate = utc.format(DateTimeFormatter.ofPattern(pattern)); 
							val cmsHost = System.getenv("cmsHost")
							val fullUrl = s"http://$cmsHost.cdn.cms.movetv.com/cms/api/linear_feed/channels/v1/$channel_guid/" + formattedDate
							log.trace(s"Full url is $fullUrl")
							Thread sleep  randomNum.nextInt(higher - lower) + lower
							(s"curl $fullUrl" #>> new File("/tmp/schedules_plus_3")).!
							(s"echo "  #>> new File("/tmp/schedules_plus_3")).! ;
						}
					}
				}
				"curl --upload-file /tmp/schedules_plus_3 http://artifact-server.marathon.l4lb.thisdcos.directory:9082/artifacts/slingtv/sports-cloud/schedules_plus_3" !


		})

	}

	val getFileContents:(String=>List[String]) = (fileName:String ) => {
		val bufferedSource = Source.fromFile(fileName)
		val lines = bufferedSource.getLines.toList
		bufferedSource.close
		lines
	}

}

object DownloadThuuzJob  {
  def main(args: Array[String]) {
    new DownloadThuuzJob().execute()
  }
}

class DownloadThuuzJob  {
  private val log = LoggerFactory.getLogger("ThuuzJob")
	def execute() {
    log.trace("Executing task : ThuuzJob")	  
		"curl http://api.thuuz.com/2.2/games?auth_code=6adf97f8142118ba&type=normal&status=5&days=5&sport_leagues=baseball.mlb,basketball.nba,basketball.ncaa,football.nfl,football.ncaa,hockey.nhl,golf.pga,soccer.mwc,soccer.chlg,soccer.epl,soccer.seri,soccer.liga,soccer.bund,soccer.fran,soccer.mls,soccer.wwc,soccer.ligamx,soccer.ered,soccer.ch-uefa2,soccer.eng2,soccer.prt1,soccer.sco1,soccer.tur1,soccer.rus1,soccer.bel1,soccer.euro&limit=999" #> new File("/tmp/thuuz.json") ! ;
		"curl --upload-file /tmp/thuuz.json http://artifact-server.marathon.l4lb.thisdcos.directory:9082/artifacts/slingtv/sports-cloud/thuuz.json" !
  }
}


object DownloadSummaryJob  {
  def main(args: Array[String]) {
    new DownloadSummaryJob().execute()
  }
}
  
class  DownloadSummaryJob {
	private val log = LoggerFactory.getLogger("DownloadSummaryJob")
  def execute() {
    log.trace("Executing task : DownloadSummaryJob")
		val cmsHost =  System.getenv("cmsHost")
		val cmsSummaryUrl = System.getenv("cmsSummaryUrl")
		s"curl http://$cmsHost.cdn.cms.movetv.com/$cmsSummaryUrl" #> new File("/tmp/summary.json") ! ;
		"curl --upload-file /tmp/summary.json http://artifact-server.marathon.l4lb.thisdcos.directory:9082/artifacts/slingtv/sports-cloud/summary.json" !
	}

}

