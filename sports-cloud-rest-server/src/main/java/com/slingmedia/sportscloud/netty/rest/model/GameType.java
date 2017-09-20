package com.slingmedia.sportscloud.netty.rest.model;

public enum GameType {
	REGULAR_SEASON("Regular Season"), UNKNOWN("uknown");
	GameType(String gts){
		gameTypeStr = gts;
	}
	private String gameTypeStr; 
	public static GameType getValue(String gameType) {
		if (gameType.equalsIgnoreCase("regular season"))
			return REGULAR_SEASON;
		else
			return UNKNOWN;
	}
	public String getGameTypeStr() {
		return gameTypeStr;
	}
	public void setGameTypeStr(String gameTypeStr) {
		this.gameTypeStr = gameTypeStr;
	}

}