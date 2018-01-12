package com.slingmedia.sportscloud.parsers.leagues.delegates.ts.impl.nfl

import com.slingmedia.sportscloud.parsers.factory.ParsedItem
import com.slingmedia.sportscloud.parsers.leagues.delegates.ts.TeamStandingsDataExtractor
import com.slingmedia.sportscloud.parsers.leagues.delegates.ts.TeamStandingsStructGenerator
import com.slingmedia.sportscloud.parsers.leagues.delegates.ts.TeamStandingsSchemaGenerator
import com.slingmedia.sportscloud.parsers.leagues.delegates.ts.TeamStandings
import com.slingmedia.sportscloud.parsers.model.League


import scala.xml.Elem
import scala.collection.JavaConverters._

import org.apache.kafka.connect.source.SourceRecord
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.data.Struct
import org.apache.kafka.connect.data.SchemaBuilder

import org.slf4j.LoggerFactory;
import com.typesafe.scalalogging.slf4j.Logger

class NflTeamStandingsParserDelegate extends ParsedItem {

  private val log = LoggerFactory.getLogger("NflTeamStandingsParserDelegate")

  override def generateRows(data: Elem, in: SourceRecord, xmlRoot: scala.xml.NodeSeq): java.util.List[SourceRecord] = {
    var teamStandingsRows = scala.collection.mutable.ListBuffer.empty[SourceRecord]
    xmlRoot.map { leagueStandings =>
      (leagueStandings \\ "football-nfl-conference-standings").map { conferenceStanding =>
        val subLeague = (conferenceStanding \ "@name").text

        val mlbDivisionStandingsRows = (conferenceStanding \\ "football-nfl-division-standings").map {
          mlbDivisionStandings =>
            val division = (mlbDivisionStandings \ "@name").text
            (mlbDivisionStandings \\ "football-nfl-team-standings").map {
              teamStandings =>
                val commonFields = new TeamStandingsDataExtractor(data,teamStandings)
                val message = TeamStandings(commonFields)
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
    }
    teamStandingsRows.toList.asJava

  }

}