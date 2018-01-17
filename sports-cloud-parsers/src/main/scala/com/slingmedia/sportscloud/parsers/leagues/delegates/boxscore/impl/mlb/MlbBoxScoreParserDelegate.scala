package com.slingmedia.sportscloud.parsers.leagues.delegates.boxscore.impl.mlb

import com.slingmedia.sportscloud.parsers.factory.ParsedItem
import com.slingmedia.sportscloud.parsers.model.League
import com.slingmedia.sportscloud.parsers.leagues.delegates.boxscore.BoxScoreDataExtractor
import com.slingmedia.sportscloud.parsers.leagues.delegates.boxscore.BoxScoreStructGenerator
import com.slingmedia.sportscloud.parsers.leagues.delegates.boxscore.BoxScoreSchemaGenerator

import scala.xml.Elem
import scala.collection.JavaConverters._

import org.apache.kafka.connect.source.SourceRecord
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.data.Struct
import org.apache.kafka.connect.data.SchemaBuilder

import org.slf4j.LoggerFactory;
import com.typesafe.scalalogging.slf4j.Logger

class MlbBoxScoreParserDelegate extends ParsedItem {

  private val log = LoggerFactory.getLogger("MlbBoxScoreParserDelegate")

  override def generateRows(data: Elem, in: SourceRecord, xmlRoot: scala.xml.NodeSeq): java.util.List[SourceRecord] = {
    log.info("Parsing rows for mlb boxscore")
    val leagueStr = (data \\ "league" \ "@alias").text

    var mlbBoxScores = scala.collection.mutable.ListBuffer.empty[SourceRecord]
    val rows = xmlRoot.map { rowData =>

      val commonFields = new BoxScoreDataExtractor(data,rowData)

      val division = (rowData \\ "league" \ "@league").text
      val firstGameBase = (rowData \\ "runners-on-base" \ "first-base" \ "@global-id").text
      val secondGameBase = (rowData \\ "runners-on-base" \ "second-base" \ "@global-id").text
      val thirdGameBase = (rowData \\ "runners-on-base" \ "third-base" \ "@global-id").text
      val awayScoreRuns = toInt(((rowData \\ "visiting-score" filter { _ \ "@type" exists (_.text == "runs") }) \ "@number").text).getOrElse(0)
      val awayScoreHits = toInt(((rowData \\ "visiting-score" filter { _ \ "@type" exists (_.text == "hits") }) \ "@number").text).getOrElse(0)
      val awayScoreErrors = toInt(((rowData \\ "visiting-score" filter { _ \ "@type" exists (_.text == "errors") }) \ "@number").text).getOrElse(0)
      val homeScoreRuns = toInt(((rowData \\ "home-score" filter { _ \ "@type" exists (_.text == "runs") }) \ "@number").text).getOrElse(0)
      val homeScoreHits = toInt(((rowData \\ "home-score" filter { _ \ "@type" exists (_.text == "hits") }) \ "@number").text).getOrElse(0)
      val homeScoreErrors = toInt(((rowData \\ "home-score" filter { _ \ "@type" exists (_.text == "errors") }) \ "@number").text).getOrElse(0)
      val currentBatterId = (rowData \\ "current-batter" \ "@global-id").text
      val currentBatterName = (rowData \\ "current-batter" \ "@last-name").text
      val awayTeamCurrPitcherId = (rowData \\ "visiting-team" \ "current-pitcher" \ "@global-id").text
      val homeTeamCurrPitcherId = (rowData \\ "home-team" \ "current-pitcher" \ "@global-id").text
      val awayTeamCurrPitcherName = (rowData \\ "visiting-team" \ "current-pitcher" \ "@last-name").text
      val homeTeamCurrPitcherName = (rowData \\ "home-team" \ "current-pitcher" \ "@last-name").text
      val inningTitle = (rowData \\ "gamestate" \ "@inning").text
      val inningNo = toInt((rowData \\ "gamestate" \ "@segment-number").text).getOrElse(0)
      val balls = toInt((rowData \\ "gamestate" \ "@balls").text).getOrElse(-1)
      val strikes = toInt((rowData \\ "gamestate" \ "@strikes").text).getOrElse(-1)
      val outs = toInt((rowData \\ "gamestate" \ "@outs").text).getOrElse(-1)
      val segmentDivision = (rowData \\ "gamestate" \ "@segment-division").text
      val awayTeamInnings = scala.collection.mutable.ListBuffer.empty[Int]
      (rowData \\ "visiting-team" \\ "innings" \ "inning").map { inning =>
        awayTeamInnings += toInt((inning \\ "@score").text).getOrElse(0)
      }
      val homeTeamInnings = scala.collection.mutable.ListBuffer.empty[Int]
      (rowData \\ "home-team" \\ "innings" \ "inning").map { inning =>
        homeTeamInnings += toInt((inning \\ "@score").text).getOrElse(0)
      }
      val message = MlbBoxScoreData(commonFields,
        division,
        firstGameBase,
        secondGameBase,
        thirdGameBase,
        homeScoreRuns,
        homeScoreHits,
        homeScoreErrors,
        awayScoreRuns,
        awayScoreHits,
        awayScoreErrors,
        inningTitle,
        inningNo,
        balls,
        strikes,
        outs,
        segmentDivision,
        homeTeamInnings.toList,
        awayTeamInnings.toList,
        currentBatterId,
        currentBatterName,
        homeTeamCurrPitcherId,
        homeTeamCurrPitcherName,
        awayTeamCurrPitcherId,
        awayTeamCurrPitcherName)

      new SourceRecord(
        in.sourcePartition,
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

  case class MlbBoxScoreData(commonFields: BoxScoreDataExtractor,
                              division: String ,
                              firstGameBase: String,
                              secondGameBase: String,
                              thirdGameBase: String,
                              homeScoreRuns: Int ,
                              homeScoreHits: Int ,
                              homeScoreErrors: Int ,
                              awayScoreRuns: Int ,
                              awayScoreHits: Int ,
                              awayScoreErrors: Int ,
                              inningTitle: String ,
                              inningNo: Int ,
                              balls: Int ,
                              strikes: Int ,
                              outs: Int ,
                              segmentDivision: String ,
                              homeTeamInnings: List[Int] ,
                              awayTeamInnings: List[Int] ,
                              currentBatterId: String ,
                              currentBatterName: String ,
                              homeTeamCurrPitcherId: String ,
                              homeTeamCurrPitcherName: String ,
                              awayTeamCurrPitcherId: String ,
                              awayTeamCurrPitcherName: String) {
    val boxScoreSchemaInited = SchemaBuilder.struct().name("c.s.s.s.Game")
    val boxScoreCommonSchema = BoxScoreSchemaGenerator(boxScoreSchemaInited)
    val boxScoreSchema: Schema = boxScoreSchemaInited
      .field("division", Schema.STRING_SCHEMA)
      .field("inningTitle", Schema.STRING_SCHEMA)
      .field("inningNo", Schema.INT32_SCHEMA)
      .field("balls", Schema.INT32_SCHEMA)
      .field("strikes", Schema.INT32_SCHEMA)
      .field("outs", Schema.INT32_SCHEMA)
      .field("segmentDiv", Schema.STRING_SCHEMA)
      .field("homeScoreRuns", Schema.INT32_SCHEMA)
      .field("homeScoreHits", Schema.INT32_SCHEMA)
      .field("homeScoreErrors", Schema.INT32_SCHEMA)
      .field("homeTeamInnings", SchemaBuilder.array(Schema.INT32_SCHEMA).build())
      .field("awayScoreRuns", Schema.INT32_SCHEMA)
      .field("awayScoreHits", Schema.INT32_SCHEMA)
      .field("awayScoreErrors", Schema.INT32_SCHEMA)
      .field("awayTeamInnings", SchemaBuilder.array(Schema.INT32_SCHEMA).build())
      .field("currBtrId", Schema.STRING_SCHEMA)
      .field("currBtrName", Schema.STRING_SCHEMA)
      .field("hTCurrPitcherId", Schema.STRING_SCHEMA)
      .field("hTCurrPitcherName", Schema.STRING_SCHEMA)
      .field("aTCurrPitcherId", Schema.STRING_SCHEMA)
      .field("aTCurrPitcherName", Schema.STRING_SCHEMA)
      .field("firstGameBase" , Schema.STRING_SCHEMA)
      .field("secondBase", Schema.STRING_SCHEMA)
      .field("thirdGameBase", Schema.STRING_SCHEMA)
      .build()

    val connectSchema: Schema = boxScoreSchema

    val boxScoreStructInited = new Struct(boxScoreSchema)
    val boxScoreCommonStruct = BoxScoreStructGenerator(boxScoreStructInited,commonFields)
    val boxScoreStruct: Struct = boxScoreStructInited
      .put("division", division)
      .put("inningTitle", inningTitle)
      .put("inningNo", inningNo)
      .put("balls", balls)
      .put("strikes", strikes)
      .put("outs", outs)
      .put("segmentDiv", segmentDivision)
      .put("homeScoreRuns", homeScoreRuns)
      .put("homeScoreHits", homeScoreHits)
      .put("homeScoreErrors", homeScoreErrors)
      .put("homeTeamInnings", homeTeamInnings.asJava)
      .put("awayScoreRuns", awayScoreRuns)
      .put("awayScoreHits", awayScoreHits)
      .put("awayScoreErrors", awayScoreErrors)
      .put("awayTeamInnings", awayTeamInnings.asJava)
      .put("currBtrId", currentBatterId)
      .put("currBtrName", currentBatterName)
      .put("hTCurrPitcherId", homeTeamCurrPitcherId)
      .put("hTCurrPitcherName", homeTeamCurrPitcherName)
      .put("aTCurrPitcherId", awayTeamCurrPitcherId)
      .put("aTCurrPitcherName", awayTeamCurrPitcherName)
      .put("firstGameBase" , firstGameBase)
      .put("secondBase", secondGameBase)
      .put("thirdGameBase", thirdGameBase)

    def getStructure: Struct = boxScoreStruct

  }

}