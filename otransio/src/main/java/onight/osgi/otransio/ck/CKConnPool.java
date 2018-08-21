package onight.osgi.otransio.ck;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.StringUtils;
import org.glassfish.grizzly.CloseListener;
import org.glassfish.grizzly.Closeable;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.ICloseType;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.otransio.impl.NodeInfo;
import onight.osgi.otransio.impl.OSocketImpl;
import onight.osgi.otransio.nio.OClient;
import onight.osgi.otransio.nio.PacketTuple;
import onight.osgi.otransio.sm.MSessionSets;
import onight.osgi.otransio.sm.RemoteModuleBean;
import onight.tfw.otransio.api.MessageException;
import onight.tfw.otransio.api.PackHeader;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.pool.ReusefulLoopPool;

@Data
@Slf4j
@SuppressWarnings({ "rawtypes", "unchecked" })
public class CKConnPool extends ReusefulLoopPool<Connection> {
	OClient client;
	String ip;
	int port;
	int core;
	int max;
	boolean stop = false;
	String subnodeURI = "";
	String from_bcuid = "";

	ArrayList<NodeInfo> subNodes = new ArrayList<>();

	MSessionSets mss;

	public void setStop(boolean isstop) {
		this.stop = true;
		if (core <= 0) {
			mss.dropSession(nameid, false);
		}
	}

	String nameid;

	public CKConnPool(OClient client, String ip, int port, int core, int max, MSessionSets mss, String nameid) {
		super();
		this.client = client;
		this.ip = ip;
		this.port = port;
		this.core = core;
		this.max = max;
		this.mss = mss;
		this.nameid = nameid;
	}

	public String getJsonStr() {
		StringBuffer sb = new StringBuffer();
		sb.append("{\"ip\":\"" + ip + "\"");
		sb.append(",\"port\":" + port + "");
		sb.append(",\"core\":" + core + "");
		sb.append(",\"max\":" + max + "");
		sb.append(",\"curr\":" + this.size() + "");
		sb.append("}");
		return sb.toString();
	}

	public synchronized Connection createOneConnection(int waitms) {
		return createOneConnection(1, waitms);
	}

	public void parseURI(String uri) {
		subNodes.clear();
		for (String str : uri.split(",")) {
			if (!StringUtils.isBlank(str.trim())) {
				try {
					NodeInfo newin = NodeInfo.fromURI(str, this.nameid);
					subNodes.add(newin);
				} catch (Exception e) {
				}
			}
		}
	}

	public Connection<?> ensureConnection() throws MessageException {
		Connection<?> conn = borrow();
		if (conn != null) {
			if (conn.isOpen()) {
				return conn;
			} else {
				removeObject(conn);
				conn = null;
			}
		}
		conn = createOneConnection(1, 100);

		return conn;
	}

	// public void sendMessage(final FramePacket pack) throws MessageException {
	// boolean writed = false;
	// for (int i = 0; i < 1; i++) {
	// Connection conn = null;
	// try {
	// conn = borrow();
	// if (conn != null) {
	// if (conn.isOpen()) {
	// if (conn.write(pack) != null) {
	// writed = true;
	// }
	// break;
	// } else {
	// removeObject(conn);
	// conn = null;
	// }
	// }
	// if (createOneConnection(1) == null) {
	// throw new MessageException("cannot connect");
	// }
	// Thread.sleep(100);
	// } catch (Exception e) {
	// log.error("sendMessageError:" + pack, e);
	// throw new MessageException(e);
	// } finally {
	// if (conn != null && conn.isOpen()) {
	// retobj(conn);
	// }
	// }
	// }
	// if (!writed) {
	// throw new MessageException("No More Connections for [" + ip + ":" + port
	// + "]");
	// }
	// }

	// public void sendMessage(final List<PacketTuple> packs) throws
	// MessageException {
	// boolean writed = false;
	// int retry_Connect = 0;
	// for (int i = 0; i < 30; i++) {
	// Connection<?> conn = null;
	// try {
	// conn = borrow();
	// if (conn != null) {
	// if (conn.isOpen()) {
	// for (PacketTuple pack : packs) {
	// conn.write(pack.getPack());
	// pack.setWrited(true);
	// }
	// writed = true;
	// return;
	// } else {
	// removeObject(conn);
	// conn = null;
	// }
	// }
	// if (size() < max) {
	// createOneConnection(1);
	// }
	// if (size() == 0) {// has no more connection but only connect 3
	// // times
	// if (retry_Connect++ <= 3) {
	// // cannot get connection
	// break;
	// }
	// Thread.sleep(1000);// try connect next time
	// } else {// try 10*6000=60 seconds
	// Thread.sleep(10);
	// }
	// } catch (Exception e) {
	// log.error("sendMessageError:", e);
	// throw new MessageException(e);
	// } finally {
	// if (conn != null && conn.isOpen()) {
	// retobj(conn);
	// }
	// }
	// }
	// if (!writed) {
	// throw new MessageException("No More Connections");
	// }
	// }

	public synchronized Connection createOneConnectionBySubNode(int maxtries) {
		for (NodeInfo node : subNodes) {
			try {
				final Connection conn = client.getConnection(node.getAddr(), node.getPort());
				if (conn != null) {
					conn.addCloseListener(new CloseListener<Closeable, ICloseType>() {
						@Override
						public void onClosed(Closeable closeable, ICloseType type) throws IOException {
							log.debug("CheckHealth remove Connection!:" + closeable);
							if (closeable instanceof Connection) {
								removeObject((Connection) closeable);
							}
						}
					});
					// final FramePacket pack = mss.getLocalModulesPacket();
					RemoteModuleBean rmb = new RemoteModuleBean();
					FramePacket pack = PacketHelper.genSyncPack(PackHeader.REMOTE_LOGIN, PackHeader.REMOTE_MODULE,

							rmb);
					conn.write(pack);
					addObject(conn);
					return conn;
				}
			} catch (Exception e) {
				// creating new Connection
				log.warn("error in create out sub conn:" + ip + ",port=" + port + "," + e.getMessage());
			}
		}
		return null;
	}

	public synchronized Connection createOneConnection(int maxtries, int waitms) {
		for (int i = 0; i < maxtries && size() < max; i++) {
			try {
				final Connection conn = client.getConnection(ip, port);
				if (conn != null) {
					conn.addCloseListener(new CloseListener<Closeable, ICloseType>() {
						@Override
						public void onClosed(Closeable closeable, ICloseType type) throws IOException {
							log.debug("CheckHealth remove Connection!:" + closeable);
							if (closeable instanceof Connection) {
								removeObject((Connection) closeable);
							}
						}
					});
					final FramePacket pack = mss.getLocalModulesPacket();
					pack.putHeader(OSocketImpl.PACK_FROM, from_bcuid);
					log.debug("write LoginModulePack from {}", mss.getRmb().getNodeInfo().getUname());
					// log.trace("!!WriteLocalModulesPacket TO:" +
					// conn.getPeerAddress() + ",From="
					// + conn.getLocalAddress() + ",pack=" + pack.getFixHead());
					conn.write(pack);
					if (waitms > 0) {
						try {
							this.wait(waitms);
						} catch (InterruptedException e) {
						}
					}
					addObject(conn);
					return conn;
				} else {
					log.debug("cannot create Connection to " + ip + ":" + port);
				}
			} catch (TimeoutException te) {
				log.debug("Timeout:", te);
				return createOneConnectionBySubNode(maxtries);
			} catch (ExecutionException ce) {
				return createOneConnectionBySubNode(maxtries);
			} catch (Exception e) {
				// creating new Connection
				log.warn("error in create out conn:" + ip + ",port=" + port, e);
			}
		}
		log.debug("cannot get more Connection:cursize=" + size() + ",max=" + max);
		try {
			Thread.sleep(1000);
		} catch (Exception e) {
		}
		return null;
	}

}
