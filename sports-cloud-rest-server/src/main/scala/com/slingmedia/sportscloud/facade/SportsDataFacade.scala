/*
 * SportsDataFacade.scala
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

import com.slingmedia.sportscloud.netty.rest.model.{ActiveTeamGame, Role}
import com.google.gson.{JsonElement,JsonParser,JsonObject,JsonArray}
import java.net.URLEncoder
import java.time.Instant
import org.slf4j.LoggerFactory;

import collection.mutable._

/**
 * Performs search queries for Sports web-view
 * 
 * @author arung
 * @version 1.0
 * @since 1.0
 */
object SportsDataFacade {
    private val log = LoggerFactory.getLogger("SportsDataFacade") 

	private val GAME_SCHEDULE_INDEX_ENTITY = "game_schedule"
	private val LIVE_INFO_INDEX_ENTITY="live_info";
	private val TEAM_STANDINGS_INDEX_ENTITY="team_standings";
	private val PLAYER_STATS_INDEX_ENTITY="player_stats";
	private val SCORING_EVENTS_INDEX_ENTITY="scoring_events";

	private val GAME_SCHEDULE_INDEX_CONTEXT="sc-game-schedule";
	private val PLAYER_STATS_INDEX_CONTEXT="sc-player-stats";
	private val TEAM_STANDINGS_INDEX_CONTEXT="sc-team-standings";
	private val LIVE_INFO_INDEX_CONTEXT="sc-live-info";
	private val SCORING_EVENTS_INDEX_CONTEXT="sc-scoring-events";


	private val INDEX_VERB="_search"
  	private val INDEX_HOST = if(System.getProperty("ELASTIC_SEARCH_URL") == null)  "localhost" else System.getProperty("ELASTIC_SEARCH_URL")
	private val INDEX_PORT = if(System.getProperty("ELASTIC_SEARCH_PORT")==null) "9200" else System.getProperty("ELASTIC_SEARCH_PORT")
	
	private val INDEX_HOST_SECONDAY = if(System.getProperty("indexingHostSec") == null)  "localhost" else System.getProperty("indexingHostSec")
	private val INDEX_PORT_SECONDARY = if(System.getProperty("indexingPortSec")==null) "9200" else System.getProperty("indexingPortSec")


	private val GAME_SCHEDULE_FETCH_BASE_URL = "/"+GAME_SCHEDULE_INDEX_CONTEXT+"/"+GAME_SCHEDULE_INDEX_ENTITY+"/"+INDEX_VERB
  	private val PLAYER_STATS_FETCH_BASE_URL = "/"+PLAYER_STATS_INDEX_CONTEXT+"/"+PLAYER_STATS_INDEX_ENTITY+"/"+INDEX_VERB
	private val TEAM_STANDINGS_FETCH_BASE_URL = "/"+TEAM_STANDINGS_INDEX_CONTEXT+"/"+TEAM_STANDINGS_INDEX_ENTITY+"/"+INDEX_VERB
	private val LIVE_INFO_FETCH_BASE_URL = "/"+LIVE_INFO_INDEX_CONTEXT+"/"+LIVE_INFO_INDEX_ENTITY+"/"+INDEX_VERB
	private val SCORING_EVENTS_FETCH_BASE_URL = "/"+SCORING_EVENTS_INDEX_CONTEXT+"/"+SCORING_EVENTS_INDEX_ENTITY+"/"+INDEX_VERB

    private val elasticSearchClient  = ElasticSearchClient()
     
 	/**
	  * Fetches live score events for game
	  *
	  * @param gameId the game id
	  * @return the result in JSON format
	  */
  	def getAllScoringEventsForGame(gameId:String): JsonElement = {
  		
  		val searchTemplate =  s""" {
  			"size"  : 100,
    		"query" : {
        		"term" : { "gameId" : "$gameId" }
    		},
    		"sort" : [
        		{ "srcTime" : {"order" : "desc"}}
    		]
		} """.stripMargin.replaceAll("\n", " ")
  	
		elasticSearchClient.search("POST",getScoringEventsURLBase(), Map[String, String](),searchTemplate)		  	
  	}
  	
  	/**
	  * Fetches leagues for active game
	  *
	  * @param activeGame the active game
	  * @return the result in JSON format
	  */
  	def getMainLeaguesForActiveGame(activeGame:ActiveTeamGame): JsonElement = {
  		val searchTemplate =  s"""{
		  "size": 0,
		  "query": {
		    "terms": {
		      "id": [
		        "${activeGame.getHomeTeamId()}",
		        "${activeGame.getAwayTeamId()}"
		      ]
		    }
		  },
		  "aggs": {
		        "top_tags": {
		            "terms": {
		                "field": "subLeague.keyword",
        				"size" : 500
		            }
		        }
		    }
		}""".stripMargin.replaceAll("\n", " ")
  	
		elasticSearchClient.search("POST",getTeamStandingsURLBase(), Map[String, String](),searchTemplate)
  	
  	}
  	
  	/**
	  * Fetches sub leagues grouped for league
	  *
	  * @param subLeague the sub league
	  * @return the result in JSON format
	  */
  	def getSubLeagues(subLeague: String):JsonElement = {
  		val searchTemplate =  s"""{
		  "size": 0,
		  "query": {
		    "term": {
		      "subLeague.keyword": "$subLeague"
		    }
		  },
		  "aggs": {
		        "top_tags": {
		            "terms": {
		                "field": "division.keyword",
        				"size" : 100
		            },"aggs": {
		                "top_division_hits": {
		                    "top_hits": {
		                        "sort": [
		                            {
		                                "date": {
		                                    "order": "desc"
		                                }
		                            }
		                        ],
		                        "size" : 100
		                    }
		                }
		            }
		        }
		    }
		}""".stripMargin.replaceAll("\n", " ")  	
		elasticSearchClient.search("POST",getTeamStandingsURLBase(), Map[String, String](),searchTemplate)
  	
  	}
  	
  	/**
	  * Fetches player stats for player id
	  *
	  * @param playerId the player id
	  * @return the result in JSON format
	  */
  	def getPlayerStatsById(playerId:String): JsonElement = {
  		val searchTemplate =  s""" { 
			  "size"  : 10,
		  "_source":  [ "wins", "losses" ],
		    "query" : {
		        "term" : { "id" : "$playerId" }
		    }
		}""".stripMargin.replaceAll("\n", " ")
		elasticSearchClient.search("POST",getPlayerStatsURLBase(), Map[String, String](),searchTemplate)		  	
  	
  	}

  	
  	/**
	  * Fetches live game info for given game id
	  *
	  * @param gameId the game id
	  * @return the result in JSON format
	  */
  	def getLiveGameById(gameId:String): JsonElement = {
  		val searchTemplate =  s""" { 
  		 		"size"  : 10,
			    "query" : {
			        "term" : { "gameId" : "$gameId" }
			    }
		}""".stripMargin.replaceAll("\n", " ")
		elasticSearchClient.search("POST",getLiveInfoURLBase(), Map[String, String](),searchTemplate)		  	
  				  	
  	}
  	
  	/**
	  * Fetches all live games for given date range
	  *
	  * @param startDate the start date
	  * @param endDate the end date
	  * @param sizeToReturn the limit for results
	  * @return the result in JSON format
	  */
  	def getAllLiveGamesInDateRange(startDate:Long, endDate:Long, sizeToReturn: Int): JsonElement = {
  		val searchTemplate =  s"""{ 
		  "size": $sizeToReturn,
		  "_source":  [ "gameId","homeScoreRuns","awayScoreRuns","statusId","homeScore","awayScore", "drives" ],
		  "query" : {
		        "constant_score": {
		            "filter": {
		             
		                 "range": {
		                    "game_date_epoch": {
		                        "gte": $startDate,
		                        "lte": $endDate
		                    }
		               }
		            }
		        }
		    }
		
		}""".stripMargin.replaceAll("\n", " ")
		elasticSearchClient.search("POST",getLiveInfoURLBase(), Map[String, String](),searchTemplate)		  		  	
  	}
  	
  	/**
	  * Fetches live info for active team
	  *
	  * @param activeGame the active game
	  * @return the result in JSON format
	  */
  	def getLiveInfoForActiveTeam(activeGame: ActiveTeamGame ): JsonElement = {
  		val searchTemplate =  s"""{
		  "size": 10,
		  "query": {
		    "constant_score": {
		      "filter": {
		        "bool": {
		          "must": [
		            {
		              "term": {
		                "gameType.keyword": "${activeGame.getGameType().getGameTypeStr()}"
		              }
		            }
		          ],
		          "should": [
		            {
		              "term": {
		                "homeTeamExtId": "${activeGame.getActiveTeamId()}"
		              }
		            },
		            {
		              "term": {
		                "awayTeamExtId": "${activeGame.getActiveTeamId()}"
		              }
		            }
		          ]
		        }
		      }
		    }
		  }
		}""".stripMargin.replaceAll("\n", " ")
		elasticSearchClient.search("POST",getLiveInfoURLBase(), Map[String, String](),searchTemplate)		  		  	 				  	
  	}
  	
  	/**
	  * Fetches game schedule for specific game
	  *
	  * @param gameId the game id
	  * @return the result in JSON format
	  */
  	def getGameScheduleByGameCode(gameId: String): JsonElement = {
  		val searchTemplate =  s"""{ 
		    "size" : 10,
		    "query" : {
		        "term" : { "gameId" : "$gameId" }
		    }
		}""".stripMargin.replaceAll("\n", " ")
		elasticSearchClient.search("POST",getGameScheduleURLBase(), Map[String, String](),searchTemplate)	  	
  	}
  	
  	/**
	  * Fetches game schedules for given date range
	  *
	  * @param startDate the start date
	  * @param startDate the end date
	  * @return the result in JSON format
	  */
  	def getGameScheduleDataForHomeScreen(startDate:Long,endDate:Long): JsonElement = {
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
		    "constant_score": {
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
		        "top_game_home_hits": {
		          "top_hits": {
		            "size": 10
		          }
		        }
		      }
		    }
		  }
		}""".stripMargin.replaceAll("\n", " ")
		elasticSearchClient.search("POST",getGameScheduleURLBase(), Map[String, String](),searchTemplate)	  	
  	}

  	/*def getGameDrivesByGameCode(gameCode: String) : JsonElement = {
			val searchTemplate =  s""" {
			  "size"  : 10,
		  	"_source":  [ "wins", "losses" ],
					"query" : {
							"term" : { "id" : "$gameCode" }
					}
				}""".stripMargin.replaceAll("\n", " ")
			elasticSearchClient.search("POST",getLiveGameById(), Map[String, String](),searchTemplate)
		}*/

  	
  	/**
	  * Fetches game schedules for media card
	  *
	  * @param gameRole the active team game
	  * @param teamId the team id
	  * @return the result in JSON format
	  */
  	def getGameSchedulesForMediaCard(gameRole:ActiveTeamGame,teamId:String):JsonElement = {
  	  	val prevSixMonth = Instant.now().getEpochSecond()-Math.round(6*30*24*60*60);
  		val searchTemplate =  s"""{
		  "size": 0,
		  "query": {
		    "constant_score": {
		      "filter": {
		        "bool": {
		          "must": [
		            {
		              "term": {
		              		${      
			                	if(gameRole.getActiveTeamRole()==Role.AWAY) 
			                	 	"\"awayTeamExternalId\":"+ "\""+ teamId + "\""
			                	else
			                		"\"homeTeamExternalId\":"+ "\""+ teamId + "\""
			                }
		              	}
		            },
		            {
		              "range": {
		                "game_date_epoch": {
		                  "gte": $prevSixMonth
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
        		"size" : 3000,
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
		        "top_game_mc_hits": {
		          "top_hits": {
		            "size": 10
		          }
		        }
		      }
		    }
		  }
		}""".stripMargin.replaceAll("\n", " ")
		elasticSearchClient.search("POST",getGameScheduleURLBase(), Map[String, String](),searchTemplate) 			  	
  	}
  	
  	/**
	  * Fetches game schedules for active team
	  *
	  * @param teamId the team id
	  * @return the result in JSON format
	  */
  	def getNearestGameScheduleForActiveTeam(teamId:String): JsonElement = {
  		val searchTemplate =  s"""{
		  "size": 10,
		  "query": {
		    "constant_score": {
		      "filter": {
		        "bool": {
		          "should": [
		            {
		              "term": {
		                "homeTeamExternalId": "$teamId"
		              }
		            },
		            {
		              "term": {
		                "awayTeamExternalId": "$teamId"
		              }
		            }
		          ]
		        }
		      }
		    }
		  },
		  "sort": [
		    {
		      "game_date_epoch": {
		        "order": "desc"
		      }
		    }
		  ]
		}""".stripMargin.replaceAll("\n", " ")
		elasticSearchClient.search("POST",getGameScheduleURLBase(), Map[String, String](),searchTemplate)			  	
  	}
  	
  	/**
	  * Returns elastic search url for game schedules
	  */
  	def getGameScheduleURLBase():StringBuilder  = {
  		new StringBuilder(GAME_SCHEDULE_FETCH_BASE_URL)
  	}
  	
  	/**
	  * Returns elastic search url for player statistics
	  */
  	def getPlayerStatsURLBase():StringBuilder  = {
  		new StringBuilder(PLAYER_STATS_FETCH_BASE_URL)
  	}
  	
  	/**
	  * Returns elastic search url for team standings
	  */
  	def getTeamStandingsURLBase():StringBuilder  = {
  		new StringBuilder(TEAM_STANDINGS_FETCH_BASE_URL)
  	}
  	
  	/**
	  * Returns elastic search url for live info
	  */
  	def getLiveInfoURLBase():StringBuilder  = {
  		new StringBuilder(LIVE_INFO_FETCH_BASE_URL)
  	}
  	
  	/**
	  * Returns elastic search url for live score events
	  */
  	def getScoringEventsURLBase():StringBuilder  = {
  		new StringBuilder(SCORING_EVENTS_FETCH_BASE_URL)
  	}
  	
  		
}