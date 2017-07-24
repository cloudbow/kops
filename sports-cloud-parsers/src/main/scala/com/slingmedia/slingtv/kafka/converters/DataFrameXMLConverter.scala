package com.slingmedia.slingtv.kafka

import com.eneco.trading.kafka.connect.ftp.source.SourceRecordConverter
import org.apache.kafka.connect.source.SourceRecord
import org.apache.kafka.connect.data.{ Schema, SchemaBuilder, Struct }
import scala.collection.JavaConverters._
import java.util
import com.slingmedia.sportscloud.parsers.factory.Parsers
import com.slingmedia.sportscloud.parsers.factory.ParserType
import org.slf4j.LoggerFactory;
import com.typesafe.scalalogging.slf4j.Logger


class DataFrameXMLConvert extends SourceRecordConverter {
  private val log = LoggerFactory.getLogger("DataFrameXMLConvert")

  def convert(in: SourceRecord): java.util.List[SourceRecord] = {
    val line = new String(in.value.asInstanceOf[Array[Byte]])
    val data = scala.xml.XML.loadString(line)

    val fileName = in.key
    val schedule = ".*MLB_SCHEDULE.XML.*".r
    val teamStandings = ".*MLB_TEAM_STANDINGS.XML.*".r
    fileName match {
      case schedule(_*) =>
        Parsers(ParserType.ScheduleParser).generateRows(data,in)
      case teamStandings(_*) =>
        Parsers(ParserType.TeamStandingsParser).generateRows(data,in)
      case _ =>
        Parsers(ParserType.Default).generateRows(data, in)
     }

  }

  override def configure(props: util.Map[String, _]): Unit = {}
}






