package com.slingmedia.sportscloud.netty.client.model;

public class Thumbnail {

	String url;
	
	int w = 0;

	int h = 0;

	public String getmUrl() {
		return url;
	}

	public void setmUrl(String mUrl) {
		this.url = mUrl;
	}

	public int getmWidth() {
		return w;
	}

	public void setmWidth(int mWidth) {
		this.w = mWidth;
	}

	public int getmHeight() {
		return h;
	}

	public void setmHeight(int mHeight) {
		this.h = mHeight;
	}

}
