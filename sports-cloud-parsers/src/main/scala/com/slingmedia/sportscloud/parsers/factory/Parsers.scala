package com.slingmedia.sportscloud.parsers.factory


import com.slingmedia.sportscloud.parsers.{ ScheduleParser, NcaafScheduleParser, NflScheduleParser, TeamStandingsParser, NcaafTeamStandingsParser, NflTeamStandingsParser, BoxScoreParser,NcaafBoxScoreParser, NflBoxScoreParser, PlayerStatsParser, NcaafPlayerStatsParser, DefaultParser }

object ParserType extends Enumeration {
  type ParserType = Value
  val ScheduleParser, NcaafScheduleParser, NflScheduleParser, TeamStandingsParser, NcaafTeamStandingsParser, NflTeamStandingsParser,BoxScoreParser, NcaafBoxScoreParser,
  NflBoxScoreParser,PlayerStatsParser, NcaafPlayerStatsParser, LiveParser, Default = Value

}

import ParserType._
import com.slingmedia.sportscloud.parsers.DefaultParser

object Parsers {
  def apply(parserType: ParserType) = {
    parserType match {
      case ParserType.ScheduleParser =>
        new ScheduleParser()
      case ParserType.NcaafScheduleParser =>
        new NcaafScheduleParser()
      case ParserType.NflScheduleParser =>
        new NflScheduleParser()
      case ParserType.TeamStandingsParser =>
        new TeamStandingsParser()
      case ParserType.NcaafTeamStandingsParser =>
        new NcaafTeamStandingsParser()
      case ParserType.NflTeamStandingsParser =>
        new NflTeamStandingsParser()
      case ParserType.BoxScoreParser =>
        new BoxScoreParser()
      case ParserType.NcaafBoxScoreParser =>
        new NcaafBoxScoreParser()
      case ParserType.NflBoxScoreParser =>
        new NflBoxScoreParser()
      case ParserType.PlayerStatsParser =>
        new PlayerStatsParser()
      case ParserType.NcaafPlayerStatsParser =>
        new NcaafPlayerStatsParser()
      case ParserType.LiveParser =>
        new BoxScoreParser()
      case ParserType.Default =>
        new DefaultParser()
      case _ =>
        new DefaultParser()
    }
  }
}