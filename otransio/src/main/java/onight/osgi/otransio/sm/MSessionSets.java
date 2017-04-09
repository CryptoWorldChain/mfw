package onight.osgi.otransio.sm;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.glassfish.grizzly.Connection;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.otransio.sm.RemoteModuleBean.ModuleBean;
import onight.tfw.otransio.api.PackHeader;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.otransio.api.session.ModuleSession;
import onight.tfw.outils.pool.ReusefulMapPool;

@Data
@Slf4j
public class MSessionSets {
	String currentNodeID;

	public MSessionSets(String currentNodeID) {
		this.currentNodeID = currentNodeID;
	}

	OutgoingSessionManager osm;

	HashMap<String, ReusefulMapPool<String, ModuleSession>> sessionByModule = new HashMap<>();
	HashMap<String, ModuleSession> localsessionByModule = new HashMap<>();

	AtomicLong recvCounter = new AtomicLong(0);
	AtomicLong sendCounter = new AtomicLong(0);
	AtomicLong duplCounter = new AtomicLong(0);
	AtomicLong dropCounter = new AtomicLong(0);
	AtomicLong allRCounter = new AtomicLong(0);
	AtomicLong allSCounter = new AtomicLong(0);
	// ConcurrentHashMap<String,HashSet<ModuleSession>> connsByNodeID=new
	// ConcurrentHashMap<String, HashSet<ModuleSession>>();

	RemoteModuleBean rmb = new RemoteModuleBean();

	public String getJsonInfo() {
		StringBuffer sb = new StringBuffer();
		sb.append("{\"locals\":[");
		int i = 0;
		for (Entry<String, ModuleSession> kv : localsessionByModule.entrySet()) {
			if (i > 0)
				sb.append(",");
			i++;
			sb.append(kv.getValue().getJsonStr());
		}
		sb.append("],\"stats\":{");
		sb.append("\"recv\":").append(recvCounter.get());
		sb.append(",\"send\":").append(sendCounter.get());
		sb.append(",\"allR\":").append(allRCounter.get());
		sb.append(",\"allS\":").append(allSCounter.get());
		sb.append(",\"drop\":").append(dropCounter.get());
		sb.append(",\"dupl\":").append(duplCounter.get());
		
		sb.append("},\"all\":[");
		i = 0;
		for (Entry<String, ReusefulMapPool<String, ModuleSession>> kv : sessionByModule.entrySet()) {
			if (i > 0)
				sb.append(",");
			i++;
			sb.append("{\"module\":\"" + kv.getKey() + "\"");
			sb.append(",\"sessions\":[");
			int v = 0;
			for (ModuleSession sm : kv.getValue().getAllObjs()) {
				if (v > 0)
					sb.append(",");
				v++;
				sb.append(sm.getJsonStr());
			}
			sb.append("]}");
		}
		sb.append("]");

		sb.append("}");
		return sb.toString();
	}

	public ModuleSession byModule(String module) {
		if (sessionByModule.containsKey(module)) {
			return sessionByModule.get(module).get();
		}
		return null;
	}

	public ModuleSession byModuleAndNodeID(String module, String nodeID) {
		if (sessionByModule.containsKey(module)) {
			return sessionByModule.get(module).getByKey(nodeID);
		}

		return null;
	}

	public synchronized ModuleSession addLocalMoudle(String module) {
		// localSessionsByModule.put(session.getModule(), session);
		if (!sessionByModule.containsKey(module)) {
			rmb.modules.add(new ModuleBean(module, this.currentNodeID));
		}
		return this.addModule(module, currentNodeID, null);
	}

	public Set<String> getLocalModules() {
		return sessionByModule.keySet();
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

	public ModuleSession getLocalModuleSession(String moduleid) {
		return localsessionByModule.get(moduleid);
	}

	public synchronized ModuleSession addModule(String moduleid, String nodeid, Connection conn) {
		ReusefulMapPool<String, ModuleSession> pool = sessionByModule.get(moduleid);
		if (pool == null) {
			pool = new ReusefulMapPool<String, ModuleSession>();
			sessionByModule.put(moduleid, pool);
		}
		ModuleSession ms = pool.getByKey(nodeid);
		if (ms == null) {
			if (nodeid.equals(currentNodeID)) {
				ms = new ModuleSession(moduleid);
				localsessionByModule.put(moduleid, ms);
			} else {
				ms = new RemoteModuleSession(moduleid, nodeid, this);
			}
			pool.addObject(nodeid, ms);
		}
		if (ms instanceof RemoteModuleSession) {
			RemoteModuleSession rms = (RemoteModuleSession) ms;
			rms.addConnection(conn);
		}
		return ms;
	}

	public synchronized void addOutogingModule(ModuleSession session, String nodeid) {
		ReusefulMapPool<String, ModuleSession> pool = sessionByModule.get(session.getModule());
		if (pool == null) {
			pool = new ReusefulMapPool<String, ModuleSession>();
			sessionByModule.put(session.getModule(), pool);
		}
		pool.addObject(nodeid, session);
	}

	public synchronized void removeLocalModule(String module) {
		removeModule(module, currentNodeID);
	}

	public synchronized void removeModule(String module, String nodeid) {
		ReusefulMapPool<String, ModuleSession> pool = sessionByModule.get(module);
		if (pool != null) {
			pool.removeByKey(nodeid);
			if (pool.size() <= 0) {
				sessionByModule.remove(module);
			}

		}
	}
}
