/*
 * GameStats.java
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
 * Game Statistics for rating and live score.
 * 
 * @author Jayachand.Konduru
 * @version 1.0
 * @since 1.0
 */
public class GameStats {
	/** Thuuz Game rating stats */
	private ThuuzStats thuuz;

	/** Game live score stats */
	private NagraStats nstats;

	public ThuuzStats getThuuz() {
		return thuuz;
	}

	public void setThuuz(ThuuzStats thuuz) {
		this.thuuz = thuuz;
	}

	public NagraStats getNstats() {
		return nstats;
	}

	public void setNstats(NagraStats nstats) {
		this.nstats = nstats;
	}

}
