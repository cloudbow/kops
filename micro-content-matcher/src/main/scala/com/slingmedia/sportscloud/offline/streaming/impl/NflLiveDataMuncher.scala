package com.slingmedia.sportscloud.offline.streaming.impl

import com.slingmedia.sportscloud.offline.batch.Muncher

import org.slf4j.LoggerFactory

import org.apache.spark.sql.functions.{ md5, concat, lit, coalesce, max, min, udf, col, explode, from_json, collect_list, concat_ws }
import org.apache.spark.sql.types.{ StructType, StructField, StringType, IntegerType, LongType, FloatType, ArrayType, MapType }
import org.apache.spark.SparkContext
import org.apache.spark.streaming.{ StreamingContext, Seconds }
import org.apache.spark.sql.{ SparkSession, DataFrame, Row, Column }

import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.streaming.kafka010.{ HasOffsetRanges, CanCommitOffsets, KafkaUtils, LocationStrategies, ConsumerStrategies }

import scala.collection.mutable.ArrayBuffer
import scala.util.{ Try, Success, Failure }

import java.time.Instant
import org.apache.spark.sql.types.IntegerType
import org.apache.spark.sql.types.DoubleType


import scala.util.parsing.json._
import org.apache.spark.sql.types.DataType

object NflHolder extends Serializable {
  val serialVersionUID = 1L;
  @transient lazy val log = LoggerFactory.getLogger("LiveDataMuncher")
}



object NflLiveDataMuncher extends Serializable {
  def main(args: Array[String]) {
    NflHolder.log.debug("Args is $args")
    new NflLiveDataMuncher().stream(args(0), args(1))
  }

}

class NflLiveDataMuncher extends Serializable with Muncher {


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


  val getReorderedStatusId: (Int => Int) = (statusId: Int) => {
    if (statusId == 23) 2 else statusId
  }
  val getReorderedStatusIdUDF = udf(getReorderedStatusId(_: Int))
  

  //All udfs ends here
  


  val mergeLiveInfo: (DataFrame) => Unit = ( kafkaLiveInfoT1DF1: DataFrame) => {
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
        :: StructField("lastPlay", StringType, true)
        :: StructField("homeTeamName", StringType, true)
        :: StructField("homeTeamAlias", StringType, true)
        :: StructField("homeTeamExtId", StringType, true)
        :: StructField("homeTeamlineScore", ArrayType(IntegerType), true)
        :: StructField("awayTeamName", StringType, true)
        :: StructField("awayTeamAlias", StringType, true)
        :: StructField("awayTeamExtId", StringType, true)
        :: StructField("awayTeamlineScore", ArrayType(IntegerType), true)
        :: StructField("period", StringType, true)
        :: StructField("position", DoubleType, true)
        :: StructField("timer", StringType, true)
        :: StructField("playType", StringType, true)
        :: StructField("drives", ArrayType(StringType), true)  
        :: StructField("gameTimeSeconds", IntegerType, true)
        :: StructField("inningNo", IntegerType, true)
        :: StructField("awayScore", IntegerType, true)
        :: StructField("homeScore", IntegerType, true)
        :: Nil)

    val kafkaLiveInfoT2DF1 = kafkaLiveInfoT1DF1.where("fileName.payload like '%BOXSCORE%' OR fileName.payload like '%FINALBOX%' OR fileName.payload like '%_LIVE%' ")

    val kafkaLiveInfoT3DF1 = kafkaLiveInfoT2DF1.select(from_json($"payloadStruct.payload", liveInfoSchema) as "liveInfoStruct")
    val kafkaLiveInfoT3DF2 = kafkaLiveInfoT3DF1.select(children("liveInfoStruct", kafkaLiveInfoT3DF1): _*)
    val kafkaLiveInfoT4DF2 = kafkaLiveInfoT3DF2.withColumn("srcTimeEpoch", timeStrToEpochUDF(concat(col("srcYear"), lit("-"), lit(getZeroPaddedUDF($"srcMonth")), lit("-"), lit(getZeroPaddedUDF($"srcDate")), lit("T"), lit(getZeroPaddedUDF($"srcHour")), lit(":"), lit(getZeroPaddedUDF($"srcMinute")), lit(":"), lit(getZeroPaddedUDF($"srcSecond")), lit(".00"), lit(getZeroPaddedUDF($"srcUtcHour")), lit(":"), lit(getZeroPaddedUDF($"srcUtcMinute")))))
    // filte only data with non null gameId
    val kafkaLiveInfoT5DF1 = kafkaLiveInfoT4DF2.filter(col("gameId").isNotNull)
    //reorder statusId so that the ordering is right
    val kafkaLiveInfoT5DF2 = kafkaLiveInfoT5DF1.withColumn("rStatusId", getReorderedStatusIdUDF($"statusId"))
    //val kafkaLiveInfoTDF2 = kafkaLiveInfoT5DF1.withColumn("drivesFlat", getFlatDrivesUDF($"drives"))
    //order by new statusId 
    //Repartition by gameId as we can update solr paralley for each game
    //Order it so that the order is updated
    val kafkaLiveInfoT6DF2 = kafkaLiveInfoT5DF2.coalesce(4)
    val kafkaLiveInfoT7DF2 = kafkaLiveInfoT6DF2.withColumn("id", $"gameId").
      //withColumn("fieldCountsTxt", getFieldsCountUDF($"balls", $"strikes", $"outs")).
      //withColumn("fieldState", getFieldStateUDF($"firstGameBase", $"secondGameBase", $"thirdGameBase")).
      withColumn("date", concat(col("year"), lit("-"), lit(getZeroPaddedUDF($"month")), lit("-"), lit(getZeroPaddedUDF($"date")), lit("T"), lit(getZeroPaddedUDF($"hour")), lit(":"), lit(getZeroPaddedUDF($"minute")), lit(":00.00"), lit(getZeroPaddedUDF($"utcHour")), lit(":"), lit(getZeroPaddedUDF($"utcMinute"))))

    val kafkaLiveInfoT8DF2 = kafkaLiveInfoT7DF2.
      withColumn("batchTime", lit(batchTimeStamp)).
      withColumn("game_date_epoch", timeStrToEpochUDF($"date")).
      withColumn("seconds", getGameSecondsUDF($"srcTimeEpoch",$"game_date_epoch")).
      withColumn("gameDate", timeEpochtoStrUDF($"game_date_epoch"))
    val kafkaLiveInfoT9DF2 = kafkaLiveInfoT8DF2 //.withColumn("playerData", getPitchingDetailsUDF($"segmentDiv", $"currBtrName", $"hTCurrPitcherName", $"aTCurrPitcherName"))
    val allCols = kafkaLiveInfoT8DF2.columns.map { it => col(it) } //++ children("drivesFlat", kafkaLiveInfoT9DF2)
    val kafkaLiveInfoT9DF3 = kafkaLiveInfoT8DF2.select(allCols.toSeq: _*).orderBy($"gameId", $"rStatusId", $"srcTimeEpoch").repartition($"gameId").coalesce(4)

    kafkaLiveInfoT9DF3.select($"gameId", $"gameCode", $"statusId", $"drives", $"awayTeamlineScore", $"homeTeamlineScore").show(false)


    val indexResult = Try(indexResults("live_info",  kafkaLiveInfoT9DF3))

    indexResult match {
      case Success(data) =>
        NflHolder.log.info(data.toString)
        val kafkaLiveInfoT10DF2 = kafkaLiveInfoT9DF3.select($"lastPlay",
          $"batchTime",
          $"srcTimeEpoch".alias("srcTime"),
          $"homeTeamExtId",
          $"awayTeamExtId",
          $"period",
          $"timer",
          $"position",
          $"playType",
          $"gameTimeSeconds",
          $"seconds",
          $"gameId", concat($"gameId", lit("_"), $"inningNo", lit("-"), md5($"lastPlay")).alias("id")).
          withColumn("img", concat(lit("http://gwserv-mobileprod.echodata.tv/Gamefinder/logos/LARGE/gid"),  $"homeTeamExtId", lit(".png"))).
          //withColumn("secondss",).
          withColumn("teamId", $"homeTeamExtId").
          drop("homeTeamExtId", "awayTeamExtId")
        val kafkaLiveInfoT11DF3 = kafkaLiveInfoT10DF2.filter("lastPlay != ''")

        indexResults( "scoring_events", kafkaLiveInfoT11DF3)

      case Failure(e) =>
        NflHolder.log.error("Error occurred in live_info indexing ", e)
    }

  }

  override def munch(inputKafkaTopic: String, outputCollName: String): Unit = {

    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._
    val ds1 = spark.read.format("kafka").option("kafka.bootstrap.servers", System.getenv("KAFKA_BROKER_EP")).option("subscribe", inputKafkaTopic).load()
    val ds2 = ds1.selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)").as[(String, String)]
    val kafkaLiveInfoT1DF1 = ds2.select(from_json($"key", StructType(StructField("payload", StringType, true) :: Nil)) as "fileName", from_json($"value", StructType(StructField("payload", StringType, true) :: Nil)) as "payloadStruct")
    mergeLiveInfo(kafkaLiveInfoT1DF1)

  }

  override def stream(inputKafkaTopic: String, outputCollName: String): Unit = {
    NflHolder.log.debug("Args is $args")

    val sc = SparkContext.getOrCreate()
    val spark = SparkSession.builder().getOrCreate()
    val ssc = new StreamingContext(sc, Seconds(5))


    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> System.getenv("KAFKA_BROKER_EP"),
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> "NcaafliveDataMatcherStream",
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
      mergeLiveInfo(kafkaLiveInfoT1DF1)
      //dstream0.asInstanceOf[CanCommitOffsets].commitAsync(offsetRanges)
    })

    ssc.start
    ssc.awaitTermination
  }

}
