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

class LiveGameInfoNflSourceConverter extends SourceRecordConverter with ConverterBase {
  private val log = LoggerFactory.getLogger("LiveGameInfoNflSourceConverter")

  override def convert(in: SourceRecord): java.util.List[SourceRecord] = {
    log.trace("Converting source for livegame info for NFL")
    val line = new String(in.value.asInstanceOf[Array[Byte]])
    val dataElem: Try[Elem] = loadXML(line)

    dataElem match {
      case Success(data) =>
        val fileName = in.key
        val boxScore = ".*NFL.*_BOXSCORE.*\\.XML.*".r
        val liveData = ".*NFL.*_LIVE.*\\.XML.*".r
        val finalBoxScore = ".*NFL.*_FINALBOX.*\\.XML.*".r
        fileName match {
          case boxScore(_*) | finalBoxScore(_*)  =>
            Parsers(ParserType.NflBoxScoreParser).generateRows(data, in, (data \\ "nfl-boxscore"))
          case liveData(_*) =>
            Parsers(ParserType.NflBoxScoreParser).generateRows(data, in, (data \\ "nfl-score"))
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