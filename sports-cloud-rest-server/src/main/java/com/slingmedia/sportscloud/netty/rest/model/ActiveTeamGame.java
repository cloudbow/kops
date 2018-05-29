/*
 * ActiveTeamGame.java
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
package com.slingmedia.sportscloud.netty.rest.model;

/**
 * Active Game details of the specific team.
 * 
 * @author arung
 * @version 1.0
 * @since 1.0
 */
public class ActiveTeamGame {

	/** Game Id */
	private String gameId;

	/** Active Team Role */
	private Role activeTeamRole;

	/** Active Team Id */
	private String activeTeamId;

	/** Home team Id */
	private String homeTeamId;

	/** Away team Id */
	private String awayTeamId;

	/** Home team name */
	private String homeTeamName;

	/** Away team name */
	private String awayTeamName;

	/** Game type */
	private GameType gameType;

	public ActiveTeamGame(String gameId, GameType gameType, String activeTeamId, String homeTeamId, String awayTeamId,
			String homeTeamName, String awayTeamName, Role role) {
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