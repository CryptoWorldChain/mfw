package onight.osgi.otransio.impl;

import java.io.IOException;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.ipojo.annotations.Invalidate;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.NActorProvider;
import onight.osgi.otransio.sm.MSessionSets;
import onight.tfw.async.CallBack;
import onight.tfw.mservice.NodeHelper;
import onight.tfw.ntrans.api.NActor;
import onight.tfw.ojpa.api.DomainDaoSupport;
import onight.tfw.ojpa.api.annotations.StoreDAO;
import onight.tfw.oparam.api.OParam;
import onight.tfw.oparam.api.OTreeValue;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.conf.PropHelper;
import onight.tfw.outils.serialize.JsonSerializer;

@NActorProvider
@Slf4j
public class ModuleDiscovery extends NActor {

	public ModuleDiscovery() {
	}

	@StoreDAO(target = "etcd", daoClass = OParam.class)
	@Getter 
	OParam oparam=new HashParam();

	public void setOparam(DomainDaoSupport daoparam) {
		if (daoparam != null && daoparam instanceof OParam) {
			oparam = (OParam) daoparam;
		} else {
			log.warn("cannot set OParam from:" + daoparam);
		}
	}

	boolean watched = false;

	@Invalidate
	public void stop() {
		log.info("mdisc stoping");

		if (oparam == null || mss == null)
			return;

		try {
			oparam.deleteDir("/zippo/nds/" + mss.getCurrentNodeID() + "/");
		} catch (Exception e) {
			log.warn("dir.delete.error", e);
		}

		log.info("mdisc stopped ... OK");
	}

	public void updateMemInfo() {
		try {
			Future<OTreeValue> f = oparam.get("/zippo/members/");
			if (f.get() != null && f.get().getNodes() != null) {
				for (OTreeValue v : f.get().getNodes()) {
					log.debug("try to Add new Member NetInfo:" + v);
					String jsonv = v.getValue();
					try {
						MemberSvcInfo memsvcinfo = JsonSerializer.getInstance().deserialize(jsonv, MemberSvcInfo.class);
						if(StringUtils.equals("audit_ok", memsvcinfo.getAuditstatus())){
							mss.getOsm().addNetPool(v.getKey().replace("/zippo/members/", ""),
									memsvcinfo.outaddr + ":" + memsvcinfo.outport,memsvcinfo.getCoreconn(),memsvcinfo.getMaxconn());
						}else if(StringUtils.equals("block", memsvcinfo.getAuditstatus())||StringUtils.equals("reject", memsvcinfo.getAuditstatus())){
							mss.getOsm().rmNetPool(v.getKey().replace("/zippo/members/", ""),
									memsvcinfo.outaddr + ":" + memsvcinfo.outport);
						}
					} catch (Exception e) {
					}
				}
			}
		} catch (Exception e) {
			// log.warn("updateMemInfo error:",e);
		}
	}
	
	public void updatePeerInfo() {
		try {
			Future<OTreeValue> f = oparam.get("/zippo/peers/");
		    String rolesstr = prop.get("org.zippo.bc.roles", "unknow");

			if (f.get() != null && f.get().getNodes() != null) {
				for (OTreeValue v : f.get().getNodes()) {
					log.debug("try to Add new Peer NetInfo:" + v);
					try {
						String jsonv = v.getValue();
						MemberSvcInfo memsvcinfo = JsonSerializer.getInstance().deserialize(jsonv, MemberSvcInfo.class);
						//如果自己本身就是member的话需要连过去做校验
						if(StringUtils.equals("audit_ok", memsvcinfo.getAuditstatus())||rolesstr.contains("member")){
							mss.getOsm().addNetPool(v.getKey().replace("/zippo/peers/", ""),
									memsvcinfo.outaddr + ":" + memsvcinfo.outport,memsvcinfo.getCoreconn(),memsvcinfo.getMaxconn());
						}else if(StringUtils.equals("block", memsvcinfo.getAuditstatus())||StringUtils.equals("reject", memsvcinfo.getAuditstatus())){
							mss.getOsm().rmNetPool(v.getKey().replace("/zippo/peers/", ""),
									memsvcinfo.outaddr + ":" + memsvcinfo.outport);
						}
					} catch (Exception e) {
					}
				}
			}
		} catch (Exception e) {
			// log.warn("updateMemInfo error:",e);
		}
	}

	@Override
	public void onDaoServiceAllReady() {
		// TODO Auto-generated method stub
		try {
			log.trace("onDaoServiceAllReady:oparam=:" + oparam);
			// String v1 = oparam.get("aabbcc").get();
			log.info("param.health==" + oparam.getHealth());
			if (watched)
				return;
			watched = true;
			updateModuleToGlobal(mss, prop);
			updateMemInfo();
			oparam.watch("/zippo/members/", new CallBack<OTreeValue>() {
				@Override
				public void onSuccess(OTreeValue ovalue) {
					log.trace("get info::" + ovalue);
					updateMemInfo();
				}

				@Override
				public void onFailed(Exception e, OTreeValue ovalue) {
					log.trace("get onfailed info::" + ovalue);
					updateMemInfo();
				}
			}, true);
			
			updatePeerInfo();

			oparam.watch("/zippo/peers/", new CallBack<OTreeValue>() {
				@Override
				public void onSuccess(OTreeValue ovalue) {
					log.trace("get info::" + ovalue);
					updatePeerInfo();
				}

				@Override
				public void onFailed(Exception e, OTreeValue ovalue) {
					log.trace("get onfailed info::" + ovalue);
					updatePeerInfo();
				}
			}, true);
		} catch (Exception e) {
			// e.printStackTrace();
			log.warn("Read param Value failed:");
		}
	}

	MSessionSets mss;
	PropHelper prop;

	public void updateModuleToGlobal(MSessionSets mss, PropHelper prop) {
		if (mss != null) {
			this.mss = mss;
			this.prop = prop;
		}
		if (oparam == null || mss == null)
			return;

		log.debug("nodeid=" + mss.getCurrentNodeID() + "::");

		for (String module : mss.getLocalModules()) {
			if (StringUtils.isNotBlank(module)) {
				log.info("module:==" + module + ",@nodeid=" + mss.getCurrentNodeID());
				try {
					Future<OTreeValue> f = oparam.put("/zippo/nds/" + mss.getCurrentNodeID() + "/" + module,
							mss.getCurrentNodeID());
					log.info("putresult==" + f.get());
				} catch (Exception e) {
					log.warn("cannot set param:" + module + ",mss=" + mss.getCurrentNodeID(), e);
				}
			}
		}
		String address = NodeHelper.getCurrNodeListenOutAddr();
		int port = NodeHelper.getCurrNodeListenOutPort();// prop.get("otrans.port",
															// prop.get("otrans.servers.default.port",
															// 5100));
		int core = prop.get("otrans.core", prop.get("otrans.servers.default.core", 2));
		int max = prop.get("otrans.max", prop.get("otrans.servers.default.max", 10));
		NodeInfo nodeinfo = new NodeInfo(address, port, core, max);
		try {
			oparam.put("/zippo/nds/" + mss.getCurrentNodeID() + "/info", JsonSerializer.formatToString(nodeinfo));
			log.debug("get zippo micro moudles===,{}" , oparam.getDir("/zippo"));
		} catch (Exception e) {
			log.warn("dir.error", e);
		}
	}

	String getConnInfo() {
		StringBuffer sb = new StringBuffer();
		sb.append("{\"conns\":").append(mss.getOsm().getNodePool().getJsonStr());
		sb.append(",\"mss\":").append(mss.getJsonInfo()).append("}");
		return sb.toString();
	}

	@Override
	public void doWeb(HttpServletRequest req, HttpServletResponse resp, FramePacket pack) throws IOException {
		String param = req.getParameter("cmd");
		if (StringUtils.isBlank(param) || StringUtils.equalsIgnoreCase(param, "conn")) {
			resp.getWriter().write(getConnInfo());
		} else {
			resp.getWriter().write(getConnInfo());
		}
	}

	@Override
	public String[] getCmds() {
		return new String[] { "INF" };
	}

	@Override
	public String getModule() {
		return "SYS";
	}

}
