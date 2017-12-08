package com.slingmedia.sportscloud.netty.client.model;

import java.util.List;

import org.joda.time.DateTime;

public class TileAsset implements Cloneable {

	String id; // content id

	String type; // always “show”

	String title; // game name

	List<String> ratings; // US MPAA rating, US TV content rating, if available

	String _href; // Media card URL

	Thumbnail thumbnail; // Game thumbnail

	int duration; // Game duration, in seconds

	String start_time; // Game start timestamp

	String stop_time; // Game end timestamp

	Channel channel; // Channel information

	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public List<String> getRatings() {
		return ratings;
	}

	public void setRatings(List<String> ratings) {
		this.ratings = ratings;
	}

	public String getHref() {
		return _href;
	}

	public void setHref(String href) {
		this._href = href;
	}

	public Thumbnail getThumbnail() {
		return thumbnail;
	}

	public void setThumbnail(Thumbnail thumbnail) {
		this.thumbnail = thumbnail;
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

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}

}
