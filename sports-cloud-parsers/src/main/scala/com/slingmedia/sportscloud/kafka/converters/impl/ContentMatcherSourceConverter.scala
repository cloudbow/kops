package com.slingmedia.sportscloud.kafka.converters.impl

import com.slingmedia.sportscloud.kafka.converters.ConverterBase
import com.slingmedia.sportscloud.parsers.factory.{ Parsers, ParserType}


import com.eneco.trading.kafka.connect.ftp.source.SourceRecordConverter
import org.apache.kafka.connect.source.SourceRecord
import scala.collection.JavaConverters._
import java.util
import org.slf4j.LoggerFactory
import scala.xml.Elem
import scala.util.{Try, Success, Failure}

class ContentMatcherSourceConverter extends SourceRecordConverter with ConverterBase{
  private val log = LoggerFactory.getLogger("ContentMatcherSourceConverter")

  override def convert(in: SourceRecord): java.util.List[SourceRecord] = {
    val line = new String(in.value.asInstanceOf[Array[Byte]])
    val dataElem: Try[Elem] = loadXML(line)

    dataElem match {
      case Success(data) =>
        val fileName = in.key
        val schedule = ".*MLB_SCHEDULE\\.XML.*".r
        fileName match {
          case schedule(_*) =>
            Parsers(ParserType.ScheduleParser).generateRows(data, in, "MLB", "baseball")
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