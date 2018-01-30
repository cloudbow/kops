/*
 * SportsCloudMCDelegate.java
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.slingmedia.sportscloud.facade.*;
import com.slingmedia.sportscloud.netty.rest.model.ActiveTeamGame;
import com.slingmedia.sportscloud.netty.rest.model.League;
import com.slingmedia.sportscloud.netty.rest.model.Role;

/**
 * Response modeling for web-view Sports Media Card
 * 
 * @author arung
 * @version 1.0
 * @since 1.0
 */
public class SportsCloudMCDelegate extends AbstractSportsCloudRestDelegate {
	public static final Logger LOGGER = LoggerFactory.getLogger(SportsCloudMCDelegate.class);

	/**
	 * Prepares JSON Response modeling for Sports Media card
	 * 
	 * @param gameScheduleId
	 *            the game schedule id
	 * @param teamId
	 *            the team id
	 * @param subpackIds
	 *            the subscription packs
	 * @param gameFinderDrillDownJson
	 *            the game index data
	 * @param activeGame
	 *            the current active game object
	 */
	public void prepareMCJson(String gameScheduleId, String teamId, Set<String> subpackIds,
			JsonObject gameFinderDrillDownJson, ActiveTeamGame activeGame) {
		if (activeGame.getActiveTeamRole() != Role.NONE) {

			JsonArray gameSchedules = new JsonArray();
			JsonObject mc = new JsonObject();
			JsonArray scoringEvents = new JsonArray();
			JsonObject divisionSeries = new JsonObject();
			JsonObject standings = new JsonObject();
			// todo
			gameFinderDrillDownJson.add("active_team", new JsonPrimitive(teamId));
			gameFinderDrillDownJson.add("gamesSchedule", gameSchedules);
			gameFinderDrillDownJson.add("mc", mc);
			gameFinderDrillDownJson.add("drives", scoringEvents);
			gameFinderDrillDownJson.add("division_series", divisionSeries);
			gameFinderDrillDownJson.add("standings", standings);

			prepareMCGameSchedule(teamId, activeGame, gameSchedules);

			String gameId = prepareMCMainGameData(activeGame, gameScheduleId, mc, subpackIds);

			// http://localhost:"+solrPort+"/solr/techproducts/select?wt=json&indent=true&fl=id,name&q=solr+memory&group=true&group.field=manu_exact
			prepareMCTeamStandings(activeGame, standings, League.MLB.toString().toLowerCase());

			prepareMCDrives(gameId, teamId, scoringEvents);

		}
	}

	/**
	 * Prepares JSON Response modeling for Sports Media card for non MLB leagues
	 * 
	 * @param gameScheduleId
	 *            the game schedule id
	 * @param teamId
	 *            the team id
	 * @param subpackIds
	 *            the subscription packs
	 * @param gameFinderDrillDownJson
	 *            the game index data
	 * @param activeGame
	 *            the current active game object
	 * @param league
	 *            the league name
	 */
	public void prepareNonMlbMCJson(String gameScheduleId, String teamId, Set<String> subpackIds,
			JsonObject gameFinderDrillDownJson, ActiveTeamGame activeGame, String league) {
		if (activeGame.getActiveTeamRole() != Role.NONE) {

			JsonArray gameSchedules = new JsonArray();
			JsonObject mc = new JsonObject();
			JsonArray scoringEvents = new JsonArray();
			JsonArray drives = new JsonArray();
			JsonObject standings = new JsonObject();
			// todo
			gameFinderDrillDownJson.add("active_team", new JsonPrimitive(teamId));
			gameFinderDrillDownJson.add("gamesSchedule", gameSchedules);
			gameFinderDrillDownJson.add("mc", mc);
			gameFinderDrillDownJson.add("drives", drives);
			gameFinderDrillDownJson.add("scoring_events", scoringEvents);
			gameFinderDrillDownJson.add("standings", standings);

			prepareNonMlbMCGameSchedule(teamId, activeGame, gameSchedules, drives);

			String gameId = prepareMCMainGameData(activeGame, gameScheduleId, mc, subpackIds);

			// http://localhost:"+solrPort+"/solr/techproducts/select?wt=json&indent=true&fl=id,name&q=solr+memory&group=true&group.field=manu_exact
			prepareMCTeamStandings(activeGame, standings, "cfb");

			prepareNcaafMCDrives(gameId, teamId, scoringEvents);
			// prepareMCFootBallDrives();

		}
	}

	/**
	 * Prepares JSON response for drives for specific game
	 * 
	 * @param gameId
	 *            the game id
	 * @param teamId
	 *            the team id
	 * @param scoringEvents
	 *            the scoring events
	 */
	public void prepareMCDrives(String gameId, String teamId, JsonArray scoringEvents) {
		JsonArray scoringEvtsResponse = SportsDataFacade$.MODULE$.getAllScoringEventsForGame(gameId).getAsJsonObject()
				.get("hits").getAsJsonObject().get("hits").getAsJsonArray();
		for (JsonElement driveSrcDoc : scoringEvtsResponse) {
			JsonObject scoringEventItem = new JsonObject();
			JsonObject driveDoc = driveSrcDoc.getAsJsonObject().get("_source").getAsJsonObject();
			if (driveDoc.has("lastPlay")) {
				scoringEventItem.add("comment", new JsonPrimitive(driveDoc.get("lastPlay").getAsString()));
				scoringEventItem.add("img", new JsonPrimitive(driveDoc.get("img").getAsString()));
				scoringEventItem.add("title", new JsonPrimitive(driveDoc.get("inningTitle").getAsString()));
				String teamIdDrive = "0";
				if (driveDoc.has("teamId")) {
					teamIdDrive = driveDoc.get("teamId").getAsString();
				}
				scoringEventItem.add("teamId", new JsonPrimitive(teamIdDrive));
				scoringEvents.add(scoringEventItem);
			}

		}
	}

	/**
	 * Prepares JSON for league Ncaaf media card drives
	 * 
	 * @param gameId
	 *            the game id
	 * @param teamId
	 *            the team id
	 * @param scoringEvents
	 *            the scoring events
	 */
	public void prepareNcaafMCDrives(String gameId, String teamId, JsonArray scoringEvents) {
		JsonArray scoringEvtsResponse = SportsDataFacade$.MODULE$.getAllScoringEventsForGame(gameId).getAsJsonObject()
				.get("hits").getAsJsonObject().get("hits").getAsJsonArray();
		for (JsonElement driveSrcDoc : scoringEvtsResponse) {
			JsonObject scoringEventItem = new JsonObject();
			JsonObject driveDoc = driveSrcDoc.getAsJsonObject().get("_source").getAsJsonObject();
			if (driveDoc.has("lastPlay")) {
				scoringEventItem.add("description", new JsonPrimitive(driveDoc.get("lastPlay").getAsString()));
				scoringEventItem.add("img", new JsonPrimitive(driveDoc.get("img").getAsString()));
				scoringEventItem.add("period", new JsonPrimitive(driveDoc.get("period").getAsString()));
				scoringEventItem.add("timer", new JsonPrimitive(driveDoc.get("timer").getAsString()));
				scoringEventItem.add("type", new JsonPrimitive(driveDoc.get("playType").getAsString()));
				scoringEventItem.add("position", new JsonPrimitive(driveDoc.get("position").getAsBigDecimal()));
				if (driveDoc.has("seconds")) {
					scoringEventItem.add("seconds", new JsonPrimitive(driveDoc.get("seconds").getAsInt()));
				} else {
					scoringEventItem.add("seconds", new JsonPrimitive(driveDoc.get("gameTimeSeconds").getAsInt()));
				}
				scoringEvents.add(scoringEventItem);
			}

		}
	}

	/**
	 * Prepares JSON for league standings
	 * 
	 * @param divisionTeam
	 *            the division team
	 * @return the team standings
	 */
	private JsonObject prepareLeagueStandingObj(JsonObject divisionTeam) {
		JsonObject leagueStanding = new JsonObject();
		// todo
		leagueStanding.add("alias", new JsonPrimitive("-"));
		leagueStanding.add("city", new JsonPrimitive(divisionTeam.getAsJsonObject().get("teamCity").getAsString()));
		leagueStanding.add("id", new JsonPrimitive(divisionTeam.getAsJsonObject().get("id").getAsString()));
		// todo
		leagueStanding.add("img", new JsonPrimitive(divisionTeam.getAsJsonObject().get("img").getAsString()));

		leagueStanding.add("name", new JsonPrimitive(divisionTeam.getAsJsonObject().get("teamName").getAsString()));
		leagueStanding.add("pct", new JsonPrimitive(divisionTeam.getAsJsonObject().get("pct").getAsString()));
		JsonObject teamRecord = new JsonObject();
		teamRecord.add("wins", new JsonPrimitive(divisionTeam.getAsJsonObject().get("wins").getAsString()));
		teamRecord.add("losses", new JsonPrimitive(divisionTeam.getAsJsonObject().get("losses").getAsString()));
		// todo
		teamRecord.add("ties", new JsonPrimitive("0"));
		teamRecord.add("pct", new JsonPrimitive(divisionTeam.getAsJsonObject().get("pct").getAsString()));

		leagueStanding.add("teamRecord", teamRecord);
		return leagueStanding;
	}

	/**
	 * Prepares JSON for team standings
	 * 
	 * @param activeGame
	 *            the active game
	 * @param standings
	 *            the team standings object
	 * @param league
	 *            the league
	 */
	public void prepareMCTeamStandings(ActiveTeamGame activeGame, JsonObject standings, String league) {
		standings.add("league", new JsonPrimitive(league));
		List<String> leagueList = new ArrayList<>();

		JsonElement tResponseJson = SportsDataFacade$.MODULE$.getMainLeaguesForActiveGame(activeGame);
		JsonArray tDocs = tResponseJson.getAsJsonObject().get("aggregations").getAsJsonObject().get("top_tags")
				.getAsJsonObject().get("buckets").getAsJsonArray();
		Iterator<JsonElement> it = tDocs.iterator();
		while (it.hasNext()) {
			JsonElement elem = it.next();
			leagueList.add(elem.getAsJsonObject().get("key").getAsString());
		}

		int noOfLeagues = 0;
		for (String subLeague : leagueList) {

			noOfLeagues++;
			standings.add(new StringBuilder().append("team").append(noOfLeagues).append("_title").toString(),
					new JsonPrimitive(subLeague));
			JsonArray teamArray = new JsonArray();
			standings.add(new StringBuilder().append("team").append(noOfLeagues).toString(), teamArray);

			try {

				JsonElement groupedTSRespJson = SportsDataFacade$.MODULE$.getSubLeagues(subLeague);
				if (groupedTSRespJson != null) {

					JsonArray mainQueryDocs = groupedTSRespJson.getAsJsonObject().get("aggregations").getAsJsonObject()
							.get("top_tags").getAsJsonObject().get("buckets").getAsJsonArray();
					mainQueryDocs.forEach(mainGroup -> {

						JsonObject divisionObj = new JsonObject();
						JsonObject mainGroupJsonObj = mainGroup.getAsJsonObject();
						String currentDivision = mainGroupJsonObj.get("key").getAsString();
						divisionObj.add("division", new JsonPrimitive(currentDivision));
						JsonArray leagueStandings = new JsonArray();
						// Add the first item to group

						divisionObj.add("league_standings", leagueStandings);
						teamArray.add(divisionObj);

						JsonArray expandedDocs = mainGroup.getAsJsonObject().get("top_division_hits").getAsJsonObject()
								.get("hits").getAsJsonObject().get("hits").getAsJsonArray();

						expandedDocs.forEach(divisionSrcDoc -> {
							JsonObject divisionTeam = divisionSrcDoc.getAsJsonObject().get("_source").getAsJsonObject();
							JsonObject leagueStanding = prepareLeagueStandingObj(divisionTeam);

							leagueStandings.add(leagueStanding);

						});

					});

				}

			} catch (Exception e1) {
				LOGGER.error("Error occurred in parsing json", e1);
			}

		}
	}

	/**
	 * Prepares JSON for only main media card section
	 * 
	 * @param activeGame
	 *            the active game
	 * @param gameScheduleId
	 *            the game schedule id
	 * @param mc
	 *            the media card
	 * @param subpackIds
	 *            the subscription packs
	 * @return the JSON for main media card section
	 */
	public String prepareMCMainGameData(ActiveTeamGame activeGame, String gameScheduleId, JsonObject mc,
			Set<String> subpackIds) {
		String gameId = "0"; // avoid nullpointer
		JsonArray gameResponse = getGameForGameId(gameScheduleId);
		if (gameResponse != null && gameResponse.size() != 0) {
			JsonObject sportDataItem = new JsonObject();

			JsonObject solrDoc = getMatchedGame(sportDataItem, gameResponse);

			gameId = solrDoc.get("gameId").getAsString();
			String awayPitcherId = "0";
			if (solrDoc.getAsJsonObject().has("awayPlayerExtId")) {
				awayPitcherId = solrDoc.getAsJsonObject().get("awayPlayerExtId").getAsString();
			}
			String homePitcherId = "0";
			if (solrDoc.getAsJsonObject().has("homePlayerExtId")) {
				homePitcherId = solrDoc.getAsJsonObject().get("homePlayerExtId").getAsString();
			}

			JsonArray htPlayerStatsJsonArr = SportsDataFacade$.MODULE$.getPlayerStatsById(homePitcherId)
					.getAsJsonObject().get("hits").getAsJsonObject().get("hits").getAsJsonArray();
			JsonArray atPlayerStatsJsonArr = SportsDataFacade$.MODULE$.getPlayerStatsById(awayPitcherId)
					.getAsJsonObject().get("hits").getAsJsonObject().get("hits").getAsJsonArray();
			int homePitcherWins = 0;
			int homePitcherLosses = 0;
			if (htPlayerStatsJsonArr.size() > 0) {
				JsonObject htPLayerStatsJson = htPlayerStatsJsonArr.get(0).getAsJsonObject().get("_source")
						.getAsJsonObject();
				homePitcherWins = htPLayerStatsJson.get("wins").getAsInt();
				homePitcherLosses = htPLayerStatsJson.get("losses").getAsInt();
			}

			int awayPitcherWins = 0;
			int awayPitcherLosses = 0;
			if (atPlayerStatsJsonArr.size() > 0) {
				JsonObject atPlayerStatsJson = atPlayerStatsJsonArr.get(0).getAsJsonObject().get("_source")
						.getAsJsonObject();
				awayPitcherWins = atPlayerStatsJson.get("wins").getAsInt();
				awayPitcherLosses = atPlayerStatsJson.get("losses").getAsInt();
			}

			getSportData(solrDoc, sportDataItem, true, homePitcherWins, homePitcherLosses, awayPitcherWins,
					awayPitcherLosses, gameResponse);
			mc.add("sport_data", sportDataItem);
			String teaser = "-";
			if (solrDoc.getAsJsonObject().has("preGameTeaser")) {
				teaser = solrDoc.getAsJsonObject().get("preGameTeaser").getAsString();
			}
			mc.add("anons", new JsonPrimitive(teaser));
			mc.add("anons_title", new JsonPrimitive(solrDoc.getAsJsonObject().get("anonsTitle").getAsString()));

			mergeLiveInfoToMediaCard(activeGame, mc, solrDoc, sportDataItem);

		}

		return gameId;
	}

	/**
	 * Prepares JSON for media card game schedule
	 * 
	 * @param teamId
	 *            the team id
	 * @param gameRole
	 *            the game role
	 * @param gameSchedules
	 *            the game schedules
	 */
	public void prepareMCGameSchedule(String teamId, ActiveTeamGame gameRole, JsonArray gameSchedules) {

		long prevSixMonth = Instant.now().getEpochSecond() - Math.round(6 * 30 * 24 * 60 * 60);

		JsonElement gameScheduleResponseJson = SportsDataFacade$.MODULE$.getGameSchedulesForMediaCard(gameRole, teamId);
		Map<String, JsonObject> liveResponseJson = prepareLiveGameInfoData(prevSixMonth, Long.MAX_VALUE, 3000);

		// create game schedule json part
		if (gameScheduleResponseJson != null) {

			try {

				JsonArray docs = gameScheduleResponseJson.getAsJsonObject().get("aggregations").getAsJsonObject()
						.get("top_tags").getAsJsonObject().get("buckets").getAsJsonArray();
				for (JsonElement groupedDocSrc : docs) {

					JsonArray gameScheduleArr = groupedDocSrc.getAsJsonObject().get("top_game_mc_hits")
							.getAsJsonObject().get("hits").getAsJsonObject().get("hits").getAsJsonArray();
					JsonObject solrDoc = gameScheduleArr.get(0).getAsJsonObject().get("_source").getAsJsonObject();
					JsonObject sportData = new JsonObject();
					JsonObject sportDataItem = new JsonObject();
					getSportData(solrDoc, sportDataItem, false, 0, 0, 0, 0, gameScheduleArr);
					updateScoreStatusFromLive(liveResponseJson, sportDataItem, solrDoc.get("gameId").getAsString());
					sportData.add("sport_data", sportDataItem);
					gameSchedules.add(sportData);

				}
			} catch (Exception e) {
				LOGGER.error("Error occurred in parsing json", e);
			}

		}
	}

	/**
	 * Prepares JSON for media card for non MLB game schedule
	 * 
	 * @param teamId
	 *            the team id
	 * @param gameRole
	 *            the game role
	 * @param gameSchedules
	 *            the game schedule
	 * @param drives
	 *            the object for the drives
	 */
	public void prepareNonMlbMCGameSchedule(String teamId, ActiveTeamGame gameRole, JsonArray gameSchedules,
			JsonArray drives) {

		long prevSixMonth = Instant.now().getEpochSecond() - Math.round(6 * 30 * 24 * 60 * 60);

		JsonElement gameScheduleResponseJson = SportsDataFacade$.MODULE$.getGameSchedulesForMediaCard(gameRole, teamId);
		Map<String, JsonObject> liveResponseJson = prepareLiveGameInfoData(prevSixMonth, Long.MAX_VALUE, 3000);

		// create game schedule json part
		if (gameScheduleResponseJson != null) {

			try {

				JsonArray docs = gameScheduleResponseJson.getAsJsonObject().get("aggregations").getAsJsonObject()
						.get("top_tags").getAsJsonObject().get("buckets").getAsJsonArray();
				for (JsonElement groupedDocSrc : docs) {

					JsonArray gameScheduleArr = groupedDocSrc.getAsJsonObject().get("top_game_mc_hits")
							.getAsJsonObject().get("hits").getAsJsonObject().get("hits").getAsJsonArray();
					JsonObject solrDoc = gameScheduleArr.get(0).getAsJsonObject().get("_source").getAsJsonObject();
					JsonObject sportData = new JsonObject();
					JsonObject sportDataItem = new JsonObject();
					getSportData(solrDoc, sportDataItem, false, 0, 0, 0, 0, gameScheduleArr);
					updateScoreStatusFromLive(liveResponseJson, sportDataItem, solrDoc.get("gameId").getAsString());
					updateDrivesFroLive(liveResponseJson, drives, solrDoc.get("gameId").getAsString());
					sportData.add("sport_data", sportDataItem);
					gameSchedules.add(sportData);

				}
			} catch (Exception e) {
				LOGGER.error("Error occurred in parsing json", e);
			}

		}
	}

	/**
	 * Updates drives for live game info
	 * 
	 * @param liveResponseJson
	 *            the live game info
	 * @param drives
	 *            the drives for the game
	 * @param gameId
	 *            the game id
	 */
	public void updateDrivesFroLive(Map<String, JsonObject> liveResponseJson, JsonArray drives, String gameId) {
		if (liveResponseJson.get(gameId) != null) {
			try {
				if (liveResponseJson.get(gameId).has("drives")) {
					JsonArray flatDrives = liveResponseJson.get(gameId).get("drives").getAsJsonArray();
					for (JsonElement driveSum : flatDrives) {
						String driveStr = driveSum.getAsString();
						JsonParser parser = new JsonParser();
						JsonObject drive = parser.parse(driveStr).getAsJsonObject();
						drives.add(drive);
					}
				}
			} catch (Exception e) {
				LOGGER.error("Error occured in parsing json", e);
			}

		}

	}

	/**
	 * Returns the JSON response for given external URL
	 * 
	 * @param requestURLBuilder
	 *            the URL of the external service
	 * @return the JSON response for given external URL
	 */
	public JsonElement getJsonObject(StringBuilder requestURLBuilder) {
		JsonParser parser = new JsonParser();
		String responseString = ExternalHttpClient$.MODULE$.getFromUrl(requestURLBuilder.toString());
		JsonElement responseJson = parser.parse("{}");
		try {
			responseJson = parser.parse(responseString);
		} catch (Exception e) {
			LOGGER.error("Error occured in parsing json", e);
		}
		return responseJson;
	}

}
