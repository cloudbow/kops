/*
 * ExternalHttpDao.scala
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
package com.slingmedia.sportscloud.dao

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import java.time.Instant;


import org.slf4j.LoggerFactory;

/**
 * Fetches data from external services
 * 
 * @author arung
 * @version 1.0
 * @since 1.0
 */
object ExternalHttpDao {

      private var closeableHttpClient:CloseableHttpClient = null
      val log = LoggerFactory.getLogger("ExternalHttpDao")    
      
      /**
      * Creates this Object and initializes HTTP connection pool
      */
      def apply() = {
      	init()
      	this
      }
      
      /**
      * Initializes HTTP connection pool
      */
      def init() {
      	val cm:PoolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager();
      	cm.setDefaultMaxPerRoute(2);
      	cm.setMaxTotal(10);
      	closeableHttpClient = HttpClients.custom().setConnectionManager(cm).build();
      	log.trace(s"Initialized ExternalHttpClient $closeableHttpClient")
      }
      
      
      /**
      * Fetches data from external service
      *
      * @param url the external URL
      * @return the response in JSON string format
      */
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