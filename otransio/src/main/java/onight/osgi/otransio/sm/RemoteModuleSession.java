package onight.osgi.otransio.sm;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import onight.tfw.async.CompleteHandler;
import onight.tfw.otransio.api.MessageException;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.otransio.api.beans.LoopPackBody;
import onight.tfw.otransio.api.beans.SendFailedBody;
import onight.tfw.otransio.api.session.ModuleSession;
import onight.tfw.outils.pool.ReusefulLoopPool;
import onight.tfw.outils.serialize.SerializerUtil;

import org.glassfish.grizzly.CloseListener;
import org.glassfish.grizzly.Closeable;
import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.ICloseType;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.utils.Futures;

@Slf4j
public class RemoteModuleSession extends ModuleSession {

	@Setter
	@Getter
	ReusefulLoopPool<Connection> connsPool = new ReusefulLoopPool<>();
	MSessionSets mss;
	String remoteNodeID;
	String packIDKey;

	private ConcurrentHashMap<String, FutureImpl<FramePacket>> packMaps = new ConcurrentHashMap<>();

	AtomicLong counter = new AtomicLong(0);

	final String rand = "r_" + String.format("%05d", (int) (Math.random() * 100000)) + "_";

	private String genPackID() {
		return rand + System.currentTimeMillis() + "_" + counter.incrementAndGet();
	}

	public RemoteModuleSession(String moduleid, String remoteNodeID, MSessionSets mss) {
		super(moduleid);
		this.mss = mss;
		this.remoteNodeID = remoteNodeID;
		packIDKey = moduleid + "." + mss.getCurrentNodeID() + ".SID";
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
			log.info("Remove RemoteModule Session:" + module + ",@" + this);
			mss.removeModule(this.module, remoteNodeID);
		}
		return this;
	}

	@Override
	public void onPacket(final FramePacket pack, final CompleteHandler handler) {

		FutureImpl<FramePacket> future = null;
		String packid = null;
		if (pack.isSync()) {
			packid = genPackID();
			if (pack.getExtHead().isExist(packIDKey)) {
				// 检查是否为响应包
				String expackid=pack.getExtProp(packIDKey);
				future = packMaps.remove(expackid);
				if (future != null) {
					pack.getExts().remove(packIDKey);
					future.result(pack);
				} else {
					log.warn("unknow ack:" + expackid + ",module=" + this.getModule() + ",packid=" + pack.getExtProp(packIDKey));
					handler.onFinished(PacketHelper.toPBReturn(pack, new LoopPackBody(packIDKey, pack)));
				}
				return;
			}
			future = Futures.createSafeFuture();
			pack.putHeader(packIDKey, packid);
			packMaps.put(packid, future);
			log.debug("sendPack:packid=" + packid + ",@nodid=" + mss.currentNodeID + ",module=" + this + ",maps.size=" + packMaps.size());
		} else {
			log.debug("postPack:=" + ",@nodid=" + mss.currentNodeID + ",module=" + this + ",maps.size=" + packMaps.size());
		}
		// 发送到远程
		for (int i = 0; i < 3; i++) {
			try {
				Connection conn = connsPool.get();
				if (conn != null) {
					connsPool.get().write(pack);
					break;
				} else {
					Thread.sleep(100);
				}

			} catch (Exception e) {
				log.error("sendMessageError:" + pack, e);
				if(packid!=null){
					packMaps.remove(packid);
				}
				throw new MessageException(e);
			}
		}
		if (pack.isSync()) {// 如果是同步接口，则要等返回
			future.addCompletionHandler(new CompletionHandler<FramePacket>() {
				@Override
				public void updated(FramePacket result) {
				}

				@Override
				public void failed(Throwable throwable) {
					handler.onFinished(PacketHelper.toPBReturn(pack, new SendFailedBody(packIDKey, pack)));
				}

				@Override
				public void completed(FramePacket result) {
					handler.onFinished(result);
				}

				@Override
				public void cancelled() {
					handler.onFinished(PacketHelper.toPBReturn(pack, new SendFailedBody(packIDKey, pack)));
				}
			});
			// connsPool.get().write(message, completionHandler);
		}
	}

}
