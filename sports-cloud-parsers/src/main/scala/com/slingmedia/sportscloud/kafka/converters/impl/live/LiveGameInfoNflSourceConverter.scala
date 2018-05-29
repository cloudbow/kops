package com.slingmedia.sportscloud.kafka.converters.impl.live

import java.util

import com.eneco.trading.kafka.connect.ftp.source.SourceRecordConverter
import com.slingmedia.sportscloud.kafka.converters.ConverterBase
import com.slingmedia.sportscloud.parsers.model.League
import com.slingmedia.sportscloud.parsers.factory.{ParserType, Parsers}
import org.apache.kafka.connect.source.SourceRecord
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}
import scala.xml.Elem

class NflSourceConverter extends SourceRecordConverter with ConverterBase {
  private val log = LoggerFactory.getLogger("LiveGameInfoNflSourceConverter")

  override def convert(in: SourceRecord): java.util.List[SourceRecord] = {
    generateLiveInfoData(in,League("NFL", "National Football League"))
  }

  override def configure(props: util.Map[String, _]): Unit = {}
}