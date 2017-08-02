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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.http.client.utils.URLEncodedUtils;
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
import io.netty.util.CharsetUtil;

/**
 * The Class GenericJsonProxyDecoder.
 *
 * @author arung
 */
public class GenericJsonProxyDecoder extends SimpleChannelInboundHandler<FullHttpRequest> {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(GenericJsonProxyDecoder.class);

	GenericJsonProxyDecoder() {

	}

	enum Role {
		HOME, AWAY, NONE
	}

	class GameRole {
		private String gameId;
		private Role role;
		private String homeTeamId;
		private String awayTeamId;

		GameRole(String gameId, String homeTeamId, String awayTeamId, Role role) {
			this.setHomeTeamId(homeTeamId);
			this.setAwayTeamId(awayTeamId);
			this.setGameId(gameId);
			this.setRole(role);
		}

		public String getGameId() {
			return gameId;
		}

		public void setGameId(String gameId) {
			this.gameId = gameId;
		}

		public Role getRole() {
			return role;
		}

		public void setRole(Role role) {
			this.role = role;
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
				// create requests here
				StringBuilder requestURLBuilder = new StringBuilder(
						"http://cqaneat02.sling.com:8983/solr/game_schedule/select");

				requestURLBuilder.append("?q=gameDate:[").append(Instant.now().getEpochSecond()).append("%20TO%20")
						.append("*").append("]").append("&wt=json");
				URL baseURLToProxy = new URL(requestURLBuilder.toString());
				String responseString = ExternalHttpClient$.MODULE$.getFromUrl(requestURLBuilder.toString());

				if (responseString != null) {
					JsonParser parser = new JsonParser();
					JsonElement responseJson = null;
					JsonArray allGames = new JsonArray();
					try {
						responseJson = parser.parse(responseString);
						JsonArray docs = responseJson.getAsJsonObject().get("response").getAsJsonObject().get("docs")
								.getAsJsonArray();
						for (JsonElement solrDoc : docs) {
							JsonObject mainObj = new JsonObject();
							mainObj.add("id", new JsonPrimitive(solrDoc.getAsJsonObject().get("id").getAsString()));
							mainObj.add("sport",
									new JsonPrimitive(solrDoc.getAsJsonObject().get("sport").getAsString()));
							mainObj.add("league",
									new JsonPrimitive(solrDoc.getAsJsonObject().get("league").getAsString()));
							long gameDateEpoch = solrDoc.getAsJsonObject().get("game_date_epoch").getAsLong();
							Instant epochTime = Instant.ofEpochSecond(gameDateEpoch);
							ZonedDateTime utc = epochTime.atZone(ZoneId.of("Z"));
							String pattern = "EEE, dd MMM yyyy HH:mm:ss Z";
							String scheduledDate = utc.format(DateTimeFormatter.ofPattern(pattern));
							mainObj.add("scheduledDate", new JsonPrimitive(scheduledDate));
							// todo create this enum based on other solr
							// collection
							mainObj.add("gameStatus", new JsonPrimitive("UPCOMING"));
							mainObj.add("rating",
									new JsonPrimitive(solrDoc.getAsJsonObject().get("gexPredict").getAsString()));
							String teaser = "-";
							if (solrDoc.getAsJsonObject().has("preGameTeaser")) {
								teaser = solrDoc.getAsJsonObject().get("preGameTeaser").getAsString();
							}
							mainObj.add("teaser", new JsonPrimitive(teaser));
							JsonObject homeTeam = new JsonObject();
							JsonObject awayTeam = new JsonObject();
							JsonObject homeTeamRecord = new JsonObject();
							JsonObject awayTeamRecord = new JsonObject();

							mainObj.add("homeTeam", homeTeam);
							homeTeam.add("name",
									new JsonPrimitive(solrDoc.getAsJsonObject().get("homeTeamName").getAsString()));
							// todo
							homeTeam.add("alias", new JsonPrimitive("-"));
							homeTeam.add("img",
									new JsonPrimitive(solrDoc.getAsJsonObject().get("homeTeamImg").getAsString()));
							homeTeam.add("id", new JsonPrimitive(
									solrDoc.getAsJsonObject().get("homeTeamExternalId").getAsString()));
							mainObj.add("awayTeam", awayTeam);
							awayTeam.add("name",
									new JsonPrimitive(solrDoc.getAsJsonObject().get("awayTeamName").getAsString()));
							// todo
							awayTeam.add("alias", new JsonPrimitive("-"));
							awayTeam.add("img",
									new JsonPrimitive(solrDoc.getAsJsonObject().get("awayTeamImg").getAsString()));
							awayTeam.add("id", new JsonPrimitive(
									solrDoc.getAsJsonObject().get("awayTeamExternalId").getAsString()));

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
							mainObj.add("homeScore", new JsonPrimitive(0));
							// todo
							mainObj.add("awayScore", new JsonPrimitive(0));

							JsonArray contentIds = new JsonArray();
							// todo
							contentIds.add(0);

							mainObj.add("contentId", contentIds);

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
					}
					finalResponse = allGames.toString();
				} else {
					responseString = "{}";
				}
			} else if (uri.startsWith("/dish/v1/mc/mlb")) {
				try {
					QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.uri());
					Map<String, List<String>> params = queryStringDecoder.parameters();
					String gameId = null;
					if (params.get("gameId") != null) {
						gameId = params.get("gameId").get(0);
					}
					String teamId = params.get("teamId").get(0);
					JsonObject gameFinderDrillDownJson = new JsonObject();
					GameRole gameRole = new GameRole("0", null, null, Role.NONE);
					if (gameId != null) {

						JsonArray allGames = new JsonArray();
						try {
							JsonArray currGameDocs = getGameForGameId(gameId);
							gameRole = getGameRole(teamId, currGameDocs);

						} catch (Exception e) {
							LOGGER.error("Error occurred in parsing json", e);
						}

					} else if (gameId == null) {

						if (teamId != null) {

							// http://cqaneat02.sling.com:8983/solr/game_schedule/select?q=game_date_epoch:[1501542300%20TO%20*]&sort=game_date_epoch%20desc&wt=json&rows=1
							StringBuilder teamIdRequestBuilder = new StringBuilder(
									"http://cqaneat02.sling.com:8983/solr/game_schedule/select");
							teamIdRequestBuilder.append("?q=awayTeamExternalId:").append(teamId).append("+OR+")
									.append("homeTeamExternalId:").append(teamId)
									.append("&sort=game_date_epoch%20desc&wt=json&rows=1");
							JsonElement teamIdResponse = getJsonObject(teamIdRequestBuilder);
							JsonArray teamDocs = teamIdResponse.getAsJsonArray();
							gameRole = getGameRole(teamId, teamDocs);
							gameId = gameRole.getGameId();
						}

					}

					if (gameRole.getRole() != Role.NONE) {

						JsonArray gameSchedules = new JsonArray();
						JsonObject mc = new JsonObject();
						JsonArray scoringEvents = new JsonArray();
						JsonObject divisionSeries = new JsonObject();
						JsonObject standings = new JsonObject();
						// todo
						gameFinderDrillDownJson.add("active_team", new JsonPrimitive(teamId));
						gameFinderDrillDownJson.add("gamesSchedule", gameSchedules);
						gameFinderDrillDownJson.add("mc", mc);
						gameFinderDrillDownJson.add("scoringEvents", scoringEvents);
						gameFinderDrillDownJson.add("division_series", divisionSeries);
						gameFinderDrillDownJson.add("standings", standings);

						prepareGameScheduleData(teamId, gameRole, gameSchedules);

						prepareMediaCardData(gameId, mc);

						// http://localhost:8983/solr/techproducts/select?wt=json&indent=true&fl=id,name&q=solr+memory&group=true&group.field=manu_exact
						prepareTeamStandings(gameRole, standings);

					}

					finalResponse = gameFinderDrillDownJson.toString();
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

	private void prepareTeamStandings(GameRole gameRole, JsonObject standings) {
		StringBuilder teamStandingsLeagueRequestBuilder = new StringBuilder(
				"http://cqaneat02.sling.com:8983/solr/team_standings/select");
		teamStandingsLeagueRequestBuilder.append("?q=id:(").append(gameRole.getHomeTeamId()).append("+")
				.append(gameRole.getAwayTeamId()).append(")").append("&facet=on&facet.field=subLeague&rows=1&wt=json");
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
				int k = 0;
				for (JsonElement it : tDocs) {
					k++;
					if (k % 2 != 0) {
						leagueList.add(it.getAsString());
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

			// http://cqaneat02.sling.com:8983/solr/team_standings/select?q=league:%22National%20League%22&fq={!collapse%20field=division}&expand=true&expand.rows=100&wt=json
			StringBuilder groupedTSReqBuilder = new StringBuilder(
					"http://cqaneat02.sling.com:8983/solr/team_standings/select");
			try {
				groupedTSReqBuilder.append("?q=subLeague:%22").append(URLEncoder.encode(subLeague, "UTF-8"))
						.append("%22")
						.append("&fq=%7B!collapse%20field=division%7D&expand=true&expand.rows=100&wt=json");

				String groupedTSResponse = ExternalHttpClient$.MODULE$.getFromUrl(groupedTSReqBuilder.toString());
				if (groupedTSResponse != null) {
					JsonParser groupedTSResParser = new JsonParser();
					JsonElement groupedTSRespJson = null;

					groupedTSRespJson = groupedTSResParser.parse(groupedTSResponse);
					JsonObject expandedDocs = groupedTSRespJson.getAsJsonObject().get("expanded").getAsJsonObject();
					expandedDocs.entrySet().forEach(entry -> {

						JsonObject divisionObj = new JsonObject();
						divisionObj.add("division", new JsonPrimitive(entry.getKey()));
						
						JsonArray leagueStandings = new JsonArray();
						JsonArray divisionsArray = entry.getValue().getAsJsonObject().get("docs").getAsJsonArray();
						divisionsArray.forEach(divisionTeam -> {
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

							leagueStandings.add(leagueStanding);
						});
						
						divisionObj.add("league-standings", leagueStandings);

						teamArray.add(divisionObj);

					});

				}

			} catch (Exception e1) {
				LOGGER.error("Error occurred in parsing json", e1);
			}

		}
	}

	private void prepareGameScheduleData(String teamId, GameRole gameRole, JsonArray gameSchedules) {
		// http://cqaneat02.sling.com:8983/solr/game_schedule/select?q=game_date_epoch:[1501542300%20TO%20*]&sort=game_date_epoch%20asc&wt=json
		StringBuilder gameScheduleReqBuilder = new StringBuilder(
				"http://cqaneat02.sling.com:8983/solr/game_schedule/select");

		switch (gameRole.getRole()) {
		case AWAY:
			gameScheduleReqBuilder.append("?q=awayTeamExternalId=").append(teamId);
			break;
		case HOME:
			gameScheduleReqBuilder.append("?q=homeTeamExternalId=").append(teamId);
			break;
		case NONE:
			break;
		default:
			break;

		}

		gameScheduleReqBuilder.append("+AND+").append("game_date_epoch:").append("[")
				.append(Instant.now().getEpochSecond()).append("%20TO%20*]");

		gameScheduleReqBuilder.append("&sort=game_date_epoch%20asc&wt=json");
		JsonElement gameScheduleResponseJson = getJsonObject(gameScheduleReqBuilder);

		// create game schedule json part
		if (gameScheduleResponseJson != null) {

			try {

				JsonArray docs = gameScheduleResponseJson.getAsJsonObject().get("response").getAsJsonObject()
						.get("docs").getAsJsonArray();

				for (JsonElement solrDoc : docs) {

					JsonObject sportData = getSportData(solrDoc);

					gameSchedules.add(sportData);

				}
			} catch (Exception e) {
				LOGGER.error("Error occurred in parsing json", e);
			}

		}
	}

	private void prepareMediaCardData(String gameId, JsonObject mc) {
		StringBuilder gameRequestBuilder = new StringBuilder(
				"http://cqaneat02.sling.com:8983/solr/game_schedule/select?q=id:").append(gameId).append("&wt=json");
		JsonArray gameResponse = getJsonObject(gameRequestBuilder).getAsJsonObject().get("response").getAsJsonObject()
				.get("docs").getAsJsonArray();
		if (gameResponse != null && gameResponse.size() != 0) {
			JsonObject solrDoc = gameResponse.get(0).getAsJsonObject();
			JsonObject sportData = getSportData(solrDoc);
			mc.add("sport-data", sportData);
			String teaser = "-";
			if (solrDoc.getAsJsonObject().has("preGameTeaser")) {
				teaser = solrDoc.getAsJsonObject().get("preGameTeaser").getAsString();
			}
			mc.add("anons", new JsonPrimitive(teaser));
			mc.add("anons_title", new JsonPrimitive(solrDoc.getAsJsonObject().get("anonsTitle").getAsString()));
			JsonObject scoreData = new JsonObject();
			JsonObject scoreHomeTeam = new JsonObject();
			JsonObject scoreAwayTeam = new JsonObject();
			// todo
			scoreHomeTeam.add("runs", new JsonPrimitive(0));
			// todo
			scoreAwayTeam.add("runs", new JsonPrimitive(0));

			scoreData.add("homeTeam", scoreHomeTeam);
			scoreData.add("awayTeam", scoreAwayTeam);

			// todo
			scoreData.add("scoreboard_title", new JsonPrimitive("-"));
			scoreData.add("sport", new JsonPrimitive(solrDoc.getAsJsonObject().get("sport").getAsString()));
		}
	}

	private JsonArray getGameForGameId(String gameId) {
		StringBuilder currentGameRequestBuilder = new StringBuilder(
				"http://cqaneat02.sling.com:8983/solr/game_schedule/select");
		currentGameRequestBuilder.append("?q=id:").append(gameId).append("&wt=json");
		JsonElement currentGameRespJson = getJsonObject(currentGameRequestBuilder);
		JsonArray currGameDocs = currentGameRespJson.getAsJsonObject().get("response").getAsJsonObject().get("docs")
				.getAsJsonArray();
		return currGameDocs;
	}

	private JsonObject getSportData(JsonElement solrDoc) {
		JsonObject sportData = new JsonObject();
		// todo
		sportData.add("homeScore", new JsonPrimitive(0));
		// todo
		sportData.add("awayScore", new JsonPrimitive(0));
		JsonObject homeTeam = new JsonObject();
		JsonObject awayTeam = new JsonObject();
		JsonObject homeTeamRecord = new JsonObject();
		JsonObject awayTeamRecord = new JsonObject();
		sportData.add("homeTeam", homeTeam);
		homeTeam.add("name", new JsonPrimitive(solrDoc.getAsJsonObject().get("homeTeamName").getAsString()));
		homeTeam.add("city", new JsonPrimitive(solrDoc.getAsJsonObject().get("homeTeamCity").getAsString()));

		// todo
		homeTeam.add("alias", new JsonPrimitive("-"));
		// todo
		homeTeam.add("img", new JsonPrimitive(solrDoc.getAsJsonObject().get("homeTeamImg").getAsString()));
		homeTeam.add("id", new JsonPrimitive(solrDoc.getAsJsonObject().get("homeTeamExternalId").getAsString()));
		// todo
		homeTeam.add("pitcherName", new JsonPrimitive("-"));

		sportData.add("awayTeam", awayTeam);
		awayTeam.add("name", new JsonPrimitive(solrDoc.getAsJsonObject().get("awayTeamName").getAsString()));
		awayTeam.add("city", new JsonPrimitive(solrDoc.getAsJsonObject().get("awayTeamCity").getAsString()));

		// todo
		awayTeam.add("alias", new JsonPrimitive("-"));
		// todo
		awayTeam.add("img", new JsonPrimitive(solrDoc.getAsJsonObject().get("awayTeamImg").getAsString()));

		awayTeam.add("id", new JsonPrimitive(solrDoc.getAsJsonObject().get("awayTeamExternalId").getAsString()));
		// todo
		awayTeam.add("pitcherName", new JsonPrimitive("-"));

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
		sportData.add("gameId", new JsonPrimitive("-"));
		// todo
		sportData.add("gameStatus", new JsonPrimitive("UPCOMING"));
		// todo
		sportData.add("gameType", new JsonPrimitive("-"));
		// todo
		sportData.add("division", new JsonPrimitive("-"));
		// todo
		long gameDateEpoch = solrDoc.getAsJsonObject().get("game_date_epoch").getAsLong();
		Instant epochTime = Instant.ofEpochSecond(gameDateEpoch);
		ZonedDateTime utc = epochTime.atZone(ZoneId.of("Z"));
		String pattern = "EEE, dd MMM yyyy HH:mm:ss Z";
		String scheduledDate = utc.format(DateTimeFormatter.ofPattern(pattern));
		sportData.add("scheduledDate", new JsonPrimitive(scheduledDate));
		sportData.add("sport", new JsonPrimitive(solrDoc.getAsJsonObject().get("sport").getAsString()));
		sportData.add("league", new JsonPrimitive(solrDoc.getAsJsonObject().get("league").getAsString()));
		JsonArray contentIds = new JsonArray();
		// todo
		contentIds.add(0);

		sportData.add("contentId", contentIds);
		return sportData;
	}

	private GameRole getGameRole(String teamId, JsonArray currGameDocs) {
		Role role = Role.NONE;
		String homeTeamId = "0";
		String awayTeamId = "0";
		String gameId = "0";
		for (JsonElement doc : currGameDocs) {
			homeTeamId = doc.getAsJsonObject().get("homeTeamExternalId").getAsString();
			awayTeamId = doc.getAsJsonObject().get("awayTeamExternalId").getAsString();
			gameId = doc.getAsJsonObject().get("id").getAsString();
		}
		if (teamId.equals(homeTeamId)) {
			role = Role.HOME;
		} else if (teamId.equals(awayTeamId)) {
			role = Role.AWAY;
		}
		return new GameRole(gameId, homeTeamId, awayTeamId, role);
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
