package com.slingmedia.sportscloud.kafka.converters
import java.util

import com.eneco.trading.kafka.connect.ftp.source.SourceRecordConverter

import com.slingmedia.sportscloud.parsers.factory.{ParserType, Parsers}
import com.slingmedia.sportscloud.parsers.model.{League, LeagueEnum }

import org.apache.kafka.connect.source.SourceRecord
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}
import scala.util.matching.Regex
import scala.xml.Elem


trait ConverterBase {
  private val log = LoggerFactory.getLogger("ConverterBase")

  def loadXML(line: String): Try[Elem] = {    
      Try(scala.xml.XML.loadString(line))
  }

  def generateMetaInfoData(in: SourceRecord, league: League): java.util.List[SourceRecord] = {
    log.trace("Converting source for metabatch")
    val line = new String(in.value.asInstanceOf[Array[Byte]])
    val dataElem: Try[Elem] = loadXML(line)

    var teamStandings:Regex = "(?!.*)".r
    var playerStats:Regex = "(?!.*)".r
    var finalBoxScores:Regex = "(?!.*)".r

    var teamStandingsParserType = ParserType.Default
    var playerStatsParserType = ParserType.Default
    var boxScoreParserType = ParserType.Default

    var teamStandingsRoot=""
    var playerStatsRoot=""

    league match {
      case LeagueEnum.NFL =>
        teamStandings = ".*NFL_TEAM_STANDINGS\\.XML.*".r
        teamStandingsParserType=ParserType.NflTeamStandingsParser
        teamStandingsRoot="football-nfl-standings"
      case LeagueEnum.NCAAF =>
        teamStandings = ".*CFB_TEAM_STANDINGS\\.XML.*".r
        teamStandingsParserType=ParserType.NcaafTeamStandingsParser
        teamStandingsRoot="cfb-division-standings"
      case LeagueEnum.MLB =>
        teamStandings = ".*MLB_TEAM_STANDINGS\\.XML.*".r
        playerStats = ".*MLB_PLAYER_STATS.*\\.XML.*".r
        finalBoxScores = ".*MLB_FINALBOX.*\\.XML.*".r
        teamStandingsParserType=ParserType.MlbTeamStandingsParser
        playerStatsParserType=ParserType.MlbPlayerStatsParser
        boxScoreParserType=ParserType.MlbBoxScoreParser
        teamStandingsRoot="baseball-mlb-league-standings"
        playerStatsRoot="baseball-mlb-player-stats"
      case LeagueEnum.NBA =>
        teamStandings = ".*NBA_TEAM_STANDINGS\\.XML.*".r
        playerStats = ".*NBA_PLAYER_STATS.*\\.XML.*".r
        playerStatsParserType=ParserType.NbaPlayerStatsParser
        teamStandingsParserType=ParserType.NbaTeamStandingsParser
        teamStandingsRoot="nba-conference-standings"
        playerStatsRoot="nba-player-split"
      case LeagueEnum.NCAAB =>
        teamStandings = ".*CBK_TEAM_STANDINGS\\.XML.*".r
        playerStats = ".*CBK_PLAYER_STATS.*\\.XML.*".r
        playerStatsParserType=ParserType.NcaabPlayerStatsParser
        teamStandingsParserType=ParserType.NcaabTeamStandingsParser
        teamStandingsRoot="cbk-conference-standings"
        playerStatsRoot="cbk-player-stat"



      case _ => throw new UnsupportedOperationException()
    }

    dataElem match {
      case Success(data) =>
        val fileName = in.key

        fileName match {
          case teamStandings(_*) =>
            if(teamStandingsParserType!=ParserType.Default) {
              Parsers(teamStandingsParserType).generateRows(data, in, (data \\ teamStandingsRoot))
            } else {
              Array[SourceRecord]().toList.asJava
            }
          case playerStats(_*) =>
            Parsers(playerStatsParserType).generateRows(data, in, (data \\ playerStatsRoot))
          case finalBoxScores(_*) =>
            Parsers(boxScoreParserType).generateRows(data, in)
          case _ =>
            Array[SourceRecord]().toList.asJava
        }
      case Failure(e) =>
        log.error("Error occurred in parsing xml ",e)
        Array[SourceRecord]().toList.asJava
    }


  }

  def generateLiveInfoData(in: SourceRecord, league: League): java.util.List[SourceRecord] = {

    log.trace("Converting source for livegame info for NFL")
    val line = new String(in.value.asInstanceOf[Array[Byte]])
    val dataElem: Try[Elem] = loadXML(line)
    var boxScore:Regex = null
    var liveData:Regex = null
    var finalBoxScore:Regex = null
    var liveScoreRoot = ""
    var boxScoreRoot = ""
    var parserType = ParserType.Default
    league match {
      case LeagueEnum.NFL =>
        boxScore = ".*NFL.*_BOXSCORE.*\\.XML.*".r
        liveData = ".*NFL.*_LIVE.*\\.XML.*".r
        finalBoxScore = ".*NFL.*_FINALBOX.*\\.XML.*".r
        liveScoreRoot= "nfl-score"
        boxScoreRoot="nfl-boxscore"
        parserType=ParserType.NflBoxScoreParser
      case LeagueEnum.NCAAF =>
        boxScore = ".*CFB.*_BOXSCORE.*\\.XML.*".r
        liveData = ".*CFB.*_LIVE.*\\.XML.*".r
        finalBoxScore = ".*CFB.*FINALBOX.*\\.XML.*".r
        liveScoreRoot= "cfb-score"
        boxScoreRoot="cfb-boxscore"
        parserType=ParserType.NcaafBoxScoreParser
      case LeagueEnum.MLB =>
        boxScore = ".*MLB.*_BOXSCORE.*\\.XML.*".r
        liveData = ".*MLB.*_LIVE.*\\.XML.*".r
        finalBoxScore = ".*MLB.*FINALBOX.*\\.XML.*".r
        liveScoreRoot= "baseball-mlb-score"
        boxScoreRoot="baseball-mlb-boxscore"
        parserType=ParserType.MlbBoxScoreParser
      case LeagueEnum.NBA =>
        boxScore = ".*NBA.*_BOXSCORE.*\\.XML.*".r
        liveData = ".*NBA.*_LIVE.*\\.XML.*".r
        finalBoxScore = ".*NBA.*FINALBOX.*\\.XML.*".r
        liveScoreRoot= "nba-score"
        boxScoreRoot="nba-boxscore"
        parserType=ParserType.NbaBoxScoreParser
      case LeagueEnum.NCAAB =>
        boxScore = ".*CBK.*_BOXSCORE.*\\.XML.*".r
        liveData = ".*CBK.*_LIVE.*\\.XML.*".r
        finalBoxScore = ".*CBK.*FINALBOX.*\\.XML.*".r
        liveScoreRoot= "cbk-score"
        boxScoreRoot="cbk-boxscore"
        parserType=ParserType.NcaabBoxScoreParser
      case _ => throw new UnsupportedOperationException()
    }
    dataElem match {
      case Success(data) =>
        val fileName = in.key
        fileName match {
          case boxScore(_*) | finalBoxScore(_*)  =>
            Parsers(parserType).generateRows(data, in, (data \\ boxScoreRoot))
          case liveData(_*) =>
            Parsers(parserType).generateRows(data, in, (data \\ liveScoreRoot))
          case _ =>
            Array[SourceRecord]().toList.asJava
        }
      case Failure(e) =>
        log.error("Error occurred in parsing xml ",e)
        Array[SourceRecord]().toList.asJava
    }
  }

  def generateContentMatchData(in: SourceRecord, league: League): java.util.List[SourceRecord] = {
    log.info("Converting source for content match info")
    val line = new String(in.value.asInstanceOf[Array[Byte]])
    val dataElem: Try[Elem] = loadXML(line)

    var schedule:Regex = null
    var parserType = ParserType.Default
    league match {
      case LeagueEnum.NFL =>
        schedule=".*NFL_SCHEDULE.*\\.XML.*".r
        parserType=ParserType.NflScheduleParser
      case LeagueEnum.NCAAF =>
        schedule=".*CFB_SCHEDULE.*\\.XML.*".r
        parserType=ParserType.NcaafScheduleParser
      case LeagueEnum.NBA =>
        schedule=".*NBA_SCHEDULE.*\\.XML.*".r
        parserType=ParserType.NbaScheduleParser
      case LeagueEnum.NCAAB =>
        schedule=".*CBK_SCHEDULE.*\\.XML.*".r
        parserType=ParserType.NcaabScheduleParser
      case LeagueEnum.MLB =>
        schedule=".*MLB_SCHEDULE\\.XML.*".r
        parserType=ParserType.MlbScheduleParser
      case _ => throw new UnsupportedOperationException()
    }

    dataElem match {
      case Success(data) =>
        val fileName = in.key
        fileName match {
          //case str : String => str match {
          case schedule(_*) =>
            Parsers(parserType).generateRows(data, in, league.name, league.fullName)
          case _ =>
            Array[SourceRecord]().toList.asJava
        }
      //case _ =>
      //Array[SourceRecord]().toList.asJava
      //}
      case Failure(e) =>
        log.error("Error occurred in parsing xml ", e)
        Array[SourceRecord]().toList.asJava
    }
  }

}