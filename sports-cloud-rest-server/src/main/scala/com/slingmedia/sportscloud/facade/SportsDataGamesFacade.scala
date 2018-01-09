package com.slingmedia.sportscloud.facade

import com.slingmedia.sportscloud.netty.rest.model.{ActiveTeamGame, Role}
import com.google.gson.{JsonElement,JsonParser,JsonObject,JsonArray}
import java.net.URLEncoder
import java.time.Instant
import org.slf4j.LoggerFactory;

import collection.mutable._


object SportsDataGamesFacade {
    private val log = LoggerFactory.getLogger("SportsDataGamesFacade") 

  	private val GAMES_SCHEDULE_INDEX_ENTITY = "game_schedule"
  	private val LIVE_INFO_INDEX_ENTITY="live_info";
  	

	private val GAME_SCHEDULE_INDEX_CONTEXT="sc-game-schedule"

	private val LIVE_INFO_INDEX_CONTEXT="sc-live-info"


	private val INDEX_VERB="_search"
  	private val INDEX_HOST = if(System.getenv("ELASTIC_SEARCH_URL") == null)  "localhost" else System.getenv("ELASTIC_SEARCH_URL")
	private val INDEX_PORT = if(System.getenv("ELASTIC_SEARCH_PORT")==null) "9200" else System.getenv("ELASTIC_SEARCH_PORT")
	
	private val INDEX_HOST_SECONDAY = if(System.getenv("indexingHostSec") == null)  "localhost" else System.getenv("indexingHostSec")
	private val INDEX_PORT_SECONDARY = if(System.getenv("indexingPortSec")==null) "9200" else System.getenv("indexingPortSec")


	private val GAMES_CATEGORIES_FETCH_BASE_URL = "/"+GAME_SCHEDULE_INDEX_CONTEXT+"/"+GAMES_SCHEDULE_INDEX_ENTITY+"/"+INDEX_VERB
	
	private val GAMES_SCHEDULE_CATEGORY_FETCH_BASE_URL = "/"+GAME_SCHEDULE_INDEX_CONTEXT+"/"+GAMES_SCHEDULE_INDEX_ENTITY+"/"+INDEX_VERB
	
	private val LIVE_INFO_FETCH_BASE_URL = "/"+LIVE_INFO_INDEX_CONTEXT+"/"+LIVE_INFO_INDEX_ENTITY+"/"+INDEX_VERB
  	
    private val elasticSearchClient  = ElasticSearchClient()
     
 	
  	def getGamesCategoriesDataForHomeScreen(startDate:Long,endDate:Long): JsonElement = {
  		val searchTemplate =  s"""{
			  "size": 0,
			  "query": {
			    "bool": {
			      "must_not": {
			        "term": {
			          "startTimeEpoch": 0
			        }
			      },
			      "filter": {
			        "range": {
			          "game_date_epoch": {
			            "gte": $startDate,
                  		"lte": $endDate
			          }
			        }
			      }
			    }
			  },
			  "aggs": {
			    "top_tags": {
			      "terms": {
			        "field": "league.keyword",
			        "size": 100
			      }
			    }
			  }
			}""".stripMargin.replaceAll("\n", " ")
		elasticSearchClient.search("POST",getGamesCategoriesURLBase(), Map[String, String](),searchTemplate)	  	
  	}
  	
  	def getGameScheduleDataForCategoryForHomeScreen(startDate:Long,endDate:Long, gameCategory: String): JsonElement = {
  		val searchTemplate =  s"""{
			  "size": 0,
			  "sort": [
			    {
			      "game_date_epoch": {
			        "order": "asc"
			      }
			    }
			  ],
			  "query": {
			    "bool": {
			      "must_not": {
			        "term": {
			          "startTimeEpoch": 0
			        }
			      },
			      "filter": {
			        "bool": {
			          "must": [
			            {
			              "term": {
			                "league.keyword": "$gameCategory"
			              }
			            },
			            {
			              "range": {
			                "game_date_epoch": {
			                   "gte": $startDate,
                  				"lte": $endDate
			                }
			              }
			            }
			          ]
			        }
			      }
			    }
			  },
			  "aggs": {
			    "top_tags": {
			      "terms": {
			        "field": "gameId.keyword",
			        "size": 500,
			        "order": {
			          "order_agg": "asc"
			        }
			      },
			      "aggs": {
			        "order_agg": {
			          "max": {
			            "field": "game_date_epoch"
			          }
			        },
			        "top_game_home_hits": {
			          "top_hits": {
			            "size": 10
			          }
			        }
			      }
			    }
			  }
			}""".stripMargin.replaceAll("\n", " ")
		elasticSearchClient.search("POST",getGamesScheduleForCategoryURLBase(), Map[String, String](),searchTemplate)	  	
  	}
  	
  	
  def getGameScheduleByGameCode(gameId: String): JsonElement = {
  		val searchTemplate =  s"""{
			  "size": 0,
			  "query": {
			    "bool": {
			      "must_not": {
			        "term": {
			          "startTimeEpoch": 0
			        }
			      },
			      "must": {
			        "term": {
			          "gameId.keyword": "$gameId"
			        }
			      }
			    }
			  },
			  "aggs": {
			    "top_tags": {
			      "terms": {
			        "field": "gameId.keyword",
			        "size": 500,
			        "order": {
			          "order_agg": "asc"
			        }
			      },
			      "aggs": {
			        "order_agg": {
			          "max": {
			            "field": "game_date_epoch"
			          }
			        },
			        "game_info": {
			          "top_hits": {
			            "size": 10
			          }
			        }
			      }
			    }
			  }
			}""".stripMargin.replaceAll("\n", " ")
		elasticSearchClient.search("POST",getGamesScheduleForCategoryURLBase(), Map[String, String](),searchTemplate)	  	
  	}
  	
  	def getLiveGameById(gameId:String): JsonElement = {
  		val searchTemplate =  s""" {
		    "size" : 0,
		    "query" : {
		        "term" : { "gameId.keyword" : "$gameId" }
		    },
		    
		    "aggs": {
		    "top_tags": {
		      "terms": {
		        "field": "gameId.keyword",
        		"size" : 500,
		        "order": {
		          "order_agg": "asc"
		        }
		      },
		      "aggs": {
		        "order_agg": {
		          "max": {
		            "field": "game_date_epoch"
		          }
		        },
		        "live_info": {
		          "top_hits": {
		            "size":1
		          }
		        }
		      }
		    }
		  }
		}""".stripMargin.replaceAll("\n", " ")
		elasticSearchClient.search("POST",getLiveInfoURLBase(), Map[String, String](),searchTemplate)		  	
  				  	
  	}
  	
  	def getGamesCategoriesURLBase():StringBuilder  = {
  		new StringBuilder(GAMES_CATEGORIES_FETCH_BASE_URL)
  		
	}
	
	def getGamesScheduleForCategoryURLBase():StringBuilder  = {
  		new StringBuilder(GAMES_SCHEDULE_CATEGORY_FETCH_BASE_URL)
  		
	}
	
	def getLiveInfoURLBase():StringBuilder  = {
  		new StringBuilder(LIVE_INFO_FETCH_BASE_URL)
  	}
	

}