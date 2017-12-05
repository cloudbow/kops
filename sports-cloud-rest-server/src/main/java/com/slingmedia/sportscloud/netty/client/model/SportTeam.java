package com.slingmedia.sportscloud.netty.client.model;

public class SportTeam {

	String name;
	
	String alias;

	String homeCity;

	Thumbnail logo;

	String id;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public String getHomeCity() {
		return homeCity;
	}

	public void setHomeCity(String homeCity) {
		this.homeCity = homeCity;
	}

	public Thumbnail getLogo() {
		return logo;
	}

	public void setLogo(Thumbnail logo) {
		this.logo = logo;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	
}
