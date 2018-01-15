package com.slingmedia.sportscloud.parsers.leagues.impl.nhl

import com.slingmedia.sportscloud.parsers.factory.ParsedItem
import com.slingmedia.sportscloud.parsers.leagues.delegates.schparser.impl.nhl.NhlScheduleParserDelegate

import scala.xml.{Elem, NodeSeq}
import org.apache.kafka.connect.source.SourceRecord

import org.slf4j.LoggerFactory;
import com.typesafe.scalalogging.slf4j.Logger



class NhlScheduleParser extends ParsedItem {

  private val log = LoggerFactory.getLogger("NhlScheduleParser")

  override def generateRows(data: Elem, in: SourceRecord, league: String, sport: String): java.util.List[SourceRecord] = {
    log.info("Running NhlScheduleParser")
    new NhlScheduleParserDelegate().generateRows(data,in,league,sport)
  }

}