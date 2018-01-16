package com.slingmedia.sportscloud.parsers.leagues.delegates.boxscore.impl.nfl

import com.slingmedia.sportscloud.parsers.factory.ParsedItem
import com.slingmedia.sportscloud.parsers.model.League
import com.slingmedia.sportscloud.parsers.leagues.delegates.boxscore.BoxScoreDataExtractor
import com.slingmedia.sportscloud.parsers.leagues.delegates.boxscore.BoxScoreStructGenerator
import com.slingmedia.sportscloud.parsers.leagues.delegates.boxscore.BoxScoreSchemaGenerator

import org.apache.kafka.connect.data.{Schema, SchemaBuilder, Struct}
import org.apache.kafka.connect.source.SourceRecord

import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.xml.Elem

class NflBoxScoreParserDelegate extends ParsedItem{

  private val log = LoggerFactory.getLogger("NflBoxScoreParserDelegate")

  val quarterStrings = Array("1st Quarter ", "2nd Quarter ", "3rd Quarter ", "4th Quarter ", "Overtime ",
    "2nd Overtime ", "3rd Overtime ", "4th Overtime ", "5th Overtime ", "6th Overtime ");


  def secondsFromGameTimeStr(gameTimeString: String): Int = {
    val times: Array[String] = gameTimeString.split(":")
    if (times.length != 2) 0
    val minutes: Int = toInt(times(0)).getOrElse(0)
    val seconds: Int = toInt(times(1)).getOrElse(0)
    minutes * 60 + seconds
  }

   def generateRows(data: Elem, in: SourceRecord, xmlRoot: scala.xml.NodeSeq, quarterTime:Int, gameDuration:Int): java.util.List[SourceRecord] = {
    log.trace("Parsing rows for boxscore")
    val leagueStr = (data \\ "league" \ "@alias").text

    //var mlbBoxScores = scala.collection.mutable.ListBuffer.empty[SourceRecord]
    val rows = xmlRoot.map { rowData =>
      val commonFields = new BoxScoreDataExtractor(data,rowData)

      val division = leagueStr
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

        val gameseconds = toInt(period).getOrElse(0).toDouble * quarterTime * 60 - gameTimeSeconds
        val total = (gameDuration * 60).toDouble
        position = if( gameseconds != 0)   { gameseconds / total * 100 }  else { 0.0 };

        gameTimeSeconds = quarterTime * 60 - gameTimeSeconds

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

      val message = NflBoxScoreData(
        commonFields,
        division,
        homeTeamlineScore.toList,
        awayTeamlineScore.toList,
        period,
        positionVal,
        timer,
        playType,
        drivesList.toList,
        inningNo,
        gameTimeSecondsVal,
        homeScore,
        awayScore)
      new SourceRecord(in.sourcePartition,
        in.sourceOffset,
        in.topic, 0,
        in.keySchema,
        in.key,
        message.connectSchema,
        message.getStructure)

    }
    rows.toList.asJava

  }

  case class NflBoxScoreData(commonFields:BoxScoreDataExtractor,
                             division: String,
                             homeTeamlineScore:List[Int],
                             awayTeamlineScore: List[Int],
                             period: String,
                             position: Double,
                             timer: String,
                             playType: String,
                             drives: List[java.util.Map[String, String]],
                             inningNo: Int,
                             gameTimeSeconds: Int,
                             homeScore : Int,
                             awayScore : Int) {
    val boxScoreSchemaInited = SchemaBuilder.struct().name("c.s.s.s.Game")
    val boxScoreCommonSchema = BoxScoreSchemaGenerator(boxScoreSchemaInited)
    val boxScoreSchema: Schema = boxScoreSchemaInited
      .field("homeTeamlineScore", SchemaBuilder.array(Schema.INT32_SCHEMA).build())
      .field("awayTeamlineScore", SchemaBuilder.array(Schema.INT32_SCHEMA).build())
      .field("drives", SchemaBuilder.array(SchemaBuilder.map(Schema.STRING_SCHEMA,Schema.STRING_SCHEMA).build()).build())
      .field("period", Schema.STRING_SCHEMA)
      .field("position", Schema.FLOAT64_SCHEMA)
      .field("timer", Schema.STRING_SCHEMA)
      .field("playType", Schema.STRING_SCHEMA)
      .field("inningNo", Schema.INT32_SCHEMA)
      .field("gameTimeSeconds", Schema.INT32_SCHEMA)
      .field("homeScore", Schema.INT32_SCHEMA)
      .field("awayScore", Schema.INT32_SCHEMA)
      .build()

    val connectSchema: Schema = boxScoreSchema

    val boxScoreStructInited = new Struct(boxScoreSchema)

    val boxScoreCommonStruct = BoxScoreStructGenerator(boxScoreStructInited,commonFields)
    val boxScoreStruct: Struct = boxScoreStructInited
      .put("homeTeamlineScore", homeTeamlineScore.asJava)
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