package com.slingmedia.sportscloud.parsers.leagues.impl.mlb

import com.slingmedia.sportscloud.parsers.factory.ParsedItem
import com.slingmedia.sportscloud.parsers.leagues.delegates.boxscore.impl.mlb.MlbBoxScoreParserDelegate

import scala.xml.Elem
import org.apache.kafka.connect.source.SourceRecord
import com.slingmedia.sportscloud.parsers.model.League
import scala.collection.JavaConverters._
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.data.Struct
import org.apache.kafka.connect.data.SchemaBuilder
import org.slf4j.LoggerFactory;
import com.typesafe.scalalogging.slf4j.Logger



class MlbBoxScoreParser extends ParsedItem {

  private val log = LoggerFactory.getLogger("BoxScoreParser")

  override def generateRows(data: Elem, in: SourceRecord, xmlRoot: scala.xml.NodeSeq): java.util.List[SourceRecord] = {
    log.info("Running MlbBoxScoreParser")
    new MlbBoxScoreParserDelegate().generateRows(data,in,xmlRoot)
  }

}