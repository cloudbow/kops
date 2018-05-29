package com.slingmedia.sportscloud.offline.streaming.playergamestats

import com.slingmedia.sportscloud.offline.streaming.live.LiveDataMuncher
import org.apache.spark.sql.types.{IntegerType, StringType, StructField}

import scala.collection.mutable.ListBuffer

/**
  * Base definition of Player Game Stats handling.
  * Inheriting class override mergeLiveInfo() to construct league specific stats structure
  */
trait PlayerGameStatsMuncher extends LiveDataMuncher {

  override def commonStructFields(): ListBuffer[StructField] = {
    ListBuffer(StructField("teamCodeId", IntegerType, nullable = true),
      StructField("teamCodeGlobalId", IntegerType, nullable = true),
      StructField("gameCodeId", IntegerType, nullable = true),
      StructField("gameCodeGlobalId", IntegerType, nullable = true),
      StructField("league", StringType, nullable = true),
      StructField("firstName", StringType, nullable = true),
      StructField("lastName", StringType, nullable = true),
      StructField("playerCodeGlobalId", IntegerType, nullable = true),
      StructField("playerCodeId", IntegerType, nullable = true))
  }
}