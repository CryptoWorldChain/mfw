package onight.osgi.otransio.ck;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.TimeoutException;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.otransio.impl.NodeInfo;
import onight.osgi.otransio.nio.OClient;
import onight.osgi.otransio.sm.MSessionSets;
import onight.tfw.otransio.api.MessageException;
import onight.tfw.otransio.api.PackHeader;
import onight.tfw.otransio.api.beans.ExtHeader;
import onight.tfw.otransio.api.beans.FixHeader;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.pool.ReusefulLoopPool;

import org.apache.commons.lang3.StringUtils;
import org.glassfish.grizzly.CloseListener;
import org.glassfish.grizzly.Closeable;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.ICloseType;
import org.glassfish.grizzly.nio.transport.TCPNIOConnection;

@Data
@Slf4j
public class CKConnPool extends ReusefulLoopPool<Connection> {
	OClient client;
	String ip;
	int port;
	int core;
	int max;
	boolean stop = false;
	String aliasURI = "";

	MSessionSets mss;

	public void setStop(boolean isstop) {
		this.stop = true;
		if (core <= 0) {
			mss.dropSession(nameid);
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

	public synchronized Connection createOneConnection() {
		return createOneConnection(1);
	}

	public void sendMessage(final FramePacket pack) throws MessageException {
		boolean writed = false;
		for (int i = 0; i < 3; i++) {
			Connection conn = null;
			try {
				conn = borrow();
				if (conn != null) {
					if (conn.isOpen()) {
						if(conn.write(pack)!=null)
						{
							writed = true;
						}
						break;
					} else {
						removeObject(conn);
						conn = null;
					}
				}
				createOneConnection(1);
				Thread.sleep(100);
			} catch (Exception e) {
				log.error("sendMessageError:" + pack, e);
				throw new MessageException(e);
			} finally {
				if (conn != null && conn.isOpen()) {
					retobj(conn);
				}
			}
		}
		if(!writed){
			throw new MessageException("No More Connections");
		}
	}

	public synchronized Connection createOneConnection(int maxtries) {
		for (int i = 0; i < maxtries && size() < max; i++) {
			try {
				final Connection conn = client.getConnection(ip, port);
				if (conn != null) {
					conn.addCloseListener(new CloseListener<Closeable, ICloseType>() {
						@Override
						public void onClosed(Closeable closeable, ICloseType type) throws IOException {
							log.info("CheckHealth remove Connection!:" + closeable);
							if (closeable instanceof Connection) {
								removeObject((Connection) closeable);
							}
						}
					});
					final FramePacket pack = mss.getLocalModulesPacket();
					log.debug("write localmodulepack:" + pack + ",writable==" + conn.canWrite());
					log.trace("!!WriteLocalModulesPacket TO:" + conn.getPeerAddress() + ",From="
							+ conn.getLocalAddress() + ",pack=" + pack.getFixHead());
					//
					conn.write(pack);
					this.addObject(conn);
					return conn;
				}
			} catch (TimeoutException te) {
				log.debug("Timeout:",te);
				if (StringUtils.isNotBlank(aliasURI))//try alias
					try {
						NodeInfo newin = NodeInfo.fromURI(aliasURI, this.nameid);
						if (newin != null) {
							this.ip = newin.getAddr();
							this.port = newin.getPort();
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

			} catch (Exception e) {
				// creating new Connection
				log.warn("error in create out conn:" + ip + ",port=" + port, e);
			}
		}
		return null;
	}

	public void broadcastMessage(Object msg) {
		Iterator<Connection> it = this.iterator();
		while (it.hasNext()) {
			it.next().write(msg);
		}
	}
}
