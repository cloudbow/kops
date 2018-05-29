/*
 * SportsCloudRestConfig.java
 * @author arung
 **********************************************************************

             Copyright (c) 2004 - 2014 by Sling Media, Inc.

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
package com.slingmedia.sportscloud.netty.rest.server.config;

import java.util.Properties;

/**
 * Initializer for System properties
 * 
 * @author arung
 * @version 1.0
 * @since 1.0
 */
public class SportsCloudRestConfig {

	/**
	 * The maximum queue length for incoming connections. If a connection
	 * indication arrives when the queue is full, then connection will be
	 * refused.
	 */
	private static int SO_BACKLOG = 128;

	private static double GAME_STOPTIME_OFFSET = 3.5 * 60 * 60;

	/**
	 * Initializes the system properties
	 * 
	 * @param propeties
	 *            the system config parameters
	 */
	public static void initialize(Properties propeties) {
		if (propeties != null && propeties.size() > 0) {
			if (propeties.contains("SO_BACKLOG")) {
				setSocketBacklog(Integer.parseInt(propeties.getProperty("SO_BACKLOG")));
			}

			if (propeties.contains("GAME_STOPTIME_OFFSET")) {
				setGameStopTimeOffset(Double.parseDouble(propeties.getProperty("GAME_STOPTIME_OFFSET")));
			}

		}
	}

	public static int getSocketBacklog() {
		return SO_BACKLOG;
	}

	public static void setSocketBacklog(int socketBacklog) {
		SO_BACKLOG = socketBacklog;
	}

	public static double getGameStopTimeOffset() {
		return GAME_STOPTIME_OFFSET;
	}

	public static void setGameStopTimeOffset(double gameStopTimeOffset) {
		GAME_STOPTIME_OFFSET = gameStopTimeOffset;
	}

}
