/*
 * AbstractSportsCloudRestDelegate.java
 * @author arung
 **********************************************************************

             Copyright (c) 2004 - 2018 by Sling Media, Inc.

All rights are reserved.  Reproduction in whole or in part is prohibited
without the written consent of the copyright owner.

Sling Media, Inc. reserves the right to make changes without notice at any time.

Sling Media, Inc. makes no warranty, expressed, implied or statutory, including
but not limited to any implied warranty of merchantability of fitness for any
particular purpose, or that the use will not infringe any third party patent,
copyright or trademark.

Sling Media, Inc. must not be liable for any loss or damage arising from its
use.

This Copyright notice may not be removed or modified without prior
written consent of Sling Media, Inc.

 ***********************************************************************//*
 * AbstractSportsCloudRestDelegate.java
 * @author arung
 **********************************************************************

             Copyright (c) 2004 - 2018 by Sling Media, Inc.

All rights are reserved.  Reproduction in whole or in part is prohibited
without the written consent of the copyright owner.

Sling Media, Inc. reserves the right to make changes without notice at any time.

Sling Media, Inc. makes no warranty, expressed, implied or statutory, including
but not limited to any implied warranty of merchantability of fitness for any
particular purpose, or that the use will not infringe any third party patent,
copyright or trademark.

Sling Media, Inc. must not be liable for any loss or damage arising from its
use.

This Copyright notice may not be removed or modified without prior
written consent of Sling Media, Inc.

 ***********************************************************************/
package com.slingmedia.sportscloud.netty.rest.server.handler.delegates

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util
import java.util.UUID
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.slingmedia.sportscloud.facade._
import com.slingmedia.sportscloud.netty.rest.model.ActiveTeamGame
import com.slingmedia.sportscloud.netty.rest.model.GameAvailability
import com.slingmedia.sportscloud.netty.rest.model.GameStatus
import com.slingmedia.sportscloud.netty.rest.model.GameType
import com.slingmedia.sportscloud.netty.rest.model.Role
import com.slingmedia.sportscloud.netty.rest.server.config.SportsCloudRestConfig

/**
  * Abstract Response modeling for web-view Sports Home screen and Media card
  *
  * @author arung
  * @version 1.0
  * @since 1.0
  */
object AbstractSportsCloudRestDelegate {
  private val LOGGER = LoggerFactory.getLogger(classOf[AbstractSportsCloudRestDelegate])
  private[delegates] val FIELD_STATE_MAP = Array[Int](0, 1, 2, 4, 3, 7, 5, 6)
}

class AbstractSportsCloudRestDelegate {
  /**
    * Returns the game schedule data for given game id.
    *
    * @param gameId
    * the game id
    * @return the game schedule in JsonFormat
    */
    def getGameForGameId(gameId: String): JsonArray = {
      if (AbstractSportsCloudRestDelegate.LOGGER.isTraceEnabled) AbstractSportsCloudRestDelegate.LOGGER.trace(String.format("Getting date for %s", gameId))
      val currentGameRespJson = SportsDataFacade.MODULE$.getGameScheduleByGameCode(gameId)
      val currGameDocs = currentGameRespJson.getAsJsonObject.get("hits").getAsJsonObject.get("hits").getAsJsonArray
      currGameDocs
    }

  /**
    * Returns the channel info for each content
    *
    * @param gameResponseArr
    * the game data index response
    * @return the content channel info map
    */
  def createContentIdAssetInfoMap(gameResponseArr: JsonArray): JsonObject = {
    val contentIdChGuid = new JsonObject
    gameResponseArr.forEach((it: JsonElement) => {
      def foo(it: JsonElement) = {
        val gameJsonObj = it.getAsJsonObject.get("_source").getAsJsonObject
        if (gameJsonObj.has("channel_guid") && gameJsonObj.has("schedule_guid")) {
          val assetInfoJson = new JsonObject
          contentIdChGuid.add(gameJsonObj.get("schedule_guid").getAsString, assetInfoJson)
          assetInfoJson.add("channelGuid", new JsonPrimitive(gameJsonObj.get("channel_guid").getAsString))
          assetInfoJson.add("assetGuid", new JsonPrimitive(gameJsonObj.get("asset_guid").getAsString))
          assetInfoJson.add("callsign", new JsonPrimitive(gameJsonObj.get("callsign").getAsString))
        }
      }

      foo(it)
    })
    contentIdChGuid
  }

  /**
    * This method returns an active team + the game selected for the media card
    *
    * @param teamId
    * the team id
    * @param currGameDocs
    * the current all games
    * @return the active team game details
    */
  def getActiveTeamGame(teamId: String, currGameDocs: JsonArray): ActiveTeamGame = {
    var role = Role.NONE
    var homeTeamId = "0"
    var awayTeamId = "0"
    var awayTeamName = "-"
    var homeTeamName = "-"
    var gameId = "0"
    var gameType = GameType.REGULAR_SEASON
    import scala.collection.JavaConversions._
    for (doc <- currGameDocs) {
      val currGamesDoc = doc.getAsJsonObject.get("_source").getAsJsonObject
      homeTeamId = currGamesDoc.get("homeTeamExternalId").getAsString
      awayTeamId = currGamesDoc.get("awayTeamExternalId").getAsString
      awayTeamName = currGamesDoc.get("awayTeamName").getAsString
      homeTeamName = currGamesDoc.get("homeTeamName").getAsString
      gameId = currGamesDoc.get("gameId").getAsString
      gameType = GameType.getValue(currGamesDoc.get("gameType").getAsString)
    }
    if (teamId == homeTeamId) role = Role.HOME
    else if (teamId == awayTeamId) role = Role.AWAY
    new ActiveTeamGame(gameId, gameType, teamId, homeTeamId, awayTeamId, homeTeamName, awayTeamName, role)
  }

  /**
    * Prepares the live game model
    *
    * @param startDate
    * the start date
    * @param endDate
    * the end date
    * @param sizeToReturn
    * the limit for fetching upcoming games
    * @return the live game model
    */
  def prepareLiveGameInfoData(startDate: Long, endDate: Long, sizeToReturn: Int): util.Map[String, JsonObject] = {
    val liveResponseJsonArr = SportsDataFacade.MODULE$.getAllLiveGamesInDateRange(startDate, endDate, sizeToReturn).getAsJsonObject.get("hits").getAsJsonObject.get("hits").getAsJsonArray
    val liveJsonObjects = new util.HashMap[String, JsonObject]
    liveResponseJsonArr.forEach((it: JsonElement) => {
      def foo(it: JsonElement) = {
        val liveJsonObject = it.getAsJsonObject.get("_source").getAsJsonObject
        liveJsonObjects.put(liveJsonObject.get("gameId").getAsString, liveJsonObject)
      }

      foo(it)
    })
    liveJsonObjects
  }

  /**
    * Prepares content ids list for the game
    *
    * @param alldocs
    * the game docs for all content
    * @return the content ids list for the game
    */
  def createContentIds(alldocs: JsonArray): JsonArray = {
    val contentIds = new JsonArray
    val scheduleGuids = getUniqueScheduleGuids(alldocs)
    if (scheduleGuids.isEmpty) contentIds.add(getDummyContentId)
    else scheduleGuids.forEach((it: String) => {
      def foo(it: String) = {
        contentIds.add(new JsonPrimitive(it))
      }

      foo(it)
    })
    contentIds
  }

  def getMatchedGame(sportDataItem: JsonObject, gameScheduleArray: JsonArray): JsonObject = {
    val solrDocs = new util.ArrayList[JsonObject]
    solrDocs.add(0, gameScheduleArray.get(0).getAsJsonObject.get("_source").getAsJsonObject)
    sportDataItem.add("contentInfo", new JsonPrimitive(GameAvailability.UNAVAILABLE.toString))
    gameScheduleArray.forEach((it: JsonElement) => {
      def foo(it: JsonElement) = {
        val item = it.getAsJsonObject.get("_source").getAsJsonObject
        if (item.has("subpackage_guids")) {
          solrDocs.add(0, item)
          //sportDataItem.add("contentInfo", new JsonPrimitive(GameAvailability.AVAILABLE.toString()));
        }
      }

      foo(it)
    })
    solrDocs.get(0)
  }

  /**
    * Finds the user subscribed channels and filters the games
    *
    * @param subpackIds
    * the subscription packs
    * @param sportDataItem
    * the the specific game data
    * @param gameScheduleArray
    * the games schedule data
    * @return the games for subscribed channels
    */
  def getSubscribedOrFirstGameSchedule(subpackIds: util.Set[String], sportDataItem: JsonObject, gameScheduleArray: JsonArray): JsonObject = {
    val foundItems = new util.ArrayList[JsonObject]
    gameScheduleArray.forEach((it: JsonElement) => {
      def foo(it: JsonElement) = {
        val intersectedSubPacks = new util.HashSet[String]
        val item = it.getAsJsonObject.get("_source").getAsJsonObject
        if (item.has("subpackage_guids")) {
          item.get("subpackage_guids").getAsJsonArray.forEach((it2: JsonElement) => {
            def foo(it2: JsonElement) =
              intersectedSubPacks.add(it2.getAsString)

            foo(it2)
          })
          intersectedSubPacks.retainAll(subpackIds)
          if (intersectedSubPacks.size != 0) foundItems.add(item)
        }
      }

      foo(it)
    })
    var solrDoc = null
    if (foundItems.isEmpty) { // get the first of unmatched content not available in slingtv
      solrDoc = gameScheduleArray.get(0).getAsJsonObject.get("_source").getAsJsonObject
      sportDataItem.add("contentInfo", new JsonPrimitive(GameAvailability.UNAVAILABLE.toString))
    }
    else { // found at least one/more intersection with slingtv
      // Do a priority ordering and findout for poc.
      val priorityOrdered = new util.ArrayList[JsonObject]
      foundItems.forEach((it: JsonObject) => {
        def foo(it: JsonObject) = {
          var callsign = ""
          if (it.has("callsign")) callsign = it.get("callsign").getAsString
          if (callsign.startsWith("CSNBA") || callsign.startsWith("CSNCH") || callsign.startsWith("CSNCA") || callsign.startsWith("CSNDC") || callsign.startsWith("CSNMA") || callsign.startsWith("ESPN")) priorityOrdered.add(it)
        }

        foo(it)
      })
      if (priorityOrdered.size != 0) solrDoc = priorityOrdered.get(0)
      else solrDoc = foundItems.get(0)
      sportDataItem.add("contentInfo", new JsonPrimitive(GameAvailability.AVAILABLE.toString))
    }
    solrDoc
  }

  /**
    * Merges live score info with actual game data
    *
    * @param liveResponseJson
    * the live score info
    * @param gameData
    * the game data
    * @param gameId
    * the game id
    */
  def updateScoreStatusFromLive(liveResponseJson: util.Map[String, JsonObject], gameData: JsonObject, gameId: String): Unit = if (liveResponseJson.get(gameId) != null) {
    var awayTeamScore = "0"
    var homeTeamScore = "0"
    if (liveResponseJson.get(gameId).has("awayScoreRuns")) {
      awayTeamScore = liveResponseJson.get(gameId).get("awayScoreRuns").getAsString
      homeTeamScore = liveResponseJson.get(gameId).get("homeScoreRuns").getAsString
    }
    if (liveResponseJson.get(gameId).has("awayScore")) {
      awayTeamScore = liveResponseJson.get(gameId).get("awayScore").getAsString
      homeTeamScore = liveResponseJson.get(gameId).get("homeScore").getAsString
    }
    gameData.add("homeScore", new JsonPrimitive(homeTeamScore))
    gameData.add("awayScore", new JsonPrimitive(awayTeamScore))
    gameData.add("gameStatus", new JsonPrimitive(GameStatus.getValue(liveResponseJson.get(gameId).get("statusId").getAsInt).toString))
  }
  else {
    gameData.add("gameStatus", new JsonPrimitive(GameStatus.UPCOMING.toString))
    val currentTime = System.currentTimeMillis
    if (gameData.has("startTimeEpoch")) {
      val gameDateAndTime = gameData.get("startTimeEpoch").getAsLong
      if ((currentTime - (gameDateAndTime * 1000)) > 86400000) gameData.add("gameStatus", new JsonPrimitive(GameStatus.COMPLETED.toString))
    }
  }

  /**
    * Prepares model for Sports item
    *
    * @param solrDoc
    * the original sports doc from indexing layer
    * @param sportDataItem
    * the sports data with live score info
    * @param addPitcherDetails
    * the pitcher details
    * @param homePitcherWins
    * the home pitcher wins
    * @param homePitcherLosses
    * the home pitcher losses
    * @param awayPitcherWins
    * the away pitcher wins
    * @param awayPitcherLosses
    * the away pitcher losses
    * @param gameResponse
    * the game response model
    */
  protected def getSportData(solrDoc: JsonElement, sportDataItem: JsonObject, addPitcherDetails: Boolean, homePitcherWins: Int, homePitcherLosses: Int, awayPitcherWins: Int, awayPitcherLosses: Int, gameResponse: JsonArray): Unit = {
    val gameScheduleJsonObj = solrDoc.getAsJsonObject
    sportDataItem.add("gameId", new JsonPrimitive(gameScheduleJsonObj.get("gameId").getAsString))
    sportDataItem.add("gameCode", new JsonPrimitive(gameScheduleJsonObj.get("gameCode").getAsString))
    // todo
    sportDataItem.add("homeScore", new JsonPrimitive(0))
    sportDataItem.add("awayScore", new JsonPrimitive(0))
    val homeTeam = new JsonObject
    val awayTeam = new JsonObject
    val homeTeamRecord = new JsonObject
    val awayTeamRecord = new JsonObject
    sportDataItem.add("homeTeam", homeTeam)
    homeTeam.add("name", new JsonPrimitive(gameScheduleJsonObj.get("homeTeamName").getAsString))
    homeTeam.add("city", new JsonPrimitive(gameScheduleJsonObj.get("homeTeamCity").getAsString))
    var homeTeamAlias = "-"
    if (gameScheduleJsonObj.has("homeTeamAlias")) {
      homeTeamAlias = gameScheduleJsonObj.get("homeTeamAlias").getAsString
      homeTeam.add("alias", new JsonPrimitive(homeTeamAlias))
    }
    val homeTeamExtId = gameScheduleJsonObj.get("homeTeamExternalId").getAsString
    homeTeam.add("img", new JsonPrimitive(String.format("http://gwserv-mobileprod.echodata.tv/Gamefinder/logos/LARGE/gid%s.png", homeTeamExtId)))
    homeTeam.add("id", new JsonPrimitive(homeTeamExtId))
    var homeTeamPitcherName = "-"
    if (gameScheduleJsonObj.has("homeTeamPitcherName")) homeTeamPitcherName = gameScheduleJsonObj.get("homeTeamPitcherName").getAsString
    homeTeam.add("pitcherName", new JsonPrimitive(homeTeamPitcherName))
    if (addPitcherDetails) addPitcherWinsLosses(homePitcherWins, homePitcherLosses, homeTeam)
    sportDataItem.add("awayTeam", awayTeam)
    awayTeam.add("name", new JsonPrimitive(gameScheduleJsonObj.get("awayTeamName").getAsString))
    awayTeam.add("city", new JsonPrimitive(gameScheduleJsonObj.get("awayTeamCity").getAsString))
    var awayTeamAlias = "-"
    if (gameScheduleJsonObj.has("awayTeamAlias")) {
      awayTeamAlias = gameScheduleJsonObj.get("awayTeamAlias").getAsString
      awayTeam.add("alias", new JsonPrimitive(awayTeamAlias))
    }
    val awayTeamExtId = gameScheduleJsonObj.get("awayTeamExternalId").getAsString
    awayTeam.add("img", new JsonPrimitive(String.format("http://gwserv-mobileprod.echodata.tv/Gamefinder/logos/LARGE/gid%s.png", awayTeamExtId)))
    awayTeam.add("id", new JsonPrimitive(awayTeamExtId))
    var awayTeamPitcherName = "-"
    if (gameScheduleJsonObj.has("awayTeamPitcherName")) awayTeamPitcherName = gameScheduleJsonObj.get("awayTeamPitcherName").getAsString
    awayTeam.add("pitcherName", new JsonPrimitive(awayTeamPitcherName))
    if (addPitcherDetails) addPitcherWinsLosses(awayPitcherWins, awayPitcherLosses, awayTeam)
    // populate default values
    homeTeamRecord.add("wins", new JsonPrimitive(0l))
    homeTeamRecord.add("losses", new JsonPrimitive(0l))
    homeTeamRecord.add("ties", new JsonPrimitive(0l))
    awayTeamRecord.add("wins", new JsonPrimitive(0l))
    awayTeamRecord.add("losses", new JsonPrimitive(0l))
    awayTeamRecord.add("ties", new JsonPrimitive(0l))
    homeTeam.add("teamRecord", homeTeamRecord)
    awayTeam.add("teamRecord", awayTeamRecord)
    sportDataItem.add("division", new JsonPrimitive("-"))
    addGameScheduleDates(sportDataItem, gameScheduleJsonObj)
    sportDataItem.add("sport", new JsonPrimitive(gameScheduleJsonObj.get("sport").getAsString))
    sportDataItem.add("league", new JsonPrimitive(gameScheduleJsonObj.get("league").getAsString))
    sportDataItem.add("rating", new JsonPrimitive(gameScheduleJsonObj.get("gexPredict").getAsString))
    val contentIds = createContentIds(gameResponse)
    val contentIdChannelGuidMap = createContentIdAssetInfoMap(gameResponse)
    sportDataItem.add("cIdToAsstInfo", contentIdChannelGuidMap)
    sportDataItem.add("channelGuid", new JsonPrimitive(gameScheduleJsonObj.get("channel_guid").getAsString))
    sportDataItem.add("programGuid", new JsonPrimitive(gameScheduleJsonObj.get("program_guid").getAsString))
    sportDataItem.add("assetGuid", new JsonPrimitive(gameScheduleJsonObj.get("asset_guid").getAsString))
    sportDataItem.add("contentId", contentIds)
    if (gameScheduleJsonObj.has("callsign")) sportDataItem.add("callsign", new JsonPrimitive(gameScheduleJsonObj.get("callsign").getAsString))
    if (gameScheduleJsonObj.has("stadiumName")) sportDataItem.add("location", new JsonPrimitive(gameScheduleJsonObj.get("stadiumName").getAsString))
    addSubPackIds(sportDataItem, gameScheduleJsonObj)
    updateGameStatusAndType(sportDataItem, gameScheduleJsonObj)
  }

  /**
    * Updates game schedule dates with offset
    *
    * @param sportDataItem
    * the sports item
    * @param gameScheduleJsonObj
    * the game schedule data
    */
  def addGameScheduleDates(sportDataItem: JsonObject, gameScheduleJsonObj: JsonObject): Unit = { // Change this once done
    val gameDateEpoch = gameScheduleJsonObj.get("game_date_epoch").getAsLong
    var startTimeEpoch = 0
    var stopTimeEpoch = 0
    if (gameScheduleJsonObj.has("startTimeEpoch")) {
      startTimeEpoch = gameScheduleJsonObj.get("startTimeEpoch").getAsLong
      if (startTimeEpoch == 0) startTimeEpoch = gameDateEpoch
    }
    if (gameScheduleJsonObj.has("stopTimeEpoch")) {
      stopTimeEpoch = gameScheduleJsonObj.get("stopTimeEpoch").getAsLong
      if (stopTimeEpoch == 0) stopTimeEpoch = startTimeEpoch + SportsCloudRestConfig.getGameStopTimeOffset.round
    }
    sportDataItem.add("stopTimeEpoch", new JsonPrimitive(stopTimeEpoch))
    sportDataItem.add("startTimeEpoch", new JsonPrimitive(startTimeEpoch))
    if (startTimeEpoch != 0 && startTimeEpoch > gameDateEpoch) addTapeDelay(sportDataItem, true)
    else addTapeDelay(sportDataItem, false)
    addScheduledDate(sportDataItem, gameDateEpoch)
    addGameDateEpoch(sportDataItem, gameDateEpoch)
  }

  /**
    * Returns unique schedule guids
    *
    * @param alldocs
    * the game docs
    * @return the unique schedule guids
    */
  def getUniqueScheduleGuids(alldocs: JsonArray): util.Set[String] = {
    val scheduleGuids = new util.HashSet[String]
    alldocs.forEach((it: JsonElement) => {
      def foo(it: JsonElement) = {
        val item = it.getAsJsonObject.get("_source").getAsJsonObject
        if (item.has("schedule_guid") && !("0" == item.get("schedule_guid").getAsString)) scheduleGuids.add(item.get("schedule_guid").getAsString)
      }

      foo(it)
    })
    scheduleGuids
  }

  /**
    * Returns the dummy content id if id is not present
    *
    * @return the dummy content id
    */
  def getDummyContentId: String = new StringBuilder("noid_").append(UUID.randomUUID.toString).toString

  def addPitcherWinsLosses(homePitcherWins: Int, homePitcherLosses: Int, team: JsonObject): Unit = {
    team.add("pitcherWins", new JsonPrimitive(homePitcherWins))
    team.add("pitcherLosses", new JsonPrimitive(homePitcherLosses))
  }

  /**
    * Adds subscription packs to sport data item
    *
    * @param sportDataItem
    * the sport data item
    * @param gameScheduleJsonObj
    * the game schedule object
    */
  def addSubPackIds(sportDataItem: JsonObject, gameScheduleJsonObj: JsonObject): Unit = {
    var subPackIds = new JsonArray
    if (gameScheduleJsonObj.has("subpackage_guids")) subPackIds = gameScheduleJsonObj.get("subpackage_guids").getAsJsonArray
    sportDataItem.add("subPackageGuids", subPackIds)
  }

  /**
    * Merges live info to Sports Media card
    *
    * @param activeGame
    * the active game
    * @param mc
    * the media card
    * @param solrDoc
    * the original index document
    * @param mcSportData
    * media card sport data
    */
  def mergeLiveInfoToMediaCard(activeGame: ActiveTeamGame, mc: JsonObject, solrDoc: JsonObject, mcSportData: JsonObject): Unit = { //
    // Fill in the live scores and other details
    //
    val gameId = solrDoc.get("gameId").getAsString
    val liveGameInfoRespJsonArr = getLiveGamesById(gameId)
    if (liveGameInfoRespJsonArr.size > 0) { // pick the first item
      val liveGameJsonObj = liveGameInfoRespJsonArr.get(0).getAsJsonObject.get("_source").getAsJsonObject
      var awayTeamScore = 0
      var homeTeamScore = 0
      if (liveGameJsonObj.has("awayScoreRuns")) {
        awayTeamScore = liveGameJsonObj.get("awayScoreRuns").getAsInt
        homeTeamScore = liveGameJsonObj.get("homeScoreRuns").getAsInt
      }
      if (liveGameJsonObj.has("awayScore")) {
        awayTeamScore = liveGameJsonObj.get("awayScore").getAsInt
        homeTeamScore = liveGameJsonObj.get("homeScore").getAsInt
      }
      // update home&away scores to media card
      mcSportData.add("homeScore", new JsonPrimitive(homeTeamScore))
      mcSportData.add("awayScore", new JsonPrimitive(awayTeamScore))
      addFieldsCount(mcSportData, liveGameJsonObj)
      // update score data into media card
      if (solrDoc.get("league").getAsString.toLowerCase == "mlb") {
        addScoreData(activeGame, mc, solrDoc, liveGameJsonObj)
        addCurrentPlayerDetails(mcSportData, liveGameJsonObj)
      }
      else addScoreDataNonMlb(activeGame, mc, solrDoc, liveGameJsonObj)
      updateGameStatusAndType(mcSportData, liveGameJsonObj)
    }
  }

  /**
    * Updates game status and type
    *
    * @param sportDataItem
    * the sport data item
    * @param gameScheduleJsonObj
    * the game schedule JSON object
    */
  def updateGameStatusAndType(sportDataItem: JsonObject, gameScheduleJsonObj: JsonObject): Unit = { // information is contianed in both live and scheduled game and hence
    // separated out.
    sportDataItem.add("gameStatus", new JsonPrimitive(GameStatus.getValue(gameScheduleJsonObj.get("statusId").getAsInt).toString))
    var gameType = GameType.UNKNOWN
    if (gameScheduleJsonObj.has("gameType")) gameType = GameType.getValue(gameScheduleJsonObj.get("gameType").getAsString)
    sportDataItem.add("gameType", new JsonPrimitive(gameType.toString))
  }

  /**
    * Updates tape delay to Sport Data item
    *
    * @param sportDataItem
    * the sport data item
    * @param tapeDelay
    * the tape delay
    */
  def addTapeDelay(sportDataItem: JsonObject, tapeDelay: Boolean): Unit = sportDataItem.add("tape_delayed_game", new JsonPrimitive(tapeDelay))

  /**
    * Updates Schedule date timestamp to Sport Data item
    *
    * @param sportDataItem
    * the sport data item
    * @param gameDateEpoch
    * the game date epoch time
    */
  def addScheduledDate(sportDataItem: JsonObject, gameDateEpoch: Long): Unit = {
    val epochTime = Instant.ofEpochSecond(gameDateEpoch)
    val utc = epochTime.atZone(ZoneId.of("Z"))
    val pattern = "EEE, dd MMM yyyy HH:mm:ss Z"
    val scheduledDate = utc.format(java.time.format.DateTimeFormatter.ofPattern(pattern))
    sportDataItem.add("scheduledDate", new JsonPrimitive(scheduledDate))
  }

  /**
    * Updates game date timestamp to Sport Data item
    *
    * @param sportDataItem
    * the sport data item
    * @param gameDateEpoch
    * the game date epoch time
    */
  def addGameDateEpoch(sportDataItem: JsonObject, gameDateEpoch: Long): Unit = sportDataItem.add("gameDateEpoch", new JsonPrimitive(gameDateEpoch))

  /**
    * Fetches the live game info from indexed data
    *
    * @param gameId
    * the game id
    * @return the live game info
    */
  def getLiveGamesById(gameId: String): JsonArray = {
    val liveGameInfoRespJson = SportsDataFacade.MODULE$.getLiveGameById(gameId).getAsJsonObject.get("hits").getAsJsonObject.get("hits").getAsJsonArray
    liveGameInfoRespJson
  }

  /**
    * Updates filed counts to media card
    *
    * @param mcSportData
    * the media card sport data
    * @param liveGameJsonObj
    * the live game Object
    */
  def addFieldsCount(mcSportData: JsonObject, liveGameJsonObj: JsonObject): Unit = {
    var fieldCountsTxt = ""
    if (liveGameJsonObj.has("fieldCountsTxt")) fieldCountsTxt = liveGameJsonObj.get("fieldCountsTxt").getAsString
    mcSportData.add("fieldCountsTxt", new JsonPrimitive(fieldCountsTxt))
    var fieldState = 0
    if (liveGameJsonObj.has("fieldState")) fieldState = AbstractSportsCloudRestDelegate.FIELD_STATE_MAP(liveGameJsonObj.get("fieldState").getAsInt & 7)
    mcSportData.add("fieldState", new JsonPrimitive(fieldState))
  }

  /**
    * Updates player details to the media card
    *
    * @param mcSportData
    * the media card sports data
    * @param liveGameJsonObj
    * the live game object
    */
  def addCurrentPlayerDetails(mcSportData: JsonObject, liveGameJsonObj: JsonObject): Unit = {
    val status = GameStatus.getValue(liveGameJsonObj.get("statusId").getAsInt)
    if (liveGameJsonObj.has("isHomePitching") && (status ne GameStatus.COMPLETED)) {
      val homeTeam = mcSportData.get("homeTeam").getAsJsonObject
      val awayTeam = mcSportData.get("awayTeam").getAsJsonObject
      val isHomePitching = liveGameJsonObj.get("isHomePitching").getAsBoolean
      var homeCurrPlayer = "-"
      if (liveGameJsonObj.has("hTCurrPlayer")) homeCurrPlayer = liveGameJsonObj.get("hTCurrPlayer").getAsString
      var awayCurrPlayer = "-"
      if (liveGameJsonObj.has("hTCurrPlayer")) awayCurrPlayer = liveGameJsonObj.get("aTCurrPlayer").getAsString
      var homePlayerRole = "-"
      var awayPlayerRole = "-"
      if (isHomePitching) {
        homePlayerRole = "pitching"
        awayPlayerRole = "atbat"
      }
      else {
        homePlayerRole = "atbat"
        awayPlayerRole = "pitching"
      }
      homeTeam.add("player_name", new JsonPrimitive(homeCurrPlayer))
      homeTeam.add("player_role", new JsonPrimitive(homePlayerRole))
      awayTeam.add("player_name", new JsonPrimitive(awayCurrPlayer))
      awayTeam.add("player_role", new JsonPrimitive(awayPlayerRole))
    }
  }

  /**
    * Updates score data to media card for MLB league
    *
    * @param activeGame
    * the active game object
    * @param mc
    * the media card object
    * @param solrDoc
    * the index doc
    * @param liveGameJsonObj
    * the live game object
    */
  def addScoreData(activeGame: ActiveTeamGame, mc: JsonObject, solrDoc: JsonObject, liveGameJsonObj: JsonObject): Unit = {
    val scoreData = new JsonObject
    mc.add("score_data", scoreData)
    val scHomeTeam = new JsonObject
    val scAwayTeam = new JsonObject
    scoreData.add("homeTeam", scHomeTeam)
    scoreData.add("awayTeam", scAwayTeam)
    scHomeTeam.add("errors", new JsonPrimitive(liveGameJsonObj.get("homeScoreErrors").getAsInt))
    scHomeTeam.add("runs", new JsonPrimitive(liveGameJsonObj.get("homeScoreRuns").getAsInt))
    scHomeTeam.add("hits", new JsonPrimitive(liveGameJsonObj.get("homeScoreHits").getAsInt))
    scAwayTeam.add("errors", new JsonPrimitive(liveGameJsonObj.get("awayScoreErrors").getAsInt))
    scAwayTeam.add("runs", new JsonPrimitive(liveGameJsonObj.get("awayScoreRuns").getAsInt))
    scAwayTeam.add("hits", new JsonPrimitive(liveGameJsonObj.get("awayScoreHits").getAsInt))
    var htScoreDetails = new JsonArray
    if (liveGameJsonObj.has("homeTeamInnings")) htScoreDetails = liveGameJsonObj.get("homeTeamInnings").getAsJsonArray
    var atScoreDetails = new JsonArray
    if (liveGameJsonObj.has("awayTeamInnings")) atScoreDetails = liveGameJsonObj.get("awayTeamInnings").getAsJsonArray
    scHomeTeam.add("scoreDetails", htScoreDetails)
    scAwayTeam.add("scoreDetails", atScoreDetails)
    scoreData.add("scoreboard_title", new JsonPrimitive(getScoreBoardTitle(activeGame)))
    scoreData.add("sport", new JsonPrimitive(solrDoc.getAsJsonObject.get("sport").getAsString))
  }

  /**
    * Updates score data to media card for non MLB leagues
    *
    * @param activeGame
    * the active game object
    * @param mc
    * the media card object
    * @param solrDoc
    * the index doc
    * @param liveGameJsonObj
    * the live game object
    */
  def addScoreDataNonMlb(activeGame: ActiveTeamGame, mc: JsonObject, solrDoc: JsonObject, liveGameJsonObj: JsonObject): Unit = {
    val scoreData = new JsonObject
    mc.add("score_data", scoreData)
    val scHomeTeam = new JsonObject
    val scAwayTeam = new JsonObject
    scoreData.add("homeTeam", scHomeTeam)
    scoreData.add("awayTeam", scAwayTeam)
    var htScoreDetails = new JsonArray
    if (liveGameJsonObj.has("homeTeamlineScore")) htScoreDetails = liveGameJsonObj.get("homeTeamlineScore").getAsJsonArray
    var atScoreDetails = new JsonArray
    if (liveGameJsonObj.has("awayTeamlineScore")) atScoreDetails = liveGameJsonObj.get("awayTeamlineScore").getAsJsonArray
    scHomeTeam.add("scoreDetails", htScoreDetails)
    scAwayTeam.add("scoreDetails", atScoreDetails)
    // scoreData.add("scoreboard_title",
    // new JsonPrimitive(getScoreBoardTitle(activeGame)));
    scoreData.add("sport", new JsonPrimitive(solrDoc.getAsJsonObject.get("sport").getAsString))
  }

  /**
    * Returns Score board title.
    *
    * @param activeGame
    * the active game object
    * @return the score board title
    */
  def getScoreBoardTitle(activeGame: ActiveTeamGame): String = { // Fill the score card title
    var scoreCardTitle = ""
    val activeAwayTeamId = activeGame.getAwayTeamId
    val allTeamGamesJson = SportsDataFacade.MODULE$.getLiveInfoForActiveTeam(activeGame).getAsJsonObject.get("hits").getAsJsonObject.get("hits").getAsJsonArray
    var team1Wins = 0
    // awayTeam
    var team2Wins = 0
    // homeTeam
    val team1ExtId = activeAwayTeamId
    import scala.collection.JavaConversions._
    for (it <- allTeamGamesJson) {
      val teamGamesJson = it.getAsJsonObject.get("_source").getAsJsonObject
      val homeWins = teamGamesJson.get("homeScoreRuns").getAsInt
      val awayWins = teamGamesJson.get("awayScoreRuns").getAsInt
      val awayTeamId = teamGamesJson.get("awayTeamExtId").getAsString
      var t1Score = 0
      var t2Score = 0
      if (awayTeamId == team1ExtId) {
        t1Score = awayWins
        t2Score = homeWins
      }
      else {
        t1Score = homeWins
        t2Score = awayWins
      }
      if (t1Score > t2Score) team1Wins += 1
      else team2Wins += 1
    }
    var seriesLeaderName = "-"
    var seriesTitle = "-"
    if (team1Wins > team2Wins) {
      seriesLeaderName = activeGame.getAwayTeamName
      seriesTitle = "lead series " + team1Wins + "-" + team2Wins
    }
    else if (team1Wins < team2Wins) {
      seriesLeaderName = activeGame.getHomeTeamName
      seriesTitle = "lead series " + team2Wins + "-" + team1Wins
    }
    else seriesTitle = "Series tied " + team1Wins + "-" + team2Wins
    scoreCardTitle = String.format("%s %s", seriesLeaderName, seriesTitle)
    scoreCardTitle
  }
}