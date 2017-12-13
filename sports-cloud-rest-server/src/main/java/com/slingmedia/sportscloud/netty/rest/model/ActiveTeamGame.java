package com.slingmedia.sportscloud.netty.rest.model;

public class ActiveTeamGame {
	/**
	 * 
	 */
	private String gameId;
	private Role activeTeamRole;
	private String activeTeamId;
	private String homeTeamId;
	private String awayTeamId;
	private String homeTeamName;
	private String awayTeamName;
	private GameType gameType;

	public ActiveTeamGame(String gameId, GameType gameType, String activeTeamId, String homeTeamId, String awayTeamId, String homeTeamName,
			String awayTeamName, Role role) {
		this.setGameType(gameType);
		this.setHomeTeamName(homeTeamName);
		this.setAwayTeamName(awayTeamName);
		this.setActiveTeamId(activeTeamId);
		this.setHomeTeamId(homeTeamId);
		this.setAwayTeamId(awayTeamId);
		this.setGameId(gameId);
		this.setActiveTeamRole(role);
	}

	public String getGameId() {
		return gameId;
	}

	public void setGameId(String gameId) {
		this.gameId = gameId;
	}

	public Role getActiveTeamRole() {
		return activeTeamRole;
	}

	public void setActiveTeamRole(Role role) {
		this.activeTeamRole = role;
	}

	public String getHomeTeamId() {
		return homeTeamId;
	}

	public void setHomeTeamId(String homeTeamId) {
		this.homeTeamId = homeTeamId;
	}

	public String getAwayTeamId() {
		return awayTeamId;
	}

	public void setAwayTeamId(String awayTeamId) {
		this.awayTeamId = awayTeamId;
	}

	public String getActiveTeamId() {
		return activeTeamId;
	}

	public void setActiveTeamId(String activeTeamId) {
		this.activeTeamId = activeTeamId;
	}

	public String getHomeTeamName() {
		return homeTeamName;
	}

	public void setHomeTeamName(String homeTeamName) {
		this.homeTeamName = homeTeamName;
	}

	public String getAwayTeamName() {
		return awayTeamName;
	}

	public void setAwayTeamName(String awayTeamName) {
		this.awayTeamName = awayTeamName;
	}

	public GameType getGameType() {
		return gameType;
	}

	public void setGameType(GameType gameType) {
		this.gameType = gameType;
	}

}