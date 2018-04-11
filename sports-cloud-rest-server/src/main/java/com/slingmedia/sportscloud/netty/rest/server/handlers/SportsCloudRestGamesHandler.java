/*
 * SportsCloudRestGamesHandler.java
 * @author jayachandra
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
package com.slingmedia.sportscloud.netty.rest.server.handlers;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.*;
import com.slingmedia.sportscloud.facade.ExternalHttpClient$;
import com.slingmedia.sportscloud.netty.rest.model.ActiveTeamGame;
import com.slingmedia.sportscloud.netty.rest.server.handler.delegates.SportsCloudMCDelegate;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
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
import com.slingmedia.sportscloud.netty.rest.server.handler.delegates.SportsCloudGamesDelegate;

/**
 * Handler service for Sling TV Sports screen
 *
 * @author jayachandra
 * @version 1.0
 * @since 1.0
 */
public class SportsCloudRestGamesHandler {

	/** The Constant LOGGER. */
	public static final Logger LOGGER = LoggerFactory.getLogger(SportsCloudRestGamesHandler.class);

	/** The Regular expression for Categories URL */
	private static final String REGEX_CATEGORIES_URL = "^/api/slingtv/airtv/v1/games\\?(.+)";

	/** The Regular expression for games URL */
	private static final String REGEX_GAMES_URL = "^/api/slingtv/airtv/v1/games/(.[^/]+)\\?(.+)";

	/** The Regular expression for media card URL */
	private static final String REGEX_MC_URL = "^/api/slingtv/airtv/v1/game/(.[^/]+)/(.[^/]+)/(.[^/]+)";

	/** The URL pattern for categories */
	private static final Pattern REGEX_CATEGORIES_URL_PATTERN = Pattern.compile(REGEX_CATEGORIES_URL);

	/** The URL pattern for games */
	private static final Pattern REGEX_GAMES_URL_PATTERN = Pattern.compile(REGEX_GAMES_URL);

	/** The URL pattern for media card */
	private static final Pattern REGEX_MC_URL_PATTERN = Pattern.compile(REGEX_MC_URL);

	/** Provides APIs for finding key value for given JSON path */
	private static Configuration jsonPathConf = Configuration.defaultConfiguration();

	/** The Date formatter for the date format YYYY-MM-dd Z */
	private DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("YYYY-MM-dd Z").withLocale(Locale.US);

	/** Delegate Service for Sling TV Sports */
	private SportsCloudGamesDelegate sportsCloudGamesDelegate = new SportsCloudGamesDelegate();

    private SportsCloudMCDelegate sportsCloudMCDelegate = new SportsCloudMCDelegate();


    private static final String nagaraTeamList = "http://gwserv-mobileprod.echodata.tv/Gamefinder/api/team/list?leagueAlias=";

    private ConcurrentHashMap<String, JsonArray> leagueTeams = new ConcurrentHashMap<>();

	/**
	 * Rest service names
	 * 
	 * @author jayachandra
	 *
	 */
	private enum RestName {
		CATEGORIES, GAMES, MC, NONE
	}

	/**
	 * Returns the Sports Category name for given league
	 * 
	 * @param league
	 *            the Sports league name
	 * @return the Sports category for given league
	 */
	private String getNewCategoryName(String league) {
		String newCategory = "";

		switch (league.toUpperCase()) {
		case "MLB":
			newCategory = "Baseball";
			break;

		case "NCAAF":
			newCategory = "College Football";
			break;

		case "NFL":
			newCategory = "Football";
			break;

		case "NBA":
			newCategory = "NBA Basketball";
			break;
			
		case "NCAAB":
			newCategory = "College Basketball";
			break;

		case "SOCCER":
			newCategory = "Soccer";
			break;

        case "EPL":
            newCategory = "Soccer - EPL";
            break;

        case "KNVB":
            newCategory = "Soccer - KNVB";
            break;

        case "SERIEA":
            newCategory = "Soccer - SERIEA";
            break;

        case "BUND":
            newCategory = "Soccer - BUND";
            break;

        case "LALIGA":
            newCategory = "Soccer - LALIGA";
            break;

        case "NHL":
			newCategory = "Hockey";
			break;

		default:
			newCategory = "other";
			break;

		}

		return newCategory;
	}

    /**
     * This method fetches the images of the teams that nagara has.
     * @param league
     * @param teamAlias
     * @return Image Url
     */
    public String buildImagesFromNagara(String league, String teamAlias) {
        JsonArray leagueTeamArr = new JsonArray();
        league = league.toLowerCase();
        if(!leagueTeams.containsKey(league)) {
            ExternalHttpClient$.MODULE$.init();
            StringBuilder builder = new StringBuilder();

            builder.append(nagaraTeamList+""+league.toLowerCase());
            JsonParser parser = new JsonParser();
            String responseString = ExternalHttpClient$.MODULE$.getFromUrl(builder.toString());
            JsonElement responseJson = parser.parse("{}");
            try {
                responseJson = parser.parse(responseString);
                leagueTeams.putIfAbsent(league, responseJson.getAsJsonArray());
                //leagueTeamArr
            } catch (Exception e) {
                LOGGER.error("error " + e);
            }
        }
        leagueTeamArr = leagueTeams.get(league);
        String img = "";

        for (JsonElement leagueObj :leagueTeamArr){
            if(leagueObj.getAsJsonObject().has("alias")) {
                String alias = leagueObj.getAsJsonObject().get("alias").getAsString();
                if (alias.equals(teamAlias)) {
                    if (leagueObj.getAsJsonObject().has("img")) {
                        img = leagueObj.getAsJsonObject().get("img").getAsString();
                        //img= img.replace("MEDIUM","LARGE");
                    }
                    break;
                }
            }
        }
        return img;
    }


    /**
	 * Handles Sling TV REST services
	 * 
	 * @param host
	 *            the server host
	 * @param uri
	 *            the REST uri
	 * @param params
	 *            the URL parameters
	 * @return the JSON Response model for Sling TV REST services
	 */
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
				gameCategory = "-";
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
				gameCategory = "-";
				gameId = "-";

			}
			finalResponse = handleGameMediaCard(params, gameCategory, gameId, host);
			break;
		default:
			LOGGER.info("This is NONE " + uri);
			finalResponse = "{}";

		}

		return finalResponse;
	}

	/**
	 * Handles Request and Response for Categories
	 * 
	 * @param serverHost
	 *            the server host
	 * @param params
	 *            the URL parameters
	 * @return the JSON Response for Categories
	 */
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

		finalResponse = sportsCloudGamesDelegate.prepareGamesCategoriesDataForHomeScreen(finalResponse, startDate,
				endDate);

		String output = transformToClientFormatForCategories(finalResponse, params.get("tz").get(0),
				params.get("startDate").get(0), params.get("endDate").get(0), serverHost);

		return output;
	}

	/**
	 * Handles Request and Response for games list
	 * 
	 * @param serverHost
	 *            the server host
	 * @param params
	 *            the URL parameters
	 * @param gameCategory
	 *            the game category
	 * @return the JSON Response for Categories
	 */
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

		finalResponse = sportsCloudGamesDelegate.prepareGameScheduleDataForCategoryForHomeScreen(finalResponse,
				startDate, endDate, gameCategory);

		String output = transformToClientFormatForGames(finalResponse, gameCategory, params.get("tz").get(0),
				params.get("startDate").get(0), params.get("endDate").get(0), serverHost);

		return output;
	}

	/**
	 * Handles Request and Response for game media card
	 * 
	 * @param params
	 *            the URL parameters
	 * @param gameCategory
	 *            the game category
	 * @param gameId
	 *            the game id
	 * @param serverHost
	 *            the server host
	 * @return the JSON Response for media card
	 */
	private String handleGameMediaCard(Map<String, List<String>> params, String gameCategory, String gameId,
			String serverHost) {
		String finalResponse = null;

		finalResponse = sportsCloudGamesDelegate.prepareJsonResponseMCForGame(gameId, gameCategory,
				new HashSet<String>());
		String output = transformToClientFormatForMediaCard(finalResponse, gameCategory, serverHost);
		JsonParser parser = new JsonParser();
		JsonObject parsedJsonObj = parser.parse(output).getAsJsonObject();

        JsonArray homeScreenGameScheduleGroup = sportsCloudMCDelegate.getGameForGameId(gameId);
        JsonObject solrDoc = sportsCloudMCDelegate.getMatchedGame(new JsonObject(), homeScreenGameScheduleGroup);
        final ActiveTeamGame activeTeamGame = sportsCloudMCDelegate.getActiveTeamGame(
                solrDoc.get("awayTeamExternalId").getAsString(),
                homeScreenGameScheduleGroup);

		String sport = "-";
		if(solrDoc.has("sport")) {
			sport = solrDoc.get("sport").getAsString().toLowerCase();
		}
		if("soccer".equalsIgnoreCase(sport)) {
			LOGGER.trace("No teamstandings or playerstats for soccer league");
		} else {

			JsonObject teamStatsObj = new JsonObject();
			parsedJsonObj.add("teamStats", teamStatsObj);
			sportsCloudMCDelegate.preparePlayerStats(
					solrDoc.get("homeTeamExternalId").getAsString(),
					solrDoc.get("awayTeamExternalId").getAsString(),
					teamStatsObj);
			JsonObject standings = new JsonObject();
			parsedJsonObj.add("standings", standings);

			sportsCloudMCDelegate.prepareMCTeamStandings(
					activeTeamGame,
					standings,
					solrDoc.get("league").getAsString().toLowerCase());
		}
        sportsCloudMCDelegate.mergeLiveInfoToMediaCard(activeTeamGame,parsedJsonObj,solrDoc,new JsonObject());
        return parsedJsonObj.toString();
	}

	/**
	 * Validates the URI
	 * 
	 * @param uri
	 *            the uri
	 * @param pattern
	 *            the uri pattern
	 * @return the true for valid uri and false for invalid uri
	 */
	private Boolean isValid(String uri, Pattern pattern) {
		return pattern.matcher(uri).matches();
	}

	/**
	 * Validates the URL is supported or not
	 * 
	 * @param uri
	 *            the URI
	 * @return the valid REST service name
	 */
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

	/**
	 * The main method for testing individual methods
	 * 
	 * @param args
	 *            the main arguments
	 */
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

	/**
	 * Transforms the JSON to Sling TV specific response for categories
	 * 
	 * @param finalResponse
	 *            the source JSON string
	 * @param tz
	 *            the time zone
	 * @param startDate
	 *            the schedule start date
	 * @param endDate
	 *            the schedule end date
	 * @param serverHost
	 *            the server host
	 * @return the JSON response for categories
	 */
	private String transformToClientFormatForCategories(String finalResponse, String tz, String startDate,
			String endDate, String serverHost) {
		String output = "{}";
		List<Ribbon> ribbons = new ArrayList<Ribbon>();

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

	/**
	 * Transforms the JSON to Sling TV specific response for games list
	 * 
	 * @param finalResponse
	 *            the source JSON string
	 * @param category
	 *            the sports category
	 * @param tz
	 *            the time zone
	 * @param startDate
	 *            the schedule start date
	 * @param endDate
	 *            the schedule end date
	 * @param serverHost
	 *            the server host
	 * @return the JSON response for games list
	 */
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

					tile.setType("sportsv2");

					String title = (String) map.get("anons_title");
					tile.setTitle(title);

					tile.setRatings((List<String>) map.get("ratings"));

					String gameId = (String) map.get("id");
					String scheduleDate = (String) map.get("scheduledDate");
					tile.setHref("http://" + serverHost + "/api/slingtv/airtv/v1/game/" + category.toLowerCase() + "/"
							+ gameId + "/" + scheduleDate + " " + title);

					Thumbnail thumbnail = new Thumbnail();
					String sport = (String) map.get("sport");
					String categoryNameForLogo = "";
					if (category.equalsIgnoreCase("NCAAF")) {
						categoryNameForLogo = "football";
					} else if (category.equalsIgnoreCase("NCAAB")) {
						categoryNameForLogo = "basketball";
					}
					else if (category.equalsIgnoreCase("NBA")) {
						categoryNameForLogo = "basketball";
					} else if(sport.equalsIgnoreCase("soccer")) {
                        categoryNameForLogo = "soccer";
                    }
					else {
						categoryNameForLogo = getNewCategoryName(category.toUpperCase()).toLowerCase();
					}
					thumbnail.setmUrl(
							"http://qahsports.slingbox.com/gf2client-1.7-e/img/bg/" + categoryNameForLogo + ".png");


					thumbnail.setmWidth(288);
					thumbnail.setmHeight(258);
					tile.setThumbnail(thumbnail);

					Integer startTimeEpoch = (Integer) map.get("startTimeEpoch");
					Integer stopTimeEpoch = (Integer) map.get("stopTimeEpoch");
					tile.setDuration((stopTimeEpoch - startTimeEpoch));

					String startTimeText = new DateTime(startTimeEpoch * 1000L).toDateTime(DateTimeZone.UTC).toString();
					String stopTimeText = new DateTime(stopTimeEpoch * 1000L).toDateTime(DateTimeZone.UTC).toString();

					tile.setStartTime(startTimeText);
					tile.setStopTime(stopTimeText);

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
					homeImage = homeImage.replace("mlb", category.toLowerCase());
                    // set the images for soccer and nhl
                    if(sport.equalsIgnoreCase("soccer") || category.equalsIgnoreCase("NHL") ) {
                        homeImage = buildImagesFromNagara(category.toLowerCase(), (String) homeJson.get("alias"));
                    }
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
					awayImage = awayImage.replace("mlb", category.toLowerCase());

                    if(sport.equalsIgnoreCase("soccer") || "NHL".equalsIgnoreCase(category)) {
                        awayImage = buildImagesFromNagara(category.toLowerCase(), (String) awayJson.get("alias"));
                    }
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

					List<String> contentIds = (List<String>) map.get("contentId");
					Map<String, Object> cIdToAsstInfo = (Map<String, Object>) map.get("cIdToAsstInfo");
					if (contentIds != null && contentIds.size() > 0) {
						ListIterator<String> litrContentIds = contentIds.listIterator();
						while (litrContentIds.hasNext()) {
							TileAsset newTile = tile;
							String contentId = litrContentIds.next();
							try {
								newTile = (TileAsset) tile.clone();

								Map<String, Object> channelInfo = (Map<String, Object>) cIdToAsstInfo.get(contentId);
								if (channelInfo != null) {
									newTile.setId((String) channelInfo.get("assetGuid"));

									Channel channel = new Channel();
									channel.setGuid((String) channelInfo.get("channelGuid"));
									channel.setTitle((String) channelInfo.get("callsign"));
									channel.setType("channel");

									newTile.setChannel(channel);
								}

							} catch (CloneNotSupportedException e) {
								LOGGER.error(e.getMessage(), e);
							} finally {
								tiles.add(newTile);
							}
						}

					} else {
						tiles.add(tile);
					}

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

	/**
	 * Transforms the JSON to Sling TV specific response for game media card
	 * 
	 * @param finalResponse
	 *            the source JSON string
	 * @param category
	 *            the category
	 * @param serverHost
	 *            the server host
	 * @return the JSON response for media card
	 */
	private String transformToClientFormatForMediaCard(String finalResponse, String category, String serverHost) {
		String output = "{}";
		MediaCard mediaCard = null;

		List<Map<String, Object>> games = JsonPath.using(jsonPathConf).parse(finalResponse).read("$.[*]");

		if (games != null && games.size() > 0) {
			ListIterator<Map<String, Object>> litr = games.listIterator();
			while (litr.hasNext()) {

				while (litr.hasNext()) {
					Map<String, Object> map = litr.next();

					SportsMediaCard mc = new SportsMediaCard();

					String gameId = (String) map.get("id");
					mc.setId(gameId);

					String sport = (String) map.get("sport");

					String categoryNameForLogo = "";
					if (category.equalsIgnoreCase("NCAAF")) {
						categoryNameForLogo = "football";
					} else if (category.equalsIgnoreCase("NCAAB")) {
						categoryNameForLogo = "basketball";
					}
					else if (category.equalsIgnoreCase("NBA")) {
						categoryNameForLogo = "basketball";
					}
					else {
						categoryNameForLogo = getNewCategoryName(category.toUpperCase()).toLowerCase();
					}

					mc.setSport(sport);
					mc.setLeague((String) map.get("league"));

					Integer startTimeEpoch = (Integer) map.get("startTimeEpoch");
					Integer stopTimeEpoch = (Integer) map.get("stopTimeEpoch");
					mc.setDuration((stopTimeEpoch - startTimeEpoch));

					String startTimeText = new DateTime(startTimeEpoch * 1000L).toDateTime(DateTimeZone.UTC).toString();
					String stopTimeText = new DateTime(stopTimeEpoch * 1000L).toDateTime(DateTimeZone.UTC).toString();

					mc.setStartTime(startTimeText);
					mc.setStopTime(stopTimeText);

					mc.setGameStatus((String) map.get("gameStatus"));
					mc.setAnons((String) map.get("anons"));
					mc.setAnons_title((String) map.get("anons_title"));
					mc.setLocation((String) map.get("location"));

					SportTeam homeTeam = new SportTeam();
					Map<String, Object> homeJson = (Map<String, Object>) map.get("homeTeam");
					homeTeam.setAlias((String) homeJson.get("alias"));
					homeTeam.setHomeCity((String) homeJson.get("city"));
					homeTeam.setId((String) homeJson.get("id"));
					homeTeam.setName((String) homeJson.get("name"));
					Thumbnail homeLogo = new Thumbnail();

					String homeImage = (String) homeJson.get("img");
					homeImage = homeImage.replace("baseball", categoryNameForLogo);
					homeImage = homeImage.replace("mlb", category.toLowerCase());

					// set the images for soccer and nhl
                    if(sport.equalsIgnoreCase("soccer") || category.equalsIgnoreCase("NHL") ) {
                        homeImage = buildImagesFromNagara(category.toLowerCase(), (String) homeJson.get("alias"));
                    }
					homeLogo.setmUrl(homeImage);
					homeLogo.setmWidth(64);
					homeLogo.setmHeight(48);
					homeTeam.setImg(homeLogo);
					mc.setHomeTeam(homeTeam);

					SportTeam awayTeam = new SportTeam();
					Map<String, Object> awayJson = (Map<String, Object>) map.get("awayTeam");
					awayTeam.setAlias((String) awayJson.get("alias"));
					awayTeam.setHomeCity((String) awayJson.get("city"));
					awayTeam.setId((String) awayJson.get("id"));
					awayTeam.setName((String) awayJson.get("name"));
					Thumbnail awayLogo = new Thumbnail();
					String awayImage = (String) awayJson.get("img");
					awayImage = awayImage.replace("baseball", categoryNameForLogo);
					awayImage = awayImage.replace("mlb", category.toLowerCase());

                    if(sport.equalsIgnoreCase("soccer") || "NHL".equalsIgnoreCase(category)) {
                        awayImage = buildImagesFromNagara(category.toLowerCase(), (String) awayJson.get("alias"));
                    }

					awayLogo.setmUrl(awayImage);
					awayLogo.setmWidth(64);
					awayLogo.setmHeight(48);
					awayTeam.setImg(awayLogo);
					mc.setAwayTeam(awayTeam);

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
					mc.setGamestats(gamestats);
					List<String> contentIds = (List<String>) map.get("contentId");
					// For slingtv native, update externel_id (assetGuid) as
					// contentId.

					List<String> assetGuids = new ArrayList<String>();

					Map<String, Object> cIdToAsstInfo = (Map<String, Object>) map.get("cIdToAsstInfo");
					if (contentIds != null && contentIds.size() > 0) {
						ListIterator<String> litrContentIds = contentIds.listIterator();
						while (litrContentIds.hasNext()) {

							String contentId = litrContentIds.next();

							Map<String, Object> channelInfo = (Map<String, Object>) cIdToAsstInfo.get(contentId);
							if (channelInfo != null) {
								assetGuids.add((String) channelInfo.get("assetGuid"));
							}

						}

					}

					mc.setContentId(assetGuids);

					mediaCard = mc;
				}

			}

			Gson gson = new GsonBuilder().disableHtmlEscaping().create();
			output = gson.toJson(mediaCard);

		}
		return output;
	}

}
