package com.slingmedia.sportscloud.parsers.leagues.impl.nba

import scala.xml.Elem
import scala.collection.JavaConverters._
import scala.xml.NodeSeq

import com.slingmedia.sportscloud.parsers.factory.ParsedItem
import com.slingmedia.sportscloud.parsers.model.{ League , TeamStandings }
import com.slingmedia.sportscloud.parsers.leagues.delegates.playerstats.impl.nba.NbaPlayerStatsParserDelegate

import org.apache.kafka.connect.source.SourceRecord
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.data.Struct
import org.apache.kafka.connect.data.SchemaBuilder

import org.slf4j.LoggerFactory;
import com.typesafe.scalalogging.slf4j.Logger

class NbaPlayerStatsParser extends ParsedItem {

  private val log = LoggerFactory.getLogger("NbaPlayerStatsParser")

  override def generateRows(data: Elem, in: SourceRecord, xmlRoot: NodeSeq, league: String, sport: String): java.util.List[SourceRecord] = {
    log.info("Running NbaPlayerStatsParser")
    new NbaPlayerStatsParserDelegate().generateRows(data,in,xmlRoot, league: String, sport: String)
  }

}