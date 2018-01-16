package com.slingmedia.sportscloud.parsers.leagues.delegates.boxscore.impl.nba

import com.slingmedia.sportscloud.parsers.factory.ParsedItem
import com.slingmedia.sportscloud.parsers.model.League
import com.slingmedia.sportscloud.parsers.leagues.delegates.boxscore.BoxScoreDataExtractor
import com.slingmedia.sportscloud.parsers.leagues.delegates.boxscore.BoxScoreStructGenerator
import com.slingmedia.sportscloud.parsers.leagues.delegates.boxscore.BoxScoreSchemaGenerator

import scala.xml.{Elem,NodeSeq}
import scala.collection.JavaConverters._

import org.apache.kafka.connect.source.SourceRecord
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.data.Struct
import org.apache.kafka.connect.data.SchemaBuilder

import org.slf4j.LoggerFactory;
import com.typesafe.scalalogging.slf4j.Logger

class NbaBoxScoreParserDelegate extends ParsedItem {

  private val log = LoggerFactory.getLogger("NbaBoxScoreParserDelegate")

  override def generateRows(data: Elem, in: SourceRecord, xmlRoot: NodeSeq): java.util.List[SourceRecord] = {
    log.trace("Parsing rows for boxscore")
    val leagueStr = (data \\ "league" \ "@alias").text

    var mlbBoxScores = scala.collection.mutable.ListBuffer.empty[SourceRecord]
    val rows = xmlRoot.map { rowData =>
      val commonFields = new BoxScoreDataExtractor(data,rowData)
      val homeTeamlineScore = scala.collection.mutable.ListBuffer.empty[Int]
      (rowData \\ "home-team" \\ "linescore" \\ "quarter").map { quarter =>
        homeTeamlineScore += toInt((quarter \\ "@score").text).getOrElse(0)
      }

      val awayTeamlineScore = scala.collection.mutable.ListBuffer.empty[Int]
      (rowData \\ "visiting-team" \\ "linescore" \\ "quarter").map { quarter =>
        awayTeamlineScore += toInt((quarter \\ "@score").text).getOrElse(0)
      }
      var awayScore = 0;
      if(awayTeamlineScore.length > 0) {
        awayScore = awayTeamlineScore.reduceLeft[Int](_ + _)
      }

      var homeScore = 0;
      if(homeTeamlineScore.length > 0) {
        homeScore = homeTeamlineScore.reduceLeft[Int](_ + _)
      }

      val message = NbaBoxScoreData(
        commonFields,
        homeTeamlineScore.toList,
        awayTeamlineScore.toList,
        homeScore,
        awayScore
      )
      new SourceRecord(in.sourcePartition,
        in.sourceOffset,
        in.topic,
        0,
        in.keySchema,
        in.key,
        message.connectSchema,
        message.getStructure)
    }
    rows.toList.asJava
  }

  case class NbaBoxScoreData(commonFields: BoxScoreDataExtractor,
                          homeTeamlineScore:List[Int],
                          awayTeamlineScore: List[Int],
                          homeScore: Int,
                          awayScore: Int
                         ) {
    val boxScoreSchemaInited = SchemaBuilder.struct().name("c.s.s.s.Game")
    val boxScoreCommonSchema = BoxScoreSchemaGenerator(boxScoreSchemaInited)
    val boxScoreSchema: Schema = boxScoreSchemaInited
      .field("homeTeamlineScore", SchemaBuilder.array(Schema.INT32_SCHEMA).build())
      .field("awayTeamlineScore", SchemaBuilder.array(Schema.INT32_SCHEMA).build())
      .field("homeScore", Schema.INT32_SCHEMA)
      .field("awayScore", Schema.INT32_SCHEMA)
      .build()

    val connectSchema: Schema = boxScoreSchema
    val boxScoreStructInited = new Struct(boxScoreSchema)
    val boxScoreCommonStruct = BoxScoreStructGenerator(boxScoreStructInited,commonFields)
    val boxScoreStruct: Struct = boxScoreStructInited
      .put("homeTeamlineScore",homeTeamlineScore.asJava)
      .put("awayTeamlineScore", awayTeamlineScore.asJava)
      .put("homeScore", homeScore)
      .put("awayScore", awayScore)
    def getStructure: Struct = boxScoreStruct

  }

}