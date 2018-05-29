package com.slingmedia.sportscloud.parsers.leagues.delegates.schparser.impl.nhl

import com.eneco.trading.kafka.connect.ftp.source.SourceRecordConverter
import org.apache.kafka.connect.source.SourceRecord
import org.apache.kafka.connect.data.{ Schema, SchemaBuilder, Struct }
import scala.collection.JavaConverters._
import java.util
import scala.xml.Elem
import com.slingmedia.sportscloud.parsers.factory.ParsedItem
import com.slingmedia.sportscloud.parsers.leagues.delegates.schparser.impl.DefaultScheduleParserDelegate
import com.slingmedia.sportscloud.parsers.leagues.delegates.schparser.ScheduleDataExtractor
import com.slingmedia.sportscloud.parsers.leagues.delegates.schparser.ScheduleStructGenerator
import com.slingmedia.sportscloud.parsers.leagues.delegates.schparser.ScheduleSchemaGenerator
import org.slf4j.LoggerFactory;
import com.typesafe.scalalogging.slf4j.Logger

class NhlScheduleParserDelegate extends DefaultScheduleParserDelegate {
  private val log = LoggerFactory.getLogger("ScheduleParser")

  override def generateRows(data: Elem, in: SourceRecord, league: String, sport: String): java.util.List[SourceRecord] = {
    log.info("Parsing rows for nhl schedule parsing")
    val rows = (data \\ "game-schedule").map { rowData =>
      val commonFields = new ScheduleDataExtractor(data,rowData,league,sport)
      val gameId = (rowData \\ "gamecode" \ "@global-code").text
      commonFields.gameId=gameId
      val message = ProgramSchedule(commonFields)
      new SourceRecord(in.sourcePartition,
        in.sourceOffset,
        in.topic, 0,
        in.keySchema,
        in.key,
        message.connectSchema,
        message.getStructure)
    }
    log.info("Generated rows")
    rows.toList.asJava
  }


}