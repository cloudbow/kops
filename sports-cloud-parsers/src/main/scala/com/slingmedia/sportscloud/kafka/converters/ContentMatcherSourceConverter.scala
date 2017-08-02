package com.slingmedia.sportscloud.kafka.converters

import com.eneco.trading.kafka.connect.ftp.source.SourceRecordConverter
import org.apache.kafka.connect.source.SourceRecord
import org.apache.kafka.connect.data.{ Schema, SchemaBuilder, Struct }
import scala.collection.JavaConverters._
import java.util
import com.slingmedia.sportscloud.parsers.factory.{ Parsers , ParserType}
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.slf4j.Logger
import scala.xml.Elem

class ContentMatcherSourceConverter extends SourceRecordConverter {
  private val log = LoggerFactory.getLogger("ContentMatcherSourceConverter")

  override def convert(in: SourceRecord): java.util.List[SourceRecord] = {
    val line = new String(in.value.asInstanceOf[Array[Byte]])
    val dataElem: Option[Elem] = Some(scala.xml.XML.loadString(line))

    dataElem match {
      case Some  (data) =>
        val fileName = in.key
        val schedule = ".*MLB_SCHEDULE\\.XML.*".r
        fileName match {
          case schedule(_*) =>
            Parsers(ParserType.ScheduleParser).generateRows(data, in, "MLB", "baseball")
          case _ =>
            Parsers(ParserType.Default).generateRows(data, in)
        }
      case None =>
        Array[SourceRecord]().toList.asJava
    }
  }

  override def configure(props: util.Map[String, _]): Unit = {}

}