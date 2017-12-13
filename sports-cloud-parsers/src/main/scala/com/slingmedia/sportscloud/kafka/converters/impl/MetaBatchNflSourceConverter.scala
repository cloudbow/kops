package com.slingmedia.sportscloud.kafka.converters.impl

import java.util

import com.eneco.trading.kafka.connect.ftp.source.SourceRecordConverter
import com.slingmedia.sportscloud.kafka.converters.ConverterBase
import com.slingmedia.sportscloud.parsers.factory.{ParserType, Parsers}
import org.apache.kafka.connect.source.SourceRecord
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}
import scala.xml.Elem

class MetaBatchNflSourceConverter extends SourceRecordConverter  with ConverterBase {
  private val log = LoggerFactory.getLogger("MetaBatchNflSourceConverter")

  override def convert(in: SourceRecord): java.util.List[SourceRecord] = {
    log.trace("Converting source for metabatch")
    val line = new String(in.value.asInstanceOf[Array[Byte]])
   val dataElem: Try[Elem] = loadXML(line)

    dataElem match {
      case Success(data) =>
        val fileName = in.key
        val nflTeamStandings = ".*NFL_TEAM_STANDINGS\\.XML.*".r
        //val ncaafPlayerStats = ".*CFB_PLAYER_STATS.*\\.XML.*".r
        //val finalBoxScores = ".*CFB_FINALBOX.*\\.XML.*".r
        fileName match {
          case nflTeamStandings(_*) =>
            Parsers(ParserType.NflTeamStandingsParser).generateRows(data, in, (data \\ "football-nfl-standings"))
          //case ncaafPlayerStats(_*) =>
            //Parsers(ParserType.NcaafPlayerStatsParser).generateRows(data, in, (data \\ "cfb-player-stats"))
          //case finalBoxScores(_*) =>
            //Parsers(ParserType.NcaafBoxScoreParser).generateRows(data, in)
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