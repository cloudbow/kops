package com.slingmedia.sportscloud.parsers.leagues.delegates.playerstats

import com.slingmedia.sportscloud.parsers.factory.ParsedItem
import com.slingmedia.sportscloud.parsers.model.League
import org.apache.kafka.connect.data.{Schema, SchemaBuilder, Struct}
import org.apache.kafka.connect.source.SourceRecord
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.xml.{Elem, Node}


class PlayerStatsDataExtractor(data:Elem, rowData: Node, leagueStr: String, sportStr: String) extends ParsedItem {

  //get source time
  val teamName = (rowData \ "team-name" \ "@name").text
  val teamCity = (rowData \ "team-city" \ "@city").text
  val teamCode = (rowData \ "team-code" \ "@global-id").text
  val playerName = (rowData \\ "name" \ "@display-name").text
  val playerCode = (rowData \\ "player-code" \ "@global-id").text
  val playerNumber = (rowData \\ "player-number" \ "@number").text
  val wins = toInt((rowData  \\ "wins" \ "@number").text).getOrElse(0)
  val losses = toInt((rowData  \\ "losses" \ "@number").text).getOrElse(0)

  //refactor this
  val league = leagueStr
  val leagueFullName = sportStr

}

case class PlayerStatsSchemaGenerator(schemaBuilder: SchemaBuilder)
{
  schemaBuilder
    .field("league",Schema.STRING_SCHEMA)
    .field("leagueFullName",Schema.STRING_SCHEMA)
    .field("teamName", Schema.STRING_SCHEMA)
    .field("teamCity", Schema.STRING_SCHEMA)
    .field("teamCode", Schema.STRING_SCHEMA)
    .field("playerName",Schema.STRING_SCHEMA)
    .field("playerCode", Schema.STRING_SCHEMA)
    .field("playerNumber", Schema.STRING_SCHEMA)
    .field("wins", Schema.INT32_SCHEMA)
    .field("losses", Schema.INT32_SCHEMA)
    .build()
}

case class PlayerStatsStructGenerator(struct: Struct, playerStatsSchema: PlayerStatsSchemaGenerator, playerStatsExtractor: PlayerStatsDataExtractor ) {

  struct
    .put("league",playerStatsExtractor.league)
    .put("leagueFullName",playerStatsExtractor.leagueFullName)
    .put("teamName",playerStatsExtractor.teamName)
    .put("teamCity",playerStatsExtractor.teamCity)
    .put("teamCode",playerStatsExtractor.teamCode)
    .put("playerName",playerStatsExtractor.playerName)
    .put("playerCode",playerStatsExtractor.playerCode)
    .put("playerNumber",playerStatsExtractor.playerNumber)
    .put("wins",playerStatsExtractor.wins)
    .put("losses",playerStatsExtractor.losses)

}