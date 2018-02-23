package onight.osgi.otransio.ck;

import java.io.IOException;
import java.util.Iterator;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.otransio.nio.OClient;
import onight.osgi.otransio.sm.MSessionSets;
import onight.tfw.otransio.api.MessageException;
import onight.tfw.otransio.api.PackHeader;
import onight.tfw.otransio.api.beans.ExtHeader;
import onight.tfw.otransio.api.beans.FixHeader;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.pool.ReusefulLoopPool;

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

	MSessionSets mss;

	public CKConnPool(OClient client, String ip, int port, int core, int max, MSessionSets mss) {
		super();
		this.client = client;
		this.ip = ip;
		this.port = port;
		this.core = core;
		this.max = max;
		this.mss = mss;
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
		return createOneConnection(5);
	}

	public void sendMessage(final FramePacket pack) throws MessageException{
		for (int i = 0; i < 3; i++) {
			Connection conn = null;
			try {
				conn = borrow();
				if (conn != null) {
					if (conn.isOpen()) {
						conn.write(pack);
						break;
					} else {
						removeObject(conn);
						conn = null;
					}
				}
				Thread.sleep(100);
				createOneConnection(3);
			} catch (Exception e) {
				log.error("sendMessageError:" + pack, e);
				throw new MessageException(e);
			} finally {
				if (conn != null && conn.isOpen()) {
					retobj(conn);
				}
			}
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
			} catch (Exception e) {
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
