package com.slingmedia.sportscloud.parsers.leagues.impl.soccer

import java.net.{HttpURLConnection, URL}
import play.api.libs.json._
import org.slf4j.LoggerFactory
import sys.process._


object SoccerCMHolder extends Serializable {
  val serialVersionUID = 1L
  @transient lazy val log = LoggerFactory.getLogger("SoccerLiveParser")
}

object SoccerLiveParser extends Serializable {
  def main(args: Array[String]) {
    SoccerCMHolder.log.debug("Args is $args")

    new SoccerLiveParser().parse("dd")

  }
}

class SoccerLiveParser {

  implicit private val StatusWrites: Writes[LiveUpdate] = Json.writes[LiveUpdate]


  //val objects

  def parse(inputTopic: String) : Unit = {

    val response = get("http://gwserv-mobileprod.echodata.tv/Gamefinder/api/game/search?page.size=300")
    val parsed = Json.parse(response)
    val content : List[JsObject] = (parsed \ "content").as[List[JsObject]]
    val soccerGames = content.filter(OnlySoccer)
    val liveUpdate = LiveUpdate
    var gameRecord: JsArray = new JsArray()
    val gamesLive = soccerGames.map( f = soccerGame => {
      val gameId, gameCode = (soccerGame \ "id")
      val homeTeamlineScore : JsArray = (soccerGame \ "homeTeam" \ "scoreDetails").asInstanceOf[JsArray]
      val awayTeamlineScore : JsArray = (soccerGame \ "awayTeam" \ "scoreDetails").asInstanceOf[JsArray]
      val homeScore: BigDecimal = (soccerGame \ "homeScore").asInstanceOf[JsNumber].value
      val awayScore: BigDecimal = (soccerGame \ "awayScore").asInstanceOf[JsNumber].value
      val statusId: BigDecimal = (soccerGame \ "statusId").asInstanceOf[JsNumber].value
      val status = (soccerGame \ "gameStatus")
      val livestatus = liveUpdate(gameId = gameId.toString(), gameCode = gameId.toString(), homeScore = homeScore.toInt,
        awayScore=awayScore.toInt, homeTeamlineScore = homeTeamlineScore,
        awayTeamlineScore = awayTeamlineScore, statusId = statusId.toInt)

      val liveJson = Json.obj("value" ->Json.toJson(livestatus))
      liveJson
    })
    val records = Json.obj("records" -> JsArray(gamesLive))


    val deploy = Seq("curl", "-X", "POST", "http://localhost:8082/topics/jsontest1",
      "-H", "Content-Type:application/vnd.kafka.json.v2+json", "-H", "Accept: application/vnd.kafka.v2+json",
      "-d", s"$records")
    deploy.!!

    //println(stautCode)
    //push it kafka queues
  }

  def OnlySoccer(a: JsObject) = a match {
    case s: JsObject => (a \ "sport").asInstanceOf[JsString].value.equals("soccer")
    case _ => false
  }

  def get(url: String,
          connectTimeout: Int = 15000,
          readTimeout: Int = 15000,
          requestMethod: String = "GET") =
  {
    val connection = (new URL(url)).openConnection.asInstanceOf[HttpURLConnection]
    connection.setConnectTimeout(connectTimeout)
    connection.setReadTimeout(readTimeout)
    connection.setRequestMethod(requestMethod)
    val inputStream = connection.getInputStream
    val content = scala.io.Source.fromInputStream(inputStream).mkString
    if (inputStream != null) inputStream.close
    content
  }

}

case class LiveUpdate(gameId: String, gameCode:String, homeScore: Int, awayScore: Int, homeTeamlineScore: JsArray,
                      awayTeamlineScore: JsArray, statusId: Int)

