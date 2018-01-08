package com.slingmedia.sportscloud.parsers.model

import scala.xml.Elem
import org.apache.kafka.connect.source.SourceRecord
import com.slingmedia.sportscloud.parsers.model.League
import scala.collection.JavaConverters._
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.data.Struct
import org.apache.kafka.connect.data.SchemaBuilder
import org.slf4j.LoggerFactory;
import com.typesafe.scalalogging.slf4j.Logger
import scala.xml.NodeSeq


case class TeamStandings(alias: String, league: String, subLeague: String, division: String, teamName: String, teamCity: String, teamCode: String, wins: Int, losses: Int, pct: Float) {

  val teamStandingSchema: Schema = SchemaBuilder.struct().name("c.s.s.s.TeamStandingSchema")
    .field("alias", Schema.STRING_SCHEMA)
    .field("league", Schema.STRING_SCHEMA)
    .field("subLeague", Schema.STRING_SCHEMA)
    .field("division", Schema.STRING_SCHEMA)
    .field("teamName", Schema.STRING_SCHEMA)
    .field("teamCity", Schema.STRING_SCHEMA)
    .field("teamCode", Schema.STRING_SCHEMA)
    .field("wins", Schema.INT32_SCHEMA)
    .field("losses", Schema.INT32_SCHEMA)
    .field("pct", Schema.FLOAT32_SCHEMA)
    .build()
  val teamStandingStruct: Struct = new Struct(teamStandingSchema)
    .put("alias", alias)
    .put("league", league)
    .put("subLeague", subLeague)
    .put("division", division)
    .put("teamName", teamName)
    .put("teamCity", teamCity)
    .put("teamCode", teamCode)
    .put("wins", wins)
    .put("losses", losses)
    .put("pct", pct)

  val connectSchema: Schema = teamStandingSchema
  def getStructure: Struct = teamStandingStruct

}

