package com.slingmedia.sportscloud.parsers.leagues.impl.mlb

import com.slingmedia.sportscloud.parsers.factory.ParsedItem
import com.slingmedia.sportscloud.parsers.leagues.delegates.boxscore.impl.mlb.MlbBoxScoreParserDelegate
import org.apache.kafka.connect.source.SourceRecord
import org.slf4j.LoggerFactory

import scala.xml.Elem



class MlbBoxScoreParser extends ParsedItem {

  private val log = LoggerFactory.getLogger("BoxScoreParser")

  override def generateRows(data: Elem, in: SourceRecord, xmlRoot: scala.xml.NodeSeq): java.util.List[SourceRecord] = {
    log.info("Running MlbBoxScoreParser")
    new MlbBoxScoreParserDelegate().generateRows(data,in,xmlRoot)
  }

}