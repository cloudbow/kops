package com.slingmedia.sportscloud.parsers.leagues.impl.ncaaf

import com.slingmedia.sportscloud.parsers.factory.ParsedItem
import scala.xml.Elem
import org.apache.kafka.connect.source.SourceRecord
import com.slingmedia.sportscloud.parsers.model.{ League , TeamStandings }
import com.slingmedia.sportscloud.parsers.leagues.delegates.ts.impl.ncaaf.NcaafTeamStandingsParserDelegate
import scala.collection.JavaConverters._
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.data.Struct
import org.apache.kafka.connect.data.SchemaBuilder
import org.slf4j.LoggerFactory;
import com.typesafe.scalalogging.slf4j.Logger
import scala.xml.NodeSeq

class NcaafTeamStandingsParser extends ParsedItem {

  private val log = LoggerFactory.getLogger("ScheduleParser")

  override def generateRows(data: Elem, in: SourceRecord, xmlRoot: NodeSeq): java.util.List[SourceRecord] = {
    log.trace("Running NcaafTeamStandingsParser")
    new NcaafTeamStandingsParserDelegate().generateRows(data,in,xmlRoot)

  }

}