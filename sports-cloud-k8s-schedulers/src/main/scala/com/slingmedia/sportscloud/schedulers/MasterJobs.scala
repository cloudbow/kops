package com.slingmedia.sportscloud.schedulers

import java.io.{File,PrintWriter}
import java.util.{TimeZone,EnumSet}
import java.time.{ ZonedDateTime , OffsetDateTime , ZoneOffset , Instant, ZoneId}
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

import sys.process._
import java.net.URL
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
    val league = args(0)
    Seq(s"/deploy-scheduled-jobs/scripts/kafka/connect/launch_kafka_connect_jobs.sh",
      s"content_match_$league",
      "5",
      "15",
      "unused",
      "36000000",
      "/project/sports-cloud-parsers/src/main/resources/kafka-standalone/cs-content-match.properties",
      s"/deploy-scheduled-jobs/scripts/kafka/connect/worker-config/$league/ftp-content-match-$league.json",
      s"/var/log/sports-cloud-kafka-jobs/cs-content-match-kafka-connect-$league.log",
      s"ftp-content-match-$league") !
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
     val league = args(0)
     Seq("/deploy-scheduled-jobs/scripts/kafka/connect/launch_kafka_connect_jobs.sh",
       s"meta_batch_$league",
       "5",
       "15",
       "unused",
       "36000000",
       "/project/sports-cloud-parsers/src/main/resources/kafka-standalone/cs-meta-batch.properties",
       s"/deploy-scheduled-jobs/scripts/kafka/connect/worker-config/$league/ftp-meta-batch-$league.json",
       s"/var/log/sports-cloud-kafka-jobs/cs-meta-batch-kafka-connect-$league.log",
       s"ftp-meta-batch-$league") !

    
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
    val league = args(0)
    log.trace("Executing task: KafkaConnectLiveInfoJob")
    Seq("/deploy-scheduled-jobs/scripts/kafka/connect/launch_kafka_connect_jobs.sh",
      s"live_info_$league",
      "0",
      "0",
      "unused",
      "1800000",
      "/project/sports-cloud-parsers/src/main/resources/kafka-standalone/cs-live-info.properties",
      s"/deploy-scheduled-jobs/scripts/kafka/connect/worker-config/$league/ftp-live-info-$league.json",
      s"/var/log/sports-cloud-kafka-jobs/cs-live-info-kafka-connect-$league.log",
      s"ftp-live-info-$league") !

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
		val returnCode = s"curl -o /tmp/summary.json http://$artifactServer/artifacts/slingtv/sports-cloud/summary.json" ! ;
     log.trace(s"Return value of download is $returnCode")

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
        var higher = 1000
        var lower = 500

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
							val returnCode = (s"curl --retry 8  $fullUrl" #>> new File("/tmp/schedules_plus_3")).!
              log.trace(s"Return value of schedule download is $returnCode")
              (s"echo "  #>> new File("/tmp/schedules_plus_3")).! ;
						}
					}
				}
				val returnCode2 = s"curl --upload-file /tmp/schedules_plus_3 http://$artifactServer/artifacts/slingtv/sports-cloud/schedules_plus_3" ! ;
        log.trace(s"Return value of upload is $returnCode2")


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
		val returnCode = "curl --retry 8 -o /tmp/thuuz.json http://api.thuuz.com/2.2/games?auth_code=6adf97f8142118ba&type=normal&status=5&days=5&sport_leagues=baseball.mlb,basketball.nba,basketball.ncaa,football.nfl,football.ncaa,hockey.nhl,golf.pga,soccer.mwc,soccer.chlg,soccer.epl,soccer.seri,soccer.liga,soccer.bund,soccer.fran,soccer.mls,soccer.wwc,soccer.ligamx,soccer.ered,soccer.ch-uefa2,soccer.eng2,soccer.prt1,soccer.sco1,soccer.tur1,soccer.rus1,soccer.bel1,soccer.euro&limit=999" ! ;
    log.trace(s"Return value of download is $returnCode")
    val returnCode2 = s"curl --upload-file /tmp/thuuz.json http://$artifactServer/artifacts/slingtv/sports-cloud/thuuz.json" ! ;
    log.trace(s"Return value of upload is $returnCode2")
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
		val returnCode = s"curl --retry 8 -o /tmp/summary.json http://$cmsHost.cdn.cms.movetv.com/$cmsSummaryUrl" ! ;
    log.trace(s"Return value of download is $returnCode")
		val returnCode2 = s"curl --upload-file /tmp/summary.json http://$artifactServer/artifacts/slingtv/sports-cloud/summary.json" ! ;
    log.trace(s"Return value of upload is $returnCode2")

  }

}

