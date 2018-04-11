package com.slingmedia.sportscloud.offline.batch.meta.impl.nba

import com.slingmedia.sportscloud.offline.batch.meta.MetaDataMuncher

import org.slf4j.LoggerFactory
import org.apache.spark.sql.types.{ArrayType, FloatType, IntegerType, LongType, StringType, StructField, StructType}
import org.apache.spark.sql.{Column, DataFrame, Row, SparkSession}
import org.apache.spark.sql.functions.{coalesce, col, collect_list, concat, explode, from_json, lit, max, min, udf}

import scala.collection.mutable.ListBuffer



object LDMHolder extends Serializable {
  @transient lazy val log = LoggerFactory.getLogger("LiveDataMuncher")
}

object NbaMetaDataMuncher extends  Serializable {

  def main(args: Array[String]) {
    LDMHolder.log.debug("Args is $args")
    new NbaMetaDataMuncher().munch(args(0),args(1),args(2))
  }

}


class NbaMetaDataMuncher extends Serializable with MetaDataMuncher {

  override def getLeagueSpecificPlayerStatsSchema(initFields: ListBuffer[StructField]) :ListBuffer[StructField] = {
    initFields += StructField("points",StringType,true)
    initFields += StructField("assists",StringType,true)
    initFields += StructField("blocks",StringType,true)
    initFields += StructField("fgMade",StringType,true)
    initFields += StructField("rebounds",StringType,true)
    initFields += StructField("steals",StringType,true)
    initFields += StructField("threePtMade",StringType,true)
    initFields
  }

  //Default implementation- nothing to do
}

