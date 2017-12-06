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

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.utils.URIBuilder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.slingmedia.sportscloud.facade.*;
import com.slingmedia.sportscloud.netty.client.model.Channel;
import com.slingmedia.sportscloud.netty.client.model.GameStats;
import com.slingmedia.sportscloud.netty.client.model.MediaCard;
import com.slingmedia.sportscloud.netty.client.model.NagraStats;
import com.slingmedia.sportscloud.netty.client.model.Ribbon;
import com.slingmedia.sportscloud.netty.client.model.Ribbons;
import com.slingmedia.sportscloud.netty.client.model.SportTeam;
import com.slingmedia.sportscloud.netty.client.model.SportsMediaCard;
import com.slingmedia.sportscloud.netty.client.model.SportsTileAsset;
import com.slingmedia.sportscloud.netty.client.model.Thumbnail;
import com.slingmedia.sportscloud.netty.client.model.ThuuzStats;
import com.slingmedia.sportscloud.netty.client.model.TileAsset;
import com.slingmedia.sportscloud.netty.rest.server.handler.delegates.SportsCloudHomeScreenDelegate;

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

	private static Configuration jsonPathConf = Configuration.defaultConfiguration();

	private DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("YYYY-MM-dd Z").withLocale(Locale.US);

	private SportsCloudHomeScreenDelegate sportsCloudHomeScreenDelegate = new SportsCloudHomeScreenDelegate();

	private enum RestName {
		CATEGORIES, GAMES, MC, NONE
	}

	private String getNewCategoryName(String category) {
		String newCategory = "";

		switch (category) {
		case "MLB":
			newCategory = "Baseball";
			break;

		case "NCAAF":
			newCategory = "College Football";
			break;

		case "NFL":
			newCategory = "Football";
			break;

		default:
			newCategory = "other";
			break;

		}

		return newCategory;
	}

	public String handle(String host, String uri, Map<String, List<String>> params) {
		String finalResponse = null;

		RestName restName = isValidUrl(uri);
		String gameCategory = null;
		String gameId = null;

		switch (restName) {
		case CATEGORIES:
			LOGGER.info("This is CATEGORIES " + uri);
			finalResponse = handleCategories(host, params);
			break;
		case GAMES:
			LOGGER.info("This is GAMES " + uri);
			Matcher gameMatcher = REGEX_GAMES_URL_PATTERN.matcher(uri);
			if (gameMatcher.find()) {
				gameCategory = gameMatcher.group(1);
			} else {
				gameCategory = null;
			}
			finalResponse = handleGames(host, params, gameCategory);
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
			finalResponse = handleGameMediaCard(params, gameCategory, gameId, host);
			break;
		default:
			LOGGER.info("This is NONE " + uri);
			finalResponse = "{}";

		}

		return finalResponse;
	}

	private String handleCategories(String serverHost, Map<String, List<String>> params) {
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

		String output = transformToClientFormatForCategories(finalResponse, params.get("tz").get(0),
				params.get("startDate").get(0), params.get("endDate").get(0), serverHost);

		return output;
	}

	private String handleGames(String serverHost, Map<String, List<String>> params, String gameCategory) {
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

		String output = transformToClientFormatForGames(finalResponse, gameCategory, params.get("tz").get(0),
				params.get("startDate").get(0), params.get("endDate").get(0), serverHost);

		return output;
	}

	private String handleGameMediaCard(Map<String, List<String>> params, String gameCategory, String gameId,
			String serverHost) {
		String finalResponse = null;

		finalResponse = prepareJsonResponseMCForGame(gameId, gameCategory, new HashSet<String>());
		String output = transformToClientFormatForMediaCard(finalResponse, gameCategory, serverHost);
		return output;
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
		handler.handle("localhost", "/api/slingtv/airtv/v1/games?tz=-0700&from=2017-11-07&to=2017-11-11", null);
		handler.handle("localhost", "/api/slingtv/airtv/v1/games/nba?tz=-0700&from=2017-11-07&to=2017-11-11", null);
		handler.handle("localhost",
				"/api/slingtv/airtv/v1/game/nba/72297/Tue%201107,%20Bucks%20at%20Cavaliers​?tz=-0700&from=2017-11-07&to=2017-11-11",
				null);
		handler.handle("localhost", "/api/slingtv/airtv/v1/game/nba/72297/Tue%201107,%20Bucks%20at%20Cavaliers​", null);
		handler.handle("localhost",
				"/api/slingtv/airtv/v1/game/nba/72297/Tue%2011/07,%20Bucks%20at%20Cavaliers/invalid​?tz=-0700&from=2017-11-07&to=2017-11-11",
				null);
		handler.handle("localhost",
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
				.getGameScheduleDataForCategoryForHomeScreen(startDate, endDate, gameCategory.toUpperCase());
		finalResponse = sportsCloudHomeScreenDelegate.prepareJsonResponseForHomeScreen(finalResponse, startDate,
				endDate, new HashSet<String>(), gameSchedulesJson);
		return finalResponse;
	}

	private String prepareJsonResponseMCForGame(String gameScheduleId, String league, Set<String> subpackIds) {
		String finalResponse = "{}";

		Map<String, JsonObject> liveResponseJson = getLiveGamesById(gameScheduleId);
		JsonArray homeScreenGameScheduleGroup = getGameForGameId(gameScheduleId);

		JsonArray allGames = new JsonArray();
		try {

			JsonObject mainObj = new JsonObject();
			JsonObject solrDoc = null;
			// get a list of subpackage ids

			// JsonArray homeScreenGameScheduleGroup =
			// groupedDocSrc.getAsJsonObject().get("top_game_home_hits").getAsJsonObject().get("hits").getAsJsonObject().get("hits").getAsJsonArray();
			solrDoc = sportsCloudHomeScreenDelegate.getSubscribedOrFirstGameSchedule(subpackIds, mainObj,
					homeScreenGameScheduleGroup);
			JsonObject gameScheduleJson = solrDoc.getAsJsonObject();
			String gameId = solrDoc.get("gameId").getAsString();

			sportsCloudHomeScreenDelegate.updateScoreStatusFromLive(liveResponseJson, mainObj, gameId);
			mainObj.add("channelGuid", new JsonPrimitive(gameScheduleJson.get("channel_guid").getAsString()));
			mainObj.add("programGuid", new JsonPrimitive(gameScheduleJson.get("program_guid").getAsString()));
			mainObj.add("assetGuid", new JsonPrimitive(gameScheduleJson.get("asset_guid").getAsString()));
			mainObj.add("id", new JsonPrimitive(gameScheduleJson.get("gameId").getAsString()));
			mainObj.add("sport", new JsonPrimitive(gameScheduleJson.get("sport").getAsString()));
			mainObj.add("league", new JsonPrimitive(gameScheduleJson.get("league").getAsString().toLowerCase()));
			sportsCloudHomeScreenDelegate.addGameScheduleDates(mainObj, gameScheduleJson);

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
			JsonArray contentIds = sportsCloudHomeScreenDelegate.createContentIds(homeScreenGameScheduleGroup);
			JsonObject contentIdChannelGuidMap = sportsCloudHomeScreenDelegate
					.createContentIdAssetInfoMap(homeScreenGameScheduleGroup);
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
		} catch (Exception e) {
			LOGGER.error("Error occurred in parsing json", e);
		} finally {
			finalResponse = allGames.toString();
		}

		return finalResponse;
	}

	/*
	 * private String prepareMCData(String gameScheduleId, String league) {
	 * String finalResponse = "{}"; JsonObject gameFinderDrillDownJson = new
	 * JsonObject(); JsonObject mc = new JsonObject();
	 * 
	 * if (gameScheduleId != null) {
	 * 
	 * try { JsonArray currGameDocs = getGameForGameId(gameScheduleId); //
	 * System.out.println("game_info:" + currGameDocs.toString());
	 * mc.add("game_info", currGameDocs);
	 * 
	 * JsonArray liveGameInfoRespJsonArr = getLiveGamesById(gameScheduleId); //
	 * System.out.println("live_info:" + currGameDocs.toString());
	 * mc.add("live_info", liveGameInfoRespJsonArr);
	 * 
	 * gameFinderDrillDownJson.add("mc", mc);
	 * 
	 * finalResponse = gameFinderDrillDownJson.toString();
	 * 
	 * } catch (Exception e) { LOGGER.error("Error occurred in parsing json",
	 * e); }
	 * 
	 * } else if (gameScheduleId == null) {
	 * 
	 * finalResponse = "{}"; // System.out.println("game_info:" +
	 * finalResponse);
	 * 
	 * }
	 * 
	 * return finalResponse; }
	 */

	private Map<String, JsonObject> getLiveGamesById(String gameId) {

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(String.format("Getting date for %s", gameId));
		}
		JsonArray currGameDocs = null;
		JsonElement currentGameRespJson = SportsDataGamesFacade$.MODULE$.getLiveGameById(gameId);

		JsonArray groupedDocs = currentGameRespJson.getAsJsonObject().get("aggregations").getAsJsonObject()
				.get("top_tags").getAsJsonObject().get("buckets").getAsJsonArray();

		// get game live info
		JsonObject liveInfo = groupedDocs.get(0).getAsJsonObject();

		if (liveInfo != null&&liveInfo.size()>0) {
			currGameDocs = liveInfo.get("live_info").getAsJsonObject().get("hits").getAsJsonObject().get("hits")
					.getAsJsonArray();
		} else {
			currGameDocs = new JsonArray();
		}

		Map<String, JsonObject> liveJsonObjects = new HashMap<>();
		currGameDocs.forEach(it -> {
			JsonObject liveJsonObject = it.getAsJsonObject().get("_source").getAsJsonObject();
			liveJsonObjects.put(liveJsonObject.get("gameId").getAsString(), liveJsonObject);

		});

		// System.out.println(currGameDocs.toString());

		return liveJsonObjects;

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

		// System.out.println(currGameDocs.toString());

		return currGameDocs;
	}

	private String transformToClientFormatForCategories(String finalResponse, String tz, String startDate,
			String endDate, String serverHost) {
		String output = "{}";
		List<Ribbon> ribbons = new ArrayList<Ribbon>();

		Ribbons ribbonsObj = new Ribbons();
		ribbonsObj.setRibbons(ribbons);

		List<String> categories = JsonPath.using(jsonPathConf).parse(finalResponse).read("$.games_categories[*]");

		if (categories != null && categories.size() > 0) {
			ListIterator<String> litr = categories.listIterator();
			while (litr.hasNext()) {
				String category = litr.next();

				Ribbon ribbon = new Ribbon();
				ribbon.setTitle(getNewCategoryName(category));
				ribbon.setExpiresAt(new DateTime().plusSeconds(30).toDateTime(DateTimeZone.UTC).toString());

				String uri = "http://" + serverHost + "/api/slingtv/airtv/v1/games/" + category.toLowerCase() + "?";
				ribbon.setHref(uri + "tz=" + tz + "&startDate=" + startDate + "&endDate=" + endDate);

				ribbon.setTiles(new ArrayList<TileAsset>());

				ribbons.add(ribbon);

			}

			Gson gson = new GsonBuilder().disableHtmlEscaping().create();
			String jsonOutput = gson.toJson(ribbons);

			JsonElement jsonElement = gson.fromJson(jsonOutput, JsonElement.class);

			JsonObject jsonObj = new JsonObject();
			jsonObj.add("ribbons", jsonElement);
			output = jsonObj.toString();

		}
		return output;
	}

	private String transformToClientFormatForGames(String finalResponse, String category, String tz, String startDate,
			String endDate, String serverHost) {
		String output = "{}";
		List<TileAsset> tiles = new ArrayList<TileAsset>();
		Ribbon ribbon = new Ribbon();

		List<Map<String, Object>> games = JsonPath.using(jsonPathConf).parse(finalResponse).read("$.[*]");

		if (games != null && games.size() > 0) {
			ListIterator<Map<String, Object>> litr = games.listIterator();
			while (litr.hasNext()) {

				while (litr.hasNext()) {
					Map<String, Object> map = litr.next();

					SportsTileAsset tile = new SportsTileAsset();

					List<Object> contentIds = (List<Object>) map.get("contentId");
					if (contentIds != null && contentIds.size() > 0) {
						String contentId = (String) contentIds.get(0);
						tile.setId(contentId);
					}
					tile.setType("show");

					String title = ((Map<String, Object>) map.get("awayTeam")).get("name") + " at "
							+ ((Map<String, Object>) map.get("homeTeam")).get("name");

					tile.setTitle(title);

					tile.setRatings(new ArrayList<String>());

					String gameId = (String) map.get("id");
					String scheduleDate = (String) map.get("scheduledDate");
					tile.setHref("http://" + serverHost + "/api/slingtv/airtv/v1/game/" + category.toLowerCase() + "/"
							+ gameId + "/" + scheduleDate + " " + title);

					Thumbnail thumbnail = new Thumbnail();
					String sport = (String) map.get("sport");
					String categoryNameForLogo = "";
					if (category.equalsIgnoreCase("NCAAF")) {
						categoryNameForLogo = "football";
					} else {
						categoryNameForLogo = getNewCategoryName(category).toLowerCase();
					}
					thumbnail.setmUrl(
							"http://qahsports.slingbox.com/gf2client-1.7-e/img/bg/" + categoryNameForLogo + ".png");
					thumbnail.setmWidth(288);
					thumbnail.setmHeight(258);
					tile.setThumbnail(thumbnail);

					Integer startTimeEpoch = (Integer) map.get("startTimeEpoch");
					Integer stopTimeEpoch = (Integer) map.get("stopTimeEpoch");
					tile.setDuration((stopTimeEpoch - startTimeEpoch));

					String startTimeText = new DateTime(Long.valueOf(startTimeEpoch)).toDateTime(DateTimeZone.UTC)
							.toString();
					String stopTimeText = new DateTime(Long.valueOf(stopTimeEpoch)).toDateTime(DateTimeZone.UTC)
							.toString();

					tile.setStartTime(startTimeText);
					tile.setStopTime(stopTimeText);

					Channel channel = new Channel();
					channel.setGuid((String) map.get("channelGuid"));
					channel.setTitle((String) map.get("callsign"));
					channel.setType("channel");
					tile.setChannel(channel);

					tile.setSport(sport);
					tile.setLeague((String) map.get("league"));
					tile.setTeaser((String) map.get("teaser"));
					tile.setGameStatus((String) map.get("gameStatus"));
					tile.setGameId(gameId);

					SportTeam homeTeam = new SportTeam();
					Map<String, Object> homeJson = (Map<String, Object>) map.get("homeTeam");
					homeTeam.setAlias((String) homeJson.get("alias"));
					homeTeam.setHomeCity((String) homeJson.get("city"));
					homeTeam.setId((String) homeJson.get("id"));
					homeTeam.setName((String) homeJson.get("name"));
					Thumbnail homeLogo = new Thumbnail();

					String homeImage = (String) homeJson.get("img");
					homeImage = homeImage.replace("baseball", categoryNameForLogo);
					homeImage=homeImage.replace("mlb", category.toLowerCase());

					homeLogo.setmUrl(homeImage);
					homeLogo.setmWidth(64);
					homeLogo.setmHeight(48);
					homeTeam.setImg(homeLogo);
					tile.setHomeTeam(homeTeam);

					SportTeam awayTeam = new SportTeam();
					Map<String, Object> awayJson = (Map<String, Object>) map.get("awayTeam");
					awayTeam.setAlias((String) awayJson.get("alias"));
					awayTeam.setHomeCity((String) awayJson.get("city"));
					awayTeam.setId((String) awayJson.get("id"));
					awayTeam.setName((String) awayJson.get("name"));
					Thumbnail awayLogo = new Thumbnail();
					String awayImage = (String) awayJson.get("img");
					awayImage = awayImage.replace("baseball", categoryNameForLogo);
					awayImage=awayImage.replace("mlb", category.toLowerCase());

					awayLogo.setmUrl(awayImage);
					awayLogo.setmWidth(64);
					awayLogo.setmHeight(48);
					awayTeam.setImg(awayLogo);
					tile.setAwayTeam(awayTeam);

					GameStats gamestats = new GameStats();
					ThuuzStats thuuz = new ThuuzStats();
					thuuz.setRating((String) map.get("rating"));
					gamestats.setThuuz(thuuz);
					NagraStats nstats = new NagraStats();
					String homeScore = (String) map.get("homeScore");
					String awayScore = (String) map.get("awayScore");

					if (homeScore != null) {
						nstats.setHomeScore(Integer.parseInt(homeScore));
					}
					if (awayScore != null) {
						nstats.setAwayScore(Integer.parseInt(awayScore));
					}
					gamestats.setNstats(nstats);
					tile.setGamestats(gamestats);

					tiles.add(tile);
				}

				ribbon.setTitle("Games");
				ribbon.setExpiresAt(new DateTime().plusSeconds(30).toDateTime(DateTimeZone.UTC).toString());

				String uri = "http://" + serverHost + "/api/slingtv/airtv/v1/games/" + category.toLowerCase() + "?";

				ribbon.setHref(uri + "tz=" + tz + "&startDate=" + startDate + "&endDate=" + endDate);

				ribbon.setTiles(tiles);
			}

			Gson gson = new GsonBuilder().disableHtmlEscaping().create();
			output = gson.toJson(ribbon);

		}
		return output;
	}

	private String transformToClientFormatForMediaCard(String finalResponse, String category, String serverHost) {
		String output = "{}";
		MediaCard mediaCard = null;

		List<Map<String, Object>> games = JsonPath.using(jsonPathConf).parse(finalResponse).read("$.[*]");

		if (games != null && games.size() > 0) {
			ListIterator<Map<String, Object>> litr = games.listIterator();
			while (litr.hasNext()) {

				while (litr.hasNext()) {
					Map<String, Object> map = litr.next();

					SportsMediaCard tile = new SportsMediaCard();

					List<Object> contentIds = (List<Object>) map.get("contentId");
					if (contentIds != null && contentIds.size() > 0) {
						String contentId = (String) contentIds.get(0);
						tile.setId(contentId);
					}
					String sport = (String) map.get("sport");
					
					String categoryNameForLogo = "";
					if (category.equalsIgnoreCase("NCAAF")) {
						categoryNameForLogo = "football";
					} else {
						categoryNameForLogo = getNewCategoryName(category).toLowerCase();
					}

					tile.setSport(sport);
					tile.setLeague((String) map.get("league"));

					Integer startTimeEpoch = (Integer) map.get("startTimeEpoch");
					Integer stopTimeEpoch = (Integer) map.get("stopTimeEpoch");
					tile.setDuration((stopTimeEpoch - startTimeEpoch));

					String startTimeText = new DateTime(Long.valueOf(startTimeEpoch)).toDateTime(DateTimeZone.UTC)
							.toString();
					String stopTimeText = new DateTime(Long.valueOf(stopTimeEpoch)).toDateTime(DateTimeZone.UTC)
							.toString();

					tile.setStartTime(startTimeText);
					tile.setStopTime(stopTimeText);

					tile.setGameStatus((String) map.get("gameStatus"));
					tile.setAnons((String) map.get("anons"));
					tile.setAnons_title((String) map.get("anons_title"));
					tile.setLocation((String) map.get("location"));
					String gameId = (String) map.get("id");
					tile.setGameId(gameId);

					SportTeam homeTeam = new SportTeam();
					Map<String, Object> homeJson = (Map<String, Object>) map.get("homeTeam");
					homeTeam.setAlias((String) homeJson.get("alias"));
					homeTeam.setHomeCity((String) homeJson.get("city"));
					homeTeam.setId((String) homeJson.get("id"));
					homeTeam.setName((String) homeJson.get("name"));
					Thumbnail homeLogo = new Thumbnail();

					String homeImage = (String) homeJson.get("img");
					homeImage = homeImage.replace("baseball", categoryNameForLogo);
					homeImage=homeImage.replace("mlb", category.toLowerCase());

					homeLogo.setmUrl(homeImage);
					homeLogo.setmWidth(64);
					homeLogo.setmHeight(48);
					homeTeam.setImg(homeLogo);
					tile.setHomeTeam(homeTeam);

					SportTeam awayTeam = new SportTeam();
					Map<String, Object> awayJson = (Map<String, Object>) map.get("awayTeam");
					awayTeam.setAlias((String) awayJson.get("alias"));
					awayTeam.setHomeCity((String) awayJson.get("city"));
					awayTeam.setId((String) awayJson.get("id"));
					awayTeam.setName((String) awayJson.get("name"));
					Thumbnail awayLogo = new Thumbnail();
					String awayImage = (String) awayJson.get("img");
					awayImage = awayImage.replace("baseball", categoryNameForLogo);
					awayImage=awayImage.replace("mlb", category.toLowerCase());

					awayLogo.setmUrl(awayImage);
					awayLogo.setmWidth(64);
					awayLogo.setmHeight(48);
					awayTeam.setImg(awayLogo);
					tile.setAwayTeam(awayTeam);

					GameStats gamestats = new GameStats();
					ThuuzStats thuuz = new ThuuzStats();
					thuuz.setRating((String) map.get("rating"));
					gamestats.setThuuz(thuuz);
					NagraStats nstats = new NagraStats();
					String homeScore = (String) map.get("homeScore");
					String awayScore = (String) map.get("awayScore");

					if (homeScore != null) {
						nstats.setHomeScore(Integer.parseInt(homeScore));
					}
					if (awayScore != null) {
						nstats.setAwayScore(Integer.parseInt(awayScore));
					}
					gamestats.setNstats(nstats);
					tile.setGamestats(gamestats);
					mediaCard = tile;
				}

			}

			Gson gson = new GsonBuilder().disableHtmlEscaping().create();
			output = gson.toJson(mediaCard);

		}
		return output;
	}

}
