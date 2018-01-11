/*
 * Ribbon.java
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

import java.util.List;

/**
 * List of upcoming Sports Games for specific sports category, example
 * Baseball,Football etc., .
 * 
 * @author Jayachand.Konduru
 * @version 1.0
 * @since 1.0
 */
public class Ribbon {

	/** Sport category name */
	private String title; //

	/** Timestamp to reload */
	private String expires_at; //

	/**
	 * URL to retrieve games list (when empty, provide games list in “tiles”)
	 */
	private String _href; //

	/** Games list (when empty, provide URL to retrieve the list of games) */
	private List<TileAsset> tiles; //

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
