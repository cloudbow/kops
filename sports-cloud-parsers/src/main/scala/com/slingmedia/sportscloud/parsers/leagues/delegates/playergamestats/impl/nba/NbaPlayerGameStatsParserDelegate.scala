package com.slingmedia.sportscloud.parsers.leagues.delegates.playergamestats.impl.nba

import java.util

import com.slingmedia.sportscloud.parsers.factory.ParsedItem
import com.slingmedia.sportscloud.parsers.leagues.delegates.playergamestats.impl.TeamType
import com.slingmedia.sportscloud.parsers.leagues.delegates.playergamestats.impl.TeamType.TeamType
import org.apache.kafka.connect.data.Struct
import org.apache.kafka.connect.source.SourceRecord
import org.slf4j.LoggerFactory

import scala.xml.{Elem, NodeSeq}

/**
  * Handles parsing Box score of NBA game and extracts key player stats.
  * The stats includes team-id, game-id to uniquely identify player details.
  * generateRows() returns A sequence of SourceRecords of home/visiting player stats
  */
class NbaPlayerGameStatsParserDelegate extends ParsedItem {

  private val log = LoggerFactory.getLogger("NbaBoxScorePlayerStatsParserDelegate")

  override def generateRows(data: Elem, in: SourceRecord, nbaBoxScoreSeq: NodeSeq): util.List[SourceRecord] = {
    log.info("Parsing rows for Nba PlayerStats of a game")

    val records:util.List[SourceRecord] = new util.ArrayList[SourceRecord]()

    // Iterate for each "nba-boxscore" node
    nbaBoxScoreSeq.map { nbaBoxScoreNode =>

      val buildListFor = (tag:String, teamType: TeamType) => {
        // Iterate for each player stats node
        (nbaBoxScoreNode \\ "player-stats" \\ tag).map { nbaPlayerStatsNode =>
          val nbaHomePlayerGameStats = new NbaPlayerGameStats(data, nbaBoxScoreNode, nbaPlayerStatsNode, teamType)
          records.add(createRecord(in, nbaHomePlayerGameStats.toStruct)) // add this to list of player records
        }
      }
      buildListFor("home-player-stats", TeamType.HOME)
      buildListFor("visiting-player-stats", TeamType.VISITING)
    }
    records // Return combined list of home/visiting player stats records
  }

  /**
    * Create an instance of SourceRecord with player stats struct
    * @param in SourceRecord containing topic, offset details
    * @param nbaPlayerStatsStruct Player status details Structure
    * @return Player stats SourceRecord
    */
  private def createRecord(in: SourceRecord, nbaPlayerStatsStruct: Struct): SourceRecord = {
    new SourceRecord(in.sourcePartition,
      in.sourceOffset,
      in.topic,
      0,
      in.keySchema,
      in.key,
      NbaPlayerGameStats.SCHEMA,
      nbaPlayerStatsStruct)
  }
}
