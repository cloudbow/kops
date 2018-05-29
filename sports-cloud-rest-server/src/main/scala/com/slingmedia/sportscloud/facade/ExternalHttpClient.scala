/*
 * ExternalHttpClient.scala
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

import com.slingmedia.sportscloud.dao.{ExternalHttpDao}

/**
 * HTTP client for external services
 * 
 * @author arung
 * @version 1.0
 * @since 1.0
 */
object ExternalHttpClient {

	private val externalHttpClientDao = ExternalHttpDao()
	
	/**
      * Initializes HTTP connection pool
      * @param url the external URL
      */
	def getFromUrl(url:String) = {
		externalHttpClientDao.get(url)
	}
	
	/**
      * Initializes Data access object
      */
	def init() = {
		externalHttpClientDao.init()
	}

} 
