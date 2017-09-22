package com.slingmedia.sportscloud.utils;

import java.net.URLEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.slingmedia.sportscloud.facade.*;


public class ClearInvalidSolrDocuments {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ClearInvalidSolrDocuments.class);
	public static void main(String[] args) {
		ClearInvalidSolrDocuments cisc = new ClearInvalidSolrDocuments();
		cisc.doIt();
		
	}
	
	private void doIt() {
		ExternalHttpClient$.MODULE$.init();
		StringBuilder builder = new StringBuilder();
		//builder.append("http://cqaneat02.sling.com:8983/solr/game_schedule/select?q=game_date_epoch:[123%20TO%20*]&fl=id&start=0&rows=500&wt=json");
		//builder.append("http://cqhlsdatacenter01.sling.com:8983/solr/game_schedule/select?q=gameCode:370823103&wt=json");
		builder.append("http://cqhlsdatacenter01.sling.com:8983/solr/game_schedule/select?q=batchTime:1504004153&start=0&rows=3000&wt=json");

		JsonElement jsonElement = getJsonObject(builder);
		jsonElement.getAsJsonObject().get("hits").getAsJsonObject().get("hits").getAsJsonArray().forEach(it -> {
			
			String id = it.getAsJsonObject().get("id").getAsString();
			if(id.split("_").length==3){
				//delete
				StringBuilder deleteRequestBuilder  = new StringBuilder();
				deleteRequestBuilder.append("http://cqhlsdatacenter01.sling.com:8983/solr/game_schedule/update?stream.body=")
				.append(URLEncoder.encode("<delete><query>"))
				.append("id:%22"+id+"%22")
				.append(URLEncoder.encode("</query></delete>"))
				.append("&commit=true");
				String response = ExternalHttpClient$.MODULE$.getFromUrl(deleteRequestBuilder.toString());
				LOGGER.info(response);
			}
			
		});
		
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
}
