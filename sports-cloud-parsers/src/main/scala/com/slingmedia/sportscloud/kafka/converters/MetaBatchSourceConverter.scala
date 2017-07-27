package com.slingmedia.sportscloud.kafka.converters

import com.eneco.trading.kafka.connect.ftp.source.SourceRecordConverter
import org.apache.kafka.connect.source.SourceRecord
import org.apache.kafka.connect.data.{ Schema, SchemaBuilder, Struct }
import scala.collection.JavaConverters._
import java.util
import com.slingmedia.sportscloud.parsers.factory.{ Parsers , ParserType}
import org.slf4j.LoggerFactory;
import com.typesafe.scalalogging.slf4j.Logger
import scala.xml.Elem

class MetaBatchSourceConverter extends SourceRecordConverter {
  private val log = LoggerFactory.getLogger("MetaBatchSourceConverter")

  override def convert(in: SourceRecord): java.util.List[SourceRecord] = {
    log.trace("Converting source for metabatch")
    val line = new String(in.value.asInstanceOf[Array[Byte]])
    val dataElem: Option[Elem] = Some(scala.xml.XML.loadString(line))

    dataElem match {
      case Some(data) =>
        val fileName = in.key
        val mlbTeamStandings = ".*MLB_TEAM_STANDINGS\\.XML.*".r
        val mlbPlayerStats = ".*MLB_PLAYER_STATS.*\\.XML.*".r
        val finalBoxScores = ".*MLB_FINALBOX.*\\.XML.*".r
        fileName match {
          case mlbTeamStandings(_*) =>
            Parsers(ParserType.TeamStandingsParser).generateRows(data, in, (data \\ "baseball-mlb-league-standings"))
          case mlbPlayerStats(_*) =>
            Parsers(ParserType.PlayerStatsParser).generateRows(data, in, (data \\ "baseball-mlb-player-stats"))
          case finalBoxScores(_*) =>
            Parsers(ParserType.BoxScoreParser).generateRows(data, in)
          case _ =>
            Parsers(ParserType.Default).generateRows(data, in)
        }
      case None =>
        Array[SourceRecord]().toList.asJava
    }

  }

  override def configure(props: util.Map[String, _]): Unit = {}

}