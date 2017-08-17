/*
 * GenericJsonProxyDecoder.java
 * @author arung
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
package com.slingmedia.sportscloud.tests.fixtures.servers.handlers;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.slingmedia.sportscloud.tests.facade.*;
import com.slingmedia.sportscloud.tests.fixtures.servers.config.JsonProxyServerConfiguration;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;

/**
 * The Class GenericJsonProxyDecoder.
 *
 * @author arung
 */
public class GenericJsonProxyDecoder extends SimpleChannelInboundHandler<FullHttpRequest> {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(GenericJsonProxyDecoder.class);

	private DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("YYYY-MM-dd Z").withLocale(Locale.US);

	private String solrHost = System.getProperty("solrHost") == null ? "cqaneat02.sling.com": System.getProperty("solrHost");
	private String solrPort =System.getProperty("solrPort")==null? "8983" : System.getProperty("solrPort");
	
	GenericJsonProxyDecoder() {

	}

	enum Role {
		HOME, AWAY, NONE
	}

	enum League {
		MLB
	}

	enum GameAvailability {
		AVAILABLE, UNAVAILABLE
	}

	private static final int[] FIELD_STATE_MAP = new int[] { 0, 1, 2, 4, 3, 7, 5, 6 };

	enum GameStatus {

		UPCOMING, COMPLETED, IN_PROGRESS, POSTPONED, DELAYED, NONE;

		public static GameStatus getValue(int statusId) {

			if (statusId == 1)
				return UPCOMING;
			else if (statusId == 2)
				return IN_PROGRESS;
			else if (statusId == 4)
				return COMPLETED;
			else if (statusId == 5)
				return POSTPONED;
			else if (statusId == 23)
				return DELAYED;
			else
				return NONE;

		}

	}

	enum GameType {
		REGULAR_SEASON("Regular Season"), UNKNOWN("uknown");
		GameType(String gts){
			gameTypeStr = gts;
		}
		private String gameTypeStr; 
		public static GameType getValue(String gameType) {
			if (gameType.equalsIgnoreCase("regular season"))
				return REGULAR_SEASON;
			else
				return UNKNOWN;
		}
		public String getGameTypeStr() {
			return gameTypeStr;
		}
		public void setGameTypeStr(String gameTypeStr) {
			this.gameTypeStr = gameTypeStr;
		}

	}

	class ActiveTeamGame {
		private String gameId;
		private Role activeTeamRole;
		private String activeTeamId;
		private String homeTeamId;
		private String awayTeamId;
		private String homeTeamName;
		private String awayTeamName;
		private GameType gameType;

		ActiveTeamGame(String gameId, GameType gameType, String activeTeamId, String homeTeamId, String awayTeamId, String homeTeamName,
				String awayTeamName, Role role) {
			this.setGameType(gameType);
			this.setHomeTeamName(homeTeamName);
			this.setAwayTeamName(awayTeamName);
			this.setActiveTeamId(activeTeamId);
			this.setHomeTeamId(homeTeamId);
			this.setAwayTeamId(awayTeamId);
			this.setGameId(gameId);
			this.setActiveTeamRole(role);
		}

		public String getGameId() {
			return gameId;
		}

		public void setGameId(String gameId) {
			this.gameId = gameId;
		}

		public Role getActiveTeamRole() {
			return activeTeamRole;
		}

		public void setActiveTeamRole(Role role) {
			this.activeTeamRole = role;
		}

		public String getHomeTeamId() {
			return homeTeamId;
		}

		public void setHomeTeamId(String homeTeamId) {
			this.homeTeamId = homeTeamId;
		}

		public String getAwayTeamId() {
			return awayTeamId;
		}

		public void setAwayTeamId(String awayTeamId) {
			this.awayTeamId = awayTeamId;
		}

		public String getActiveTeamId() {
			return activeTeamId;
		}

		public void setActiveTeamId(String activeTeamId) {
			this.activeTeamId = activeTeamId;
		}

		public String getHomeTeamName() {
			return homeTeamName;
		}

		public void setHomeTeamName(String homeTeamName) {
			this.homeTeamName = homeTeamName;
		}

		public String getAwayTeamName() {
			return awayTeamName;
		}

		public void setAwayTeamName(String awayTeamName) {
			this.awayTeamName = awayTeamName;
		}

		public GameType getGameType() {
			return gameType;
		}

		public void setGameType(GameType gameType) {
			this.gameType = gameType;
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.netty.channel.SimpleChannelInboundHandler#channelRead0(io.netty.
	 * channel.ChannelHandlerContext, java.lang.Object)
	 */
	@Override
	protected void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest request) throws Exception {
		// legacy(ctx, request);
		final boolean keepAlive = HttpHeaders.isKeepAlive(request);
		final ByteBuf buf = ctx.alloc().directBuffer();
		try {
			String finalResponse = "{}";

			HttpResponse response = null;
			response = new DefaultHttpResponse(request.protocolVersion(), HttpResponseStatus.OK);
			response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "application/json");
			response.headers().set(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN, "*");

			String uri = request.uri();
			if (uri.startsWith("/dish/v1/sport")) {
				QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.uri());
				Map<String, List<String>> params = queryStringDecoder.parameters();
				long startDate = Instant.now().getEpochSecond();
				if (params.get("startDate") != null) {
					startDate = dateTimeFormatter.parseDateTime(params.get("startDate").get(0)).getMillis() / 1000;
				}
				String endDate = "*";
				if (params.get("endDate") != null) {
					endDate = new StringBuilder()
							.append(dateTimeFormatter.parseDateTime(params.get("endDate").get(0)).getMillis() / 1000)
							.toString();
				}

				Set<String> subpackIds = getSubPackIdsFromParam(params);

				finalResponse = prepareGameScheduleDataForHomeScreen(finalResponse, startDate, endDate, subpackIds);
			} else if (uri.startsWith("/dish/v1/mc/mlb")) {
				try {
					QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.uri());
					Map<String, List<String>> params = queryStringDecoder.parameters();
					String gameScheduleId = null;
					if (params.get("gameId") != null) {
						gameScheduleId = params.get("gameId").get(0);
					}
					String teamId = params.get("teamId").get(0);
					Set<String> subpackIds = getSubPackIdsFromParam(params);

					finalResponse = prepareResponseForMC(gameScheduleId, teamId, subpackIds);
				} catch (Exception e) {
					LOGGER.error("Error occurred in parsing json", e);
				}

			}

			if (keepAlive) {
				response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
			} else {
				response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
			}
			byte[] bytes = finalResponse.getBytes();
			response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, bytes.length);

			// response.headers().set(HttpHeaders.Names.CONNECTION,
			// HttpHeaders.Values.CLOSE);

			ctx.write(response);

			buf.writeBytes(bytes);

		} catch (

		Exception e) {
			LOGGER.error("Error occurred during encoding", e);
		} finally {
			final ChannelFuture future = ctx.writeAndFlush(new DefaultLastHttpContent(buf));
			future.addListener(ChannelFutureListener.CLOSE);
		}
	}

	private String prepareResponseForMC(String gameScheduleId, String teamId, Set<String> subpackIds) {
		String finalResponse;
		JsonObject gameFinderDrillDownJson = new JsonObject();
		ActiveTeamGame activeGame = new ActiveTeamGame("0",null, "0", null, null, null, null, Role.NONE);
		if (gameScheduleId != null) {

			try {
				JsonArray currGameDocs = getGameForGameId(gameScheduleId);
				activeGame = getActiveTeamGame(teamId, currGameDocs);

			} catch (Exception e) {
				LOGGER.error("Error occurred in parsing json", e);
			}

		} else if (gameScheduleId == null) {

			if (teamId != null) {

				// http://"+solrHost+":8983/solr/game_schedule/select?q=game_date_epoch:[1501542300%20TO%20*]&sort=game_date_epoch%20desc&wt=json&rows=1
				StringBuilder teamIdRequestBuilder = new StringBuilder(
						"http://"+solrHost+":"+solrPort+"/solr/game_schedule/select");
				teamIdRequestBuilder.append("?q=awayTeamExternalId:").append(teamId).append("+OR+")
						.append("homeTeamExternalId:").append(teamId)
						.append("&sort=game_date_epoch%20desc&wt=json&rows=1");
				JsonElement teamIdResponse = getJsonObject(teamIdRequestBuilder);
				JsonArray teamDocs = teamIdResponse.getAsJsonObject().get("response").getAsJsonObject().get("docs").getAsJsonArray();
				activeGame = getActiveTeamGame(teamId, teamDocs);
				gameScheduleId = activeGame.getGameId();
			}

		}

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

		finalResponse = gameFinderDrillDownJson.toString();
		return finalResponse;
	}

	private String prepareGameScheduleDataForHomeScreen(String finalResponse, long startDate, String endDate,
			Set<String> subpackIds) {
		// create requests here
		StringBuilder requestURLBuilder = new StringBuilder(
				"http://"+solrHost+":"+solrPort+"/solr/game_schedule/select");

		requestURLBuilder.append("?q=game_date_epoch:[").append(startDate).append("%20TO%20").append(endDate)
				.append("]").append("&start=0&rows=500").append("&sort=game_date_epoch%20asc")
				.append("&group=true&group.field=gameCode&group.limit=10").append("&wt=json");
		JsonElement gameSchedulesJson = getJsonObject(requestURLBuilder);

		// &fq=%7B!collapse%20field=gameId%7D&expand=true&fl=homeTeamScore,awayTeamScore&expand.rows=100&wt=json
		Map<String, JsonObject> liveResponseJson = prepareLiveGameInfoData(startDate, endDate);

		if (gameSchedulesJson != null) {
			JsonArray allGames = new JsonArray();
			try {
				JsonArray groupedDocs = gameSchedulesJson.getAsJsonObject().get("grouped").getAsJsonObject()
						.get("gameCode").getAsJsonObject().get("groups").getAsJsonArray();
				for (JsonElement groupedDoc : groupedDocs) {
					JsonObject mainObj = new JsonObject();
					JsonObject solrDoc = null;
					// get a list of subpackage ids

					solrDoc = getSubscribedOrFirstGameSchedule(subpackIds, mainObj,
							groupedDoc.getAsJsonObject().get("doclist").getAsJsonObject().get("docs").getAsJsonArray());

					JsonObject gameScheduleJson = solrDoc.getAsJsonObject();
					String gameId = solrDoc.get("gameId").getAsString();

					updateScoreStatusFromLive(liveResponseJson, mainObj, gameId);
					mainObj.add("channelGuid", new JsonPrimitive(gameScheduleJson.get("channel_guid").getAsString()));
					mainObj.add("programGuid", new JsonPrimitive(gameScheduleJson.get("program_guid").getAsString()));
					mainObj.add("assetGuid", new JsonPrimitive(gameScheduleJson.get("asset_guid").getAsString()));
					mainObj.add("id", new JsonPrimitive(gameScheduleJson.get("id").getAsString()));
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

					JsonArray contentIds = createContentIds(gameScheduleJson);

					mainObj.add("contentId", contentIds);

					String callsign = "-";
					if(gameScheduleJson.has("callsign")){
						callsign = gameScheduleJson.get("callsign").getAsString();
					}
					mainObj.add("callsign", new JsonPrimitive(callsign));

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

	private void updateScoreStatusFromLive(Map<String, JsonObject> liveResponseJson, JsonObject mainObj,
			String gameId) {
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

	private Map<String, JsonObject> prepareLiveGameInfoData(long startDate, String endDate) {
		StringBuilder liveInfoRequestBuilder = new StringBuilder(
				"http://"+solrHost+":"+solrPort+"/solr/live_info/select");
		liveInfoRequestBuilder.append("?q=game_date_epoch:[").append(startDate).append("%20TO%20").append(endDate)
				.append("]").append("&fl=gameId,homeScoreRuns,awayScoreRuns,statusId")
				.append("&start=0&rows=500&wt=json");
		JsonElement liveResponseJson = getJsonObject(liveInfoRequestBuilder);
		Map<String, JsonObject> liveJsonObjects = new HashMap<>();
		liveResponseJson.getAsJsonObject().get("response").getAsJsonObject().get("docs").getAsJsonArray()
				.forEach(it -> {

					liveJsonObjects.put(it.getAsJsonObject().get("gameId").getAsString(), it.getAsJsonObject());

				});
		return liveJsonObjects;
	}

	private JsonObject getSubscribedOrFirstGameSchedule(Set<String> subpackIds, JsonObject sportDataItem,
			JsonArray gameScheduleArray) {

		List<JsonObject> foundItems = new ArrayList<>();
		gameScheduleArray.forEach(it -> {
			Set<String> intersectedSubPacks = new HashSet<String>();
			JsonObject item = it.getAsJsonObject();
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
			solrDoc = gameScheduleArray.get(0).getAsJsonObject();
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
				if(callsign.startsWith("CSNCH") || callsign.startsWith("ESPN") ){
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

	private Set<String> getSubPackIdsFromParam(Map<String, List<String>> params) {
		List<String> subPackIdsParam = params.get("sub_pack_ids");
		List<String> legacySubPackIds = params.get("legacy_sub_pack_ids");
		Set<String> finalSubpackIds = new HashSet<>();
		if (subPackIdsParam != null) {
			finalSubpackIds.addAll(Arrays.asList(subPackIdsParam.get(0).split(" ")));
		}
		if (legacySubPackIds != null) {
			finalSubpackIds.addAll(Arrays.asList(legacySubPackIds.get(0).split(" ")));

		}
		return finalSubpackIds;
	}

	private void prepareMCDrives(String gameId, String teamId, JsonArray scoringEvents) {
		StringBuilder scoringEvtReqBuilder = new StringBuilder();
		scoringEvtReqBuilder.append("http://"+solrHost+":"+solrPort+"/solr/scoring_events/select?indent=on&q=gameId:")
				.append(gameId).append("&sort=srcTime%20desc").append("&start=0&rows=100").append("&wt=json");
		JsonArray scoringEvtsResponse = getJsonObject(scoringEvtReqBuilder).getAsJsonObject().get("response")
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

	private void prepareMCTeamStandings(ActiveTeamGame activeGame, JsonObject standings, String league) {
		standings.add("league", new JsonPrimitive(league));
		StringBuilder teamStandingsLeagueRequestBuilder = new StringBuilder(
				"http://"+solrHost+":"+solrPort+"/solr/team_standings/select");
		teamStandingsLeagueRequestBuilder.append("?q=id:(").append(activeGame.getHomeTeamId()).append("+")
				.append(activeGame.getAwayTeamId()).append(")")
				.append("&facet=on&facet.field=subLeague&rows=1&wt=json");
		String teamStandingsResponse = ExternalHttpClient$.MODULE$
				.getFromUrl(teamStandingsLeagueRequestBuilder.toString());
		List<String> leagueList = new ArrayList<>();
		if (teamStandingsResponse != null) {
			JsonParser tParser = new JsonParser();
			JsonElement tResponseJson = null;
			try {
				tResponseJson = tParser.parse(teamStandingsResponse);
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

			} catch (Exception e) {
				LOGGER.error("Error occurred in parsing json", e);
			}

		}
		int noOfLeagues = 0;
		for (String subLeague : leagueList) {

			noOfLeagues++;
			standings.add(new StringBuilder().append("team").append(noOfLeagues).append("_title").toString(),
					new JsonPrimitive(subLeague));
			JsonArray teamArray = new JsonArray();
			standings.add(new StringBuilder().append("team").append(noOfLeagues).toString(), teamArray);

			// http://"+solrHost+":"+solrPort+"/solr/team_standings/select?q=league:%22National%20League%22&fq={!collapse%20field=division}&expand=true&expand.rows=100&wt=json
			StringBuilder groupedTSReqBuilder = new StringBuilder(
					"http://"+solrHost+":"+solrPort+"/solr/team_standings/select");
			try {
				groupedTSReqBuilder.append("?q=subLeague:%22").append(URLEncoder.encode(subLeague, "UTF-8"))
						.append("%22")
						.append("&fq=%7B!collapse%20field=division%7D&expand=true&expand.rows=100&wt=json");

				JsonElement groupedTSRespJson = getJsonObject(groupedTSReqBuilder);
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

	private void prepareMCGameSchedule(String teamId, ActiveTeamGame gameRole, JsonArray gameSchedules) {

		// http://"+solrHost+":"+solrPort+"/solr/game_schedule/select?q=game_date_epoch:[1501542300%20TO%20*]&sort=game_date_epoch%20asc&wt=json
		StringBuilder gameScheduleReqBuilder = new StringBuilder(
				"http://"+solrHost+":"+solrPort+"/solr/game_schedule/select");

		switch (gameRole.getActiveTeamRole()) {
		case AWAY:
			gameScheduleReqBuilder.append("?q=awayTeamExternalId:").append(teamId);
			break;
		case HOME:
			gameScheduleReqBuilder.append("?q=homeTeamExternalId:").append(teamId);
			break;
		case NONE:
			break;
		default:
			break;

		}
		long prevSixMonth = Instant.now().getEpochSecond()-Math.round(6*30*24*60*60);
		gameScheduleReqBuilder.append("+AND+").append("game_date_epoch:").append("[")
				.append(prevSixMonth).append("%20TO%20*]");

		gameScheduleReqBuilder.append("&start=0&rows=100").append("&group=true&group.field=gameCode")
				.append("&sort=game_date_epoch%20asc&wt=json");
		JsonElement gameScheduleResponseJson = getJsonObject(gameScheduleReqBuilder);
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
					getSportData(solrDoc, sportDataItem, false, 0, 0, 0, 0);
					updateScoreStatusFromLive(liveResponseJson, sportDataItem, solrDoc.get("gameId").getAsString());
					sportData.add("sport_data", sportDataItem);
					gameSchedules.add(sportData);

				}
			} catch (Exception e) {
				LOGGER.error("Error occurred in parsing json", e);
			}

		}
	}

	private String prepareMCMainGameData(ActiveTeamGame activeGame, String gameScheduleId, JsonObject mc,
			Set<String> subpackIds) {
		String gameId = "0"; // avoid nullpointer
		StringBuilder gameRequestBuilder = new StringBuilder(
				"http://"+solrHost+":"+solrPort+"/solr/game_schedule/select?q=id:").append(gameScheduleId)
						.append("&wt=json");

		JsonArray gameResponse = getJsonObject(gameRequestBuilder).getAsJsonObject().get("response").getAsJsonObject()
				.get("docs").getAsJsonArray();
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
			StringBuilder htPlayerStatsBuilder = new StringBuilder(
					"http://"+solrHost+":"+solrPort+"/solr/player_stats/select?q=id:").append(homePitcherId)
							.append("&fl=wins,losses&wt=json");
			JsonArray htPlayerStatsJson = getJsonObject(htPlayerStatsBuilder).getAsJsonObject().get("response")
					.getAsJsonObject().get("docs").getAsJsonArray();
			StringBuilder atPlayerStatsBuilder = new StringBuilder(
					"http://"+solrHost+":"+solrPort+"/solr/player_stats/select?q=id:").append(awayPitcherId)
							.append("&fl=wins,losses&wt=json");
			JsonArray atPlayerStatsJson = getJsonObject(atPlayerStatsBuilder).getAsJsonObject().get("response")
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
					awayPitcherLosses);
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

	private void mergeLiveInfoToMediaCard(ActiveTeamGame activeGame, JsonObject mc, JsonObject solrDoc,
			JsonObject mcSportData) {
		//
		// Fill in the live scores and other details
		//
		String gameId = solrDoc.get("gameId").getAsString();
		JsonArray liveGameInfoRespJson = getLiveGamesById(gameId);
		if (liveGameInfoRespJson.size() > 0) {
			// pick the first item
			JsonObject liveGameJsonObj = liveGameInfoRespJson.get(0).getAsJsonObject();

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

	private void addCurrentPlayerDetails(JsonObject mcSportData, JsonObject liveGameJsonObj) {
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

	private void addScoreData(ActiveTeamGame activeGame, JsonObject mc, JsonObject solrDoc,
			JsonObject liveGameJsonObj) {
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

	private void addFieldsCount(JsonObject mcSportData, JsonObject liveGameJsonObj) {
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

	private JsonArray getLiveGamesById(String gameId) {
		StringBuilder liveGameInfoReqBuilder = new StringBuilder(
				"http://"+solrHost+":"+solrPort+"/solr/live_info/select");
		liveGameInfoReqBuilder.append("?q=gameId:").append(gameId).append("&wt=json");
		JsonArray liveGameInfoRespJson = getJsonObject(liveGameInfoReqBuilder).getAsJsonObject().get("response")
				.getAsJsonObject().get("docs").getAsJsonArray();
		return liveGameInfoRespJson;
	}

	private String getScoreBoardTitle(ActiveTeamGame activeGame) {
		//
		// Fill the score card title
		//
		String scoreCardTitle = "";
		String activeHomeTeamId = activeGame.getHomeTeamId();
		String activeAwayTeamId = activeGame.getAwayTeamId();
		
		
		StringBuilder allTeamGamesRequestBuilder = new StringBuilder(
				"http://"+solrHost+":"+solrPort+"/solr/live_info/select");
		try {
			//@formatter:off
			allTeamGamesRequestBuilder.
			append("?q=gameType:%22").append(URLEncoder.encode(activeGame.getGameType().getGameTypeStr(), "UTF-8")).append("%22").
			append("+AND+(").
			append("homeTeamExtId:").append(activeGame.getActiveTeamId()).
			append("+OR+").
			append("awayTeamExtId:").append(activeGame.getActiveTeamId()).append(")").
			append("&wt=json");
			//@formatter:on
			JsonArray allTeamGamesJson = getJsonObject(allTeamGamesRequestBuilder).getAsJsonObject().get("response")
					.getAsJsonObject().get("docs").getAsJsonArray();
			int team1Wins = 0; // awayTeam
			int team2Wins = 0; // homeTeam
			String team1ExtId = activeAwayTeamId;
			for (JsonElement it : allTeamGamesJson) {
				int homeWins = it.getAsJsonObject().get("homeScoreRuns").getAsInt();
				int awayWins = it.getAsJsonObject().get("awayScoreRuns").getAsInt();
				String awayTeamId = it.getAsJsonObject().get("awayTeamExtId").getAsString();
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
		
		} catch (UnsupportedEncodingException e) {
			
		}

		 
		return scoreCardTitle;
	}

	private JsonArray getGameForGameId(String gameId) {
		StringBuilder currentGameRequestBuilder = new StringBuilder(
				"http://"+solrHost+":"+solrPort+"/solr/game_schedule/select");
		currentGameRequestBuilder.append("?q=id:").append(gameId).append("&wt=json");
		JsonElement currentGameRespJson = getJsonObject(currentGameRequestBuilder);
		JsonArray currGameDocs = currentGameRespJson.getAsJsonObject().get("response").getAsJsonObject().get("docs")
				.getAsJsonArray();
		return currGameDocs;
	}

	private void getSportData(JsonElement solrDoc, JsonObject sportDataItem, boolean addPitcherDetails,
			int homePitcherWins, int homePitcherLosses, int awayPitcherWins, int awayPitcherLosses) {
		JsonObject gameScheduleJsonObj = solrDoc.getAsJsonObject();
		sportDataItem.add("gameId", new JsonPrimitive(gameScheduleJsonObj.get("id").getAsString()));
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
		JsonArray contentIds = createContentIds(gameScheduleJsonObj);
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

	private void addGameScheduleDates(JsonObject sportDataItem, JsonObject gameScheduleJsonObj) {
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

	private void addTapeDelay(JsonObject sportDataItem, boolean tapeDelay) {
		sportDataItem.add("tape_delayed_game", new JsonPrimitive(tapeDelay));
	}

	private void addGameDateEpoch(JsonObject sportDataItem, long gameDateEpoch) {
		sportDataItem.add("gameDateEpoch", new JsonPrimitive(gameDateEpoch));
	}

	private void addScheduledDate(JsonObject sportDataItem, long gameDateEpoch) {
		Instant epochTime = Instant.ofEpochSecond(gameDateEpoch);
		ZonedDateTime utc = epochTime.atZone(ZoneId.of("Z"));
		String pattern = "EEE, dd MMM yyyy HH:mm:ss Z";
		String scheduledDate = utc.format(java.time.format.DateTimeFormatter.ofPattern(pattern));
		sportDataItem.add("scheduledDate", new JsonPrimitive(scheduledDate));
	}

	private void addSubPackIds(JsonObject sportDataItem, JsonObject gameScheduleJsonObj) {
		JsonArray subPackIds = new JsonArray();
		if(gameScheduleJsonObj.has("subpackage_guids")){
			subPackIds = gameScheduleJsonObj.get("subpackage_guids").getAsJsonArray();
		}
		sportDataItem.add("subPackageGuids", subPackIds);
	}

	private JsonArray createContentIds(JsonObject gameScheduleJsonObj) {
		JsonArray contentIds = new JsonArray();
		String scheduleGuid = getDummyContentId();
		if (gameScheduleJsonObj.has("schedule_guid")) {
			String discoveredScheduleGuid = gameScheduleJsonObj.get("schedule_guid").getAsString();
			if(discoveredScheduleGuid.equals("0")){
				scheduleGuid = getDummyContentId();
			}else {
				scheduleGuid = discoveredScheduleGuid;
			}
		}
		contentIds.add(new JsonPrimitive(scheduleGuid));
		return contentIds;
	}

	private String getDummyContentId() {
		return new StringBuilder("noid_").append(UUID.randomUUID().toString()).toString();
	}


	private void updateGameStatusAndType(JsonObject sportDataItem, JsonObject gameScheduleJsonObj) {
		// information is contianed in both live and scheduled game and hence
		// separated out.
		sportDataItem.add("gameStatus",
				new JsonPrimitive(GameStatus.getValue(gameScheduleJsonObj.get("statusId").getAsInt()).toString()));
		sportDataItem.add("gameType",
				new JsonPrimitive(GameType.getValue(gameScheduleJsonObj.get("gameType").getAsString()).toString()));
	}

	private void addPitcherWinsLosses(int homePitcherWins, int homePitcherLosses, JsonObject team) {

		team.add("pitcherWins", new JsonPrimitive(homePitcherWins));
		team.add("pitcherLosses", new JsonPrimitive(homePitcherLosses));

	}

	/**
	 * This method returns an active team + the game selected for the media card
	 * 
	 * @param teamId
	 * @param currGameDocs
	 * @return
	 */
	private ActiveTeamGame getActiveTeamGame(String teamId, JsonArray currGameDocs) {
		Role role = Role.NONE;
		String homeTeamId = "0";
		String awayTeamId = "0";
		String awayTeamName = "-";
		String homeTeamName = "-";
		String gameId = "0";
		GameType gameType = GameType.REGULAR_SEASON;
		for (JsonElement doc : currGameDocs) {
			homeTeamId = doc.getAsJsonObject().get("homeTeamExternalId").getAsString();
			awayTeamId = doc.getAsJsonObject().get("awayTeamExternalId").getAsString();
			awayTeamName = doc.getAsJsonObject().get("awayTeamName").getAsString();
			homeTeamName = doc.getAsJsonObject().get("homeTeamName").getAsString();
			gameId = doc.getAsJsonObject().get("id").getAsString();
			gameType = GameType.getValue(doc.getAsJsonObject().get("gameType").getAsString());
		}
		if (teamId.equals(homeTeamId)) {
			role = Role.HOME;
		} else if (teamId.equals(awayTeamId)) {
			role = Role.AWAY;
		}
		return new ActiveTeamGame(gameId,gameType, teamId, homeTeamId, awayTeamId, homeTeamName, awayTeamName, role);
	}

	private void legacy(final ChannelHandlerContext ctx, final FullHttpRequest request) {
		final boolean keepAlive = HttpHeaders.isKeepAlive(request);
		final ByteBuf buf = ctx.alloc().directBuffer();
		try {
			HttpResponse response = null;
			response = new DefaultHttpResponse(request.protocolVersion(), HttpResponseStatus.OK);
			response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "application/json");
			response.headers().set(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
			StringBuilder requestURLBuilder = new StringBuilder();
			requestURLBuilder.append(JsonProxyServerConfiguration.getTARGET_HOST_TO_PROXY()).append(request.uri());

			URL baseURLToProxy = new URL(JsonProxyServerConfiguration.getTARGET_HOST_TO_PROXY());
			String responseString = ExternalHttpClient$.MODULE$.getFromUrl(requestURLBuilder.toString());

			if (responseString != null) {
				JsonParser parser = new JsonParser();
				JsonElement responseJson = null;
				try {
					responseJson = parser.parse(responseString);
					String programId = responseJson.getAsJsonObject().get("program").getAsJsonObject().get("id")
							.getAsString();
					JsonArray scheduleArray = responseJson.getAsJsonObject().get("schedules").getAsJsonArray();
					JsonElement mappingObj = null;
					for (JsonElement jsonElement : scheduleArray) {
						String channelGuid = jsonElement.getAsJsonObject().get("channel_guid").getAsString();
						String callsign = jsonElement.getAsJsonObject().get("channel_title").getAsString();
						try {
							mappingObj = ContentMatchFacade$.MODULE$.getDataForChannelGuidAndProgramID(channelGuid,
									programId);
						} catch (NoSuchElementException e) {
							LOGGER.info("Unable to get info1");
						}
						if (mappingObj != null)
							break;
					}

					JsonObject sportApiJson = new JsonObject();

					if (mappingObj == null) {
						LOGGER.error(String.format("Mapping for content %s not found", programId));
					} else {
						// GET 0th item
						JsonElement mappingObjItem = mappingObj.getAsJsonObject().get("gameEvents").getAsJsonArray()
								.get(0);

						String homeTeamName = "-";
						if (mappingObjItem.getAsJsonObject().get("homeTeamName") != null) {
							homeTeamName = mappingObjItem.getAsJsonObject().get("homeTeamName").getAsString();
						}

						String awayTeamName = "-";
						if (mappingObjItem.getAsJsonObject().get("awayTeamName") != null) {
							awayTeamName = mappingObjItem.getAsJsonObject().get("awayTeamName").getAsString();
						}

						String homeTeamScore = "0";
						if (mappingObjItem.getAsJsonObject().get("homeTeamScore") != null) {
							homeTeamScore = mappingObjItem.getAsJsonObject().get("homeTeamScore").getAsJsonObject()
									.get("$numberLong").getAsString();
						}

						String awayTeamScore = "0";
						if (mappingObjItem.getAsJsonObject().get("awayTeamScore") != null) {
							awayTeamScore = mappingObjItem.getAsJsonObject().get("awayTeamScore").getAsJsonObject()
									.get("$numberLong").getAsString();
						}

						String awayTeamPitcherName = "-";
						if (mappingObjItem.getAsJsonObject().get("awayTeamPitcherName") != null) {
							awayTeamPitcherName = mappingObjItem.getAsJsonObject().get("awayTeamPitcherName")
									.getAsString().split(" ")[0];
						}

						String homeTeamPitcherName = "-";
						if (mappingObjItem.getAsJsonObject().get("homeTeamPitcherName") != null) {
							homeTeamPitcherName = mappingObjItem.getAsJsonObject().get("homeTeamPitcherName")
									.getAsString().split(" ")[0];
						}

						String homeTeamImg = "-";
						if (mappingObjItem.getAsJsonObject().get("homeTeamImg") != null) {
							homeTeamImg = mappingObjItem.getAsJsonObject().get("homeTeamImg").getAsString();
						}

						String awayTeamImg = "-";
						if (mappingObjItem.getAsJsonObject().get("awayTeamImg") != null) {
							awayTeamImg = mappingObjItem.getAsJsonObject().get("awayTeamImg").getAsString();
						}

						String gexPredict = "0";
						if (mappingObjItem.getAsJsonObject().get("gexPredict") != null) {
							gexPredict = mappingObjItem.getAsJsonObject().get("gexPredict").getAsJsonObject()
									.get("$numberLong").getAsString();
						}

						JsonObject awayTeamObject = new JsonObject();
						awayTeamObject.add("name", new JsonPrimitive(awayTeamName));
						awayTeamObject.add("pitcherName", new JsonPrimitive(awayTeamPitcherName));
						awayTeamObject.add("img", new JsonPrimitive(awayTeamImg));
						JsonObject homeTeamObject = new JsonObject();
						homeTeamObject.add("name", new JsonPrimitive(homeTeamName));
						homeTeamObject.add("pitcherName", new JsonPrimitive(homeTeamPitcherName));
						homeTeamObject.add("img", new JsonPrimitive(homeTeamImg));
						JsonObject sportData = new JsonObject();
						sportData.add("homeTeam", homeTeamObject);
						sportData.add("awayTeam", awayTeamObject);
						sportData.add("homeScore", new JsonPrimitive(homeTeamScore));
						sportData.add("awayScore", new JsonPrimitive(awayTeamScore));
						sportData.add("rating", new JsonPrimitive(gexPredict));
						JsonObject mediaCardJsonObj = new JsonObject();
						mediaCardJsonObj.add("sport_data", sportData);
						sportApiJson.getAsJsonObject().add("mc", mediaCardJsonObj);
					}

					if (responseJson != null) {
						responseJson.getAsJsonObject().add("sports-cloud", sportApiJson);
						responseString = responseJson.toString();
					}
				} catch (Exception e) {
					LOGGER.error("Error occurred in parsing json", e);
				}
			} else {
				responseString = "";
			}

			if (keepAlive) {
				response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
			} else {
				response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
			}
			byte[] bytes = responseString.toString().getBytes();
			response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, bytes.length);

			// response.headers().set(HttpHeaders.Names.CONNECTION,
			// HttpHeaders.Values.CLOSE);

			ctx.write(response);

			buf.writeBytes(bytes);

		} catch (Exception e) {
			LOGGER.error("Error occurred during encoding", e);
		} finally {
			final ChannelFuture future = ctx.writeAndFlush(new DefaultLastHttpContent(buf));
			future.addListener(ChannelFutureListener.CLOSE);
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

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.close();
	}

}
