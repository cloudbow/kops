package com.slingmedia.sportscloud.netty.client.model;

public class SportsTileAsset extends TileAsset {

	String sport;
	
	String league;

	String teaser;

	String gameId;

	String gameStatus;

	SportTeam homeTeam;

	SportTeam awayTeam;

	GameStats gamestats;

	public String getSport() {
		return sport;
	}

	public void setSport(String sport) {
		this.sport = sport;
	}

	public String getLeague() {
		return league;
	}

	public void setLeague(String league) {
		this.league = league;
	}

	public String getTeaser() {
		return teaser;
	}

	public void setTeaser(String teaser) {
		this.teaser = teaser;
	}

	public String getGameId() {
		return gameId;
	}

	public void setGameId(String gameId) {
		this.gameId = gameId;
	}

	public String getGameStatus() {
		return gameStatus;
	}

	public void setGameStatus(String gameStatus) {
		this.gameStatus = gameStatus;
	}

	public SportTeam getHomeTeam() {
		return homeTeam;
	}

	public void setHomeTeam(SportTeam homeTeam) {
		this.homeTeam = homeTeam;
	}

	public SportTeam getAwayTeam() {
		return awayTeam;
	}

	public void setAwayTeam(SportTeam awayTeam) {
		this.awayTeam = awayTeam;
	}

	public GameStats getGamestats() {
		return gamestats;
	}

	public void setGamestats(GameStats gamestats) {
		this.gamestats = gamestats;
	}

	

}
