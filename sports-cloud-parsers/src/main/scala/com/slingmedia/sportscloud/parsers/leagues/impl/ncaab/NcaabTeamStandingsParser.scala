package com.slingmedia.sportscloud.parsers.leagues.impl.ncaab


import com.slingmedia.sportscloud.parsers.factory.ParsedItem
import com.slingmedia.sportscloud.parsers.leagues.delegates.ts.impl.ncaab.NcaabTeamStandingsParserDelegate
import scala.xml.Elem
import org.apache.kafka.connect.source.SourceRecord
import com.slingmedia.sportscloud.parsers.model.{ League , TeamStandings }
import scala.collection.JavaConverters._
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.data.Struct
import org.apache.kafka.connect.data.SchemaBuilder
import org.slf4j.LoggerFactory;
import com.typesafe.scalalogging.slf4j.Logger
import scala.xml.NodeSeq

class NcaabTeamStandingsParser extends ParsedItem {

  private val log = LoggerFactory.getLogger("ScheduleParser")

  override def generateRows(data: Elem, in: SourceRecord, xmlRoot: NodeSeq): java.util.List[SourceRecord] = {
   new NcaabTeamStandingsParserDelegate().generateRows(data,in,xmlRoot)

  }

}