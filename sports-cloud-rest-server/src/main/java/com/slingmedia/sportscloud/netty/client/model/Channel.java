package com.slingmedia.sportscloud.netty.client.model;

public class Channel {
	
	String guid; // Channel guid

	String title; // Channel callsign

	String type; // Type of channel (always “channel”)

	public String getGuid() {
		return guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

}
