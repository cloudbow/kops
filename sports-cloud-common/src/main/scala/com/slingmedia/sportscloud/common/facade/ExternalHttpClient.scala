package com.slingmedia.sportscloud.common.facade

import com.slingmedia.sportscloud.common.dao.ExternalHttpDao

object ExternalHttpClient {

	private val externalHttpClientDao = ExternalHttpDao
	
	def getFromUrl(url:String) = {
		externalHttpClientDao.get(url)
	}
	
	def init() = {
		externalHttpClientDao.init()
	}

} 
