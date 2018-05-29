package com.slingmedia.sportscloud.parsers.leagues.impl.ncaaf

import com.slingmedia.sportscloud.parsers.factory.ParsedItem
import com.slingmedia.sportscloud.parsers.leagues.delegates.boxscore.impl.nfl.NflBoxScoreParserDelegate
import org.apache.kafka.connect.source.SourceRecord
import org.slf4j.LoggerFactory

import scala.xml.Elem

class NcaafBoxScoreParser extends ParsedItem {

  private val log = LoggerFactory.getLogger("NcaafBoxScoreParser")

  override def generateRows(data: Elem, in: SourceRecord, xmlRoot: scala.xml.NodeSeq): java.util.List[SourceRecord] = {
    log.info("Running NcaafBoxScoreParser")
    new NflBoxScoreParserDelegate().generateRows(data,in,xmlRoot,12,48)
  }

}