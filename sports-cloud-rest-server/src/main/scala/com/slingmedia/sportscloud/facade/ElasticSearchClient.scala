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

object ElasticSearchClient {

    private val log = LoggerFactory.getLogger("ElasticSearchClient") 
    private val credentialsProvider = new BasicCredentialsProvider();
    

    class RestClientConfigCallback extends RestClientBuilder.HttpClientConfigCallback {

		override def  customizeHttpClient(httpClientBuilder:HttpAsyncClientBuilder):HttpAsyncClientBuilder = {
             httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
    	}

	}
	
   private var nativeElasticClient:RestClient  = null

	
	def apply() = {
		credentialsProvider.setCredentials(AuthScope.ANY,
        new UsernamePasswordCredentials("elastic", "changeme"))
		val builder = RestClient.builder(new HttpHost(System.getProperty("indexingHost"), System.getProperty("indexingPort").toInt))
		builder.setHttpClientConfigCallback(new RestClientConfigCallback())       
	    val defaultHeaders = Array(new BasicHeader("User-Agent", "SCElasticSearch/v1.0").asInstanceOf[Header])
		builder.setDefaultHeaders(defaultHeaders)
		nativeElasticClient = builder.build()
		this
    }  
    
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
  	
  	def destroy() = {
  		nativeElasticClient.close()
  	}

} 
