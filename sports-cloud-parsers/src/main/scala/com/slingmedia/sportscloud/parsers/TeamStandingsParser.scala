package com.slingmedia.sportscloud.parsers

import com.slingmedia.sportscloud.parsers.factory.ParsedItem
import scala.xml.Elem
import org.apache.kafka.connect.source.SourceRecord
import com.slingmedia.sportscloud.parsers.model.League
import scala.collection.JavaConverters._
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.data.Struct
import org.apache.kafka.connect.data.SchemaBuilder
import org.slf4j.LoggerFactory;
import com.typesafe.scalalogging.slf4j.Logger

class TeamStandingsParser extends ParsedItem {

  private val log = LoggerFactory.getLogger("ScheduleParser")

  def generateRows(data: Elem, in: SourceRecord): java.util.List[SourceRecord] = {
    val leagueStr = (data \\ "league" \ "@alias").toString
    val league = League.withNameOpt(leagueStr)
    league match {
      case Some(l) =>
        l match {
          case League.MLB =>
            var teamStandingsRows = scala.collection.mutable.ListBuffer.empty[SourceRecord]
            (data \\ "baseball-mlb-league-standings").map { mlbLeagueStandings =>
              val baseLeagueName = (mlbLeagueStandings \ "@league").toString
              val mlbDivisionStandingsRows = (mlbLeagueStandings \\ "baseball-mlb-division-standings").map {
                mlbDivisionStandings =>
                  val division = (mlbDivisionStandings \ "@division").toString
                   (mlbDivisionStandings \\ "baseball-mlb-team-standings").map {
                    teamStandings =>
                      val teamName = (teamStandings \\ "team-name" \ "@name").toString
                      val teamCity = (teamStandings \\ "team-city" \ "@city").toString
                      val teamCode = (teamStandings \\ "team-code" \ "@global-id").toString
                      val wins = (teamStandings \\ "wins" \ "@number").toString
                      val losses = (teamStandings \\ "losses" \ "@number").toString
                      val pct = (teamStandings \\ "winning-percentage" \ "@percentage").toString
                      val message = LeagueStandings(baseLeagueName, division, teamName, teamCity, teamCode, wins, losses, pct)
                     teamStandingsRows += new SourceRecord(in.sourcePartition, in.sourceOffset, in.topic, 0, in.keySchema, in.key, message.connectSchema, message.getStructure)
                  }                
              }
            }
            teamStandingsRows.toList.asJava
        }
      case None =>
        Array[SourceRecord]().toList.asJava
    }

  }

  case class LeagueStandings(league: String, division: String, teamName: String, teamCity: String, teamCode: String, wins: String, losses: String, pct: String) {

    val teamStandingSchema: Schema = SchemaBuilder.struct().name("c.s.s.s.TeamStandingSchema")
      .field("league", Schema.STRING_SCHEMA)
      .field("division", Schema.STRING_SCHEMA)
      .field("team-name", Schema.STRING_SCHEMA)
      .field("team-city", Schema.STRING_SCHEMA)
      .field("team-code", Schema.STRING_SCHEMA)
      .field("wins", Schema.STRING_SCHEMA)
      .field("losses", Schema.STRING_SCHEMA)
      .field("pct", Schema.STRING_SCHEMA)
      .build()
    val teamStandingStruct: Struct = new Struct(teamStandingSchema)
      .put("league", league)
      .put("division", division)
      .put("team-name", teamName)
      .put("team-city", teamCity)
      .put("team-code", teamCode)
      .put("wins", wins)
      .put("losses", losses)
      .put("pct", pct)

    val connectSchema: Schema = teamStandingSchema
    def getStructure: Struct = teamStandingStruct

  }

}