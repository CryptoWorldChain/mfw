package onight.osgi.otransio.impl;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

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
import onight.osgi.annotation.NActorProvider;
import onight.osgi.otransio.nio.OServer;
import onight.osgi.otransio.sm.MSessionSets;
import onight.osgi.otransio.sm.OutgoingSessionManager;
import onight.osgi.otransio.sm.RemoteModuleBean;
import onight.osgi.otransio.sm.RemoteModuleBean.ModuleBean;
import onight.tfw.async.CompleteHandler;
import onight.tfw.mservice.NodeHelper;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.ojpa.api.IJPAClient;
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
public class OSocketImpl implements Serializable,ActorService {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6301364196672462354L;

	@Getter
	private PropHelper params;
	
	@ActorRequire
	@Getter @Setter
	ModuleDiscovery mdisc;


	public static String PACK_FROM = PackHeader.EXT_HIDDEN + "_" + "from";
	public static String PACK_TO = PackHeader.EXT_HIDDEN + "_" + "to";
	BundleContext context;

	public OSocketImpl(BundleContext context) {
		this.context = context;
		params = new PropHelper(context);
		mss = new MSessionSets(NodeHelper.getCurrNodeID());
		osm = new OutgoingSessionManager(this, params, mss);
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
				log.debug("ModuleDiscovery.required=="+mdisc);
				if(mdisc!=null){
					mdisc.updateModuleToGlobal(mss,params);
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
		if(mdisc!=null){
			mdisc.updateModuleToGlobal(mss,params);
		}
	}

	@Unbind(aggregate = true, optional = true)
	public void unbindCMDService(CMDService service) {
		log.info("Remove ModuleSession::" + service);
		mss.removeLocalModule(service.getModule());
		if(mdisc!=null){
			mdisc.updateModuleToGlobal(mss,params);
		}
	}

	public void onPacket(FramePacket pack, final CompleteHandler handler, Connection<?> conn) {
		if (PackHeader.REMOTE_LOGIN.equals(pack.getCMD())) {// 来自远端的登录
			RemoteModuleBean rmb = pack.parseBO(RemoteModuleBean.class);
			if (rmb != null) {
				for (ModuleBean mb : rmb.getModules()) {
					mss.addModule(mb.getModule(), mb.getNodeID(), conn);
				}
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
		if (ms != null) {
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
