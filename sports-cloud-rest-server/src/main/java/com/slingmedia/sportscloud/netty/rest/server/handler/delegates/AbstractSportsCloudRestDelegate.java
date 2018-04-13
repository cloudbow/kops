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

 ***********************************************************************/
package com.slingmedia.sportscloud.netty.rest.server.handler.delegates;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.slingmedia.sportscloud.facade.*;
import com.slingmedia.sportscloud.netty.rest.model.ActiveTeamGame;
import com.slingmedia.sportscloud.netty.rest.model.GameAvailability;
import com.slingmedia.sportscloud.netty.rest.model.GameStatus;
import com.slingmedia.sportscloud.netty.rest.model.GameType;
import com.slingmedia.sportscloud.netty.rest.model.Role;
import com.slingmedia.sportscloud.netty.rest.server.config.SportsCloudRestConfig;

/**
 * Abstract Response modeling for web-view Sports Home screen and Media card
 * 
 * @author arung
 * @version 1.0
 * @since 1.0
 */
public class AbstractSportsCloudRestDelegate {
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSportsCloudRestDelegate.class);
	static final int[] FIELD_STATE_MAP = new int[] { 0, 1, 2, 4, 3, 7, 5, 6 };

	/**
	 * Returns the game schedule data for given game id.
	 * 
	 * @param gameId
	 *            the game id
	 * @return the game schedule in JsonFormat
	 */
	public JsonArray getGameForGameId(String gameId) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(String.format("Getting date for %s", gameId));
		}
		JsonElement currentGameRespJson = SportsDataFacade$.MODULE$.getGameScheduleByGameCode(gameId);
		JsonArray currGameDocs = currentGameRespJson.getAsJsonObject().get("hits").getAsJsonObject().get("hits")
				.getAsJsonArray();
		return currGameDocs;
	}

	/**
	 * Returns the channel info for each content
	 * 
	 * @param gameResponseArr
	 *            the game data index response
	 * @return the content channel info map
	 */
	public JsonObject createContentIdAssetInfoMap(JsonArray gameResponseArr) {
		JsonObject contentIdChGuid = new JsonObject();
		gameResponseArr.forEach(it -> {
			JsonObject gameJsonObj = it.getAsJsonObject().get("_source").getAsJsonObject();
			if (gameJsonObj.has("channel_guid") && gameJsonObj.has("schedule_guid")) {
				JsonObject assetInfoJson = new JsonObject();
				contentIdChGuid.add(gameJsonObj.get("schedule_guid").getAsString(), assetInfoJson);
				assetInfoJson.add("channelGuid", new JsonPrimitive(gameJsonObj.get("channel_guid").getAsString()));
				assetInfoJson.add("assetGuid", new JsonPrimitive(gameJsonObj.get("asset_guid").getAsString()));
				assetInfoJson.add("callsign", new JsonPrimitive(gameJsonObj.get("callsign").getAsString()));
			}
		});
		return contentIdChGuid;
	}

	/**
	 * This method returns an active team + the game selected for the media card
	 * 
	 * @param teamId
	 *            the team id
	 * @param currGameDocs
	 *            the current all games
	 * @return the active team game details
	 */
	public ActiveTeamGame getActiveTeamGame(String teamId, JsonArray currGameDocs) {
		Role role = Role.NONE;
		String homeTeamId = "0";
		String awayTeamId = "0";
		String awayTeamName = "-";
		String homeTeamName = "-";
		String gameId = "0";
		GameType gameType = GameType.REGULAR_SEASON;
		for (JsonElement doc : currGameDocs) {
			JsonObject currGamesDoc = doc.getAsJsonObject().get("_source").getAsJsonObject();
			homeTeamId = currGamesDoc.get("homeTeamExternalId").getAsString();
			awayTeamId = currGamesDoc.get("awayTeamExternalId").getAsString();
			awayTeamName = currGamesDoc.get("awayTeamName").getAsString();
			homeTeamName = currGamesDoc.get("homeTeamName").getAsString();
			gameId = currGamesDoc.get("gameId").getAsString();
			gameType = GameType.getValue(currGamesDoc.get("gameType").getAsString());
		}
		if (teamId.equals(homeTeamId)) {
			role = Role.HOME;
		} else if (teamId.equals(awayTeamId)) {
			role = Role.AWAY;
		}
		return new ActiveTeamGame(gameId, gameType, teamId, homeTeamId, awayTeamId, homeTeamName, awayTeamName, role);
	}

	/**
	 * Prepares the live game model
	 * 
	 * @param startDate
	 *            the start date
	 * @param endDate
	 *            the end date
	 * @param sizeToReturn
	 *            the limit for fetching upcoming games
	 * @return the live game model
	 */
	public Map<String, JsonObject> prepareLiveGameInfoData(long startDate, long endDate, int sizeToReturn) {
		JsonArray liveResponseJsonArr = SportsDataFacade$.MODULE$
				.getAllLiveGamesInDateRange(startDate, endDate, sizeToReturn).getAsJsonObject().get("hits")
				.getAsJsonObject().get("hits").getAsJsonArray();
		Map<String, JsonObject> liveJsonObjects = new HashMap<>();
		liveResponseJsonArr.forEach(it -> {
			JsonObject liveJsonObject = it.getAsJsonObject().get("_source").getAsJsonObject();
			liveJsonObjects.put(liveJsonObject.get("gameId").getAsString(), liveJsonObject);

		});
		return liveJsonObjects;
	}

	/**
	 * Prepares content ids list for the game
	 * 
	 * @param alldocs
	 *            the game docs for all content
	 * @return the content ids list for the game
	 */
	public JsonArray createContentIds(JsonArray alldocs) {
		JsonArray contentIds = new JsonArray();
		Set<String> scheduleGuids = getUniqueScheduleGuids(alldocs);

		if (scheduleGuids.isEmpty()) {
			contentIds.add(getDummyContentId());
		} else {
			scheduleGuids.forEach(it -> {
				contentIds.add(new JsonPrimitive(it));
			});
		}

		return contentIds;
	}

	public JsonObject getMatchedGame( JsonObject sportDataItem,
													   JsonArray gameScheduleArray) {

		List<JsonObject> solrDocs = new ArrayList<>();
		solrDocs.add(0,gameScheduleArray.get(0).getAsJsonObject().get("_source").getAsJsonObject());
		sportDataItem.add("contentInfo", new JsonPrimitive(GameAvailability.UNAVAILABLE.toString()));
		gameScheduleArray.forEach(it -> {
			JsonObject item = it.getAsJsonObject().get("_source").getAsJsonObject();
			if (item.has("subpackage_guids")) {
				solrDocs.add(0,item);
				//sportDataItem.add("contentInfo", new JsonPrimitive(GameAvailability.AVAILABLE.toString()));
			}
		});
		return solrDocs.get(0);
	}
	/**
	 * Finds the user subscribed channels and filters the games
	 * 
	 * @param subpackIds
	 *            the subscription packs
	 * @param sportDataItem
	 *            the the specific game data
	 * @param gameScheduleArray
	 *            the games schedule data
	 * @return the games for subscribed channels
	 */
	public JsonObject getSubscribedOrFirstGameSchedule(Set<String> subpackIds, JsonObject sportDataItem,
			JsonArray gameScheduleArray) {

		List<JsonObject> foundItems = new ArrayList<>();
		gameScheduleArray.forEach(it -> {
			Set<String> intersectedSubPacks = new HashSet<String>();
			JsonObject item = it.getAsJsonObject().get("_source").getAsJsonObject();
			if (item.has("subpackage_guids")) {
				item.get("subpackage_guids").getAsJsonArray().forEach(it2 -> {
					intersectedSubPacks.add(it2.getAsString());
				});
				intersectedSubPacks.retainAll(subpackIds);
				if (intersectedSubPacks.size() != 0) {
					foundItems.add(item);
				}
			}
		});

		JsonObject solrDoc = null;
		if (foundItems.isEmpty()) {
			// get the first of unmatched content not available in slingtv
			solrDoc = gameScheduleArray.get(0).getAsJsonObject().get("_source").getAsJsonObject();
			sportDataItem.add("contentInfo", new JsonPrimitive(GameAvailability.UNAVAILABLE.toString()));
		} else {
			// found at least one/more intersection with slingtv
			// Do a priority ordering and findout for poc.
			List<JsonObject> priorityOrdered = new ArrayList<>();
			foundItems.forEach(it -> {
				String callsign = "";
				if (it.has("callsign")) {
					callsign = it.get("callsign").getAsString();
				}
				if (callsign.startsWith("CSNBA") || callsign.startsWith("CSNCH") || callsign.startsWith("CSNCA")
						|| callsign.startsWith("CSNDC") || callsign.startsWith("CSNMA")
						|| callsign.startsWith("ESPN")) {
					priorityOrdered.add(it);
				}
			});
			if (priorityOrdered.size() != 0) {
				solrDoc = priorityOrdered.get(0);
			} else {
				solrDoc = foundItems.get(0);
			}
			sportDataItem.add("contentInfo", new JsonPrimitive(GameAvailability.AVAILABLE.toString()));
		}

		return solrDoc;
	}

	/**
	 * Merges live score info with actual game data
	 * 
	 * @param liveResponseJson
	 *            the live score info
	 * @param gameData
	 *            the game data
	 * @param gameId
	 *            the game id
	 */
	public void updateScoreStatusFromLive(Map<String, JsonObject> liveResponseJson, JsonObject gameData,
			String gameId) {
		if (liveResponseJson.get(gameId) != null) {
			String awayTeamScore = "0";
			String homeTeamScore = "0";
			if (liveResponseJson.get(gameId).has("awayScoreRuns")) {
				awayTeamScore = liveResponseJson.get(gameId).get("awayScoreRuns").getAsString();
				homeTeamScore = liveResponseJson.get(gameId).get("homeScoreRuns").getAsString();
			}
			if (liveResponseJson.get(gameId).has("awayScore")) {
				awayTeamScore = liveResponseJson.get(gameId).get("awayScore").getAsString();
				homeTeamScore = liveResponseJson.get(gameId).get("homeScore").getAsString();
			}
			gameData.add("homeScore", new JsonPrimitive(homeTeamScore));
			gameData.add("awayScore", new JsonPrimitive(awayTeamScore));
			gameData.add("gameStatus", new JsonPrimitive(
					GameStatus.getValue(liveResponseJson.get(gameId).get("statusId").getAsInt()).toString()));
		} else {
            gameData.add("gameStatus", new JsonPrimitive(GameStatus.UPCOMING.toString()));
		    long currentTime = System.currentTimeMillis();
            if(gameData.has("startTimeEpoch") ) {
                long gameDateAndTime = gameData.get("startTimeEpoch").getAsLong();
                if((currentTime - (gameDateAndTime*1000)) > 86400000) {
                    gameData.add("gameStatus", new JsonPrimitive(GameStatus.COMPLETED.toString()));
                }
            }


		}
	}

	/**
	 * Prepares model for Sports item
	 * 
	 * @param solrDoc
	 *            the original sports doc from indexing layer
	 * @param sportDataItem
	 *            the sports data with live score info
	 * @param addPitcherDetails
	 *            the pitcher details
	 * @param homePitcherWins
	 *            the home pitcher wins
	 * @param homePitcherLosses
	 *            the home pitcher losses
	 * @param awayPitcherWins
	 *            the away pitcher wins
	 * @param awayPitcherLosses
	 *            the away pitcher losses
	 * @param gameResponse
	 *            the game response model
	 */
	protected void getSportData(JsonElement solrDoc, JsonObject sportDataItem, boolean addPitcherDetails,
			int homePitcherWins, int homePitcherLosses, int awayPitcherWins, int awayPitcherLosses,
			JsonArray gameResponse) {
		JsonObject gameScheduleJsonObj = solrDoc.getAsJsonObject();
		sportDataItem.add("gameId", new JsonPrimitive(gameScheduleJsonObj.get("gameId").getAsString()));
		sportDataItem.add("gameCode", new JsonPrimitive(gameScheduleJsonObj.get("gameCode").getAsString()));

		// todo
		sportDataItem.add("homeScore", new JsonPrimitive(0));
		// todo
		sportDataItem.add("awayScore", new JsonPrimitive(0));
		JsonObject homeTeam = new JsonObject();
		JsonObject awayTeam = new JsonObject();
		JsonObject homeTeamRecord = new JsonObject();
		JsonObject awayTeamRecord = new JsonObject();
		sportDataItem.add("homeTeam", homeTeam);
		homeTeam.add("name", new JsonPrimitive(gameScheduleJsonObj.get("homeTeamName").getAsString()));
		homeTeam.add("city", new JsonPrimitive(gameScheduleJsonObj.get("homeTeamCity").getAsString()));

		String homeTeamAlias = "-";
		if (gameScheduleJsonObj.has("homeTeamAlias")) {
			homeTeamAlias = gameScheduleJsonObj.get("homeTeamAlias").getAsString();
			homeTeam.add("alias", new JsonPrimitive(homeTeamAlias));
		}
		String homeTeamExtId = gameScheduleJsonObj.get("homeTeamExternalId").getAsString();
		homeTeam.add("img", new JsonPrimitive(
				String.format("http://gwserv-mobileprod.echodata.tv/Gamefinder/logos/LARGE/gid%s.png", homeTeamExtId)));
		homeTeam.add("id", new JsonPrimitive(homeTeamExtId));
		String homeTeamPitcherName = "-";
		if (gameScheduleJsonObj.has("homeTeamPitcherName")) {
			homeTeamPitcherName = gameScheduleJsonObj.get("homeTeamPitcherName").getAsString();
		}
		homeTeam.add("pitcherName", new JsonPrimitive(homeTeamPitcherName));

		if (addPitcherDetails) {
			addPitcherWinsLosses(homePitcherWins, homePitcherLosses, homeTeam);
		}

		sportDataItem.add("awayTeam", awayTeam);
		awayTeam.add("name", new JsonPrimitive(gameScheduleJsonObj.get("awayTeamName").getAsString()));
		awayTeam.add("city", new JsonPrimitive(gameScheduleJsonObj.get("awayTeamCity").getAsString()));
		String awayTeamAlias = "-";
		if (gameScheduleJsonObj.has("awayTeamAlias")) {
			awayTeamAlias = gameScheduleJsonObj.get("awayTeamAlias").getAsString();
			awayTeam.add("alias", new JsonPrimitive(awayTeamAlias));
		}
		String awayTeamExtId = gameScheduleJsonObj.get("awayTeamExternalId").getAsString();
		awayTeam.add("img", new JsonPrimitive(
				String.format("http://gwserv-mobileprod.echodata.tv/Gamefinder/logos/LARGE/gid%s.png", awayTeamExtId)));

		awayTeam.add("id", new JsonPrimitive(awayTeamExtId));
		String awayTeamPitcherName = "-";
		if (gameScheduleJsonObj.has("awayTeamPitcherName")) {
			awayTeamPitcherName = gameScheduleJsonObj.get("awayTeamPitcherName").getAsString();
		}
		awayTeam.add("pitcherName", new JsonPrimitive(awayTeamPitcherName));
		if (addPitcherDetails) {
			addPitcherWinsLosses(awayPitcherWins, awayPitcherLosses, awayTeam);
		}

		// populate default values
		homeTeamRecord.add("wins", new JsonPrimitive(0l));
		homeTeamRecord.add("losses", new JsonPrimitive(0l));
		homeTeamRecord.add("ties", new JsonPrimitive(0l));
		awayTeamRecord.add("wins", new JsonPrimitive(0l));
		awayTeamRecord.add("losses", new JsonPrimitive(0l));
		awayTeamRecord.add("ties", new JsonPrimitive(0l));

		homeTeam.add("teamRecord", homeTeamRecord);
		awayTeam.add("teamRecord", awayTeamRecord);

		// populate default values
		sportDataItem.add("division", new JsonPrimitive("-"));
		addGameScheduleDates(sportDataItem, gameScheduleJsonObj);
		sportDataItem.add("sport", new JsonPrimitive(gameScheduleJsonObj.get("sport").getAsString()));
		sportDataItem.add("league", new JsonPrimitive(gameScheduleJsonObj.get("league").getAsString()));
		sportDataItem.add("rating", new JsonPrimitive(gameScheduleJsonObj.get("gexPredict").getAsString()));
		JsonArray contentIds = createContentIds(gameResponse);
		JsonObject contentIdChannelGuidMap = createContentIdAssetInfoMap(gameResponse);
		sportDataItem.add("cIdToAsstInfo", contentIdChannelGuidMap);
		sportDataItem.add("channelGuid", new JsonPrimitive(gameScheduleJsonObj.get("channel_guid").getAsString()));
		sportDataItem.add("programGuid", new JsonPrimitive(gameScheduleJsonObj.get("program_guid").getAsString()));
		sportDataItem.add("assetGuid", new JsonPrimitive(gameScheduleJsonObj.get("asset_guid").getAsString()));
		sportDataItem.add("contentId", contentIds);
		if (gameScheduleJsonObj.has("callsign")) {
			sportDataItem.add("callsign", new JsonPrimitive(gameScheduleJsonObj.get("callsign").getAsString()));
		}
		if (gameScheduleJsonObj.has("stadiumName")) {
			sportDataItem.add("location", new JsonPrimitive(gameScheduleJsonObj.get("stadiumName").getAsString()));
		}

		addSubPackIds(sportDataItem, gameScheduleJsonObj);

		updateGameStatusAndType(sportDataItem, gameScheduleJsonObj);

	}

	/**
	 * Updates game schedule dates with offset
	 * 
	 * @param sportDataItem
	 *            the sports item
	 * @param gameScheduleJsonObj
	 *            the game schedule data
	 */
	public void addGameScheduleDates(JsonObject sportDataItem, JsonObject gameScheduleJsonObj) {
		// Change this once done
		long gameDateEpoch = gameScheduleJsonObj.get("game_date_epoch").getAsLong();

		long startTimeEpoch = 0;
		long stopTimeEpoch = 0;
		if (gameScheduleJsonObj.has("startTimeEpoch")) {
			startTimeEpoch = gameScheduleJsonObj.get("startTimeEpoch").getAsLong();
			if(startTimeEpoch == 0) {
				startTimeEpoch = gameDateEpoch;
			}
		}
		if (gameScheduleJsonObj.has("stopTimeEpoch")) {
			stopTimeEpoch = gameScheduleJsonObj.get("stopTimeEpoch").getAsLong();
			if (stopTimeEpoch == 0) {
				stopTimeEpoch = startTimeEpoch + Math.round(SportsCloudRestConfig.getGameStopTimeOffset());
			}
		}
		sportDataItem.add("stopTimeEpoch", new JsonPrimitive(stopTimeEpoch));
		sportDataItem.add("startTimeEpoch", new JsonPrimitive(startTimeEpoch));
		if (startTimeEpoch != 0 && startTimeEpoch > gameDateEpoch) {
			addTapeDelay(sportDataItem, true);
		} else {
			addTapeDelay(sportDataItem, false);
		}
		addScheduledDate(sportDataItem, gameDateEpoch);
		addGameDateEpoch(sportDataItem, gameDateEpoch);

	}

	/**
	 * Returns unique schedule guids
	 * 
	 * @param alldocs
	 *            the game docs
	 * @return the unique schedule guids
	 */
	public Set<String> getUniqueScheduleGuids(JsonArray alldocs) {
		Set<String> scheduleGuids = new HashSet<String>();
		alldocs.forEach(it -> {
			JsonObject item = it.getAsJsonObject().get("_source").getAsJsonObject();
			if (item.has("schedule_guid") && !"0".equals(item.get("schedule_guid").getAsString())) {
				scheduleGuids.add(item.get("schedule_guid").getAsString());
			}

		});
		return scheduleGuids;
	}

	/**
	 * Returns the dummy content id if id is not present
	 * 
	 * @return the dummy content id
	 */
	public String getDummyContentId() {
		return new StringBuilder("noid_").append(UUID.randomUUID().toString()).toString();
	}

	public void addPitcherWinsLosses(int homePitcherWins, int homePitcherLosses, JsonObject team) {

		team.add("pitcherWins", new JsonPrimitive(homePitcherWins));
		team.add("pitcherLosses", new JsonPrimitive(homePitcherLosses));

	}

	/**
	 * Adds subscription packs to sport data item
	 * 
	 * @param sportDataItem
	 *            the sport data item
	 * @param gameScheduleJsonObj
	 *            the game schedule object
	 */
	public void addSubPackIds(JsonObject sportDataItem, JsonObject gameScheduleJsonObj) {
		JsonArray subPackIds = new JsonArray();
		if (gameScheduleJsonObj.has("subpackage_guids")) {
			subPackIds = gameScheduleJsonObj.get("subpackage_guids").getAsJsonArray();
		}
		sportDataItem.add("subPackageGuids", subPackIds);
	}

	/**
	 * Merges live info to Sports Media card
	 * 
	 * @param activeGame
	 *            the active game
	 * @param mc
	 *            the media card
	 * @param solrDoc
	 *            the original index document
	 * @param mcSportData
	 *            media card sport data
	 */
	public void mergeLiveInfoToMediaCard(ActiveTeamGame activeGame, JsonObject mc, JsonObject solrDoc,
			JsonObject mcSportData) {
		//
		// Fill in the live scores and other details
		//
		String gameId = solrDoc.get("gameId").getAsString();
		JsonArray liveGameInfoRespJsonArr = getLiveGamesById(gameId);
		if (liveGameInfoRespJsonArr.size() > 0) {
			// pick the first item
			JsonObject liveGameJsonObj = liveGameInfoRespJsonArr.get(0).getAsJsonObject().get("_source")
					.getAsJsonObject();

			int awayTeamScore = 0;
			int homeTeamScore = 0;
			if (liveGameJsonObj.has("awayScoreRuns")) {
				awayTeamScore = liveGameJsonObj.get("awayScoreRuns").getAsInt();
				homeTeamScore = liveGameJsonObj.get("homeScoreRuns").getAsInt();
			}
			if (liveGameJsonObj.has("awayScore")) {
				awayTeamScore = liveGameJsonObj.get("awayScore").getAsInt();
				homeTeamScore = liveGameJsonObj.get("homeScore").getAsInt();
			}
			// update home&away scores to media card
			mcSportData.add("homeScore", new JsonPrimitive(homeTeamScore));
			mcSportData.add("awayScore", new JsonPrimitive(awayTeamScore));
			addFieldsCount(mcSportData, liveGameJsonObj);

			// update score data into media card
			if (solrDoc.get("league").getAsString().toLowerCase().equals("mlb")) {
				addScoreData(activeGame, mc, solrDoc, liveGameJsonObj);
				addCurrentPlayerDetails(mcSportData, liveGameJsonObj);
			} else {
				addScoreDataNonMlb(activeGame, mc, solrDoc, liveGameJsonObj);
			}

			updateGameStatusAndType(mcSportData, liveGameJsonObj);
		}

	}

	/**
	 * Updates game status and type
	 * 
	 * @param sportDataItem
	 *            the sport data item
	 * @param gameScheduleJsonObj
	 *            the game schedule JSON object
	 */
	public void updateGameStatusAndType(JsonObject sportDataItem, JsonObject gameScheduleJsonObj) {
		// information is contianed in both live and scheduled game and hence
		// separated out.
		sportDataItem.add("gameStatus",
				new JsonPrimitive(GameStatus.getValue(gameScheduleJsonObj.get("statusId").getAsInt()).toString()));
		GameType gameType= GameType.UNKNOWN;
		if(gameScheduleJsonObj.has("gameType")){
			gameType=GameType.getValue(gameScheduleJsonObj.get("gameType").getAsString());
		}
		sportDataItem.add("gameType",
				new JsonPrimitive(gameType.toString()));
	}

	/**
	 * Updates tape delay to Sport Data item
	 * 
	 * @param sportDataItem
	 *            the sport data item
	 * @param tapeDelay
	 *            the tape delay
	 */
	public void addTapeDelay(JsonObject sportDataItem, boolean tapeDelay) {
		sportDataItem.add("tape_delayed_game", new JsonPrimitive(tapeDelay));
	}

	/**
	 * Updates Schedule date timestamp to Sport Data item
	 * 
	 * @param sportDataItem
	 *            the sport data item
	 * @param gameDateEpoch
	 *            the game date epoch time
	 */
	public void addScheduledDate(JsonObject sportDataItem, long gameDateEpoch) {
		Instant epochTime = Instant.ofEpochSecond(gameDateEpoch);
		ZonedDateTime utc = epochTime.atZone(ZoneId.of("Z"));
		String pattern = "EEE, dd MMM yyyy HH:mm:ss Z";
		String scheduledDate = utc.format(java.time.format.DateTimeFormatter.ofPattern(pattern));
		sportDataItem.add("scheduledDate", new JsonPrimitive(scheduledDate));
	}

	/**
	 * Updates game date timestamp to Sport Data item
	 * 
	 * @param sportDataItem
	 *            the sport data item
	 * @param gameDateEpoch
	 *            the game date epoch time
	 */
	public void addGameDateEpoch(JsonObject sportDataItem, long gameDateEpoch) {
		sportDataItem.add("gameDateEpoch", new JsonPrimitive(gameDateEpoch));
	}

	/**
	 * Fetches the live game info from indexed data
	 * 
	 * @param gameId
	 *            the game id
	 * @return the live game info
	 */
	public JsonArray getLiveGamesById(String gameId) {
		JsonArray liveGameInfoRespJson = SportsDataFacade$.MODULE$.getLiveGameById(gameId).getAsJsonObject().get("hits")
				.getAsJsonObject().get("hits").getAsJsonArray();
		return liveGameInfoRespJson;
	}

	/**
	 * Updates filed counts to media card
	 * 
	 * @param mcSportData
	 *            the media card sport data
	 * @param liveGameJsonObj
	 *            the live game Object
	 */
	public void addFieldsCount(JsonObject mcSportData, JsonObject liveGameJsonObj) {
		String fieldCountsTxt = "";
		if (liveGameJsonObj.has("fieldCountsTxt")) {
			fieldCountsTxt = liveGameJsonObj.get("fieldCountsTxt").getAsString();

		}
		mcSportData.add("fieldCountsTxt", new JsonPrimitive(fieldCountsTxt));

		int fieldState = 0;
		if (liveGameJsonObj.has("fieldState")) {
			fieldState = FIELD_STATE_MAP[liveGameJsonObj.get("fieldState").getAsInt() & 7];
		}
		mcSportData.add("fieldState", new JsonPrimitive(fieldState));
	}

	/**
	 * Updates player details to the media card
	 * 
	 * @param mcSportData
	 *            the media card sports data
	 * @param liveGameJsonObj
	 *            the live game object
	 */
	public void addCurrentPlayerDetails(JsonObject mcSportData, JsonObject liveGameJsonObj) {
		GameStatus status = GameStatus.getValue(liveGameJsonObj.get("statusId").getAsInt());
		if (liveGameJsonObj.has("isHomePitching") && status != GameStatus.COMPLETED) {
			JsonElement homeTeamElement = mcSportData.get("homeTeam");
			JsonElement awayTeamElement = mcSportData.get("awayTeam");
			if (homeTeamElement != null && awayTeamElement!= null) { // Check is added for In-Progress mlb games, where team details were empty
				JsonObject homeTeam = homeTeamElement.getAsJsonObject();
				JsonObject awayTeam = awayTeamElement.getAsJsonObject();

				Boolean isHomePitching = liveGameJsonObj.get("isHomePitching").getAsBoolean();
				String homeCurrPlayer = "-";
				if (liveGameJsonObj.has("hTCurrPlayer")) {
					homeCurrPlayer = liveGameJsonObj.get("hTCurrPlayer").getAsString();
				}
				String awayCurrPlayer = "-";
				if (liveGameJsonObj.has("hTCurrPlayer")) {
					awayCurrPlayer = liveGameJsonObj.get("aTCurrPlayer").getAsString();
				}

				String homePlayerRole = "-";
				String awayPlayerRole = "-";
				if (isHomePitching) {
					homePlayerRole = "pitching";
					awayPlayerRole = "atbat";
				} else {
					homePlayerRole = "atbat";
					awayPlayerRole = "pitching";
				}
				homeTeam.add("player_name", new JsonPrimitive(homeCurrPlayer));
				homeTeam.add("player_role", new JsonPrimitive(homePlayerRole));

				awayTeam.add("player_name", new JsonPrimitive(awayCurrPlayer));
				awayTeam.add("player_role", new JsonPrimitive(awayPlayerRole));
			}
		}
	}

	/**
	 * Updates score data to media card for MLB league
	 * 
	 * @param activeGame
	 *            the active game object
	 * @param mc
	 *            the media card object
	 * @param solrDoc
	 *            the index doc
	 * @param liveGameJsonObj
	 *            the live game object
	 */
	public void addScoreData(ActiveTeamGame activeGame, JsonObject mc, JsonObject solrDoc, JsonObject liveGameJsonObj) {
		JsonObject scoreData = new JsonObject();
		mc.add("score_data", scoreData);
		JsonObject scHomeTeam = new JsonObject();
		JsonObject scAwayTeam = new JsonObject();
		scoreData.add("homeTeam", scHomeTeam);
		scoreData.add("awayTeam", scAwayTeam);

		scHomeTeam.add("errors", new JsonPrimitive(liveGameJsonObj.get("homeScoreErrors").getAsInt()));
		scHomeTeam.add("runs", new JsonPrimitive(liveGameJsonObj.get("homeScoreRuns").getAsInt()));
		scHomeTeam.add("hits", new JsonPrimitive(liveGameJsonObj.get("homeScoreHits").getAsInt()));
		scAwayTeam.add("errors", new JsonPrimitive(liveGameJsonObj.get("awayScoreErrors").getAsInt()));
		scAwayTeam.add("runs", new JsonPrimitive(liveGameJsonObj.get("awayScoreRuns").getAsInt()));
		scAwayTeam.add("hits", new JsonPrimitive(liveGameJsonObj.get("awayScoreHits").getAsInt()));
		JsonArray htScoreDetails = new JsonArray();
		if (liveGameJsonObj.has("homeTeamInnings")) {
			htScoreDetails = liveGameJsonObj.get("homeTeamInnings").getAsJsonArray();
		}

		JsonArray atScoreDetails = new JsonArray();
		if (liveGameJsonObj.has("awayTeamInnings")) {
			atScoreDetails = liveGameJsonObj.get("awayTeamInnings").getAsJsonArray();
		}
		scHomeTeam.add("scoreDetails", htScoreDetails);
		scAwayTeam.add("scoreDetails", atScoreDetails);
		scoreData.add("scoreboard_title", new JsonPrimitive(getScoreBoardTitle(activeGame)));
		scoreData.add("sport", new JsonPrimitive(solrDoc.getAsJsonObject().get("sport").getAsString()));
	}

	/**
	 * Updates score data to media card for non MLB leagues
	 * 
	 * @param activeGame
	 *            the active game object
	 * @param mc
	 *            the media card object
	 * @param solrDoc
	 *            the index doc
	 * @param liveGameJsonObj
	 *            the live game object
	 */
	public void addScoreDataNonMlb(ActiveTeamGame activeGame, JsonObject mc, JsonObject solrDoc,
			JsonObject liveGameJsonObj) {
		JsonObject scoreData = new JsonObject();
		mc.add("score_data", scoreData);
		JsonObject scHomeTeam = new JsonObject();
		JsonObject scAwayTeam = new JsonObject();
		scoreData.add("homeTeam", scHomeTeam);
		scoreData.add("awayTeam", scAwayTeam);

		JsonArray htScoreDetails = new JsonArray();
		if (liveGameJsonObj.has("homeTeamlineScore")) {
			htScoreDetails = liveGameJsonObj.get("homeTeamlineScore").getAsJsonArray();
		}

		JsonArray atScoreDetails = new JsonArray();
		if (liveGameJsonObj.has("awayTeamlineScore")) {
			atScoreDetails = liveGameJsonObj.get("awayTeamlineScore").getAsJsonArray();
		}
		scHomeTeam.add("scoreDetails", htScoreDetails);
		scAwayTeam.add("scoreDetails", atScoreDetails);
		// scoreData.add("scoreboard_title",
		// new JsonPrimitive(getScoreBoardTitle(activeGame)));
		scoreData.add("sport", new JsonPrimitive(solrDoc.getAsJsonObject().get("sport").getAsString()));
	}

	/**
	 * Returns Score board title.
	 * 
	 * @param activeGame
	 *            the active game object
	 * @return the score board title
	 */
	public String getScoreBoardTitle(ActiveTeamGame activeGame) {
		//
		// Fill the score card title
		//
		String scoreCardTitle = "";
		String activeAwayTeamId = activeGame.getAwayTeamId();

		JsonArray allTeamGamesJson = SportsDataFacade$.MODULE$.getLiveInfoForActiveTeam(activeGame).getAsJsonObject()
				.get("hits").getAsJsonObject().get("hits").getAsJsonArray();
		int team1Wins = 0; // awayTeam
		int team2Wins = 0; // homeTeam
		String team1ExtId = activeAwayTeamId;
		for (JsonElement it : allTeamGamesJson) {
			JsonObject teamGamesJson = it.getAsJsonObject().get("_source").getAsJsonObject();
			int homeWins = teamGamesJson.get("homeScoreRuns").getAsInt();
			int awayWins = teamGamesJson.get("awayScoreRuns").getAsInt();
			String awayTeamId = teamGamesJson.get("awayTeamExtId").getAsString();
			int t1Score = 0;
			int t2Score = 0;
			if (awayTeamId.equals(team1ExtId)) {
				t1Score = awayWins;
				t2Score = homeWins;
			} else {
				t1Score = homeWins;
				t2Score = awayWins;
			}
			if (t1Score > t2Score) {
				team1Wins++;
			} else {
				team2Wins++;
			}

		}

		String seriesLeaderName = "-";
		String seriesTitle = "-";
		if (team1Wins > team2Wins) {
			seriesLeaderName = activeGame.getAwayTeamName();
			seriesTitle = "lead series " + team1Wins + "-" + team2Wins;
		} else if (team1Wins < team2Wins) {
			seriesLeaderName = activeGame.getHomeTeamName();
			seriesTitle = "lead series " + team2Wins + "-" + team1Wins;
		} else {
			seriesTitle = "Series tied " + team1Wins + "-" + team2Wins;
		}

		scoreCardTitle = String.format("%s %s", seriesLeaderName, seriesTitle);

		return scoreCardTitle;
	}

}
