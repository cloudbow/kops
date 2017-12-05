package com.slingmedia.sportscloud.netty.client.model;

public class SportsMediaCard extends MediaCard {

	String sport;

	String league;
	
	String gameId;

	String gameStatus;

	String anons;

	String anons_title;

	String location;

	SportTeam homeTeam;

	SportTeam awayTeam;

	GameStats gamestats;

	public String getAnons() {
		return anons;
	}

	public void setAnons(String anons) {
		this.anons = anons;
	}

	public String getAnons_title() {
		return anons_title;
	}

	public void setAnons_title(String anons_title) {
		this.anons_title = anons_title;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

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
