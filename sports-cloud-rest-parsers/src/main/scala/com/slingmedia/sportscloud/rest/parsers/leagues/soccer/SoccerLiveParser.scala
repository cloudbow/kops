package com.slingmedia.sportscloud.rest.parsers.leagues.soccer

import java.net.{HttpURLConnection, URL}
import play.api.libs.json._
import org.slf4j.LoggerFactory
import sys.process._
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

object SoccerCMHolder extends Serializable {
  val serialVersionUID = 1L
  @transient lazy val log = LoggerFactory.getLogger("SoccerLiveParser")
}

object SoccerLiveParser extends Serializable {
  def main(args: Array[String]) {
    SoccerCMHolder.log.info("Args is $args")


    val scheduledExecutorService : ScheduledExecutorService= Executors.newScheduledThreadPool(5)

    val scheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(
      new Runnable {
        override def run(): Unit = new SoccerLiveParser().parse(args(0))
      } ,5, 15, TimeUnit.SECONDS)

  }
}

class SoccerLiveParser {

  implicit private val StatusWrites: Writes[LiveUpdate] = Json.writes[LiveUpdate]


  //val objects

  def parse(inputTopic: String) : Unit = {
    var response = ""
    try {
       response = get("http://gwserv-mobileprod.echodata.tv/Gamefinder/api/game/search?page.size=300")
       SoccerCMHolder.log.info("response "+ response)
    } catch {
      case e: Exception => throw new RuntimeException

    }
    //val response = get("http://gwserv-mobileprod.echodata.tv/Gamefinder/api/game/search?page.size=300")
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
      val status = (soccerGame \ "gameStatus").asInstanceOf[JsString].value
      val scheduledDate = (soccerGame \ "scheduledDate").asInstanceOf[JsString].value
      val livestatus = liveUpdate(gameId = gameId.toString(), gameCode = gameId.toString(), gameStatus=status,
        scheduledDate= scheduledDate,homeScore = homeScore.toInt, awayScore=awayScore.toInt, homeTeamlineScore = homeTeamlineScore,
        awayTeamlineScore = awayTeamlineScore, statusId = statusId.toInt)

      val liveJson = Json.obj("payload" ->Json.toJson(livestatus))
      val schema = createSchemaForJson
      val schemaJosn = Json.obj("schema" -> schema)
      val finalJson = Json.obj("payload" ->Json.toJson(livestatus),"schema" -> schema)
      val valuesJson = Json.obj("value" ->finalJson)

      valuesJson
    })

    val records = Json.obj("records" -> JsArray(gamesLive))

    val restProxyEndPoint = System.getenv("KAFKA_REST_EP")
    try {
      val uploadToTopic = Seq("curl", "-X", "POST", s"http://$restProxyEndPoint/topics/$inputTopic",
        "-H", "Content-Type:application/vnd.kafka.json.v2+json", "-H", "Accept: application/vnd.kafka.v2+json",
        "-d", s"$records")
      uploadToTopic.!!
    } catch {
      case e: Exception => SoccerCMHolder.log.info("exception caught: " + e);
    }
    SoccerCMHolder.log.info("uploaded to rest")

  }

  def createSchemaForJson (): JsValue = {

    val schemaJson =
      """
         {"type":"struct","fields":[{"type":"string","optional":false,"field":"gameId"},{"type":"string","optional":false,"field":"gameCode"},
         {"type":"string","optional":false,"field":"gameStatus"}, {"type":"string","optional":false,"field":"scheduledDate"}
         ,{"type":"int32","optional":false,"field":"homeScore"},
         {"type":"int32","optional":false,"field":"awayScore"},{"type":"array","items":{"type":"int32","optional":true},
         "optional":false,"field":"homeTeamlineScore"},
         {"type":"array","items":{"type":"int32","optional":false},"optional":true,"field":"awayTeamlineScore"},
          {"type":"int32","optional":false,"field":"statusId"}],"optional":false,"name":"c.s.s.s.Live"}
      """.replace("\r", "").stripMargin
    Json.parse(schemaJson)
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

case class LiveUpdate(gameId: String, gameCode:String, gameStatus:String, scheduledDate: String, homeScore: Int,
                      awayScore: Int, homeTeamlineScore: JsArray, awayTeamlineScore: JsArray, statusId: Int)

