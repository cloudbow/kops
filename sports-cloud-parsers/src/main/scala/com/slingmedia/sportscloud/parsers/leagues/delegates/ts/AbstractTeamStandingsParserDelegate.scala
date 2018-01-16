package com.slingmedia.sportscloud.parsers.leagues.delegates.ts

import com.slingmedia.sportscloud.parsers.factory.ParsedItem
import com.slingmedia.sportscloud.parsers.model.League
import org.apache.kafka.connect.data.{Schema, SchemaBuilder, Struct}
import org.apache.kafka.connect.source.SourceRecord
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.xml.{Elem, Node}


class TeamStandingsDataExtractor(data:Elem, rowData: Node) extends ParsedItem {

  //allow edit of teamname & team city for college leagues
  val league = (data \\ "league" \ "@alias").text
  var teamName = (rowData \\ "team-name" \ "@name").text
  val alias = (rowData \\ "team-name" \ "@alias").text
  var teamCity = (rowData \\ "team-city" \ "@city").text
  val teamCode = (rowData \\ "team-code" \ "@global-id").text
  val wins = toInt((rowData \\ "wins" \ "@number").text).getOrElse(0)
  val losses = toInt((rowData \\ "losses" \ "@number").text).getOrElse(0)
  val pct = toFloat((rowData \\ "winning-percentage" \ "@percentage").text).getOrElse(0f)



}

case class TeamStandingsSchemaGenerator(schemaBuilder: SchemaBuilder) {

  schemaBuilder
    .field("alias", Schema.STRING_SCHEMA)
    .field("league", Schema.STRING_SCHEMA)
    .field("teamName", Schema.STRING_SCHEMA)
    .field("teamCity", Schema.STRING_SCHEMA)
    .field("teamCode", Schema.STRING_SCHEMA)
    .field("wins", Schema.INT32_SCHEMA)
    .field("losses", Schema.INT32_SCHEMA)
    .field("pct", Schema.FLOAT32_SCHEMA)

}

case class TeamStandingsStructGenerator(struct: Struct, boxScoreExtractor: TeamStandingsDataExtractor ) {
  struct
    .put("alias", boxScoreExtractor.alias)
    .put("league", boxScoreExtractor.league)
    .put("teamName", boxScoreExtractor.teamName)
    .put("teamCity", boxScoreExtractor.teamCity)
    .put("teamCode", boxScoreExtractor.teamCode)
    .put("wins", boxScoreExtractor.wins)
    .put("losses", boxScoreExtractor.losses)
    .put("pct", boxScoreExtractor.pct)


}

case class TeamStandings(commonFields: TeamStandingsDataExtractor) {
  val teamStandingsSchemaInited = SchemaBuilder.struct().name("c.s.s.s.Game")
  val teamStandingsCommonSchema = TeamStandingsSchemaGenerator(teamStandingsSchemaInited)
  val teamStandingsSchema: Schema = teamStandingsSchemaInited
    .build()

  val connectSchema: Schema = teamStandingsSchema

  val teamStandingsStructInited = new Struct(teamStandingsSchema)
  val teamStandingsCommonStruct = TeamStandingsStructGenerator(teamStandingsStructInited,commonFields)
  val teamStandingsStruct: Struct = teamStandingsStructInited

  def getStructure: Struct = teamStandingsStruct

}