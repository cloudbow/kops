/*
 * SportsTileAsset.java
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
package com.slingmedia.sportscloud.netty.client.model;

/**
 * Game Tile details
 * 
 * @author Jayachand.Konduru
 * @version 1.0
 * @since 1.0
 */
public class SportsTileAsset extends TileAsset {

	/** Sport name */
	private String sport;

	/** League name MLB,NFL,NCAAF,NBA etc., */
	private String league;

	/** Game teaser */
	private String teaser;

	/** Game Id */
	private String gameId;

	/** Game status UPCOMING, POSTPONED, TAPEDELAY, COMPLETED */
	private String gameStatus;

	/** Home team details */
	private SportTeam homeTeam;

	/** Away team details */
	private SportTeam awayTeam;

	/** Game statistics rating and live score */
	private GameStats gamestats;

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
