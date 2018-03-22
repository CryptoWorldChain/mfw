package onight.osgi.otransio.ck;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;
import onight.osgi.otransio.nio.OClient;
import onight.osgi.otransio.sm.MSessionSets;

@Slf4j
public class NodeConnectionPool {

	public ConcurrentHashMap<String, CKConnPool> ckPoolByNodeName = new ConcurrentHashMap<String, CKConnPool>();

	public CKConnPool addPool(OClient client,String nodeName, String ip, int port, int core, int max, MSessionSets mss) {
		CKConnPool pool = ckPoolByNodeName.get(nodeName);
		if (pool == null) {
			pool = new CKConnPool(client, ip, port, core, max, mss);
			log.debug("create new Pool :" + pool);
			ckPoolByNodeName.put(nodeName, pool);
		}
		return pool;
	}
	
	public String getJsonStr(){
		StringBuffer sb=new StringBuffer();
		sb.append("[");
		int i=0;
		for(Entry<String,CKConnPool> sets:ckPoolByNodeName.entrySet()){
			if(i>0)sb.append(",");
			i++;
			sb.append("{\"nodename\":\""+sets.getKey()+"\"");
			sb.append(",\"conns\":"+sets.getValue().getJsonStr()+"");
			sb.append("}");
		}
		sb.append("]");
		return sb.toString();
	}
	
	public CKConnPool getPool(String nodeName){
		return ckPoolByNodeName.get(nodeName);
	}
	
	public void broadcastLocalModule(MSessionSets mss){
		
		for(CKConnPool pool:ckPoolByNodeName.values()){
			try {
				pool.broadcastMessage(mss.getLocalModulesPacket());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
