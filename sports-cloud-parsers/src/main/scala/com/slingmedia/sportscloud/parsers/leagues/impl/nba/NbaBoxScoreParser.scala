package com.slingmedia.sportscloud.parsers.leagues.impl.nba

import com.slingmedia.sportscloud.parsers.factory.ParsedItem
import com.slingmedia.sportscloud.parsers.leagues.delegates.boxscore.impl.nba.NbaBoxScoreParserDelegate
import org.apache.kafka.connect.source.SourceRecord
import org.slf4j.LoggerFactory

import scala.xml.{Elem, NodeSeq}



class NbaBoxScoreParser extends ParsedItem {

  private val log = LoggerFactory.getLogger("NbaBoxScoreParser")

  override def generateRows(data: Elem, in: SourceRecord, xmlRoot: NodeSeq): java.util.List[SourceRecord] = {
    log.info("Parser NbaBoxScoreParser")
    new NbaBoxScoreParserDelegate().generateRows(data,in,xmlRoot,"quarter")
  }

}