package com.slingmedia.sportscloud.offline.streaming.impl

import org.slf4j.LoggerFactory
import org.apache.spark.sql.functions.{ udf,lit,concat,md5,col }
import scala.util.{ Try, Success, Failure }
import org.apache.spark.sql.types.{ StructType,StructField,IntegerType,ArrayType,DoubleType, StringType }
import org.apache.spark.sql.{ SparkSession, DataFrame }
import com.slingmedia.sportscloud.offline.streaming.live.LiveDataMuncher


object MLDHolder extends Serializable {
  val serialVersionUID = 1L;
  @transient lazy val log = LoggerFactory.getLogger("LiveDataMuncher")
}

case class Pitcher(isHomePitching: Boolean, hTCurrPlayer: String, aTCurrPlayer: String)


object MlbLiveDataMuncher extends Serializable {
  def main(args: Array[String]) {
    MLDHolder.log.debug("Args is $args")
    new MlbLiveDataMuncher().stream(args(0), args(1))
  }

}

class MlbLiveDataMuncher extends Serializable with LiveDataMuncher {

  //All Udfs starts here
  val getFieldsCount: (Int, Int, Int) => String = (balls: Int, strikes: Int, outs: Int) => {
    var fieldCountTxt = ""
    if (balls != -1 && strikes != -1 && outs != -1) {
      fieldCountTxt = balls + "-" + strikes + ", " + outs + (if (outs == 1) " out" else " outs");
    }
    fieldCountTxt
  }

  val getFieldsCountUDF = udf(getFieldsCount(_: Int, _: Int, _: Int))

  val getFieldState: (String, String, String) => Int = (firstGameBase: String, secondGameBase: String, thirdGameBase: String) => {
    var map: Int = 0
    if (!"".equals(firstGameBase)) {
      map = map | 1
    } else if (!"".equals(secondGameBase)) {
      map = map | 2
    } else if (!"".equals(thirdGameBase)) {
      map = map | 4
    }
    map
  }

  val getFieldStateUDF = udf(getFieldState(_: String, _: String, _: String))

  val getTeamId: (String, String, String) => String = (inningTitle: String, htId: String, atId: String) => {
    var teamId = "0"
    if (inningTitle != null) {
      if (inningTitle.toLowerCase.startsWith("bottom")) {
        teamId = htId
      } else {
        teamId = atId
      }
    }
    teamId
  }

  val getTeamIdUDF = udf(getTeamId(_: String, _: String, _: String))

  val getPitchingDetails: (String, String, String, String) => Pitcher = (sd: String, curBtr: String, htCurrPName: String, atCurrPName: String) => {
    val isHomePitching = if (sd.equals("Top")) true else false
    var awayCurrPlayer = "-"
    var homeCurrPlayer = "-"
    if (curBtr != null) {
      if (isHomePitching) {
        awayCurrPlayer = curBtr
        homeCurrPlayer = htCurrPName
      } else {
        awayCurrPlayer = atCurrPName
        homeCurrPlayer = curBtr
      }
    }
    Pitcher(isHomePitching, homeCurrPlayer, awayCurrPlayer)
  }

  val getPitchingDetailsUDF = udf(getPitchingDetails(_: String, _: String, _: String, _: String))

  //All udfs ends here


  override def getSchema():StructType = {

    StructType(
      StructField("srcMonth", StringType, true)
        :: StructField("srcDate", StringType, true)
        :: StructField("srcDay", StringType, true)
        :: StructField("srcYear", StringType, true)
        :: StructField("srcHour", StringType, true)
        :: StructField("srcMinute", StringType, true)
        :: StructField("srcSecond", StringType, true)
        :: StructField("srcUtcHour", StringType, true)
        :: StructField("srcUtcMinute", StringType, true)
        :: StructField("month", StringType, true)
        :: StructField("date", StringType, true)
        :: StructField("day", StringType, true)
        :: StructField("year", StringType, true)
        :: StructField("hour", StringType, true)
        :: StructField("minute", StringType, true)
        :: StructField("utcHour", StringType, true)
        :: StructField("utcMinute", StringType, true)
        :: StructField("status", StringType, true)
        :: StructField("statusId", IntegerType, true)
        :: StructField("gameType", StringType, true)
        :: StructField("league", StringType, true)
        :: StructField("gameId", StringType, true)
        :: StructField("gameCode", StringType, true)
        :: StructField("lastPlay", StringType, true)
        :: StructField("homeTeamName", StringType, true)
        :: StructField("homeTeamAlias", StringType, true)
        :: StructField("homeTeamExtId", StringType, true)
        :: StructField("awayTeamName", StringType, true)
        :: StructField("awayTeamAlias", StringType, true)
        :: StructField("awayTeamExtId", StringType, true)
        :: StructField("firstGameBase", StringType, true)
        :: StructField("secondGameBase", StringType, true)
        :: StructField("thirdGameBase", StringType, true)
        :: StructField("balls", IntegerType, true)
        :: StructField("strikes", IntegerType, true)
        :: StructField("outs", IntegerType, true)
        :: StructField("segmentDiv", StringType, true)
        :: StructField("currBtrName", StringType, true)
        :: StructField("hTCurrPitcherName", StringType, true)
        :: StructField("aTCurrPitcherName", StringType, true)
        :: StructField("inningTitle", StringType, true)
        :: StructField("inningNo", StringType, true)
        :: StructField("homeScoreRuns", IntegerType, true)
        :: StructField("homeScoreHits", IntegerType, true)
        :: StructField("homeScoreErrors", IntegerType, true)
        :: StructField("homeTeamInnings", ArrayType(IntegerType), true)
        :: StructField("awayScoreRuns", IntegerType, true)
        :: StructField("awayScoreHits", IntegerType, true)
        :: StructField("awayScoreErrors", IntegerType, true)
        :: StructField("awayTeamInnings", ArrayType(IntegerType), true)
        :: Nil)
  }

  override def addLeagueSpecificData(df: DataFrame): Unit = {
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._
    val kafkaLiveInfoT5DF2 = df.
      withColumn("fieldCountsTxt", getFieldsCountUDF($"balls", $"strikes", $"outs")).
      withColumn("fieldState",
        getFieldStateUDF($"firstGameBase",$"secondGameBase",$"thirdGameBase"))
    val kafkaLiveInfoT9DF2 = kafkaLiveInfoT5DF2.
      withColumn("playerData",
        getPitchingDetailsUDF($"segmentDiv",
          $"currBtrName",
          $"hTCurrPitcherName",
          $"aTCurrPitcherName"))
    val allCols = kafkaLiveInfoT5DF2.columns.map { it => col(it) } ++
      children("playerData", kafkaLiveInfoT9DF2)
    val kafkaLiveInfoT9DF3 = kafkaLiveInfoT9DF2.
      select(allCols.toSeq: _*).
      drop("playerData")
    val kafkaLiveInfoT10DF3 = kafkaLiveInfoT9DF3.
      orderBy($"gameId", $"rStatusId", $"srcTimeEpoch").
      repartition($"gameId").
      coalesce(4)
  }


  override def showSelected(df: DataFrame): Unit = {
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._
    df.select($"gameId",
      $"gameCode",
      $"statusId",
      $"isHomePitching",
      $"hTCurrPlayer",
      $"aTCurrPlayer",
      $"srcTimeEpoch",
      $"awayTeamInnings",
      $"homeTeamInnings",
      $"inningTitle").
      show(false)

  }
  override def createScoringEventsAndIndex(df: DataFrame): Unit = {
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._
    val indexResult = Try(indexResults("sc-live-info", "live_info",  df))
    indexResult match {
      case Success(data) =>
        MLDHolder.log.info(data.toString)
        val kafkaLiveInfoT10DF2 = df.select($"lastPlay",
          $"batchTime",
          $"srcTimeEpoch".alias("srcTime"),
          $"homeTeamExtId",
          $"awayTeamExtId",
          $"gameId", concat($"gameId", lit("_"), $"inningNo", lit("-"), md5($"lastPlay"),
            $"inningTitle").alias("id")).
          withColumn("img",
            concat(lit("http://gwserv-mobileprod.echodata.tv/Gamefinder/logos/LARGE/gid"),
              lit(getTeamIdUDF($"inningTitle", $"homeTeamExtId", $"awayTeamExtId")),
              lit(".png"))).
          withColumn("teamId", getTeamIdUDF($"inningTitle", $"homeTeamExtId", $"awayTeamExtId")).
          drop("homeTeamExtId", "awayTeamExtId")
        val kafkaLiveInfoT11DF3 = kafkaLiveInfoT10DF2.filter("lastPlay != ''")
        indexResults( "sc-scoring-events", "scoring_events", kafkaLiveInfoT11DF3)

      case Failure(e) =>
        MLDHolder.log.error("Error occurred in live_info indexing ", e)
    }
  }




}

