package com.slingmedia.sportscloud.offline.batch.meta.impl

import com.slingmedia.sportscloud.offline.batch.meta.MetaDataMuncher

import org.slf4j.LoggerFactory
import org.apache.spark.sql.types.{ArrayType, FloatType, IntegerType, LongType, StringType, StructField, StructType}
import org.apache.spark.sql.{Column, DataFrame, Row, SparkSession}
import org.apache.spark.sql.functions.{coalesce, col, collect_list, concat, explode, from_json, lit, max, min, udf}

import scala.collection.mutable.ListBuffer



object LDMHolder extends Serializable {
  @transient lazy val log = LoggerFactory.getLogger("LiveDataMuncher")
}

object DefaultMetaDataMuncher extends  Serializable {

  def main(args: Array[String]) {
    LDMHolder.log.debug("Args is $args")

    new DefaultMetaDataMuncher().munch(args(0),args(1),args(2))
  }

}


class DefaultMetaDataMuncher extends Serializable with MetaDataMuncher {

  //Default implementation- nothing to do
}

