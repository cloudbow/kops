package com.slingmedia.sportscloud.parsers.leagues.delegates.playergamestats.impl

import com.slingmedia.sportscloud.parsers.factory.ParsedItem
import com.slingmedia.sportscloud.parsers.leagues.delegates.playergamestats.impl.TeamType.TeamType
import org.apache.kafka.connect.data.{Schema, SchemaBuilder, Struct}

import scala.xml.{Elem, NodeSeq}


/**
  * Base definition of Player stats data for a game.
  * This Contains common fields required for Player stats from any league.
  *
  * @param data Full XML data
  * @param boxScoreNode Node containing player stats info
  * @param playerStatsNode Player stats object with tag Name "home-player-stats" or "visiting-player-stats"
  * @param teamType Home/Visiting team
  */
abstract class PlayerGameStats(data:Elem, boxScoreNode: NodeSeq, playerStatsNode: NodeSeq, teamType: TeamType) extends ParsedItem {

  /** Appends specific data fields of a Player in league. To be implemented by deriving class */
  protected def appendToStruct(struct: Struct)

  /**
    * Get Definitions of data for this Player. To be implemented by deriving class.
    * Deriving classes must set the Common schema using PlayerGameStatsSchemaGenerator()
    */
  protected def getSchema: Schema

  // Attributes Extractor
  private val teamCodeId = toInt((boxScoreNode \\ teamType.toString \\ "team-code" \ "@id").text).getOrElse(0)
  private val teamCodeGlobalId = toInt((boxScoreNode \\ teamType.toString \\ "team-code" \ "@global-id").text).getOrElse(0)

  private val gameCodeId = toInt((boxScoreNode \\ "gamecode" \ "@code").text).getOrElse(0)
  private val gameCodeGlobalId = toInt((boxScoreNode \\ "gamecode" \ "@global-id").text).getOrElse(0)

  private val league = (data \\ "league" \ "@alias").text

  private val firstName = (playerStatsNode \\ "name" \ "@first-name").text
  private val lastName = (playerStatsNode \\ "name" \ "@last-name").text

  private val playerCodeGlobalId = toInt((playerStatsNode \\ "player-code" \ "@global-id").text).getOrElse(0)
  private val playerCodeId = toInt((playerStatsNode \\ "player-code" \ "@id").text).getOrElse(0)

  /** Convert the data model to kafka data structure */
  final def toStruct: Struct = {
    val struct = new Struct(getSchema) // Schema for the data structure
      .put("teamCodeId", teamCodeId)
      .put("teamCodeGlobalId", teamCodeGlobalId)
      .put("gameCodeId", gameCodeId)
      .put("gameCodeGlobalId", gameCodeGlobalId)
      .put("league", league)
      .put("firstName", firstName)
      .put("lastName", lastName)
      .put("playerCodeGlobalId", playerCodeGlobalId)
      .put("playerCodeId", playerCodeId)

    appendToStruct(struct)

    struct
  }
}

object TeamType extends Enumeration {
  type TeamType = Value
  val HOME = Value("home-team")
  val VISITING = Value("visiting-team")
}

/** Sets Base (common fields) schema for Player stats data */
case class PlayerGameStatsSchemaGenerator(schemaBuilder: SchemaBuilder)
{
  schemaBuilder
    .field("teamCodeId", Schema.INT32_SCHEMA)
    .field("teamCodeGlobalId", Schema.INT32_SCHEMA)
    .field("gameCodeId", Schema.INT32_SCHEMA)
    .field("gameCodeGlobalId", Schema.INT32_SCHEMA)
    .field("league", Schema.STRING_SCHEMA)
    .field("firstName", Schema.STRING_SCHEMA)
    .field("lastName", Schema.STRING_SCHEMA)
    .field("playerCodeGlobalId", Schema.INT32_SCHEMA)
    .field("playerCodeId", Schema.INT32_SCHEMA)
}
