package com.slingmedia.sportscloud.parsers.leagues.delegates.ts.impl.ncaaf

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

class NcaafTeamStandingsParserDelegate extends ParsedItem {

  private val log = LoggerFactory.getLogger("NcaafTeamStandingsParserDelegate")

  override def generateRows(data: Elem, in: SourceRecord, xmlRoot: scala.xml.NodeSeq): java.util.List[SourceRecord] = {

    var teamStandingsRows = scala.collection.mutable.ListBuffer.empty[SourceRecord]
    xmlRoot.map { leagueStandings =>
      (leagueStandings \\ "cfb-conference-standings").map { conferenceStanding =>
        val subLeague = (conferenceStanding \ "@abbrev").text

        val mlbDivisionStandingsRows = (conferenceStanding \\ "cfb-conference-division").map {
          mlbDivisionStandings =>
            val division = (mlbDivisionStandings \ "@division").text
            (mlbDivisionStandings \\ "cfb-team-standings").map {
              teamStandings =>
                val commonFields = new TeamStandingsDataExtractor(data,teamStandings)
                commonFields.subLeague=subLeague
                commonFields.division=division
                //swap team name and city
                var teamName =  commonFields.teamName
                var teamCity = (teamStandings \\ "college-name" \ "@name").text
                val tempCity = teamCity
                teamCity = teamName
                teamName = tempCity

                commonFields.teamName=teamName
                commonFields.teamCity=teamCity

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