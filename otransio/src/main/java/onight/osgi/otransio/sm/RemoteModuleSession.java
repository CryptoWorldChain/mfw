package onight.osgi.otransio.sm;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

import org.glassfish.grizzly.CloseListener;
import org.glassfish.grizzly.Closeable;
import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.ICloseType;
import org.glassfish.grizzly.WriteResult;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.utils.Futures;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.otransio.ck.CKConnPool;
import onight.osgi.otransio.impl.NodeInfo;
import onight.osgi.otransio.impl.OSocketImpl;
import onight.osgi.otransio.nio.PacketQueue;
import onight.tfw.async.CompleteHandler;
import onight.tfw.otransio.api.MessageException;
import onight.tfw.otransio.api.PackHeader;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.otransio.api.session.PSession;

@Slf4j
@Data
public class RemoteModuleSession extends PSession {

	@Setter
	@Getter
	CKConnPool connsPool = null;
	MSessionSets mss;
	NodeInfo nodeInfo;
	AtomicLong sendCounter = new AtomicLong(0);
	AtomicLong dropCounter = new AtomicLong(0);
	AtomicLong sentCounter = new AtomicLong(0);
	AtomicLong recvCounter = new AtomicLong(0);

	AtomicLong counter = new AtomicLong(0);

	PacketQueue writerQ;
	final String rand = "r_" + String.format("%05d", (int) (Math.random() * 100000)) + "_";

	@Override
	public String toString() {
		return "RemoteModuleSession(" + nodeInfo.getNodeName() + ")";
	}

	private String genPackID() {
		return rand + System.currentTimeMillis() + "_" + counter.incrementAndGet();
	}

	public String getJsonStr() {
		StringBuffer sb = new StringBuffer();
		sb.append("{\"remoteid\":\"").append(nodeInfo.getNodeName()).append("\"");
		sb.append(",\"alias\":\"").append(connsPool.getSubnodeURI()).append("\"");
		sb.append(",\"channels\":").append(connsPool.size()).append("");
		sb.append(",\"recvcc\":").append(recvCounter.get()).append("");
		sb.append(",\"sentcc\":").append(sendCounter.get()).append("");
		sb.append(",\"sendcc\":").append(sentCounter.get()).append("");
		sb.append(",\"dropcc\":").append(dropCounter.get()).append("");
		sb.append(",\"core\":").append(nodeInfo.getCore()).append("");
		sb.append(",\"max\":").append(nodeInfo.getMax()).append("");
		sb.append(",\"uri\":\"").append(nodeInfo.getAddr() + ":" + nodeInfo.getPort()).append("\"");

		sb.append(",\"chdetails\":[");
		Iterator<Connection> it = connsPool.iterator();
		while (it.hasNext()) {
			Connection conn = it.next();
			sb.append("{\"local\":\"").append(conn.getLocalAddress()).append("\"");
			sb.append(",\"peer\":\"").append(conn.getPeerAddress()).append("\"");
			sb.append("}");
			if (it.hasNext())
				sb.append(",");
		}
		sb.append("]");
		sb.append("}");
		return sb.toString();
	}

	public RemoteModuleSession(NodeInfo nodeInfo, MSessionSets mss, CKConnPool ckpool) {
		this.mss = mss;
		this.nodeInfo = nodeInfo;
		this.connsPool = ckpool;

		writerQ = new PacketQueue(ckpool, mss.packet_buffer_size, mss.write_thread_count);
	}

	public RemoteModuleSession addConnection(Connection<?> conn) {
		if (conn != null && connsPool.addObject(conn)) {
			conn.addCloseListener(new CloseListener<Closeable, ICloseType>() {
				@Override
				public void onClosed(Closeable closeable, ICloseType type) throws IOException {
					log.debug("RemoteModuleSession remove Connection!:" + closeable);
					if (closeable instanceof Connection) {
						removeConnection((Connection) closeable);
					}
				}
			});
		}
		return this;
	}

	public RemoteModuleSession removeConnection(Connection<?> conn) {
		connsPool.removeObject(conn);
		if (connsPool.size() <= 0) {
			log.debug("Remove RemoteModule Session:@" + this);
		}
		dropCounter.incrementAndGet();
		return this;
	}

	@Override
	public void onPacket(final FramePacket pack, final CompleteHandler handler) {
		String packid = null;
		// FutureImpl<FramePacket> future = null;
		CompleteHandler rehandler = handler;
		if (pack.isSync()) {
			// 发送到远程
			packid = genPackID();
			// future = Futures.createSafeFuture();
			pack.putHeader(mss.packIDKey, packid);
			pack.getExtHead().remove(OSocketImpl.PACK_TO);
			final String fpackid = packid;
			rehandler = new CompleteHandler() {
				@Override
				public void onFinished(FramePacket arg0) {
					mss.packMaps.remove(fpackid);
					handler.onFinished(arg0);
				}

				@Override
				public void onFailed(Exception arg0) {
					mss.packMaps.remove(fpackid);
					handler.onFailed(arg0);
				}
			};
			mss.packMaps.put(packid, handler);
			log.debug("sendSyncPack:packid=" + packid + ",maps.size=" + mss.packMaps.size());

		}
		try {
			writerQ.offer(pack, rehandler);
		} catch (MessageException me) {
			if (packid != null && pack.isSync()) {
				handler.onFailed(me);
				mss.packMaps.remove(packid);
			}
			throw me;
		} catch (Exception e) {
			log.error("sendMessageError:" + pack, e);
			if (packid != null && pack.isSync()) {
				handler.onFailed(e);
				mss.packMaps.remove(packid);
			}
			throw new MessageException(e);
		}
	}

	public void destroy(boolean sendDDNode) {
		connsPool.setStop(true);
		writerQ.setStop(true);
		Iterator<Connection> it = connsPool.iterator();
		FramePacket dropp = PacketHelper.genSyncPack("DRO", "P**", connsPool.getNameid());
		dropp.genBodyBytes();
		dropp.genHeader();
		int cc = 0;
		while (it.hasNext()) {
			try {
				final Connection conn = it.next();
				if (cc == 0 && sendDDNode) {
					conn.write(dropp, new CompletionHandler() {
						@Override
						public void cancelled() {
							try {
								conn.close();
								
							} catch (Exception e) {
							}
						}
						@Override
						public void failed(Throwable throwable) {
							try {
								conn.close();
							} catch (Exception e) {
							}
						}

						@Override
						public void completed(Object result) {
							try {
								conn.close();
							} catch (Exception e) {
							}
						}

						@Override
						public void updated(Object result) {
						}

					});
				} else {
					conn.close();
				}
				cc++;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
