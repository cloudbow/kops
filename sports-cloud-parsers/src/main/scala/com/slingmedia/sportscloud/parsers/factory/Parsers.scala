package com.slingmedia.sportscloud.parsers.factory

import com.slingmedia.sportscloud.parsers.mlb.{ MlbScheduleParser, MlbBoxScoreParser,MlbPlayerStatsParser, MlbTeamStandingsParser}
import com.slingmedia.sportscloud.parsers.nfl.{ NflScheduleParser, NflTeamStandingsParser,NflBoxScoreParser }
import com.slingmedia.sportscloud.parsers.ncaaf.{  NcaafScheduleParser, NcaafTeamStandingsParser, NcaafBoxScoreParser }
import com.slingmedia.sportscloud.parsers.{ DefaultParser }

object ParserType extends Enumeration {
  type ParserType = Value
  val MlbScheduleParser, NcaafScheduleParser, NflScheduleParser, MlbTeamStandingsParser, NcaafTeamStandingsParser, NflTeamStandingsParser,MlbBoxScoreParser, NcaafBoxScoreParser,
  NflBoxScoreParser,MlbPlayerStatsParser, LiveParser, Default = Value

}

import ParserType._
import com.slingmedia.sportscloud.parsers.DefaultParser

object Parsers {
  def apply(parserType: ParserType) = {
    parserType match {
      case ParserType.MlbScheduleParser =>
        new MlbScheduleParser()
      case ParserType.NcaafScheduleParser =>
        new NcaafScheduleParser()
      case ParserType.NflScheduleParser =>
        new NflScheduleParser()
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
      case ParserType.MlbPlayerStatsParser =>
        new MlbPlayerStatsParser()
      case ParserType.LiveParser =>
        new MlbBoxScoreParser()
      case ParserType.Default =>
        new DefaultParser()
      case _ =>
        new DefaultParser()
    }
  }
}