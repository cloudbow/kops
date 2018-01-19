package com.slingmedia.sportscloud.parsers.leagues.impl

import com.eneco.trading.kafka.connect.ftp.source.SourceRecordConverter
import org.apache.kafka.connect.source.SourceRecord
import org.apache.kafka.connect.data.{ Schema, SchemaBuilder, Struct }
import scala.collection.JavaConverters._
import java.util
import scala.xml.Elem
import com.slingmedia.sportscloud.parsers.factory.ParsedItem
import com.slingmedia.sportscloud.parsers.leagues.delegates.schparser.impl.DefaultScheduleParserDelegate

import org.slf4j.LoggerFactory;
import com.typesafe.scalalogging.slf4j.Logger

class ScheduleParser extends ParsedItem {
  private val log = LoggerFactory.getLogger("ScheduleParser")

  override def generateRows(data: Elem, in: SourceRecord, league: String, sport: String): java.util.List[SourceRecord] = {
    log.info("Running ScheduleParser")
    new DefaultScheduleParserDelegate().generateRows(data,in,league,sport)
  }


}