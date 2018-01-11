/*
 * ClearInvalidSolrDocuments.java
 * @author arung
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
package com.slingmedia.sportscloud.utils;

import java.net.URLEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.slingmedia.sportscloud.facade.*;

/**
 * Utility for testing indexing layer
 * 
 * @author arung
 * @version 1.0
 * @since 1.0
 */
public class ClearInvalidSolrDocuments {

	/** The constant LOGGER */
	private static final Logger LOGGER = LoggerFactory.getLogger(ClearInvalidSolrDocuments.class);

	/**
	 * The main method for testing indexing response
	 * 
	 * @param args
	 *            the main arguments
	 */
	public static void main(String[] args) {
		ClearInvalidSolrDocuments cisc = new ClearInvalidSolrDocuments();
		cisc.doIt();

	}

	/**
	 * The test method for indexing documents
	 */
	private void doIt() {
		ExternalHttpClient$.MODULE$.init();
		StringBuilder builder = new StringBuilder();
		// builder.append("http://cqaneat02.sling.com:8983/solr/game_schedule/select?q=game_date_epoch:[123%20TO%20*]&fl=id&start=0&rows=500&wt=json");
		// builder.append("http://cqhlsdatacenter01.sling.com:8983/solr/game_schedule/select?q=gameCode:370823103&wt=json");
		builder.append(
				"http://cqhlsdatacenter01.sling.com:8983/solr/game_schedule/select?q=batchTime:1504004153&start=0&rows=3000&wt=json");

		JsonElement jsonElement = getJsonObject(builder);
		jsonElement.getAsJsonObject().get("hits").getAsJsonObject().get("hits").getAsJsonArray().forEach(it -> {

			String id = it.getAsJsonObject().get("id").getAsString();
			if (id.split("_").length == 3) {
				// delete
				StringBuilder deleteRequestBuilder = new StringBuilder();
				deleteRequestBuilder
						.append("http://cqhlsdatacenter01.sling.com:8983/solr/game_schedule/update?stream.body=")
						.append(URLEncoder.encode("<delete><query>")).append("id:%22" + id + "%22")
						.append(URLEncoder.encode("</query></delete>")).append("&commit=true");
				String response = ExternalHttpClient$.MODULE$.getFromUrl(deleteRequestBuilder.toString());
				LOGGER.info(response);
			}

		});

	}

	/**
	 * Returns the JSON response for the given URL
	 * 
	 * @param requestURLBuilder
	 *            the URL
	 * @return the JSON response for the given URL
	 */
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
}
