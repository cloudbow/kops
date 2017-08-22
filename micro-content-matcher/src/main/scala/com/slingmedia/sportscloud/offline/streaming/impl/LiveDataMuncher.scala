package com.slingmedia.sportscloud.offline.streaming.impl

import com.slingmedia.sportscloud.offline.batch.Muncher

import org.slf4j.LoggerFactory

import org.apache.spark.sql.functions.{ md5, concat, lit, coalesce, max, min, udf, col, explode, from_json, collect_list, concat_ws }
import org.apache.spark.sql.types.{ StructType, StructField, StringType, IntegerType, LongType, FloatType, ArrayType };
import org.apache.spark.SparkContext
import org.apache.spark.streaming.{ StreamingContext, Seconds }
import org.apache.spark.sql.{ SparkSession, DataFrame, Row, Column }

import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.streaming.kafka010.{ HasOffsetRanges, CanCommitOffsets, KafkaUtils, LocationStrategies, ConsumerStrategies }

import scala.util.{ Try, Success, Failure }

import java.time.Instant
import org.apache.spark.sql.types.IntegerType

object Holder extends Serializable {
  val serialVersionUID = 1L;
  @transient lazy val log = LoggerFactory.getLogger("LiveDataMuncher")
}

case class Pitcher(isHomePitching: Boolean, hTCurrPlayer: String, aTCurrPlayer: String)

object LiveDataMuncher extends Serializable {
  def main(args: Array[String]) {
    Holder.log.debug("Args is $args")
    new LiveDataMuncher().stream(args(0), args(1), args(2))
  }

}

class LiveDataMuncher extends Serializable with Muncher {

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

  val getReorderedStatusId: (Int => Int) = (statusId: Int) => {
    if (statusId == 23) 2 else statusId
  }
  val getReorderedStatusIdUDF = udf(getReorderedStatusId(_: Int))
  //All udfs ends here

  val mergeLiveInfo: (String, DataFrame) => Unit = (zkHost: String, kafkaLiveInfoT1DF1: DataFrame) => {
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._
    val batchTimeStamp = Instant.now().getEpochSecond

    val liveInfoSchema = StructType(
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
        :: StructField("division", StringType, true)
        :: StructField("gameId", StringType, true)
        :: StructField("gameCode", StringType, true)
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
        :: StructField("lastPlay", StringType, true)
        :: StructField("inningTitle", StringType, true)
        :: StructField("inningNo", StringType, true)
        :: StructField("homeTeamName", StringType, true)
        :: StructField("homeTeamAlias", StringType, true)
        :: StructField("homeTeamExtId", StringType, true)
        :: StructField("homeScoreRuns", IntegerType, true)
        :: StructField("homeScoreHits", IntegerType, true)
        :: StructField("homeScoreErrors", IntegerType, true)
        :: StructField("homeTeamInnings", ArrayType(IntegerType), true)
        :: StructField("awayTeamName", StringType, true)
        :: StructField("awayTeamAlias", StringType, true)
        :: StructField("awayTeamExtId", StringType, true)
        :: StructField("awayScoreRuns", IntegerType, true)
        :: StructField("awayScoreHits", IntegerType, true)
        :: StructField("awayScoreErrors", IntegerType, true)
        :: StructField("awayTeamInnings", ArrayType(IntegerType), true)
        :: Nil)

    val kafkaLiveInfoT2DF1 = kafkaLiveInfoT1DF1.where("fileName.payload like '%BOXSCORE%' OR fileName.payload like '%FINALBOX%' OR fileName.payload like '%_LIVE%' ")

    val kafkaLiveInfoT3DF1 = kafkaLiveInfoT2DF1.select(from_json($"payloadStruct.payload", liveInfoSchema) as "liveInfoStruct")
    val kafkaLiveInfoT3DF2 = kafkaLiveInfoT3DF1.select(children("liveInfoStruct", kafkaLiveInfoT3DF1): _*)
    val kafkaLiveInfoT4DF2 = kafkaLiveInfoT3DF2.withColumn("srcTimeEpoch", timeStrToEpochUDF(concat(col("srcYear"), lit("-"), lit(getZeroPaddedUDF($"srcMonth")), lit("-"), lit(getZeroPaddedUDF($"srcDate")), lit("T"), lit(getZeroPaddedUDF($"srcHour")), lit(":"), col("srcMinute"), lit(":"), col("srcSecond"), lit(".00"), lit(getZeroPaddedUDF($"srcUtcHour")), lit(":"), col("srcUtcMinute"))))
    // filte only data with non null gameId
    val kafkaLiveInfoT5DF1 = kafkaLiveInfoT4DF2.filter(col("gameId").isNotNull)
    //reorder statusId so that the ordering is right
    val kafkaLiveInfoT5DF2 = kafkaLiveInfoT5DF1.withColumn("rStatusId", getReorderedStatusIdUDF($"statusId"))
    //order by new statusId 
    //Repartition by gameId as we can update solr paralley for each game
    //Order it so that the order is updated
    val kafkaLiveInfoT6DF2 = kafkaLiveInfoT5DF2.repartition($"gameId").coalesce(3).orderBy($"gameId", $"rStatusId", $"srcTimeEpoch")
    val kafkaLiveInfoT7DF2 = kafkaLiveInfoT6DF2.withColumn("id", $"gameId").
      withColumn("fieldCountsTxt", getFieldsCountUDF($"balls", $"strikes", $"outs")).
      withColumn("fieldState", getFieldStateUDF($"firstGameBase", $"secondGameBase", $"thirdGameBase")).
      withColumn("date", concat(col("year"), lit("-"), lit(getZeroPaddedUDF($"month")), lit("-"), lit(getZeroPaddedUDF($"date")), lit("T"), col("hour"), lit(":"), col("minute"), lit(":00.00"), lit(getZeroPaddedUDF($"utcHour")), lit(":"), col("utcMinute")))

    val kafkaLiveInfoT8DF2 = kafkaLiveInfoT7DF2.
      withColumn("batchTime", lit(batchTimeStamp)).
      withColumn("game_date_epoch", timeStrToEpochUDF($"date")).
      withColumn("gameDate", timeEpochtoStrUDF($"game_date_epoch"))
    val kafkaLiveInfoT9DF2 = kafkaLiveInfoT8DF2.withColumn("playerData", getPitchingDetailsUDF($"segmentDiv", $"currBtrName", $"hTCurrPitcherName", $"aTCurrPitcherName"))
    val allCols = kafkaLiveInfoT8DF2.columns.map { it => col(it) } ++ children("playerData", kafkaLiveInfoT9DF2)
    val kafkaLiveInfoT9DF3 = kafkaLiveInfoT9DF2.select(allCols.toSeq: _*).drop("playerData")
    kafkaLiveInfoT9DF3.select($"gameId", $"gameCode", $"statusId", $"isHomePitching", $"hTCurrPlayer", $"aTCurrPlayer", $"srcTimeEpoch", $"awayTeamInnings", $"homeTeamInnings", $"inningTitle").show(false)

    val indexResult = indexToSolr(zkHost, "live_info", "false", kafkaLiveInfoT9DF3)
    indexResult match {
      case Success(data) =>
        Holder.log.info(data.toString)
        val kafkaLiveInfoT10DF2 = kafkaLiveInfoT9DF3.select($"lastPlay",
          $"batchTime",
          $"srcTimeEpoch".alias("srcTime"),
          $"homeTeamExtId",
          $"awayTeamExtId",
          $"inningTitle",
          $"gameId", concat($"gameId", lit("_"), $"inningNo", lit("-"), md5($"lastPlay")).alias("id")).
          withColumn("img", concat(lit("http://gwserv-mobileprod.echodata.tv/Gamefinder/logos/LARGE/gid"), lit(getTeamIdUDF($"inningTitle", $"homeTeamExtId", $"awayTeamExtId")), lit(".png"))).
          withColumn("teamId", getTeamIdUDF($"inningTitle", $"homeTeamExtId", $"awayTeamExtId")).
          drop("homeTeamExtId", "awayTeamExtId")
        val kafkaLiveInfoT11DF3 = kafkaLiveInfoT10DF2.filter("lastPlay != ''")
        val indexResult2 = indexToSolr(zkHost, "scoring_events", "false", kafkaLiveInfoT11DF3)
        indexResult2 match {
          case Success(data) =>
            Holder.log.info(data.toString)
          case Failure(e) =>
            Holder.log.error("Error occurred in scoring_events indexing ", e)
        }
      case Failure(e) =>
        Holder.log.error("Error occurred in live_info indexing ", e)
    }

  }

  override def munch(inputKafkaTopic: String, outputCollName: String, zkHost: String): Unit = {

    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._
    val ds1 = spark.read.format("kafka").option("kafka.bootstrap.servers", "localhost:9092").option("subscribe", inputKafkaTopic).load()
    val ds2 = ds1.selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)").as[(String, String)]
    val kafkaLiveInfoT1DF1 = ds2.select(from_json($"key", StructType(StructField("payload", StringType, true) :: Nil)) as "fileName", from_json($"value", StructType(StructField("payload", StringType, true) :: Nil)) as "payloadStruct")
    mergeLiveInfo(zkHost, kafkaLiveInfoT1DF1)

  }

  override def stream(inputKafkaTopic: String, outputCollName: String, zkHost: String): Unit = {
    Holder.log.debug("Args is $args")

    val sc = SparkContext.getOrCreate()
    val spark = SparkSession.builder().getOrCreate()
    val ssc = new StreamingContext(sc, Seconds(1))

    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> "localhost:9092",
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> "liveDataMatcherStream",
      "auto.offset.reset" -> "latest",
      "enable.auto.commit" -> (false: java.lang.Boolean))

    val topics = Array(inputKafkaTopic)
    val stream = KafkaUtils.createDirectStream[String, String](
      ssc,
      LocationStrategies.PreferConsistent,
      ConsumerStrategies.Subscribe[String, String](topics, kafkaParams))
    val dstream0 = stream.map(record => (record.key, record.value))
    val dstream = dstream0.foreachRDD(kafkaRDD => {
      //val offsetRanges = kafkaRDD.asInstanceOf[HasOffsetRanges].offsetRanges
      val spark = SparkSession.builder.config(kafkaRDD.sparkContext.getConf).getOrCreate()
      import spark.implicits._
      val kafkaLiveInfoDF1 = kafkaRDD.toDF
      val kafkaLiveInfoT1DF1 = kafkaLiveInfoDF1.select(from_json($"_1", StructType(StructField("payload", StringType, true) :: Nil)) as "fileName", from_json($"_2", StructType(StructField("payload", StringType, true) :: Nil)) as "payloadStruct")
      mergeLiveInfo(zkHost, kafkaLiveInfoT1DF1)
      //dstream0.asInstanceOf[CanCommitOffsets].commitAsync(offsetRanges)
    })

    ssc.start
    ssc.awaitTermination
  }

}