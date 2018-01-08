package com.slingmedia.sportscloud.kafka.converters.impl

import com.slingmedia.sportscloud.kafka.converters.ConverterBase
import com.slingmedia.sportscloud.parsers.model.League
import com.slingmedia.sportscloud.parsers.factory.{ Parsers, ParserType}


import com.eneco.trading.kafka.connect.ftp.source.SourceRecordConverter
import org.apache.kafka.connect.source.SourceRecord
import scala.collection.JavaConverters._
import java.util
import org.slf4j.LoggerFactory
import scala.xml.Elem
import scala.util.{Try, Success, Failure}

class ContentMatcherMlbSourceConverter extends SourceRecordConverter with ConverterBase{
  private val log = LoggerFactory.getLogger("ContentMatcherMlbSourceConverter")

  override def convert(in: SourceRecord): java.util.List[SourceRecord] = {
    generateContentMatchData(in,League("MLB", "baseball"))
  }

  override def configure(props: util.Map[String, _]): Unit = {}

}