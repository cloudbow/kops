package com.slingmedia.sportscloud.tests.facade

import com.slingmedia.sportscloud.tests.dao.ExternalHttpDao

object ExternalHttpClient {

	private val externalHttpClientDao = ExternalHttpDao
	
	def getFromUrl(url:String) = {
		externalHttpClientDao.get(url)
	}
	
	def init() = {
		externalHttpClientDao.init()
	}

} 
