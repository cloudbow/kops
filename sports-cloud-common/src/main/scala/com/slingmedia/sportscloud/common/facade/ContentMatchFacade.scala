package com.slingmedia.sportscloud.common.facade
import com.slingmedia.sportscloud.common.dao.MongoDao


import com.google.gson.{JsonElement}


object ContentMatchFacade {

	private val mongoDao = MongoDao
	private val SLINGTV_SPORTS_COLL="slingtv_sports_events" 
  	
  	def getDataForChannelGuidAndProgramID(channelGuid: String, programId: String): JsonElement = {
    	val key: String = channelGuid + "_" + programId
    	val x = mongoDao.getDataById(key, SLINGTV_SPORTS_COLL)
    	x
  	}
  	
  	def init(url:String) = {
  		mongoDao.init(url)
  	}	
}