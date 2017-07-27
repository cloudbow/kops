package com.slingmedia.sportscloud.parsers

import com.eneco.trading.kafka.connect.ftp.source.SourceRecordConverter
import org.apache.kafka.connect.source.SourceRecord
import org.apache.kafka.connect.data.{ Schema, SchemaBuilder, Struct }
import scala.collection.JavaConverters._
import java.util
import scala.xml.Elem
import com.slingmedia.sportscloud.parsers.factory.ParsedItem
import org.slf4j.LoggerFactory;
import com.typesafe.scalalogging.slf4j.Logger
import scala.xml.NodeSeq

class PlayerStatsParser extends ParsedItem {
  private val log = LoggerFactory.getLogger("ScheduleParser")

  override def generateRows(data: Elem, in: SourceRecord,xmlRoot:NodeSeq): java.util.List[SourceRecord] = {
    log.trace("Generating rows for schedule parsing")
    val rows = xmlRoot.map { rowData =>
      val playerExternalId = (rowData \\ "player-code" \ "@global-id").toString
      val playerWins = toInt((rowData  \\ "wins" \ "@number").text).getOrElse(0)
      val playerLosses = toInt((rowData  \\ "losses" \ "@number").text).getOrElse(0)
      val message = PlayerStats(playerExternalId, playerWins, playerLosses)
      new SourceRecord(in.sourcePartition, in.sourceOffset, in.topic, 0, in.keySchema, in.key, message.connectSchema, message.getStructure)
    }
    log.trace("Generated rows")
    rows.toList.asJava
  }

  case class PlayerStats(playerExternalId: String, playerWins: Int, playerLosses: Int) {

    val playerStatusSchema: Schema = SchemaBuilder.struct().name("c.s.s.s.PlayerStats")
      .field("playerCode", Schema.STRING_SCHEMA)
      .field("wins", Schema.INT32_SCHEMA)
      .field("losses", Schema.INT32_SCHEMA)
      .build()
    val playerStatusStruct: Struct = new Struct(playerStatusSchema)
      .put("playerCode", playerExternalId)
      .put("wins", playerWins)
      .put("losses", playerLosses)

    val connectSchema: Schema = playerStatusSchema
    def getStructure: Struct = playerStatusStruct

  }
}