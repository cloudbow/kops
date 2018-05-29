/*
 * MediaCard.java
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
 * Media card details of the Sports Game.
 * 
 * @author Jayachand.Konduru
 * @version 1.0
 * @since 1.0
 */
public class MediaCard {

	/** Game Id */
	private String id;

	/** Game duration, in seconds */
	private int duration; 

	/** Game start timestamp */
	private String start_time;

	/** Game end timestamp */
	private String stop_time;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public String getStartTime() {
		return start_time;
	}

	public void setStartTime(String startTime) {
		this.start_time = startTime;
	}

	public String getStopTime() {
		return stop_time;
	}

	public void setStopTime(String stopTime) {
		this.stop_time = stopTime;
	}

}
