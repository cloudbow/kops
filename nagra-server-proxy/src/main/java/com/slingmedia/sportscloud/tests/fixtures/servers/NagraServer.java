/*
 * NagraServer.java
 * @author arung
 **********************************************************************

             Copyright (c) 2004 - 2014 by Sling Media, Inc.

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
package com.slingmedia.sportscloud.tests.fixtures.servers;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.slingmedia.sportscloud.tests.fixtures.servers.handlers.NagraServerInitializer;

/**
 * The Class NagraServer.
 */
public class NagraServer {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(NagraServer.class);

	/** The Constant DEFAULT_BOSS_THREADS. */
	private static final int DEFAULT_BOSS_THREADS = 10;
	
	/** The Constant DEFAULT_WORKER_THREADS. */
	private static final int DEFAULT_WORKER_THREADS = 20;
	
	/** The is secure. */
	public static boolean IS_SECURE = false;

	/** The Constant NAGRA_SERVER. */
	private static final NagraServer NAGRA_SERVER = new NagraServer();

	/**
	 * Instantiates a new nagra server.
	 */
	private NagraServer() {
		LOGGER.info("Iam initialized");
	}

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(final String[] args) {
		NagraServer.LOGGER.trace("--NAGRASERVER INIT--");
		if (args.length < 2) {
			NagraServer.LOGGER.info("Unable to start server . Please provide server name");
			System.exit(1);
		}
		if (args.length > 3) {
			NagraServer.IS_SECURE = Boolean.parseBoolean(args[3]);
		}
		// assign boss and worker threads
		int bossThreads = NagraServer.DEFAULT_BOSS_THREADS;
		int workerThreads = NagraServer.DEFAULT_WORKER_THREADS;
		if (args.length > 3) {
			bossThreads = Integer.parseInt(args[3]);
			workerThreads = Integer.parseInt(args[4]);
		}
		final String server = args[0];
		final int port = Integer.parseInt(args[1]);

		NagraServer.NAGRA_SERVER.createServer(server, port, bossThreads, workerThreads);
	}

	/**
	 * Creates the server.
	 *
	 * @param server the server
	 * @param port the port
	 * @param bossThreads the boss threads
	 * @param workerThreads the worker threads
	 */
	private void createServer(final String server, final int port, final int bossThreads, final int workerThreads) {

		NioEventLoopGroup bossGroup = null;
		NioEventLoopGroup workerGroup = null;

		try {
			final ServerBootstrap batchBootstrap = new ServerBootstrap();

			bossGroup = new NioEventLoopGroup(bossThreads, new DefaultThreadFactory("NagraServerThreads"));
			workerGroup = new NioEventLoopGroup(workerThreads, new DefaultThreadFactory("NagraWorkerThreads"));
			//@formatter:off
            batchBootstrap
            	.group(bossGroup, workerGroup)
            	.channel(NioServerSocketChannel.class)
            	.childHandler(new NagraServerInitializer())
            	.option(ChannelOption.SO_BACKLOG, 128);
            //@formatter:on
			ChannelFuture f = null;
			try {
				f = batchBootstrap.bind(port).sync();
			} catch (final InterruptedException e) {
				LOGGER.error("Interrupted ..",e );
			}
			NagraServer.LOGGER.info("STARTED NAGRA SERVER SUCCESSFULLY");
			f.channel().closeFuture().syncUninterruptibly();

		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}

	}
}
