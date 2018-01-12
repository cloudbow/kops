package com.slingmedia.sportscloud.parsers.leagues.impl.mlb

import com.slingmedia.sportscloud.parsers.factory.ParsedItem
import com.slingmedia.sportscloud.parsers.leagues.delegates.ts.impl.mlb.MlbTeamStandingsParserDelegate
import com.slingmedia.sportscloud.parsers.model.{ League , TeamStandings }

import scala.xml.Elem
import scala.collection.JavaConverters._
import scala.xml.NodeSeq

import org.apache.kafka.connect.source.SourceRecord
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.data.Struct
import org.apache.kafka.connect.data.SchemaBuilder

import org.slf4j.LoggerFactory;
import com.typesafe.scalalogging.slf4j.Logger

class MlbTeamStandingsParser extends ParsedItem {

  private val log = LoggerFactory.getLogger("MlbTeamStandingsParser")

  override def generateRows(data: Elem, in: SourceRecord, xmlRoot: NodeSeq): java.util.List[SourceRecord] = {
    log.trace("Running MlbTeamStandingsParser")
    new MlbTeamStandingsParserDelegate().generateRows(data,in,xmlRoot)
  }

}