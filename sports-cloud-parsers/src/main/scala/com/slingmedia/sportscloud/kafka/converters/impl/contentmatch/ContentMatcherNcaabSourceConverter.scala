package com.slingmedia.sportscloud.kafka.converters.impl.contentmatch

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

class NcaabSourceConverter extends SourceRecordConverter with ConverterBase {
  private val log = LoggerFactory.getLogger("ContentMatcherNcaabSourceConverter")

  override def convert(in: SourceRecord): java.util.List[SourceRecord] = {
    log.info("Running NcaabSourceConverter")
    generateContentMatchData(in,League("NCAAB", "College Basketball"))
  }

  override def configure(props: util.Map[String, _]): Unit = {}

}