package com.slingmedia.sportscloud.parsers.leagues.impl.nhl

import com.slingmedia.sportscloud.parsers.factory.ParsedItem

import com.slingmedia.sportscloud.parsers.leagues.delegates.boxscore.impl.nhl.NhlBoxScoreParserDelegate

import scala.xml.{Elem, NodeSeq}
import org.apache.kafka.connect.source.SourceRecord

import org.slf4j.LoggerFactory;
import com.typesafe.scalalogging.slf4j.Logger



class NhlBoxScoreParser extends ParsedItem {

  private val log = LoggerFactory.getLogger("NbaBoxScoreParser")

  override def generateRows(data: Elem, in: SourceRecord, xmlRoot: NodeSeq): java.util.List[SourceRecord] = {
    log.trace("Parsing rows for boxscore")
    new NhlBoxScoreParserDelegate().generateRows(data,in,xmlRoot)
  }

}