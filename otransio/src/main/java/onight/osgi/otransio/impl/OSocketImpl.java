package onight.osgi.otransio.impl;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;
import org.glassfish.grizzly.Connection;
import org.osgi.framework.BundleContext;

import jnr.posix.POSIXFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.otransio.nio.OServer;
import onight.osgi.otransio.sm.MSessionSets;
import onight.osgi.otransio.sm.OutgoingSessionManager;
import onight.osgi.otransio.sm.RemoteModuleBean;
import onight.osgi.otransio.sm.RemoteModuleBean.ModuleBean;
import onight.osgi.otransio.sm.RemoteModuleSession;
import onight.tfw.async.CompleteHandler;
import onight.tfw.mservice.NodeHelper;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PSenderService;
import onight.tfw.otransio.api.PackHeader;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.otransio.api.beans.UnknowModuleBody;
import onight.tfw.otransio.api.session.CMDService;
import onight.tfw.otransio.api.session.ModuleSession;
import onight.tfw.outils.conf.PropHelper;

@Component(immediate = true)
@Instantiate(name = "osocketimpl")
@Provides(specifications = ActorService.class)
@Slf4j
public class OSocketImpl implements Serializable, ActorService {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6301364196672462354L;

	@Getter
	private PropHelper params;

	@ActorRequire
	@Getter
	@Setter
	ModuleDiscovery mdisc;

	public static String PACK_FROM = PackHeader.EXT_HIDDEN + "_" + "from";
	public static String PACK_TO = PackHeader.EXT_HIDDEN + "_" + "to";
	BundleContext context;

	public OSocketImpl(BundleContext context) {
		this.context = context;
		params = new PropHelper(context);
		mss = new MSessionSets(NodeHelper.getCurrNodeID());
		osm = new OutgoingSessionManager(this, params, mss);
		mss.setOsm(osm);
	}

	MSessionSets mss;

	OTransSender sender = new OTransSender(this);

	OServer server = new OServer();

	OutgoingSessionManager osm;

	public String getHostName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
		}
		return "localhost";
	}

	@Validate
	public void start() {
		server.startServer(this, params);

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					/**
					 * 等2秒主要是为了等注册
					 */
					Thread.sleep(2000);
				} catch (InterruptedException e) {
				}
				// 建立外联服务
				log.info("trying to init remote session.");
				osm.init();
				log.debug("ModuleDiscovery.required==" + mdisc);
				if (mdisc != null) {
					mdisc.updateModuleToGlobal(mss, params);
				}
				log.info("init remote session.[success] ");
			}
		}).start();
	}

	@Invalidate
	public void stop() {
		log.info("nio stoping");
		server.stop();
		log.info("nio stopped ... OK");
	}

	@Bind(aggregate = true, optional = true)
	public void bindPSender(PSenderService pl) {
		log.info("Register PSender::" + pl + ",sender=" + sender);
		SenderPolicy.bindPSender(pl, sender);
	}

	@Unbind(aggregate = true, optional = true)
	public void unbindPSender(PSenderService pl) {
		log.info("Remove PSender::" + pl);
	}

	@Bind(aggregate = true, optional = true)
	public void bindCMDService(CMDService service) {
		log.info("Register CMDService::" + service);
		ModuleSession ms = mss.addLocalMoudle(service.getModule());
		for (String cmd : service.getCmds()) {
			ms.registerService(cmd, service);
		}
		if (osm != null && osm.isReady()) {
			osm.getNodePool().broadcastLocalModule(mss);
		}
		if (mdisc != null) {
			mdisc.updateModuleToGlobal(mss, params);
		}
	}

	@Unbind(aggregate = true, optional = true)
	public void unbindCMDService(CMDService service) {
		log.info("Remove ModuleSession::" + service);
		mss.removeLocalModule(service.getModule());
		if (mdisc != null) {
			mdisc.updateModuleToGlobal(mss, params);
		}
	}

	public void onPacket(FramePacket pack, final CompleteHandler handler, Connection<?> conn) {
		if (PackHeader.REMOTE_LOGIN.equals(pack.getCMD())) {// 来自远端的登录
			RemoteModuleBean rmb = pack.parseBO(RemoteModuleBean.class);
			if (rmb != null) {
				for (ModuleBean mb : rmb.getModules()) {
					mss.addModule(mb.getModule(), mb.getNodeID(), conn);
				} // 交换信息
			}
			conn.write(mss.getLocalModulesPacketBack());
		} else if (PackHeader.REMOTE_LOGIN_RET.equals(pack.getCMD())) {// 来自远端的模块信息返回
			RemoteModuleBean rmb = pack.parseBO(RemoteModuleBean.class);
			if (rmb != null) {
				for (ModuleBean mb : rmb.getModules()) {
					mss.addModule(mb.getModule(), mb.getNodeID(), conn);
				} // 交换信息
			}
		} else if (PackHeader.CMD_HB.equals(pack.getCMD())) {// 来自远端的心跳线
			log.trace("[HB] From " + conn.getPeerAddress() + " , to " + conn.getLocalAddress());
		} else {
			routePacket(pack, handler);
		}
	}

	public void routeOutPacket(FramePacket pack, final CompleteHandler handler) {

		pack.putHeader(PACK_FROM, "");

		routePacket(pack, handler);
	}

	public void routePacket(FramePacket pack, final CompleteHandler handler) {
		mss.getAllRCounter().incrementAndGet();
		String destTO = pack.getExtStrProp(PACK_TO);
		ModuleSession ms = null;
		if (StringUtils.isNotBlank(destTO)) {// 固定给某个节点id的
			ms = mss.byModuleAndNodeID(pack.getModule(), destTO);
		} else {
			if (pack.getExtProp(PACK_FROM) != null) {
				ms = mss.getLocalModuleSession(pack.getModule());
			} else {
				ms = mss.byModule(pack.getModule());
			}
		}
		if (pack.isWallMessage()) {
			// 广播消息
			if (mss.getSessionByModule().get(pack.getModule()) != null) {
				String oldwr = pack.getExtStrProp(PackHeader.WALL_ROUTE);
				if (oldwr == null) {
					oldwr = "|" + mss.getCurrentNodeID();
					mss.getRecvCounter().incrementAndGet();

				}else{
					if (oldwr.contains("|" + mss.getCurrentNodeID())) {
						// 重复的
						mss.getDuplCounter().incrementAndGet();
					}else{
						mss.getRecvCounter().incrementAndGet();
					}
				}
				StringBuffer nodes = new StringBuffer(oldwr);
				if (!nodes.toString().contains("|" + mss.getCurrentNodeID())) {
					nodes.append("|" + mss.getCurrentNodeID());
				}
				for (String nodeid : mss.getSessionByModule().get(pack.getModule()).getAllObjMaps().keySet()) {
					if (!nodes.toString().contains("|" + nodeid)) {
						nodes.append("|").append(nodeid);
					}
				}
				
				mss.getLocalModuleSession(pack.getModule()).onPacket(pack, handler);

				pack.putHeader(PackHeader.WALL_ROUTE, nodes.toString());


				for (Entry<String, ModuleSession> kv : mss.getSessionByModule().get(pack.getModule()).getAllObjMaps()
						.entrySet()) {
					if (kv.getValue() instanceof RemoteModuleSession) {
						if (!oldwr.contains("|" + kv.getKey())) {
							FramePacket wallpack = PacketHelper.clonePacket(pack);
							wallpack.putHeader(PackHeader.TTL, "" + (pack.getTTL()));
							wallpack.putHeader(PACK_FROM, mss.getCurrentNodeID());
							mss.getSendCounter().incrementAndGet();
							kv.getValue().onPacket(wallpack, handler);
							mss.getAllSCounter().incrementAndGet();
						} else if (pack.getTTL() > 0) {// 相同的只广播几次
							FramePacket wallpack = PacketHelper.clonePacket(pack);
							wallpack.putHeader(PackHeader.TTL, "" + (pack.getTTL() - 1));
							wallpack.putHeader(PACK_FROM, mss.getCurrentNodeID());
							mss.getAllSCounter().incrementAndGet();
							kv.getValue().onPacket(wallpack, handler);
						} else {
							mss.getDropCounter().incrementAndGet();
						}
					}
				}

			}

			return;

		}
		if (ms != null) {
			if(ms instanceof RemoteModuleSession){
				mss.getSendCounter().incrementAndGet();
				mss.getAllSCounter().incrementAndGet();
			}else{
				mss.getRecvCounter().incrementAndGet();
			}
			ms.onPacket(pack, handler);
		} else {
			// 没有找到对应的消息
			if (pack.isSync()) {
				handler.onFinished(
						PacketHelper.toPBReturn(pack, new UnknowModuleBody(pack.getModule() + ",to=" + destTO, pack)));
			}
		}

	}

	public static void main(String[] args) {
		System.out.println("pid=" + POSIXFactory.getPOSIX().getpid());
		try {
			System.out.println("pid=" + InetAddress.getLocalHost().getHostName());
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
