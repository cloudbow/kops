package com.slingmedia.sportscloud.parsers.leagues.impl.nfl

import com.slingmedia.sportscloud.parsers.factory.ParsedItem
import com.slingmedia.sportscloud.parsers.leagues.delegates.boxscore.impl.nfl.NflBoxScoreParserDelegate
import com.slingmedia.sportscloud.parsers.model.League
import org.apache.kafka.connect.data.{Schema, SchemaBuilder, Struct}
import org.apache.kafka.connect.source.SourceRecord
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.xml.Elem

class NflBoxScoreParser extends ParsedItem {

  private val log = LoggerFactory.getLogger("NcaafBoxScoreParser")


  override def generateRows(data: Elem, in: SourceRecord, xmlRoot: scala.xml.NodeSeq): java.util.List[SourceRecord] = {
    new NflBoxScoreParserDelegate().generateRows(data,in,xmlRoot,15,60)

  }

}