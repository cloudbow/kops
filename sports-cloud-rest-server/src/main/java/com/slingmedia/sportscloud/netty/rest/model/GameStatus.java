/*
 * GameStatus.java
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
 * Constants for Game Status.
 * 
 * @author Jayachand.Konduru
 * @version 1.0
 * @since 1.0
 */
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