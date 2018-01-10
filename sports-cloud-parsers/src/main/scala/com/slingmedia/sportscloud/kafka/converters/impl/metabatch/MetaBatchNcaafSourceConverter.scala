package com.slingmedia.sportscloud.kafka.converters.impl.metabatch

import com.slingmedia.sportscloud.kafka.converters.ConverterBase
import com.slingmedia.sportscloud.parsers.model.League
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

class NcaafSourceConverter extends SourceRecordConverter  with ConverterBase {
  private val log = LoggerFactory.getLogger("MetaBatchNcaafSourceConverter")

  override def convert(in: SourceRecord): java.util.List[SourceRecord] = {
    generateMetaInfoData(in,League("NCAAF", "College Football League"))

  }

  override def configure(props: util.Map[String, _]): Unit = {}

}