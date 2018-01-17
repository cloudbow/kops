package com.slingmedia.sportscloud.parsers.leagues.delegates.boxscore

import com.slingmedia.sportscloud.parsers.factory.ParsedItem
import com.slingmedia.sportscloud.parsers.model.League
import org.apache.kafka.connect.data.{Schema, SchemaBuilder, Struct}
import org.apache.kafka.connect.source.SourceRecord
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.xml.{Elem, Node}


class BoxScoreDataExtractor(data:Elem, rowData: Node) extends ParsedItem {

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

  val homeTeamExtId = (rowData \\ "home-team" \\ "team-code" \ "@global-id").text
  val homeTeamAlias = (rowData \\ "home-team" \\ "team-name" \ "@alias").text
  val homeTeamName =  (rowData \\ "home-team" \\ "team-name" \ "@name").text

  val awayTeamAlias = (rowData \\ "visiting-team" \\ "team-name" \ "@alias").text
  val awayTeamExtId = (rowData \\ "visiting-team" \\ "team-code" \ "@global-id").text
  val awayTeamName =  (rowData \\ "visiting-team" \\ "team-name" \ "@name").text

  var gameId = (rowData \\ "gamecode" \ "@global-id").text
  val gameCode = (rowData \\ "gamecode" \ "@code").text
  val gameType = (rowData \\ "gametype" \ "@type").text
  val gameStatus = (rowData \\ "gamestate" \ "@status").text
  val gameStatusId = toInt((rowData \\ "gamestate" \ "@status-id").text).getOrElse(0)

  val league = (data \\ "league" \ "@alias").text
  val lastPlay = (rowData \\ "last-play" \ "@details").text

}

case class BoxScoreSchemaGenerator(schemaBuilder: SchemaBuilder)
{

  schemaBuilder
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
    .field("homeTeamExtId", Schema.STRING_SCHEMA)
    .field("homeTeamAlias", Schema.STRING_SCHEMA)
    .field("homeTeamName", Schema.STRING_SCHEMA)
    .field("awayTeamAlias", Schema.STRING_SCHEMA)
    .field("awayTeamExtId", Schema.STRING_SCHEMA)
    .field("awayTeamName", Schema.STRING_SCHEMA)
    .field("gameId", Schema.STRING_SCHEMA)
    .field("gameCode", Schema.STRING_SCHEMA)
    .field("gameType", Schema.STRING_SCHEMA)
    .field("status", Schema.STRING_SCHEMA)
    .field("statusId", Schema.INT32_SCHEMA)
    .field("league", Schema.STRING_SCHEMA)
    .field("lastPlay", Schema.STRING_SCHEMA)

}

case class BoxScoreStructGenerator(struct: Struct, boxScoreExtractor: BoxScoreDataExtractor ) {
  struct
  .put("srcMonth",boxScoreExtractor.srcMonth)
  .put("srcDate",boxScoreExtractor.srcDate)
  .put("srcDay",boxScoreExtractor.srcDay)
  .put("srcYear",boxScoreExtractor.srcYear)
  .put("srcHour",boxScoreExtractor.srcHour)
  .put("srcMinute",boxScoreExtractor.srcMinute)
  .put("srcSecond",boxScoreExtractor.srcSecond)
  .put("srcUtcHour",boxScoreExtractor.srcUtcHour)
  .put("srcUtcMinute",boxScoreExtractor.srcUtcMinute)
  .put("month",boxScoreExtractor.month)
  .put("date",boxScoreExtractor.date)
  .put("day",boxScoreExtractor.day)
  .put("year",boxScoreExtractor.year)
  .put("hour",boxScoreExtractor.hour)
  .put("minute",boxScoreExtractor.minute)
  .put("utcHour",boxScoreExtractor.utcHour)
  .put("utcMinute",boxScoreExtractor.utcMinute)
  .put("homeTeamExtId",boxScoreExtractor.homeTeamExtId)
  .put("homeTeamAlias",boxScoreExtractor.homeTeamAlias)
  .put("homeTeamName",boxScoreExtractor.homeTeamName)
  .put("awayTeamAlias",boxScoreExtractor.awayTeamAlias)
  .put("awayTeamExtId",boxScoreExtractor.awayTeamExtId)
  .put("awayTeamName",boxScoreExtractor.awayTeamName)
  .put("gameId",boxScoreExtractor.gameId)
  .put("gameCode",boxScoreExtractor.gameCode)
  .put("gameType",boxScoreExtractor.gameType)
  .put("status",boxScoreExtractor.gameStatus)
  .put("statusId",boxScoreExtractor.gameStatusId)
  .put("league",boxScoreExtractor.league)
  .put("lastPlay",boxScoreExtractor.lastPlay)
}