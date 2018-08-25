package onight.osgi.otransio.sm;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.glassfish.grizzly.CloseListener;
import org.glassfish.grizzly.Closeable;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.ICloseType;
import org.glassfish.grizzly.attributes.Attribute;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.otransio.ck.CKConnPool;
import onight.osgi.otransio.ck.CheckHealth;
import onight.osgi.otransio.ck.NewConnCheckHealth;
import onight.osgi.otransio.ck.NodeConnectionPool;
import onight.osgi.otransio.ck.PackMapsCheckHealth;
import onight.osgi.otransio.ck.SyncMapCheckHealth;
import onight.osgi.otransio.exception.NoneServerException;
import onight.osgi.otransio.exception.UnAuthorizedConnectionException;
import onight.osgi.otransio.impl.NodeInfo;
import onight.osgi.otransio.impl.OSocketImpl;
import onight.osgi.otransio.nio.OClient;
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
	NewConnCheckHealth nck;

	@Getter
	@Setter
	boolean ready;
	@Getter
	NodeConnectionPool nodePool = new NodeConnectionPool();

	public String getJsonInfo() {
		StringBuffer sb = new StringBuffer();
		sb.append(nodePool.getJsonStr());
		return sb.toString();
	}
	PackMapsCheckHealth pmch ;
	SyncMapCheckHealth smch;

	public OutgoingSessionManager(OSocketImpl oimpl, PropHelper params, MSessionSets mss) {
		this.params = params;
		this.mss = mss;
		client = new OClient(oimpl);
		client.init(this, params);
		ck = new CheckHealth(params.get("otrans.checkhealth.size", 2), params.get("otrans.checkhealth.delay", 30));
		nck = new NewConnCheckHealth(params.get("otrans.max.conn.each.ip", 100),
				params.get("otrans.max.conn.timeout.sec", 30), ck.getExec());
		pmch = new PackMapsCheckHealth(mss);
		smch = new SyncMapCheckHealth(mss);
		ck.getExec().scheduleWithFixedDelay(smch, 10, 3, TimeUnit.SECONDS);
		ck.getExec().scheduleWithFixedDelay(pmch, 60, 60, TimeUnit.SECONDS);
	}

	public synchronized void rmNetPool(String nodeName) {
		CKConnPool pool = nodePool.destroyPool(nodeName);// unknow modules
		if (pool != null) {
			pool.setStop(true);
		}else{
			log.debug("unknow nodepool for "+nodeName);
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
				core = params.get(key + ".core", params.get("otrans.servers.default.core", 3));
			}
			int max = maxconn;
			if (max == 0) {
				max = params.get(key + ".max", params.get("otrans.servers.default.max", 10));
			}
			CKConnPool pool = nodePool.getPool(nodeName);// unknow modules
			if (pool == null) {
				pool = nodePool.addPool(client, nodeName, addr, port, core, max, mss);
				ck.addCheckHealth(nodePool, pool);
			}
			return pool;
		} catch (Exception e) {
			throw new NoneServerException("add net pool error :" + e.getMessage(), e);
		}

	}

	public synchronized void init() {

		this.ready = true;
	}

	public synchronized RemoteModuleSession createOutgoingSSByURI(NodeInfo node,String from_bcuid) throws NoneServerException {
		PSession ms = mss.byNodeName(node.getNodeName());
		if (ms != null && ms instanceof RemoteModuleSession) {
			RemoteModuleSession pms = (RemoteModuleSession) ms;
			return pms;
		}
		CKConnPool pool = addNetPool(node.getNodeName(), node.getAddr() + ":" + node.getPort(), 0, 0);
		pool.setFrom_bcuid(from_bcuid);
		return createOutgoingSS(node, pool);
	}

	protected final Attribute<RemoteModuleSession> osmStore = Grizzly.DEFAULT_ATTRIBUTE_BUILDER
			.createAttribute("session.by.connection");

	public void dropSessionByRemote(final Connection<?> conn) {
		RemoteModuleSession rms = osmStore.get(conn);
		if (rms != null) {
			log.debug("dropSessionByRemote:" + rms.nodeInfo.getNodeName());
			mss.dropSession(rms.nodeInfo.getNodeName(), false);
		}
	}

	public RemoteModuleSession addIncomming(final String nodename, final Connection<?> conn)
			throws UnAuthorizedConnectionException {
		RemoteModuleSession rms = osmStore.get(conn);
		if (rms == null) {
			synchronized (nodename.intern()) {
				PSession ms = mss.byNodeName(nodename);
				if (ms != null) {
					rms = (RemoteModuleSession) ms;
					log.debug("add exist connection:uid={},peer={}" , nodename , conn.getPeerAddress());
					rms.addConnection(conn);
					osmStore.set(conn, rms);
					conn.addCloseListener(new CloseListener<Closeable, ICloseType>() {
						@Override
						public void onClosed(Closeable closeable, ICloseType type) throws IOException {
							try {
								log.debug("remove attr from name=" + nodename + "conn:" + conn.getPeerAddress());
								osmStore.remove(conn);
							} catch (Exception e) {
							}
						}
					});
				} else {
					log.info("unknow connection from nodename:" + nodename);
					throw new UnAuthorizedConnectionException("unknow connnection node name:" + nodename);
					// NodeInfo ni = NodeInfo.fromName(nodename, conn);
					// CKConnPool pool = nodePool.getPool(nodename);// unknow
					// // modules
					// if (pool == null) {
					// pool = nodePool.addPool(client, nodename, ni.getAddr(),
					// ni.getPort(), ni.getCore(), ni.getMax(),
					// mss);
					// ck.addCheckHealth(nodePool,pool);
					// }
					// log.debug("add new connection:uid=" + nodename + ",peer="
					// + conn.getPeerAddress());
					// rms = mss.addRemoteSession(ni, pool);
				}

			}
		}

		return rms;
	}

	public synchronized RemoteModuleSession createOutgoingSS(NodeInfo node, CKConnPool ckpool)
			throws NoneServerException {
		PSession ms = mss.byNodeName(node.getNodeName());
		if (ms != null) {
			log.warn("Override Existing Remote nodeIdx=" + node.getNodeName() + ",ms=" + ms);
		}
		RemoteModuleSession rms = mss.addRemoteSession(node, ckpool);

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
