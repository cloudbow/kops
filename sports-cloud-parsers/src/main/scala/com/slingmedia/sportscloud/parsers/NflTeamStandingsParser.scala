package com.slingmedia.sportscloud.parsers

import com.slingmedia.sportscloud.parsers.factory.ParsedItem
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
                val message = NflLeagueStandings(alias, leagueStr, subLeague, division, teamName, teamCity, teamCode, wins, losses, pct)
                teamStandingsRows += new SourceRecord(in.sourcePartition, in.sourceOffset, in.topic, 0, in.keySchema, in.key, message.connectSchema, message.getStructure)
            }
        }
      }
    }
    teamStandingsRows.toList.asJava

  }

  case class NflLeagueStandings(alias: String, league: String, subLeague: String, division: String, teamName: String, teamCity: String, teamCode: String, wins: Int, losses: Int, pct: Float) {

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

}