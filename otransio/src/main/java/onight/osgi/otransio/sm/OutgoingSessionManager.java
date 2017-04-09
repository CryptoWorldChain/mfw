package onight.osgi.otransio.sm;

import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.otransio.ck.CKConnPool;
import onight.osgi.otransio.ck.CheckHealth;
import onight.osgi.otransio.ck.NodeConnectionPool;
import onight.osgi.otransio.exception.NoneServerException;
import onight.osgi.otransio.impl.OSocketImpl;
import onight.osgi.otransio.nio.OClient;
import onight.tfw.mservice.NodeHelper;
import onight.tfw.otransio.api.session.ModuleSession;
import onight.tfw.outils.conf.PropHelper;
import onight.tfw.outils.conf.PropHelper.IFinder;

@Slf4j
public class OutgoingSessionManager {

	PropHelper params;
	@Getter
	OClient client;

	MSessionSets mss;
	CheckHealth ck;

	@Getter
	@Setter
	boolean ready;
	@Getter
	NodeConnectionPool nodePool;
	
	
	public String getJsonInfo()
	{
		StringBuffer sb=new StringBuffer();
		sb.append("{\"cur\":{");
			sb.append("\"nodeid\":\"").append(mss.currentNodeID).append("\"");
			sb.append(",\"inaddr\":\"").append(NodeHelper.getCurrNodeListenInAddr()).append("\"");
			sb.append(",\"inport\":\"").append(NodeHelper.getCurrNodeListenInPort()).append("\"");
			sb.append(",\"outaddr\":\"").append(NodeHelper.getCurrNodeListenOutAddr()).append("\"");
			sb.append(",\"outport\":\"").append(NodeHelper.getCurrNodeListenOutPort()).append("\"");
			sb.append("}");
		sb.append(",\"conns\":").append(nodePool.getJsonStr());
		sb.append("}");
		return sb.toString();
	}
	public OutgoingSessionManager(OSocketImpl oimpl, PropHelper params, MSessionSets mss) {
		this.params = params;
		this.mss = mss;
		client = new OClient(oimpl);
		client.init(this, params);
		ck = new CheckHealth(params.get("otrans.checkhealth.size", 2), params.get("otrans.checkhealth.delay", 30));
	}
	
	public synchronized void initNetPool() {
		nodePool = new NodeConnectionPool();
		final HashSet<String> nodeNames = new HashSet<String>();
		params.findMatch("otrans.servers.node.*", new IFinder() {
			@Override
			public void onMatch(String key, String v) {
				nodeNames.add(key.substring("otrans.servers.node.".length()).split("\\.")[0]);
			}
		});

		for (String key : nodeNames) {
			String node = key;
			key = "otrans.servers.node." + key;
			String address = params.get(key + ".addr", "127.0.0.1");
			int port = params.get(key + ".port", params.get("otrans.servers.default.port", 5100));
			int core = params.get(key + ".core", params.get("otrans.servers.default.core", 2));
			int max = params.get(key + ".max", params.get("otrans.servers.default.max", 10));
			nodePool.addPool(client, node, address, port, core, max, mss);
		}

	}

	public synchronized void addNetPool(String nodeID, String addrport) {
		if (addrport == null)
			return;
		String addrports[] = addrport.split(":");
		if (addrports.length != 2)
			return;
		if(StringUtils.equalsIgnoreCase(nodeID, NodeHelper.getCurrNodeID())){
			log.debug("same node ,not need to add net pool");
			return;
		}

		try {
			String addr = addrports[0].trim();
			int port = Integer.parseInt(addrports[1].trim());
			String key = "otrans.servers.node." + nodeID;
			int core = params.get(key + ".core", params.get("otrans.servers.default.core", 2));
			int max = params.get(key + ".max", params.get("otrans.servers.default.max", 10));
			CKConnPool pool = nodePool.getPool(nodeID);//unknow modules
			if (pool == null) {
				pool = nodePool.addPool(client, nodeID, addr, port, core, max, mss);
				ck.addCheckHealth(pool);
			}

		} catch (Exception e) {
		}

	}

	public synchronized void init() {

		initNetPool();

		final HashSet<String> mmidNames = new HashSet<String>();

		params.findMatch("otrans.servers.mmid.*", new IFinder() {
			@Override
			public void onMatch(String key, String v) {
				String module = key.substring("otrans.servers.mmid.".length()).split("\\.")[0];
				mmidNames.add(module);
			}
		});
		// String mmids = params.get("otrans.servers.mmids", "");
		log.info("otrans.servers.mmids=" + mmidNames);
		for (String mmid : mmidNames) {
			try {
				createOutgoingSS(mmid);
			} catch (NoneServerException e) {
				log.warn("error in creating servers:" + mmid);
			}
		}
		this.ready = true;
	}

	public void createOutgoingSS(String module) throws NoneServerException {
		String nodeIDs = params.get("otrans.servers.mmid." + module + ".nodes", "");
		for (String nodeID : nodeIDs.split(",")) {
			ModuleSession ms = mss.byModuleAndNodeID(module, nodeID);
			OutgoingModuleSession rms = new OutgoingModuleSession(module, nodeID, mss);
			if (ms != null) {
				log.warn("Override Existing Remote Module:module=" + module + ",nodeid=" + nodeID + ",ms=" + ms);
			}
			mss.addOutogingModule(rms, nodeID);
			CKConnPool ckpool = nodePool.getPool(nodeID);
			rms.setConnsPool(ckpool);
			ck.addCheckHealth(ckpool);
		}

	}

	public void wallLocalModule() {

	}

	public HashMap<String, String> connParamToMap(String connvars) {
		HashMap<String, String> convars = new HashMap<String, String>();
		for (String conn : connvars.split("&")) {
			String ccparams[] = conn.split("=");
			if (ccparams.length == 2) {
				convars.put(ccparams[0], ccparams[1]);
			}
		}
		return convars;
	}

	public int getParamV(String key, HashMap<String, String> map, int defaultv) {
		try {
			if (map.containsKey(key)) {
				return Integer.parseInt(key);
			}
		} catch (NumberFormatException e) {
			log.warn("getParamV NumberFormatError for Key=" + key + ",defaultv=" + defaultv + ",maps=" + map, e);
		}
		return defaultv;
	}

	public static void main(String[] args) {
		String m = "otrans.servers.node.*";
		String key = "otrans.servers.node.n1.addr";
		System.out.println("match=" + key.matches(m));

	}

}
