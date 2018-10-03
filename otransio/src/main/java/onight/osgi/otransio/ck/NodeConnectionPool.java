package onight.osgi.otransio.ck;

import java.util.HashMap;
import java.util.Map.Entry;

import lombok.extern.slf4j.Slf4j;
import onight.osgi.otransio.nio.OClient;
import onight.osgi.otransio.sm.MSessionSets;

@Slf4j
public class NodeConnectionPool {

	public HashMap<String, CKConnPool> ckPoolByNodeName = new HashMap<String, CKConnPool>();

	public synchronized CKConnPool addPool(OClient client, String nodeName, String ip, int port, int core, int max,
			MSessionSets mss) {
		CKConnPool pool = ckPoolByNodeName.get(nodeName);
		if (pool == null) {
			pool = new CKConnPool(client, ip, port, core, max, mss, nodeName);
			log.debug("create new Pool :" + nodeName + ", " + ip + ":" + port);
			ckPoolByNodeName.put(nodeName, pool);
		}
		return pool;
	}

	public String getJsonStr() {
		StringBuffer sb = new StringBuffer();
		sb.append("[");
		int i = 0;
		for (Entry<String, CKConnPool> sets : ckPoolByNodeName.entrySet()) {
			if (i > 0)
			{
				sb.append(",");
			}
			i++;
			sb.append("{\"nodename\":\"" + sets.getKey() + "\"");
			sb.append(",\"conns\":" + sets.getValue().getJsonStr() + "");
			sb.append("}");
		}
		sb.append("]");
		return sb.toString();
	}

	public synchronized CKConnPool getPool(String nodeName) {
		return ckPoolByNodeName.get(nodeName);
	}

	public synchronized void changePoolName(String oldname, String newname) {
		CKConnPool pool = ckPoolByNodeName.get(oldname);
		if (pool != null) {
			pool.setNameid(newname);
			CKConnPool existpool = ckPoolByNodeName.put(newname, pool);
			if (existpool != null && pool != existpool) {
				log.debug("stop exist pool for:" + existpool.getNameid() + ":" + existpool.getIp() + ":"
						+ existpool.getPort());
				existpool.setStop(true);
			}
		}
		ckPoolByNodeName.remove(oldname);
	}

	public synchronized CKConnPool destroyPool(String nodeName) {
		return ckPoolByNodeName.remove(nodeName);
	}

	public synchronized void removeByPool(CKConnPool pool) {
		CKConnPool localPool = ckPoolByNodeName.get(pool.nameid);
		if (localPool == pool) {
			ckPoolByNodeName.remove(pool.nameid);
		}
	}

	// public void broadcastLocalModule(MSessionSets mss) {
	// for (CKConnPool pool : ckPoolByNodeName.values()) {
	// try {
	// pool.broadcastMessage(mss.getLocalModulesPacket());
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }
	// }
}
