package onight.osgi.otransio.sm;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;

import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.otransio.ck.CKConnPool;
import onight.osgi.otransio.impl.NodeInfo;
import onight.osgi.otransio.nio.PacketTuple;
import onight.osgi.otransio.util.PacketTuplePool;
import onight.osgi.otransio.util.PacketWriterPool;
import onight.tfw.async.CompleteHandler;
import onight.tfw.otransio.api.PackHeader;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.otransio.api.session.LocalModuleSession;
import onight.tfw.otransio.api.session.PSession;
import onight.tfw.outils.conf.PropHelper;
import onight.tfw.outils.serialize.UUIDGenerator;

@Data
@Slf4j
public class MSessionSets {

	String packIDKey = "";
	private PropHelper params;

	int packet_buffer_size = 10;
	// int write_thread_count = 10;

	PacketTuplePool packPool;
	PacketWriterPool writerPool;
	ConcurrentHashMap<String, PacketTuple> resendMap = new ConcurrentHashMap<>();
	ConcurrentHashMap<String, Long> duplicateCheckMap = new ConcurrentHashMap<>();
	int max_packet_buffer = 10;
	ForkJoinPool exec;
	ForkJoinPool readerexec;
	ForkJoinPool writerexec;

	int resendBufferSize = 100000;
	int resendTimeOutMS = 60000;
	int resendTimeMS = 3000;
	int resendTryTimes = 5;
	AtomicLong resendTimes = new AtomicLong(0);
	AtomicLong resendPacketTimes = new AtomicLong(0);

	public MSessionSets(PropHelper params) {
		packIDKey = UUIDGenerator.generate() + ".SID";
		this.params = params;
		resendBufferSize = params.get("org.zippo.otransio.resend.buffer.size", 100000);
		resendTimeOutMS = params.get("org.zippo.otransio.resend.timeoutms", 60000);
		resendTimeMS = params.get("org.zippo.otransio.resend.timems", 3000);
		resendTryTimes = params.get("org.zippo.otransio.resend.try.times", 5);

		packet_buffer_size = params.get("org.zippo.otransio.maxpacketqueue", 10);
		// write_thread_count =
		// params.get("org.zippo.otransio.write_thread_count", 10);
		packPool = new PacketTuplePool(params.get("org.zippo.otransio.maxpackbuffer", 10000));
		writerPool = new PacketWriterPool(params.get("org.zippo.otransio.maxwriterbuffer", 1000));
		exec = new ForkJoinPool(
				params.get("org.zippo.otransio.exec.parrel", java.lang.Runtime.getRuntime().availableProcessors() * 2));
		writerexec = new ForkJoinPool(params.get("org.zippo.otransio.writerexec.parrel",
				java.lang.Runtime.getRuntime().availableProcessors() * 2));
		readerexec = new ForkJoinPool(params.get("org.zippo.otransio.readerexec.parrel",
				java.lang.Runtime.getRuntime().availableProcessors() * 2));
	}

	OutgoingSessionManager osm;

	HashMap<String, PSession> sessionByNodeName = new HashMap<>();
	HashMap<String, RemoteModuleSession> sessionByURI = new HashMap<>();

	HashMap<String, LocalModuleSession> localsessionByModule = new HashMap<>();

	ConcurrentHashMap<String, CompleteHandler> packMaps = new ConcurrentHashMap<>();

	// Cache<String, CompleteHandler> packMapsCache =
	// CacheBuilder.newBuilder().expireAfterWrite(120, TimeUnit.SECONDS)
	// .removalListener(new RemovalListener<String, CompleteHandler>() {
	// @Override
	// public void onRemoval(RemovalNotification<String, CompleteHandler>
	// notification) {
	// if (notification != null && notification.getValue() != null) {
	// notification.getValue().onFailed(new RuntimeException("Timeout"));
	// }
	// }
	// }).build();
	AtomicLong recvCounter = new AtomicLong(0);
	AtomicLong sendCounter = new AtomicLong(0);
	AtomicLong duplCounter = new AtomicLong(0);
	AtomicLong dropCounter = new AtomicLong(0);
	AtomicLong sentCounter = new AtomicLong(0);
	// AtomicLong allRCounter = new AtomicLong(0);
	// AtomicLong allSCounter = new AtomicLong(0);
	// ConcurrentHashMap<String,HashSet<PSession>> connsByNodeID=new
	// ConcurrentHashMap<String, HashSet<PSession>>();

	RemoteModuleBean rmb = new RemoteModuleBean();

	public String getJsonInfo() {
		StringBuffer sb = new StringBuffer();
		sb.append("{");
		sb.append("\"name\":\"").append(rmb.getNodeInfo().getNodeName()).append("\"");
		sb.append(",\"addr\":\"").append(rmb.getNodeInfo().getAddr()).append(":").append(rmb.getNodeInfo().getPort())
				.append("\"");
		int i = 0;
		sb.append(",\"recv\":").append(recvCounter.get());
		sb.append(",\"send\":").append(sendCounter.get());
		sb.append(",\"sent\":").append(sentCounter.get());
		sb.append(",\"execpool\":\"").append(exec.getActiveThreadCount() + "/" + exec.getPoolSize());
		sb.append(",\"readerexecpool\":\"").append(readerexec.getActiveThreadCount() + "/" + readerexec.getPoolSize());
		sb.append(",\"writerexecpool\":\"").append(writerexec.getActiveThreadCount() + "/" + writerexec.getPoolSize());
		sb.append(",\"pioresendsize\":").append(resendMap.size());
		sb.append(",\"pioduplicatesize\":").append(duplicateCheckMap.size());
		sb.append(",\"packchecksize\":").append(packMaps.size());
		sb.append(",\"resendtimes\":").append(resendTimes.get());
		sb.append(",\"resendpacktimes\":").append(resendPacketTimes.get());
		// sb.append(",\"allS\":").append(allSCounter.get());
		sb.append(",\"drop\":").append(dropCounter.get());
		sb.append(",\"dupl\":").append(duplCounter.get());
		sb.append(",\"queues\":[");
		i = 0;
		for (Entry<String, PSession> kv : sessionByNodeName.entrySet()) {
			if (i > 0)
				sb.append(",");
			if (kv.getValue() instanceof RemoteModuleSession) {
				sb.append(((RemoteModuleSession) kv.getValue()).getQueueJsonStr());
				i++;
			} else {
			}

		}
		sb.append("]");
		sb.append(",\"modules\":[");
		i = 0;
		for (Entry<String, LocalModuleSession> kv : localsessionByModule.entrySet()) {
			if (kv.getKey().length() > 0) {
				for (String cmd : kv.getValue().getServiceByCMD().keySet()) {
					if (i > 0)
						sb.append(",");
					i++;
					sb.append("\"").append(kv.getKey()).append(cmd).append("\"");
				}
			}
		}
		sb.append("]");
		sb.append(",\"sessions\":[");
		i = 0;
		for (Entry<String, PSession> kv : sessionByNodeName.entrySet()) {
			if (i > 0)
				sb.append(",");
			if (kv.getValue() instanceof RemoteModuleSession) {
				sb.append(((RemoteModuleSession) kv.getValue()).getJsonStr());
				i++;
			} else {
			}

		}
		sb.append("]");

		// sb.append(",\"osm\":").append(osm.getJsonInfo());
		sb.append("}");
		return sb.toString();
	}

	// public RemoteModuleSession byNodeIdx(Integer idx) {
	// if (sessionByNodeIdx.containsKey(idx)) {
	// return sessionByNodeIdx.get(idx);
	// }
	// return null;
	// }

	public PSession byNodeName(String name) {
		return sessionByNodeName.get(name);
	}

	public synchronized RemoteModuleSession addRemoteSession(NodeInfo node, CKConnPool ckpool) {
		PSession psession = sessionByNodeName.get(node.getNodeName());
		RemoteModuleSession session = null;
		if (psession == null) {
			String uri=node.getURI();
			session=sessionByURI.get(uri);
			if(session!=null){
				return session;
			}
			session = new RemoteModuleSession(node, this, ckpool);
			psession = session;
			sessionByNodeName.put(node.getNodeName(), psession);
			sessionByURI.put(uri, session);
			// session.setConnsPool(ckpool);
			// osm.ck.addCheckHealth(ckpool);
		} //
		return session;

	}

	public FramePacket getLocalModulesPacket() {
		FramePacket ret = PacketHelper.genSyncPack(PackHeader.REMOTE_LOGIN, PackHeader.REMOTE_MODULE, rmb);
		// log.debug("getLocalModulePack:" + ret.getFixHead().toStrHead() + ":"
		// + rmb);
		return ret;
	}

	public FramePacket getLocalModulesPacketBack() {
		FramePacket ret = PacketHelper.genSyncPack(PackHeader.REMOTE_LOGIN_RET, PackHeader.REMOTE_MODULE, rmb);
		// log.debug("getLocalModulePack.back:" + ret.getFixHead().toStrHead() +
		// ":" + rmb);
		return ret;
	}

	public synchronized LocalModuleSession addLocalMoudle(String module) {
		// localSessionsByModule.put(session.getModule(), session);
		LocalModuleSession lms = localsessionByModule.get(module);
		if (lms == null) {
			lms = new LocalModuleSession(module);
			localsessionByModule.put(module, lms);
		}
		return lms;
	}

	public synchronized void dropSession(String name, boolean sendDDNode) {
		if (StringUtils.isNotBlank(name)) {
			log.error("dropSession:" + name + ",sendDD=" + sendDDNode);
			PSession session = sessionByNodeName.remove(name);
			osm.rmNetPool(name);
			try {
				throw new RuntimeException("log drop:" + name);
			} catch (RuntimeException t) {
				log.error("drop session,", t);
			}
			if (session != null) {
				dropCounter.incrementAndGet();
				if (session instanceof RemoteModuleSession) {
					((RemoteModuleSession) session).destroy(sendDDNode);
				}
			}

		}
	}

	public synchronized void renameSession(String oldname, String newname) {
		if (StringUtils.isNotBlank(oldname) && StringUtils.isNotBlank(newname)
				&& !StringUtils.equals(oldname, newname)) {
			PSession session = sessionByNodeName.get(oldname);
			if (session != null) {
				session.setMmid(newname);
				osm.nodePool.changePoolName(oldname, newname);
				if (session instanceof RemoteModuleSession) {
					RemoteModuleSession rms = (RemoteModuleSession) session;
					rms.nodeInfo.setNodeName(newname);
					rms.writerQ.setName(newname);
				}
				sessionByNodeName.put(newname, session);
				sessionByNodeName.remove(oldname);
			}
		}
	}

}
