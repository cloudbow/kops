package com.slingmedia.sportscloud.facade

import com.slingmedia.sportscloud.netty.rest.model.{ActiveTeamGame, Role}
import com.google.gson.{JsonElement,JsonParser}
import java.net.URLEncoder
import java.time.Instant
import org.slf4j.LoggerFactory;


object SportsDataFacade {

  	private val GAME_SCHEDULE_INDEX_ENTITY = "game_schedule"
  	private val LIVE_INFO_INDEX_ENTITY="live_info";
	private val TEAM_STANDINGS_INDEX_ENTITY="team_standings";
	private val PLAYER_STATS_INDEX_ENTITY="player_stats";
	private val SCORING_EVENTS_INDEX_ENTITY="scoring_events";

  	private val INDEX_HOST = if(System.getProperty("indexingHost") == null)  "cqaneat02.sling.com" else System.getProperty("indexingHost")
	private val INDEX_PORT = if(System.getProperty("indexingPort")==null) "8983" else System.getProperty("indexingPort")
	
	private val GAME_SCHEDULE_FETCH_BASE_URL = "http://"+INDEX_HOST+":"+INDEX_PORT+"/solr/"+GAME_SCHEDULE_INDEX_ENTITY+"/select"
  	private val PLAYER_STATS_FETCH_BASE_URL = "http://"+INDEX_HOST+":"+INDEX_PORT+"/solr/"+PLAYER_STATS_INDEX_ENTITY+"/select"
	private val TEAM_STANDINGS_FETCH_BASE_URL = "http://"+INDEX_HOST+":"+INDEX_PORT+"/solr/"+TEAM_STANDINGS_INDEX_ENTITY+"/select"
	private val LIVE_INFO_FETCH_BASE_URL = "http://"+INDEX_HOST+":"+INDEX_PORT+"/solr/"+LIVE_INFO_INDEX_ENTITY+"/select"
	private val SCORING_EVENTS_FETCH_BASE_URL = "http://"+INDEX_HOST+":"+INDEX_PORT+"/solr/"+SCORING_EVENTS_INDEX_ENTITY+"/select"

    private val externalHttpClientImpl = ExternalHttpClient
    private val log = LoggerFactory.getLogger("ExternalHttpDao")    

  	def getGameScheduleByGameCode(gameCode:String): JsonElement = {
  		val gameRequestBuilder = getGameScheduleURLBase()
  						.append("?q=gameCode:")
  						.append(gameCode)
						.append("&wt=json");
		getJsonObject(gameRequestBuilder)			  	
  	}
  	
  	def getAllScoringEventsForGame(gameId:String): JsonElement = {
  		val gameRequestBuilder = getScoringEventsURLBase()
  					.append("?q=gameId:")
					.append(gameId)
					.append("&sort=srcTime%20desc")
					.append("&start=0&rows=100")
					.append("&wt=json");
		getJsonObject(gameRequestBuilder)			  	
  	}

  	
  	def getLiveGameById(id:String): JsonElement = {
  		val gameRequestBuilder = getLiveInfoURLBase()
  						.append("?q=gameId:")
  						.append(id)
  						.append("&wt=json");
		getJsonObject(gameRequestBuilder)			  	
  	}
  	
  	def getAllLiveGamesInDateRange(startDate:Long, endDate:String): JsonElement = {
  		val gameRequestBuilder = getLiveInfoURLBase()
  						.append("?q=game_date_epoch:[")
  						.append(startDate)
  						.append("%20TO%20")
  						.append(endDate)
						.append("]").append("&fl=gameId,homeScoreRuns,awayScoreRuns,statusId")
						.append("&start=0&rows=500&wt=json");
		getJsonObject(gameRequestBuilder)			  	
  	}
  	
  	def getLiveInfoForActiveTeam(activeGame: ActiveTeamGame ): JsonElement = {
  		val gameRequestBuilder = getLiveInfoURLBase().
  						append("?q=gameType:%22").append(URLEncoder.encode(activeGame.getGameType().getGameTypeStr(), "UTF-8")).append("%22").
						append("+AND+(").
						append("homeTeamExtId:").append(activeGame.getActiveTeamId()).
						append("+OR+").
						append("awayTeamExtId:").append(activeGame.getActiveTeamId()).append(")").
						append("&wt=json");
		getJsonObject(gameRequestBuilder)			  	
  	}
  	
  	def getGameScheduleDataForHomeScreen(startDate:Long,endDate:String): JsonElement = {
  		val gameRequestBuilder = getGameScheduleURLBase()
  				.append("?q=game_date_epoch:[").append(startDate).append("%20TO%20")
  				.append(endDate)
				.append("]")
				.append("&start=0&rows=500")
				.append("&sort=game_date_epoch%20asc")
				.append("&group=true&group.field=gameCode&group.limit=10&group.sort=batchTime%20desc").append("&wt=json")
		getJsonObject(gameRequestBuilder)			  	
  	}
  	
  	def getMainLeaguesForActiveGame(activeGame:ActiveTeamGame): JsonElement = {
  		val gameRequestBuilder = getTeamStandingsURLBase()
  							.append("?q=id:(")
  							.append(activeGame.getHomeTeamId()).append("+")
							.append(activeGame.getAwayTeamId()).append(")")
							.append("&facet=on&facet.field=subLeague&rows=1&wt=json");	
		getJsonObject(gameRequestBuilder)	
  	
  	}
  	
  	def getSubLeagues(subLeague:String):JsonElement = {
  		val gameRequestBuilder = getTeamStandingsURLBase()
  							.append("?q=subLeague:%22")
  							.append(URLEncoder.encode(subLeague, "UTF-8"))
							.append("%22")
							.append("&fq=%7B!collapse%20field=division%7D&expand=true&expand.rows=100&wt=json");
		getJsonObject(gameRequestBuilder)
  	
  	}
  	
  	def getPlayerStatsById(playerId:String): JsonElement = {
  		val gameRequestBuilder = getPlayerStatsURLBase()
  							.append("?q=id:")
  							.append(playerId)
							.append("&fl=wins,losses&wt=json");		
		getJsonObject(gameRequestBuilder)			 
  	
  	}
  	
  	def getGameSchedulesForMediaCard(gameRole:ActiveTeamGame,teamId:String):JsonElement = {
  		val gameScheduleReqBuilder = getGameScheduleURLBase()
  		gameRole.getActiveTeamRole()  match  {
			case Role.AWAY =>
				gameScheduleReqBuilder.append("?q=awayTeamExternalId:").append(teamId);
			case Role.HOME =>
				gameScheduleReqBuilder.append("?q=homeTeamExternalId:").append(teamId);
			case _ =>
				//do nothing

		}
		val prevSixMonth = Instant.now().getEpochSecond()-Math.round(6*30*24*60*60);
		gameScheduleReqBuilder
			.append("+AND+").append("game_date_epoch:")
			.append("[")
			.append(prevSixMonth).append("%20TO%20*]")
			.append("&start=0&rows=100")
			.append("&group=true&group.field=gameCode")
			.append("&sort=game_date_epoch%20asc&wt=json");
  	    getJsonObject(gameScheduleReqBuilder)			  	
  	}
  	
  	def getNearestGameScheduleForActiveTeam(teamId:String): JsonElement = {
  		val gameRequestBuilder = getGameScheduleURLBase()
  					.append("?q=awayTeamExternalId:")
  					.append(teamId)
  					.append("+OR+")
					.append("homeTeamExternalId:").append(teamId)
					.append("&sort=game_date_epoch%20desc&wt=json&rows=1");
		getJsonObject(gameRequestBuilder)			  	
  	}
  	
  	def getJsonObject(requestURLBuilder:StringBuilder): JsonElement = {
  		val parser = new JsonParser();
		val responseString = externalHttpClientImpl.getFromUrl(requestURLBuilder.toString());
		var responseJson = parser.parse("{}");
		try {
			responseJson = parser.parse(responseString);
		} catch {
      		case e: Exception => log.error("Error ocurred in parsing",e)
    	}
		return responseJson;
  	}
  	
  	
  	def getGameScheduleURLBase():StringBuilder  = {
  		new StringBuilder(GAME_SCHEDULE_FETCH_BASE_URL)
  	}
  	
  	def getPlayerStatsURLBase():StringBuilder  = {
  		new StringBuilder(PLAYER_STATS_FETCH_BASE_URL)
  	}
  	
  	def getTeamStandingsURLBase():StringBuilder  = {
  		new StringBuilder(TEAM_STANDINGS_FETCH_BASE_URL)
  	}
  	
  	def getLiveInfoURLBase():StringBuilder  = {
  		new StringBuilder(LIVE_INFO_FETCH_BASE_URL)
  	}
  	
  	def getScoringEventsURLBase():StringBuilder  = {
  		new StringBuilder(SCORING_EVENTS_FETCH_BASE_URL)
  	}
  	
}