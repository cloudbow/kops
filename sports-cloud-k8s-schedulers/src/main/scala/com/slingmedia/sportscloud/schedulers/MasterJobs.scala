package com.slingmedia.sportscloud.schedulers

import java.io.{File,PrintWriter}
import java.util.{TimeZone,EnumSet}
import java.time.{ ZonedDateTime , OffsetDateTime , ZoneOffset , Instant, ZoneId}
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

import sys.process._
import scala.io.Source
import scala.util.Properties
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



object DownloadSchedulesJob  {
  def main(args: Array[String]) {
    new DownloadSchedulesJob().execute()
  }
}


class DownloadSchedulesJob {
  private val log = LoggerFactory.getLogger("DownloadSchedulesJob")

	 def execute() {
    log.trace("Executing task : DownloadSchedulesJob")
		val artifactServer = System.getenv("ARTIFACT_SERVER_EP")
		s"curl http://$artifactServer/artifacts/slingtv/summary.json" #> new File("/tmp/summary.json") ! ;

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
				s"curl --upload-file /tmp/schedules_plus_3 http://$artifactServer/artifacts/slingtv/sports-cloud/schedules_plus_3" !


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
    val artifactServer = System.getenv("ARTIFACT_SERVER_EP")
    log.trace("Executing task : ThuuzJob")	  
		"curl http://api.thuuz.com/2.2/games?auth_code=6adf97f8142118ba&type=normal&status=5&days=5&sport_leagues=baseball.mlb,basketball.nba,basketball.ncaa,football.nfl,football.ncaa,hockey.nhl,golf.pga,soccer.mwc,soccer.chlg,soccer.epl,soccer.seri,soccer.liga,soccer.bund,soccer.fran,soccer.mls,soccer.wwc,soccer.ligamx,soccer.ered,soccer.ch-uefa2,soccer.eng2,soccer.prt1,soccer.sco1,soccer.tur1,soccer.rus1,soccer.bel1,soccer.euro&limit=999" #> new File("/tmp/thuuz.json") ! ;
		s"curl --upload-file /tmp/thuuz.json http://$artifactServer/artifacts/slingtv/sports-cloud/thuuz.json" !
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
    val artifactServer = System.getenv("ARTIFACT_SERVER_EP")
		s"curl http://$cmsHost.cdn.cms.movetv.com/$cmsSummaryUrl" #> new File("/tmp/summary.json") ! ;
		s"curl --upload-file /tmp/summary.json http://$artifactServer/artifacts/slingtv/sports-cloud/summary.json" !
	}

}

