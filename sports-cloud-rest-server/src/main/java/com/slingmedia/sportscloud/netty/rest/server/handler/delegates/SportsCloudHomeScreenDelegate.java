/*
 * SportsCloudHomeScreenDelegate.java
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

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Response modeling for web-view Sports Home screen
 * 
 * @author arung
 * @version 1.0
 * @since 1.0
 */
public class SportsCloudHomeScreenDelegate extends AbstractSportsCloudRestDelegate {
	
	/** The Constant LOGGER. */
	public static final Logger LOGGER = LoggerFactory.getLogger(SportsCloudHomeScreenDelegate.class);

	/**
	 * Prepares JSON Response modeling for Sports Home Screen
	 * 
	 * @param finalResponse
	 *            the JSON for home screen
	 * @param startDate
	 *            the schedule start date
	 * @param endDate
	 *            the schedule end date
	 * @param subpackIds
	 *            the subscription packs
	 * @param gameSchedulesJson
	 *            the game schedule index data
	 * @return the JSON Response modeling for Sports Home Screen
	 */
	public String prepareJsonResponseForHomeScreen(String finalResponse, long startDate, long endDate,
			Set<String> subpackIds, JsonElement gameSchedulesJson) {
		// &fq=%7B!collapse%20field=gameId%7D&expand=true&fl=homeTeamScore,awayTeamScore&expand.rows=100&wt=json
		Map<String, JsonObject> liveResponseJson = prepareLiveGameInfoData(startDate, endDate, 500);

		if (gameSchedulesJson != null) {
			JsonArray allGames = new JsonArray();
			try {
				JsonArray groupedDocsByGameId = gameSchedulesJson.getAsJsonObject()
						.get("aggregations").getAsJsonObject()
						.get("group_by_gameId").getAsJsonObject()
						.get("buckets").getAsJsonArray();
				for(JsonElement groupByGameId : groupedDocsByGameId) {
					JsonArray groupedMaxBatchTimeDocs = groupByGameId.getAsJsonObject()
							.get("top_hits_by_batch_time").getAsJsonObject()
							.get("buckets").getAsJsonArray();
					for (JsonElement maxBatchTimeDoc : groupedMaxBatchTimeDocs) {

							JsonObject mainObj = new JsonObject();
						JsonObject solrDoc = null;
						// get a list of subpackage ids

						JsonArray homeScreenGameScheduleGroup = maxBatchTimeDoc.getAsJsonObject().get("top_game_hits")
								.getAsJsonObject().get("hits").getAsJsonObject().get("hits").getAsJsonArray();
						solrDoc = getMatchedGame(mainObj, homeScreenGameScheduleGroup);
						JsonObject gameScheduleJson = solrDoc.getAsJsonObject();
						String gameId = solrDoc.get("gameId").getAsString();


						mainObj.add("channelGuid", new JsonPrimitive(gameScheduleJson.get("channel_guid").getAsString()));
						mainObj.add("programGuid", new JsonPrimitive(gameScheduleJson.get("program_guid").getAsString()));
						mainObj.add("assetGuid", new JsonPrimitive(gameScheduleJson.get("asset_guid").getAsString()));
						mainObj.add("id", new JsonPrimitive(gameScheduleJson.get("gameId").getAsString()));
						mainObj.add("sport", new JsonPrimitive(gameScheduleJson.get("sport").getAsString()));
						mainObj.add("league",
								new JsonPrimitive(gameScheduleJson.get("league").getAsString().toLowerCase()));
						addGameScheduleDates(mainObj, gameScheduleJson);

						// let mainObj fill with game_date_epoch and call liveScores
						updateScoreStatusFromLive(liveResponseJson, mainObj, gameId);

						// title
						mainObj.add("anons_title",
								new JsonPrimitive(solrDoc.getAsJsonObject().get("anonsTitle").getAsString()));

						// collection
						mainObj.add("rating", new JsonPrimitive(gameScheduleJson.get("gexPredict").getAsString()));

						// Sling TV ratings
						if (gameScheduleJson.has("ratings")) {
							mainObj.add("ratings", gameScheduleJson.get("ratings").getAsJsonArray());
						}

						String teaser = "-";
						if (gameScheduleJson.has("preGameTeaser")) {
							teaser = gameScheduleJson.get("preGameTeaser").getAsString();
						}
						mainObj.add("teaser", new JsonPrimitive(teaser));

						// anons
						mainObj.add("anons", new JsonPrimitive(teaser));

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
						// Added for city
						String homeTeamCity = "-";
						if (solrDoc.has("homeTeamCity")) {
							homeTeamCity = solrDoc.get("homeTeamCity").getAsString();
							homeTeam.add("city", new JsonPrimitive(homeTeamCity));
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
						// Added for city
						String awayTeamCity = "-";
						if (solrDoc.has("awayTeamCity")) {
							awayTeamCity = solrDoc.get("awayTeamCity").getAsString();
							awayTeam.add("city", new JsonPrimitive(awayTeamCity));
						}

						awayTeam.add("img", new JsonPrimitive(gameScheduleJson.get("awayTeamImg").getAsString()));
						awayTeam.add("id", new JsonPrimitive(gameScheduleJson.get("awayTeamExternalId").getAsString()));

						// populate default values
						homeTeamRecord.add("wins", new JsonPrimitive(0l));
						homeTeamRecord.add("losses", new JsonPrimitive(0l));
						homeTeamRecord.add("ties", new JsonPrimitive(0l));
						awayTeamRecord.add("wins", new JsonPrimitive(0l));
						awayTeamRecord.add("losses", new JsonPrimitive(0l));
						awayTeamRecord.add("ties", new JsonPrimitive(0l));

						homeTeam.add("teamRecord", homeTeamRecord);
						awayTeam.add("teamRecord", awayTeamRecord);
						JsonArray contentIds = createContentIds(homeScreenGameScheduleGroup);
						JsonObject contentIdChannelGuidMap = createContentIdAssetInfoMap(homeScreenGameScheduleGroup);
						mainObj.add("cIdToAsstInfo", contentIdChannelGuidMap);
						mainObj.add("contentId", contentIds);

						String callsign = "-";
						if (gameScheduleJson.has("callsign")) {
							callsign = gameScheduleJson.get("callsign").getAsString();
						}
						mainObj.add("callsign", new JsonPrimitive(callsign));

						JsonArray subPackageTitles = new JsonArray();
						if (gameScheduleJson.has("subpack_titles")) {
							subPackageTitles = gameScheduleJson.get("subpack_titles").getAsJsonArray();
						}
						mainObj.add("subPackTitles", subPackageTitles);
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
			}
			} catch (Exception e) {
				LOGGER.error("Error occurred in parsing json", e);
			} finally {
				finalResponse = allGames.toString();
			}
		}
		return finalResponse;
	}

	/**
	 * Prepares JSON Response modeling for Sports categories or Ribbons
	 * 
	 * @param finalResponse
	 *            the JSON for home screen
	 * @param startDate
	 *            the schedule start date
	 * @param endDate
	 *            the schedule end date
	 * @param subpackIds
	 *            the subscription packs
	 * @param gamesCategoriesJson
	 *            the game categories index data
	 * @return the JSON Response modeling for Sports categories
	 */
	public String prepareJsonResponseForCategories(String finalResponse, long startDate, long endDate,
			Set<String> subpackIds, JsonElement gamesCategoriesJson) {
		// &fq=%7B!collapse%20field=gameId%7D&expand=true&fl=homeTeamScore,awayTeamScore&expand.rows=100&wt=json
		// Map<String, JsonObject> liveResponseJson =
		// prepareLiveGameInfoData(startDate, endDate,500);

		if (gamesCategoriesJson != null) {
			JsonArray allCategories = new JsonArray();
			JsonObject jsonCategories = new JsonObject();
			try {
				JsonArray groupedDocs = gamesCategoriesJson.getAsJsonObject().get("aggregations").getAsJsonObject()
						.get("top_tags").getAsJsonObject().get("buckets").getAsJsonArray();
				for (JsonElement groupedDocSrc : groupedDocs) {
					// get game categories

					String gameCategory = groupedDocSrc.getAsJsonObject().get("key").getAsString();
					System.out.println(gameCategory);

					allCategories.add(gameCategory);
				}

				jsonCategories.add("games_categories", allCategories);
			} catch (Exception e) {
				LOGGER.error("Error occurred in parsing json", e);
			} finally {
				finalResponse = jsonCategories.toString();
			}
		}
		return finalResponse;
	}

}
