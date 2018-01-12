/*
 * ElasticSearchClient.scala
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
package com.slingmedia.sportscloud.facade

import com.google.gson.{JsonElement,JsonParser,JsonObject}

import org.slf4j.LoggerFactory;

import collection.JavaConverters._
import collection.mutable._

import org.elasticsearch.client.{RestClient,RestClientBuilder}
import org.apache.http.{HttpHost,Header}
import org.apache.http.entity.{ContentType}
import org.apache.http.nio.entity.{NStringEntity}

import org.apache.http.message.{BasicHeader}
import org.apache.http.auth.{AuthScope,UsernamePasswordCredentials}
import org.apache.http.impl.nio.client.{HttpAsyncClientBuilder}
import org.apache.http.client.{CredentialsProvider}
import org.apache.http.util.{EntityUtils}
import org.apache.http.impl.client.{BasicCredentialsProvider}

/**
 * Elastic search client for Sports data
 * 
 * @author arung
 * @version 1.0
 * @since 1.0
 */
object ElasticSearchClient {

    private val log = LoggerFactory.getLogger("ElasticSearchClient") 
    private val credentialsProvider = new BasicCredentialsProvider();
    private var nativeElasticClient:RestClient  = null
    
	/** Elastic search configuration for access*/
    class RestClientConfigCallback extends RestClientBuilder.HttpClientConfigCallback {

		override def  customizeHttpClient(httpClientBuilder:HttpAsyncClientBuilder):HttpAsyncClientBuilder = {
             httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
    	}

	}
	
	/**
      * Creates this Object and initializes Elastic search client
      */
	def apply() = {
		credentialsProvider.setCredentials(AuthScope.ANY,
        new UsernamePasswordCredentials("elastic", "changeme"))
		val builder = RestClient.builder(new HttpHost(System.getenv("ELASTIC_SEARCH_URL"), System.getenv("ELASTIC_SEARCH_PORT").toInt))
		builder.setHttpClientConfigCallback(new RestClientConfigCallback())       
	    val defaultHeaders = Array(new BasicHeader("User-Agent", "SCElasticSearch/v1.0").asInstanceOf[Header])
		builder.setDefaultHeaders(defaultHeaders)
		nativeElasticClient = builder.build()
		this
    }  
    
    /**
	  * Returns the results from Elastic search for given search criteria
	  *
	  * @param method the HTTP method
	  * @param urlFrag the URL
	  * @param params the config params 
	  * @param jsonString the JSON search string
	  * @return the result in JSON format
	  */
    def search(method:String,urlFrag:StringBuilder,params:Map[String,String], jsonString:String):JsonElement = {
        val entity = new NStringEntity(jsonString, ContentType.APPLICATION_JSON);
        var responseJsonStr:String = null
        try { 
        	log.trace(s"Calling  url ${urlFrag.toString} $jsonString")
			val response = nativeElasticClient.performRequest(method, urlFrag.toString , params.asJava, entity);
			val statusCode = response.getStatusLine().getStatusCode();			
			responseJsonStr = EntityUtils.toString(response.getEntity())
		} catch {
		 	case e: Exception => log.error("Error ocurred in parsing",e)		 		
		}
		getJsonObjectV2(responseJsonStr)  
    }
    
    /**
	  * Parses JSON string to JSON element
	  *
	  * @param responseString the JSON string
	  * @return the JSON element
	  */
    def getJsonObjectV2(responseString:String): JsonElement = {
  		val parser = new JsonParser();
		var responseJson = parser.parse("{}");
		try {
			responseJson = parser.parse(responseString);
		} catch {
      		case e: Exception => log.error("Error ocurred in parsing",e)
    	}
		return responseJson;
  	}
  	
  	/**
  	* Destroys the Elastic search client
  	*/
  	def destroy() = {
  		nativeElasticClient.close()
  	}

} 
