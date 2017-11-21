package com.slingmedia.sportscloud.netty.rest.server.handler.delegates;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class SportsCloudHomeScreenDelegate extends AbstractSportsCloudRestDelegate{
	public static final Logger LOGGER = LoggerFactory.getLogger(SportsCloudHomeScreenDelegate.class);
	public String prepareJsonResponseForHomeScreen(String finalResponse, long startDate, long endDate, Set<String> subpackIds, JsonElement gameSchedulesJson) {
		// &fq=%7B!collapse%20field=gameId%7D&expand=true&fl=homeTeamScore,awayTeamScore&expand.rows=100&wt=json
		Map<String, JsonObject> liveResponseJson = prepareLiveGameInfoData(startDate, endDate,500);
	
		if (gameSchedulesJson != null) {
			JsonArray allGames = new JsonArray();
			try {
				JsonArray groupedDocs = gameSchedulesJson.getAsJsonObject().get("aggregations").getAsJsonObject()
						.get("top_tags").getAsJsonObject().get("buckets").getAsJsonArray();
				for (JsonElement groupedDocSrc : groupedDocs) {
					JsonObject mainObj = new JsonObject();
					JsonObject solrDoc = null;
					// get a list of subpackage ids
	
					JsonArray homeScreenGameScheduleGroup = groupedDocSrc.getAsJsonObject().get("top_game_home_hits").getAsJsonObject().get("hits").getAsJsonObject().get("hits").getAsJsonArray();
					solrDoc = getSubscribedOrFirstGameSchedule(subpackIds, mainObj,
							homeScreenGameScheduleGroup);
					JsonObject gameScheduleJson = solrDoc.getAsJsonObject();
					String gameId = solrDoc.get("gameId").getAsString();
	
					updateScoreStatusFromLive(liveResponseJson, mainObj, gameId);
					mainObj.add("channelGuid", new JsonPrimitive(gameScheduleJson.get("channel_guid").getAsString()));
					mainObj.add("programGuid", new JsonPrimitive(gameScheduleJson.get("program_guid").getAsString()));
					mainObj.add("assetGuid", new JsonPrimitive(gameScheduleJson.get("asset_guid").getAsString()));
					mainObj.add("id", new JsonPrimitive(gameScheduleJson.get("gameId").getAsString()));
					mainObj.add("sport", new JsonPrimitive(gameScheduleJson.get("sport").getAsString()));
					mainObj.add("league",
							new JsonPrimitive(gameScheduleJson.get("league").getAsString().toLowerCase()));
					addGameScheduleDates(mainObj, gameScheduleJson);
	
					// collection
					mainObj.add("rating", new JsonPrimitive(gameScheduleJson.get("gexPredict").getAsString()));
					String teaser = "-";
					if (gameScheduleJson.has("preGameTeaser")) {
						teaser = gameScheduleJson.get("preGameTeaser").getAsString();
					}
					mainObj.add("teaser", new JsonPrimitive(teaser));
					JsonObject homeTeam = new JsonObject();
					JsonObject awayTeam = new JsonObject();
					JsonObject homeTeamRecord = new JsonObject();
					JsonObject awayTeamRecord = new JsonObject();
	
					mainObj.add("homeTeam", homeTeam);
					homeTeam.add("name", new JsonPrimitive(gameScheduleJson.get("homeTeamName").getAsString()));
					String homeTeamAlias = "-";
					if (solrDoc.has("homeTeamAlias")) {
						homeTeamAlias = solrDoc.get("homeTeamAlias").getAsString();
						homeTeam.add("alias", new JsonPrimitive(homeTeamAlias));
					}
					homeTeam.add("img", new JsonPrimitive(gameScheduleJson.get("homeTeamImg").getAsString()));
					homeTeam.add("id", new JsonPrimitive(gameScheduleJson.get("homeTeamExternalId").getAsString()));
					mainObj.add("awayTeam", awayTeam);
					awayTeam.add("name", new JsonPrimitive(gameScheduleJson.get("awayTeamName").getAsString()));
					String awayTeamAlias = "-";
					if (solrDoc.has("awayTeamAlias")) {
						awayTeamAlias = solrDoc.get("awayTeamAlias").getAsString();
						awayTeam.add("alias", new JsonPrimitive(awayTeamAlias));
					}
					awayTeam.add("img", new JsonPrimitive(gameScheduleJson.get("awayTeamImg").getAsString()));
					awayTeam.add("id", new JsonPrimitive(gameScheduleJson.get("awayTeamExternalId").getAsString()));
	
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
					JsonArray contentIds = createContentIds(homeScreenGameScheduleGroup);
					JsonObject contentIdChannelGuidMap = createContentIdAssetInfoMap(homeScreenGameScheduleGroup);
					mainObj.add("cIdToAsstInfo", contentIdChannelGuidMap);
					mainObj.add("contentId", contentIds);
	
					String callsign = "-";
					if(gameScheduleJson.has("callsign")){
						callsign = gameScheduleJson.get("callsign").getAsString();
					}
					mainObj.add("callsign", new JsonPrimitive(callsign));
					
					JsonArray subPackageTitles = new JsonArray();
					if(gameScheduleJson.has("subpack_titles")){
						subPackageTitles = gameScheduleJson.get("subpack_titles").getAsJsonArray();
					}
					mainObj.add("subPackTitles",subPackageTitles);
					JsonObject statsObj = new JsonObject();
					JsonObject statsHomeTeam = new JsonObject();
					JsonObject statsAwayTeam = new JsonObject();
					JsonArray homeScoreArray = new JsonArray();
					JsonArray awayScoreArray = new JsonArray();
					statsHomeTeam.add("scoreDetails", homeScoreArray);
					statsAwayTeam.add("scoreDetails", awayScoreArray);
					statsObj.add("homeTeam", statsHomeTeam);
					statsObj.add("awayTeam", statsAwayTeam);
					allGames.add(mainObj);
				}
			} catch (Exception e) {
				LOGGER.error("Error occurred in parsing json", e);
			} finally {
				finalResponse = allGames.toString();
			}
		}
		return finalResponse;
	}

}
