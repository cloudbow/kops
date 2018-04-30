package com.slingmedia.sportscloud.parsers.leagues.delegates.playergamestats.impl.nba

import com.slingmedia.sportscloud.parsers.leagues.delegates.playergamestats.impl.TeamType.TeamType
import com.slingmedia.sportscloud.parsers.leagues.delegates.playergamestats.impl.{PlayerGameStats, PlayerGameStatsSchemaGenerator}
import org.apache.kafka.connect.data.{Schema, SchemaBuilder, Struct}

import scala.language.postfixOps
import scala.xml.{Elem, NodeSeq}

/**
  * Implementation for Nba Player stats extraction
  */
class NbaPlayerGameStats(data:Elem, nbaBoxScoreNode: NodeSeq, nbaPlayerStatsNode: NodeSeq, teamType: TeamType)
  extends PlayerGameStats(data:Elem, nbaBoxScoreNode: NodeSeq, nbaPlayerStatsNode: NodeSeq, teamType: TeamType) {

  private val minutes = toInt((nbaPlayerStatsNode \\ "minutes" \ "@minutes").text).getOrElse(0)

  private val fieldGoalsMade = toInt((nbaPlayerStatsNode \\ "field-goals" \ "@made").text).getOrElse(0)
  private val fieldGoalsAttempted = toInt((nbaPlayerStatsNode \\ "field-goals" \ "@attempted").text).getOrElse(0)

  private val freeThrowsMade = toInt((nbaPlayerStatsNode \\ "free-throws" \ "@made").text).getOrElse(0)
  private val freeThrowsAttempted = toInt((nbaPlayerStatsNode \\ "free-throws" \ "@attempted").text).getOrElse(0)


  private val threePointFieldGoalsMade = toInt((nbaPlayerStatsNode \\ "three-point-field-goals" \ "@made").text).getOrElse(0)
  private val threePointFieldGoalsAttempted = toInt((nbaPlayerStatsNode \\ "three-point-field-goals" \ "@attempted").text).getOrElse(0)

  private val points = toInt((nbaPlayerStatsNode \\ "points" \ "@points").text).getOrElse(0)

  private val reboundsOffensive = toInt((nbaPlayerStatsNode \\ "rebounds" \ "@offensive").text).getOrElse(0)
  private val reboundsDefensive = toInt((nbaPlayerStatsNode \\ "rebounds" \ "@defensive").text).getOrElse(0)
  private val reboundsTotal = toInt((nbaPlayerStatsNode \\ "rebounds" \ "@total").text).getOrElse(0)

  private val assists = toInt((nbaPlayerStatsNode \\ "assists" \ "@assists").text).getOrElse(0)

  private val steals = toInt((nbaPlayerStatsNode \\ "steals" \ "@steals").text).getOrElse(0)

  private val blockedShots = toInt((nbaPlayerStatsNode \\ "blocked-shots" \ "@blocked-shots").text).getOrElse(0)

  private val turnovers = toInt((nbaPlayerStatsNode \\ "turnovers" \ "@turnovers").text).getOrElse(0)

  private val personalFouls = toInt((nbaPlayerStatsNode \\ "personal-fouls" \ "@fouls").text).getOrElse(0)

  private val plusMinus = toInt((nbaPlayerStatsNode \\ "plus-minus" \ "@number").text).getOrElse(0)

  override protected def getSchema: Schema = NbaPlayerGameStats.SCHEMA

  override protected def appendToStruct(struct: Struct): Unit = {
    struct
    .put("minutes", minutes)
    .put("fieldGoalsMade", fieldGoalsMade)
    .put("fieldGoalsAttempted", fieldGoalsAttempted)
    .put("freeThrowsMade", freeThrowsMade)
    .put("freeThrowsAttempted", freeThrowsAttempted)
    .put("threePointFieldGoalsMade", threePointFieldGoalsMade)
    .put("threePointFieldGoalsAttempted", threePointFieldGoalsAttempted)
    .put("points", points)
    .put("reboundsOffensive", reboundsOffensive)
    .put("reboundsDefensive", reboundsDefensive)
    .put("reboundsTotal", reboundsTotal)
    .put("assists", assists)
    .put("steals", steals)
    .put("blockedShots", blockedShots)
    .put("turnovers", turnovers)
    .put("personalFouls", personalFouls)
    .put("plusMinus", plusMinus)
  }
}

/** Defines schema for NBA Player stats attributes  */
object NbaPlayerGameStats {
  val SCHEMA:Schema = getSchema

  private def getSchema: Schema = {
    val schemaBuilder: SchemaBuilder = SchemaBuilder.struct()
    PlayerGameStatsSchemaGenerator(schemaBuilder) // set schema for common fields
    schemaBuilder
      .name("c.s.s.boxScore.NbaPlayerStats")
      .field("minutes", Schema.INT32_SCHEMA)
      .field("fieldGoalsMade", Schema.INT32_SCHEMA)
      .field("fieldGoalsAttempted", Schema.INT32_SCHEMA)
      .field("freeThrowsMade", Schema.INT32_SCHEMA)
      .field("freeThrowsAttempted", Schema.INT32_SCHEMA)
      .field("threePointFieldGoalsMade", Schema.INT32_SCHEMA)
      .field("threePointFieldGoalsAttempted", Schema.INT32_SCHEMA)
      .field("points", Schema.INT32_SCHEMA)
      .field("reboundsOffensive", Schema.INT32_SCHEMA)
      .field("reboundsDefensive", Schema.INT32_SCHEMA)
      .field("reboundsTotal", Schema.INT32_SCHEMA)
      .field("assists", Schema.INT32_SCHEMA)
      .field("steals", Schema.INT32_SCHEMA)
      .field("blockedShots", Schema.INT32_SCHEMA)
      .field("turnovers", Schema.INT32_SCHEMA)
      .field("personalFouls", Schema.INT32_SCHEMA)
      .field("plusMinus", Schema.INT32_SCHEMA)
    schemaBuilder.build()
  }
}