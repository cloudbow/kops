package com.slingmedia.sportscloud.parsers.factory

import com.slingmedia.sportscloud.parsers.{ ScheduleParser, TeamStandingsParser, BoxScoreParser, PlayerStatsParser, DefaultParser }

object ParserType extends Enumeration {
  type ParserType = Value
  val ScheduleParser, TeamStandingsParser, BoxScoreParser,  PlayerStatsParser,LiveParser, Default = Value
}

import ParserType._
import com.slingmedia.sportscloud.parsers.DefaultParser

object Parsers {
  def apply(parserType: ParserType) = {
    parserType match {
      case ParserType.ScheduleParser =>
        new ScheduleParser()
      case ParserType.TeamStandingsParser =>
        new TeamStandingsParser()
      case ParserType.BoxScoreParser =>
        new BoxScoreParser()
      case ParserType.PlayerStatsParser =>
        new PlayerStatsParser()
      case ParserType.LiveParser =>
        new BoxScoreParser()
      case ParserType.Default =>
        new DefaultParser()
      case _ =>
        new DefaultParser()
    }
  }
}