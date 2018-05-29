package com.slingmedia.sportscloud.offline.streaming.playergamestats.impl

import java.time.Instant

import com.slingmedia.sportscloud.offline.streaming.playergamestats.PlayerGameStatsMuncher
import org.apache.spark.sql.functions.{from_json, lit, concat}
import org.apache.spark.sql.types.{IntegerType, StructField, StructType}
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.slf4j.{Logger, LoggerFactory}

object NPGHolder extends Serializable {
  val serialVersionUID = 1L
  @transient lazy val log: Logger = LoggerFactory.getLogger("NbaPlayerGameStatsMuncher")
}

object NbaPlayerGameStatsMuncher {
  def main(args: Array[String]): Unit = {
    NPGHolder.log.debug(s"Args is $args")
    new NbaPlayerGameStatsMuncher().stream(args(0), args(1))
  }
}

/**
  * Player Stats streaming handler for NBA league
  */
class NbaPlayerGameStatsMuncher extends Serializable with PlayerGameStatsMuncher {
  override def getSchema(): StructType = {
    var finalSchema = commonStructFields() // Common fields required for any league
    finalSchema += StructField("minutes", IntegerType, nullable = true)
    finalSchema += StructField("fieldGoalsMade", IntegerType, nullable = true)
    finalSchema += StructField("fieldGoalsAttempted", IntegerType, nullable = true)
    finalSchema += StructField("freeThrowsMade", IntegerType, nullable = true)
    finalSchema += StructField("freeThrowsAttempted", IntegerType, nullable = true)
    finalSchema += StructField("threePointFieldGoalsMade", IntegerType, nullable = true)
    finalSchema += StructField("threePointFieldGoalsAttempted", IntegerType, nullable = true)
    finalSchema += StructField("points", IntegerType, nullable = true)
    finalSchema += StructField("reboundsOffensive", IntegerType, nullable = true)
    finalSchema += StructField("reboundsDefensive", IntegerType, nullable = true)
    finalSchema += StructField("reboundsTotal", IntegerType, nullable = true)
    finalSchema += StructField("assists", IntegerType, nullable = true)
    finalSchema += StructField("steals", IntegerType, nullable = true)
    finalSchema += StructField("blockedShots", IntegerType, nullable = true)
    finalSchema += StructField("turnovers", IntegerType, nullable = true)
    finalSchema += StructField("personalFouls", IntegerType, nullable = true)
    finalSchema += StructField("plusMinus", IntegerType, nullable = true)
    StructType(finalSchema.toList)
  }

  /**
    * Construct Player Game stats info NBA and indexes into ElasticSearch
    */
  override val mergeLiveInfo: (DataFrame) => Unit = (dataFrame: DataFrame) => {
    NPGHolder.log.debug("mergeLiveInfo ++")

    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._

    val playerGameStatsStructDF = dataFrame.select(from_json($"payloadStruct.payload", getSchema()) as "playerGameStatsStruct")

    val playerStatsRowsDF = playerGameStatsStructDF.select(children("playerGameStatsStruct", playerGameStatsStructDF): _*)

    val batchTimeStamp = Instant.now().getEpochSecond

    // Append batch process time and an id for indexing
    val finalDF = playerStatsRowsDF.withColumn("id", concat($"playerCodeId", lit("_"), $"gameCodeId")).withColumn("batchTime", lit(batchTimeStamp))
    finalDF.select($"id", $"teamCodeId", $"gameCodeId", $"firstName", $"playerCodeId").show(1, truncate = false)

    indexResults( "sc-player-game-stats", "player-game-stats", finalDF)
  }
}