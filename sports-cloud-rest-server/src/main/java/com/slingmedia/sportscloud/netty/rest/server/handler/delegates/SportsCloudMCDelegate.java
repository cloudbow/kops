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


public class SportsCloudMCDelegate extends AbstractSportsCloudRestDelegate {
	public static final Logger LOGGER = LoggerFactory.getLogger(SportsCloudMCDelegate.class);


	public void prepareMCJson( String gameScheduleId, String teamId, Set<String> subpackIds, JsonObject gameFinderDrillDownJson, ActiveTeamGame activeGame) {
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
	
			String gameId =prepareMCMainGameData(activeGame, gameScheduleId, mc, subpackIds);
	
			// http://localhost:"+solrPort+"/solr/techproducts/select?wt=json&indent=true&fl=id,name&q=solr+memory&group=true&group.field=manu_exact
			prepareMCTeamStandings(activeGame, standings, League.MLB.toString().toLowerCase());
	
			prepareMCDrives(gameId, teamId, scoringEvents);
	
		}
	}
	
	public void prepareMCDrives(String gameId, String teamId, JsonArray scoringEvents) {
		JsonArray scoringEvtsResponse = SportsDataFacade$.MODULE$.getAllScoringEventsForGame(gameId).getAsJsonObject().get("response")
				.getAsJsonObject().get("docs").getAsJsonArray();
		for (JsonElement drive : scoringEvtsResponse) {
			JsonObject scoringEventItem = new JsonObject();
			if (drive.getAsJsonObject().has("lastPlay")) {
				scoringEventItem.add("comment",
						new JsonPrimitive(drive.getAsJsonObject().get("lastPlay").getAsString()));
				scoringEventItem.add("img", new JsonPrimitive(drive.getAsJsonObject().get("img").getAsString()));
				scoringEventItem.add("title",
						new JsonPrimitive(drive.getAsJsonObject().get("inningTitle").getAsString()));
				String teamIdDrive = "0";
				if (drive.getAsJsonObject().has("teamId")) {
					teamIdDrive = drive.getAsJsonObject().get("teamId").getAsString();
				}
				scoringEventItem.add("teamId", new JsonPrimitive(teamIdDrive));
				scoringEvents.add(scoringEventItem);
			}

		}
	}
	
	private JsonObject prepareLeagueStandingObj(JsonObject divisionTeam) {
		JsonObject leagueStanding = new JsonObject();
		// todo
		leagueStanding.add("alias", new JsonPrimitive("-"));
		leagueStanding.add("city",
				new JsonPrimitive(divisionTeam.getAsJsonObject().get("teamCity").getAsString()));
		leagueStanding.add("id",
				new JsonPrimitive(divisionTeam.getAsJsonObject().get("id").getAsString()));
		// todo
		leagueStanding.add("img",
				new JsonPrimitive(divisionTeam.getAsJsonObject().get("img").getAsString()));

		leagueStanding.add("name",
				new JsonPrimitive(divisionTeam.getAsJsonObject().get("teamName").getAsString()));
		leagueStanding.add("pct",
				new JsonPrimitive(divisionTeam.getAsJsonObject().get("pct").getAsString()));
		JsonObject teamRecord = new JsonObject();
		teamRecord.add("wins",
				new JsonPrimitive(divisionTeam.getAsJsonObject().get("wins").getAsString()));
		teamRecord.add("losses",
				new JsonPrimitive(divisionTeam.getAsJsonObject().get("losses").getAsString()));
		// todo
		teamRecord.add("ties", new JsonPrimitive("0"));
		teamRecord.add("pct",
				new JsonPrimitive(divisionTeam.getAsJsonObject().get("pct").getAsString()));

		leagueStanding.add("teamRecord", teamRecord);
		return leagueStanding;
	}

	

	

	public void prepareMCTeamStandings(ActiveTeamGame activeGame, JsonObject standings, String league) {
		standings.add("league", new JsonPrimitive(league));
		List<String> leagueList = new ArrayList<>();
	
		
		JsonElement tResponseJson = SportsDataFacade$.MODULE$.getMainLeaguesForActiveGame(activeGame);
		JsonArray tDocs = tResponseJson.getAsJsonObject().get("facet_counts").getAsJsonObject()
				.get("facet_fields").getAsJsonObject().get("subLeague").getAsJsonArray();			
		Iterator<JsonElement> it = tDocs.iterator();
		while(it.hasNext()){
			JsonElement elem = it.next();
			if(it.hasNext()){
				if(it.next().getAsInt()>0){
					leagueList.add(elem.getAsString());
				}
			}
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

					JsonArray mainQueryDocs = groupedTSRespJson.getAsJsonObject().get("response").getAsJsonObject().get("docs").getAsJsonArray();
					mainQueryDocs.forEach(mainGroup -> {
						
						JsonObject divisionObj = new JsonObject();
						JsonObject mainGroupJsonObj = mainGroup.getAsJsonObject();
						String currentDivision = mainGroupJsonObj.get("division").getAsString();
						divisionObj.add("division", new JsonPrimitive(currentDivision));
						JsonArray leagueStandings = new JsonArray();
						//Add the first item to group
						leagueStandings.add(prepareLeagueStandingObj(mainGroupJsonObj));
						divisionObj.add("league_standings", leagueStandings);
						teamArray.add(divisionObj);
						
						JsonArray expandedDocs = groupedTSRespJson.getAsJsonObject().
								get("expanded").
								getAsJsonObject().get(currentDivision).getAsJsonObject().get("docs").getAsJsonArray();
						
						expandedDocs.forEach(divisionDoc -> {
								JsonObject divisionTeam = divisionDoc.getAsJsonObject();
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
	
	public String prepareMCMainGameData(ActiveTeamGame activeGame, String gameScheduleId, JsonObject mc,
			Set<String> subpackIds) {
		String gameId = "0"; // avoid nullpointer
		JsonArray gameResponse = getGameForGameId(gameScheduleId);
		if (gameResponse != null && gameResponse.size() != 0) {
			JsonObject sportDataItem = new JsonObject();

			JsonObject solrDoc = getSubscribedOrFirstGameSchedule(subpackIds, sportDataItem, gameResponse);

			gameId = solrDoc.get("gameId").getAsString();
			String awayPitcherId="0";
			if(solrDoc.getAsJsonObject().has("awayPlayerExtId")){
				 awayPitcherId = solrDoc.getAsJsonObject().get("awayPlayerExtId").getAsString();
			}
			String homePitcherId = "0";
			if(solrDoc.getAsJsonObject().has("homePlayerExtId")){
				homePitcherId = solrDoc.getAsJsonObject().get("homePlayerExtId").getAsString();
			}
			
			JsonArray htPlayerStatsJson = SportsDataFacade$.MODULE$.getPlayerStatsById(homePitcherId).getAsJsonObject().get("response")
					.getAsJsonObject().get("docs").getAsJsonArray();
			JsonArray atPlayerStatsJson = SportsDataFacade$.MODULE$.getPlayerStatsById(awayPitcherId).getAsJsonObject().get("response")
					.getAsJsonObject().get("docs").getAsJsonArray();
			int homePitcherWins = 0;
			int homePitcherLosses = 0;
			if (htPlayerStatsJson.size() > 0) {
				homePitcherWins = htPlayerStatsJson.get(0).getAsJsonObject().get("wins").getAsInt();
				homePitcherLosses = htPlayerStatsJson.get(0).getAsJsonObject().get("losses").getAsInt();
			}

			int awayPitcherWins = 0;
			int awayPitcherLosses = 0;
			if (atPlayerStatsJson.size() > 0) {
				awayPitcherWins = atPlayerStatsJson.get(0).getAsJsonObject().get("wins").getAsInt();
				awayPitcherLosses = atPlayerStatsJson.get(0).getAsJsonObject().get("losses").getAsInt();
			}

			getSportData(solrDoc, sportDataItem, true, homePitcherWins, homePitcherLosses, awayPitcherWins,
					awayPitcherLosses,gameResponse);
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
	
	public void prepareMCGameSchedule(String teamId, ActiveTeamGame gameRole, JsonArray gameSchedules) {

		long prevSixMonth = Instant.now().getEpochSecond()-Math.round(6*30*24*60*60);

		JsonElement gameScheduleResponseJson = SportsDataFacade$.MODULE$.getGameSchedulesForMediaCard(gameRole,teamId);
		Map<String, JsonObject> liveResponseJson = prepareLiveGameInfoData(prevSixMonth, "*");

		// create game schedule json part
		if (gameScheduleResponseJson != null) {

			try {

				JsonArray docs = gameScheduleResponseJson.getAsJsonObject().get("grouped").getAsJsonObject()
						.get("gameCode").getAsJsonObject().get("groups").getAsJsonArray();
				for (JsonElement groupedDoc : docs) {
					
					JsonObject solrDoc = groupedDoc.getAsJsonObject().get("doclist").getAsJsonObject().get("docs")
							.getAsJsonArray().get(0).getAsJsonObject();
					JsonObject sportData = new JsonObject();
					JsonObject sportDataItem = new JsonObject();
					getSportData(solrDoc, sportDataItem, false, 0, 0, 0, 0,groupedDoc.getAsJsonObject().get("doclist").getAsJsonObject().get("docs")
							.getAsJsonArray());
					updateScoreStatusFromLive(liveResponseJson, sportDataItem, solrDoc.get("gameId").getAsString());
					sportData.add("sport_data", sportDataItem);
					gameSchedules.add(sportData);

				}
			} catch (Exception e) {
				LOGGER.error("Error occurred in parsing json", e);
			}

		}
	}
	


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
