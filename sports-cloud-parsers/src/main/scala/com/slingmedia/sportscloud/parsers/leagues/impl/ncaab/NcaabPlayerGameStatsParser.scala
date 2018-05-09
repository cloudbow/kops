package com.slingmedia.sportscloud.parsers.leagues.impl.ncaab

import com.slingmedia.sportscloud.parsers.factory.ParsedItem
import com.slingmedia.sportscloud.parsers.leagues.delegates.playergamestats.impl.nba.NbaPlayerGameStatsParserDelegate
import org.apache.kafka.connect.source.SourceRecord
import org.slf4j.LoggerFactory

import scala.xml.{Elem, NodeSeq}

class NcaabPlayerGameStatsParser extends ParsedItem{
  private val log = LoggerFactory.getLogger("NcaabPlayerGameStatsParser")

  override def generateRows(data: Elem, in: SourceRecord, xmlRoot: NodeSeq): java.util.List[SourceRecord] = {
    log.info("Running NcaabPlayerGameStatsParser")
    new NbaPlayerGameStatsParserDelegate().generateRows(data,in,xmlRoot)
  }
}
