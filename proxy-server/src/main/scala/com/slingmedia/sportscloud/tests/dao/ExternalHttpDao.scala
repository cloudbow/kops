package com.slingmedia.sportscloud.tests.dao

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import java.time.Instant;


import org.slf4j.LoggerFactory;




object ExternalHttpDao {

      private var closeableHttpClient:CloseableHttpClient = null
      val log = LoggerFactory.getLogger("ExternalHttpDao")    
      def init() {
      	val cm:PoolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager();
      	cm.setDefaultMaxPerRoute(2);
      	cm.setMaxTotal(10);
      	closeableHttpClient = HttpClients.custom().setConnectionManager(cm).build();
      	log.trace(s"Initialized ExternalHttpClient $closeableHttpClient")
      }
      
      
      def  get(url:String):String = {
         log.trace(s"Calling  url $url")
      	 var responseString:String = null ;
      	 var httpResponse:CloseableHttpResponse  = null;
      	 val httpGet:HttpGet = new HttpGet(url);
      	 try {
      	 	var startTime = 0
      	 	var endTime= 0
      	 	if(log.isTraceEnabled()){
      	 		startTime = Instant.now().getNano
      	 	}
      	 	httpResponse = closeableHttpClient.execute(httpGet);
      	 	
      	 	if(httpResponse!=null) {
				responseString = EntityUtils.toString(httpResponse.getEntity(),"UTF-8");
		    	EntityUtils.consume(httpResponse.getEntity());
		    }
		    if(log.isTraceEnabled()){
      	 		endTime = Instant.now().getNano
      	 		val totalTime = endTime-startTime
      	 		log.trace(s"Total time taken: $totalTime" )
      	 	}
      	 	
      	 }  finally {
      	 	if(httpResponse!=null) httpResponse.close()     	 	
      	 }
      	 responseString     	       	 
      }
      
 }