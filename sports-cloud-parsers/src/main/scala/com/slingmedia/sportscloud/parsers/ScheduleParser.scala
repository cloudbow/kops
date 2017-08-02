package com.slingmedia.sportscloud.parsers

import com.eneco.trading.kafka.connect.ftp.source.SourceRecordConverter
import org.apache.kafka.connect.source.SourceRecord
import org.apache.kafka.connect.data.{ Schema, SchemaBuilder, Struct }
import scala.collection.JavaConverters._
import java.util
import scala.xml.Elem
import com.slingmedia.sportscloud.parsers.factory.ParsedItem
import org.slf4j.LoggerFactory;
import com.typesafe.scalalogging.slf4j.Logger

class ScheduleParser extends ParsedItem {
  private val log = LoggerFactory.getLogger("ScheduleParser")

  override def generateRows(data:Elem,in:SourceRecord,league:String,sport:String):java.util.List[SourceRecord] = {
    log.trace("Generating rows for schedule parsing")
    val rows = (data \\ "game-schedule").map { rowData =>
      val visitingTeamScore = (rowData \\ "visiting-team-score" \ "@score").toString
      val homeTeamScore = (rowData \\ "home-team-score" \ "@score").toString
      val homeStartingPitcher = (rowData \\ "home-starting-pitcher" \\ "name" \ "@first-name").toString
      val awayStartingPitcher = (rowData \\ "away-starting-pitcher" \\ "name" \ "@first-name").toString
      val awaySPExtId = (rowData \\ "away-starting-pitcher" \\ "player-code" \ "@global-id").toString
      val homeSPExtId = (rowData \\ "home-starting-pitcher" \\ "player-code" \ "@global-id").toString
      val homeTeamName = (rowData \\ "home-team" \\ "team-name" \ "@name").toString
      val homeTeamCity = (rowData \\ "home-team" \\ "team-city" \ "@city").toString
      val homeTeamExtId = (rowData \\ "home-team" \\ "team-code" \ "@global-id").toString
      val awayTeamName = (rowData \\ "visiting-team" \\ "team-name" \ "@name").toString
      val awayTeamCity = (rowData \\ "visiting-team" \\ "team-city" \ "@city").toString
      val awayTeamExtId = (rowData \\ "visiting-team" \\ "team-code" \ "@global-id").toString
      val stadiumName = (rowData \\ "stadium" \ "@name").toString
      val month = (rowData \\ "date" \ "@month").toString
      val date = (rowData \\ "date" \ "@date").toString
      val day = (rowData \\ "date" \ "@day").toString
      val year = (rowData \\ "date" \ "@year").toString
      val hour = (rowData \\ "time" \ "@hour").toString
      val minute = (rowData \\ "time" \ "@minute").toString
      val utcHour = (rowData \\ "time" \ "@utc-hour").toString
      val utcMinute = (rowData \\ "time" \ "@utc-minute").toString
      val gameCode = (rowData \\ "gamecode" \ "@global-id").toString
      val message = MLBSchedule(league, sport, stadiumName, visitingTeamScore, homeTeamScore, homeStartingPitcher, awayStartingPitcher,awaySPExtId,homeSPExtId, homeTeamName, homeTeamCity, homeTeamExtId, awayTeamName, awayTeamCity, awayTeamExtId, month, date, day, year, hour, minute, utcHour, utcMinute, gameCode)
      new SourceRecord(in.sourcePartition, in.sourceOffset, in.topic, 0, in.keySchema, in.key, message.connectSchema, message.getStructure)
    }
    log.trace("Generated rows")
    rows.toList.asJava
  }

  case class MLBSchedule(league:String, sport:String, stadiumName: String, visitingTeamScore: String, homeTeamScore: String, homeStartingPitcher: String, awayStartingPitcher: String, awaySPExtId:String,homeSPExtId:String, homeTeamName: String, homeTeamCity: String, homeTeamExternalId: String, awayTeamName: String, awayTeamCity: String, awayTeamExternalId: String, month: String, date: String, day: String, year: String, hour: String, minute: String, utcHour: String, utcMinute: String, gameCode: String) {
    log.trace("preparing schema")
    val scoreSchema: Schema = SchemaBuilder.struct().name("c.s.s.s.Score").field("score", Schema.STRING_SCHEMA).build()
    val stadiumSchema: Schema = SchemaBuilder.struct().name("c.s.s.s.Stadium").field("name", Schema.STRING_SCHEMA).build()
    val playerDataItemSchema: Schema = SchemaBuilder.struct().name("c.s.s.s.Name").field("name", Schema.STRING_SCHEMA).field("player-code", Schema.STRING_SCHEMA).build()
    val playerDataSchema: Schema = SchemaBuilder.struct().name("c.s.s.s.PlayerData").field("player-data", playerDataItemSchema).build()
    val teamSchema: Schema = SchemaBuilder.struct().name("c.s.s.s.Team").field("team-name", Schema.STRING_SCHEMA).field("team-city", Schema.STRING_SCHEMA).field("team-code", Schema.STRING_SCHEMA).build()
    val dateSchema: Schema = SchemaBuilder.struct().name("c.s.s.s.Date").field("month", Schema.STRING_SCHEMA).field("date", Schema.STRING_SCHEMA).field("day", Schema.STRING_SCHEMA).field("year", Schema.STRING_SCHEMA).build()
    val timeSchema: Schema = SchemaBuilder.struct().name("c.s.s.s.Time").field("hour", Schema.STRING_SCHEMA).field("minute", Schema.STRING_SCHEMA).field("utc-hour", Schema.STRING_SCHEMA).field("utc-minute", Schema.STRING_SCHEMA).build()

    val gameScheduleItemSchema: Schema = SchemaBuilder.struct().name("c.s.s.s.GameScheduleItem")
      .field("gamecode", Schema.STRING_SCHEMA)
      .field("league", Schema.STRING_SCHEMA)
      .field("sport", Schema.STRING_SCHEMA)
      .field("stadium", stadiumSchema)
      .field("home-team-score", scoreSchema)
      .field("visiting-team-score", scoreSchema)
      .field("away-starting-pitcher", playerDataSchema)
      .field("home-starting-pitcher", playerDataSchema)
      .field("home-team", teamSchema)
      .field("visiting-team", teamSchema)
      .field("date", dateSchema)
      .field("time", timeSchema)
      .build()
    val gameScheduleSchema: Schema = SchemaBuilder.struct().name("c.s.s.s.BaseballGameSchedule").field("game-schedule", gameScheduleItemSchema).build()

    val connectSchema: Schema = gameScheduleSchema

    val stadiumStruct: Struct = new Struct(stadiumSchema).put("name", stadiumName)

    val visitingTeamScoreStruct: Struct = new Struct(scoreSchema).put("score", visitingTeamScore)
    val homeTeamScoreStruct: Struct = new Struct(scoreSchema).put("score", homeTeamScore)

    val nameSchemaHtStruct: Struct = new Struct(playerDataItemSchema).put("name", homeStartingPitcher).put("player-code",homeSPExtId)
    val nameSchemaAtStruct: Struct = new Struct(playerDataItemSchema).put("name", awayStartingPitcher).put("player-code",awaySPExtId)

    val startingPitcherHtStruct: Struct = new Struct(playerDataSchema).put("player-data", nameSchemaHtStruct)
    val startingPitcherAtStruct: Struct = new Struct(playerDataSchema).put("player-data", nameSchemaAtStruct)

    val teamHtStruct: Struct = new Struct(teamSchema).put("team-name", homeTeamName).put("team-city", homeTeamCity).put("team-code", homeTeamExternalId)
    val teamAtStruct: Struct = new Struct(teamSchema).put("team-name", awayTeamName).put("team-city", awayTeamCity).put("team-code", awayTeamExternalId)

    val dateStruct: Struct = new Struct(dateSchema).put("month", month).put("date", date).put("day", day).put("year", year)
    val timeStruct: Struct = new Struct(timeSchema).put("hour", hour).put("minute", minute).put("utc-hour", utcHour).put("utc-minute", utcMinute)

    val gameScheduleItemStruct: Struct = new Struct(gameScheduleItemSchema)
      .put("stadium", stadiumStruct)
      .put("visiting-team-score", visitingTeamScoreStruct)
      .put("home-team-score", homeTeamScoreStruct)
      .put("away-starting-pitcher", startingPitcherAtStruct)
      .put("home-starting-pitcher", startingPitcherHtStruct)
      .put("home-team", teamHtStruct)
      .put("visiting-team", teamAtStruct)
      .put("date", dateStruct)
      .put("time", timeStruct)
      .put("gamecode", gameCode)
      .put("league", league)
      .put("sport", sport)
      
      
    val gameScheduleStruct: Struct = new Struct(gameScheduleSchema).put("game-schedule", gameScheduleItemStruct)

    def getStructure: Struct = gameScheduleStruct
    
    log.trace("prepared schema")
  }
}