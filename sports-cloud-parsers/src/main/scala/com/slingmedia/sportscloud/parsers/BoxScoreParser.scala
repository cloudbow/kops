package com.slingmedia.sportscloud.parsers

import com.slingmedia.sportscloud.parsers.factory.ParsedItem
import scala.xml.Elem
import org.apache.kafka.connect.source.SourceRecord
import com.slingmedia.sportscloud.parsers.model.League
import scala.collection.JavaConverters._
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.data.Struct
import org.apache.kafka.connect.data.SchemaBuilder
import org.slf4j.LoggerFactory;
import com.typesafe.scalalogging.slf4j.Logger

class BoxScoreParser extends ParsedItem {

  private val log = LoggerFactory.getLogger("BoxScoreParser")

  override def generateRows(data: Elem, in: SourceRecord, xmlRoot: scala.xml.NodeSeq): java.util.List[SourceRecord] = {
    log.trace("Parsing rows for boxscore")
    val leagueStr = (data \\ "league" \ "@alias").toString
    val league = League.withNameOpt(leagueStr.toUpperCase)

    var mlbBoxScores = scala.collection.mutable.ListBuffer.empty[SourceRecord]
    val rows = xmlRoot.map { rowData =>
      val homeTeamExId = (rowData \\ "home-team" \\ "team-code" \ "@global-id").toString
      val homeTeamAlias = (rowData \\ "home-team" \\ "team-name" \ "@alias").toString
      val awayTeamAlias = (rowData \\ "visiting-team" \\ "team-name" \ "@alias").toString
      val awayTeamExtId = (rowData \\ "visiting-team" \\ "team-code" \ "@global-id").toString
      val gameCode = (rowData \\ "gamecode" \ "@global-id").toString
      val gameType = (rowData \\ "gametype" \ "@type").toString
      val awayScoreRuns = toInt(((rowData \\ "visiting-score" filter { _ \ "@type" exists (_.text == "runs") }) \ "@number").text).getOrElse(0)
      val awayScoreHits =  toInt(((rowData \\ "visiting-score" filter { _ \ "@type" exists (_.text == "hits") }) \ "@number").text).getOrElse(0)
      val awayScoreErrors =  toInt(((rowData \\ "visiting-score" filter { _ \ "@type" exists (_.text == "errors") }) \ "@number").text).getOrElse(0)
      val homeScoreRuns =  toInt(((rowData \\ "home-score" filter { _ \ "@type" exists (_.text == "runs") }) \ "@number").text).getOrElse(0)
      val homeScoreHits = toInt(((rowData \\ "home-score" filter { _ \ "@type" exists (_.text == "hits") }) \ "@number").text).getOrElse(0)
      val homeScoreErrors = toInt(((rowData \\ "home-score" filter { _ \ "@type" exists (_.text == "errors") }) \ "@number").text).getOrElse(0)
      val lastPlay = (rowData \\ "last-play" \ "@text").toString
      val gameStatus = (rowData \\ "gamestate" \ "@status").toString
      val inningTitle = (rowData \\ "gamestate" \ "@inning").toString
      val balls = toInt((rowData \\ "gamestate" \ "@balls").text).getOrElse(0)
      val strikes = toInt((rowData \\ "gamestate" \ "@strikes").text).getOrElse(0)
      val outs = toInt((rowData \\ "gamestate" \ "@outs").text).getOrElse(0)
      val segmentDivision = (rowData \\ "gamestate" \ "@segment-division").toString
      val awayTeamInnings = scala.collection.mutable.ListBuffer.empty[Int]
      (rowData \\ "visiting-team" \\ "innings" \ "inning").map { inning =>
        awayTeamInnings += toInt((inning \\ "@score").text).getOrElse(0)
      }
      val homeTeamInnings = scala.collection.mutable.ListBuffer.empty[Int]
      (rowData \\ "home-team" \\ "innings" \ "inning").map { inning =>
        homeTeamInnings += toInt((inning \\ "@score").text).getOrElse(0)
      }
      val message = BoxScoreData(homeTeamExId, homeTeamAlias, awayTeamAlias, awayTeamExtId, gameCode, gameType, awayScoreRuns, awayScoreHits, awayScoreErrors, homeScoreRuns, homeScoreHits, homeScoreErrors, lastPlay, gameStatus, inningTitle, balls, strikes, outs, segmentDivision, awayTeamInnings.toList, homeTeamInnings.toList)
      new SourceRecord(in.sourcePartition, in.sourceOffset, in.topic, 0, in.keySchema, in.key, message.connectSchema, message.getStructure)

    }
    rows.toList.asJava

  }

  case class BoxScoreData(homeTeamExtId: String, homeTeamAlias: String, awayTeamAlias: String, awayTeamExtId: String, gameCode: String, gameType: String, awayScoreRuns: Int, awayScoreHits: Int, awayScoreErrors: Int, homeScoreRuns: Int, homeScoreHits: Int, homeScoreErrors: Int, lastPlay: String, gameStatus: String, inningTitle: String, balls: Int, strikes: Int, outs: Int, segmentDivision: String, awayTeamInnings: List[Int], homeTeamInnings: List[Int]) {

    val teamSchema: Schema = SchemaBuilder.struct().name("c.s.s.s.Team")
      .field("alias", Schema.STRING_SCHEMA)
      .field("extId", Schema.STRING_SCHEMA)
      .field("runs", Schema.INT32_SCHEMA)
      .field("hits", Schema.INT32_SCHEMA)
      .field("errors", Schema.INT32_SCHEMA)
      .field("innings", SchemaBuilder.array(Schema.INT32_SCHEMA).build()).build()

    val gameSchema: Schema = SchemaBuilder.struct().name("c.s.s.s.Game")
      .field("status", Schema.STRING_SCHEMA)
      .field("gameType", Schema.STRING_SCHEMA)
      .field("gameCode", Schema.STRING_SCHEMA)
      .field("lastPlay", Schema.STRING_SCHEMA)
      .field("inningTitle", Schema.STRING_SCHEMA)
      .field("balls", Schema.INT32_SCHEMA)
      .field("strikes", Schema.INT32_SCHEMA)
      .field("outs", Schema.INT32_SCHEMA)
      .field("segmentDivision", Schema.STRING_SCHEMA).build()

    val boxScoreSchema = SchemaBuilder.struct().name("c.s.s.s.BoxScore")
      .field("game", gameSchema)
      .field("homeTeam", teamSchema)
      .field("awayTeam", teamSchema).build()

    val connectSchema: Schema = boxScoreSchema

    val homeTeamStruct: Struct = new Struct(teamSchema)
      .put("alias", homeTeamAlias)
      .put("extId", homeTeamExtId)
      .put("runs", homeScoreErrors)
      .put("hits", homeScoreHits)
      .put("errors", homeScoreErrors)
      .put("innings", homeTeamInnings.asJava)

    val awayTeamStruct: Struct = new Struct(teamSchema)
      .put("alias", awayTeamAlias)
      .put("extId", awayTeamExtId)
      .put("runs", awayScoreErrors)
      .put("hits", awayScoreHits)
      .put("errors", awayScoreErrors)
      .put("innings", awayTeamInnings.asJava)

    val gameStruct: Struct = new Struct(gameSchema)
      .put("status", gameStatus)
      .put("gameType",gameType)
      .put("gameCode",gameCode)
      .put("lastPlay", lastPlay)
      .put("inningTitle", inningTitle)
      .put("balls", balls)
      .put("strikes", strikes)
      .put("outs", outs)
      .put("segmentDivision", segmentDivision)

    val boxScoreStruct: Struct = new Struct(boxScoreSchema)
      .put("homeTeam", homeTeamStruct)
      .put("awayTeam", awayTeamStruct)
      .put("game", gameStruct)
    def getStructure: Struct = boxScoreStruct

  }

}