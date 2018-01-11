package com.slingmedia.sportscloud.parsers.leagues.impl.ncaaf

import com.slingmedia.sportscloud.parsers.factory.ParsedItem
import com.slingmedia.sportscloud.parsers.leagues.delegates.boxscore.impl.nfl.NflBoxScoreParserDelegate
import scala.xml.Elem
import org.apache.kafka.connect.source.SourceRecord
import com.slingmedia.sportscloud.parsers.model.League
import scala.collection.JavaConverters._
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.data.Struct
import org.apache.kafka.connect.data.SchemaBuilder
import org.slf4j.LoggerFactory;
import com.typesafe.scalalogging.slf4j.Logger
 
import scala.collection.immutable.HashMap
import scala.collection.immutable.Map

class NcaafBoxScoreParser extends ParsedItem {

  private val log = LoggerFactory.getLogger("NcaafBoxScoreParser")

  override def generateRows(data: Elem, in: SourceRecord, xmlRoot: scala.xml.NodeSeq): java.util.List[SourceRecord] = {
    new NflBoxScoreParserDelegate().generateRows(data,in,xmlRoot,12,48)
  }

}