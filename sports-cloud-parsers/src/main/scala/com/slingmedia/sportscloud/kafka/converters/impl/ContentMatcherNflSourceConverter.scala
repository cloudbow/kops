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

class ContentMatcherNflSourceConverter extends SourceRecordConverter with ConverterBase{
  private val log = LoggerFactory.getLogger("ContentMatcherNflSourceConverter")

  override def convert(in: SourceRecord): java.util.List[SourceRecord] = {
    val line = new String(in.value.asInstanceOf[Array[Byte]])
    val dataElem: Try[Elem] = loadXML(line)

    dataElem match {
      case Success(data) =>
        val fileName = in.key
        val schedule = ".*NFL_SCHEDULE.*\\.XML.*".r
        fileName match {
          //case str : String => str match {
            case schedule(_*) =>
              Parsers(ParserType.NflScheduleParser).generateRows(data, in, "NFL", "National Football League")
            case _ =>
              Array[SourceRecord]().toList.asJava
          }
          //case _ =>
            //Array[SourceRecord]().toList.asJava
        //}
      case Failure(e) =>
        log.error("Error occurred in parsing xml ",e)
        Array[SourceRecord]().toList.asJava
    }
  }

  override def configure(props: util.Map[String, _]): Unit = {}

}