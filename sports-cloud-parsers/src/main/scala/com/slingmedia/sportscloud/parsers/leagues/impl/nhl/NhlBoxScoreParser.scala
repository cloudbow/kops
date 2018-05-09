package com.slingmedia.sportscloud.parsers.leagues.impl.nhl

import com.slingmedia.sportscloud.parsers.factory.ParsedItem
import com.slingmedia.sportscloud.parsers.leagues.delegates.boxscore.impl.nhl.NhlBoxScoreParserDelegate
import org.apache.kafka.connect.source.SourceRecord
import org.slf4j.LoggerFactory

import scala.xml.{Elem, NodeSeq}



class NhlBoxScoreParser extends ParsedItem {

  private val log = LoggerFactory.getLogger("NhlBoxScoreParser")

  override def generateRows(data: Elem, in: SourceRecord, xmlRoot: NodeSeq): java.util.List[SourceRecord] = {
    log.info("Running NhlBoxScoreParser")
    new NhlBoxScoreParserDelegate().generateRows(data,in,xmlRoot)
  }

}