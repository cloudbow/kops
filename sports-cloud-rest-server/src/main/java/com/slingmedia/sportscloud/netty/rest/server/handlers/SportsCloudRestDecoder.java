/*
 * SportsCloudRestDecoder.java
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
package com.slingmedia.sportscloud.netty.rest.server.handlers;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.slingmedia.sportscloud.facade.*;
import com.slingmedia.sportscloud.netty.rest.model.ActiveTeamGame;
import com.slingmedia.sportscloud.netty.rest.model.Role;
import com.slingmedia.sportscloud.netty.rest.server.handler.delegates.SportsCloudHomeScreenDelegate;
import com.slingmedia.sportscloud.netty.rest.server.handler.delegates.SportsCloudMCDelegate;

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
 * The Class SportsCloudRestDecoder.
 *
 * @author arung
 */
public class SportsCloudRestDecoder extends SimpleChannelInboundHandler<FullHttpRequest> {

	/** The Constant LOGGER. */
	public static final Logger LOGGER = LoggerFactory.getLogger(SportsCloudRestDecoder.class);
	
	private SportsCloudHomeScreenDelegate sportsCloudHomeScreenDelegate = new SportsCloudHomeScreenDelegate();
	private SportsCloudMCDelegate sportsCloudMCDelegate = new SportsCloudMCDelegate();

	private DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("YYYY-MM-dd Z").withLocale(Locale.US);
	
	SportsCloudRestDecoder() {

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

					finalResponse = prepareMCData(gameScheduleId, teamId, subpackIds);
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

	private String prepareMCData(String gameScheduleId, String teamId, Set<String> subpackIds) {
		String finalResponse;
		JsonObject gameFinderDrillDownJson = new JsonObject();
		ActiveTeamGame activeGame = new ActiveTeamGame( "0",null, "0", null, null, null, null, Role.NONE);
		if (gameScheduleId != null) {

			try {
				JsonArray currGameDocs = sportsCloudMCDelegate.getGameForGameId(gameScheduleId);
				activeGame = sportsCloudMCDelegate.getActiveTeamGame(teamId, currGameDocs);

			} catch (Exception e) {
				LOGGER.error("Error occurred in parsing json", e);
			}

		} else if (gameScheduleId == null) {

			if (teamId != null) {
				JsonElement teamIdResponse = SportsDataFacade$.MODULE$.getNearestGameScheduleForActiveTeam(teamId);
				JsonArray teamDocs = teamIdResponse.getAsJsonObject().get("response").getAsJsonObject().get("docs").getAsJsonArray();
				activeGame = sportsCloudMCDelegate.getActiveTeamGame(teamId, teamDocs);
				gameScheduleId = activeGame.getGameId();
			}

		}

		sportsCloudMCDelegate.prepareMCJson(gameScheduleId, teamId, subpackIds, gameFinderDrillDownJson, activeGame);

		finalResponse = gameFinderDrillDownJson.toString();
		return finalResponse;
	}

	private String prepareGameScheduleDataForHomeScreen(String finalResponse, long startDate, String endDate,
			Set<String> subpackIds) {
		
	   JsonElement gameSchedulesJson = SportsDataFacade$.MODULE$.getGameScheduleDataForHomeScreen(startDate,endDate);
	   finalResponse = sportsCloudHomeScreenDelegate.prepareJsonResponseForHomeScreen( finalResponse, startDate, endDate, subpackIds,
				gameSchedulesJson);
		return finalResponse;
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.close();
	}

}
