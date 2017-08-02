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

import com.lucidworks.spark.util.{ SolrSupport, SolrQuerySupport, ConfigurationConstants }

import java.time.Instant

object Holder extends Serializable {
  val serialVersionUID = 1L;
  @transient lazy val log = LoggerFactory.getLogger("LiveDataMuncher")
}

object LiveDataMuncher extends Serializable {
  def main(args: Array[String]) {
    Holder.log.debug("Args is $args")
    new LiveDataMuncher().stream("live_info", "live_info", "localhost:9983")
  }

}

class LiveDataMuncher extends Serializable with Muncher {

  //All Udfs starts here
    val getFieldsCount:(Int,Int,Int)=>String = (balls:Int, strikes:Int, outs:Int) => {
      var fieldCountTxt = ""
      if (balls != -1 && strikes != -1 && outs != -1) {
        fieldCountTxt =  balls + "-" + strikes + ", " + outs + (if(outs==1)  " out" else " outs");
      } 
      fieldCountTxt
    }

      val getFieldsCountUDF = udf(getFieldsCount(_: Int, _: Int, _: Int))

  //All udfs ends here

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
      val batchTimeStamp = Instant.now().getEpochSecond
      //val offsetRanges = kafkaRDD.asInstanceOf[HasOffsetRanges].offsetRanges
      val spark = SparkSession.builder.config(kafkaRDD.sparkContext.getConf).getOrCreate()
      import spark.implicits._
      val kafkaLiveInfoDF1 = kafkaRDD.toDF
      val kafkaLiveInfoT1DF1 = kafkaLiveInfoDF1.select(from_json($"_1", StructType(StructField("payload", StringType, true) :: Nil)) as "fileName", from_json($"_2", StructType(StructField("payload", StringType, true) :: Nil)) as "payloadStruct")
      val liveInfoSchema = StructType(StructField("status", StringType, true)
        :: StructField("statusId", StringType, true)
        :: StructField("gameType", StringType, true)
        :: StructField("gameCode", StringType, true)
        :: StructField("balls", IntegerType, true)
        :: StructField("strikes", IntegerType, true)
        :: StructField("outs", IntegerType, true)
        :: StructField("segmentDivision", StringType, true)
        :: StructField("lastPlay", StringType, true) 
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
      val kafkaLiveInfoT3DF2 = kafkaLiveInfoT3DF1.select(children("liveInfoStruct", kafkaLiveInfoT3DF1): _*).orderBy($"statusId")
      val kafkaLiveInfoT3DF3 = kafkaLiveInfoT3DF2.withColumn("id", $"gameCode").withColumn("fieldsCountHome",getFieldsCountUDF($"balls",$"strikes",$"outs"))
      val kafkaLiveInfoT4DF3 = kafkaLiveInfoT3DF3.withColumn("batchTime", lit(batchTimeStamp))
      kafkaLiveInfoT4DF3.select($"statusId").distinct.show(false)
      if (kafkaLiveInfoT4DF3.count > 0) {
        val solrOpts = Map("zkhost" -> zkHost, "collection" -> outputCollName, ConfigurationConstants.GENERATE_UNIQUE_KEY -> "false")
        val solrCloudClient = SolrSupport.getCachedCloudClient(zkHost)
        kafkaLiveInfoT4DF3.write.format("solr").options(solrOpts).mode(org.apache.spark.sql.SaveMode.Overwrite).save()
        solrCloudClient.commit(outputCollName, true, true)
        
      
      
      }
      //dstream0.asInstanceOf[CanCommitOffsets].commitAsync(offsetRanges)
    })

    ssc.start
    ssc.awaitTermination
  }

}