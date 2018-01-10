package com.slingmedia.sportscloud.parsers.leagues.impl.nba

import com.slingmedia.sportscloud.parsers.factory.ParsedItem
import scala.xml.Elem
import org.apache.kafka.connect.source.SourceRecord
import com.slingmedia.sportscloud.parsers.model.{ League , TeamStandings }
import scala.collection.JavaConverters._
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.data.Struct
import org.apache.kafka.connect.data.SchemaBuilder
import org.slf4j.LoggerFactory;
import com.typesafe.scalalogging.slf4j.Logger
import scala.xml.NodeSeq

class NbaTeamStandingsParser extends ParsedItem {

  private val log = LoggerFactory.getLogger("NbaTeamStandingsParser")

  override def generateRows(data: Elem, in: SourceRecord, xmlRoot: NodeSeq): java.util.List[SourceRecord] = {
    val leagueStr = (data \\ "league" \ "@alias").text

    var teamStandingsRows = scala.collection.mutable.ListBuffer.empty[SourceRecord]
    xmlRoot.map { leagueStandings =>
      val subLeague = (leagueStandings \ "@league").text
      val mlbDivisionStandingsRows = (leagueStandings \\ "nba-division-standings").map {
        mlbDivisionStandings =>
          val division = (mlbDivisionStandings \ "@division").text
          (mlbDivisionStandings \\ "nba-team-standings").map {
            teamStandings =>
              val teamName = (teamStandings \\ "team-name" \ "@name").text
              val alias = (teamStandings \\ "team-name" \ "@alias").text
              val teamCity = (teamStandings \\ "team-city" \ "@city").text
              val teamCode = (teamStandings \\ "team-code" \ "@global-id").text
              val wins = toInt((teamStandings \\ "wins" \ "@number").text).getOrElse(0)
              val losses = toInt((teamStandings \\ "losses" \ "@number").text).getOrElse(0)
              val pct = toFloat((teamStandings \\ "winning-percentage" \ "@percentage").text).getOrElse(0f)
              val message = TeamStandings(alias,leagueStr, subLeague, division, teamName, teamCity, teamCode, wins, losses, pct)
              teamStandingsRows += new SourceRecord(in.sourcePartition, in.sourceOffset, in.topic, 0, in.keySchema, in.key, message.connectSchema, message.getStructure)
          }
      }
    }
    teamStandingsRows.toList.asJava

  }

}