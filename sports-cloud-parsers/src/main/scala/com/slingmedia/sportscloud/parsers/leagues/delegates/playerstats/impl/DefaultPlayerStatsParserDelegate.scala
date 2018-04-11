package com.slingmedia.sportscloud.parsers.leagues.delegates.playerstats.impl

import com.eneco.trading.kafka.connect.ftp.source.SourceRecordConverter
import org.apache.kafka.connect.source.SourceRecord
import org.apache.kafka.connect.data.{ Schema, SchemaBuilder, Struct }
import scala.collection.JavaConverters._
import java.util
import scala.xml.Elem
import com.slingmedia.sportscloud.parsers.factory.ParsedItem
import com.slingmedia.sportscloud.parsers.leagues.delegates.playerstats.PlayerStatsDataExtractor
import com.slingmedia.sportscloud.parsers.leagues.delegates.playerstats.PlayerStatsStructGenerator
import com.slingmedia.sportscloud.parsers.leagues.delegates.playerstats.PlayerStatsSchemaGenerator
import org.slf4j.LoggerFactory;
import com.typesafe.scalalogging.slf4j.Logger

class DefaultPlayerStatsParserDelegate extends ParsedItem {
  private val log = LoggerFactory.getLogger("DefaultPlayerStatsParserDelegate")

  override def generateRows(data: Elem, in: SourceRecord, xmlRoot: scala.xml.NodeSeq, league: String, sport: String): java.util.List[SourceRecord] = {
    log.info(s"Parsing rows for player stats parsing for league $league sport $sport")
    val rows = (xmlRoot).map { rowData =>
      val commonFields = new PlayerStatsDataExtractor(data,rowData, league, sport)
      val message = PlayerStatsData(commonFields)
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

  case class PlayerStatsData(commonFields: PlayerStatsDataExtractor) {
    log.trace("preparing schema")
    val playerStatsSchemaInited = SchemaBuilder.struct().name("c.s.s.s.PlayerStats")
    val playerStatsCommonSchema = PlayerStatsSchemaGenerator(playerStatsSchemaInited)
    val playerStatsSchema: Schema = playerStatsSchemaInited

    val connectSchema: Schema = playerStatsSchema

    val playerStatsStructInited = new Struct(playerStatsSchema)
    val playerStatsCommonStruct = PlayerStatsStructGenerator(playerStatsStructInited,playerStatsCommonSchema,commonFields)
    val playerStatsStruct: Struct = playerStatsStructInited

    def getStructure: Struct = playerStatsStruct

    log.trace("prepared schema")
  }
}