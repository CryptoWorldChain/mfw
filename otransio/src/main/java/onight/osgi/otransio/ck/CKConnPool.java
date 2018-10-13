package onight.osgi.otransio.ck;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
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
	// String from_bcuid = "";

	ArrayList<NodeInfo> subNodes = new ArrayList<>();

	MSessionSets mss;

	public void setStop(boolean isstop) {
		this.stop = true;
		if (core <= 0) {
			log.error("setDrop:" + nameid);
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
		Connection<?> conn = null;
		int trytimes = size();
		for (int i = 0; i < trytimes; i++) {
			conn = borrow();
			if (conn == null) {
				// log.error("get a null conn:" + getActiveObjs().size() +
				// ",try=" + trytimes);
			} else if (conn.isOpen()) {
				return conn;
			} else {
				log.error("remove close connection:" + conn + ",size=F" + getActiveObjs().size() + ",try=" + trytimes);
			}
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		conn = createOneConnection(1, 10);
		if (conn != null) {
			mss.getOsm().getCk().addCheckHealth(conn);
		} else if (size() > 0) {
			log.error("cannot get more Connection:cursize=" + size() + ",max=" + max + ",try=" + trytimes + ",nameid="
					+ nameid + ",ip=" + ip + ",port=" + port);
		}
		return conn;
	}

	public synchronized Connection createOneConnection(int maxtries, int waitms) {
		for (int i = 0; i < maxtries && size() < max; i++) {
			try {
				final Connection conn = client.getConnection(ip, port);
				if (conn != null) {
					conn.addCloseListener(new CloseListener<Closeable, ICloseType>() {
						@Override
						public void onClosed(Closeable closeable, ICloseType type) throws IOException {
							log.debug("CheckHealth onClosed Connection!:" + closeable);
							if (closeable instanceof Connection) {
								removeObject((Connection) closeable);
							}
						}
					});
					final FramePacket pack = mss.getLocalModulesPacket();
					pack.putHeader(OSocketImpl.PACK_FROM, mss.getRmb().getNodeInfo().getNodeName());
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
					this.getAllObjs().put(conn, conn);
					return conn;
				} else {
					log.error("cannot create Connection to " + ip + ":" + port);
				}
			} catch (TimeoutException te) {
				log.error("TimeoutConnect:to=" + ip + ":" + port + ",name=" + nameid, te);
				// return createOneConnectionBySubNode(maxtries);
			} catch (ExecutionException ce) {
				// java.net.ConnectException: Connection refused
				log.debug("ExecutionException=" + ip + ":" + port + ",name=" + nameid, ce);
				break;
				// return createOneConnectionBySubNode(maxtries);
			} catch (Exception e) {
				// creating new Connection
				log.error("error in create out conn:" + ip + ",port=" + port, e);
			}
		}
		if (waitms <= 0) {
			log.error("cannot get more Connection:cursize=" + size() + ",max=" + max+",ip="+ip + ",port=" + port+",name="+nameid);
			try {
				Thread.sleep(100);
			} catch (Exception e) {
			}
			return null;
		}
		if (size() >= max) {
			Iterator<Connection> it = this.iterator();
			List<Connection> rmList = new ArrayList<>();
			while (it.hasNext()) {
				Connection conn = it.next();
				if (conn != null && !conn.isOpen()) {
					rmList.add(conn);
				}
			}
			for (Connection conn : rmList) {
				removeObject(conn);
			}
			if (size() < max) {
				return createOneConnection(1, -1);
			}
		}
		try {
			Thread.sleep(100);
		} catch (Exception e) {
		}
		return null;
	}

}
