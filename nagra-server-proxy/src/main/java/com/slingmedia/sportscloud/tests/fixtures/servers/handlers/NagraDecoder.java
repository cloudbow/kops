/*
 * NagraDecoder.java
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

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReaderFactory;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import pns.alltypes.netty.httpclient.config.HostConfig;
import pns.alltypes.netty.httpclient.config.SyncType;
import pns.alltypes.netty.httpclient.exception.AlreadyRegisteredHostException;
import pns.alltypes.netty.httpclient.exception.InvalidResponseException;
import pns.alltypes.netty.httpclient.request.HttpRequestMessage.Builder;
import pns.alltypes.netty.httpclient.request.RequestMaker;
import pns.alltypes.netty.httpclient.response.ResponseMsg;


/**
 * The Class NagraDecoder.
 *
 * @author arung
 */
public class NagraDecoder extends SimpleChannelInboundHandler<FullHttpRequest> {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(NagraDecoder.class);
	
	/** The Constant REQUEST_MAKER. */
	private static final RequestMaker REQUEST_MAKER = RequestMaker.getInstance();

	/** The Constant JSON_READER_FACTORY. */
	private static final JsonReaderFactory JSON_READER_FACTORY = Json.createReaderFactory(null);

	/**
	 * Instantiates a new nagra decoder.
	 */
	NagraDecoder() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.netty.channel.SimpleChannelInboundHandler#channelRead0(io.netty.
	 * channel.ChannelHandlerContext, java.lang.Object)
	 */
	@Override
	protected void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest request) throws Exception {

		@SuppressWarnings("deprecation")
		String uri = request.getUri();

		if (uri.contains("/games")) {

			HttpResponse response = null;
			response = new DefaultHttpResponse(request.protocolVersion(), HttpResponseStatus.OK);
			response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "application/json");

			final boolean keepAlive = HttpHeaders.isKeepAlive(request);
			HostConfig hostConfig = null;
			try {
				hostConfig = new HostConfig("gwserv-mobileprod.echodata.tv", 80, SyncType.OPENCLOSE);
				REQUEST_MAKER.registerHost(hostConfig);
			} catch (final AlreadyRegisteredHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			String urlTemplate = "http://gwserv-mobileprod.echodata.tv/Gamefinder/api/game/search?page.size=100";

			final Builder registerCallBack = REQUEST_MAKER.registerCallBack(HttpMethod.GET, urlTemplate);
			registerCallBack.url().headers().addHeader("host", hostConfig.getHost());
			ResponseMsg requestSync = null;
			try {
				requestSync = REQUEST_MAKER.requestSync(registerCallBack.build(), hostConfig);
			} catch (final InvalidResponseException e) {

			}
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace(requestSync.getResponse());
			}
			JsonObject resultJSON = NagraDecoder.JSON_READER_FACTORY
					.createReader(new StringReader(requestSync.getResponse())).readObject();

			if (keepAlive) {
				response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
			} else {
				response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
			}
			JsonObjectBuilder mainJson = Json.createObjectBuilder();
			JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();

			// iterate nagra json object . check with a file
			JsonArray jsonArray = resultJSON.getJsonArray("content");
			for (int i = 0; i < jsonArray.size(); i++) {
				JsonObject obj = jsonArray.getJsonObject(i);
				jsonArrayBuilder
						.add(getScheduledDateFromFileForGames(obj, System.getProperty("nagra-games-file")).build());
			}
			mainJson.add("content", jsonArrayBuilder.build());

			byte[] bytes = mainJson.build().toString().getBytes();
			response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, bytes.length);

			// response.headers().set(HttpHeaders.Names.CONNECTION,
			// HttpHeaders.Values.CLOSE);

			ctx.write(response);

			final ByteBuf buf = ctx.alloc().directBuffer();
			buf.writeBytes(bytes);
			final ChannelFuture future = ctx.writeAndFlush(new DefaultLastHttpContent(buf));
			if (!keepAlive) {
				future.addListener(ChannelFutureListener.CLOSE);
			}

		} else if (uri.contains("/alert")) {
			HttpResponse response = null;
			response = new DefaultHttpResponse(request.protocolVersion(), HttpResponseStatus.OK);
			response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "application/json");

			final boolean keepAlive = HttpHeaders.isKeepAlive(request);
			HostConfig hostConfig = null;
			try {
				hostConfig = new HostConfig("gwserv-mobileprod.echodata.tv", 80, SyncType.OPENCLOSE);
				REQUEST_MAKER.registerHost(hostConfig);
			} catch (final AlreadyRegisteredHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			String urlTemplate = "http://gwserv-mobileprod.echodata.tv/Gamefinder/api/thuuz/alert";

			final Builder registerCallBack = REQUEST_MAKER.registerCallBack(HttpMethod.GET, urlTemplate);
			registerCallBack.url().headers().addHeader("host", hostConfig.getHost());
			ResponseMsg requestSync = null;
			try {
				requestSync = REQUEST_MAKER.requestSync(registerCallBack.build(), hostConfig);
			} catch (final InvalidResponseException e) {

			}
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace(requestSync.getResponse());
			}
			JsonObject resultJSON = NagraDecoder.JSON_READER_FACTORY
					.createReader(new StringReader(requestSync.getResponse())).readObject();

			if (keepAlive) {
				response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
			} else {
				response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
			}
			JsonObjectBuilder mainJson = Json.createObjectBuilder();
			JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();

			// iterate nagra json object . check with a file
			JsonArray jsonArray = resultJSON.getJsonArray("ratings");
			for (int i = 0; i < jsonArray.size(); i++) {
				JsonObject obj = jsonArray.getJsonObject(i);
				jsonArrayBuilder
						.add(getScheduledDateFromFileForAlerts(obj, System.getProperty("nagra-alerts-file")).build());
			}
			mainJson.add("ratings", jsonArrayBuilder.build());

			byte[] bytes = mainJson.build().toString().getBytes();
			response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, bytes.length);

			// response.headers().set(HttpHeaders.Names.CONNECTION,
			// HttpHeaders.Values.CLOSE);

			ctx.write(response);

			final ByteBuf buf = ctx.alloc().directBuffer();
			buf.writeBytes(bytes);
			final ChannelFuture future = ctx.writeAndFlush(new DefaultLastHttpContent(buf));
			if (!keepAlive) {
				future.addListener(ChannelFutureListener.CLOSE);
			}
		}
	}

	/**
	 * Gets the scheduled date from file for alerts.
	 *
	 * @param rating the rating
	 * @param fileName the file name
	 * @return the scheduled date from file for alerts
	 */
	private JsonObjectBuilder getScheduledDateFromFileForAlerts(JsonObject rating, String fileName) {

		JsonObjectBuilder job = Json.createObjectBuilder();

		for (Entry<String, JsonValue> entry : rating.entrySet()) {
			job.add(entry.getKey(), entry.getValue());

		}

		String homeTeam = rating.containsKey("home_team_short_name") ? rating.getString("home_team_short_name") : "";
		String awayTeam = rating.containsKey("away_team_short_name") ? rating.getString("away_team_short_name") : "";
		String league = rating.containsKey("league") ? rating.getString("league") : "";
		String sport = rating.containsKey("sport") ? rating.getString("sport") : "";
		String leagueKey = homeTeam + ":" + awayTeam + ":" + league + ":" + sport + ":alert_gex";
		String homeKey = homeTeam + ":" + awayTeam + ":" + league + ":" + sport + ":alert_gex_home";
		String awayKey = homeTeam + ":" + awayTeam + ":" + league + ":" + sport + ":alert_gex_away";
		findLineAndReplaceField(job, fileName, leagueKey, "alert_gex");
		findLineAndReplaceField(job, fileName, homeKey, "alert_gex_home");
		findLineAndReplaceField(job, fileName, awayKey, "alert_gex_away");

		return job;
	}

	/**
	 * Replace field.
	 *
	 * @param job the job
	 * @param foundLine the found line
	 * @param fieldName the field name
	 */
	private void replaceField(JsonObjectBuilder job, String foundLine, String fieldName) {
		if (foundLine != null) {

			String s = foundLine;
			String[] halsScheduleDate = s.split("=");
			job.add(fieldName, halsScheduleDate[1]);

		}
	}

	/**
	 * Find line and replace field.
	 *
	 * @param job the job
	 * @param fileName the file name
	 * @param leagueKey the league key
	 * @param fieldToReplace the field to replace
	 */
	private void findLineAndReplaceField(JsonObjectBuilder job, String fileName, String leagueKey,
			String fieldToReplace) {
		// read file into stream, try-with-resources
		String foundLine = null;
		try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
			// @formatter:off
			foundLine = stream.filter(line -> line.startsWith(leagueKey)).findFirst().get();
			// @formatter:on
		} catch (IOException | NoSuchElementException e) {
		} finally {

		}
		replaceField(job, foundLine, fieldToReplace);

	}

	/**
	 * Gets the scheduled date from file for games.
	 *
	 * @param rating the rating
	 * @param fileName the file name
	 * @return the scheduled date from file for games
	 */
	private JsonObjectBuilder getScheduledDateFromFileForGames(JsonObject rating, String fileName) {

		JsonObjectBuilder job = Json.createObjectBuilder();

		for (Entry<String, JsonValue> entry : rating.entrySet()) {
			job.add(entry.getKey(), entry.getValue());

		}

		String homeTeam = rating.containsKey("homeTeam") && rating.getJsonObject("homeTeam").containsKey("name")
				? rating.getJsonObject("homeTeam").getString("name") : "";
		String awayTeam = rating.containsKey("awayTeam") && rating.getJsonObject("awayTeam").containsKey("name")
				? rating.getJsonObject("awayTeam").getString("name") : "";
		String league = rating.containsKey("league") ? rating.getString("league") : "";
		String sport = rating.containsKey("sport") ? rating.getString("sport") : "";
		String key = homeTeam + ":" + awayTeam + ":" + league + ":" + sport;
		LOGGER.info(String.format("key is %s", key));
		findLineAndReplaceField(job,fileName, key,"scheduledDate");
		return job;

	}

}
