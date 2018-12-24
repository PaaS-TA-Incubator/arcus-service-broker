package org.openpaas.servicebroker.arcus;

import net.spy.memcached.AdminConnectTimeoutException;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.openpaas.servicebroker.arcus.exception.ArcusServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ZkConnection implements Watcher {
	private static final String ARCUS_BASE = "/arcus";
	private static final int ZK_SESSION_TIMEOUT = 15000;

	private static final long ZK_CONNECT_TIMEOUT = ZK_SESSION_TIMEOUT;

	private Logger logger = LoggerFactory.getLogger(ZkConnection.class);

	private String host;

	private CountDownLatch zkInitLatch;

	private ZooKeeper zk;

	private boolean base_check() throws KeeperException, InterruptedException, ArcusServiceException {
		if (zk.exists(ARCUS_BASE, false) == null) {
			throw new ArcusServiceException("Arcus base does not exist");
		}
		return true;
	}

	public ZooKeeper getZk() throws ArcusServiceException {
		try {
			if (base_check()) {
				return zk;
            }
		} catch (KeeperException e) {
			logger.warn("Can't connect to Arcus admin(%) %s", host, e.getMessage());
			shutdownZooKeeperClient();
			initZooKeeperClient();
		} catch (InterruptedException e) {
			logger.warn("Can't connect to Arcus admin(%) %s", host, e.getMessage());
			shutdownZooKeeperClient();
			initZooKeeperClient();
		}

		return zk;
	}



	public ZkConnection(String host) throws IOException, InterruptedException {
		logger.info("Initializing the ZkConnection.");
		this.host = host;
		initZooKeeperClient();
	}

	public void initZooKeeperClient() {
		try {
			logger.info("Trying to connect to Arcus admin(%s)", host);

			zk = new ZooKeeper(host, ZK_SESSION_TIMEOUT, this);
			zkInitLatch = new CountDownLatch(1);
			try {
				if (zkInitLatch.await(ZK_CONNECT_TIMEOUT, TimeUnit.MILLISECONDS) == false) {
                    logger.error("Connecting to Arcus admin(%s) timed out : %d miliseconds", host, ZK_CONNECT_TIMEOUT);
                    throw new AdminConnectTimeoutException(host);
                }
			} catch (InterruptedException e) {
				logger.warn("Can't connect to Arcus admin(%s) %s", e.getMessage());
				e.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void shutdownZooKeeperClient() {
		if (zk == null) {
			return;
		}

		try {
			logger.info("Close the ZooKeeper client. "+ "adminSessionId=0x" + Long.toHexString(zk.getSessionId()));
			zk.close();
			zk = null;
		} catch (InterruptedException e) {
			logger.warn("An exception occured while closing ZooKeeper client.", e);
		}
	}

	public void process(WatchedEvent event) {
		if (event.getType() == Event.EventType.None) {
			// Processes session events
			switch (event.getState()) {
			case SyncConnected:
				logger.warn("Connected to the Arcus admin. " + getInfo());
				zkInitLatch.countDown();
				return;
			case Disconnected:
				logger.warn("Disconnected from the Arcus admin. Trying to reconnect. " + getInfo());
				return;
			case Expired:
				logger.warn("Session expired. Trying to reconnect to the Arcus admin." + getInfo());
				shutdownZooKeeperClient();
				initZooKeeperClient();
				return;
			}
		}
	}

	private String getInfo() {
		String zkSessionId = null;
		if (zk != null) {
			zkSessionId = "0x" + Long.toHexString(zk.getSessionId());
		}
		return "[adminSessionId=" + zkSessionId + "]";
	}
}
