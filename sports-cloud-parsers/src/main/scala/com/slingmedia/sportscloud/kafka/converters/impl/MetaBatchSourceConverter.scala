package com.slingmedia.sportscloud.kafka.converters.impl

import com.slingmedia.sportscloud.kafka.converters.ConverterBase
import com.slingmedia.sportscloud.parsers.factory.{ Parsers , ParserType}


import com.eneco.trading.kafka.connect.ftp.source.SourceRecordConverter
import org.apache.kafka.connect.source.SourceRecord
import org.apache.kafka.connect.data.{ Schema, SchemaBuilder, Struct }
import scala.collection.JavaConverters._
import java.util
import org.slf4j.LoggerFactory;
import com.typesafe.scalalogging.slf4j.Logger
import scala.xml.Elem
import scala.util.{Try, Success, Failure}

class MetaBatchSourceConverter extends SourceRecordConverter  with ConverterBase {
  private val log = LoggerFactory.getLogger("MetaBatchSourceConverter")

  override def convert(in: SourceRecord): java.util.List[SourceRecord] = {
    log.trace("Converting source for metabatch")
    val line = new String(in.value.asInstanceOf[Array[Byte]])
   val dataElem: Try[Elem] = loadXML(line)

    dataElem match {
      case Success(data) =>
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
            Array[SourceRecord]().toList.asJava
        }
      case Failure(e) =>
        log.error("Error occurred in parsing xml ",e)
        Array[SourceRecord]().toList.asJava
    }

  }

  override def configure(props: util.Map[String, _]): Unit = {}

}