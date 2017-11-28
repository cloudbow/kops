/*
 * SportsCloudRestGamesHandler.java
 * @author jayachandra
 **********************************************************************

             Copyright (c) 2004 - 2014 by Sling Media, Inc.

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
package com.slingmedia.sportscloud.netty.rest.server.handlers;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.slingmedia.sportscloud.facade.*;
import com.slingmedia.sportscloud.netty.rest.server.handler.delegates.SportsCloudHomeScreenDelegate;
import com.slingmedia.sportscloud.netty.rest.server.handler.delegates.SportsCloudMCDelegate;

/**
 * The Class SportsCloudRestGamesHandler.
 *
 * @author jayachandra
 */
public class SportsCloudRestGamesHandler {

	/** The Constant LOGGER. */
	public static final Logger LOGGER = LoggerFactory.getLogger(SportsCloudRestGamesHandler.class);

	private static final String REGEX_CATEGORIES_URL = "^/api/slingtv/airtv/v1/games\\?(.+)";

	private static final String REGEX_GAMES_URL = "^/api/slingtv/airtv/v1/games/(.[^/]+)\\?(.+)";

	private static final String REGEX_MC_URL = "^/api/slingtv/airtv/v1/game/(.[^/]+)/(.[^/]+)/(.[^/]+)";

	private static final Pattern REGEX_CATEGORIES_URL_PATTERN = Pattern.compile(REGEX_CATEGORIES_URL);

	private static final Pattern REGEX_GAMES_URL_PATTERN = Pattern.compile(REGEX_GAMES_URL);

	private static final Pattern REGEX_MC_URL_PATTERN = Pattern.compile(REGEX_MC_URL);

	private DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("YYYY-MM-dd Z").withLocale(Locale.US);

	private SportsCloudHomeScreenDelegate sportsCloudHomeScreenDelegate = new SportsCloudHomeScreenDelegate();

	private enum RestName {
		CATEGORIES, GAMES, MC, NONE
	}

	public String handle(String uri, Map<String, List<String>> params) {
		String finalResponse = null;

		RestName restName = isValidUrl(uri);
		String gameCategory = null;
		String gameId = null;

		switch (restName) {
		case CATEGORIES:
			LOGGER.info("This is CATEGORIES " + uri);
			finalResponse = handleCategories(params);
			break;
		case GAMES:
			LOGGER.info("This is GAMES " + uri);
			Matcher gameMatcher = REGEX_GAMES_URL_PATTERN.matcher(uri);
			if (gameMatcher.find()) {
				gameCategory = gameMatcher.group(1);
			} else {
				gameCategory = null;
			}
			finalResponse = handleGames(params, gameCategory);
			break;
		case MC:
			LOGGER.info("This is MC " + uri);
			Matcher mcMatcher = REGEX_MC_URL_PATTERN.matcher(uri);
			if (mcMatcher.find()) {
				gameCategory = mcMatcher.group(1);
				gameId = mcMatcher.group(2);
			} else {
				gameCategory = null;
				gameId = null;

			}
			finalResponse = handleGameMediaCard(params, gameCategory, gameId);
			break;
		default:
			LOGGER.info("This is NONE " + uri);
			finalResponse = "{}";

		}

		return finalResponse;
	}

	private String handleCategories(Map<String, List<String>> params) {
		String finalResponse = null;

		long startDate = Instant.now().getEpochSecond();
		if (params.get("startDate") != null) {
			startDate = dateTimeFormatter.parseDateTime(params.get("startDate").get(0) + " " + params.get("tz").get(0))
					.getMillis() / 1000;
		}
		long endDate = Long.MAX_VALUE;
		if (params.get("endDate") != null) {
			endDate = dateTimeFormatter.parseDateTime(params.get("endDate").get(0) + " " + params.get("tz").get(0))
					.getMillis() / 1000;
		}

		finalResponse = prepareGamesCategoriesDataForHomeScreen(finalResponse, startDate, endDate);
		return finalResponse;
	}

	private String handleGames(Map<String, List<String>> params, String gameCategory) {
		String finalResponse = null;

		long startDate = Instant.now().getEpochSecond();
		if (params.get("startDate") != null) {
			startDate = dateTimeFormatter.parseDateTime(params.get("startDate").get(0) + " " + params.get("tz").get(0))
					.getMillis() / 1000;
		}
		long endDate = Long.MAX_VALUE;
		if (params.get("endDate") != null) {
			endDate = dateTimeFormatter.parseDateTime(params.get("endDate").get(0) + " " + params.get("tz").get(0))
					.getMillis() / 1000;
		}

		finalResponse = prepareGameScheduleDataForCategoryForHomeScreen(finalResponse, startDate, endDate,
				gameCategory);
		return finalResponse;
	}

	private String handleGameMediaCard(Map<String, List<String>> params, String gameCategory, String gameId) {
		String finalResponse = null;

		finalResponse = prepareMCData(gameId, gameCategory);
		return finalResponse;
	}

	private Boolean isValid(String uri, Pattern pattern) {
		return pattern.matcher(uri).matches();
	}

	private RestName isValidUrl(String uri) {
		RestName restName = null;

		if (isValid(uri, REGEX_CATEGORIES_URL_PATTERN)) {
			restName = RestName.CATEGORIES;
		} else if (isValid(uri, REGEX_GAMES_URL_PATTERN)) {
			restName = RestName.GAMES;
		} else if (isValid(uri, REGEX_MC_URL_PATTERN)) {
			restName = RestName.MC;
		} else {
			restName = RestName.NONE;
		}

		return restName;
	}

	public static void main(String[] args) {
		SportsCloudRestGamesHandler handler = new SportsCloudRestGamesHandler();
		handler.handle("/api/slingtv/airtv/v1/games?tz=-0700&from=2017-11-07&to=2017-11-11", null);
		handler.handle("/api/slingtv/airtv/v1/games/nba?tz=-0700&from=2017-11-07&to=2017-11-11", null);
		handler.handle(
				"/api/slingtv/airtv/v1/game/nba/72297/Tue%201107,%20Bucks%20at%20Cavaliers​?tz=-0700&from=2017-11-07&to=2017-11-11",
				null);
		handler.handle("/api/slingtv/airtv/v1/game/nba/72297/Tue%201107,%20Bucks%20at%20Cavaliers​", null);
		handler.handle(
				"/api/slingtv/airtv/v1/game/nba/72297/Tue%2011/07,%20Bucks%20at%20Cavaliers/invalid​?tz=-0700&from=2017-11-07&to=2017-11-11",
				null);
		handler.handle(
				"/api/slingtv/airtv/v1/game/nba/72297/Tue%201107,%20Bucks%20at%20Cavaliers/invalid​?tz=-0700&from=2017-11-07&to=2017-11-11",
				null);

	}

	private String prepareGamesCategoriesDataForHomeScreen(String finalResponse, long startDate, long endDate) {

		JsonElement gameSchedulesJson = SportsDataGamesFacade$.MODULE$.getGamesCategoriesDataForHomeScreen(startDate,
				endDate);
		finalResponse = sportsCloudHomeScreenDelegate.prepareJsonResponseForCategories(finalResponse, startDate,
				endDate, new HashSet<String>(), gameSchedulesJson);
		return finalResponse;
	}

	private String prepareGameScheduleDataForCategoryForHomeScreen(String finalResponse, long startDate, long endDate,
			String gameCategory) {

		JsonElement gameSchedulesJson = SportsDataGamesFacade$.MODULE$
				.getGameScheduleDataForCategoryForHomeScreen(startDate, endDate, gameCategory);
		finalResponse = sportsCloudHomeScreenDelegate.prepareJsonResponseForHomeScreen(finalResponse, startDate,
				endDate, new HashSet<String>(), gameSchedulesJson);
		return finalResponse;
	}

	private String prepareMCData(String gameScheduleId, String league) {
		String finalResponse = "{}";
		JsonObject gameFinderDrillDownJson = new JsonObject();
		JsonObject mc = new JsonObject();

		if (gameScheduleId != null) {

			try {
				JsonArray currGameDocs = getGameForGameId(gameScheduleId);
				//System.out.println("game_info:" + currGameDocs.toString());
				mc.add("game_info", currGameDocs);

				JsonArray liveGameInfoRespJsonArr = getLiveGamesById(gameScheduleId);
				//System.out.println("live_info:" + currGameDocs.toString());
				mc.add("live_info", liveGameInfoRespJsonArr);

				gameFinderDrillDownJson.add("mc", mc);

				finalResponse = gameFinderDrillDownJson.toString();

			} catch (Exception e) {
				LOGGER.error("Error occurred in parsing json", e);
			}

		} else if (gameScheduleId == null) {

			finalResponse = "{}";
			//System.out.println("game_info:" + finalResponse);

		}

		return finalResponse;
	}

	private JsonArray getLiveGamesById(String gameId) {

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(String.format("Getting date for %s", gameId));
		}
		JsonArray currGameDocs = null;
		JsonElement currentGameRespJson = SportsDataGamesFacade$.MODULE$.getLiveGameById(gameId);

		JsonArray groupedDocs = currentGameRespJson.getAsJsonObject().get("aggregations").getAsJsonObject()
				.get("top_tags").getAsJsonObject().get("buckets").getAsJsonArray();

		// get game live info
		JsonObject liveInfo = groupedDocs.get(0).getAsJsonObject();

		if (liveInfo != null) {
			currGameDocs = liveInfo.get("live_info").getAsJsonObject().get("hits").getAsJsonObject().get("hits")
					.getAsJsonArray();
		} else {
			currGameDocs = new JsonArray();
		}

		//System.out.println(currGameDocs.toString());

		return currGameDocs;

	}

	private JsonArray getGameForGameId(String gameId) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(String.format("Getting date for %s", gameId));
		}
		JsonArray currGameDocs = null;
		JsonElement currentGameRespJson = SportsDataGamesFacade$.MODULE$.getGameScheduleByGameCode(gameId);

		JsonArray groupedDocs = currentGameRespJson.getAsJsonObject().get("aggregations").getAsJsonObject()
				.get("top_tags").getAsJsonObject().get("buckets").getAsJsonArray();

		// get game info
		JsonObject gameInfo = groupedDocs.get(0).getAsJsonObject();

		if (gameInfo != null) {
			currGameDocs = gameInfo.get("game_info").getAsJsonObject().get("hits").getAsJsonObject().get("hits")
					.getAsJsonArray();
		} else {
			currGameDocs = new JsonArray();
		}

		//System.out.println(currGameDocs.toString());

		return currGameDocs;
	}

}
