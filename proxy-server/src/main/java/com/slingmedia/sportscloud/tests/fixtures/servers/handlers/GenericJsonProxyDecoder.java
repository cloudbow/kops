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

import java.net.URL;
import java.util.NoSuchElementException;


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

						String homeTeamName = "-" ;
						if(mappingObjItem.getAsJsonObject().get("homeTeamName")!=null) {
							homeTeamName = mappingObjItem.getAsJsonObject().get("homeTeamName").getAsString();
						}
						
						String awayTeamName = "-";
						if(mappingObjItem.getAsJsonObject().get("awayTeamName")!=null) {
							awayTeamName = mappingObjItem.getAsJsonObject().get("awayTeamName").getAsString();
						}
						
						
						
						String homeTeamScore = "0";
						if(mappingObjItem.getAsJsonObject().get("homeTeamScore")!=null) {
							homeTeamScore = mappingObjItem.getAsJsonObject().get("homeTeamScore").getAsJsonObject().get("$numberLong").getAsString();
						}
								
								
						String awayTeamScore = "0";
						if(mappingObjItem.getAsJsonObject().get("awayTeamScore")!=null) {
							awayTeamScore = mappingObjItem.getAsJsonObject().get("awayTeamScore").getAsJsonObject().get("$numberLong").getAsString();
						}
						
								
						String awayTeamPitcherName = "-";
						if(mappingObjItem.getAsJsonObject().get("awayTeamPitcherName")!=null) {
							awayTeamPitcherName = mappingObjItem.getAsJsonObject().get("awayTeamPitcherName").getAsString().split(" ")[0];
						}
								
								
						String homeTeamPitcherName = "-";
						if(mappingObjItem.getAsJsonObject().get("homeTeamPitcherName")!=null) {
							homeTeamPitcherName= mappingObjItem.getAsJsonObject().get("homeTeamPitcherName").getAsString().split(" ")[0];
						}
								
						String homeTeamImg = "-" ;
						if(mappingObjItem.getAsJsonObject().get("homeTeamImg")!=null) {
							homeTeamImg = mappingObjItem.getAsJsonObject().get("homeTeamImg").getAsString();
						}
								
								
						String awayTeamImg = "-";
						if(mappingObjItem.getAsJsonObject().get("awayTeamImg")!=null) {
							awayTeamImg=mappingObjItem.getAsJsonObject().get("awayTeamImg").getAsString();
						}
						
						String gexPredict = "0";
						if(mappingObjItem.getAsJsonObject().get("gexPredict")!=null) {
							gexPredict=mappingObjItem.getAsJsonObject().get("gexPredict").getAsJsonObject().get("$numberLong").getAsString();
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

	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.close();
	}

}
