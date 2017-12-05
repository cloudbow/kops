package com.slingmedia.sportscloud.netty.client.model;

import java.util.List;

import org.joda.time.DateTime;

public class Ribbon {

	String title; // sport category name
	
	String expires_at; // timestamp to reload

	String _href; // URL to retrieve games list (when empty, provide games list
					// in “tiles”)

	List<TileAsset> tiles; // Games list (when empty, provide URL to retrieve
							// the list of games)
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getExpiresAt() {
		return expires_at;
	}

	public void setExpiresAt(String expiresAt) {
		this.expires_at = expiresAt;
	}

	public String getHref() {
		return _href;
	}

	public void setHref(String href) {
		this._href = href;
	}

	public List<TileAsset> getTiles() {
		return tiles;
	}

	public void setTiles(List<TileAsset> tiles) {
		this.tiles = tiles;
	}


}
