package com.slingmedia.sportscloud.offline.streaming.live

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

object LDMHolder extends Serializable {
  val serialVersionUID = 1L;
  @transient lazy val log = LoggerFactory.getLogger("LiveDataMuncher")
}

trait LiveDataMuncher extends Muncher {

  def getSchema(): StructType = {StructType(
    StructField("unknown", StringType, true)::Nil)}
  def getKafkaConsumerGroupId(): String = {
    LDMHolder.log.debug("Using consumer groupId:"+getClass().getName())
    getClass().getName()
  }
  def addLeagueSpecificData(df: DataFrame): DataFrame = {
    val spark = SparkSession.builder().getOrCreate()
    val df = spark.createDataFrame(Seq())
    df
  }
  def showSelected(df: DataFrame):Unit = {}
  def createScoringEventsAndIndex(df: DataFrame):Unit = {}

  val mergeLiveInfo: (DataFrame) => Unit = ( kafkaLiveInfoT1DF1: DataFrame) => {
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._
    val batchTimeStamp = Instant.now().getEpochSecond

    val liveInfoSchema = getSchema()

    val kafkaLiveInfoT2DF1 = kafkaLiveInfoT1DF1.
      where("fileName.payload like '%BOXSCORE%' OR fileName.payload like '%FINALBOX%' OR fileName.payload like '%_LIVE%' ")

    val kafkaLiveInfoT3DF1 = kafkaLiveInfoT2DF1.
      select(from_json($"payloadStruct.payload", liveInfoSchema) as "liveInfoStruct")
    val kafkaLiveInfoT3DF2 = kafkaLiveInfoT3DF1.
      select(children("liveInfoStruct", kafkaLiveInfoT3DF1): _*)
    val kafkaLiveInfoT4DF2 = kafkaLiveInfoT3DF2.
      withColumn("srcTimeEpoch",
        timeStrToEpochUDF(concat(col("srcYear"),
          lit("-"),
          lit(zeroPadDateTimeUDF($"srcMonth")),
          lit("-"),
          lit(zeroPadDateTimeUDF($"srcDate")), lit("T"),
          lit(zeroPadDateTimeUDF($"srcHour")), lit(":"),
          lit(zeroPadDateTimeUDF($"srcMinute")), lit(":"),
          lit(zeroPadDateTimeUDF($"srcSecond")), lit(".00"),
          lit(zeroPadTimeOffsetUDF($"srcUtcHour",$"srcUtcMinute"))))).
      filter(col("gameId").isNotNull).
      withColumn("rStatusId", getReorderedStatusIdUDF($"statusId")).
      coalesce(4).
      withColumn("id", $"gameId").
      withColumn("date",
        concat(col("year"),
                lit("-"),
                lit(zeroPadDateTimeUDF($"month")),
                lit("-"),
                lit(zeroPadDateTimeUDF($"date")),
                lit("T"),
                lit(zeroPadDateTimeUDF($"hour")), lit(":"),
                lit(zeroPadDateTimeUDF($"minute")), lit(":00.00"),
                lit(zeroPadTimeOffsetUDF($"utcHour",$"utcMinute")))).
      withColumn("batchTime", lit(batchTimeStamp)).
      withColumn("game_date_epoch", timeStrToEpochUDF($"date")).
      withColumn("gameDate", timeEpochtoStrUDF($"game_date_epoch"))

    val kafkaLiveInfoT4DF3 = addLeagueSpecificData(kafkaLiveInfoT4DF2)



    val kafkaLiveInfoT10DF3 = kafkaLiveInfoT4DF3.
      orderBy($"gameId", $"rStatusId", $"srcTimeEpoch").
      repartition($"gameId").
      coalesce(4)


    showSelected(kafkaLiveInfoT10DF3)

    createScoringEventsAndIndex(kafkaLiveInfoT4DF3)

  }

  override def stream(inputKafkaTopic: String, outputCollName: String): Unit = {
    LDMHolder.log.debug("Args is $args")

    val sc = SparkContext.getOrCreate()
    val spark = SparkSession.builder().getOrCreate()
    val ssc = new StreamingContext(sc, Seconds(5))


    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> System.getenv("KAFKA_BROKER_EP"),
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> getKafkaConsumerGroupId(),
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



  override def munch(inputKafkaTopic: String, outputCollName: String): Unit = {

    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._
    val ds1 = spark.read.format("kafka").option("kafka.bootstrap.servers", System.getenv("KAFKA_BROKER_EP")).option("subscribe", inputKafkaTopic).load()
    val ds2 = ds1.selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)").as[(String, String)]
    val kafkaLiveInfoT1DF1 = ds2.select(from_json($"key", StructType(StructField("payload", StringType, true) :: Nil)) as "fileName", from_json($"value", StructType(StructField("payload", StringType, true) :: Nil)) as "payloadStruct")
    mergeLiveInfo(kafkaLiveInfoT1DF1)

  }
}