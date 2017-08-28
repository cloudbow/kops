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

class LiveGameInfoSourceConverter extends SourceRecordConverter with ConverterBase {
  private val log = LoggerFactory.getLogger("LiveGameInfoSourceConverter")

  override def convert(in: SourceRecord): java.util.List[SourceRecord] = {
    log.trace("Converting source for livegame info")
    val line = new String(in.value.asInstanceOf[Array[Byte]])
    val dataElem: Try[Elem] = loadXML(line)

    dataElem match {
      case Success(data) =>
        val fileName = in.key
        val boxScore = ".*MLB.*_BOXSCORE.*\\.XML.*".r
        val liveData = ".*MLB.*_LIVE.*\\.XML.*".r
        val finalBoxScore = ".*MLB.*FINALBOX.*\\.XML.*".r
        fileName match {
          case boxScore(_*) | finalBoxScore(_*)  =>
            Parsers(ParserType.BoxScoreParser).generateRows(data, in, (data \\ "baseball-mlb-boxscore"))
          case liveData(_*) =>
            Parsers(ParserType.BoxScoreParser).generateRows(data, in, (data \\ "baseball-mlb-score"))
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