package com.slingmedia.sportscloud.netty.rest.model;

public enum GameStatus {

	UPCOMING, COMPLETED, IN_PROGRESS, POSTPONED, DELAYED, NONE;

	public static GameStatus getValue(int statusId) {

		if (statusId == 1)
			return UPCOMING;
		else if (statusId == 2)
			return IN_PROGRESS;
		else if (statusId == 4)
			return COMPLETED;
		else if (statusId == 5)
			return POSTPONED;
		else if (statusId == 23)
			return DELAYED;
		else
			return NONE;

	}

}