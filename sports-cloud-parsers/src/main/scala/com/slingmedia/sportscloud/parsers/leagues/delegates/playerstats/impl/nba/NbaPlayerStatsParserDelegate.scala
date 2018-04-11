package com.slingmedia.sportscloud.parsers.leagues.delegates.playerstats.impl.nba

import com.eneco.trading.kafka.connect.ftp.source.SourceRecordConverter
import org.apache.kafka.connect.source.SourceRecord
import org.apache.kafka.connect.data.{ Schema, SchemaBuilder, Struct }
import scala.collection.JavaConverters._
import java.util
import scala.xml.Elem
import com.slingmedia.sportscloud.parsers.factory.ParsedItem
import com.slingmedia.sportscloud.parsers.leagues.delegates.playerstats.impl.DefaultPlayerStatsParserDelegate
import com.slingmedia.sportscloud.parsers.leagues.delegates.playerstats.PlayerStatsDataExtractor
import com.slingmedia.sportscloud.parsers.leagues.delegates.playerstats.PlayerStatsStructGenerator
import com.slingmedia.sportscloud.parsers.leagues.delegates.playerstats.PlayerStatsSchemaGenerator
import org.slf4j.LoggerFactory;
import com.typesafe.scalalogging.slf4j.Logger

class NbaPlayerStatsParserDelegate extends DefaultPlayerStatsParserDelegate {
  private val log = LoggerFactory.getLogger("NbaPlayerStatsDelegate")

  override def generateRows(data: Elem, in: SourceRecord, xmlRoot: scala.xml.NodeSeq, league: String, sport: String): java.util.List[SourceRecord] = {
    log.info("Parsing rows for nba player stats parsing")
    val rows = (xmlRoot).map { rowData =>
      val commonFields = new PlayerStatsDataExtractor(data,rowData,league,sport)
      val points = (rowData \\ "points" \\ "@points").text
      val assists = (rowData \\ "assists" \\ "@assists").text
      val blocks =  (rowData \\ "blocked-shots" \\ "@blocked-shots").text
      val fgMade = (rowData \\ "field-goals" \\ "@made").text
      val rebounds = (rowData \\ "rebounds" \\ "@total").text
      val steals = (rowData \\ "steals" \\ "@steals").text
      val threePtMade = (rowData \\ "three-point-field-goals" \\  "@made").text
      val message = NbaPlayerStatsData(commonFields,
        points,
        assists,
        blocks,
        fgMade,
        rebounds,
        steals,
        threePtMade)
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

  case class NbaPlayerStatsData(commonFields: PlayerStatsDataExtractor,
                                points: String,
                                assists: String,
                                blocks: String,
                                fgMade: String,
                                rebounds: String,
                                steals: String,
                                threePtMade: String) {
    log.trace("preparing schema")
    val playerStatsSchemaInited = SchemaBuilder.struct().name("c.s.s.s.NbaPlayerStats")
    val playerStatsCommonSchema = PlayerStatsSchemaGenerator(playerStatsSchemaInited)
    val playerStatsSchema: Schema = playerStatsSchemaInited
      .field("points", Schema.STRING_SCHEMA)
      .field("assists", Schema.STRING_SCHEMA)
      .field("blocks", Schema.STRING_SCHEMA)
      .field("fgMade", Schema.STRING_SCHEMA)
      .field("rebounds", Schema.STRING_SCHEMA)
      .field("steals", Schema.STRING_SCHEMA)
      .field("threePtMade", Schema.STRING_SCHEMA)

    val connectSchema: Schema = playerStatsSchema

    val playerStatsStructInited = new Struct(playerStatsSchema)
    val playerStatsCommonStruct = PlayerStatsStructGenerator(playerStatsStructInited,playerStatsCommonSchema,commonFields)
    val playerStatsStruct: Struct = playerStatsStructInited
      .put("points",points)
      .put("assists",assists)
      .put("blocks",blocks)
      .put("fgMade",fgMade)
      .put("rebounds",rebounds)
      .put("steals",steals)
      .put("threePtMade",threePtMade)
    def getStructure: Struct = playerStatsStruct

    log.trace("prepared schema")
  }


}