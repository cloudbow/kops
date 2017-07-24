package com.slingmedia.sportscloud.parsers.factory

import com.slingmedia.sportscloud.parsers.{ScheduleParser,TeamStandingsParser,DefaultParser}

object ParserType extends Enumeration {
  type ParserType = Value
  val ScheduleParser, TeamStandingsParser, Default = Value
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
      case ParserType.Default =>
        new DefaultParser()
      case _ =>
        new DefaultParser()
    }
  }
}