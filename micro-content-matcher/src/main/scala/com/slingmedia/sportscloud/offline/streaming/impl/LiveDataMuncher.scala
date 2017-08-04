package com.slingmedia.sportscloud.offline.streaming.impl

import com.slingmedia.sportscloud.offline.batch.Muncher

import org.slf4j.LoggerFactory

import org.apache.spark.sql.functions.{ concat, lit, coalesce, max, min, udf, col, explode, from_json, collect_list, concat_ws }
import org.apache.spark.sql.types.{ StructType, StructField, StringType, IntegerType, LongType, FloatType, ArrayType };
import org.apache.spark.SparkContext
import org.apache.spark.streaming.{ StreamingContext, Seconds }
import org.apache.spark.sql.{ SparkSession, DataFrame, Row, Column }

import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.streaming.kafka010.{ HasOffsetRanges, CanCommitOffsets, KafkaUtils, LocationStrategies, ConsumerStrategies }

import java.time.Instant
import org.apache.spark.sql.types.IntegerType

object Holder extends Serializable {
  val serialVersionUID = 1L;
  @transient lazy val log = LoggerFactory.getLogger("LiveDataMuncher")
}

object LiveDataMuncher extends Serializable {
  def main(args: Array[String]) {
    Holder.log.debug("Args is $args")
    new LiveDataMuncher().stream(args(0), args(1), args(2))
  }

}

case class Pitcher(isHomePitching: Boolean, hTCurrPlayer: String, aTCurrPlayer: String)

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

  val getImg: (String, String, String) => String = (inningTitle: String, htId: String, atId: String) => {
    var teamId = "0"
    if (inningTitle != null) {
      if (inningTitle.toLowerCase.startsWith("bottom")) {
        teamId = htId
      } else {
        teamId = atId
      }
    }
    val imgStr = s"http://gwserv-mobileprod.echodata.tv/Gamefinder/logos/LARGE/gid$teamId.png"
    imgStr
  }

  val getImgUDF = udf(getImg(_: String, _: String, _: String))

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
      :: StructField("srcUtcHour", StringType, true)
      :: StructField("srcUtcMinute", StringType, true)
      :: StructField("status", StringType, true)
      :: StructField("statusId", IntegerType, true)
      :: StructField("gameType", StringType, true)
      :: StructField("gameCode", StringType, true)
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
      :: StructField("homeTeamAlias", StringType, true)
      :: StructField("homeTeamExtId", StringType, true)
      :: StructField("homeScoreRuns", IntegerType, true)
      :: StructField("homeScoreHits", IntegerType, true)
      :: StructField("homeScoreErrors", IntegerType, true)
      :: StructField("homeTeamInnings", ArrayType(IntegerType), true)
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
    val kafkaLiveInfoT4DF2 = kafkaLiveInfoT3DF2.withColumn("srcTimeEpoch", timeStrToEpochUDF(concat(col("srcYear"), lit("-"), lit(getZeroPaddedUDF($"srcMonth")), lit("-"), lit(getZeroPaddedUDF($"srcDate")), lit("T"), col("srcHour"), lit(":"), col("srcMinute"), lit(":00.00"), lit(getZeroPaddedUDF($"srcUtcHour")), lit(":"), col("srcUtcMinute"))))
    val kafkaLiveInfoT5DF2 = kafkaLiveInfoT4DF2.repartition($"gameCode").coalesce(3).orderBy($"gameCode",$"statusId", $"srcTimeEpoch")

    val kafkaLiveInfoT6DF2 = kafkaLiveInfoT5DF2.filter(col("gameCode").isNotNull)
    val kafkaLiveInfoT7DF2 = kafkaLiveInfoT6DF2.withColumn("id", $"gameCode").withColumn("fieldsCountHome", getFieldsCountUDF($"balls", $"strikes", $"outs"))
    val kafkaLiveInfoT8DF2 = kafkaLiveInfoT7DF2.withColumn("batchTime", lit(batchTimeStamp))
    val kafkaLiveInfoT9DF2 = kafkaLiveInfoT8DF2.withColumn("playerData", getPitchingDetailsUDF($"segmentDiv", $"currBtrName", $"hTCurrPitcherName", $"aTCurrPitcherName"))
    kafkaLiveInfoT9DF2.select($"statusId").distinct.show(false)
    if (kafkaLiveInfoT9DF2.count > 0) {

      indexToSolr(zkHost, "live_info", "false", kafkaLiveInfoT9DF2)

      val kafkaLiveInfoT10DF2 = kafkaLiveInfoT9DF2.select($"lastPlay",
        $"homeTeamExtId",
        $"awayTeamExtId",
        $"inningTitle",
        $"gameCode", concat($"gameCode", lit("_"), $"inningNo").alias("id")).
        withColumn("img", getImgUDF($"inningTitle", $"homeTeamExtId", $"awayTeamExtId")).
        drop("homeTeamExtId", "awayTeamExtId")

      indexToSolr(zkHost, "scoring_events", "false", kafkaLiveInfoT10DF2)

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