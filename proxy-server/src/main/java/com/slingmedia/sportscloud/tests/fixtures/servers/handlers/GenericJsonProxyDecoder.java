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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Scanner;

import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
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
import io.netty.util.CharsetUtil;
import pns.alltypes.netty.httpclient.config.HostConfig;
import pns.alltypes.netty.httpclient.config.SyncType;
import pns.alltypes.netty.httpclient.exception.AlreadyRegisteredHostException;
import pns.alltypes.netty.httpclient.exception.InvalidResponseException;
import pns.alltypes.netty.httpclient.request.HttpRequestMessage.Builder;
import pns.alltypes.netty.httpclient.request.RequestMaker;
import pns.alltypes.netty.httpclient.response.ResponseMsg;

/**
 * The Class GenericJsonProxyDecoder.
 *
 * @author arung
 */
public class GenericJsonProxyDecoder extends SimpleChannelInboundHandler<FullHttpRequest> {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(GenericJsonProxyDecoder.class);

	/** The Constant REQUEST_MAKER. */
	private static final RequestMaker REQUEST_MAKER = RequestMaker.getInstance();

	//@formatter:off
	private static final Configuration CONFIGURAION_OPTION = Configuration.builder()
			.options(Option.AS_PATH_LIST).build();
	private static final Configuration CONFIG_NO_OPT = Configuration.builder()
			 .jsonProvider(new JacksonJsonNodeJsonProvider())
			 .mappingProvider(new JacksonMappingProvider())
		     .build();
    //@formatter:on

	private static CloseableHttpAsyncClient httpClient;


	GenericJsonProxyDecoder() {

		httpClient = HttpAsyncClients.createDefault();
		httpClient.start();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.netty.channel.SimpleChannelInboundHandler#channelRead0(io.netty.
	 * channel.ChannelHandlerContext, java.lang.Object)
	 */
	@Override
	protected void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest request) throws Exception {

		HttpResponse response = null;
		response = new DefaultHttpResponse(request.protocolVersion(), HttpResponseStatus.OK);
		response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "application/json");

		final boolean keepAlive = HttpHeaders.isKeepAlive(request);
		//@formatter:on
		StringBuilder requestURLBuilder = new StringBuilder();
		requestURLBuilder.append(JsonProxyServerConfiguration.getTARGET_HOST_TO_PROXY()).append(request.uri());

		//@formatter:off
		
		URL baseURLToProxy = new URL(JsonProxyServerConfiguration.getTARGET_HOST_TO_PROXY());
		
		HostConfig hostConfig = null;
		try {
			hostConfig = new HostConfig(baseURLToProxy.getHost(), baseURLToProxy.getPort()==-1?80:baseURLToProxy.getPort(), SyncType.OPENCLOSE);
			REQUEST_MAKER.registerHost(hostConfig);
		} catch (final AlreadyRegisteredHostException e) {
			LOGGER.error("Already registered",e);
		}
		final Builder registerCallBack = REQUEST_MAKER.registerCallBack(request.method(), requestURLBuilder.toString());
		registerCallBack.url().headers().body(request.content().toString(CharsetUtil.UTF_8)).addHeader("host", hostConfig.getHost());
		ResponseMsg requestSync = null;
		try {
			requestSync = REQUEST_MAKER.requestSync(registerCallBack.build(), hostConfig);
		} catch (final InvalidResponseException e) {

		}
		String responseString = requestSync.getResponse();

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(responseString);
		}

		if (keepAlive) {
			response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
		} else {
			response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
		}

		try (Scanner scanner = new Scanner(new File(System.getProperty("json-path-file")))) {

			while (scanner.hasNext()) {
				String nextLine = scanner.nextLine();
				String[] lines = nextLine.split("<::>");
				String predicates = lines[0];
				String targetOffsetPath = lines[1];
				String replacementValue = lines[2];
				ReplaceType replaceType = ReplaceType.valueOf(lines[3]);
				if (replaceType == ReplaceType.BLIND_REPLACE) {
					try {
					Object updatedJson = JsonPath.using(CONFIG_NO_OPT).parse(responseString)
							.set(targetOffsetPath, replacementValue).json();
					responseString = updatedJson.toString();
					} catch(com.jayway.jsonpath.PathNotFoundException e){
						LOGGER.error("path not found",e);
					}
				} else if (replaceType == ReplaceType.PREDICATE_REPLACE) {
					try {
					List<String> pathList = JsonPath.using(CONFIGURAION_OPTION).parse(responseString).read(predicates);
					for (String path : pathList) {
						Object updatedJson = JsonPath.using(CONFIG_NO_OPT).parse(responseString)
								.set(path.concat(targetOffsetPath), replacementValue).json();
						responseString = updatedJson.toString();
						LOGGER.trace(updatedJson.toString());
					}
					}catch(com.jayway.jsonpath.PathNotFoundException e){
						LOGGER.error("path not found",e);
					}
				}
			}

		} catch (IOException e) {
			LOGGER.error("IOException ",e);
		}

		byte[] bytes = responseString.toString().getBytes();
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

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.close();
	}

}
