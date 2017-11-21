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
 
import scala.collection.immutable.HashMap
import scala.collection.immutable.Map

class NcaafBoxScoreParser extends ParsedItem {

  private val log = LoggerFactory.getLogger("NcaafBoxScoreParser")
  
  val quarterStrings = Array("1st Quarter ", "2nd Quarter ", "3rd Quarter ", "4th Quarter ", "Overtime ", 
    "2nd Overtime ", "3rd Overtime ", "4th Overtime ", "5th Overtime ", "6th Overtime ");
  
  
  def secondsFromGameTimeStr(gameTimeString: String): Int = {
    val times: Array[String] = gameTimeString.split(":")
    if (times.length != 2) 0
    val minutes: Int = toInt(times(0)).getOrElse(0)
    val seconds: Int = toInt(times(1)).getOrElse(0)
    minutes * 60 + seconds
  }

  override def generateRows(data: Elem, in: SourceRecord, xmlRoot: scala.xml.NodeSeq): java.util.List[SourceRecord] = {
    log.trace("Parsing rows for boxscore")
    val leagueStr = (data \\ "league" \ "@alias").text
    val league = League.withNameOpt(leagueStr.toUpperCase)

    //var mlbBoxScores = scala.collection.mutable.ListBuffer.empty[SourceRecord]
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
      val inningNo = toInt((rowData \\ "gamestate" \ "@segment-number").text).getOrElse(0)
      
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
      
       val lastPlay = (rowData \\ "last-play" \ "@details").text
       val period = (rowData \\ "last-play" \ "@quarter").text
       var position  = 0.0
       val timer  = (rowData \\ "last-play" \ "@time").text
       val playType = (rowData \\ "last-play" \ "@play-type").text
       
       val lastPlayMins = (rowData \\ "last-play" \ "@time-minutes").text
       val lastPlaySecs = (rowData \\ "last-play" \ "@time-seconds").text
      var gameTimeSeconds = 0
     
      if (timer != "" || lastPlayMins != "" && lastPlaySecs != "") {
          if(timer != "") {
              gameTimeSeconds = secondsFromGameTimeStr(timer);
          } else {
              var mins = toInt(lastPlayMins).getOrElse(0)
              var secs = toInt(lastPlaySecs).getOrElse(0)
              gameTimeSeconds = mins * 60 + secs;
          }
      
          val gameseconds = toInt(period).getOrElse(0).toDouble * 12 * 60 - gameTimeSeconds
          val total = (48 * 60).toDouble
          position = if( gameseconds != 0)   { gameseconds / total * 100 }  else { 0.0 };
      
          gameTimeSeconds = 12 * 60 - gameTimeSeconds
      
      } else {

          position = 100
      } 
       
       val drivesList = scala.collection.mutable.ListBuffer.empty[java.util.Map[String, String]]
       (rowData \\ "scoring-summaries" \\ "scoring-summary").map { scoringSummary => 
         //val drivesSummary = scala.collection.mutable.Map.empty[String, String]
         val quarterDrive = (scoringSummary \ "@quarter").text
         
         ( scoringSummary \\ "score-summary").map { scoreSummary =>
            val drivesSummary = scala.collection.Map.empty[String, String]
             val summaryText = (scoreSummary \\ "summary-text" \ "@text").text
             val min = (scoreSummary  \ "@minutes").text
             val seconds = (scoreSummary  \ "@seconds").text
             val teamId = (scoreSummary \\ "team-code" \ "@global-id").text
             
            // val endMins = (min == "" ? "0" : min)
             val endMins = if (min == "") "0" else min
                                val endSecs = if(seconds == "")  "0"  else seconds
              var endQuarter = quarterDrive.toInt - 1
                                var startQuarter = endQuarter;
              val drive = (scoreSummary  \\ "drive")
              var driveTitle = "";
              if (drive.length > 0) {
                  var startMins = toInt(endMins).getOrElse(0) + toInt((scoreSummary  \\ "drive" \ "@minutes").text).getOrElse(0);
                  var startSecs = toInt(endSecs).getOrElse(0) + toInt((scoreSummary  \\ "drive" \ "@seconds").text).getOrElse(0);
                  if (startSecs >= 60) {
                      startSecs -= 60;
                      startMins+=1;
                  }
                  if(startMins >= 15) {
                      if(startMins > 15 || startSecs > 0) {
                          startQuarter-=1;
                      }
                  }
                  if(startQuarter != endQuarter) {
                      driveTitle = "%s%d:%02d - %s%d:%02d".format( quarterStrings(startQuarter), startMins, startSecs, quarterStrings(endQuarter), toInt(endMins).getOrElse(0), toInt(endSecs).getOrElse(0));
                  } else {
                      driveTitle = "%s%d:%02d - %d:%02d".format( quarterStrings(endQuarter), startMins, startSecs, toInt(endMins).getOrElse(0), toInt(endSecs).getOrElse(0));
                  }
              } else {
                  driveTitle = "%s%d:%02d".format( quarterStrings(toInt(quarterDrive).getOrElse(1) - 1), toInt(endMins).getOrElse(0), toInt(endSecs).getOrElse(0));
              }
             val drivesSummary1 = drivesSummary + ("quarter" -> s"$quarterDrive", "summaryText" -> s"$summaryText")
             val drivesSummary2 = drivesSummary1 + ("teamId" -> s"$teamId", "minutes" -> s"$min", "seconds" -> s"$seconds", "driveTitle" ->s"$driveTitle")
             
             drivesList += drivesSummary2.asJava
         }
        
        }
       
      
      val gameTimeSecondsVal = gameTimeSeconds
      val positionVal = position
      val gameStatus = (rowData \\ "gamestate" \ "@status").text
      val gameStatusId = toInt((rowData \\ "gamestate" \ "@status-id").text).getOrElse(0)

      val message = NcaafBoxScoreData(homeTeamName,awayTeamName, division, month, date, day, year, hour, minute, utcHour, 
          utcMinute,srcMonth, srcDate, srcDay, srcYear, srcHour, srcMinute, srcSecond, srcUtcHour, srcUtcMinute, 
          homeTeamExId, homeTeamAlias, awayTeamAlias, awayTeamExtId, gameId, gameCode, gameType, lastPlay, gameStatus, 
          gameStatusId, homeTeamlineScore.toList, awayTeamlineScore.toList, period, positionVal, timer, playType, drivesList.toList,
          inningNo, gameTimeSecondsVal, awayScore, homeScore)
      new SourceRecord(in.sourcePartition, in.sourceOffset, in.topic, 0, in.keySchema, in.key, message.connectSchema, message.getStructure)

    }
    rows.toList.asJava

  }

  case class NcaafBoxScoreData(homeTeamName:String,awayTeamName:String ,  division:String , month: String, date: String, day: String, 
      year: String, hour: String, minute: String, utcHour: String, utcMinute: String, srcMonth: String, srcDate: String, 
      srcDay: String, srcYear: String, srcHour: String, srcMinute: String, srcSecond: String, srcUtcHour: String, srcUtcMinute: String, 
      homeTeamExtId: String, homeTeamAlias: String, awayTeamAlias: String, awayTeamExtId: String, gameId: String, gameCode: String, 
      gameType: String, lastPlay: String, gameStatus: String, gameStatusId: Int, homeTeamlineScore:List[Int], 
      awayTeamlineScore: List[Int], period: String, position: Double, timer: String, playType: String, drives: List[java.util.Map[String, String]]
  , inningNo: Int, gameTimeSeconds: Int, awayScore : Int, homeScore : Int) {

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
      .field("homeTeamName", Schema.STRING_SCHEMA)
      .field("homeTeamAlias", Schema.STRING_SCHEMA)
      .field("homeTeamExtId", Schema.STRING_SCHEMA)
      .field("homeTeamlineScore", SchemaBuilder.array(Schema.INT32_SCHEMA).build())
      .field("awayTeamName", Schema.STRING_SCHEMA)
      .field("awayTeamAlias", Schema.STRING_SCHEMA)
      .field("awayTeamExtId", Schema.STRING_SCHEMA)
      .field("awayTeamlineScore", SchemaBuilder.array(Schema.INT32_SCHEMA).build())
      .field("drives", SchemaBuilder.array(SchemaBuilder.map(Schema.STRING_SCHEMA,Schema.STRING_SCHEMA).build()).build())
      .field("period", Schema.STRING_SCHEMA)
      .field("position", Schema.FLOAT64_SCHEMA)
      .field("timer", Schema.STRING_SCHEMA)
      .field("playType", Schema.STRING_SCHEMA)
      .field("inningNo", Schema.INT32_SCHEMA)
      .field("gameTimeSeconds", Schema.INT32_SCHEMA)
      .field("awayScore", Schema.INT32_SCHEMA)
      .field("homeScore", Schema.INT32_SCHEMA)
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
      .put("homeTeamName", homeTeamName)
      .put("homeTeamAlias", homeTeamAlias)
      .put("homeTeamExtId", homeTeamExtId)
      .put("homeTeamlineScore", homeTeamlineScore.asJava)
      .put("awayTeamName", awayTeamName)
      .put("awayTeamAlias", awayTeamAlias)
      .put("awayTeamExtId", awayTeamExtId)
      .put("awayTeamlineScore", awayTeamlineScore.asJava)
      .put("drives", drives.asJava)
      .put("period", period)
      .put("position", position)
      .put("timer", timer)
      .put("playType", playType)
      .put("inningNo", inningNo)
      .put("gameTimeSeconds", gameTimeSeconds)
      .put("awayScore", awayScore)
      .put("homeScore", homeScore)
    def getStructure: Struct = boxScoreStruct

  }

}