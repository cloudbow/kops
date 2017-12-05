package com.slingmedia.sportscloud.netty.client.model;

public class MediaCard {

	String id; // content id

	int duration; // Game duration, in seconds

	String start_time; // Game start timestamp

	String stop_time; // Game end timestamp

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
