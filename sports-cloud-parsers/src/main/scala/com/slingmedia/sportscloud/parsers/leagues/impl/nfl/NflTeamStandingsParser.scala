package com.slingmedia.sportscloud.parsers.leagues.impl.nfl

import com.slingmedia.sportscloud.parsers.factory.ParsedItem
import com.slingmedia.sportscloud.parsers.model.{ League , TeamStandings }
import org.apache.kafka.connect.data.{Schema, SchemaBuilder, Struct}
import org.apache.kafka.connect.source.SourceRecord
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.xml.{Elem, NodeSeq}

class NflTeamStandingsParser extends ParsedItem {

  private val log = LoggerFactory.getLogger("ScheduleParser")

  override def generateRows(data: Elem, in: SourceRecord, xmlRoot: NodeSeq): java.util.List[SourceRecord] = {
    val leagueStr = (data \\ "league" \ "@alias").text

    var teamStandingsRows = scala.collection.mutable.ListBuffer.empty[SourceRecord]
    xmlRoot.map { leagueStandings =>
      (leagueStandings \\ "football-nfl-conference-standings").map { conferenceStanding =>
        val subLeague = (conferenceStanding \ "@name").text

        val mlbDivisionStandingsRows = (conferenceStanding \\ "football-nfl-division-standings").map {
          mlbDivisionStandings =>
            val division = (mlbDivisionStandings \ "@name").text
            (mlbDivisionStandings \\ "football-nfl-team-standings").map {
              teamStandings =>
                val teamName = (teamStandings \\ "team-name" \ "@name").text
                val teamCity = (teamStandings \\ "team-city" \ "@city").text
                val alias = (teamStandings \\ "team-name" \ "@alias").text
                //val teamCity = (teamStandings \\ "team-city" \ "@city").text
                val teamCode = (teamStandings \\ "team-code" \ "@global-id").text
                val wins = toInt((teamStandings \\ "wins" \ "@number").text).getOrElse(0)
                val losses = toInt((teamStandings \\ "losses" \ "@number").text).getOrElse(0)
                val pct = toFloat((teamStandings \\ "winning-percentage" \ "@percentage").text).getOrElse(0f)
                val message = TeamStandings(alias, leagueStr, subLeague, division, teamName, teamCity, teamCode, wins, losses, pct)
                teamStandingsRows += new SourceRecord(in.sourcePartition, in.sourceOffset, in.topic, 0, in.keySchema, in.key, message.connectSchema, message.getStructure)
            }
        }
      }
    }
    teamStandingsRows.toList.asJava

  }

}