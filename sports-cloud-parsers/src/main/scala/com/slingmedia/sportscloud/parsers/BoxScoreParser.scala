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
    val leagueStr = (data \\ "league" \ "@alias").text
    val league = League.withNameOpt(leagueStr.toUpperCase)

    var mlbBoxScores = scala.collection.mutable.ListBuffer.empty[SourceRecord]
    val rows = xmlRoot.map { rowData =>
      //get source time
      val srcMonth = ((data \\ "date")(0) \ "@month").text
      val srcDate = ((data \\ "date")(0) \ "@date").text
      val srcDay = ((data \\ "date")(0) \ "@day").text
      val srcYear = ((data \\ "date")(0) \ "@year").text
      val srcHour = ((data \\ "time")(0) \ "@hour").text
      val srcMinute = ((data \\ "time")(0) \ "@minute").text
      val srcSecond = ((data \\ "time")(0) \ "@second").text
      val srcUtcHour = ((data \\ "time")(0) \ "@utc-hour").text
      val srcUtcMinute = ((data \\ "time")(0) \ "@utc-minute").text
      //get game date
      val month = (rowData \\ "date" \ "@month").text
      val date = (rowData \\ "date" \ "@date").text
      val day = (rowData \\ "date" \ "@day").text
      val year = (rowData \\ "date" \ "@year").text
      val hour = (rowData \\ "time" \ "@hour").text
      val minute = (rowData \\ "time" \ "@minute").text
      val utcHour = (rowData \\ "time" \ "@utc-hour").text
      val utcMinute = (rowData \\ "time" \ "@utc-minute").text

      val homeTeamExId = (rowData \\ "home-team" \\ "team-code" \ "@global-id").text
      val homeTeamAlias = (rowData \\ "home-team" \\ "team-name" \ "@alias").text
      val homeTeamName =  (rowData \\ "home-team" \\ "team-name" \ "@name").text
      val awayTeamAlias = (rowData \\ "visiting-team" \\ "team-name" \ "@alias").text
      val awayTeamExtId = (rowData \\ "visiting-team" \\ "team-code" \ "@global-id").text
      val awayTeamName =  (rowData \\ "visiting-team" \\ "team-name" \ "@name").text
      val gameId = (rowData \\ "gamecode" \ "@global-id").text
      val gameCode = (rowData \\ "gamecode" \ "@code").text
      val gameType = (rowData \\ "gametype" \ "@type").text
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
      val lastPlay = (rowData \\ "last-play" \ "@text").text
      val gameStatus = (rowData \\ "gamestate" \ "@status").text
      val gameStatusId = toInt((rowData \\ "gamestate" \ "@status-id").text).getOrElse(0)
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
      val message = BoxScoreData(homeTeamName,awayTeamName, division,firstGameBase,secondGameBase, thirdGameBase, month, date, day, year, hour, minute, utcHour, utcMinute,srcMonth, srcDate, srcDay, srcYear, srcHour, srcMinute, srcSecond, srcUtcHour, srcUtcMinute, homeTeamExId, homeTeamAlias, awayTeamAlias, awayTeamExtId, gameId, gameCode, gameType, awayScoreRuns, awayScoreHits, awayScoreErrors, homeScoreRuns, homeScoreHits, homeScoreErrors, lastPlay, gameStatus, gameStatusId, inningTitle, inningNo, balls, strikes, outs, segmentDivision, awayTeamInnings.toList, homeTeamInnings.toList, currentBatterId, currentBatterName, homeTeamCurrPitcherId, homeTeamCurrPitcherName, awayTeamCurrPitcherId, awayTeamCurrPitcherName)
      new SourceRecord(in.sourcePartition, in.sourceOffset, in.topic, 0, in.keySchema, in.key, message.connectSchema, message.getStructure)

    }
    rows.toList.asJava

  }

  case class BoxScoreData(homeTeamName:String,awayTeamName:String ,  division:String,  firstGameBase:String ,secondGameBase:String , thirdGameBase:String , month: String, date: String, day: String, year: String, hour: String, minute: String, utcHour: String, utcMinute: String, srcMonth: String, srcDate: String, srcDay: String, srcYear: String, srcHour: String, srcMinute: String, srcSecond: String, srcUtcHour: String, srcUtcMinute: String, homeTeamExtId: String, homeTeamAlias: String, awayTeamAlias: String, awayTeamExtId: String, gameId: String, gameCode: String, gameType: String, awayScoreRuns: Int, awayScoreHits: Int, awayScoreErrors: Int, homeScoreRuns: Int, homeScoreHits: Int, homeScoreErrors: Int, lastPlay: String, gameStatus: String, gameStatusId: Int, inningTitle: String, inningNo: Int, balls: Int, strikes: Int, outs: Int, segmentDivision: String, awayTeamInnings: List[Int], homeTeamInnings: List[Int], currentBatterId: String, currentBatterName: String, homeTeamCurrPitcherId: String, homeTeamCurrPitcherName: String, awayTeamCurrPitcherId: String, awayTeamCurrPitcherName: String) {

    val boxScoreSchema: Schema = SchemaBuilder.struct().name("c.s.s.s.Game")
      .field("srcMonth", Schema.STRING_SCHEMA)
      .field("srcDate", Schema.STRING_SCHEMA)
      .field("srcDay", Schema.STRING_SCHEMA)
      .field("srcYear", Schema.STRING_SCHEMA)
      .field("srcHour", Schema.STRING_SCHEMA)
      .field("srcMinute", Schema.STRING_SCHEMA)
      .field("srcSecond", Schema.STRING_SCHEMA)
      .field("srcUtcHour", Schema.STRING_SCHEMA)
      .field("srcUtcMinute", Schema.STRING_SCHEMA)
      .field("month", Schema.STRING_SCHEMA)
      .field("date", Schema.STRING_SCHEMA)
      .field("day", Schema.STRING_SCHEMA)
      .field("year", Schema.STRING_SCHEMA)
      .field("hour", Schema.STRING_SCHEMA)
      .field("minute", Schema.STRING_SCHEMA)
      .field("utcHour", Schema.STRING_SCHEMA)
      .field("utcMinute", Schema.STRING_SCHEMA)
      .field("status", Schema.STRING_SCHEMA)
      .field("statusId", Schema.INT32_SCHEMA)
      .field("gameType", Schema.STRING_SCHEMA)
      .field("division", Schema.STRING_SCHEMA) 
      .field("gameId", Schema.STRING_SCHEMA)
      .field("gameCode", Schema.STRING_SCHEMA)
      .field("lastPlay", Schema.STRING_SCHEMA)
      .field("inningTitle", Schema.STRING_SCHEMA)
      .field("inningNo", Schema.INT32_SCHEMA)
      .field("balls", Schema.INT32_SCHEMA)
      .field("strikes", Schema.INT32_SCHEMA)
      .field("outs", Schema.INT32_SCHEMA)
      .field("segmentDiv", Schema.STRING_SCHEMA)
      .field("homeTeamName", Schema.STRING_SCHEMA)
      .field("homeTeamAlias", Schema.STRING_SCHEMA)
      .field("homeTeamExtId", Schema.STRING_SCHEMA)
      .field("homeScoreRuns", Schema.INT32_SCHEMA)
      .field("homeScoreHits", Schema.INT32_SCHEMA)
      .field("homeScoreErrors", Schema.INT32_SCHEMA)
      .field("homeTeamInnings", SchemaBuilder.array(Schema.INT32_SCHEMA).build())
      .field("awayTeamName", Schema.STRING_SCHEMA)
      .field("awayTeamAlias", Schema.STRING_SCHEMA)
      .field("awayTeamExtId", Schema.STRING_SCHEMA)
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

    val boxScoreStruct: Struct = new Struct(boxScoreSchema)
      .put("srcMonth", srcMonth)
      .put("srcDate", srcDate)
      .put("srcDay", srcDay)
      .put("srcYear", srcYear)
      .put("srcHour", srcHour)
      .put("srcMinute", srcMinute)
      .put("srcSecond", srcSecond)
      .put("srcUtcHour", srcUtcHour)
      .put("srcUtcMinute", srcUtcMinute)
      .put("month", month )
      .put("date", date )
      .put("day",day )
      .put("year", year)
      .put("hour", hour)
      .put("minute", minute)
      .put("utcHour", utcHour)
      .put("utcMinute", utcMinute)
      .put("status", gameStatus)
      .put("statusId", gameStatusId)
      .put("gameType", gameType)
      .put("division", division)
      .put("gameId", gameId)
      .put("gameCode", gameCode)
      .put("lastPlay", lastPlay)
      .put("inningTitle", inningTitle)
      .put("inningNo", inningNo)
      .put("balls", balls)
      .put("strikes", strikes)
      .put("outs", outs)
      .put("segmentDiv", segmentDivision)
      .put("homeTeamName", homeTeamName)
      .put("homeTeamAlias", homeTeamAlias)
      .put("homeTeamExtId", homeTeamExtId)
      .put("homeScoreRuns", homeScoreRuns)
      .put("homeScoreHits", homeScoreHits)
      .put("homeScoreErrors", homeScoreErrors)
      .put("homeTeamInnings", homeTeamInnings.asJava)
      .put("awayTeamName", awayTeamName)
      .put("awayTeamAlias", awayTeamAlias)
      .put("awayTeamExtId", awayTeamExtId)
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