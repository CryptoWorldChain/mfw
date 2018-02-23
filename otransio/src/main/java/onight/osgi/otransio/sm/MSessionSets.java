package onight.osgi.otransio.sm;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.impl.FutureImpl;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.otransio.impl.NodeInfo;
import onight.tfw.mservice.NodeHelper;
import onight.tfw.otransio.api.PackHeader;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.otransio.api.session.LocalModuleSession;
import onight.tfw.outils.serialize.UUIDGenerator;

@Data
@Slf4j
public class MSessionSets {

	String packIDKey = "";

	public MSessionSets() {
		packIDKey = UUIDGenerator.generate() + "." + NodeHelper.getCurrNodeIdx() + ".SID";
	}

	OutgoingSessionManager osm;

	HashMap<Integer, RemoteModuleSession> sessionByNodeIdx = new HashMap<>();

	HashMap<String, LocalModuleSession> localsessionByModule = new HashMap<>();

	ConcurrentHashMap<String, FutureImpl<FramePacket>> packMaps = new ConcurrentHashMap<>();
	AtomicLong recvCounter = new AtomicLong(0);
	AtomicLong sendCounter = new AtomicLong(0);
	AtomicLong duplCounter = new AtomicLong(0);
	AtomicLong dropCounter = new AtomicLong(0);
	AtomicLong allRCounter = new AtomicLong(0);
	AtomicLong allSCounter = new AtomicLong(0);
	// ConcurrentHashMap<String,HashSet<PSession>> connsByNodeID=new
	// ConcurrentHashMap<String, HashSet<PSession>>();

	RemoteModuleBean rmb = new RemoteModuleBean();

	public String getJsonInfo() {
		StringBuffer sb = new StringBuffer();
		sb.append("{\"locals\":[");
		int i = 0;
		for (Entry<Integer, RemoteModuleSession> kv : sessionByNodeIdx.entrySet()) {
			if (i > 0)
				sb.append(",");
			i++;
			sb.append(kv.getValue().getMmid());
		}
		sb.append("],\"stats\":{");
		sb.append("\"recv\":").append(recvCounter.get());
		sb.append(",\"send\":").append(sendCounter.get());
		sb.append(",\"allR\":").append(allRCounter.get());
		sb.append(",\"allS\":").append(allSCounter.get());
		sb.append(",\"drop\":").append(dropCounter.get());
		sb.append(",\"dupl\":").append(duplCounter.get());

		sb.append("}");
		return sb.toString();
	}

	public RemoteModuleSession byNodeIdx(Integer idx) {
		if (sessionByNodeIdx.containsKey(idx)) {
			return sessionByNodeIdx.get(idx);
		}
		return null;
	}

	public RemoteModuleSession byNodeName(String name) {
		if (StringUtils.isBlank(name))
			return null;
		Integer idx = 0;
		try {
			idx = Integer.parseInt(name);
		} catch (NumberFormatException e) {
			idx = name.hashCode();
		}
		return sessionByNodeIdx.get(idx);
	}

	public synchronized RemoteModuleSession addRemoteSession(NodeInfo node, Connection<?> conn) {
		RemoteModuleSession session = sessionByNodeIdx.get(node.getNodeIdx());
		if (session == null) {
			session = new RemoteModuleSession(node, this);
			sessionByNodeIdx.put(node.getNodeIdx(), session);
		} //
			// TODO: all connection should be verify!
		if (conn != null && conn.isOpen()) {
			session.addConnection(conn);
		}

		return session;

	}

	public FramePacket getLocalModulesPacket() {
		FramePacket ret = PacketHelper.genSyncPack(PackHeader.REMOTE_LOGIN, PackHeader.REMOTE_MODULE, rmb);
		log.debug("getLocalModulePack:" + ret.getFixHead().toStrHead() + ":" + rmb);
		return ret;
	}

	public FramePacket getLocalModulesPacketBack() {
		FramePacket ret = PacketHelper.genSyncPack(PackHeader.REMOTE_LOGIN_RET, PackHeader.REMOTE_MODULE, rmb);
		log.debug("getLocalModulePack.back:" + ret.getFixHead().toStrHead() + ":" + rmb);
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

	public void dropSession(String name) {
		if (StringUtils.isNotBlank(name)) {
			Integer idx = 0;
			try {
				idx = Integer.parseInt(name);
			} catch (NumberFormatException e) {
				idx = name.hashCode();
			}
			RemoteModuleSession session=sessionByNodeIdx.remove(idx);
			session.destroy();
			
		}
	}

}
