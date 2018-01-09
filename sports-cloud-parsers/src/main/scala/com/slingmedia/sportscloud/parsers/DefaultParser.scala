package com.slingmedia.sportscloud.parsers

import scala.xml.{Elem,NodeSeq}
import org.apache.kafka.connect.source.SourceRecord
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.data.SchemaBuilder
import org.apache.kafka.connect.data.Struct
import scala.collection.JavaConverters._
import com.slingmedia.sportscloud.parsers.factory.ParsedItem
import org.slf4j.LoggerFactory;
import com.typesafe.scalalogging.slf4j.Logger
//DefaultParser is a placeholder
class DefaultParser extends ParsedItem {
  private val log = LoggerFactory.getLogger("DefaultParser")
  override def generateRows(data: Elem, in: SourceRecord): java.util.List[SourceRecord] = {
    log.error("Default Parser called! Does not implement anything")
    super.generateRows(data,in)
  }

  override def generateRows(data: Elem, in: SourceRecord,league:String, sport:String): java.util.List[SourceRecord] = {
    log.error("Default Parser called! Does not implement anything")
    super.generateRows(data,in,league,sport)
  }

  override def generateRows(data: Elem, in: SourceRecord, xmlRoot: NodeSeq): java.util.List[SourceRecord] = {
    log.error("Default Parser called! Does not implement anything")
    super.generateRows(data,in,xmlRoot)
  }
}