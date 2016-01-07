package onight.osgi.otransio.sm;

import java.util.HashMap;

import lombok.Data;
import onight.osgi.otransio.sm.RemoteModuleBean.ModuleBean;
import onight.tfw.otransio.api.PackHeader;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.otransio.api.session.ModuleSession;
import onight.tfw.outils.pool.ReusefulMapPool;

import org.glassfish.grizzly.Connection;

@Data
public class MSessionSets {
	String currentNodeID;

	public MSessionSets(String currentNodeID) {
		this.currentNodeID = currentNodeID;
	}

	HashMap<String, ReusefulMapPool<String, ModuleSession>> sessionByModule = new HashMap<>();

	// ConcurrentHashMap<String,HashSet<ModuleSession>> connsByNodeID=new
	// ConcurrentHashMap<String, HashSet<ModuleSession>>();

	RemoteModuleBean rmb = new RemoteModuleBean();

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

	public FramePacket getLocalModulesPacket() {
		return PacketHelper.genSyncPack(PackHeader.REMOTE_LOGIN, PackHeader.REMOTE_MODULE, rmb);
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
