package onight.osgi.otransio.ck;

import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;
import onight.osgi.otransio.nio.OClient;
import onight.osgi.otransio.sm.MSessionSets;

@Slf4j
public class NodeConnectionPool {

	public ConcurrentHashMap<String, CKConnPool> ckPoolByNodeID = new ConcurrentHashMap<String, CKConnPool>();

	
	
	public CKConnPool addPool(OClient client,String nodeID, String ip, int port, int core, int max, MSessionSets mss) {
		CKConnPool pool = ckPoolByNodeID.get(nodeID);
		if (pool == null) {
			pool = new CKConnPool(client, ip, port, core, max, mss);
			log.debug("create new Pool :" + pool);
			ckPoolByNodeID.put(nodeID, pool);
		}
		return pool;
	}
	
	public CKConnPool getPool(String nodeID){
		return ckPoolByNodeID.get(nodeID);
	}
	
	public void broadcastLocalModule(MSessionSets mss){
		
		for(CKConnPool pool:ckPoolByNodeID.values()){
			try {
				pool.broadcastMessage(mss.getLocalModulesPacket());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
