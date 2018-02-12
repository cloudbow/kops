package com.slingmedia.sportscloud.parsers.factory
import com.slingmedia.sportscloud.parsers.leagues.impl.{ScheduleParser,PlayerStatsParser}
import com.slingmedia.sportscloud.parsers.leagues.impl.nba.{ NbaTeamStandingsParser, NbaBoxScoreParser, NbaPlayerStatsParser }
import com.slingmedia.sportscloud.parsers.leagues.impl.ncaab.{ NcaabTeamStandingsParser, NcaabBoxScoreParser }
import com.slingmedia.sportscloud.parsers.leagues.impl.mlb.{ MlbBoxScoreParser, MlbTeamStandingsParser}
import com.slingmedia.sportscloud.parsers.leagues.impl.nfl.{ NflTeamStandingsParser,NflBoxScoreParser }
import com.slingmedia.sportscloud.parsers.leagues.impl.ncaaf.{ NcaafTeamStandingsParser, NcaafBoxScoreParser }
import com.slingmedia.sportscloud.parsers.leagues.impl.nhl.{ NhlTeamStandingsParser, NhlBoxScoreParser, NhlScheduleParser }

import com.slingmedia.sportscloud.parsers.{ DefaultParser }

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
  NcaabTeamStandingsParser,
  NcaabPlayerStatsParser,
  NcaabScheduleParser,
  NcaabBoxScoreParser,
  Default = Value

}

import ParserType._
import com.slingmedia.sportscloud.parsers.DefaultParser

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
           | ParserType.NcaabPlayerStatsParser
           | ParserType.NhlPlayerStatsParser =>
        new PlayerStatsParser()
      case ParserType.NbaPlayerStatsParser =>
        new NbaPlayerStatsParser()
      case ParserType.NbaTeamStandingsParser =>
        new NbaTeamStandingsParser()
      case ParserType.NcaabTeamStandingsParser =>
        new NcaabTeamStandingsParser()
      case ParserType.Default =>
        new DefaultParser()
      case _ =>
        new DefaultParser()
    }
  }
}