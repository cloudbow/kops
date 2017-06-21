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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.slingmedia.sportscloud.tests.dao.*;
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.netty.channel.SimpleChannelInboundHandler#channelRead0(io.netty.
	 * channel.ChannelHandlerContext, java.lang.Object)
	 */
	@Override
	protected void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest request) throws Exception {
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
							mappingObj = MongoDAO$.MODULE$.getDataForChannelGuidAndProgramIDAndCallSign(channelGuid,
									programId, callsign);
						} catch (NoSuchElementException e) {
							LOGGER.info("Unable to get info1");
						}
						if (mappingObj != null)
							break;
					}

					if (mappingObj == null) {
						for (JsonElement jsonElement : scheduleArray) {
							String channelGuid = jsonElement.getAsJsonObject().get("channel_guid").getAsString();
							try {
								mappingObj = MongoDAO$.MODULE$.getDataForChannelGuidAndProgramID(channelGuid,
										programId);
							} catch (NoSuchElementException e) {
								LOGGER.info("Unable to get info2");
							}
							if (mappingObj != null)
								break;
						}
					}
					JsonElement sportApiJson = parser.parse("{}");

					if (mappingObj == null) {
						LOGGER.error(String.format("Mapping for content %s not found", programId));
					} else {
						// GET 0th item
						JsonElement mappingObjItem = mappingObj.getAsJsonObject().get("gameEvents").getAsJsonArray()
								.get(0);

						String contentId = mappingObjItem.getAsJsonObject().get("contentId").getAsString();
						LOGGER.trace(String.format("Got contentId %s", contentId));
						String gameId = mappingObjItem.getAsJsonObject().get("gameId").getAsString();
						String teamId = mappingObjItem.getAsJsonObject().get("teamId").getAsString();
						String genres = mappingObjItem.getAsJsonObject().get("genres").getAsString();

						String sportApiResponse = fetchSportApiResponse(gameId, teamId, genres);
						sportApiJson = parser.parse(sportApiResponse);
					}
					if (sportApiJson != null)
						responseJson.getAsJsonObject().add("sports-cloud", sportApiJson);

					if (responseJson != null) {
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

	private String fetchSportApiResponse(String gameId, String teamId, String genres) {
		String league = "";
		String genresLower = genres.toLowerCase();
		if (genresLower.contains("basketball")) {
			if (genresLower.contains("college")) {// College basketball //ncaab
				league="ncaab";
			} else {// nba
				league="nba";
			}
		} else if (genresLower.contains("football")) {
			if (genresLower.contains("college")) {// ncaaf
				league="ncaaf";
			} else {// nfl
				league="nfl";
			}
		} else if (genresLower.contains("baseball")) {// mlb
			league="mlb";
		} else if (genresLower.contains("soccer")) {// soccer
			league="soccer";
		} else if (genresLower.contains("hockey")) {// soccer
			league="nhl";
		}

		String url1Template = "http://hsportsdata.slingbox.com/dish/v1/mc/%s?gameId=%s&teamId=%s";
		String url2Template = "http://hsportsdata.slingbox.com/dish/games/%s/%s";
		String url = "";
		if(league.equals("nhl") || league.equals("soccer")) {
			url  = String.format(url2Template);
		} else {
			url  = String.format(url1Template,league,gameId,teamId);
		}
		

		URL baseURLToProxy = null;
		try {
			baseURLToProxy = new URL(url);
		} catch (MalformedURLException e1) {
			LOGGER.error("Error occurred in url", e1);
		}
		//@formatter:on
		StringBuilder requestURLBuilder = new StringBuilder();
		requestURLBuilder.append(String.format(url1Template, league,gameId, teamId));

		//@formatter:off
		String responseString = ExternalHttpClient$.MODULE$.getFromUrl(requestURLBuilder.toString());		
		return responseString;
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.close();
	}

}
