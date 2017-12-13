package com.slingmedia.sportscloud.facade

import com.slingmedia.sportscloud.dao.{ExternalHttpDao}

object ExternalHttpClient {

	private val externalHttpClientDao = ExternalHttpDao()
	
	def getFromUrl(url:String) = {
		externalHttpClientDao.get(url)
	}
	
	def init() = {
		externalHttpClientDao.init()
	}

} 
