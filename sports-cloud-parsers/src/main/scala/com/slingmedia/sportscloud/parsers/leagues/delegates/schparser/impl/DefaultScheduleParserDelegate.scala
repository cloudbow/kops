package com.slingmedia.sportscloud.parsers.leagues.delegates.schparser.impl

import com.eneco.trading.kafka.connect.ftp.source.SourceRecordConverter
import org.apache.kafka.connect.source.SourceRecord
import org.apache.kafka.connect.data.{ Schema, SchemaBuilder, Struct }
import scala.collection.JavaConverters._
import java.util
import scala.xml.Elem
import com.slingmedia.sportscloud.parsers.factory.ParsedItem
import com.slingmedia.sportscloud.parsers.leagues.delegates.schparser.ScheduleDataExtractor
import com.slingmedia.sportscloud.parsers.leagues.delegates.schparser.ScheduleStructGenerator
import com.slingmedia.sportscloud.parsers.leagues.delegates.schparser.ScheduleSchemaGenerator
import org.slf4j.LoggerFactory;
import com.typesafe.scalalogging.slf4j.Logger

class DefaultScheduleParserDelegate extends ParsedItem {
  private val log = LoggerFactory.getLogger("ScheduleParser")

  override def generateRows(data: Elem, in: SourceRecord, league: String, sport: String): java.util.List[SourceRecord] = {
    log.info("Generating rows for schedule parsing")
    val rows = (data \\ "game-schedule").map { rowData =>
      val commonFields = new ScheduleDataExtractor(data,rowData, league, sport)
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

  case class ProgramSchedule(commonFields: ScheduleDataExtractor) {
    log.trace("preparing schema")
    val scheduleSchemaInited = SchemaBuilder.struct().name("c.s.s.s.Game")
    val scheduleCommonSchema = ScheduleSchemaGenerator(scheduleSchemaInited)
    val scheduleSchema: Schema = scheduleSchemaInited

    val connectSchema: Schema = scheduleSchema

    val scheduleStructInited = new Struct(scheduleSchema)
    val scheduleCommonStruct = ScheduleStructGenerator(scheduleStructInited,scheduleCommonSchema,commonFields)
    val scheduleStruct: Struct = scheduleStructInited

    def getStructure: Struct = scheduleStruct

    log.trace("prepared schema")
  }
}