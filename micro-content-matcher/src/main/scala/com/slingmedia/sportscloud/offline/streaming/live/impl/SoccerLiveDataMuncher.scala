package com.slingmedia.sportscloud.offline.streaming.live.impl

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZonedDateTime}

import org.slf4j.LoggerFactory
import org.apache.spark.sql.functions._

import scala.util.{Failure, Success, Try}
import org.apache.spark.sql.types._
import org.apache.spark.sql.{DataFrame, SparkSession}
import com.slingmedia.sportscloud.offline.streaming.live.{LDMHolder, LiveDataMuncher}

import scala.collection.mutable.ListBuffer

object SoccerHolder extends Serializable {
  val serialVersionUID = 1L;
  @transient lazy val log = LoggerFactory.getLogger("Scocer Live")
}

object SoccerLiveDataMuncher extends Serializable {
  def main(args: Array[String]) {
    SoccerHolder.log.debug("Args is $args")
    //new SoccerLiveDataMuncher().munch(args(0), args(1))
    new SoccerLiveDataMuncher().stream(args(0), args(1))

  }

}

class SoccerLiveDataMuncher extends Serializable with LiveDataMuncher  {

  override val getReorderedStatusId: (Int => Int) = (statusId: Int) => {
    if (statusId == 10)  {
      4
    }  else if (statusId == 40) {
      2
    } else if (statusId == 80) {
      1
    }
    else {
      statusId
    }
  }
  def nagraUtcStrToEpochFunc(utcStr: String): Long = {
    val zdt = ZonedDateTime.parse(utcStr,DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss Z"));
    val epoch = zdt.toEpochSecond()
    epoch
  }

  val nagraUtcStrToEpochUDF = udf(nagraUtcStrToEpochFunc(_: String))
  //override val getReorderedStatusIdUDF = udf(getReorderedStatusId(_: Int))

  override def getSchema():StructType = {
    val feilds = ListBuffer(
      StructField("gameId",StringType,true) ,
      StructField("gameCode",StringType,true) ,
      StructField("gameStatus",StringType,true) ,
      StructField("statusId",IntegerType,true) ,
      StructField("scheduledDate",StringType,true) ,
      StructField("awayTeamlineScore", ArrayType(IntegerType), true),
      StructField("homeTeamlineScore", ArrayType(IntegerType), true),
      StructField("homeScore", IntegerType, true),
      StructField("awayScore", IntegerType, true)
    )
    StructType(feilds.toList)
  }


  override val mergeLiveInfo: (DataFrame) => Unit = (kafkaLiveInfoT1DF1: DataFrame) => {
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._
    val batchTimeStamp = Instant.now().getEpochSecond

    val liveInfoSchema = getSchema()

    //val kafkaLiveInfoT2DF1 = kafkaLiveInfoT1DF1.
      //where("fileName.payload like '%BOXSCORE%' OR fileName.payload like '%FINALBOX%' OR fileName.payload like '%_LIVE%' ")

    val kafkaLiveInfoT3DF1 = kafkaLiveInfoT1DF1.
      select(from_json($"payloadStruct.payload", liveInfoSchema) as "liveInfoStruct")
    val kafkaLiveInfoT3DF2 = kafkaLiveInfoT3DF1.
      select(children("liveInfoStruct", kafkaLiveInfoT3DF1): _*)
    val kafkaLiveInfoT4DF2 = kafkaLiveInfoT3DF2.filter(col("gameId").isNotNull).
      withColumn("rStatusId", getReorderedStatusIdUDF($"statusId")).
      withColumn("statusId", getReorderedStatusIdUDF($"statusId")).
      coalesce(4).
      withColumn("game_date_epoch",nagraUtcStrToEpochUDF(col("scheduledDate"))).
      withColumn("id", $"gameId").
      withColumn("batchTime", lit(batchTimeStamp))

    //val kafkaLiveInfoT4DF3 = addLeagueSpecificData(kafkaLiveInfoT4DF2)



    val kafkaLiveInfoT10DF3 = kafkaLiveInfoT4DF2.
      orderBy($"gameId", $"rStatusId").
      repartition($"gameId").
      coalesce(4)


    showSelected(kafkaLiveInfoT10DF3)

    Try(createScoringEventsAndIndex(kafkaLiveInfoT4DF2))

  }

  override def showSelected(df: DataFrame): Unit = {
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._
    df.select($"gameId",
      $"gameCode",
      $"statusId",
      $"awayTeamlineScore",
      $"homeTeamlineScore").
      show(false)

  }

  override def createScoringEventsAndIndex(df: DataFrame): Unit = {
    val spark = SparkSession.builder().getOrCreate()
    val indexResult = Try(indexResults("sc-live-info", "live_info",  df))

    indexResult match {
      case Success(data) =>
        SoccerHolder.log.info(data.toString)
      case Failure(e) =>
        SoccerHolder.log.error("Error occurred in live_info indexing ", e)
    }
  }


}

