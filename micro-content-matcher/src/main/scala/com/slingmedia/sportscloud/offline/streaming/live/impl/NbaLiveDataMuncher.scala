package com.slingmedia.sportscloud.offline.streaming.live.impl

import org.slf4j.LoggerFactory
import org.apache.spark.sql.functions.{ udf,lit,concat,md5 }
import scala.util.{ Try, Success, Failure }
import org.apache.spark.sql.types.{ StructType,StructField,IntegerType,ArrayType,DoubleType, StringType }
import org.apache.spark.sql.{ SparkSession, DataFrame }
import com.slingmedia.sportscloud.offline.streaming.live.LiveDataMuncher

object NBLDHolder extends Serializable {
  val serialVersionUID = 1L;
  @transient lazy val log = LoggerFactory.getLogger("LiveDataMuncher")
}

object NbaLiveDataMuncher extends Serializable {
  def main(args: Array[String]) {
    NBLDHolder.log.debug("Args is $args")
    new NbaLiveDataMuncher().stream(args(0), args(1))
  }

}

class NbaLiveDataMuncher extends Serializable with LiveDataMuncher {


  override def getSchema():StructType = {

    StructType(
        StructField("srcMonth",StringType,true)
        :: StructField("srcDate",StringType,true)
        :: StructField("srcDay",StringType,true)
        :: StructField("srcYear",StringType,true)
        :: StructField("srcHour",StringType,true)
        :: StructField("srcMinute",StringType,true)
        :: StructField("srcSecond",StringType,true)
        :: StructField("srcUtcHour",StringType,true)
        :: StructField("srcUtcMinute",StringType,true)
        :: StructField("month",StringType,true)
        :: StructField("date",StringType,true)
        :: StructField("day",StringType,true)
        :: StructField("year",StringType,true)
        :: StructField("hour",StringType,true)
        :: StructField("minute",StringType,true)
        :: StructField("utcHour",StringType,true)
        :: StructField("utcMinute",StringType,true)
        :: StructField("homeTeamExtId",StringType,true)
        :: StructField("homeTeamAlias",StringType,true)
        :: StructField("homeTeamName",StringType,true)
        :: StructField("awayTeamAlias",StringType,true)
        :: StructField("awayTeamExtId",StringType,true)
        :: StructField("awayTeamName",StringType,true)
        :: StructField("gameId",StringType,true)
        :: StructField("gameCode",StringType,true)
        :: StructField("gameType",StringType,true)
        :: StructField("status",StringType,true)
        :: StructField("statusId",IntegerType,true)
        :: StructField("league",StringType,true)
        :: StructField("lastPlay",StringType,true)
        :: StructField("homeTeamlineScore", ArrayType(IntegerType), true)
        :: StructField("awayTeamlineScore", ArrayType(IntegerType), true)
        :: StructField("homeScore", IntegerType, true)
        :: StructField("awayScore", IntegerType, true)
        :: Nil)
  }

  override def addLeagueSpecificData(df: DataFrame): Unit = {
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._
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
    import spark.implicits._
    val indexResult = Try(indexResults("sc-live-info", "live_info",  df))

    indexResult match {
      case Success(data) =>
        NLDHolder.log.info(data.toString)
        val kafkaLiveInfoT10DF2 = df.select($"lastPlay",
          $"batchTime",
          $"srcTimeEpoch".alias("srcTime"),
          $"homeTeamExtId",
          $"awayTeamExtId",
          $"gameId",
          concat($"gameId", lit("_"),
            md5($"lastPlay")).alias("id")).
          withColumn("img",
            concat(lit("http://gwserv-mobileprod.echodata.tv/Gamefinder/logos/LARGE/gid"),
              $"homeTeamExtId",
              lit(".png"))).
          withColumn("teamId", $"homeTeamExtId").
          drop("homeTeamExtId", "awayTeamExtId")
        val kafkaLiveInfoT11DF3 = kafkaLiveInfoT10DF2.filter("lastPlay != ''")

        indexResults( "sc-scoring-events", "scoring_events", kafkaLiveInfoT11DF3)

      case Failure(e) =>
        NLDHolder.log.error("Error occurred in live_info indexing ", e)
    }
  }




}

