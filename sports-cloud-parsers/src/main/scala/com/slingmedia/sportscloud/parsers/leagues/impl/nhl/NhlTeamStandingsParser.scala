package com.slingmedia.sportscloud.parsers.leagues.impl.nhl

import com.slingmedia.sportscloud.parsers.factory.ParsedItem
import com.slingmedia.sportscloud.parsers.leagues.delegates.ts.impl.nhl.NhlTeamStandingsParserDelegate
import com.slingmedia.sportscloud.parsers.model.{ League , TeamStandings }
import org.apache.kafka.connect.data.{Schema, SchemaBuilder, Struct}
import org.apache.kafka.connect.source.SourceRecord
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.xml.{Elem, NodeSeq}

class NhlTeamStandingsParser extends ParsedItem {

  private val log = LoggerFactory.getLogger("NhlTeamStandingsParser")

  override def generateRows(data: Elem, in: SourceRecord, xmlRoot: NodeSeq): java.util.List[SourceRecord] = {
   log.trace("Running NhlTeamStandingsParser")
    new NhlTeamStandingsParserDelegate().generateRows(data,in,xmlRoot)
  }

}