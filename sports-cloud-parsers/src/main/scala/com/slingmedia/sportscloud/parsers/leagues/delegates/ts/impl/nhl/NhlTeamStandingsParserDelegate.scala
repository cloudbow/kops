package com.slingmedia.sportscloud.parsers.leagues.delegates.ts.impl.nhl

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

class NhlTeamStandingsParserDelegate extends ParsedItem {

  private val log = LoggerFactory.getLogger("NhlTeamStandingsParserDelegate")

  override def generateRows(data: Elem, in: SourceRecord, xmlRoot: scala.xml.NodeSeq): java.util.List[SourceRecord] = {
    var teamStandingsRows = scala.collection.mutable.ListBuffer.empty[SourceRecord]
    xmlRoot.map { leagueStandings =>
      (leagueStandings \\ "hockey-nhl-conference-standings").map { conferenceStanding =>
        val subLeague = (conferenceStanding \ "title" \ "@label").text

        val mlbDivisionStandingsRows = (conferenceStanding \\ "hockey-nhl-division-standings").map {
          mlbDivisionStandings =>
            val division = (mlbDivisionStandings \ "title" \ "@name").text
            (mlbDivisionStandings \\ "hockey-nhl-team-standings").map {
              teamStandings =>
                val commonFields = new TeamStandingsDataExtractor(data,teamStandings,subLeague)
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