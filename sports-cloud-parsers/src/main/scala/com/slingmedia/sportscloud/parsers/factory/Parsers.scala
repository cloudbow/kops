package com.slingmedia.sportscloud.parsers.factory
import com.slingmedia.sportscloud.parsers.leagues.impl.{ScheduleParser,PlayerStatsParser}
import com.slingmedia.sportscloud.parsers.leagues.impl.nba.{ NbaTeamStandingsParser }
import com.slingmedia.sportscloud.parsers.leagues.impl.ncaab.{ NcaabTeamStandingsParser }
import com.slingmedia.sportscloud.parsers.leagues.impl.mlb.{ MlbBoxScoreParser, MlbTeamStandingsParser}
import com.slingmedia.sportscloud.parsers.leagues.impl.nfl.{ NflTeamStandingsParser,NflBoxScoreParser }
import com.slingmedia.sportscloud.parsers.leagues.impl.ncaaf.{ NcaafTeamStandingsParser, NcaafBoxScoreParser }
import com.slingmedia.sportscloud.parsers.{ DefaultParser }

object ParserType extends Enumeration {
  type ParserType = Value
  val MlbScheduleParser,
  MlbBoxScoreParser,
  MlbPlayerStatsParser,
  MlbTeamStandingsParser,
  NflScheduleParser,
  NflBoxScoreParser,
  NflTeamStandingsParser,
  NcaafBoxScoreParser,
  NcaafScheduleParser,
  NcaafTeamStandingsParser,
  NbaTeamStandingsParser,
  NbaPlayerStatsParser,
  NbaScheduleParser,
  NcaabTeamStandingsParser,
  NcaabPlayerStatsParser,
  NcaabScheduleParser,
  Default = Value

}

import ParserType._
import com.slingmedia.sportscloud.parsers.DefaultParser

object Parsers {
  def apply(parserType: ParserType) = {
    parserType match {
      case ParserType.NflScheduleParser | ParserType.NcaafScheduleParser | ParserType.NbaScheduleParser | ParserType.NcaabScheduleParser  =>
        new ScheduleParser()
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
      case ParserType.MlbPlayerStatsParser | ParserType.NbaPlayerStatsParser | ParserType.NcaabPlayerStatsParser =>
        new PlayerStatsParser()
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