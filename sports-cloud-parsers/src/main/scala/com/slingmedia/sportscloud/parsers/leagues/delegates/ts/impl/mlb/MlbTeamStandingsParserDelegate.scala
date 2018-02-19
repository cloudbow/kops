package com.slingmedia.sportscloud.parsers.leagues.delegates.ts.impl.mlb

import com.slingmedia.sportscloud.parsers.factory.ParsedItem
import com.slingmedia.sportscloud.parsers.leagues.delegates.ts.TeamStandingsDataExtractor
import com.slingmedia.sportscloud.parsers.leagues.delegates.ts.TeamStandingsStructGenerator
import com.slingmedia.sportscloud.parsers.leagues.delegates.ts.TeamStandingsSchemaGenerator
import com.slingmedia.sportscloud.parsers.model.League


import scala.xml.Elem
import scala.collection.JavaConverters._

import org.apache.kafka.connect.source.SourceRecord
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.data.Struct
import org.apache.kafka.connect.data.SchemaBuilder

import org.slf4j.LoggerFactory;
import com.typesafe.scalalogging.slf4j.Logger

class MlbTeamStandingsParserDelegate extends ParsedItem {

  private val log = LoggerFactory.getLogger("MlbTeamStandingsParserDelegate")

  override def generateRows(data: Elem, in: SourceRecord, xmlRoot: scala.xml.NodeSeq): java.util.List[SourceRecord] = {
    var teamStandingsRows = scala.collection.mutable.ListBuffer.empty[SourceRecord]
    xmlRoot.map { leagueStandings =>
      val subLeague = (leagueStandings \ "@league").text
      val mlbDivisionStandingsRows = (leagueStandings \\ "baseball-mlb-division-standings").map {
        mlbDivisionStandings =>
          val division = (mlbDivisionStandings \ "@division").text
          (mlbDivisionStandings \\ "baseball-mlb-team-standings").map {
            teamStandings =>
              val commonFields = new TeamStandingsDataExtractor(data,teamStandings,subLeague)
              val message = MlbTeamStandings(commonFields,
                subLeague,
                division)
              teamStandingsRows += new SourceRecord(
                in.sourcePartition,
                in.sourceOffset,
                in.topic,
                0,
                in.keySchema,
                in.key,
                message.connectSchema,
                message.getStructure)
          }
      }
    }
    teamStandingsRows.toList.asJava

  }

  case class MlbTeamStandings(commonFields: TeamStandingsDataExtractor,
                              subLeague: String ,
                              division: String) {
    val teamStandingsSchemaInited = SchemaBuilder.struct().name("c.s.s.s.Game")
    val teamStandingsCommonSchema = TeamStandingsSchemaGenerator(teamStandingsSchemaInited)
    val teamStandingsSchema: Schema = teamStandingsSchemaInited
      .field("division", Schema.STRING_SCHEMA)
      .field("subLeague", Schema.STRING_SCHEMA)
      .build()

    val connectSchema: Schema = teamStandingsSchema

    val teamStandingsStructInited = new Struct(teamStandingsSchema)
    val teamStandingsCommonStruct = TeamStandingsStructGenerator(teamStandingsStructInited,commonFields)
    val teamStandingsStruct: Struct = teamStandingsStructInited
      .put("division", division)
      .put("subLeague", subLeague)

    def getStructure: Struct = teamStandingsStruct

  }

}