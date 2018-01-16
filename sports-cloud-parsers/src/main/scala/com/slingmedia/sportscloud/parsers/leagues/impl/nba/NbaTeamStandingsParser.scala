package com.slingmedia.sportscloud.parsers.leagues.impl.nba

import scala.xml.Elem
import scala.collection.JavaConverters._
import scala.xml.NodeSeq

import com.slingmedia.sportscloud.parsers.factory.ParsedItem
import com.slingmedia.sportscloud.parsers.model.{ League , TeamStandings }
import com.slingmedia.sportscloud.parsers.leagues.delegates.ts.impl.nba.NbaTeamStandingsParserDelegate

import org.apache.kafka.connect.source.SourceRecord
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.data.Struct
import org.apache.kafka.connect.data.SchemaBuilder

import org.slf4j.LoggerFactory;
import com.typesafe.scalalogging.slf4j.Logger

class NbaTeamStandingsParser extends ParsedItem {

  private val log = LoggerFactory.getLogger("NbaTeamStandingsParser")

  override def generateRows(data: Elem, in: SourceRecord, xmlRoot: NodeSeq): java.util.List[SourceRecord] = {
   new NbaTeamStandingsParserDelegate().generateRows(data,in,xmlRoot)
  }

}