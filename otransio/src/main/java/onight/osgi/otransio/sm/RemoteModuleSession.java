package onight.osgi.otransio.sm;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

import org.glassfish.grizzly.CloseListener;
import org.glassfish.grizzly.Closeable;
import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.ICloseType;
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

	public RemoteModuleSession(NodeInfo nodeInfo, MSessionSets mss,CKConnPool ckpool) {
		this.mss = mss;
		this.nodeInfo = nodeInfo;
		this.connsPool = ckpool;
		
		writerQ = new PacketQueue(ckpool, mss.packet_buffer_size,mss.write_thread_count);
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
		FutureImpl<FramePacket> future = null;

		if (pack.isSync()) {
			// 发送到远程
			packid = genPackID();
			future = Futures.createSafeFuture();
			pack.putHeader(mss.packIDKey, packid);
			// pack.putHeader(OSocketImpl.PACK_FROM, "" +
			// NodeHelper.getCurrNodeIdx());
			pack.getExtHead().remove(OSocketImpl.PACK_TO);
//			future.addCompletionHandler(new CompletionHandler<FramePacket>() {
//				@Override
//				public void updated(FramePacket result) {
//				}
//
//				@Override
//				public void failed(Throwable throwable) {
//					handler.onFailed(new RuntimeException(throwable));
//				}
//
//				@Override
//				public void completed(FramePacket result) {
//					handler.onFinished(result);
//				}
//
//				@Override
//				public void cancelled() {
//
//					handler.onFailed(new RuntimeException("cancelled"));
//				}
//			});
			mss.packMaps.put(packid, handler);
			log.debug("sendPack:packid=" + packid + ",maps.size=" + mss.packMaps.size());

		}
		try {
//			sendCounter.incrementAndGet();
			writerQ.offer(pack,handler,future);
//			connsPool.sendMessage(pack);
//			sentCounter.incrementAndGet();
//			mss.sentCounter.incrementAndGet();
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

	public void destroy() {
		connsPool.setStop(true);
		Iterator<Connection> it = connsPool.iterator();
		FramePacket dropp = PacketHelper.buildFromBody(null, OSocketImpl.DROP_CONN);
		dropp.genBodyBytes();
		dropp.genHeader();
		while (it.hasNext()) {
			try {
				Connection conn = it.next();
				conn.write(dropp);
				conn.closeSilently();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
