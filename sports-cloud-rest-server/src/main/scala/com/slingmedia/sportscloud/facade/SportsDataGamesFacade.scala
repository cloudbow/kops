/*
 * SportsDataGamesFacade.scala
 * @author jayachandra
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

import com.slingmedia.sportscloud.netty.rest.model.{ActiveTeamGame, Role}
import com.google.gson.{JsonElement,JsonParser,JsonObject,JsonArray}
import java.net.URLEncoder
import java.time.Instant
import org.slf4j.LoggerFactory;

import collection.mutable._

/**
 * Performs search queries for Sling TV sports 
 * 
 * @author jayachandra
 * @version 1.0
 * @since 1.0
 */
object SportsDataGamesFacade {
    private val log = LoggerFactory.getLogger("SportsDataGamesFacade") 

  	private val GAMES_SCHEDULE_INDEX_ENTITY = "game_schedule"
  	private val LIVE_INFO_INDEX_ENTITY="live_info";
  	

	private val GAME_SCHEDULE_INDEX_CONTEXT="sc-game-schedule"

	private val LIVE_INFO_INDEX_CONTEXT="sc-live-info"


	private val INDEX_VERB="_search"

	private val GAMES_CATEGORIES_FETCH_BASE_URL = "/"+GAME_SCHEDULE_INDEX_CONTEXT+"/"+GAMES_SCHEDULE_INDEX_ENTITY+"/"+INDEX_VERB
	
	private val GAMES_SCHEDULE_CATEGORY_FETCH_BASE_URL = "/"+GAME_SCHEDULE_INDEX_CONTEXT+"/"+GAMES_SCHEDULE_INDEX_ENTITY+"/"+INDEX_VERB
	
	private val LIVE_INFO_FETCH_BASE_URL = "/"+LIVE_INFO_INDEX_CONTEXT+"/"+LIVE_INFO_INDEX_ENTITY+"/"+INDEX_VERB
  	
    private val elasticSearchClient  = ElasticSearchClient()
     
 	
 	/**
	  * Fetches game categories for given date range
	  *
	  * @param startDate the start date
	  * @param endDate the end date
	  * @return the result in JSON format
	  */
  	def getGamesCategoriesDataForHomeScreen(startDate:Long,endDate:Long): JsonElement = {
  		val searchTemplate =  s"""{
			  "size": 0,
			  "query": {
			    "bool": {
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
  	
  	/**
	  * Fetches game schedules for given date range and category
	  *
	  * @param startDate the start date
	  * @param endDate the end date
	  * @param gameCategory the game category
	  * @return the result in JSON format
	  */
  	def getGameScheduleDataForCategoryForHomeScreen(startDate:Long,endDate:Long, gameCategory: String): JsonElement = {
  		val searchTemplate =  s"""{
			  "size": 0,
			  "sort": [
			    {
			      "game_date_epoch": {
			        "order": "asc"
			      },
			      "channel_no": {
			        "order": "asc"
			      },
			      "id.keyword": {
			        "order": "asc"
			      }
			    }
			  ],
			  "query": {
			    "bool": {
			      "filter": {
			        "bool": {
			          "must": [
			            {
			              "match": {
			                "league": "$gameCategory"
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
			          ],
			          "must_not": [
			            {
			              "term": {
			                "homeTeamName.keyword": ""
			              }
			            },
			            {
			              "term": {
			                "awayTeamName.keyword": ""
			              }
			            }
			          ]
			        }
			      }
			    }
			  },
			  "aggs": {
			    "group_by_gameId": {
			      "terms": {
			        "field": "gameId.keyword",
			        "size": 500
			      },
			      "aggs": {
			        "order_by_game_date_agg": {
			          "max": {
			            "field": "game_date_epoch"
			          }
			        },			      	
			        "top_hits_by_batch_time": {
			          "terms": {
			            "field": "batchTime",
			            "size": 1,
			            "order": {
			              "order_agg": "desc"
			            }
			          },
			          "aggs": {
			            "order_agg": {
			              "max": {
			                "field": "batchTime"
			              }
			            },
			            "top_game_hits": {
			              "top_hits": {
			                "size": 10
			              }
			            }
			          }
			        }
			      }
			    }
			  }
			}""".stripMargin.replaceAll("\n", " ")
		elasticSearchClient.search("POST",getGamesScheduleForCategoryURLBase(), Map[String, String](),searchTemplate)	  	
  	}
  	
  	
  	/**
	  * Fetches game schedule for specific game
	  *
	  * @param gameId the game id
	  * @return the result in JSON format
	  */
  def getGameScheduleByGameCode(gameId: String): JsonElement = {
  		val searchTemplate =  s"""{
			  "size": 0,
			  "query": {
			    "bool": {
			      "must": {
			        "term": {
			          "gameId.keyword": "$gameId"
			        }
			      }
			    }
			  },
			  "aggs": {
			    "group_by_gameId": {
			      "terms": {
			        "field": "gameId.keyword",
			        "size": 500
			      },
			      "aggs": {
			        "order_by_game_date_agg": {
			          "max": {
			            "field": "game_date_epoch"
			          }
			        },			      	
			        "top_hits_by_batch_time": {
			          "terms": {
			            "field": "batchTime",
			            "size": 1,
			            "order": {
			              "order_agg": "desc"
			            }
			          },
			          "aggs": {
			            "order_agg": {
			              "max": {
			                "field": "batchTime"
			              }
			            },
			            "top_game_hits": {
			              "top_hits": {
			                "size": 10
			              }
			            }
			          }
			        }
			      }
			    }
			  }
			}""".stripMargin.replaceAll("\n", " ")
		elasticSearchClient.search("POST",getGamesScheduleForCategoryURLBase(), Map[String, String](),searchTemplate)	  	
  	}
  	
  	/**
	  * Fetches live info for specific game
	  *
	  * @param gameId the game id
	  * @return the result in JSON format
	  */
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
  	
  	/**
	  * Returns elastic search url for categories
	  */
  	def getGamesCategoriesURLBase():StringBuilder  = {
  		new StringBuilder(GAMES_CATEGORIES_FETCH_BASE_URL)
  		
	}
	
	/**
	  * Returns elastic search url for game schedules
	  */
	def getGamesScheduleForCategoryURLBase():StringBuilder  = {
  		new StringBuilder(GAMES_SCHEDULE_CATEGORY_FETCH_BASE_URL)
  		
	}
	
	/**
	  * Returns elastic search url for live info
	  */
	def getLiveInfoURLBase():StringBuilder  = {
  		new StringBuilder(LIVE_INFO_FETCH_BASE_URL)
  	}
	

}