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
import onight.tfw.async.CompleteHandler;
import onight.tfw.mservice.NodeHelper;
import onight.tfw.otransio.api.MessageException;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.otransio.api.beans.SendFailedBody;
import onight.tfw.otransio.api.session.PSession;

@Slf4j
@Data
public class RemoteModuleSession extends PSession {

	@Setter
	@Getter
	CKConnPool connsPool = null;
	MSessionSets mss;
	NodeInfo nodeInfo;
	AtomicLong counter = new AtomicLong(0);

	final String rand = "r_" + String.format("%05d", (int) (Math.random() * 100000)) + "_";

	private String genPackID() {
		return rand + System.currentTimeMillis() + "_" + counter.incrementAndGet();
	}

	public String getJsonStr() {
		StringBuffer sb = new StringBuffer();
		sb.append("{\"remoteid\":\"").append(nodeInfo.getNodeName()).append("\"");
		sb.append(",\"channels\":").append(connsPool.size()).append("");
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

	public RemoteModuleSession(NodeInfo nodeInfo, MSessionSets mss) {
		this.mss = mss;
		this.nodeInfo = nodeInfo;
	}

	public RemoteModuleSession addConnection(Connection<?> conn) {
		if (conn != null && connsPool.addObject(conn)) {
			conn.addCloseListener(new CloseListener<Closeable, ICloseType>() {
				@Override
				public void onClosed(Closeable closeable, ICloseType type) throws IOException {
					log.info("RemoteModuleSession remove Connection!:" + closeable);
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
			log.info("Remove RemoteModule Session:@" + this);
		}
		return this;
	}

	@Override
	public void onPacket(final FramePacket pack, final CompleteHandler handler) {
		if (pack.isSync()) {
			FutureImpl<FramePacket> future = null;
			String packid = null;
			// 发送到远程
			packid = genPackID();
			future = Futures.createSafeFuture();
			pack.putHeader(mss.packIDKey, packid);
			pack.putHeader(OSocketImpl.PACK_FROM, "" + NodeHelper.getCurrNodeIdx());
			pack.getExtHead().remove(OSocketImpl.PACK_TO);
			pack.getExtHead().remove(OSocketImpl.PACK_TO_IDX);
			future.addCompletionHandler(new CompletionHandler<FramePacket>() {
				@Override
				public void updated(FramePacket result) {
				}

				@Override
				public void failed(Throwable throwable) {
					handler.onFinished(PacketHelper.toPBReturn(pack, new SendFailedBody(mss.packIDKey, pack)));
				}

				@Override
				public void completed(FramePacket result) {
					handler.onFinished(result);
				}

				@Override
				public void cancelled() {
					handler.onFinished(PacketHelper.toPBReturn(pack, new SendFailedBody(mss.packIDKey, pack)));
				}
			});
			mss.packMaps.put(packid, future);
			log.debug("sendPack:packid=" + packid + ",maps.size=" + mss.packMaps.size());

		}

		try {
			connsPool.sendMessage(pack);
		} catch (MessageException me) {

			throw me;
		} catch (Exception e) {
			log.error("sendMessageError:" + pack, e);
			throw new MessageException(e);
		}
	}
	
	public void destroy(){
		connsPool.setStop(true);
		Iterator<Connection> it=connsPool.iterator();
		while(it.hasNext()){
			try {
				it.next().close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
