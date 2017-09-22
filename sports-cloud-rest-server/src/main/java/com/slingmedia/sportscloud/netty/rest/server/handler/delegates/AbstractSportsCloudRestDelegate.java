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

public class AbstractSportsCloudRestDelegate {
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSportsCloudRestDelegate.class);
	static final int[] FIELD_STATE_MAP = new int[] { 0, 1, 2, 4, 3, 7, 5, 6 };

	public JsonArray getGameForGameId(String gameId) {
		if(LOGGER.isTraceEnabled()){
			LOGGER.trace(String.format("Getting date for %s",gameId));
		}
		JsonElement currentGameRespJson = SportsDataFacade$.MODULE$.getGameScheduleByGameCode(gameId);
		JsonArray currGameDocs = currentGameRespJson.getAsJsonObject().get("hits").getAsJsonObject().get("hits")
				.getAsJsonArray();
		return currGameDocs;
	}
	
	public JsonObject createContentIdAssetInfoMap(JsonArray gameResponseArr) {
		JsonObject contentIdChGuid = new JsonObject();
		gameResponseArr.forEach(it -> {
			JsonObject gameJsonObj = it.getAsJsonObject().get("_source").getAsJsonObject();
			if(gameJsonObj.has("channel_guid") && gameJsonObj.has("schedule_guid")) {
				JsonObject assetInfoJson = new JsonObject();
				contentIdChGuid.add(gameJsonObj.get("schedule_guid").getAsString(),assetInfoJson);
				assetInfoJson.add("channelGuid",new JsonPrimitive(gameJsonObj.get("channel_guid").getAsString()));
				assetInfoJson.add("assetGuid",new JsonPrimitive(gameJsonObj.get("asset_guid").getAsString()));
			}
		});
		return contentIdChGuid;
	}

	/**
	 * This method returns an active team + the game selected for the media card
	 * 
	 * @param teamId
	 * @param currGameDocs
	 * @return
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
			gameId = currGamesDoc.get("gameCode").getAsString();
			gameType = GameType.getValue(currGamesDoc.get("gameType").getAsString());
		}
		if (teamId.equals(homeTeamId)) {
			role = Role.HOME;
		} else if (teamId.equals(awayTeamId)) {
			role = Role.AWAY;
		}
		return new ActiveTeamGame( gameId,gameType, teamId, homeTeamId, awayTeamId, homeTeamName, awayTeamName, role);
	}

	public Map<String, JsonObject> prepareLiveGameInfoData(long startDate, long endDate, int sizeToReturn) {
		JsonArray liveResponseJsonArr = SportsDataFacade$.MODULE$.getAllLiveGamesInDateRange(startDate,endDate,sizeToReturn).getAsJsonObject().get("hits").getAsJsonObject().get("hits").getAsJsonArray();
		Map<String, JsonObject> liveJsonObjects = new HashMap<>();
		liveResponseJsonArr
				.forEach(it -> {
					JsonObject liveJsonObject = it.getAsJsonObject().get("_source").getAsJsonObject();
					liveJsonObjects.put(liveJsonObject.get("gameId").getAsString(), liveJsonObject);
	
				});
		return liveJsonObjects;
	}

	public JsonArray createContentIds(JsonArray alldocs) {
		JsonArray contentIds = new JsonArray();
		Set<String> scheduleGuids =getUniqueScheduleGuids(alldocs);
	
		if(scheduleGuids.isEmpty()){
			contentIds.add(getDummyContentId());
		} else {
			scheduleGuids.forEach(it-> {
				contentIds.add(new JsonPrimitive(it));
			});
		}
	
		return contentIds;
	}

	public JsonObject getSubscribedOrFirstGameSchedule(Set<String> subpackIds, JsonObject sportDataItem, JsonArray gameScheduleArray) {
	
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
			//get the first of unmatched content not available in slingtv
			solrDoc = gameScheduleArray.get(0).getAsJsonObject().get("_source").getAsJsonObject();
			sportDataItem.add("contentInfo", new JsonPrimitive(GameAvailability.UNAVAILABLE.toString()));
		} else {
			//found at least one/more intersection with slingtv
			//Do a priority ordering and findout for poc.
			List<JsonObject> priorityOrdered = new ArrayList<>();
			foundItems.forEach( it -> {
				String callsign = "" ;
				if(it.has("callsign")) {
					callsign = it.get("callsign").getAsString();
				}
				if(		callsign.startsWith("CSNBA")  
						|| callsign.startsWith("CSNCH") 
						|| callsign.startsWith("CSNCA")
						|| callsign.startsWith("CSNDC")
						|| callsign.startsWith("CSNMA")
						|| callsign.startsWith("ESPN") ){
					priorityOrdered.add(it);
				}			
			});
			if(priorityOrdered.size()!=0) {
				solrDoc = priorityOrdered.get(0);
			} else {
				solrDoc = foundItems.get(0);
			}
			sportDataItem.add("contentInfo", new JsonPrimitive(GameAvailability.AVAILABLE.toString()));
		}
	
		return solrDoc;
	}

	public void updateScoreStatusFromLive(Map<String, JsonObject> liveResponseJson, JsonObject mainObj, String gameId) {
		if (liveResponseJson.get(gameId) != null) {
			String awayTeamScore = liveResponseJson.get(gameId).get("awayScoreRuns").getAsString();
			String homeTeamScore = liveResponseJson.get(gameId).get("homeScoreRuns").getAsString();
			mainObj.add("homeScore", new JsonPrimitive(homeTeamScore));
			mainObj.add("awayScore", new JsonPrimitive(awayTeamScore));
			mainObj.add("gameStatus", new JsonPrimitive(GameStatus
					.getValue(liveResponseJson.get(gameId).get("statusId").getAsInt()).toString()));
		} else {
			mainObj.add("gameStatus", new JsonPrimitive(GameStatus.UPCOMING.toString()));
		}
	}

	protected void getSportData(JsonElement solrDoc, JsonObject sportDataItem, boolean addPitcherDetails, int homePitcherWins, int homePitcherLosses, int awayPitcherWins,
			int awayPitcherLosses, JsonArray gameResponse) {
				JsonObject gameScheduleJsonObj = solrDoc.getAsJsonObject();
				sportDataItem.add("gameId", new JsonPrimitive(gameScheduleJsonObj.get("gameCode").getAsString()));
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
				if(gameScheduleJsonObj.has("homeTeamPitcherName")){
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
				if(gameScheduleJsonObj.has("awayTeamPitcherName")){
					awayTeamPitcherName = gameScheduleJsonObj.get("awayTeamPitcherName").getAsString();
				}
				awayTeam.add("pitcherName", new JsonPrimitive(awayTeamPitcherName));
				if (addPitcherDetails) {
					addPitcherWinsLosses(awayPitcherWins, awayPitcherLosses, awayTeam);
				}
			
				// todo
				homeTeamRecord.add("wins", new JsonPrimitive(0l));
				// todo
				homeTeamRecord.add("losses", new JsonPrimitive(0l));
				// todo
				homeTeamRecord.add("ties", new JsonPrimitive(0l));
			
				// todo
				awayTeamRecord.add("wins", new JsonPrimitive(0l));
				// todo
				awayTeamRecord.add("losses", new JsonPrimitive(0l));
				// todo
				awayTeamRecord.add("ties", new JsonPrimitive(0l));
			
				homeTeam.add("teamRecord", homeTeamRecord);
				awayTeam.add("teamRecord", awayTeamRecord);
			
				// todo
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
				if(gameScheduleJsonObj.has("callsign")){
					sportDataItem.add("callsign", new JsonPrimitive(gameScheduleJsonObj.get("callsign").getAsString()));
				}
				if (gameScheduleJsonObj.has("stadiumName")) {
					sportDataItem.add("location", new JsonPrimitive(gameScheduleJsonObj.get("stadiumName").getAsString()));
				}
				
				addSubPackIds(sportDataItem, gameScheduleJsonObj);
				
				updateGameStatusAndType(sportDataItem, gameScheduleJsonObj);
			
			}

	public void addGameScheduleDates(JsonObject sportDataItem, JsonObject gameScheduleJsonObj) {
		//Change this once done
		long gameDateEpoch = gameScheduleJsonObj.get("game_date_epoch").getAsLong();
	
		long startTimeEpoch =  0;
		long stopTimeEpoch = 0;
		if(gameScheduleJsonObj.has("startTimeEpoch")) {
			startTimeEpoch = gameScheduleJsonObj.get("startTimeEpoch").getAsLong();
		}
		if(gameScheduleJsonObj.has("stopTimeEpoch")){
			stopTimeEpoch = gameScheduleJsonObj.get("stopTimeEpoch").getAsLong();		
		} else {
			if(startTimeEpoch==0){
				stopTimeEpoch = gameDateEpoch + Math.round(3.5*60*60);
			} else {
				stopTimeEpoch = startTimeEpoch + Math.round(3.5*60*60);
			}
		}
		sportDataItem.add("stopTimeEpoch", new JsonPrimitive(stopTimeEpoch));
		sportDataItem.add("startTimeEpoch", new JsonPrimitive(startTimeEpoch));
		if(startTimeEpoch!=0 && startTimeEpoch > gameDateEpoch ){
			addTapeDelay(sportDataItem,true);
		} else {
			addTapeDelay(sportDataItem,false);
		}	
		addScheduledDate(sportDataItem, gameDateEpoch);
		addGameDateEpoch(sportDataItem, gameDateEpoch);
	
	}

	Set<String> getUniqueScheduleGuids(JsonArray alldocs) {
		Set<String>  scheduleGuids = new HashSet<String>();
		alldocs.forEach(it -> {
			JsonObject item = it.getAsJsonObject().get("_source").getAsJsonObject();
			if(item.has("schedule_guid") 
					&& 
			  ! "0".equals(item.get("schedule_guid").getAsString())) {
				scheduleGuids.add(item.get("schedule_guid").getAsString());
			}
			
		});
		return scheduleGuids;
	}

	String getDummyContentId() {
		return new StringBuilder("noid_").append(UUID.randomUUID().toString()).toString();
	}

	void addPitcherWinsLosses(int homePitcherWins, int homePitcherLosses, JsonObject team) {
	
		team.add("pitcherWins", new JsonPrimitive(homePitcherWins));
		team.add("pitcherLosses", new JsonPrimitive(homePitcherLosses));
	
	}

	void addSubPackIds(JsonObject sportDataItem, JsonObject gameScheduleJsonObj) {
		JsonArray subPackIds = new JsonArray();
		if(gameScheduleJsonObj.has("subpackage_guids")){
			subPackIds = gameScheduleJsonObj.get("subpackage_guids").getAsJsonArray();
		}
		sportDataItem.add("subPackageGuids", subPackIds);
	}

	protected void mergeLiveInfoToMediaCard(ActiveTeamGame activeGame, JsonObject mc, JsonObject solrDoc, JsonObject mcSportData) {
		//
		// Fill in the live scores and other details
		//
		String gameId = solrDoc.get("gameId").getAsString();
		JsonArray liveGameInfoRespJsonArr = getLiveGamesById(gameId);
		if (liveGameInfoRespJsonArr.size() > 0) {
			// pick the first item
			JsonObject liveGameJsonObj = liveGameInfoRespJsonArr.get(0).getAsJsonObject().get("_source").getAsJsonObject();
	
			// update home&away scores to media card
			mcSportData.add("homeScore", new JsonPrimitive(liveGameJsonObj.get("homeScoreRuns").getAsInt()));
			mcSportData.add("awayScore", new JsonPrimitive(liveGameJsonObj.get("awayScoreRuns").getAsInt()));
			addFieldsCount(mcSportData, liveGameJsonObj);
	
			// update score data into media card
			addScoreData(activeGame, mc, solrDoc, liveGameJsonObj);
			addCurrentPlayerDetails(mcSportData, liveGameJsonObj);
			updateGameStatusAndType(mcSportData, liveGameJsonObj);
		}
	
	}

	void updateGameStatusAndType(JsonObject sportDataItem, JsonObject gameScheduleJsonObj) {
		// information is contianed in both live and scheduled game and hence
		// separated out.
		sportDataItem.add("gameStatus",
				new JsonPrimitive(GameStatus.getValue(gameScheduleJsonObj.get("statusId").getAsInt()).toString()));
		sportDataItem.add("gameType",
				new JsonPrimitive(GameType.getValue(gameScheduleJsonObj.get("gameType").getAsString()).toString()));
	}

	void addTapeDelay(JsonObject sportDataItem, boolean tapeDelay) {
		sportDataItem.add("tape_delayed_game", new JsonPrimitive(tapeDelay));
	}

	void addScheduledDate(JsonObject sportDataItem, long gameDateEpoch) {
		Instant epochTime = Instant.ofEpochSecond(gameDateEpoch);
		ZonedDateTime utc = epochTime.atZone(ZoneId.of("Z"));
		String pattern = "EEE, dd MMM yyyy HH:mm:ss Z";
		String scheduledDate = utc.format(java.time.format.DateTimeFormatter.ofPattern(pattern));
		sportDataItem.add("scheduledDate", new JsonPrimitive(scheduledDate));
	}

	void addGameDateEpoch(JsonObject sportDataItem, long gameDateEpoch) {
		sportDataItem.add("gameDateEpoch", new JsonPrimitive(gameDateEpoch));
	}

	JsonArray getLiveGamesById(String gameId) {
		JsonArray liveGameInfoRespJson = SportsDataFacade$.MODULE$.getLiveGameById(gameId).getAsJsonObject().get("hits")
				.getAsJsonObject().get("hits").getAsJsonArray();
		return liveGameInfoRespJson;
	}

	void addFieldsCount(JsonObject mcSportData, JsonObject liveGameJsonObj) {
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

	void addCurrentPlayerDetails(JsonObject mcSportData, JsonObject liveGameJsonObj) {
		GameStatus status = GameStatus.getValue(liveGameJsonObj.get("statusId").getAsInt());
		if (liveGameJsonObj.has("isHomePitching") && status != GameStatus.COMPLETED) {
			JsonObject homeTeam = mcSportData.get("homeTeam").getAsJsonObject();
			JsonObject awayTeam = mcSportData.get("awayTeam").getAsJsonObject();
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

	void addScoreData(ActiveTeamGame activeGame, JsonObject mc, JsonObject solrDoc, JsonObject liveGameJsonObj) {
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
		scoreData.add("scoreboard_title",
				new JsonPrimitive(getScoreBoardTitle(activeGame)));
		scoreData.add("sport", new JsonPrimitive(solrDoc.getAsJsonObject().get("sport").getAsString()));
	}

	String getScoreBoardTitle(ActiveTeamGame activeGame) {
		//
		// Fill the score card title
		//
		String scoreCardTitle = "";
		String activeAwayTeamId = activeGame.getAwayTeamId();
		
		JsonArray allTeamGamesJson = SportsDataFacade$.MODULE$.getLiveInfoForActiveTeam(activeGame).getAsJsonObject().get("hits")
				.getAsJsonObject().get("hits").getAsJsonArray();
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
