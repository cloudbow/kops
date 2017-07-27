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
import scala.xml.NodeSeq

class TeamStandingsParser extends ParsedItem {

  private val log = LoggerFactory.getLogger("ScheduleParser")

  override def generateRows(data: Elem, in: SourceRecord,xmlRoot:NodeSeq): java.util.List[SourceRecord] = {
    val leagueStr = (data \\ "league" \ "@alias").toString
    val league = League.withNameOpt(leagueStr.toUpperCase)

    var teamStandingsRows = scala.collection.mutable.ListBuffer.empty[SourceRecord]
   xmlRoot.map { leagueStandings =>
      val baseLeagueName = (leagueStandings \ "@league").toString
      val mlbDivisionStandingsRows = (leagueStandings \\ "baseball-mlb-division-standings").map {
        mlbDivisionStandings =>
          val division = (mlbDivisionStandings \ "@division").toString
          (mlbDivisionStandings \\ "baseball-mlb-team-standings").map {
            teamStandings =>
              val teamName = (teamStandings \\ "team-name" \ "@name").toString
              val teamCity = (teamStandings \\ "team-city" \ "@city").toString
              val teamCode = (teamStandings \\ "team-code" \ "@global-id").toString
              val wins = toInt((teamStandings \\ "wins" \ "@number").text).getOrElse(0)
              val losses = toInt((teamStandings \\ "losses" \ "@number").text).getOrElse(0)
              val pct = toFloat((teamStandings \\ "winning-percentage" \ "@percentage").text).getOrElse(0f)
              val message = LeagueStandings(baseLeagueName, division, teamName, teamCity, teamCode, wins, losses, pct)
              teamStandingsRows += new SourceRecord(in.sourcePartition, in.sourceOffset, in.topic, 0, in.keySchema, in.key, message.connectSchema, message.getStructure)
          }
      }
    }
    teamStandingsRows.toList.asJava

  }

  case class LeagueStandings(league: String, division: String, teamName: String, teamCity: String, teamCode: String, wins: Int, losses: Int, pct: Float) {

    val teamStandingSchema: Schema = SchemaBuilder.struct().name("c.s.s.s.TeamStandingSchema")
      .field("league", Schema.STRING_SCHEMA)
      .field("division", Schema.STRING_SCHEMA)
      .field("teamName", Schema.STRING_SCHEMA)
      .field("teamCity", Schema.STRING_SCHEMA)
      .field("teamCode", Schema.STRING_SCHEMA)
      .field("wins", Schema.INT32_SCHEMA)
      .field("losses", Schema.INT32_SCHEMA)
      .field("pct", Schema.FLOAT32_SCHEMA)
      .build()
    val teamStandingStruct: Struct = new Struct(teamStandingSchema)
      .put("league", league)
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