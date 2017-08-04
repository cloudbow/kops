package com.slingmedia.sportscloud.offline.batch.impl

import com.slingmedia.sportscloud.offline.batch.Muncher

import org.slf4j.LoggerFactory;



import com.lucidworks.spark.util.{ SolrSupport, SolrQuerySupport, ConfigurationConstants }

import org.apache.spark.sql.types.{ StructType, StructField, StringType, IntegerType, LongType, FloatType, ArrayType };
import org.apache.spark.sql.{ SparkSession, DataFrame, Row, Column }
import org.apache.spark.sql.functions.{ concat, lit, coalesce, max, min, udf, col, explode, from_json, collect_list }

import java.time.Instant
import com.slingmedia.sportscloud.offline.streaming.impl.LiveDataMuncher

object LDMHolder extends Serializable {
  val serialVersionUID = 1L;
  @transient lazy val log = LoggerFactory.getLogger("LiveDataMuncher")
}

object MetaBatchJobType extends Enumeration {
  type MetaBatchJobType = Value
  val TEAMSTANDINGS, PLAYERSTATS, LIVEINFO = Value
}

object MetaDataMuncher extends Serializable {
  def main(args: Array[String]) {
    LDMHolder.log.debug("Args is $args")
    val mucherType = MetaBatchJobType.withName(args(0).toUpperCase)
    var schema: StructType = null
    val batchTimeStamp = Instant.now().getEpochSecond
    mucherType match {
      case MetaBatchJobType.PLAYERSTATS =>
        schema = StructType(StructField("playerCode", StringType, true)
          :: StructField("wins", IntegerType, true)
          :: StructField("losses", IntegerType, true) :: Nil)
          //"meta_batch", "player_stats", "localhost:9983"
        new MetaDataMuncher().munch(batchTimeStamp, args(1), args(2), args(3), schema, true, col("playerCode"), "key like '%PLAYER_STATS%.XML%'", col("playerCode").isNotNull)
      case MetaBatchJobType.TEAMSTANDINGS =>
        schema = StructType(StructField("league", StringType, true) ::
          StructField("alias", StringType, true) ::
          StructField("subLeague", StringType, true) ::
          StructField("division", StringType, true) ::
          StructField("teamName", StringType, true) ::
          StructField("teamCity", StringType, true) ::
          StructField("teamCode", StringType, true) ::
          StructField("wins", IntegerType, true) ::
          StructField("losses", IntegerType, true) ::
          StructField("pct", FloatType, true) :: Nil)
          //"meta_batch", "team_standings", "localhost:9983"
        new MetaDataMuncher().munch(batchTimeStamp, args(1), args(2), args(3), schema, false, col("teamCode"), "key like '%TEAM_STANDINGS.XML%'", col("league").isNotNull)
      case MetaBatchJobType.LIVEINFO =>
        //live_info, live_info, localhost:9983
        new LiveDataMuncher().munch(args(1),args(2),args(3))
    }
  }

}

class MetaDataMuncher extends Serializable with Muncher {
  override def munch(batchTimeStamp: Long, inputKafkaTopic: String, outputCollName: String, zkHost: String, schema: StructType, imgRequired: Boolean, idColumn: Column, filterCond: String, testColumn: Column): Unit = {

    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._
    val ds1 = spark.read.format("kafka").option("kafka.bootstrap.servers", "localhost:9092").option("subscribe", inputKafkaTopic).load()
    val ds2 = ds1.selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)").as[(String, String)]
    val ds3 = ds2.where(filterCond)
    val ds4 = ds3.select(from_json($"key", StructType(StructField("payload", StringType, true) :: Nil)) as "fileName", from_json($"value", StructType(StructField("payload", StringType, true) :: Nil)) as "payloadStruct")

    val ds5 = ds4.select($"fileName", from_json($"payloadStruct.payload", schema) as "metaDataStruct")
    val ds6 = ds5.select(children("metaDataStruct", ds5): _*)
    val ds7 = ds6.withColumn("id", idColumn)
    val ds8 = ds7.filter(testColumn)
    val ds9 = ds8.withColumn("batchTime", lit(batchTimeStamp))
    var finalDataFrame:DataFrame = null
    if (imgRequired) {
      val allCols = ds9.columns.map { it => col(it) } :+ concat(lit("http://gwserv-mobileprod.echodata.tv/Gamefinder/logos/LARGE/gid"), $"id", lit(".png")).alias("img")
      finalDataFrame = ds9.select(allCols.toSeq:_*)
    } else {
      finalDataFrame = ds9
    }

    if (finalDataFrame.count > 0) {
      val solrOpts = Map("zkhost" -> zkHost, "collection" -> outputCollName, ConfigurationConstants.GENERATE_UNIQUE_KEY -> "false")
      finalDataFrame.write.format("solr").options(solrOpts).mode(org.apache.spark.sql.SaveMode.Overwrite).save()
      val solrCloudClient = SolrSupport.getCachedCloudClient(zkHost)
      solrCloudClient.commit(outputCollName, true, true)
    }

  }

}