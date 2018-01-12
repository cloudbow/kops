package com.slingmedia.sportscloud.parsers.leagues.impl.nfl

import com.slingmedia.sportscloud.parsers.factory.ParsedItem
import com.slingmedia.sportscloud.parsers.leagues.delegates.ts.impl.nfl.NflTeamStandingsParserDelegate
import com.slingmedia.sportscloud.parsers.model.{ League , TeamStandings }
import org.apache.kafka.connect.data.{Schema, SchemaBuilder, Struct}
import org.apache.kafka.connect.source.SourceRecord
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.xml.{Elem, NodeSeq}

class NflTeamStandingsParser extends ParsedItem {

  private val log = LoggerFactory.getLogger("ScheduleParser")

  override def generateRows(data: Elem, in: SourceRecord, xmlRoot: NodeSeq): java.util.List[SourceRecord] = {
   log.trace("Running NflTeamStandingsParser")
    new NflTeamStandingsParserDelegate().generateRows(data,in,xmlRoot)
  }

}