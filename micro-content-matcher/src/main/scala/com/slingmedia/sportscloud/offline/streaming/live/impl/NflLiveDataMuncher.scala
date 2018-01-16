package com.slingmedia.sportscloud.offline.streaming.live.impl

import org.slf4j.LoggerFactory
import org.apache.spark.sql.functions.{ udf,lit,concat,md5 }
import scala.util.{ Try, Success, Failure }
import org.apache.spark.sql.types.{ StructType,StructField,IntegerType,ArrayType,DoubleType, StringType }
import org.apache.spark.sql.{ SparkSession, DataFrame }
import com.slingmedia.sportscloud.offline.streaming.live.LiveDataMuncher

object NLDHolder extends Serializable {
  val serialVersionUID = 1L;
  @transient lazy val log = LoggerFactory.getLogger("LiveDataMuncher")
}

object NflLiveDataMuncher extends Serializable {
  def main(args: Array[String]) {
    NLDHolder.log.debug("Args is $args")
    new NflLiveDataMuncher().stream(args(0), args(1))
  }

}

class NflLiveDataMuncher extends Serializable with LiveDataMuncher {

  //get game seconds
  val getGameSeconds: (Int, Int) => Int = (sourceSeconds: Int, gameStartSeconds: Int) => {
    (sourceSeconds - gameStartSeconds)
  }

  val getGameSecondsUDF = udf(getGameSeconds(_: Int, _: Int))


  override def getSchema():StructType = {
    var finalSchema = commonStructFields()
    finalSchema += StructField("division", StringType, true)
    finalSchema += StructField("homeTeamlineScore", ArrayType(IntegerType), true)
    finalSchema += StructField("awayTeamlineScore", ArrayType(IntegerType), true)
    finalSchema += StructField("period", StringType, true)
    finalSchema += StructField("position", DoubleType, true)
    finalSchema += StructField("timer", StringType, true)
    finalSchema += StructField("playType", StringType, true)
    finalSchema += StructField("drives", ArrayType(StringType), true)
    finalSchema += StructField("gameTimeSeconds", IntegerType, true)
    finalSchema += StructField("inningNo", IntegerType, true)
    finalSchema += StructField("homeScore", IntegerType, true)
    finalSchema += StructField("awayScore", IntegerType, true)
    StructType(finalSchema.toList)
  }
  
  override def addLeagueSpecificData(df: DataFrame): DataFrame = {
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._
    val df2 = df.withColumn("seconds", getGameSecondsUDF($"srcTimeEpoch",$"game_date_epoch"))
    df2
  }


  override def showSelected(df: DataFrame): Unit = {
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._
    df.select($"gameId",
      $"gameCode",
      $"statusId",
      $"drives",
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
          $"gameId", concat($"gameId", lit("_"), $"inningNo", lit("-"), md5($"lastPlay")).alias("id"),
          $"period",
          $"timer",
          $"position",
          $"playType",
          $"gameTimeSeconds",
          $"seconds").
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

