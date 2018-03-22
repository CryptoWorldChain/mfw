package onight.osgi.otransio.sm;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.otransio.ck.CKConnPool;
import onight.osgi.otransio.ck.CheckHealth;
import onight.osgi.otransio.ck.NodeConnectionPool;
import onight.osgi.otransio.exception.NoneServerException;
import onight.osgi.otransio.impl.NodeInfo;
import onight.osgi.otransio.impl.OSocketImpl;
import onight.osgi.otransio.nio.OClient;
import onight.tfw.mservice.NodeHelper;
import onight.tfw.otransio.api.session.PSession;
import onight.tfw.outils.conf.PropHelper;

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
	NodeConnectionPool nodePool = new NodeConnectionPool();

	public String getJsonInfo() {
		StringBuffer sb = new StringBuffer();
		sb.append("{\"cur\":{");
		sb.append("\"nodename\":\"").append(NodeHelper.getCurrNodeName()).append("\"");
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

	public synchronized void rmNetPool(String nodeName, String addrport) {
		CKConnPool pool = nodePool.getPool(nodeName);// unknow modules
		if (pool != null) {
			pool.setStop(true);
		}

	}

	public synchronized CKConnPool addNetPool(String nodeName, String addrport, int coreconn, int maxconn)
			throws NoneServerException {
		if (addrport == null)
			throw new NoneServerException("addrPort is null");
		String addrports[] = addrport.split(":");
		if (addrports.length != 2)
			throw new NoneServerException("addrports format error try 'host:port' :" + addrport);

		try {
			String addr = addrports[0].trim();
			int port = Integer.parseInt(addrports[1].trim());
			String key = "otrans.servers.node." + nodeName;
			int core = coreconn;
			if (core == 0) {
				core = params.get(key + ".core", params.get("otrans.servers.default.core", 1));
			}
			int max = maxconn;
			if (max == 0) {
				max = params.get(key + ".max", params.get("otrans.servers.default.max", 3));
			}
			CKConnPool pool = nodePool.getPool(nodeName);// unknow modules
			if (pool == null) {
				pool = nodePool.addPool(client, nodeName, addr, port, core, max, mss);
				ck.addCheckHealth(pool);
			}
			return pool;
		} catch (Exception e) {
			throw new NoneServerException("add net pool error :" + e.getMessage(), e);
		}

	}

	public synchronized void init() {

		this.ready = true;
	}

	public RemoteModuleSession createOutgoingSSByURI(NodeInfo node) throws NoneServerException {
		CKConnPool pool = addNetPool(node.getNodeName(), node.getAddr() + ":" + node.getPort(), 0, 0);
		return createOutgoingSS(node, pool);
	}

	public synchronized RemoteModuleSession createOutgoingSS(NodeInfo node, CKConnPool ckpool)
			throws NoneServerException {
		PSession ms = mss.byNodeName(node.getNodeName());
		if (ms != null) {
			log.warn("Override Existing Remote nodeIdx=" + node.getNodeName() + ",ms=" + ms);
		}
		RemoteModuleSession rms = mss.addRemoteSession(node, null);
		if (rms != null) {
			rms.setConnsPool(ckpool);
			ck.addCheckHealth(ckpool);
		}
		return rms;
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

}
