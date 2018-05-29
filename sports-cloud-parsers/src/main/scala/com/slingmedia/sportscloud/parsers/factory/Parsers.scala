package com.slingmedia.sportscloud.parsers.factory
import com.slingmedia.sportscloud.parsers.leagues.impl.mlb.{MlbBoxScoreParser, MlbTeamStandingsParser}
import com.slingmedia.sportscloud.parsers.leagues.impl.nba.{NbaBoxScoreParser, NbaPlayerGameStatsParser, NbaPlayerStatsParser, NbaTeamStandingsParser}
import com.slingmedia.sportscloud.parsers.leagues.impl.ncaab.{NcaabBoxScoreParser, NcaabPlayerGameStatsParser, NcaabTeamStandingsParser}
import com.slingmedia.sportscloud.parsers.leagues.impl.ncaaf.{NcaafBoxScoreParser, NcaafTeamStandingsParser}
import com.slingmedia.sportscloud.parsers.leagues.impl.nfl.{NflBoxScoreParser, NflTeamStandingsParser}
import com.slingmedia.sportscloud.parsers.leagues.impl.nhl.{NhlBoxScoreParser, NhlScheduleParser, NhlTeamStandingsParser}
import com.slingmedia.sportscloud.parsers.leagues.impl.{PlayerStatsParser, ScheduleParser}

object ParserType extends Enumeration {
  type ParserType = Value
  val MlbScheduleParser,
  MlbBoxScoreParser,
  MlbPlayerStatsParser,
  MlbTeamStandingsParser,
  NhlBoxScoreParser,
  NhlPlayerStatsParser,
  NhlTeamStandingsParser,
  NhlScheduleParser,
  NflScheduleParser,
  NflBoxScoreParser,
  NflTeamStandingsParser,
  NcaafBoxScoreParser,
  NcaafScheduleParser,
  NcaafTeamStandingsParser,
  NbaTeamStandingsParser,
  NbaPlayerStatsParser,
  NbaScheduleParser,
  NbaBoxScoreParser,
  NbaPlayerGameStatsParser,
  NcaabTeamStandingsParser,
  NcaabPlayerStatsParser,
  NcaabScheduleParser,
  NcaabBoxScoreParser,
  NcaabPlayerGameStatsParser,
  Default = Value

}

import com.slingmedia.sportscloud.parsers.DefaultParser
import com.slingmedia.sportscloud.parsers.factory.ParserType._

object Parsers {
  def apply(parserType: ParserType) = {
    parserType match {
      case ParserType.NflScheduleParser
           | ParserType.NcaafScheduleParser
           | ParserType.NbaScheduleParser
           | ParserType.NcaabScheduleParser
           | ParserType.MlbScheduleParser =>
        new ScheduleParser()
      case ParserType.NhlScheduleParser =>
        new NhlScheduleParser()
      case ParserType.MlbTeamStandingsParser =>
        new MlbTeamStandingsParser()
      case ParserType.NcaafTeamStandingsParser =>
        new NcaafTeamStandingsParser()
      case ParserType.NflTeamStandingsParser =>
        new NflTeamStandingsParser()
      case ParserType.MlbBoxScoreParser =>
        new MlbBoxScoreParser()
      case ParserType.NcaafBoxScoreParser =>
        new NcaafBoxScoreParser()
      case ParserType.NflBoxScoreParser =>
        new NflBoxScoreParser()
      case ParserType.NbaBoxScoreParser =>
        new NbaBoxScoreParser()
      case ParserType.NcaabBoxScoreParser =>
        new NcaabBoxScoreParser()
      case ParserType.NhlBoxScoreParser =>
        new NhlBoxScoreParser()
      case ParserType.NhlTeamStandingsParser =>
        new NhlTeamStandingsParser()
      case ParserType.MlbPlayerStatsParser
           | ParserType.NhlPlayerStatsParser =>
        new PlayerStatsParser()
      case ParserType.NbaPlayerStatsParser
           | ParserType.NcaabPlayerStatsParser =>
        new NbaPlayerStatsParser()
      case ParserType.NbaTeamStandingsParser =>
        new NbaTeamStandingsParser()
      case ParserType.NcaabTeamStandingsParser =>
        new NcaabTeamStandingsParser()
      case ParserType.NbaPlayerGameStatsParser =>
        new NbaPlayerGameStatsParser()
      case ParserType.NcaabPlayerGameStatsParser =>
        new NcaabPlayerGameStatsParser()
      case ParserType.Default =>
        new DefaultParser()
      case _ =>
        new DefaultParser()
    }
  }
}