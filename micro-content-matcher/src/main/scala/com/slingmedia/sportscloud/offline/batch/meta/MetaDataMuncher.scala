package com.slingmedia.sportscloud.offline.batch.meta


import scala.collection.mutable.ListBuffer
import org.slf4j.LoggerFactory
import org.apache.spark.sql.types.{ArrayType, FloatType, IntegerType, LongType, StringType, StructField, StructType}
import org.apache.spark.sql.{Column, DataFrame, Row, SparkSession}
import org.apache.spark.sql.functions.{coalesce, col, collect_list, concat, explode, from_json, lit, max, min, udf}
import java.time.{ ZonedDateTime, OffsetDateTime, ZoneOffset, Instant, ZoneId }

import com.slingmedia.sportscloud.offline.streaming.live.impl.{MlbLiveDataMuncher, NbaLiveDataMuncher, NflLiveDataMuncher}
import com.slingmedia.sportscloud.offline.batch.Muncher

object MetaBatchJobType extends Enumeration {
  type MetaBatchJobType = Value
  val TEAMSTANDINGS,
  PLAYERSTATS,
  LIVEINFO,
  NCAAFLIVEINFO,
  NFLLIVEINFO,
  NBALIVEINFO,
  NCAABLIVEINFO = Value
}

object MDMHolder extends Serializable {
  @transient lazy val log = LoggerFactory.getLogger("LiveDataMuncher")
}

trait MetaDataMuncher extends Muncher {

 override def munch(arg0: String, arg1:String, arg2: String): Unit = {
    var schema: StructType = null
   val mucherType = MetaBatchJobType.withName(arg0.toUpperCase)
    val batchTimeStamp = Instant.now().getEpochSecond
    mucherType match {
      case MetaBatchJobType.PLAYERSTATS =>
        var finalSchema = commonStructFields()
        finalSchema = getLeagueSpecificPlayerStatsSchema(finalSchema)
        schema = StructType(finalSchema.toList)
        MDMHolder.log.info(s"Final schema used is $schema")
        //"meta_batch", "player_stats", "localhost:9983"
        process(batchTimeStamp, "sc-player-stats", arg1, arg2, schema, false, col("playerCode"), "key like '%PLAYER_STATS%.XML%'", col("playerCode").isNotNull)
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
        process(batchTimeStamp, "sc-team-standings", arg1, arg2, schema, true, col("teamCode"), "key like '%TEAM_STANDINGS.XML%'", col("league").isNotNull)
      case MetaBatchJobType.LIVEINFO =>
        //live_info, live_info, localhost:9983
        new MlbLiveDataMuncher().munch(arg1, arg2)
      case MetaBatchJobType.NCAAFLIVEINFO =>
        //live_info, live_info, localhost:9983
        new NflLiveDataMuncher().munch(arg1, arg2)
      case MetaBatchJobType.NFLLIVEINFO =>
        //live_info, live_info, localhost:9983
        new NflLiveDataMuncher().munch(arg1, arg2)
      case MetaBatchJobType.NBALIVEINFO =>
        //live_info, live_info, localhost:9983
        new NbaLiveDataMuncher().munch(arg1, arg2)
      case MetaBatchJobType.NCAABLIVEINFO =>
        //live_info, live_info, localhost:9983
        new NbaLiveDataMuncher().munch(arg1, arg2)

    }
  }

  def getLeagueSpecificPlayerStatsSchema(initFields: ListBuffer[StructField]) :ListBuffer[StructField] = {
    return initFields
  }

  def commonStructFields(): ListBuffer[StructField] = {
    ListBuffer(
      StructField("league",StringType,true) ,
      StructField("leagueFullName",StringType,true) ,
      StructField("teamName",StringType,true) ,
      StructField("teamCity",StringType,true) ,
      StructField("teamCode",StringType,true) ,
      StructField("playerName",StringType,true) ,
      StructField("playerCode",StringType,true) ,
      StructField("playerNumber",StringType,true) ,
      StructField("wins",StringType,true) ,
      StructField("losses",StringType,true))
  }

   def process(batchTimeStamp: Long, index: String, inputKafkaTopic: String, outputCollName: String, schema: StructType, imgRequired: Boolean, idColumn: Column, filterCond: String, testColumn: Column): Unit = {

    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._
    val ds1 = spark.read.format("kafka").option("kafka.bootstrap.servers", System.getenv("KAFKA_BROKER_EP")).option("subscribe", inputKafkaTopic).load()
    val ds2 = ds1.selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)").as[(String, String)]
    val ds3 = ds2.where(filterCond)
    val ds4 = ds3.select(from_json($"key", StructType(StructField("payload", StringType, true) :: Nil)) as "fileName", from_json($"value", StructType(StructField("payload", StringType, true) :: Nil)) as "payloadStruct")

    val ds5 = ds4.select($"fileName", from_json($"payloadStruct.payload", schema) as "metaDataStruct")
    val ds6 = ds5.select(children("metaDataStruct", ds5): _*).
      filter(idColumn =!= "")


    val ds7 = ds6.withColumn("id", idColumn)
    val ds8 = ds7.filter(testColumn)
    val ds9 = ds8.withColumn("batchTime", lit(batchTimeStamp))
    var finalDataFrame: DataFrame = null
    if (imgRequired) {
      val allCols = ds9.columns.map { it => col(it) } :+ concat(lit("http://gwserv-mobileprod.echodata.tv/Gamefinder/logos/LARGE/gid"), $"id", lit(".png")).alias("img")
      finalDataFrame = ds9.select(allCols.toSeq: _*)
    } else {
      finalDataFrame = ds9
    }
    finalDataFrame.toJSON.toDF.show(120,false)
    indexResults( index, outputCollName,  finalDataFrame)


  }


}

